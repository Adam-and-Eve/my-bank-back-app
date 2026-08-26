package ru.yandex.practicum.bank.shared.viewmodels;

import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель запроса на обновление курса обмена валюты (ExchangeRateUpdateRequestViewModel).
 * Содержит валюту и значения курса покупки и продажи.
 * </summary>
 * @param currency Валюта, для которой обновляется курс обмена.
 * @param buyRate Курс покупки валюты.
 * @param sellRate Курс продажи валюты.
 **/
public record ExchangeRateUpdateRequestViewModel (
        @NotNull
        CurrencyEnumModel currency,

        @NotNull
        BigDecimal buyRate,

        @NotNull
        BigDecimal sellRate
) {
}