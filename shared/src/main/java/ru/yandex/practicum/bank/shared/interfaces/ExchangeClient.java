package ru.yandex.practicum.bank.shared.interfaces;

import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

import java.math.BigDecimal;

public interface ExchangeClient {

    // region Methods

    public void updateRates(ExchangeRatesUpdateRequestViewModel request);

    // endregion
}