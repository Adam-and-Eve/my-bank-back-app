package ru.yandex.practicum.bank.exchange.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.bank.exchange.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.exchange.exceptions.InvalidRateException;
import ru.yandex.practicum.bank.exchange.mappers.ExchangeMapper;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateUpdateRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <summary>
 * Тесты сервиса обмена валют ExchangeServiceImpl.
 * Проверяет получение и обновление курсов валют, а также выполнение операций конвертации.
 * </summary>
 */
public class ExchangeServiceImplTest {

    // region Constants

    /**
     * <summary>
     * Фиксированный момент времени, используемый для проверки времени обновления курсов.
     * </summary>
     */
    private static final Instant INITIAL_TIME =
            Instant.parse("2026-08-19T10:00:00Z");

    /**
     * <summary>
     * Момент времени, используемый для проверки обновления курсов.
     * </summary>
     */
     private static final Instant UPDATE_TIME =
            Instant.parse("2026-08-19T11:00:00Z");

     // endregion

     // region Fields

     /**
     * <summary>
     * Сервис обмена валют, тестируемый в рамках тестов.
     * </summary>
     */
    private ExchangeServiceImpl exchangeService;

    /**
     * <summary>
     * Источник фиксированного времени для тестируемого сервиса.
     * </summary>
     */
    private Clock clock;

    // endregion

    // region Setup

    /**
     * <summary>
     * Создает тестовый сервис с фиксированным источником времени
     * перед выполнением каждого теста.
     * </summary>
     */
    @BeforeEach
    void setUp() {
        clock = Clock.fixed(INITIAL_TIME, ZoneOffset.UTC);

        exchangeService = new ExchangeServiceImpl(
                clock,
                new ExchangeMapper()
        );
    }

    // endregion

    // region getRates

    /**
     * <summary>
     * Проверяет, что после создания сервиса возвращаются начальные курсы
     * для RUB, USD и CNY.
     * </summary>
     */
    @Test
    void getRatesShouldReturnInitialRates() {
        var result = exchangeService.getRates();

        assertEquals(3, result.size());

        assertRate(
                result.get(0),
                CurrencyEnumModel.RUB,
                "1.0000",
                "1.0000"
        );

        assertRate(
                result.get(1),
                CurrencyEnumModel.USD,
                "90.0000",
                "92.0000"
        );

        assertRate(
                result.get(2),
                CurrencyEnumModel.CNY,
                "12.4000",
                "12.8000"
        );
    }

    /**
     * <summary>
     * Проверяет, что начальные курсы содержат время их первоначальной инициализации.
     * </summary>
     */
    @Test
    void getRatesShouldReturnInitialUpdatedAt() {
        var result = exchangeService.getRates();

        result.forEach(rate ->
                assertEquals(INITIAL_TIME, rate.updatedAt())
        );
    }

    /**
     * <summary>
     * Проверяет, что курсы валют возвращаются в порядке,
     * определяемом значением CurrencyEnumModel.
     * </summary>
     */
    @Test
    void getRatesShouldReturnRatesSortedByCurrency() {
        var result = exchangeService.getRates();

        var currencies = result.stream()
                .map(ExchangeRateResponseViewModel::currency)
                .toList();

        assertEquals(
                List.of(
                        CurrencyEnumModel.RUB,
                        CurrencyEnumModel.USD,
                        CurrencyEnumModel.CNY
                ),
                currencies
        );
    }

    // endregion

    // region updateRates

    /**
     * <summary>
     * Проверяет, что курс USD успешно обновляется
     * и новое значение сохраняется в сервисе.
     * </summary>
     */
    @Test
    void updateRatesShouldUpdateUsdRate() {
        clock = Clock.fixed(UPDATE_TIME, ZoneOffset.UTC);
        exchangeService = new ExchangeServiceImpl(
                clock,
                new ExchangeMapper()
        );

        var request = new ExchangeRatesUpdateRequestViewModel(
                List.of(
                        new ExchangeRateUpdateRequestViewModel(
                                CurrencyEnumModel.USD,
                                new BigDecimal("91.1234"),
                                new BigDecimal("93.5678")
                        )
                )
        );

        var result = exchangeService.updateRates(request);

        var usdRate = findRate(result, CurrencyEnumModel.USD);

        assertRate(
                usdRate,
                CurrencyEnumModel.USD,
                "91.1234",
                "93.5678"
        );

        assertEquals(UPDATE_TIME, usdRate.updatedAt());
    }

    /**
     * <summary>
     * Проверяет, что курс CNY успешно обновляется
     * и новое значение сохраняется в сервисе.
     * </summary>
     */
    @Test
    void updateRatesShouldUpdateCnyRate() {
        var request = new ExchangeRatesUpdateRequestViewModel(
                List.of(
                        new ExchangeRateUpdateRequestViewModel(
                                CurrencyEnumModel.CNY,
                                new BigDecimal("13.1234"),
                                new BigDecimal("13.5678")
                        )
                )
        );

        var result = exchangeService.updateRates(request);

        var cnyRate = findRate(result, CurrencyEnumModel.CNY);

        assertRate(
                cnyRate,
                CurrencyEnumModel.CNY,
                "13.1234",
                "13.5678"
        );
    }

    /**
     * <summary>
     * Проверяет, что при обновлении RUB переданные значения курса
     * игнорируются, а курс покупки и продажи остаются равными единице.
     * </summary>
     */
    @Test
    void updateRatesShouldKeepRubRateEqualToOne() {
        var request = new ExchangeRatesUpdateRequestViewModel(
                List.of(
                        new ExchangeRateUpdateRequestViewModel(
                                CurrencyEnumModel.RUB,
                                new BigDecimal("90.0000"),
                                new BigDecimal("95.0000")
                        )
                )
        );

        var result = exchangeService.updateRates(request);

        var rubRate = findRate(result, CurrencyEnumModel.RUB);

        assertRate(
                rubRate,
                CurrencyEnumModel.RUB,
                "1.0000",
                "1.0000"
        );
    }

    /**
     * <summary>
     * Проверяет, что за один запрос можно обновить курсы нескольких валют.
     * </summary>
     */
    @Test
    void updateRatesShouldUpdateMultipleCurrencies() {
        var request = new ExchangeRatesUpdateRequestViewModel(
                List.of(
                        new ExchangeRateUpdateRequestViewModel(
                                CurrencyEnumModel.USD,
                                new BigDecimal("91.0000"),
                                new BigDecimal("93.0000")
                        ),
                        new ExchangeRateUpdateRequestViewModel(
                                CurrencyEnumModel.CNY,
                                new BigDecimal("13.0000"),
                                new BigDecimal("14.0000")
                        )
                )
        );

        var result = exchangeService.updateRates(request);

        assertRate(
                findRate(result, CurrencyEnumModel.USD),
                CurrencyEnumModel.USD,
                "91.0000",
                "93.0000"
        );

        assertRate(
                findRate(result, CurrencyEnumModel.CNY),
                CurrencyEnumModel.CNY,
                "13.0000",
                "14.0000"
        );
    }

    /**
     * <summary>
     * Проверяет, что обновление курса с более чем четырьмя знаками
     * после запятой завершается исключением InvalidRateException.
     * </summary>
     */
    @Test
    void updateRatesShouldRejectRateWithMoreThanFourDecimalPlaces() {
        var request = new ExchangeRatesUpdateRequestViewModel(
                List.of(
                        new ExchangeRateUpdateRequestViewModel(
                                CurrencyEnumModel.USD,
                                new BigDecimal("91.12345"),
                                new BigDecimal("93.0000")
                        )
                )
        );

        assertThrows(
                InvalidRateException.class,
                () -> exchangeService.updateRates(request)
        );
    }

    /**
     * <summary>
     * Проверяет, что при передаче некорректного курса
     * ранее сохраненные курсы не изменяются.
     * </summary>
     */
    @Test
    void updateRatesShouldKeepPreviousRatesWhenUpdateFails() {
        var request = new ExchangeRatesUpdateRequestViewModel(
                List.of(
                        new ExchangeRateUpdateRequestViewModel(
                                CurrencyEnumModel.USD,
                                new BigDecimal("91.12345"),
                                new BigDecimal("93.0000")
                        )
                )
        );

        assertThrows(
                InvalidRateException.class,
                () -> exchangeService.updateRates(request)
        );

        var result = exchangeService.getRates();

        assertRate(
                findRate(result, CurrencyEnumModel.USD),
                CurrencyEnumModel.USD,
                "90.0000",
                "92.0000"
        );
    }

    /**
     * <summary>
     * Проверяет, что при обновлении курса его значения нормализуются
     * до четырех знаков после запятой.
     * </summary>
     */
    @Test
    void updateRatesShouldNormalizeRates() {
        var request = new ExchangeRatesUpdateRequestViewModel(
                List.of(
                        new ExchangeRateUpdateRequestViewModel(
                                CurrencyEnumModel.USD,
                                new BigDecimal("91.12"),
                                new BigDecimal("93")
                        )
                )
        );

        var result = exchangeService.updateRates(request);

        assertRate(
                findRate(result, CurrencyEnumModel.USD),
                CurrencyEnumModel.USD,
                "91.1200",
                "93.0000"
        );
    }

    /**
     * <summary>
     * Проверяет, что после обновления курса время обновления
     * устанавливается для обновленного курса.
     * </summary>
     */
    @Test
    void updateRatesShouldSetUpdatedAt() {
        clock = Clock.fixed(UPDATE_TIME, ZoneOffset.UTC);
        exchangeService = new ExchangeServiceImpl(
                clock,
                new ExchangeMapper()
        );

        var request = new ExchangeRatesUpdateRequestViewModel(
                List.of(
                        new ExchangeRateUpdateRequestViewModel(
                                CurrencyEnumModel.USD,
                                new BigDecimal("91.0000"),
                                new BigDecimal("93.0000")
                        )
                )
        );

        var result = exchangeService.updateRates(request);

        assertEquals(
                UPDATE_TIME,
                findRate(result, CurrencyEnumModel.USD).updatedAt()
        );
    }

    // endregion

    // region convert

    /**
     * <summary>
     * Проверяет, что конвертация валюты в саму себя
     * возвращает исходную сумму без изменения.
     * </summary>
     */
    @Test
    void convertShouldReturnSameAmountForSameCurrency() {
        var result = exchangeService.convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.USD,
                new BigDecimal("100.00")
        );

        assertEquals(
                CurrencyEnumModel.USD,
                result.sourceCurrency()
        );

        assertEquals(
                CurrencyEnumModel.USD,
                result.targetCurrency()
        );

        assertEquals(
                new BigDecimal("100.00"),
                result.sourceAmount()
        );

        assertEquals(
                new BigDecimal("100.00"),
                result.targetAmount()
        );

        assertEquals(
                BigDecimal.ONE,
                result.rate()
        );

        assertNull(result.updatedAt());
    }

    /**
     * <summary>
     * Проверяет конвертацию RUB в USD по курсу продажи USD.
     * </summary>
     */
    @Test
    void convertShouldConvertRubToUsd() {
        var result = exchangeService.convert(
                CurrencyEnumModel.RUB,
                CurrencyEnumModel.USD,
                new BigDecimal("100.00")
        );

        assertConversion(
                result,
                CurrencyEnumModel.RUB,
                CurrencyEnumModel.USD,
                "100.00",
                "1.11",
                "0.011111"
        );

        assertEquals(INITIAL_TIME, result.updatedAt());
    }

    /**
     * <summary>
     * Проверяет конвертацию USD в RUB по курсу продажи USD.
     * </summary>
     */
    @Test
    void convertShouldConvertUsdToRub() {
        var result = exchangeService.convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                new BigDecimal("100.00")
        );

        assertConversion(
                result,
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                "100.00",
                "9200.00",
                "92.000000"
        );

        assertEquals(INITIAL_TIME, result.updatedAt());
    }

    /**
     * <summary>
     * Проверяет конвертацию USD в CNY с использованием курса продажи USD
     * и курса покупки CNY.
     * </summary>
     */
    @Test
    void convertShouldConvertUsdToCny() {
        var result = exchangeService.convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.CNY,
                new BigDecimal("100.00")
        );

        assertConversion(
                result,
                CurrencyEnumModel.USD,
                CurrencyEnumModel.CNY,
                "100.00",
                "741.94",
                "7.419355"
        );

        assertEquals(INITIAL_TIME, result.updatedAt());
    }

    /**
     * <summary>
     * Проверяет конвертацию CNY в USD с использованием курса продажи CNY
     * и курса покупки USD.
     * </summary>
     */
    @Test
    void convertShouldConvertCnyToUsd() {
        var result = exchangeService.convert(
                CurrencyEnumModel.CNY,
                CurrencyEnumModel.USD,
                new BigDecimal("100.00")
        );

        assertConversion(
                result,
                CurrencyEnumModel.CNY,
                CurrencyEnumModel.USD,
                "100.00",
                "14.22",
                "0.142222"
        );

        assertEquals(INITIAL_TIME, result.updatedAt());
    }

    /**
     * <summary>
     * Проверяет округление результата конвертации до двух знаков
     * после запятой.
     * </summary>
     */
    @Test
    void convertShouldRoundTargetAmountToTwoDecimalPlaces() {
        var result = exchangeService.convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.CNY,
                new BigDecimal("1.00")
        );

        assertEquals(
                new BigDecimal("7.42"),
                result.targetAmount()
        );
    }

    /**
     * <summary>
     * Проверяет, что сумма с более чем двумя знаками после запятой
     * не проходит валидацию при конвертации.
     * </summary>
     */
    @Test
    void convertShouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        assertThrows(
                InvalidAmountException.class,
                () -> exchangeService.convert(
                        CurrencyEnumModel.USD,
                        CurrencyEnumModel.RUB,
                        new BigDecimal("100.001")
                )
        );
    }

    /**
     * <summary>
     * Проверяет, что нулевая сумма не может быть использована
     * для конвертации.
     * </summary>
     */
    @Test
    void convertShouldRejectZeroAmount() {
        assertThrows(
                InvalidAmountException.class,
                () -> exchangeService.convert(
                        CurrencyEnumModel.USD,
                        CurrencyEnumModel.RUB,
                        new BigDecimal("0.00")
                )
        );
    }

    /**
     * <summary>
     * Проверяет, что отрицательная сумма не может быть использована
     * для конвертации.
     * </summary>
     */
    @Test
    void convertShouldRejectNegativeAmount() {
        assertThrows(
                InvalidAmountException.class,
                () -> exchangeService.convert(
                        CurrencyEnumModel.USD,
                        CurrencyEnumModel.RUB,
                        new BigDecimal("-100.00")
                )
        );
    }

    // endregion

    // region Private Methods

    /**
     * <summary>
     * Находит курс указанной валюты в списке результатов.
     * </summary>
     *
     * @param rates Список курсов валют.
     * @param currency Искомая валюта.
     * @return Курс указанной валюты.
     */
    private ExchangeRateResponseViewModel findRate(
            List<ExchangeRateResponseViewModel> rates,
            CurrencyEnumModel currency) {

        return rates.stream()
                .filter(rate -> rate.currency() == currency)
                .findFirst()
                .orElseThrow();
    }

    /**
     * <summary>
     * Проверяет значения курса валюты.
     * </summary>
     *
     * @param rate Курс валюты.
     * @param currency Ожидаемая валюта.
     * @param expectedBuyRate Ожидаемый курс покупки.
     * @param expectedSellRate Ожидаемый курс продажи.
     */
    private void assertRate(
            ExchangeRateResponseViewModel rate,
            CurrencyEnumModel currency,
            String expectedBuyRate,
            String expectedSellRate) {

        assertEquals(currency, rate.currency());

        assertEquals(new BigDecimal(expectedBuyRate), rate.buyRate());

        assertEquals(new BigDecimal(expectedSellRate), rate.sellRate());
    }

    /**
     * <summary>
     * Проверяет значения результата конвертации валют.
     * </summary>
     *
     * @param result Результат конвертации.
     * @param sourceCurrency Ожидаемая исходная валюта.
     * @param targetCurrency Ожидаемая целевая валюта.
     * @param sourceAmount Ожидаемая исходная сумма.
     * @param targetAmount Ожидаемая целевая сумма.
     * @param conversionRate Ожидаемый курс конвертации.
     */
    private void assertConversion(
            ConversionResponseViewModel result,
            CurrencyEnumModel sourceCurrency,
            CurrencyEnumModel targetCurrency,
            String sourceAmount,
            String targetAmount,
            String conversionRate) {

        assertEquals(sourceCurrency, result.sourceCurrency());

        assertEquals(targetCurrency, result.targetCurrency());

        assertEquals(new BigDecimal(sourceAmount), result.sourceAmount());

        assertEquals(new BigDecimal(targetAmount), result.targetAmount());

        assertEquals(new BigDecimal(conversionRate), result.rate());
    }

    // endregion
}