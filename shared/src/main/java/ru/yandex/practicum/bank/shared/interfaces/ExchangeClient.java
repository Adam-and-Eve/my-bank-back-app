package ru.yandex.practicum.bank.shared.interfaces;

import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;

import java.math.BigDecimal;

public interface ExchangeClient {

    // region Methods

    public ConversionResponseViewModel convert(
            CurrencyEnumModel sourceCurrency,
            CurrencyEnumModel targetCurrency,
            BigDecimal amount);

    // endregion
}