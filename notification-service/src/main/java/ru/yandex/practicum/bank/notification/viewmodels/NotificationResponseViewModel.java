package ru.yandex.practicum.bank.notification.viewmodels;

/**
 * <summary>
 * Модель представления (ViewModel) ответа на запрос создания и отправки уведомления.
 * </summary>
 * @param status Статус обработки запроса на отправку уведомления (например, ACCEPTED).
 **/
public record NotificationResponseViewModel (
        String status
) {
}