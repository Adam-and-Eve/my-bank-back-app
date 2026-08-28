package ru.yandex.practicum.bank.account.factories;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.account.viewmodels.AccountProfileUpdatedEventViewModel;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationSourceEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;

import java.time.Instant;
import java.util.UUID;

/**
 * <summary>
 * Фабрика для создания событий-уведомлений об успешном обновлении данных профиля пользователя.
 * Формирует объекты {@link NotificationEventModel}, которые в дальнейшем отправляются
 * в брокер сообщений (Kafka) для доставки клиенту.
 * </summary>
 **/
@Component
public class AccountUpdatedNotificationFactory {

    // region Constants

    /**
     * <summary>
     * Текст уведомления об обновлении профиля по умолчанию.
     * </summary>
     **/
    public static final String NOTIFICATION_MESSAGE = "Данные профиля обновлены";

    // endregion

    // region Public Methods

    /**
     * <summary>
     * Создает событие-уведомление на основе готовой модели события (AccountProfileUpdatedEventViewModel).
     * Автоматически генерирует новый уникальный идентификатор (UUID) для уведомления.
     * </summary>
     * @param event Модель события обновления профиля аккаунта.
     * @return Сформированная модель уведомления.
     **/
    public NotificationEventModel create(AccountProfileUpdatedEventViewModel event) {
        return create(UUID.randomUUID(), event.operationId(), event.recipientLogin(), event.occurredAt());
    }

    /**
     * <summary>
     * Создает детализированное событие-уведомление с явным указанием параметров.
     * Жестко привязывает источник уведомления (ACCOUNT) и его тип (ACCOUNT_UPDATED).
     * </summary>
     * @param eventId Уникальный идентификатор самого события-уведомления.
     * @param operationId Идентификатор операции (запроса), в рамках которой профиль был обновлен.
     * @param recipientLogin Логин пользователя (получателя), которому адресовано уведомление.
     * @param occurredAt Временная метка совершения операции.
     * @return Сформированная модель уведомления.
     **/
    public NotificationEventModel create(
            UUID eventId,
            UUID operationId,
            String recipientLogin,
            Instant occurredAt
    ) {
        return new NotificationEventModel(
                eventId,
                operationId,
                NotificationSourceEnumModel.ACCOUNT,
                NotificationTypeEnumModel.ACCOUNT_UPDATED,
                recipientLogin,
                NOTIFICATION_MESSAGE,
                occurredAt,
                null,
                null
        );
    }

    // endregion
}