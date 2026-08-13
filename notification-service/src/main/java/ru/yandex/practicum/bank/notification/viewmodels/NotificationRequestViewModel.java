package ru.yandex.practicum.bank.notification.viewmodels;

import jakarta.validation.constraints.NotBlank;

/**
 * <summary>
 * Модель представления (ViewModel) запроса на создание и отправку уведомления.
 * </summary>
 * @param recipientLogin Уникальный логин пользователя-получателя уведомления.
 * @param type Тип или канал отправки уведомления (например, EMAIL, SMS, PUSH).
 * @param message Текстовое содержимое отправляемого уведомления.
 * @param operationId Уникальный идентификатор операции для обеспечения идемпотентности обработки.
 **/
public record NotificationRequestViewModel (
        @NotBlank
        String recipientLogin,

        @NotBlank
        String type,

        @NotBlank
        String message,

        @NotBlank
        String operationId
) {
}