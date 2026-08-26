package ru.yandex.practicum.bank.exchangegenerator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.bank.exchangegenerator.mappers.ExchangeGeneratorMapper;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * <summary>
 * Unit-тесты сервиса генерации последовательности курсов валют.
 * </summary>
 **/
public class ExchangeRateGeneratorServiceImplTest {

    // region Fields

    /**
     * <summary>
     * Маппер для преобразования данных курсов валют в модели запросов.
     * </summary>
     **/
    private ExchangeGeneratorMapper mapper;

    /**
     * <summary>
     * Тестируемый сервис генерации курсов валют.
     * </summary>
     **/
    private ExchangeRateGeneratorServiceImpl service;

    // endregion

    // region Setup

    /**
     * <summary>
     * Инициализирует маппер и создает экземпляр тестируемого сервиса.
     * </summary>
     **/
    @BeforeEach
    void setUp() {
        mapper = new ExchangeGeneratorMapper();
        service = new ExchangeRateGeneratorServiceImpl(mapper);
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет генерацию первого набора курсов для RUB, USD и CNY.
     * </summary>
     **/
    @Test
    void nextRates_shouldReturnFirstRates() {
        var result = service.nextRates();

        assertNotNull(result);
        assertEquals(3, result.rates().size());

        assertEquals(
                CurrencyEnumModel.RUB,
                result.rates().get(0).currency()
        );
        assertEquals(
                CurrencyEnumModel.USD,
                result.rates().get(1).currency()
        );
        assertEquals(
                CurrencyEnumModel.CNY,
                result.rates().get(2).currency()
        );
    }

    /**
     * <summary>
     * Проверяет циклическое переключение между последовательностями курсов.
     * После последнего шага генератор возвращается к первому.
     * </summary>
     **/
    @Test
    void nextRates_shouldCycleThroughRateSteps() {
        var first = service.nextRates();
        var second = service.nextRates();
        var third = service.nextRates();
        var fourth = service.nextRates();

        assertEquals(
                first.rates().get(1).buyRate(),
                fourth.rates().get(1).buyRate()
        );
        assertEquals(
                first.rates().get(1).sellRate(),
                fourth.rates().get(1).sellRate()
        );

        assertEquals(
                first.rates().get(2).buyRate(),
                fourth.rates().get(2).buyRate()
        );
        assertEquals(
                first.rates().get(2).sellRate(),
                fourth.rates().get(2).sellRate()
        );

        assertEquals(
                "90.0000",
                first.rates().get(1).buyRate().toString()
        );
        assertEquals(
                "91.0000",
                second.rates().get(1).buyRate().toString()
        );
        assertEquals(
                "89.5000",
                third.rates().get(1).buyRate().toString()
        );
    }

    /**
     * <summary>
     * Проверяет, что базовая валюта RUB всегда имеет единичный курс покупки и продажи.
     * </summary>
     **/
    @Test
    void nextRates_shouldReturnOneToOneRateForRub() {
        var result = service.nextRates();

        var rubRate = result.rates().getFirst();

        assertEquals(CurrencyEnumModel.RUB, rubRate.currency());
        assertEquals("1.0000", rubRate.buyRate().toString());
        assertEquals("1.0000", rubRate.sellRate().toString());
    }

    /**
     * <summary>
     * Проверяет генерацию ожидаемых курсов USD и CNY на каждом шаге последовательности.
     * </summary>
     **/
    @Test
    void nextRates_shouldReturnExpectedRatesForEachStep() {
        var first = service.nextRates();
        var second = service.nextRates();
        var third = service.nextRates();

        assertEquals("90.0000", first.rates().get(1).buyRate().toString());
        assertEquals("92.0000", first.rates().get(1).sellRate().toString());
        assertEquals("12.4000", first.rates().get(2).buyRate().toString());
        assertEquals("12.8000", first.rates().get(2).sellRate().toString());

        assertEquals("91.0000", second.rates().get(1).buyRate().toString());
        assertEquals("93.0000", second.rates().get(1).sellRate().toString());
        assertEquals("12.5000", second.rates().get(2).buyRate().toString());
        assertEquals("12.9000", second.rates().get(2).sellRate().toString());

        assertEquals("89.5000", third.rates().get(1).buyRate().toString());
        assertEquals("91.5000", third.rates().get(1).sellRate().toString());
        assertEquals("12.3000", third.rates().get(2).buyRate().toString());
        assertEquals("12.7000", third.rates().get(2).sellRate().toString());
    }

    // endregion
}