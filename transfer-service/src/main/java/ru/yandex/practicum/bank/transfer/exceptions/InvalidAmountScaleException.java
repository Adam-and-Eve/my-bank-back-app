package ru.yandex.practicum.bank.transfer.exceptions;

/**
 * <summary>
 * Исключение, возникающее при попытке выполнить перевод с некорректным количеством знаков после запятой (дробной частью более 2 знаков).
 * </summary>
 **/
public class InvalidAmountScaleException extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения со стандартным сообщением о том, что количество знаков после запятой не должно превышать 2.
     * </summary>
     **/
    public InvalidAmountScaleException() {
        super("Amount scale must not be greater than 2");
    }
}