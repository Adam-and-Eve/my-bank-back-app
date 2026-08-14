package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при передаче некорректной (неположительной) суммы операции (InvalidAmountException).
 * </summary>
 **/
public class InvalidAmountException extends RuntimeException {

    /**
     * Конструктор исключения.
     */
    public InvalidAmountException() {
        super("Amount must be greater than zero");
    }
}