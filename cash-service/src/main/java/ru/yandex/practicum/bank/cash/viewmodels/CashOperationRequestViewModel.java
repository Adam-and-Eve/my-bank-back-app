package ru.yandex.practicum.bank.cash.viewmodels;

import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.bank.cash.models.CurrencyEnumModel;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель запроса на выполнение операции с наличностью (пополнение или снятие средств).
 * Содержит сумму и валюту проводимой финансовой операции.
 * </summary>
 * @param amount Сумма операции. Не может быть null.
 * @param currency Валюта операции из списка поддерживаемых CurrencyEnumModel. Не может быть null.
 **/
public record CashOperationRequestViewModel (
        @NotNull
        BigDecimal amount,

        @NotNull
        CurrencyEnumModel currency
) {
}