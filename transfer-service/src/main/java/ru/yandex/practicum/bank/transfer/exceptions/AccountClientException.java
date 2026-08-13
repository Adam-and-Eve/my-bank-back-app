package ru.yandex.practicum.bank.transfer.exceptions;

/**
 * <summary>
 * Исключение, возникающее при ошибках взаимодействия с клиентом сервиса счетов (Accounts Service).
 * </summary>
 **/
public class AccountClientException extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения с указанным понятным сообщением об ошибке.
     * </summary>
     * @param message Сообщение с подробным описанием причины сбоя.
     **/
    public AccountClientException(String message) {
        super(message);
    }

    /**
     * <summary>
     * Создает новый экземпляр исключения с указанным сообщением и исходной причиной ошибки.
     * </summary>
     * @param message Сообщение с подробным описанием причины сбоя.
     * @param cause Исходное исключение (причина сбоя).
     **/
    public AccountClientException(String message, Throwable cause) {
        super(message, cause);
    }
}