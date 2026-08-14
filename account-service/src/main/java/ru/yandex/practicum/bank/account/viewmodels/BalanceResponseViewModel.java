package ru.yandex.practicum.bank.account.viewmodels;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель ответа с информацией об актуальном балансе аккаунта (BalanceResponseViewModel).
 * Возвращается при проведении межсервисных операций или запросе баланса.
 * </summary>
 * @param login Логин пользователя.
 * @param balance Текущий баланс аккаунта (сериализуется в виде строки для сохранения точности).
 * @param currency Трехбуквенный код валюты аккаунта (например, "RUB").
 **/
public record BalanceResponseViewModel (
        String login,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal balance,

        String currency
) {
}