package ru.yandex.practicum.bank.account.viewmodels;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * <summary>
 * Модель запроса для обновления личных данных аккаунта (UpdateAccountRequestViewModel).
 * Содержит передаваемые пользователем данные для изменения профиля.
 * </summary>
 * @param name Имя пользователя (длина от 2 до 120 символов).
 * @param birthdate Дата рождения пользователя.
 **/
public record UpdateAccountRequestViewModel (
        @NotNull
        @Size(min = 2, max = 120)
        String name,

        @NotNull
        LocalDate birthdate
) {
}