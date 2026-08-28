package ru.yandex.practicum.bank.cash.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * <summary>
 * Исключение, выбрасываемое при сбоях в процессе взаимодействия с внешним сервисом счетов (Accounts Service).
 * Возникает при ошибках выполнения HTTP-запросов, недоступности сервиса или некорректных ответах.
 * </summary>
 **/
public class AccountClientException extends RuntimeException {

    // region Fields

    private final HttpStatusCode statusCode;
    private final String code;

    // endregion

    // region Methods

    /**
     * <summary>
     * Создает исключение с указанным сообщением.
     * По умолчанию устанавливает HTTP-статус 502 Bad Gateway и код ошибки ACCOUNT_SERVICE_UNAVAILABLE.
     * </summary>
     * @param message Текст сообщения об ошибке.
     */
    public AccountClientException(String message) {
        this(message, HttpStatus.BAD_GATEWAY, "ACCOUNT_SERVICE_UNAVAILABLE", null);
    }

    /**
     * <summary>
     * Создает исключение с указанным сообщением и исходной причиной (cause).
     * По умолчанию устанавливает HTTP-статус 502 Bad Gateway и код ошибки ACCOUNT_SERVICE_UNAVAILABLE.
     * </summary>
     * @param message Текст сообщения об ошибке.
     * @param cause Исходное исключение, спровоцировавшее сбой.
     */
    public AccountClientException(String message, Throwable cause) {
        this(message, HttpStatus.BAD_GATEWAY, "ACCOUNT_SERVICE_UNAVAILABLE", cause);
    }

    /**
     * <summary>
     * Создает исключение с указанным сообщением, HTTP-статусом ответа и исходной причиной (cause).
     * Строковый код ошибки будет вычислен автоматически на основе переданного HTTP-статуса.
     * </summary>
     * @param message Текст сообщения об ошибке.
     * @param statusCode HTTP-статус ответа от внешнего сервиса.
     * @param cause Исходное исключение, спровоцировавшее сбой.
     */
    public AccountClientException(String message, HttpStatusCode statusCode, Throwable cause) {
        this(message, statusCode, defaultCode(statusCode), cause);
    }

    /**
     * <summary>
     * Базовый конструктор для создания исключения с полным набором параметров.
     * </summary>
     * @param message Текст сообщения об ошибке.
     * @param statusCode HTTP-статус ответа от внешнего сервиса.
     * @param code Строковый код ошибки (доменный код).
     * @param cause Исходное исключение, спровоцировавшее сбой.
     */
    public AccountClientException(String message, HttpStatusCode statusCode, String code, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.code = code;
    }

    /**
     * <summary>
     * Получает HTTP-статус, связанный с ошибкой интеграции.
     * </summary>
     * @return Экземпляр HttpStatusCode.
     */
    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    /**
     * <summary>
     * Получает строковый (доменный) код ошибки.
     * </summary>
     * @return Код ошибки в виде строки.
     */
    public String getCode() {
        return code;
    }

    /**
     * <summary>
     * Вспомогательный метод для определения строкового кода ошибки по умолчанию на основе HTTP-статуса.
     * </summary>
     * @param statusCode HTTP-статус, полученный от внешнего сервиса.
     * @return Код ошибки (например, IDEMPOTENCY_CONFLICT для 409 Conflict).
     */
    private static String defaultCode(HttpStatusCode statusCode) {
        if (statusCode.value() == HttpStatus.CONFLICT.value()) {
            return "IDEMPOTENCY_CONFLICT";
        }

        return "ACCOUNT_SERVICE_UNAVAILABLE";
    }

    // endregion
}