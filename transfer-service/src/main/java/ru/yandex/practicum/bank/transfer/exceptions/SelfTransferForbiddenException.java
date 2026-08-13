package ru.yandex.practicum.bank.transfer.exceptions;

/**
 * <summary>
 * Исключение, возникающее при попытке выполнить перевод самому себе (на тот же аккаунт/логин).
 * </summary>
 **/
public class SelfTransferForbiddenException extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения со стандартным сообщением о запрете самоперевода.
     * </summary>
     **/
    public SelfTransferForbiddenException() {
        super("Transfer to the same account is forbidden");
    }
}