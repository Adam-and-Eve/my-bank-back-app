package ru.yandex.practicum.bank.transfer.interfaces;

import ru.yandex.practicum.bank.transfer.viewmodels.NotificationRequestViewModel;

/**
 * <summary>
 * Контракт клиента для взаимодействия с сервисом уведомлений (Notification Service).
 * </summary>
 **/
public interface NotificationClient {

    // region Methods

    /**
     * <summary>
     * Отправляет запрос на создание и доставку уведомления пользователю.
     * </summary>
     * @param request Данные запроса на отправку уведомления.
     **/
    public void notify(NotificationRequestViewModel request);

    // endregion
}