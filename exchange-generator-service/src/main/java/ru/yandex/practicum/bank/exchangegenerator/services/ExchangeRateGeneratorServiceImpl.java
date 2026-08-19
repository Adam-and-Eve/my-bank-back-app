package ru.yandex.practicum.bank.exchangegenerator.services;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.exchangegenerator.interfaces.ExchangeRateGeneratorService;
import ru.yandex.practicum.bank.exchangegenerator.mappers.ExchangeGeneratorMapper;
import ru.yandex.practicum.bank.exchangegenerator.models.RateStepModel;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateUpdateRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

import java.math.BigDecimal;
import java.util.List;

/**
 * <summary>
 * Сервис для генерации последовательности курсов валют.
 * </summary>
 **/
@Service
public class ExchangeRateGeneratorServiceImpl implements ExchangeRateGeneratorService {

    // region Fields

    private final List<RateStepModel> usdSteps;

    private final List<RateStepModel> cnySteps;

    private int index;

    private final ExchangeGeneratorMapper mapper;

    // endregion

    // region Constructors

    public ExchangeRateGeneratorServiceImpl(
            ExchangeGeneratorMapper mapper
    ) {
        usdSteps = List.of(
                new RateStepModel("90.0000", "92.0000"),
                new RateStepModel("91.0000", "93.0000"),
                new RateStepModel("89.5000", "91.5000")
        );

        cnySteps = List.of(
                new RateStepModel("12.4000", "12.8000"),
                new RateStepModel("12.5000", "12.9000"),
                new RateStepModel("12.3000", "12.7000")
        );

        index = 0;

        this.mapper = mapper;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Возвращает следующий набор курсов валют из заданной последовательности.
     * После достижения конца последовательности начинает использовать значения с начала.
     * Метод синхронизирован для безопасного получения курсов при параллельных вызовах.
     * </summary>
     * @return Модель запроса с обновленными курсами RUB, USD и CNY.
     **/
    @Override
    public synchronized ExchangeRatesUpdateRequestViewModel nextRates() {
        var usd = usdSteps.get(index % usdSteps.size());
        var cny = cnySteps.get(index % cnySteps.size());

        index++;

        return new ExchangeRatesUpdateRequestViewModel(List.of(
                new ExchangeRateUpdateRequestViewModel(
                        CurrencyEnumModel.RUB,
                        new BigDecimal("1.0000"), new BigDecimal("1.0000")),
                mapper.toExchangeRateUpdateRequestViewModel(
                        CurrencyEnumModel.USD,
                        usd),
                mapper.toExchangeRateUpdateRequestViewModel(
                        CurrencyEnumModel.CNY,
                        cny)
        ));
    }

    // endregion
}