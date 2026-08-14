package ru.yandex.practicum.bank.frontui.viewmodels;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * <summary>
 * Модель формы для обновления данных пользовательского аккаунта.
 * Содержит имя пользователя и дату рождения с валидацией обязательности
 * заполнения и допустимой длины имени.
 * </summary>
 **/
public record AccountFormViewModel (
        @NotNull
        @Size(min = 2, max = 120)
        String name,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate birthdate
) {
}