package ru.yandex.practicum.bank.transfer.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * <summary>
 * Исключение, возникающее при ошибках взаимодействия с клиентом сервиса счетов (Accounts Service).
 * </summary>
 **/
public class AccountClientException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String code;

    public AccountClientException(String message) {
        this(message, HttpStatus.BAD_GATEWAY, "ACCOUNT_SERVICE_UNAVAILABLE", null);
    }

    public AccountClientException(String message, Throwable cause) {
        this(message, HttpStatus.BAD_GATEWAY, "ACCOUNT_SERVICE_UNAVAILABLE", cause);
    }

    public AccountClientException(String message, HttpStatusCode statusCode, Throwable cause) {
        this(message, statusCode, defaultCode(statusCode), cause);
    }

    public AccountClientException(String message, HttpStatusCode statusCode, String code, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.code = code;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }

    private static String defaultCode(HttpStatusCode statusCode) {
        if (statusCode.value() == HttpStatus.CONFLICT.value()) {
            return "IDEMPOTENCY_CONFLICT";
        }

        return "ACCOUNT_SERVICE_UNAVAILABLE";
    }
}