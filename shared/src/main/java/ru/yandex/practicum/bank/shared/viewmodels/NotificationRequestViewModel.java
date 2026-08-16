package ru.yandex.practicum.bank.shared.viewmodels;

/**
 * <summary>
 * Модель представления (ViewModel) запроса на отправку уведомления в сервисе переводов (Transfer Service).
 * Используется при межсервисном обращении к сервису уведомлений (Notification Service).
 * </summary>
 * @param recipientLogin Уникальный логин пользователя-получателя уведомления.
 * @param type Тип или событие отправляемого уведомления (например, TRANSFER).
 * @param message Текстовое содержимое сообщения уведомления.
 * @param operationId Уникальный идентификатор операции для обеспечения идемпотентности.
 **/
public record NotificationRequestViewModel (
        String recipientLogin,
        String type,
        String message,
        String operationId
) {
}