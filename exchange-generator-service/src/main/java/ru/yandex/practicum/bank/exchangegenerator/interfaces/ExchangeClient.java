package ru.yandex.practicum.bank.exchangegenerator.interfaces;

import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

/**
 * <summary>
 * Контракт HTTP-клиента для взаимодействия с сервисом курсов валют (Exchange Service).
 * </summary>
 **/
public interface ExchangeClient {

    // region Methods

    /**
     * <summary>
     * Отправляет запрос на обновление курсов валют с использованием паттерна Circuit Breaker.
     * Сбои при обновлении курсов мягко глушатся в fallback.
     * </summary>
     * @param request Данные запроса на обновление курсов валют.
     **/
    public void updateRates(ExchangeRatesUpdateRequestViewModel request);

    // endregion
}