package ru.yandex.practicum.bank.shared.exceptions;

/**
 * <summary>
 * Исключение, возникающее при ошибках взаимодействия с клиентом сервиса.
 * </summary>
 **/
public class ServiceClientException  extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения с указанным сообщением и исходной причиной ошибки.
     * </summary>
     * @param message Сообщение с подробным описанием причины сбоя.
     **/
    public ServiceClientException(String message) {
        super(message);
    }

    /**
     * <summary>
     * Создает новый экземпляр исключения с указанным сообщением и исходной причиной ошибки.
     * </summary>
     * @param message Сообщение с подробным описанием причины сбоя.
     * @param cause Исходное исключение (причина сбоя).
     **/
    public ServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }
}