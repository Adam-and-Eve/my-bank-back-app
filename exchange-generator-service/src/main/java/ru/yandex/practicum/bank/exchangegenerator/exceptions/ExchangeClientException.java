package ru.yandex.practicum.bank.exchangegenerator.exceptions;

/**
 * <summary>
 * Исключение, возникающее при ошибках взаимодействия с сервисом курсов валют.
 * </summary>
 **/
public class ExchangeClientException extends RuntimeException{

    public ExchangeClientException(String message) {
        super(message);
    }

    public ExchangeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}