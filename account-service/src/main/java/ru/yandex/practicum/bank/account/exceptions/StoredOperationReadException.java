package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при ошибке чтения ранее сохранённого ответа для операции (StoredOperationReadException).
 * </summary>
 **/
public class StoredOperationReadException extends RuntimeException {

    /**
     * Конструктор исключения.
     * @param operationId Идентификатор операции, для которой не удалось прочитать ответ.
     * @param cause Первопричина возникновения ошибки.
     */
    public StoredOperationReadException(String operationId, Throwable cause) {
        super("Cannot read stored response for operation " + operationId, cause);
    }
}
