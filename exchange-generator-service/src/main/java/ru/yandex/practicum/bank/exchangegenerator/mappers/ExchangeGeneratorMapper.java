package ru.yandex.practicum.bank.exchangegenerator.mappers;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.exchangegenerator.models.RateStepModel;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateUpdateRequestViewModel;

import java.math.BigDecimal;

/**
 * <summary>
 * Маппер для преобразования моделей генератора курсов валют в модели запросов
 * на обновление курсов валют.
 * </summary>
 **/
@Component
public class ExchangeGeneratorMapper {

    // region Methods

    /**
     * <summary>
     * Преобразует данные шага изменения курса валюты в модель запроса на обновление курса.
     * </summary>
     * @param currency Валюта, для которой обновляется курс.
     * @param step Данные шага с курсами покупки и продажи.
     * @return Модель запроса на обновление курса валюты.
     **/
    public ExchangeRateUpdateRequestViewModel toExchangeRateUpdateRequestViewModel(
            CurrencyEnumModel currency,
            RateStepModel step) {
        return new ExchangeRateUpdateRequestViewModel(
                currency,
                new BigDecimal(step.buyRate()),
                new BigDecimal(step.sellRate())
        );
    }

    // endregion
}