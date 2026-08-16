package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при конфликте идемпотентности (IdempotencyConflictException).
 * Возникает, когда идентификатор операции уже был использован с другими параметрами запроса.
 * </summary>
 **/
public class IdempotencyConflictException extends RuntimeException {

    /**
     * Конструктор исключения.
     * @param operationId Идентификатор операции, вызвавший конфликт.
     */
    public IdempotencyConflictException(String operationId) {
        super("Operation id " + operationId + " was already used with another request");
    }
}