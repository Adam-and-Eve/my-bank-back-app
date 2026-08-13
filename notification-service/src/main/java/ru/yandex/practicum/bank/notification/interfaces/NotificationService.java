package ru.yandex.practicum.bank.notification.interfaces;

import ru.yandex.practicum.bank.notification.viewmodels.NotificationRequestViewModel;

/**
 * <summary>
 * Контракт сервиса управления и отправки уведомлений.
 * </summary>
 **/
public interface NotificationService {

    // region Methods

    /**
     * <summary>
     * Принимает запрос на отправку уведомления и фиксирует его в логах системы.
     * </summary>
     * @param request Модель данных с параметрами отправляемого уведомления.
     **/
    public void notify(NotificationRequestViewModel request);

    // endregion
}