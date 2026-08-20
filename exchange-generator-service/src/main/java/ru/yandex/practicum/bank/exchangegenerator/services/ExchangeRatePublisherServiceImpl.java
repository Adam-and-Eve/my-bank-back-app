package ru.yandex.practicum.bank.exchangegenerator.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.exchangegenerator.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.exchangegenerator.interfaces.ExchangeRateGeneratorService;
import ru.yandex.practicum.bank.exchangegenerator.interfaces.ExchangeRatePublisherService;

/**
 * <summary>
 * Сервис для периодической публикации сгенерированных курсов валют в сервис курсов валют.
 * </summary>
 **/
@Service
public class ExchangeRatePublisherServiceImpl implements ExchangeRatePublisherService {

    // region Fields

    private final ExchangeRateGeneratorService exchangeRateGenerator;

    private final ExchangeClient exchangeClient;

    // endregion

    // region Constructors

    public ExchangeRatePublisherServiceImpl(
            ExchangeRateGeneratorService exchangeRateGenerator,
            ExchangeClient exchangeClient
    ) {
        this.exchangeRateGenerator = exchangeRateGenerator;
        this.exchangeClient = exchangeClient;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Генерирует следующий набор курсов валют и публикует его в сервис курсов валют.
     * Метод автоматически вызывается с фиксированной задержкой между запусками.
     * </summary>
     **/
    @Override
    @Scheduled(fixedDelayString = "${bank.services.exchange-generator-service.fixed-delay-ms:1000}")
    public void publishNextRates() {
        exchangeClient.updateRates(exchangeRateGenerator.nextRates());
    }

    // endregion
}