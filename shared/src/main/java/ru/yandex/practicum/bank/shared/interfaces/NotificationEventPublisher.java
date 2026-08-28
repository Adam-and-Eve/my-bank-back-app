package ru.yandex.practicum.bank.shared.interfaces;

import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

/**
 * <summary>
 * Контракт для публикации событий уведомлений (например, в брокер сообщений или шину данных).
 * </summary>
 */
public interface NotificationEventPublisher {

    // region Methods

    /**
     * <summary>
     * Публикует событие уведомления для его последующей асинхронной обработки и доставки клиенту.
     * </summary>
     * @param event Модель события уведомления NotificationEventModel.
     */
    void publish(NotificationEventModel event);

    // endregion
}