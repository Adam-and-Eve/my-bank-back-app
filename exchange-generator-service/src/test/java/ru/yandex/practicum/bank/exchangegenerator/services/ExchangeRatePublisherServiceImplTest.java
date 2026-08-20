package ru.yandex.practicum.bank.exchangegenerator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.bank.exchangegenerator.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.exchangegenerator.interfaces.ExchangeRateGeneratorService;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Unit-тесты сервиса периодической публикации курсов валют.
 * </summary>
 **/
public class ExchangeRatePublisherServiceImplTest {

    // region Fields

    /**
     * <summary>
     * Mock сервиса генерации курсов валют.
     * </summary>
     **/
    private ExchangeRateGeneratorService exchangeRateGenerator;

    /**
     * <summary>
     * Mock клиента для публикации курсов валют в Exchange Service.
     * </summary>
     **/
    private ExchangeClient exchangeClient;

    /**
     * <summary>
     * Тестируемый сервис публикации курсов валют.
     * </summary>
     **/
    private ExchangeRatePublisherServiceImpl service;

    // endregion

    // region Setup

    /**
     * <summary>
     * Инициализирует зависимости и создает экземпляр тестируемого сервиса.
     * </summary>
     **/
    @BeforeEach
    void setUp() {
        exchangeRateGenerator = mock(ExchangeRateGeneratorService.class);

        exchangeClient = mock(ExchangeClient.class);

        service = new ExchangeRatePublisherServiceImpl(
                exchangeRateGenerator,
                exchangeClient
        );
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет генерацию следующего набора курсов и его публикацию через Exchange Client.
     * </summary>
     **/
    @Test
    void publishNextRates_shouldGenerateAndPublishRates() {
        var rates = mock(ExchangeRatesUpdateRequestViewModel.class);

        when(exchangeRateGenerator.nextRates()).thenReturn(rates);

        service.publishNextRates();

        verify(exchangeRateGenerator).nextRates();

        verify(exchangeClient).updateRates(rates);
    }

    /**
     * <summary>
     * Проверяет, что при каждом вызове метода публикуется актуальный набор
     * курсов, полученный от генератора.
     * </summary>
     **/
    @Test
    void publishNextRates_shouldPublishRatesReturnedByGenerator() {
        var firstRates = mock(ExchangeRatesUpdateRequestViewModel.class);

        var secondRates = mock(ExchangeRatesUpdateRequestViewModel.class);

        when(exchangeRateGenerator.nextRates())
                .thenReturn(firstRates)
                .thenReturn(secondRates);

        service.publishNextRates();

        service.publishNextRates();

        verify(exchangeClient).updateRates(firstRates);

        verify(exchangeClient).updateRates(secondRates);
    }

    // endregion
}
