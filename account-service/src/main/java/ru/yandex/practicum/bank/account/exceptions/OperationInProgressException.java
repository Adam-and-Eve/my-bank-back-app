package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при попытке повторно запустить операцию, которая в данный момент уже находится в процессе обработки (OperationInProgressException).
 * </summary>
 **/
public class OperationInProgressException extends  RuntimeException {

    /**
     * Конструктор исключения.
     *
     * @param operationId Идентификатор операции, находящейся в процессе обработки.
     */
    public OperationInProgressException(String operationId) {
        super("Operation " + operationId + " is already processing");
    }
}