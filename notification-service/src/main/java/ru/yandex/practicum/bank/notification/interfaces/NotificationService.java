package ru.yandex.practicum.bank.notification.interfaces;

import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

/**
 * <summary>
 * Контракт сервиса управления и отправки уведомлений.
 * Определяет основную бизнес-логику обработки входящих событий нотификации.
 * </summary>
 **/
public interface NotificationService {

    // region Methods

    /**
     * <summary>
     * Принимает запрос (событие) на отправку уведомления и фиксирует его в логах системы.
     * </summary>
     * @param event Модель события уведомления, содержащая информацию об операции, получателе и тексте сообщения.
     **/
    void notify(NotificationEventModel event);

    // endregion
}