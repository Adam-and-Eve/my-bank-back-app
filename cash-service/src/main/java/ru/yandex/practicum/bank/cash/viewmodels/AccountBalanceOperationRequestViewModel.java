package ru.yandex.practicum.bank.cash.viewmodels;

import ru.yandex.practicum.bank.cash.models.CurrencyEnumModel;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель запроса для операции изменения баланса в сервисе счетов (Accounts Service).
 * Передается при межсервисном взаимодействии для проведения операций пополнения или списания средств.
 * </summary>
 * @param login Логин пользователя, владеющего счетом.
 * @param amount Сумма операции для изменения баланса.
 * @param currency Валюта операции из списка поддерживаемых CurrencyEnumModel.
 * @param operationId Уникальный идентификатор операции для обеспечения идемпотентности.
 **/
public record AccountBalanceOperationRequestViewModel(
        String login,
        BigDecimal amount,
        CurrencyEnumModel currency,
        String operationId
) {
}