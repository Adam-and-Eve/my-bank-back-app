package ru.yandex.practicum.bank.gateway.viewmodels;

/**
 * <summary>
 * DTO-модель ответа об ошибке API (View Model).
 * Используется для передачи сведений об ошибках аутентификации, авторизации
 * и других сбоях клиентам API-шлюза в унифицированном JSON-формате.
 * </summary>
 * @param code Текстовый код ошибки (например, "UNAUTHORIZED", "FORBIDDEN").
 * @param message Человекочитаемое описание причины возникновения ошибки.
 **/
public record ApiErrorResponseViewModel (
        String code,
        String message
) {
}