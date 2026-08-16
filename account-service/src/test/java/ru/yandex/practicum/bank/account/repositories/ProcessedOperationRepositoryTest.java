package ru.yandex.practicum.bank.account.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Тесты уровня данных (Data JPA) для репозитория ProcessedOperationRepository.
 * Проверяют корректность выполнения нативного SQL-запроса атомарной регистрации
 * новой операции со статусом PROCESSING и обработку уникальности ключей.
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
     * Проверяет, что вернётся 1 изменённая строка, и данные корректно сохранятся в БД.
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

        org.junit.jupiter.api.Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> processedOperationRepository.insertProcessing(
                        OPERATION_ID,
                        OPERATION_TYPE,
                        REQUEST_HASH,
                        now
                )
        );
    }

    // endregion
}