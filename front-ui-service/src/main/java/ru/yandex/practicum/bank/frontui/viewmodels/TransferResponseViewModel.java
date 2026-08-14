package ru.yandex.practicum.bank.frontui.viewmodels;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель ответа на выполнение перевода денежных средств.
 * Содержит логин отправителя, логин получателя, обновлённый баланс отправителя,
 * валюту операции и сообщение с результатом перевода.
 * </summary>
 **/
public record TransferResponseViewModel (
        String senderLogin,
        String recipientLogin,
        BigDecimal senderBalance,
        String currency,
        String message
) {
}