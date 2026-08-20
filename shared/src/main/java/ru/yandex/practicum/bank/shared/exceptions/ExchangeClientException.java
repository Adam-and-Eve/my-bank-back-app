package ru.yandex.practicum.bank.shared.exceptions;

public class ExchangeClientException extends RuntimeException {

    public ExchangeClientException(String message) {
        super(message);
    }

    public ExchangeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}