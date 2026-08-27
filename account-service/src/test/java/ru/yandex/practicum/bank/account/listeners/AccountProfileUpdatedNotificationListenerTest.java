package ru.yandex.practicum.bank.account.listeners;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.account.factories.AccountUpdatedNotificationFactory;
import ru.yandex.practicum.bank.account.viewmodels.AccountProfileUpdatedEventViewModel;
import ru.yandex.practicum.bank.shared.interfaces.NotificationEventPublisher;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationSourceEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Юнит-тесты для слушателя событий обновления профиля (AccountProfileUpdatedNotificationListener).
 * Проверяют корректность вызова фабрики уведомлений и отправку полученного события через публикатор нотификаций.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class AccountProfileUpdatedNotificationListenerTest {

    // region Fields

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private AccountUpdatedNotificationFactory notificationFactory;

    @InjectMocks
    private AccountProfileUpdatedNotificationListener notificationListener;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что при получении события обновления профиля слушатель успешно
     * генерирует уведомление через фабрику и передает его в публикатор.
     * </summary>
     **/
    @Test
    public void shouldCreateAndPublishNotificationWhenProfileUpdated() {
        var event = new AccountProfileUpdatedEventViewModel(
                UUID.randomUUID(),
                "dmitry",
                Instant.now()
        );

        var notificationModel = new NotificationEventModel(
                UUID.randomUUID(),
                event.operationId(),
                NotificationSourceEnumModel.ACCOUNT,
                NotificationTypeEnumModel.ACCOUNT_UPDATED,
                event.recipientLogin(),
                "Профиль успешно обновлен",
                event.occurredAt(),
                null,
                null
        );

        when(notificationFactory.create(event)).thenReturn(notificationModel);

        notificationListener.onProfileUpdated(event);

        verify(notificationFactory).create(event);

        verify(notificationEventPublisher).publish(notificationModel);
    }

    // endregion
}