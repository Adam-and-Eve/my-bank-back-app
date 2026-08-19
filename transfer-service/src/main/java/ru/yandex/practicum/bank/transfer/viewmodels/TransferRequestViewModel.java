package ru.yandex.practicum.bank.transfer.viewmodels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель представления (ViewModel) запроса на перевод денежных средств.
 * </summary>
 * @param recipientLogin Логин пользователя-получателя перевода.
 * @param amount Сумма перевода.
 * @param currency Валюта выполнения операции.
 **/
public record TransferRequestViewModel (
        @NotBlank
        String recipientLogin,

        @NotNull
        BigDecimal amount,

        @NotNull
        CurrencyEnumModel currency
) {
}