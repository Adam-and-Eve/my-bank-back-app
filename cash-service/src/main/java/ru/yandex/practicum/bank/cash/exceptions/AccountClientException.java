package ru.yandex.practicum.bank.cash.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при сбоях в процессе взаимодействия с внешним сервисом счетов (Accounts Service).
 * Возникает при ошибках выполнения HTTP-запросов, недоступности сервиса или некорректных ответах.
 * </summary>
 **/
public class AccountClientException extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения AccountsClientException с указанным сообщением об ошибке.
     * </summary>
     * @param message Сообщение с описанием причины ошибки.
     **/
    public AccountClientException(String message) {
        super(message);
    }

    /**
     * <summary>
     * Создает новый экземпляр исключения AccountsClientException с указанным сообщением и первопричиной.
     * </summary>
     * @param message Сообщение с описанием причины ошибки.
     * @param cause Первопричина исключения (Throwable).
     **/
    public AccountClientException(String message, Throwable cause) {
        super(message, cause);
    }
}