package ru.yandex.practicum.bank.frontui.viewmodels;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <summary>
 * Модель ответа с данными пользовательского аккаунта.
 * Содержит логин пользователя, имя, дату рождения, текущий баланс
 * и валюту счёта.
 * </summary>
 **/
public record AccountResponseViewModel (
        String login,
        String name,
        LocalDate birthdate,
        BigDecimal balance,
        String currency
) {
}