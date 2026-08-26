package ru.yandex.practicum.bank.shared.viewmodels;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * <summary>
 * Модель ответа с результатом конвертации валют (ConversionResponseViewModel).
 * Содержит исходную и целевую валюты, суммы конвертации, курс и время
 * последнего обновления курса.
 * </summary>
 * @param sourceCurrency Исходная валюта.
 * @param targetCurrency Целевая валюта.
 * @param sourceAmount Сумма в исходной валюте.
 * @param targetAmount Результирующая сумма в целевой валюте.
 * @param rate Курс конвертации исходной валюты в целевую.
 * @param updatedAt Время последнего обновления курса.
 **/
public record ConversionResponseViewModel (
        CurrencyEnumModel sourceCurrency,
        CurrencyEnumModel targetCurrency,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal sourceAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal targetAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal rate,
        Instant updatedAt
) {
}