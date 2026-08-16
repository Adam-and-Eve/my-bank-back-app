package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при нехватке средств на счёте для совершения операции (InsufficientFundsException).
 * </summary>
 **/
public class InsufficientFundsException extends RuntimeException {

    /**
     * Конструктор исключения.
     */
    public InsufficientFundsException() {
        super("Недостаточно средств");
    }
}