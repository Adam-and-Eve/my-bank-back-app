package ru.yandex.practicum.bank.transfer.viewmodels;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель представления (ViewModel) ответа сервиса счетов (Accounts Service) на проведение перевода.
 * </summary>
 * @param senderLogin Уникальный логин пользователя-отправителя перевода.
 * @param recipientLogin Уникальный логин пользователя-получателя перевода.
 * @param senderBalance Обновленный баланс счета отправителя после успешного перевода.
 * @param currency Валюта выполнения операции.
 **/
public record AccountTransferResponseViewModel(
        String senderLogin,
        String recipientLogin,
        BigDecimal senderBalance,
        String currency
) {
}