package ru.yandex.practicum.bank.cash.viewmodels;

/**
 * <summary>
 * Модель представления ответа с ошибкой API (DTO).
 * Содержит строковый код ошибки и понятное пользователю сообщение.
 * </summary>
 * @param code Уникальный код ошибки API.
 * @param message Описание ошибки.
 **/
public record ApiErrorResponseViewModel (
        String code,
        String message
) {
}