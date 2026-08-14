package ru.yandex.practicum.bank.frontui.viewmodels;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель запроса для выполнения операции с наличными денежными средствами.
 * Содержит сумму операции и валюту, в которой выполняется операция.
 * </summary>
 **/
public record CashOperationRequestViewModel (
        BigDecimal amount,
        String currency
) {
}