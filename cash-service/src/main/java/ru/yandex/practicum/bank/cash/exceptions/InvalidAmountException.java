package ru.yandex.practicum.bank.cash.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при попытке выполнить операцию с некорректной суммой.
 * Возникает, если передаваемая сумма меньше или равна нулю.
 * </summary>
 **/
public class InvalidAmountException extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения InvalidAmountException с дефолтным сообщением об ошибке.
     * </summary>
     **/
    public InvalidAmountException() {
        super("Amount must be greater than zero");
    }
}