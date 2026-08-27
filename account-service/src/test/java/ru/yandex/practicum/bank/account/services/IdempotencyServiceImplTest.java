package ru.yandex.practicum.bank.account.services;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import ru.yandex.practicum.bank.account.exceptions.IdempotencyConflictException;
import ru.yandex.practicum.bank.account.exceptions.OperationAlreadyFailedException;
import ru.yandex.practicum.bank.account.exceptions.OperationInProgressException;
import ru.yandex.practicum.bank.account.exceptions.StoredOperationReadException;
import ru.yandex.practicum.bank.account.interfaces.BalanceTransactionRetryService;
import ru.yandex.practicum.bank.account.models.ProcessedOperationModel;
import ru.yandex.practicum.bank.account.models.ProcessedOperationStatusEnumModel;
import ru.yandex.practicum.bank.account.repositories.ProcessedOperationRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <summary>
 * Юнит-тесты сервиса идемпотентности (IdempotencyServiceImpl).
 * Проверяют регистрацию операций, кэширование успешных ответов,
 * обработку конфликтов по хешу запроса, статусы PROCESSING/FAILED
 * и корректное пробрасывание ошибок бизнес-логики.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class IdempotencyServiceImplTest {

    // region Constants

    private static final String OPERATION_ID = "op-12345";

    private static final String OPERATION_TYPE = "TRANSFER";

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-14T12:00:00Z");

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static final LocalDateTime FIXED_NOW = LocalDateTime.ofInstant(FIXED_INSTANT, UTC);

    // endregion

    // region Fields

    @Mock
    private ProcessedOperationRepository operationRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private BalanceTransactionRetryService retryService;

    private ObjectMapper objectMapper;

    private Clock fixedClock;

    private IdempotencyServiceImpl idempotencyService;

    // endregion

    // region Setup

    /**
     * <summary>
     * Инициализирует окружение перед каждым тестом, фиксирует часы (Clock),
     * настраивает маппер и моки для транзакций и повторных попыток.
     * </summary>
     **/
    @BeforeEach
    public void setUp() {
        fixedClock = Clock.fixed(FIXED_INSTANT, UTC);

        objectMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .findAndAddModules()
                .build();

        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);

        lenient().when(retryService.execute(any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

        idempotencyService = new IdempotencyServiceImpl(
                operationRepository,
                transactionManager,
                retryService,
                objectMapper,
                fixedClock
        );
    }

    // endregion

    // region Tests - successful first execution

    /**
     * <summary>
     * Проверяет успешный первый вызов: операция регистрируется как PROCESSING,
     * выполняется бизнес-логика, статус переводится в COMPLETED, результат возвращается.
     * </summary>
     **/
    @Test
    public void shouldExecuteBusinessOperationAndCompleteWhenFirstCall() {
        var request = new TestRequest("payload-1");

        var expectedResponse = new TestResponse("result-ok");

        when(operationRepository.findById(OPERATION_ID))
                .thenReturn(Optional.of(createProcessingOperation(OPERATION_ID, hashOf(request))));

        Supplier<TestResponse> businessOperation = () -> expectedResponse;

        var actual = idempotencyService.execute(
                OPERATION_ID,
                OPERATION_TYPE,
                request,
                TestResponse.class,
                businessOperation
        );

        assertThat(actual).isEqualTo(expectedResponse);

        verify(operationRepository).insertProcessing(
                eq(OPERATION_ID),
                eq(OPERATION_TYPE),
                anyString(),
                eq(FIXED_NOW)
        );

        ArgumentCaptor<ProcessedOperationModel> saveCaptor =
                ArgumentCaptor.forClass(ProcessedOperationModel.class);

        verify(operationRepository).save(saveCaptor.capture());

        var saved = saveCaptor.getValue();

        assertThat(saved.getStatus()).isEqualTo(ProcessedOperationStatusEnumModel.COMPLETED);

        assertThat(saved.getResponseJson()).isNotBlank();

        assertThat(saved.getUpdatedAt()).isEqualTo(FIXED_NOW);
    }

    // endregion

    // region Tests - existing COMPLETED operation (same hash)

    /**
     * <summary>
     * Проверяет повторный вызов с тем же operationId и тем же хешем запроса:
     * возвращается ранее сохранённый ответ без повторного выполнения бизнес-логики.
     * </summary>
     **/
    @Test
    public void shouldReturnCachedResponseWhenOperationAlreadyCompletedWithSameHash() {
        var request = new TestRequest("payload-1");

        var cachedResponse = new TestResponse("cached-result");

        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(operationRepository)
                .insertProcessing(anyString(), anyString(), anyString(), any(LocalDateTime.class));

        var completedOperation = createCompletedOperation(
                OPERATION_ID,
                hashOf(request),
                "{\"value\":\"cached-result\"}"
        );

        when(operationRepository.findById(OPERATION_ID))
                .thenReturn(Optional.of(completedOperation));

        Supplier<TestResponse> businessOperation = () -> {
            throw new AssertionError("Business operation must not be called");
        };

        var actual = idempotencyService.execute(
                OPERATION_ID,
                OPERATION_TYPE,
                request,
                TestResponse.class,
                businessOperation
        );

        assertThat(actual).isEqualTo(cachedResponse);

        verify(operationRepository).insertProcessing(
                eq(OPERATION_ID),
                eq(OPERATION_TYPE),
                anyString(),
                eq(FIXED_NOW)
        );
        verify(operationRepository, times(2)).findById(OPERATION_ID);

        verify(operationRepository, never()).save(any());
    }

    // endregion

    // region Tests - idempotency conflict (different hash)

    /**
     * <summary>
     * Проверяет выброс IdempotencyConflictException, когда operationId уже существует,
     * но хеш текущего запроса отличается от сохранённого.
     * </summary>
     **/
    @Test
    public void shouldThrowIdempotencyConflictExceptionWhenRequestHashDiffers() {
        var request = new TestRequest("payload-new");

        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(operationRepository)
                .insertProcessing(anyString(), anyString(), anyString(), any(LocalDateTime.class));

        var existingOperation = createCompletedOperation(
                OPERATION_ID,
                "different-hash-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "{\"value\":\"old\"}"
        );

        when(operationRepository.findById(OPERATION_ID))
                .thenReturn(Optional.of(existingOperation));

        assertThatThrownBy(() -> idempotencyService.execute(
                OPERATION_ID,
                OPERATION_TYPE,
                request,
                TestResponse.class,
                () -> new TestResponse("should-not-run")
        )).isInstanceOf(IdempotencyConflictException.class);

        verify(operationRepository).findById(OPERATION_ID);

        verify(operationRepository, never()).save(any());
    }

    // endregion

    // region Tests - operation still PROCESSING

    /**
     * <summary>
     * Проверяет выброс OperationInProgressException, если операция с данным ID
     * находится в статусе PROCESSING.
     * </summary>
     **/
    @Test
    public void shouldThrowOperationInProgressExceptionWhenStatusIsProcessing() {
        var request = new TestRequest("payload-1");

        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(operationRepository)
                .insertProcessing(anyString(), anyString(), anyString(), any(LocalDateTime.class));

        var processingOperation = createProcessingOperation(OPERATION_ID, hashOf(request));

        when(operationRepository.findById(OPERATION_ID))
                .thenReturn(Optional.of(processingOperation));

        assertThatThrownBy(() -> idempotencyService.execute(
                OPERATION_ID,
                OPERATION_TYPE,
                request,
                TestResponse.class,
                () -> new TestResponse("should-not-run")
        )).isInstanceOf(OperationInProgressException.class);

        verify(operationRepository, times(2)).findById(OPERATION_ID);

        verify(operationRepository, never()).save(any());
    }

    /**
     * <summary>
     * Проверяет выброс OperationInProgressException, когда запись не найдена
     * сразу после DataIntegrityViolationException (гонка).
     * </summary>
     **/
    @Test
    public void shouldThrowOperationInProgressExceptionWhenRecordNotFoundAfterConflict() {
        var request = new TestRequest("payload-1");

        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(operationRepository)
                .insertProcessing(anyString(), anyString(), anyString(), any(LocalDateTime.class));

        when(operationRepository.findById(OPERATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> idempotencyService.execute(
                OPERATION_ID,
                OPERATION_TYPE,
                request,
                TestResponse.class,
                () -> new TestResponse("should-not-run")
        )).isInstanceOf(OperationInProgressException.class);
    }

    // endregion

    // region Tests - operation already FAILED

    /**
     * <summary>
     * Проверяет выброс OperationAlreadyFailedException, если операция ранее
     * завершилась со статусом FAILED.
     * </summary>
     **/
    @Test
    public void shouldThrowOperationAlreadyFailedExceptionWhenStatusIsFailed() {
        var request = new TestRequest("payload-1");

        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(operationRepository)
                .insertProcessing(anyString(), anyString(), anyString(), any(LocalDateTime.class));

        var failedOperation = createFailedOperation(OPERATION_ID, hashOf(request));

        when(operationRepository.findById(OPERATION_ID))
                .thenReturn(Optional.of(failedOperation));

        assertThatThrownBy(() -> idempotencyService.execute(
                OPERATION_ID,
                OPERATION_TYPE,
                request,
                TestResponse.class,
                () -> new TestResponse("should-not-run")
        )).isInstanceOf(OperationAlreadyFailedException.class);

        verify(operationRepository, times(2)).findById(OPERATION_ID);

        verify(operationRepository, never()).save(any());
    }

    // endregion

    // region Tests - business operation failure

    /**
     * <summary>
     * Проверяет, что при RuntimeException из бизнес-логики операция освобождается (удаляется)
     * и исключение корректно пробрасывается наверх.
     * </summary>
     **/
    @Test
    public void shouldReleaseOperationAndRethrowWhenBusinessOperationThrows() {
        var request = new TestRequest("payload-1");

        var businessException = new IllegalStateException("business failed");

        Supplier<TestResponse> businessOperation = () -> {
            throw businessException;
        };

        assertThatThrownBy(() -> idempotencyService.execute(
                OPERATION_ID,
                OPERATION_TYPE,
                request,
                TestResponse.class,
                businessOperation
        )).isSameAs(businessException);

        verify(operationRepository).insertProcessing(
                eq(OPERATION_ID),
                eq(OPERATION_TYPE),
                anyString(),
                eq(FIXED_NOW)
        );

        verify(operationRepository).deleteById(OPERATION_ID);
    }

    // endregion

    // region Tests - stored response deserialization failure

    /**
     * <summary>
     * Проверяет выброс StoredOperationReadException, если сохранённый JSON
     * невозможно десериализовать в запрошенный тип ответа.
     * </summary>
     **/
    @Test
    public void shouldThrowStoredOperationReadExceptionWhenCachedJsonIsInvalid() {
        var request = new TestRequest("payload-1");

        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(operationRepository)
                .insertProcessing(anyString(), anyString(), anyString(), any(LocalDateTime.class));

        var completedOperation = createCompletedOperation(
                OPERATION_ID,
                hashOf(request),
                "{invalid-json"
        );

        when(operationRepository.findById(OPERATION_ID))
                .thenReturn(Optional.of(completedOperation));

        assertThatThrownBy(() -> idempotencyService.execute(
                OPERATION_ID,
                OPERATION_TYPE,
                request,
                TestResponse.class,
                () -> new TestResponse("should-not-run")
        )).isInstanceOf(StoredOperationReadException.class);
    }

    // endregion

    // region Helper Methods

    /**
     * <summary>
     * Создает модель тестовой операции в статусе PROCESSING.
     * </summary>
     **/
    private ProcessedOperationModel createProcessingOperation(String operationId, String requestHash) {
        return new ProcessedOperationModel(
                operationId,
                OPERATION_TYPE,
                requestHash,
                FIXED_NOW
        );
    }

    /**
     * <summary>
     * Создает модель тестовой операции в статусе COMPLETED с закешированным ответом.
     * </summary>
     **/
    private ProcessedOperationModel createCompletedOperation(
            String operationId,
            String requestHash,
            String responseJson
    ) {
        var operation = new ProcessedOperationModel(
                operationId,
                OPERATION_TYPE,
                requestHash,
                FIXED_NOW
        );
        operation.complete(responseJson, FIXED_NOW);

        return operation;
    }

    /**
     * <summary>
     * Создает модель тестовой операции в статусе FAILED.
     * </summary>
     **/
    private ProcessedOperationModel createFailedOperation(String operationId, String requestHash) {
        var operation = new ProcessedOperationModel(
                operationId,
                OPERATION_TYPE,
                requestHash,
                FIXED_NOW
        );

        operation.fail(FIXED_NOW);

        return operation;
    }

    /**
     * <summary>
     * Вычисляет SHA-256 хеш запроса через тестируемый сервис для обеспечения идентичности генерации отпечатка.
     * </summary>
     **/
    private String hashOf(Object request) {
        return idempotencyService.hashRequest(OPERATION_TYPE, request);
    }

    // endregion

    // region Test DTOs

    /**
     * <summary>
     * Тестовый класс запроса (DTO/Record) для проверки сериализации и хеширования.
     * </summary>
     **/
    public record TestRequest(String value) {
    }

    /**
     * <summary>
     * Тестовый класс ответа (DTO/Record) для проверки успешного возврата и кэширования.
     * </summary>
     **/
    public record TestResponse(String value) {
    }

    // endregion
}