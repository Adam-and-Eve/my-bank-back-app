package ru.yandex.practicum.bank.frontui.viewmodels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

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
        String currency,

        @NotBlank
        String idempotencyKey
) {
        public CashFormViewModel(BigDecimal amount, String currency) {
                this(amount, currency, UUID.randomUUID().toString());
        }
}