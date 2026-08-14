package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при попытке повторного выполнения операции, которая ранее уже завершилась с ошибкой (OperationAlreadyFailedException).
 * </summary>
 **/
public class OperationAlreadyFailedException extends RuntimeException {

    /**
     * Конструктор исключения.
     * @param operationId Идентификатор операции, ранее завершившейся с ошибкой.
     */
    public OperationAlreadyFailedException(String operationId) {
        super("Operation " + operationId + " has already failed");
    }
}