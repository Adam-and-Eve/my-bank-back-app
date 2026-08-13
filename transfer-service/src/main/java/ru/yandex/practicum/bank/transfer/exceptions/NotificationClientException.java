package ru.yandex.practicum.bank.transfer.exceptions;

/**
 * <summary>
 * Исключение, возникающее при ошибках взаимодействия с клиентом сервиса уведомлений (Notification Service).
 * </summary>
 **/
public class NotificationClientException extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения с указанным сообщением и исходной причиной ошибки.
     * </summary>
     * @param message Сообщение с подробным описанием причины сбоя.
     * @param cause Исходное исключение (причина сбоя).
     **/
    public NotificationClientException(String message, Throwable cause) {
        super(message, cause);
    }
}