package ru.yandex.practicum.bank.cash.viewmodels;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель представления ответа с информацией о балансе счета из сервиса счетов (Accounts Service).
 * Используется для получения актуального состояния счета пользователя при межсервисном взаимодействии.
 * </summary>
 * @param login Логин пользователя, владельца счета.
 * @param balance Текущий остаток средств на счете.
 * @param currency Строковый код валюты счета.
 **/
public record AccountBalanceResponseViewModel(
        String login,
        BigDecimal balance,
        String currency
) {
}