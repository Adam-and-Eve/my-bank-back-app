package ru.yandex.practicum.bank.account.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.bank.account.exceptions.IdempotencyConflictException;
import ru.yandex.practicum.bank.account.exceptions.OperationAlreadyFailedException;
import ru.yandex.practicum.bank.account.exceptions.OperationInProgressException;
import ru.yandex.practicum.bank.account.exceptions.StoredOperationReadException;
import ru.yandex.practicum.bank.account.interfaces.IdempotencyService;
import ru.yandex.practicum.bank.account.models.ProcessedOperationModel;
import ru.yandex.practicum.bank.account.models.ProcessedOperationStatusEnumModel;
import ru.yandex.practicum.bank.account.repositories.ProcessedOperationRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
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

    // region Fields

    private final ProcessedOperationRepository operationRepository;
    private final TransactionTemplate operationTransaction;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    // endregion

    // region Constructors

    public IdempotencyServiceImpl(
            ProcessedOperationRepository operationRepository,
            PlatformTransactionManager transactionManager,
            Clock clock
    ) {
        this.operationRepository = Objects.requireNonNull(operationRepository, "Operation repository must not be null");

        Objects.requireNonNull(transactionManager, "Transaction manager must not be null");

        this.operationTransaction = new TransactionTemplate(transactionManager);

        this.operationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.objectMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .findAndAddModules()
                .build();

        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Выполняет бизнес-операцию с обеспечением гарантии идемпотентности.
     * При повторном вызове с тем же operationId возвращает сохраненный результат или выбрасывает соответствующее исключение.
     * </summary>
     * @param operationId Уникальный идентификатор операции (ключ идемпотентности).
     * @param operationType Наименование типа операции.
     * @param request Объект запроса для расчета SHA-256 хеша.
     * @param responseType Класс ожидаемого результата.
     * @param businessOperation Функция выполнения бизнес-логики.
     * @param <T> Тип возвращаемого результата.
     * @return Результат выполнения бизнес-операции или ранее сохранённый ответ.
     * @throws IdempotencyConflictException Если операция с таким ID выполнялась с другими параметрами.
     * @throws OperationInProgressException Если операция с таким ID сейчас находится в процессе выполнения.
     * @throws OperationAlreadyFailedException Если операция с таким ID ранее завершилась ошибкой.
     */
    @Override
    public <T> T execute(
            String operationId,
            String operationType,
            Object request,
            Class<T> responseType,
            Supplier<T> businessOperation
    ) {
        Objects.requireNonNull(operationId, "Operation ID must not be null");

        Objects.requireNonNull(operationType, "Operation type must not be null");

        Objects.requireNonNull(request, "Request object must not be null");

        Objects.requireNonNull(responseType, "Response type must not be null");

        Objects.requireNonNull(businessOperation, "Business operation supplier must not be null");

        var requestHash = hashRequest(request);

        if (!tryStartOperation(operationId, operationType, requestHash)) {
            return handleExistingOperation(operationId, requestHash, responseType);
        }

        try {
            T response = businessOperation.get();

            completeOperation(operationId, response);

            return response;
        } catch (RuntimeException exception) {
            failOperation(operationId);

            throw exception;
        }
    }

    /**
     * <summary>
     * Пытается зарегистрировать операцию со статусом PROCESSING в отдельной транзакции.
     * </summary>
     * @param operationId ID операции.
     * @param operationType Тип операции.
     * @param requestHash Хеш запроса.
     * @return {@code true}, если запись успешно создана; {@code false}, если ключ уже существует в БД.
     */
    private boolean tryStartOperation(String operationId, String operationType, String requestHash) {
        try {
            operationTransaction.executeWithoutResult(status -> {
                operationRepository.insertProcessing(
                        operationId,
                        operationType,
                        requestHash,
                        LocalDateTime.now(clock)
                );
            });
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    /**
     * <summary>
     * Обрабатывает случай, когда операция с данным ID уже зарегистрирована в системе.
     * </summary>
     * @param operationId ID операции.
     * @param requestHash Хеш текущего запроса.
     * @param responseType Класс требуемого ответа.
     * @param <T> Тип ответа.
     * @return Ранее сохраненный и десериализованный результат операции.
     */
    private <T> T handleExistingOperation(String operationId, String requestHash, Class<T> responseType) {
        ProcessedOperationModel operation = operationTransaction.execute(status ->
                operationRepository.findById(operationId)
                        .orElseThrow(() -> new OperationInProgressException(operationId))
        );

        if (operation == null) {
            throw new OperationInProgressException(operationId);
        }

        if (!operation.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(operationId);
        }

        if (operation.getStatus() == ProcessedOperationStatusEnumModel.PROCESSING) {
            throw new OperationInProgressException(operationId);
        }

        if (operation.getStatus() == ProcessedOperationStatusEnumModel.FAILED) {
            throw new OperationAlreadyFailedException(operationId);
        }

        try {
            return objectMapper.readValue(operation.getResponseJson(), responseType);
        } catch (JsonProcessingException exception) {
            throw new StoredOperationReadException(operationId, exception);
        }
    }

    /**
     * <summary>
     * Переводит статус операции в COMPLETED и сохраняет JSON-ответ в отдельной транзакции.
     * </summary>
     * @param operationId ID операции.
     * @param response Объект ответа для сериализации.
     */
    private void completeOperation(String operationId, Object response) {
        var responseJson = writeJson(operationId, response);

        operationTransaction.executeWithoutResult(status -> {
            ProcessedOperationModel operation = operationRepository.findById(operationId)
                    .orElseThrow(() -> new OperationInProgressException(operationId));
            operation.complete(responseJson, LocalDateTime.now(clock));

            operationRepository.save(operation);
        });
    }

    /**
     * <summary>
     * Переводит статус операции в FAILED в отдельной транзакции при возникновении ошибки бизнес-логики.
     * </summary>
     * @param operationId ID операции.
     */
    private void failOperation(String operationId) {
        operationTransaction.executeWithoutResult(status -> operationRepository.findById(operationId)
                .ifPresent(operation -> {
                    operation.fail(LocalDateTime.now(clock));

                    operationRepository.save(operation);
                }));
    }

    /**
     * <summary>
     * Рассчитывает SHA-256 хеш от JSON-представления запроса.
     * </summary>
     * @param request Объект запроса.
     * @return Шестнадцатеричное представление хеша в виде строки.
     */
    private String hashRequest(Object request) {
        return sha256(writeJson("request", request));
    }

    /**
     * <summary>
     * Сериализует объект в JSON-строку с детерминированным порядком полей.
     * </summary>
     * @param operationId ID операции для формирования исключения в случае ошибки.
     * @param value Объект для сериализации.
     * @return Сформированная JSON-строка.
     */
    private String writeJson(String operationId, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new StoredOperationReadException(operationId, exception);
        }
    }

    /**
     * <summary>
     * Вычисляет SHA-256 от переданной UTF-8 строки.
     * </summary>
     * @param value Входная строка.
     * @return Хеш-сумма в виде Hex-строки.
     */
    private String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");

            var hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available in the environment", exception);
        }
    }

    // endregion
}
