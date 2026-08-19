package ru.yandex.practicum.bank.exchange.helpers;

import ru.yandex.practicum.bank.exchange.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.exchange.exceptions.InvalidRateException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * <summary>
 * Вспомогательный класс для выполнения операций, связанных с обменом валют.
 * Содержит методы валидации и нормализации курсов и сумм.
 * </summary>
 */
public class ExchangeHelper {

    // region Constants

    /**
     * <summary>
     * Максимальное количество знаков после запятой для курса обмена валюты.
     * </summary>
     */
    public static final int RATE_SCALE = 4;

    /**
     * <summary>
     * Максимальное количество знаков после запятой для суммы конвертации.
     * </summary>
     */
    public static final int AMOUNT_SCALE = 2;

    // endregion

    // region Methods

    /**
     * <summary>
     * Проверяет корректность курса обмена валюты.
     * Курс должен быть положительным и содержать не более четырех знаков после запятой.
     * </summary>
     * @param rate Курс обмена валюты.
     * @throws InvalidRateException Если курс является неположительным или содержит больше четырех знаков после запятой.
     */
    public static void validateRate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.scale() > RATE_SCALE) {
            throw new InvalidRateException();
        }
    }

    /**
     * <summary>
     * Проверяет корректность суммы для конвертации.
     * Сумма должна быть положительной и содержать не более двух знаков после запятой.
     * </summary>
     * @param amount Сумма для конвертации.
     * @throws InvalidAmountException Если сумма является неположительной или содержит больше двух знаков после запятой.
     */
    public static void validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > AMOUNT_SCALE) {
            throw new InvalidAmountException();
        }
    }

    /**
     * <summary>
     * Приводит курс обмена валюты к стандартному масштабу в четыре знака после запятой.
     * </summary>
     * @param rate Курс обмена валюты.
     * @return Курс с масштабом в четыре знака после запятой.
     */
    public static BigDecimal normalizeRate(BigDecimal rate) {
        return rate.setScale(RATE_SCALE, RoundingMode.UNNECESSARY);
    }

    // endregion
}