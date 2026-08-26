package ru.yandex.practicum.bank.shared.viewmodels;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * <summary>
 * Модель ответа с информацией о курсе обмена валюты (ExchangeRateResponseViewModel).
 * Содержит валюту, курсы покупки и продажи, а также время последнего обновления курса.
 * </summary>
 * @param currency Валюта, для которой указан курс обмена.
 * @param buyRate Курс покупки валюты.
 * @param sellRate Курс продажи валюты.
 * @param updatedAt Время последнего обновления курса.
 **/
public record ExchangeRateResponseViewModel (
        CurrencyEnumModel currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal buyRate,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal sellRate,
        Instant updatedAt
) {
}