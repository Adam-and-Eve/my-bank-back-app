package ru.yandex.practicum.bank.cash.viewmodels;

/**
 * <summary>
 * Модель запроса на отправку уведомления в сервис уведомлений (Notification Service).
 * Передается при межсервисном взаимодействии для информирования пользователя о проведенных операциях с наличностью.
 * </summary>
 * @param recipientLogin Логин пользователя, получателя уведомления.
 * @param type Тип отправляемого уведомления (например, CASH_DEPOSIT или CASH_WITHDRAW).
 * @param message Текст информационного сообщения для пользователя.
 * @param operationId Уникальный идентификатор операции для обеспечения идемпотентности и трейсинга.
 **/
public record NotificationRequestViewModel (
        String recipientLogin,
        String type,
        String message,
        String operationId
) {
}