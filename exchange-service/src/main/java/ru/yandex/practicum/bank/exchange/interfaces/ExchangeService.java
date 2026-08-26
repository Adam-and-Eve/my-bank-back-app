package ru.yandex.practicum.bank.exchange.interfaces;

import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

import java.math.BigDecimal;
import java.util.List;

/**
 * <summary>
 * Контракт сервиса для получения и обновления курсов валют,
 * а также выполнения операций конвертации валют.
 * </summary>
 */
public interface ExchangeService {

    // region Methods

    /**
     * <summary>
     * Возвращает текущие курсы обмена всех поддерживаемых валют.
     * </summary>
     * @return Список текущих курсов обмена валют.
     */
    public List<ExchangeRateResponseViewModel> getRates();

    /**
     * <summary>
     * Обновляет курсы обмена указанных валют.
     * Для российского рубля курс всегда остается равным единице.
     * </summary>
     * @param request Запрос с данными для обновления курсов обмена валют.
     * @return Список актуальных курсов обмена всех поддерживаемых валют.
     */
    public List<ExchangeRateResponseViewModel> updateRates(
            ExchangeRatesUpdateRequestViewModel request);

    /**
     * <summary>
     * Конвертирует указанную сумму из одной валюты в другую по текущим курсам.
     * </summary>
     * @param sourceCurrency Исходная валюта.
     * @param targetCurrency Целевая валюта.
     * @param amount Сумма для конвертации.
     * @return Результат конвертации с примененным курсом.
     */
    public ConversionResponseViewModel convert(
            CurrencyEnumModel sourceCurrency,
            CurrencyEnumModel targetCurrency,
            BigDecimal amount);

    // endregion
}