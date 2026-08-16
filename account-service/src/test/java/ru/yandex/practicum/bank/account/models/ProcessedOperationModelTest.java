package ru.yandex.practicum.bank.account.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <summary>
 * Модульные тесты для доменной сущности ProcessedOperationModel.
 * Проверяют создание сущности в начальном статусе PROCESSING, переходы по жизненному циклу (complete, fail),
 * валидацию аргументов и корректность работы equals и hashCode по бизнес-ключу operationId.
 * </summary>
 **/
public class ProcessedOperationModelTest {

    // region Constants

    private static final String OPERATION_ID = "op-transfer-100";

    private static final String OPERATION_TYPE = "TRANSFER";

    private static final String REQUEST_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 14, 15, 0, 0);

    private static final String RESPONSE_JSON = "{\"status\":\"SUCCESS\"}";

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешное создание сущности ProcessedOperationModel в начальном статусе PROCESSING.
     * </summary>
     **/
    @Test
    public void shouldCreateProcessedOperationModelWithInitialStatusProcessing() {
        ProcessedOperationModel operation = new ProcessedOperationModel(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                NOW
        );

        assertThat(operation.getOperationId()).isEqualTo(OPERATION_ID);

        assertThat(operation.getOperationType()).isEqualTo(OPERATION_TYPE);

        assertThat(operation.getRequestHash()).isEqualTo(REQUEST_HASH);

        assertThat(operation.getStatus()).isEqualTo(ProcessedOperationStatusEnumModel.PROCESSING);

        assertThat(operation.getResponseJson()).isNull();

        assertThat(operation.getCreatedAt()).isEqualTo(NOW);

        assertThat(operation.getUpdatedAt()).isEqualTo(NOW);
    }

    /**
     * <summary>
     * Проверяет выброс NullPointerException с соответствующими сообщениями при передаче null в конструктор.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenConstructorArgsAreNull() {
        assertThatThrownBy(() -> new ProcessedOperationModel(null, OPERATION_TYPE, REQUEST_HASH, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Operation ID must not be null");

        assertThatThrownBy(() -> new ProcessedOperationModel(OPERATION_ID, null, REQUEST_HASH, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Operation type must not be null");

        assertThatThrownBy(() -> new ProcessedOperationModel(OPERATION_ID, OPERATION_TYPE, null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Request hash must not be null");

        assertThatThrownBy(() -> new ProcessedOperationModel(OPERATION_ID, OPERATION_TYPE, REQUEST_HASH, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Created at timestamp must not be null");
    }

    /**
     * <summary>
     * Проверяет успешный перевод операции в статус COMPLETED с сохранением ответа и времени завершения.
     * </summary>
     **/
    @Test
    public void shouldCompleteOperationSuccessfully() {
        ProcessedOperationModel operation = new ProcessedOperationModel(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                NOW
        );
        LocalDateTime completedAt = NOW.plusSeconds(5);

        operation.complete(RESPONSE_JSON, completedAt);

        assertThat(operation.getStatus()).isEqualTo(ProcessedOperationStatusEnumModel.COMPLETED);

        assertThat(operation.getResponseJson()).isEqualTo(RESPONSE_JSON);

        assertThat(operation.getUpdatedAt()).isEqualTo(completedAt);

        assertThat(operation.getCreatedAt()).isEqualTo(NOW);
    }

    /**
     * <summary>
     * Проверяет выброс NullPointerException при попытке завершения операции с null в качестве timestamp обновления.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenCompleteWithNullTimestamp() {
        ProcessedOperationModel operation = new ProcessedOperationModel(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                NOW
        );

        assertThatThrownBy(() -> operation.complete(RESPONSE_JSON, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Updated at timestamp must not be null");
    }

    /**
     * <summary>
     * Проверяет успешный перевод операции в статус FAILED с обновлением времени.
     * </summary>
     **/
    @Test
    public void shouldFailOperationSuccessfully() {
        ProcessedOperationModel operation = new ProcessedOperationModel(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                NOW
        );
        LocalDateTime failedAt = NOW.plusSeconds(2);

        operation.fail(failedAt);

        assertThat(operation.getStatus()).isEqualTo(ProcessedOperationStatusEnumModel.FAILED);

        assertThat(operation.getUpdatedAt()).isEqualTo(failedAt);

        assertThat(operation.getResponseJson()).isNull();

        assertThat(operation.getCreatedAt()).isEqualTo(NOW);
    }

    /**
     * <summary>
     * Проверяет выброс NullPointerException при попытке перевести операцию в FAILED с null в качестве timestamp обновления.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenFailWithNullTimestamp() {
        ProcessedOperationModel operation = new ProcessedOperationModel(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                NOW
        );

        assertThatThrownBy(() -> operation.fail(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Updated at timestamp must not be null");
    }

    /**
     * <summary>
     * Проверяет равенство объектов по бизнес-ключу operationId и совпадение их hashCode.
     * </summary>
     **/
    @Test
    public void shouldBeEqualWhenSameOperationId() {
        ProcessedOperationModel operation1 = new ProcessedOperationModel(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                NOW
        );
        ProcessedOperationModel operation2 = new ProcessedOperationModel(
                OPERATION_ID,
                "OTHER_TYPE",
                "other_hash",
                NOW.plusDays(1)
        );

        assertThat(operation1).isEqualTo(operation2);

        assertThat(operation1.hashCode()).isEqualTo(operation2.hashCode());
    }

    /**
     * <summary>
     * Проверяет неравенство объектов при отличающихся operationId.
     * </summary>
     **/
    @Test
    public void shouldNotBeEqualWhenDifferentOperationId() {
        ProcessedOperationModel operation1 = new ProcessedOperationModel(
                "op-1",
                OPERATION_TYPE,
                REQUEST_HASH,
                NOW
        );
        ProcessedOperationModel operation2 = new ProcessedOperationModel(
                "op-2",
                OPERATION_TYPE,
                REQUEST_HASH,
                NOW
        );

        assertThat(operation1).isNotEqualTo(operation2);
    }

    /**
     * <summary>
     * Проверяет граничные случаи работы метода equals (рефлексивность, сравнение с null и с объектом другого класса).
     * </summary>
     **/
    @Test
    public void shouldHandleEqualsEdgeCases() {
        ProcessedOperationModel operation = new ProcessedOperationModel(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                NOW
        );

        assertThat(operation).isEqualTo(operation);

        assertThat(operation).isNotEqualTo(null);

        assertThat(operation).isNotEqualTo("other_type_object");
    }

    // endregion
}