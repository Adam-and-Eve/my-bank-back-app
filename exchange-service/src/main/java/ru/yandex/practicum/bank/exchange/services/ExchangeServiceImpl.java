package ru.yandex.practicum.bank.exchange.services;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.exchange.helpers.ExchangeHelper;
import ru.yandex.practicum.bank.exchange.interfaces.ExchangeService;
import ru.yandex.practicum.bank.exchange.mappers.ExchangeMapper;
import ru.yandex.practicum.bank.exchange.models.ExchangeRateSnapshotModel;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateUpdateRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * <summary>
 * Сервис для получения и обновления курсов валют,
 * а также выполнения операций конвертации валют.
 * </summary>
 */
@Service
public class ExchangeServiceImpl implements ExchangeService {

    // region Constants

    /**
     * <summary>
     * Фиксированный курс российского рубля.
     * </summary>
     */
    private static final BigDecimal RUB_RATE = new BigDecimal("1.0000");

    // endregion

    // region Fields

    /**
     * <summary>
     * Источник текущего времени, используемый для определения времени обновления курсов.
     * </summary>
     */
    private final Clock clock;

    /**
     * <summary>
     * Текущие курсы поддерживаемых валют.
     * </summary>
     */
    private volatile Map<CurrencyEnumModel, ExchangeRateSnapshotModel> rates;

    /**
     * <summary>
     * Mapper для преобразования моделей курсов в модели ответов.
     * </summary>
     */
    private final ExchangeMapper exchangeMapper;

    // endregion

    // region Constructors

    /**
     * <summary>
     * Создает сервис обмена валют и инициализирует начальные курсы.
     * </summary>
     * @param clock Источник текущего времени.
     * @param exchangeMapper Mapper для преобразования моделей курсов.
     */
    public ExchangeServiceImpl(
            Clock clock,
            ExchangeMapper exchangeMapper) {

        this.clock = clock;
        this.exchangeMapper = exchangeMapper;
        this.rates = initialRates(clock.instant());
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Возвращает текущие курсы обмена всех поддерживаемых валют.
     * </summary>
     * @return Список текущих курсов обмена валют.
     */
    public List<ExchangeRateResponseViewModel> getRates() {
        var currentRates = rates;

        return currentRates.values().stream()
                .sorted(Comparator.comparing(ExchangeRateSnapshotModel::currency))
                .map(exchangeMapper::toExchangeRateResponseViewModel)
                .toList();
    }

    /**
     * <summary>
     * Обновляет курсы обмена указанных валют.
     * Для российского рубля курс всегда остается равным единице.
     * </summary>
     * @param request Запрос с данными для обновления курсов обмена валют.
     * @return Список актуальных курсов обмена всех поддерживаемых валют.
     */
    public synchronized List<ExchangeRateResponseViewModel> updateRates(
            ExchangeRatesUpdateRequestViewModel request) {

        var updatedAt = clock.instant();
        var updatedRates = new EnumMap<>(rates);

        for (ExchangeRateUpdateRequestViewModel rate : request.rates()) {
            if (rate.currency() == CurrencyEnumModel.RUB) {
                updatedRates.put(
                        CurrencyEnumModel.RUB,
                        snapshot(
                                CurrencyEnumModel.RUB,
                                RUB_RATE,
                                RUB_RATE,
                                updatedAt)
                );

                continue;
            }

            ExchangeHelper.validateRate(rate.buyRate());
            ExchangeHelper.validateRate(rate.sellRate());

            updatedRates.put(
                    rate.currency(),
                    snapshot(
                            rate.currency(),
                            ExchangeHelper.normalizeRate(rate.buyRate()),
                            ExchangeHelper.normalizeRate(rate.sellRate()),
                            updatedAt)
            );
        }

        rates = Map.copyOf(updatedRates);

        return getRates();
    }

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
            BigDecimal amount) {

        ExchangeHelper.validateAmount(amount);

        if (sourceCurrency == targetCurrency) {
            return new ConversionResponseViewModel(
                    sourceCurrency,
                    targetCurrency,
                    amount,
                    amount,
                    BigDecimal.ONE,
                    null
            );
        }

        var currentRates = rates;

        var sourceRate = currentRates.get(sourceCurrency);
        var targetRate = currentRates.get(targetCurrency);

        var conversionRate = sourceRate.sellRate()
                .divide(targetRate.buyRate(), 6, RoundingMode.HALF_UP);

        var targetAmount = amount.multiply(conversionRate)
                .setScale(ExchangeHelper.AMOUNT_SCALE, RoundingMode.HALF_UP);

        var updatedAt = sourceRate.updatedAt().isAfter(targetRate.updatedAt())
                ? sourceRate.updatedAt()
                : targetRate.updatedAt();

        return new ConversionResponseViewModel(
                sourceCurrency,
                targetCurrency,
                amount.setScale(
                        ExchangeHelper.AMOUNT_SCALE,
                        RoundingMode.UNNECESSARY),
                targetAmount,
                conversionRate,
                updatedAt
        );
    }

    // endregion

    // region Private Methods

    /**
     * <summary>
     * Создает снимок курса валюты на указанный момент времени.
     * </summary>
     * @param currency Валюта.
     * @param buyRate Курс покупки.
     * @param sellRate Курс продажи.
     * @param updatedAt Время обновления курса.
     * @return Снимок курса валюты.
     */
    private ExchangeRateSnapshotModel snapshot(
            CurrencyEnumModel currency,
            BigDecimal buyRate,
            BigDecimal sellRate,
            Instant updatedAt) {

        return new ExchangeRateSnapshotModel(
                currency,
                buyRate,
                sellRate,
                updatedAt
        );
    }

    /**
     * <summary>
     * Создает начальный набор курсов поддерживаемых валют.
     * </summary>
     * @param now Время создания начальных курсов.
     * @return Карта начальных курсов валют.
     */
    private Map<CurrencyEnumModel, ExchangeRateSnapshotModel> initialRates(
            Instant now) {

        Map<CurrencyEnumModel, ExchangeRateSnapshotModel> initialRates =
                new EnumMap<>(CurrencyEnumModel.class);

        initialRates.put(
                CurrencyEnumModel.RUB,
                snapshot(
                        CurrencyEnumModel.RUB,
                        RUB_RATE,
                        RUB_RATE,
                        now)
        );

        initialRates.put(
                CurrencyEnumModel.USD,
                snapshot(
                        CurrencyEnumModel.USD,
                        new BigDecimal("90.0000"),
                        new BigDecimal("92.0000"),
                        now)
        );

        initialRates.put(
                CurrencyEnumModel.CNY,
                snapshot(
                        CurrencyEnumModel.CNY,
                        new BigDecimal("12.4000"),
                        new BigDecimal("12.8000"),
                        now)
        );

        return Map.copyOf(initialRates);
    }

    // endregion
}