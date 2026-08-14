package ru.yandex.practicum.bank.frontui.viewmodels;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель ответа на операцию с наличными денежными средствами.
 * Содержит обновлённый баланс счёта, валюту счёта и сообщение
 * с результатом выполненной операции.
 * </summary>
 **/
public record CashOperationResponseViewModel (
        BigDecimal balance,
        String currency,
        String message
) {
}