package ru.yandex.practicum.bank.frontui.viewmodels;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель формы для выполнения операции с наличными денежными средствами.
 * Содержит сумму операции и валюту счёта с проверкой обязательности
 * заполнения и положительного значения суммы.
 * </summary>
 **/
public record CashFormViewModel (
        @NotNull
        @Positive
        BigDecimal amount,

        @NotNull
        String currency
) {
}