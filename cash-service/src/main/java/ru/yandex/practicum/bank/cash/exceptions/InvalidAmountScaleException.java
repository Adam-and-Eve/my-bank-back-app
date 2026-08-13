package ru.yandex.practicum.bank.cash.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при попытке передать сумму с недопустимым количеством знаков после запятой.
 * Возникает, если точность (scale) переданного значения BigDecimal превышает 2 знака.
 * </summary>
 **/
public class InvalidAmountScaleException extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения InvalidAmountScaleException с дефолтным сообщением об ошибке.
     * </summary>
     **/
    public InvalidAmountScaleException() {
        super("Amount scale must not be greater than 2");
    }
}