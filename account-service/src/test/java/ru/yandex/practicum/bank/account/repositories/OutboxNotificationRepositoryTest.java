package ru.yandex.practicum.bank.account.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.practicum.bank.account.models.OutboxNotificationModel;
import ru.yandex.practicum.bank.account.models.OutboxStatusEnumModel;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Тесты уровня данных (Data JPA) для репозитория OutboxNotificationRepository.
 * Проверяют корректность выполнения выборки сообщений из Outbox:
 * фильтрацию по статусу PENDING, ограничение по времени nextAttemptAt,
 * лимитирование размера порции (batch size) и правильную сортировку по дате создания.
 * </summary>
 **/
@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class OutboxNotificationRepositoryTest {

    // region Constants

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 27, 12, 0);

    // endregion

    // region Fields

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OutboxNotificationRepository outboxRepository;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что запрос находит только те уведомления, которые находятся в статусе PENDING
     * и время попытки отправки которых меньше либо равно указанному времени.
     * </summary>
     **/
    @Test
    public void shouldFindPendingNotificationsReadyForProcessing() {
        var readyNotification = createNotification(UUID.randomUUID(), NOW.minusMinutes(5), NOW.minusMinutes(1), OutboxStatusEnumModel.PENDING);

        var futureNotification = createNotification(UUID.randomUUID(), NOW.minusMinutes(5), NOW.plusMinutes(10), OutboxStatusEnumModel.PENDING);

        var sentNotification = createNotification(UUID.randomUUID(), NOW.minusMinutes(5), NOW.minusMinutes(2), OutboxStatusEnumModel.SENT);

        entityManager.persist(readyNotification);

        entityManager.persist(futureNotification);

        entityManager.persist(sentNotification);

        entityManager.flush();

        var pending = outboxRepository.findPendingNotifications(NOW, 10);

        assertThat(pending)
                .hasSize(1)
                .extracting(OutboxNotificationModel::getId)
                .containsExactly(readyNotification.getId());
    }

    /**
     * <summary>
     * Проверяет, что результат выборки ограничивается переданным значением batchSize.
     * </summary>
     **/
    @Test
    public void shouldRespectBatchSizeLimit() {
        var first = createNotification(UUID.randomUUID(), NOW.minusMinutes(5), NOW, OutboxStatusEnumModel.PENDING);

        var second = createNotification(UUID.randomUUID(), NOW.minusMinutes(4), NOW, OutboxStatusEnumModel.PENDING);

        var third = createNotification(UUID.randomUUID(), NOW.minusMinutes(3), NOW, OutboxStatusEnumModel.PENDING);

        entityManager.persist(first);

        entityManager.persist(second);

        entityManager.persist(third);

        entityManager.flush();

        var pending = outboxRepository.findPendingNotifications(NOW, 2);

        assertThat(pending).hasSize(2);
    }

    /**
     * <summary>
     * Проверяет, что результаты сортируются по времени создания (createdAt) по возрастанию,
     * чтобы старые сообщения обрабатывались первыми.
     * </summary>
     **/
    @Test
    public void shouldOrderResultsByCreatedAtAscending() {
        var newest = createNotification(UUID.randomUUID(), NOW.minusMinutes(1), NOW, OutboxStatusEnumModel.PENDING);

        var oldest = createNotification(UUID.randomUUID(), NOW.minusMinutes(10), NOW, OutboxStatusEnumModel.PENDING);

        var middle = createNotification(UUID.randomUUID(), NOW.minusMinutes(5), NOW, OutboxStatusEnumModel.PENDING);

        entityManager.persist(newest);

        entityManager.persist(oldest);

        entityManager.persist(middle);

        entityManager.flush();

        var pending = outboxRepository.findPendingNotifications(NOW, 10);

        assertThat(pending)
                .hasSize(3)
                .extracting(OutboxNotificationModel::getId)
                .containsExactly(oldest.getId(), middle.getId(), newest.getId());
    }

    /**
     * <summary>
     * Проверяет возврат пустого списка, если в БД нет сообщений, удовлетворяющих условиям
     * (все сообщения либо отправлены, либо ожидают будущей попытки, либо завершились с ошибкой).
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyListWhenNoNotificationsMatch() {
        var sent = createNotification(UUID.randomUUID(), NOW.minusMinutes(5), NOW.minusMinutes(1), OutboxStatusEnumModel.SENT);

        var failed = createNotification(UUID.randomUUID(), NOW.minusMinutes(5), NOW.minusMinutes(1), OutboxStatusEnumModel.FAILED);

        var future = createNotification(UUID.randomUUID(), NOW.minusMinutes(5), NOW.plusMinutes(1), OutboxStatusEnumModel.PENDING);

        entityManager.persist(sent);

        entityManager.persist(failed);

        entityManager.persist(future);

        entityManager.flush();

        var pending = outboxRepository.findPendingNotifications(NOW, 10);

        assertThat(pending).isEmpty();
    }

    // endregion

    // region Helper Methods

    /**
     * <summary>
     * Вспомогательный метод для создания объекта уведомления с заданными параметрами времени и статуса
     * в обход стандартного конструктора.
     * </summary>
     **/
    private OutboxNotificationModel createNotification(
            UUID id,
            LocalDateTime createdAt,
            LocalDateTime nextAttemptAt,
            OutboxStatusEnumModel status
    ) {
        var notification = new OutboxNotificationModel(
                id,
                UUID.randomUUID(),
                "op-test-123",
                "{\"message\":\"test\"}",
                createdAt
        );

        ReflectionTestUtils.setField(notification, "createdAt", createdAt);

        ReflectionTestUtils.setField(notification, "nextAttemptAt", nextAttemptAt);

        ReflectionTestUtils.setField(notification, "status", status);

        return notification;
    }

    // endregion
}