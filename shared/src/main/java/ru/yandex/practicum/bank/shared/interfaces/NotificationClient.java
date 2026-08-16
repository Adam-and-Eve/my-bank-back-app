package ru.yandex.practicum.bank.shared.interfaces;

import ru.yandex.practicum.bank.shared.viewmodels.NotificationRequestViewModel;

/**
 * <summary>
 * Контракт клиента для взаимодействия с сервисом уведомлений (Notifications Service).
 * </summary>
 **/
public interface NotificationClient {

    // region Methods

    /**
     * <summary>
     * Отправляет запрос на создание и доставку уведомления пользователю.
     * </summary>
     * @param request Модель запроса на отправку уведомления NotificationRequestViewModel.
     **/
    public void notify(NotificationRequestViewModel request);

    // endregion
}