package ru.yandex.practicum.bank.exchange.helpers;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.bank.exchange.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.exchange.exceptions.InvalidRateException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <summary>
 * Тесты вспомогательного класса ExchangeHelper.
 * Проверяет валидацию и нормализацию курсов обмена валют и сумм конвертации.
 * </summary>
 */
public class ExchangeHelperTest {

    // region Tests

    /**
     * <summary>
     * Проверяет, что корректный положительный курс с четырьмя знаками после запятой
     * проходит валидацию.
     * </summary>
     */
    @Test
    void validateRateShouldAcceptPositiveRateWithFourDecimalPlaces() {
        assertDoesNotThrow(() ->
                ExchangeHelper.validateRate(new BigDecimal("92.1234"))
        );
    }

    /**
     * <summary>
     * Проверяет, что положительный курс с количеством знаков после запятой,
     * меньшим допустимого, проходит валидацию.
     * </summary>
     */
    @Test
    void validateRateShouldAcceptPositiveRateWithFewerThanFourDecimalPlaces() {
        assertDoesNotThrow(() ->
                ExchangeHelper.validateRate(new BigDecimal("92.12"))
        );
    }

    /**
     * <summary>
     * Проверяет, что нулевой курс не проходит валидацию.
     * </summary>
     */
    @Test
    void validateRateShouldRejectZeroRate() {
        assertThrows(
                InvalidRateException.class,
                () -> ExchangeHelper.validateRate(new BigDecimal("0.0000"))
        );
    }

    /**
     * <summary>
     * Проверяет, что отрицательный курс не проходит валидацию.
     * </summary>
     */
    @Test
    void validateRateShouldRejectNegativeRate() {
        assertThrows(
                InvalidRateException.class,
                () -> ExchangeHelper.validateRate(new BigDecimal("-92.0000"))
        );
    }

    /**
     * <summary>
     * Проверяет, что курс с более чем четырьмя знаками после запятой
     * не проходит валидацию.
     * </summary>
     */
    @Test
    void validateRateShouldRejectRateWithMoreThanFourDecimalPlaces() {
        assertThrows(
                InvalidRateException.class,
                () -> ExchangeHelper.validateRate(new BigDecimal("92.12345"))
        );
    }

    /**
     * <summary>
     * Проверяет, что корректная положительная сумма с двумя знаками после запятой
     * проходит валидацию.
     * </summary>
     */
    @Test
    void validateAmountShouldAcceptPositiveAmountWithTwoDecimalPlaces() {
        assertDoesNotThrow(() ->
                ExchangeHelper.validateAmount(new BigDecimal("100.25"))
        );
    }

    /**
     * <summary>
     * Проверяет, что положительная сумма с количеством знаков после запятой,
     * меньшим допустимого, проходит валидацию.
     * </summary>
     */
    @Test
    void validateAmountShouldAcceptPositiveAmountWithFewerThanTwoDecimalPlaces() {
        assertDoesNotThrow(() ->
                ExchangeHelper.validateAmount(new BigDecimal("100.2"))
        );
    }

    /**
     * <summary>
     * Проверяет, что нулевая сумма не проходит валидацию.
     * </summary>
     */
    @Test
    void validateAmountShouldRejectZeroAmount() {
        assertThrows(
                InvalidAmountException.class,
                () -> ExchangeHelper.validateAmount(new BigDecimal("0.00"))
        );
    }

    /**
     * <summary>
     * Проверяет, что отрицательная сумма не проходит валидацию.
     * </summary>
     */
    @Test
    void validateAmountShouldRejectNegativeAmount() {
        assertThrows(
                InvalidAmountException.class,
                () -> ExchangeHelper.validateAmount(new BigDecimal("-100.00"))
        );
    }

    /**
     * <summary>
     * Проверяет, что сумма с более чем двумя знаками после запятой
     * не проходит валидацию.
     * </summary>
     */
    @Test
    void validateAmountShouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        assertThrows(
                InvalidAmountException.class,
                () -> ExchangeHelper.validateAmount(new BigDecimal("100.001"))
        );
    }

    /**
     * <summary>
     * Проверяет, что курс с четырьмя знаками после запятой
     * сохраняет исходное значение при нормализации.
     * </summary>
     */
    @Test
    void normalizeRateShouldKeepRateWithFourDecimalPlaces() {
        var rate = new BigDecimal("92.1234");

        var result = ExchangeHelper.normalizeRate(rate);

        assertEquals(
                new BigDecimal("92.1234"),
                result
        );
    }

    /**
     * <summary>
     * Проверяет, что при нормализации курса недостающие знаки
     * после запятой заполняются нулями.
     * </summary>
     */
    @Test
    void normalizeRateShouldAddTrailingZeros() {
        var rate = new BigDecimal("92.12");

        var result = ExchangeHelper.normalizeRate(rate);

        assertEquals(
                new BigDecimal("92.1200"),
                result
        );
    }

    /**
     * <summary>
     * Проверяет, что при нормализации целого курса
     * добавляются четыре знака после запятой.
     * </summary>
     */
    @Test
    void normalizeRateShouldAddTrailingZerosToIntegerRate() {
        var rate = new BigDecimal("92");

        var result = ExchangeHelper.normalizeRate(rate);

        assertEquals(
                new BigDecimal("92.0000"),
                result
        );
    }

    /**
    * <summary>
    * Проверяет, что нормализация курса с более чем четырьмя знаками
    * после запятой приводит к исключению ArithmeticException.
    * </summary>
    */
    @Test
    void normalizeRateShouldRejectRateWithMoreThanFourDecimalPlaces() {
        var rate = new BigDecimal("92.12345");

        assertThrows(
            ArithmeticException.class,
            () -> ExchangeHelper.normalizeRate(rate)
        );
    }

    // endregion
}