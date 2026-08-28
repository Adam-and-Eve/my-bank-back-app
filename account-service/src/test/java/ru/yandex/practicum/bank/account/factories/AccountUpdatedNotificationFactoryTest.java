package ru.yandex.practicum.bank.account.factories;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.bank.account.viewmodels.AccountProfileUpdatedEventViewModel;
import ru.yandex.practicum.bank.shared.models.NotificationSourceEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Юнит-тесты для фабрики создания уведомлений об обновлении профиля (AccountUpdatedNotificationFactory).
 * Проверяют корректность маппинга полей из доменного события в модель NotificationEventModel
 * как при перегрузке с одним аргументом, так и при явном указании параметров.
 * </summary>
 **/
public class AccountUpdatedNotificationFactoryTest {

    // region Fields

    private final AccountUpdatedNotificationFactory notificationFactory = new AccountUpdatedNotificationFactory();

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет корректное создание NotificationEventModel на основе модели события AccountProfileUpdatedEventViewModel.
     * Убеждается, что все основные поля проставляются правильно, а текст уведомления соответствует константе.
     * </summary>
     **/
    @Test
    public void shouldCreateNotificationFromEventViewModel() {
        var operationId = UUID.randomUUID();

        var recipientLogin = "dmitry";

        var occurredAt = Instant.now();

        var event = new AccountProfileUpdatedEventViewModel(
                operationId,
                recipientLogin,
                occurredAt
        );

        var notification = notificationFactory.create(event);

        assertThat(notification).isNotNull();

        assertThat(notification.eventId()).isNotNull();

        assertThat(notification.operationId()).isEqualTo(operationId);

        assertThat(notification.source()).isEqualTo(NotificationSourceEnumModel.ACCOUNT);

        assertThat(notification.type()).isEqualTo(NotificationTypeEnumModel.ACCOUNT_UPDATED);

        assertThat(notification.recipientLogin()).isEqualTo(recipientLogin);

        assertThat(notification.message()).isEqualTo(AccountUpdatedNotificationFactory.NOTIFICATION_MESSAGE);

        assertThat(notification.occurredAt()).isEqualTo(occurredAt);

        assertThat(notification.amount()).isNull();

        assertThat(notification.currency()).isNull();
    }

    /**
     * <summary>
     * Проверяет корректное создание NotificationEventModel при вызове перегруженного метода
     * с явной передачей всех параметров (eventId, operationId, recipientLogin, occurredAt).
     * </summary>
     **/
    @Test
    public void shouldCreateNotificationWithExplicitParameters() {
        var eventId = UUID.randomUUID();

        var operationId = UUID.randomUUID();

        var recipientLogin = "alexey";

        var occurredAt = Instant.now();

        var notification = notificationFactory.create(
                eventId,
                operationId,
                recipientLogin,
                occurredAt
        );

        assertThat(notification).isNotNull();

        assertThat(notification.eventId()).isEqualTo(eventId);

        assertThat(notification.operationId()).isEqualTo(operationId);

        assertThat(notification.source()).isEqualTo(NotificationSourceEnumModel.ACCOUNT);

        assertThat(notification.type()).isEqualTo(NotificationTypeEnumModel.ACCOUNT_UPDATED);

        assertThat(notification.recipientLogin()).isEqualTo(recipientLogin);

        assertThat(notification.message()).isEqualTo(AccountUpdatedNotificationFactory.NOTIFICATION_MESSAGE);
        assertThat(notification.occurredAt()).isEqualTo(occurredAt);

        assertThat(notification.amount()).isNull();

        assertThat(notification.currency()).isNull();
    }

    // endregion
}