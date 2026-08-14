package ru.yandex.practicum.bank.frontui.viewmodels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель формы для выполнения перевода денежных средств.
 * Содержит логин получателя, сумму перевода и валюту операции
 * с проверкой обязательности заполнения полей.
 * </summary>
 **/
public record TransferFormViewModel (
        @NotBlank
        String recipientLogin,

        @NotNull
        BigDecimal amount,

        @NotBlank
        String currency
) {
}