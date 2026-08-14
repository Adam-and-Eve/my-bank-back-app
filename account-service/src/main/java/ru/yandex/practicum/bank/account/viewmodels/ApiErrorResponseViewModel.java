package ru.yandex.practicum.bank.account.viewmodels;

/**
 * <summary>
 * Модель ответа с информацией об ошибке API (ApiErrorResponseViewModel).
 * Содержит строковый код ошибки и понятное текстовое сообщение для клиента.
 * </summary>
 * @param code Уникальный код или тип возникшей ошибки.
 * @param message Описание ошибки с подробностями.
 **/
public record ApiErrorResponseViewModel (
        String code,
        String message
) {
}