package ru.yandex.practicum.bank.account.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.bank.account.models.ProcessedOperationModel;
import ru.yandex.practicum.bank.account.models.ProcessedOperationStatusEnumModel;

import java.time.LocalDateTime;

/**
 * <summary>
 * Репозиторий для управления записями об обработанных операциях.
 * Используется для обеспечения идемпотентности запросов: регистрации начатых
 * транзакций, предотвращения дублирования и очистки зависших (stale) операций.
 * </summary>
 **/
@Repository
public interface ProcessedOperationRepository extends JpaRepository<ProcessedOperationModel, String> {

    // region Methods

    /**
     * <summary>
     * Выполняет атомарную вставку (INSERT) первичной записи об операции со статусом PROCESSING.
     * Нативный запрос предотвращает лишний вызов SELECT перед вставкой (в отличие от стандартного save).
     * </summary>
     * @param operationId Уникальный идентификатор операции (ключ идемпотентности).
     * @param operationType Тип выполняемой операции.
     * @param requestHash Хеш тела или параметров запроса.
     * @param now Временная метка создания и обновления записи.
     * @return Количество вставленных строк (1 при успешной регистрации).
     **/
    @Modifying
    @Query(value = """
            INSERT INTO processed_operations (operation_id, operation_type, request_hash, status, created_at, updated_at)
            VALUES (:operationId, :operationType, :requestHash, 'PROCESSING', :now, :now)
            """, nativeQuery = true)
    public int insertProcessing(
            @Param("operationId") String operationId,
            @Param("operationType") String operationType,
            @Param("requestHash") String requestHash,
            @Param("now") LocalDateTime now
    );

    /**
     * <summary>
     * Удаляет запись об операции, если она зависла в статусе PROCESSING (stale).
     * Позволяет высвободить ключ идемпотентности для повторного выполнения операции,
     * если предыдущая попытка завершилась критическим сбоем (например, падением пода)
     * без финального обновления статуса.
     * </summary>
     * @param operationId Уникальный идентификатор зависшей операции.
     * @param staleBefore Временная метка (порог). Записи, не обновлявшиеся дольше этого времени, считаются зависшими.
     * @return Количество удаленных строк (1, если зависшая операция была успешно очищена).
     **/
    @Modifying
    @Query("""
            DELETE FROM ProcessedOperationModel operation
            WHERE operation.operationId = :operationId
              AND operation.status = ProcessedOperationStatusEnumModel.PROCESSING
              AND operation.updatedAt <= :staleBefore
            """)
    public int deleteStaleProcessing(
            @Param("operationId") String operationId,
            @Param("staleBefore") LocalDateTime staleBefore
    );

    // endregion
}