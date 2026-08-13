package ru.yandex.practicum.bank.cash.viewmodels;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель представления ответа после выполнения операции с наличностью (DTO).
 * Содержит обновленный баланс счета, наименование валюты и информационное сообщение о статусе операции.
 * </summary>
 * @param balance Обновленный баланс счета после проведения операции. Форматируется как строка в JSON для сохранения точности.
 * @param currency Строковый код валюты проведенной операции.
 * @param message Информационное сообщение о результатах выполнения операции.
 **/
public record CashOperationResponseViewModel (
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal balance,
        String currency,
        String message
) {
}