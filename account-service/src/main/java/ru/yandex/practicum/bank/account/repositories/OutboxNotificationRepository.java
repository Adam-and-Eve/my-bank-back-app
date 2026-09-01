package ru.yandex.practicum.bank.account.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.bank.account.models.OutboxNotificationModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * <summary>
 * Репозиторий Spring Data JPA для работы с событиями Outbox (OutboxNotificationModel).
 * </summary>
 **/
@Repository
public interface OutboxNotificationRepository extends JpaRepository<OutboxNotificationModel, UUID> {

    // region Methods

    /**
     * <summary>
     * Находит порцию ожидающих отправки сообщений, время которых уже наступило.
     * Выборка сортируется по времени создания, чтобы сохранять порядок сообщений.
     * </summary>
     * @param now Текущее время (для выборки отложенных повторов).
     * @param batchSize Максимальное количество сообщений в порции.
     * @return Список ожидающих сообщений.
     */
    @Query("""
            SELECT o FROM OutboxNotificationModel o 
            WHERE o.status = ru.yandex.practicum.bank.account.models.OutboxStatusEnumModel.PENDING 
              AND o.nextAttemptAt <= :now 
            ORDER BY o.createdAt ASC 
            LIMIT :batchSize
            """)
    public List<OutboxNotificationModel> findPendingNotifications(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );

    // endregion
}