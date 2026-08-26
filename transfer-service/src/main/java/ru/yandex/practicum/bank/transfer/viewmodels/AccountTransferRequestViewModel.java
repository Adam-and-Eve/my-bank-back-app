package ru.yandex.practicum.bank.transfer.viewmodels;

import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель представления (ViewModel) запроса на проведение перевода между счетами для обращения к сервису счетов (Accounts Service).
 * </summary>
 * @param senderLogin Уникальный логин пользователя-отправителя перевода.
 * @param recipientLogin Уникальный логин пользователя-получателя перевода.
 * @param amount Сумма перевода.
 * @param currency Валюта выполнения операции.
 * @param operationId Уникальный идентификатор операции для обеспечения идемпотентности.
 **/
public record AccountTransferRequestViewModel(
        String senderLogin,
        String recipientLogin,
        BigDecimal amount,
        CurrencyEnumModel currency,
        BigDecimal recipientAmount,
        CurrencyEnumModel recipientCurrency,
        String operationId
) {
}