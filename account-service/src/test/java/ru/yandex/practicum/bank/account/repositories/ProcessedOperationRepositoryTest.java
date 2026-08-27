package ru.yandex.practicum.bank.account.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <summary>
 * Тесты уровня данных (Data JPA) для репозитория ProcessedOperationRepository.
 * Проверяют корректность выполнения нативного SQL-запроса атомарной регистрации
 * новой операции со статусом PROCESSING, обработку уникальности ключей
 * и логику очистки зависших (stale) транзакций.
 * </summary>
 **/
@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class ProcessedOperationRepositoryTest {

    // region Constants

    private static final String OPERATION_ID = "op-uuid-12345-67890";

    private static final String OPERATION_TYPE = "TRANSFER";

    private static final String REQUEST_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    // endregion

    // region Fields

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProcessedOperationRepository processedOperationRepository;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную вставку новой записи операции со статусом PROCESSING.
     * Убеждается, что возвращается 1 изменённая строка, и данные корректно сохраняются в БД.
     * </summary>
     **/
    @Test
    public void shouldInsertProcessingOperationSuccessfully() {
        var now = LocalDateTime.now();

        int rowsInserted = processedOperationRepository.insertProcessing(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                now
        );

        assertThat(rowsInserted).isEqualTo(1);

        entityManager.clear();

        var foundOptional = processedOperationRepository.findById(OPERATION_ID);

        assertThat(foundOptional).isPresent();

        var operation = foundOptional.get();

        assertThat(operation.getOperationId()).isEqualTo(OPERATION_ID);

        assertThat(operation.getOperationType()).isEqualTo(OPERATION_TYPE);

        assertThat(operation.getRequestHash()).isEqualTo(REQUEST_HASH);

        assertThat(operation.getStatus().toString()).isEqualTo("PROCESSING");
    }

    /**
     * <summary>
     * Проверяет выброс исключения DataIntegrityViolationException при попытке повторной вставки
     * записи с уже существующим operationId (проверка ограничения первичного ключа).
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenInsertingDuplicateOperationId() {
        var now = LocalDateTime.now();

        processedOperationRepository.insertProcessing(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                now
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> processedOperationRepository.insertProcessing(
                        OPERATION_ID,
                        OPERATION_TYPE,
                        REQUEST_HASH,
                        now
                )
        );
    }

    /**
     * <summary>
     * Проверяет успешное удаление зависшей операции со статусом PROCESSING,
     * если время её последнего обновления меньше или равно пороговому значению.
     * </summary>
     **/
    @Test
    public void shouldDeleteStaleProcessingOperationSuccessfully() {
        var oldDate = LocalDateTime.now().minusMinutes(10);

        processedOperationRepository.insertProcessing(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                oldDate
        );

        var staleBefore = LocalDateTime.now().minusMinutes(5);

        int rowsDeleted = processedOperationRepository.deleteStaleProcessing(OPERATION_ID, staleBefore);

        assertThat(rowsDeleted).isEqualTo(1);

        entityManager.clear();

        assertThat(processedOperationRepository.findById(OPERATION_ID)).isEmpty();
    }

    /**
     * <summary>
     * Проверяет, что операция не удаляется, если она была обновлена позже порогового значения
     * (то есть она еще активна и не считается зависшей).
     * </summary>
     **/
    @Test
    public void shouldNotDeleteRecentProcessingOperation() {
        var recentDate = LocalDateTime.now().minusMinutes(2);

        processedOperationRepository.insertProcessing(
                OPERATION_ID,
                OPERATION_TYPE,
                REQUEST_HASH,
                recentDate
        );

        var staleBefore = LocalDateTime.now().minusMinutes(5);

        int rowsDeleted = processedOperationRepository.deleteStaleProcessing(OPERATION_ID, staleBefore);

        assertThat(rowsDeleted).isEqualTo(0);

        entityManager.clear();

        assertThat(processedOperationRepository.findById(OPERATION_ID)).isPresent();
    }

    // endregion
}