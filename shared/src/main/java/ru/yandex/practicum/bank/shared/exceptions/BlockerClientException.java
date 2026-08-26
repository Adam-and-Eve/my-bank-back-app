package ru.yandex.practicum.bank.shared.exceptions;

/**
 * <summary>
 * Исключение, возникающее при ошибках взаимодействия с сервисом блокировки операций.
 * </summary>
 **/
public class BlockerClientException extends RuntimeException {

    /**
     * <summary>
     * Создает исключение с указанным сообщением об ошибке.
     * </summary>
     * @param message Сообщение об ошибке.
     **/
    public BlockerClientException(String message) {
        super(message);
    }

    /**
     * <summary>
     * Создает исключение с указанным сообщением и исходной причиной ошибки.
     * </summary>
     * @param message Сообщение об ошибке.
     * @param cause Исходное исключение, вызвавшее ошибку.
     **/
    public BlockerClientException(String message, Throwable cause) {
        super(message, cause);
    }
}