package ru.yandex.practicum.bank.transfer.viewmodels;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель представления (ViewModel) результатов выполненной операции перевода.
 * </summary>
 * @param senderLogin Уникальный логин пользователя-отправителя перевода.
 * @param recipientLogin Уникальный логин пользователя-получателя перевода.
 * @param senderBalance Обновленный баланс счета отправителя после выполнения перевода.
 * @param currency Валюта выполненной операции.
 **/
public record TransferResultViewModel (
        String senderLogin,
        String recipientLogin,
        BigDecimal senderBalance,
        String currency
){
}