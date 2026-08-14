package ru.yandex.practicum.bank.frontui.viewmodels;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель запроса для выполнения перевода денежных средств.
 * Содержит логин получателя, сумму перевода и валюту операции.
 * </summary>
 **/
public record TransferRequestViewModel (
        String recipientLogin,
        BigDecimal amount,
        String currency
) {
}