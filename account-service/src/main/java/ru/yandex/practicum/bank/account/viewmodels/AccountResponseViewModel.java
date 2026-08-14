package ru.yandex.practicum.bank.account.viewmodels;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <summary>
 * Модель ответа с данными аккаунта пользователя (AccountResponseViewModel).
 * Содержит профильную информацию о пользователе и его текущий баланс средств.
 * </summary>
 * @param login Логин пользователя.
 * @param name Имя пользователя.
 * @param birthdate Дата рождения пользователя.
 * @param balance Текущий баланс аккаунта (сериализуется в виде строки для сохранения точности).
 * @param currency Трехбуквенный код валюты аккаунта (например, "RUB").
 **/
public record AccountResponseViewModel (
        String login,
        String name,
        LocalDate birthdate,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal balance,
        String currency
) {
}