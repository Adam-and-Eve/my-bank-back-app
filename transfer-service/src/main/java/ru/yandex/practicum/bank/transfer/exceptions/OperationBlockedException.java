package ru.yandex.practicum.bank.transfer.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при блокировке подозрительной финансовой операции.
 * Содержит причину, по которой операция была отклонена сервисом блокировки.
 * </summary>
 **/
public class OperationBlockedException extends RuntimeException {

    public OperationBlockedException(String reason) {
        super(reason);
    }
}