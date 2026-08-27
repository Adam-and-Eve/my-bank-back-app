package ru.yandex.practicum.bank.account.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.bank.account.exceptions.IdempotencyConflictException;
import ru.yandex.practicum.bank.account.exceptions.OperationAlreadyFailedException;
import ru.yandex.practicum.bank.account.exceptions.OperationInProgressException;
import ru.yandex.practicum.bank.account.exceptions.StoredOperationReadException;
import ru.yandex.practicum.bank.account.interfaces.BalanceTransactionRetryService;
import ru.yandex.practicum.bank.account.interfaces.IdempotencyService;
import ru.yandex.practicum.bank.account.models.ProcessedOperationModel;
import ru.yandex.practicum.bank.account.models.ProcessedOperationStatusEnumModel;
import ru.yandex.practicum.bank.account.repositories.ProcessedOperationRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <summary>
 * Сервис обеспечения идемпотентности выполняемых операций (IdempotencyServiceImpl).
 * Предотвращает дублирование бизнес-действий с помощью регистрации ключей идемпотентности,
 * проверки SHA-256 хеша запроса и кэширования результатов выполнения.
 * </summary>
 **/
@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    // region Constants

    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(5);

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    // endregion

    // region Fields

    private final ProcessedOperationRepository operationRepository;
    private final TransactionTemplate claimTransaction;
    private final TransactionTemplate businessTransaction;
    private final BalanceTransactionRetryService retryService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    // endregion

    // region Constructors

    // region Constructors

    /**
     * <summary>
     * Инициализирует сервис с необходимыми компонентами и настраивает TransactionTemplate
     * для безопасного управления транзакциями (включая PROPAGATION_REQUIRES_NEW для claim-операций).
     * </summary>
     * @param operationRepository Репозиторий хранения метаданных о статусах операций.
     * @param transactionManager Менеджер транзакций платформы.
     * @param retryService Сервис для повторного выполнения транзакций при блокировках.
     * @param objectMapper Маппер JSON (копируется и безопасно донастраивается для детерминированного хеширования).
     * @param clock Часы приложения для контроля таймаутов зависших операций.
     **/
    public IdempotencyServiceImpl(
            ProcessedOperationRepository operationRepository,
            PlatformTransactionManager transactionManager,
            BalanceTransactionRetryService retryService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.operationRepository = operationRepository;

        this.claimTransaction = new TransactionTemplate(transactionManager);

        this.claimTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.businessTransaction = new TransactionTemplate(transactionManager);

        this.retryService = retryService;

        this.objectMapper = objectMapper.copy();

        this.objectMapper.setConfig(
                this.objectMapper.getSerializationConfig()
                        .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                        .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        );

        this.objectMapper.setConfig(
                this.objectMapper.getDeserializationConfig()
                        .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        );

        this.clock = clock;
    }

    // endregion

    // region Public Methods

    /**
     * <summary>
     * Выполняет операцию с гарантиями идемпотентности. Если операция с указанным ID выполняется впервые,
     * она регистрируется и выполняется. Если она уже существует, проверяется ее отпечаток (хеш) и возвращается
     * сохраненный результат (или выбрасывается исключение, если операция в обработке или завершилась с ошибкой).
     * </summary>
     * @param operationId Уникальный идентификатор операции.
     * @param operationType Тип операции.
     * @param request Объект запроса (используется для формирования хеша).
     * @param responseType Ожидаемый тип ответа.
     * @param businessOperation Функция, содержащая бизнес-логику операции.
     * @param <T> Тип возвращаемого значения.
     * @return Результат выполнения бизнес-операции или ранее сохраненный результат.
     **/
    @Override
    public <T> T execute(
            String operationId,
            String operationType,
            Object request,
            Class<T> responseType,
            Supplier<T> businessOperation
    ) {
        var requestHash = hashRequest(operationType, request);

        if (!tryStartOperation(operationId, operationType, requestHash)) {
            return handleExistingOperation(operationId, operationType, requestHash, responseType);
        }

        try {
            return retryService.execute(() ->
                    businessTransaction.execute(status -> {
                        T response = businessOperation.get();
                        completeOperation(operationId, response);
                        return response;
                    }));
        } catch (RuntimeException exception) {
            releaseOperation(operationId);

            throw exception;
        }
    }

    // endregion

    // region Private Methods

    /**
     * <summary>
     * Пытается атомарно захватить (зарегистрировать) новую операцию в базе данных.
     * Если происходит DataIntegrityViolationException (нарушение уникальности), это означает, что операция
     * с таким ID уже есть, и мы пытаемся обработать зависшие (stale) транзакции.
     * </summary>
     **/
    private boolean tryStartOperation(String operationId, String operationType, String requestHash) {
        try {
            claimTransaction.executeWithoutResult(status -> {
                operationRepository.insertProcessing(
                        operationId,
                        operationType,
                        requestHash,
                        LocalDateTime.now(clock)
                );
            });

            return true;
        } catch (DataIntegrityViolationException exception) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Processed operation already exists operationId={} operationType={} status=conflict source=account-service",
                        operationId,
                        operationType
                );
            }

            return retryStaleOperation(operationId, operationType, requestHash);
        } catch (RuntimeException exception) {
            log.error(
                    "Processed operation write failed operationId={} operationType={} status=error errorCategory=database errorType={} source=account-service",
                    operationId,
                    operationType,
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }
    }

    /**
     * <summary>
     * Обрабатывает ситуацию, когда операция уже существует, но возможно она зависла в статусе PROCESSING.
     * Если время обработки превышает PROCESSING_TIMEOUT, зависшая операция удаляется и захватывается заново.
     * </summary>
     **/
    private boolean retryStaleOperation(String operationId, String operationType, String requestHash) {
        var operation = claimTransaction.execute(status -> operationRepository.findById(operationId)
                .orElseThrow(() -> new OperationInProgressException(operationId)));

        Objects.requireNonNull(operation, "Processed operation cannot be null"); // Исправлен warning

        validateFingerprint(operationId, operationType, requestHash, operation);

        if (operation.getStatus() != ProcessedOperationStatusEnumModel.PROCESSING) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Processed operation result reused operationId={} operationType={} status={} source=account-service",
                        operationId,
                        operationType,
                        operation.getStatus()
                );
            }

            return false;
        }

        var staleBefore = LocalDateTime.now(clock).minus(PROCESSING_TIMEOUT);

        if (operation.getUpdatedAt().isAfter(staleBefore)) {
            return false;
        }

        Integer deletedObj = claimTransaction.execute(status ->
                operationRepository.deleteStaleProcessing(operationId, staleBefore));

        int deleted = deletedObj != null ? deletedObj : 0; // Безопасная распаковка (устраняет warning)

        var retryStarted = deleted == 1 && tryStartOperation(operationId, operationType, requestHash);

        if (retryStarted && log.isDebugEnabled()) {
            log.debug(
                    "Stale processed operation retry applied operationId={} operationType={} status=retry source=account-service",
                    operationId,
                    operationType
            );
        }

        return retryStarted;
    }

    /**
     * <summary>
     * Обрабатывает сценарии, когда операция была найдена в базе: возвращает закешированный результат,
     * либо выбрасывает ошибку, если она еще выполняется или была неуспешной.
     * </summary>
     **/
    private <T> T handleExistingOperation(
            String operationId,
            String operationType,
            String requestHash,
            Class<T> responseType
    ) {
        var operation = claimTransaction.execute(status -> operationRepository.findById(operationId)
                .orElseThrow(() -> new OperationInProgressException(operationId)));

        Objects.requireNonNull(operation, "Processed operation cannot be null"); // Исправлен warning

        validateFingerprint(operationId, operationType, requestHash, operation);

        if (operation.getStatus() == ProcessedOperationStatusEnumModel.PROCESSING) {
            log.warn(
                    "Processed operation rejected operationId={} operationType={} status=in_progress errorCode=OPERATION_IN_PROGRESS source=account-service",
                    operationId,
                    operationType
            );

            throw new OperationInProgressException(operationId);
        }
        if (operation.getStatus() == ProcessedOperationStatusEnumModel.FAILED) {
            log.warn(
                    "Processed operation rejected operationId={} operationType={} status=failed errorCode=OPERATION_ALREADY_FAILED source=account-service",
                    operationId,
                    operationType
            );

            throw new OperationAlreadyFailedException(operationId);
        }

        try {
            T response = objectMapper.readValue(operation.getResponseJson(), responseType);

            if (log.isDebugEnabled()) {
                log.debug(
                        "Stored processed operation response returned operationId={} operationType={} status=completed source=account-service",
                        operationId,
                        operationType
                );
            }

            return response;
        } catch (JsonProcessingException exception) {
            log.error(
                    "Processed operation response read failed operationId={} operationType={} status=error errorCategory=serialization errorType={} source=account-service",
                    operationId,
                    operationType,
                    exception.getClass().getSimpleName()
            );

            throw new StoredOperationReadException(operationId, exception);
        }
    }

    /**
     * <summary>
     * Валидирует отпечаток сохраненной операции (тип и хеш), чтобы предотвратить подмену данных.
     * </summary>
     **/
    private void validateFingerprint(
            String operationId,
            String operationType,
            String requestHash,
            ProcessedOperationModel operation
    ) {
        if (!operation.getOperationType().equals(operationType)
                || !operation.getRequestHash().equals(requestHash)) {
            log.warn(
                    "Processed operation rejected operationId={} operationType={} status=conflict errorCode=IDEMPOTENCY_CONFLICT source=account-service",
                    operationId,
                    operationType
            );

            throw new IdempotencyConflictException(operationId);
        }
    }

    /**
     * <summary>
     * Фиксирует успешное завершение операции и сохраняет результат в JSON-формате для будущих идемпотентных обращений.
     * </summary>
     **/
    private void completeOperation(String operationId, Object response) {
        var responseJson = writeJson(operationId, response);

        try {
            var operation = operationRepository.findById(operationId)
                    .orElseThrow(() -> new OperationInProgressException(operationId));

            operation.complete(responseJson, LocalDateTime.now(clock));

            operationRepository.save(operation);
        } catch (RuntimeException exception) {
            log.error(
                    "Processed operation write failed operationId={} status=error errorCategory=database errorType={} source=account-service",
                    operationId,
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }
    }

    /**
     * <summary>
     * Удаляет операцию из таблицы в случае возникновения критической ошибки при ее выполнении,
     * позволяя клиенту повторить запрос (при условии, что ошибка восстановимая).
     * </summary>
     **/
    private void releaseOperation(String operationId) {
        try {
            claimTransaction.executeWithoutResult(status -> operationRepository.deleteById(operationId));
        } catch (RuntimeException exception) {
            log.error(
                    "Processed operation release failed operationId={} status=error errorCategory=database errorType={} source=account-service",
                    operationId,
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }
    }

    /**
     * <summary>
     * Формирует детерминированный SHA-256 хеш от нормализованного тела запроса и типа операции.
     * </summary>
     **/
    String hashRequest(String operationType, Object request) {
        JsonNode payload = normalize(objectMapper.valueToTree(request));

        ObjectNode fingerprint = objectMapper.createObjectNode();

        fingerprint.put("operationType", operationType);

        fingerprint.set("payload", payload);

        return sha256(writeJson("request", fingerprint));
    }

    /**
     * <summary>
     * Рекурсивно нормализует JSON дерево (сортирует ключи, обрезает лишние нули у чисел с плавающей точкой)
     * для гарантии одинакового хеша при идентичных, но структурно по-разному переданных данных.
     * </summary>
     **/
    private JsonNode normalize(JsonNode node) {
        if (node.isObject()) {
            var normalized = objectMapper.createObjectNode();

            node.properties().forEach(entry ->
                    normalized.set(entry.getKey(), normalize(entry.getValue())));

            return normalized;
        }

        if (node.isArray()) {
            var normalized = objectMapper.createArrayNode();

            node.forEach(value -> normalized.add(normalize(value)));

            return normalized;
        }

        if (node.isBigDecimal() || node.isFloatingPointNumber()) {
            return objectMapper.getNodeFactory().numberNode(node.decimalValue().stripTrailingZeros());
        }

        return node;
    }

    /**
     * <summary>
     * Сериализует объект в JSON-строку.
     * </summary>
     **/
    private String writeJson(String operationId, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            log.error(
                    "Processed operation serialization failed operationId={} status=error errorCategory=serialization errorType={} source=account-service",
                    operationId,
                    exception.getClass().getSimpleName()
            );

            throw new StoredOperationReadException(operationId, exception);
        }
    }

    /**
     * <summary>
     * Вычисляет SHA-256 хеш-сумму строки и возвращает ее в HEX-формате.
     * </summary>
     **/
    private String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");

            var hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    // endregion
}