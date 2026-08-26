package ru.yandex.practicum.bank.shared.viewmodels;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * <summary>
 * Модель запроса на обновление курсов обмена валют (ExchangeRatesUpdateRequestViewModel).
 * Содержит список курсов валют, предназначенных для обновления.
 * </summary>
 * @param rates Список данных для обновления курсов обмена валют.
 **/
public record ExchangeRatesUpdateRequestViewModel (
        @NotEmpty
        List<@Valid ExchangeRateUpdateRequestViewModel> rates
) {
}