package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при попытке перевода средств на тот же самый счёт (SelfTransferForbiddenException).
 * </summary>
 **/
public class SelfTransferForbiddenException extends  RuntimeException {

    /**
     * Конструктор исключения.
     */
    public SelfTransferForbiddenException() {
        super("Transfer to the same account is forbidden");
    }
}