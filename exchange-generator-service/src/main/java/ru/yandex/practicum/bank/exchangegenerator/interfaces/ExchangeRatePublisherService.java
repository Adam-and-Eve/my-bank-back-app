package ru.yandex.practicum.bank.exchangegenerator.interfaces;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * <summary>
 * Контракт сервиса для периодической публикации сгенерированных курсов валют в сервис курсов валют.
 * </summary>
 **/
public interface ExchangeRatePublisherService {

    // region Methods

    /**
     * <summary>
     * Генерирует следующий набор курсов валют и публикует его в сервис курсов валют.
     * Метод автоматически вызывается с фиксированной задержкой между запусками.
     * </summary>
     **/
    public void publishNextRates();

    // endregion
}