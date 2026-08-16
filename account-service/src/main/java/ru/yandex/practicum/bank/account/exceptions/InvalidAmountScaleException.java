package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при передаче суммы с превышением допустимого количества знаков после запятой (InvalidAmountScaleException).
 * Количество знаков после запятой (scale) не должно превышать 2.
 * </summary>
 **/
public class InvalidAmountScaleException extends RuntimeException {

    /**
     * Конструктор исключения.
     */
    public InvalidAmountScaleException() {
        super("Amount scale must not be greater than 2");
    }
}