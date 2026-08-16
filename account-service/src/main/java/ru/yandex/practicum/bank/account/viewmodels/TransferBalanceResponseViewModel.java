package ru.yandex.practicum.bank.account.viewmodels;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель ответа на запрос перевода средств между аккаунтами (TransferBalanceResponseViewModel).
 * Содержит информацию об отправителе, получателе, обновлённом балансе отправителя и валюте операции.
 * </summary>
 * @param senderLogin Логин отправителя перевода.
 * @param recipientLogin Логин получателя перевода.
 * @param senderBalance Актуальный остаток средств на счёте отправителя после совершения перевода (сериализуется в виде строки для сохранения точности).
 * @param currency Трехбуквенный код валюты операции (например, "RUB").
 **/
public record TransferBalanceResponseViewModel (
        String senderLogin,
        String recipientLogin,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal senderBalance,

        String currency
) {
}