package ru.yandex.practicum.bank.notification.viewmodels;

/**
 * <summary>
 * Модель представления (ViewModel) для передачи стандартизированной информации об ошибках API.
 * </summary>
 * @param code Уникальный строковый код ошибки (например, VALIDATION_ERROR, UNAUTHORIZED, FORBIDDEN).
 * @param message Понятное человеку описание ошибки или детальное сообщение о причине сбоя.
 **/
public record ApiErrorResponseViewModel (
        String code,
        String message
) {
}