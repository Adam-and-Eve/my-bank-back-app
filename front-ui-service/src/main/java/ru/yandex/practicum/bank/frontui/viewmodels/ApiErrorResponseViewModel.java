package ru.yandex.practicum.bank.frontui.viewmodels;

/**
 * <summary>
 * Модель ответа с информацией об ошибке API.
 * Содержит код ошибки и сообщение с описанием причины возникновения ошибки.
 * </summary>
 **/
public record ApiErrorResponseViewModel (
        String code,
        String message
) {
}