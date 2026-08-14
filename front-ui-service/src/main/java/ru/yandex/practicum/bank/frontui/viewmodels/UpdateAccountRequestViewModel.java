package ru.yandex.practicum.bank.frontui.viewmodels;

import java.time.LocalDate;

/**
 * <summary>
 * Модель запроса для обновления данных пользовательского аккаунта.
 * Содержит имя пользователя и дату рождения.
 * </summary>
 **/
public record UpdateAccountRequestViewModel (
        String name,
        LocalDate birthdate
) {
}