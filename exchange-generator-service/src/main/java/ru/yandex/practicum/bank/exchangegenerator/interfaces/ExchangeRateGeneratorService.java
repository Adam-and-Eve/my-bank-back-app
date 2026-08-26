package ru.yandex.practicum.bank.exchangegenerator.interfaces;

import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

/**
 * <summary>
 * Контракт сервиса для генерации последовательности курсов валют.
 * </summary>
 **/
public interface ExchangeRateGeneratorService {

    // region Methods

    /**
     * <summary>
     * Возвращает следующий набор курсов валют из заданной последовательности.
     * После достижения конца последовательности начинает использовать значения с начала.
     * Метод синхронизирован для безопасного получения курсов при параллельных вызовах.
     * </summary>
     * @return Модель запроса с обновленными курсами RUB, USD и CNY.
     **/
    public ExchangeRatesUpdateRequestViewModel nextRates();

    // endregion
}