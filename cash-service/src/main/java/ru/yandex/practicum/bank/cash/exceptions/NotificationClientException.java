package ru.yandex.practicum.bank.cash.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при сбоях в процессе взаимодействия с внешним сервисом уведомлений (Notifications Service).
 * Возникает при ошибках выполнения HTTP-запросов, недоступности сервиса или некорректных ответах.
 * </summary>
 **/
public class NotificationClientException extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения NotificationsClientException с указанным сообщением и первопричиной.
     * </summary>
     * @param message Сообщение с описанием причины ошибки.
     * @param cause Первопричина исключения (Throwable).
     **/
    public NotificationClientException(String message, Throwable cause) {
        super(message, cause);
    }
}