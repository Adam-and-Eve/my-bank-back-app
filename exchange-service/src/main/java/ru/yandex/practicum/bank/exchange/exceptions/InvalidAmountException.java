package ru.yandex.practicum.bank.exchange.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при указании некорректной суммы операции.
 * Сумма должна быть положительной и иметь не более двух знаков после запятой.
 * </summary>
 **/
public class InvalidAmountException extends RuntimeException {

    /**
     * Конструктор исключения.
     */
    public InvalidAmountException() {
        super("Amount must be positive and have scale no more than 2");
    }
}