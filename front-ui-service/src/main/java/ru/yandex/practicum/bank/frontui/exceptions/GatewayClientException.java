package ru.yandex.practicum.bank.frontui.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при ошибках сетевого взаимодействия фронтенд-сервиса (front-ui-service)
 * с шлюзом (gateway-service) или внешними микросервисами.
 * </summary>
 **/
public class GatewayClientException extends RuntimeException {

    /**
     * <summary>
     * Создает новое исключение с детальным сообщением об ошибке.
     * </summary>
     * @param message Сообщение, описывающее причину возникновения ошибки.
     **/
    public GatewayClientException(String message) {
        super(message);
    }

    /**
     * <summary>
     * Создает новое исключение с детальным сообщением и первопричиной (cause).
     * </summary>
     * @param message Сообщение, описывающее причину возникновения ошибки.
     * @param cause Первопричина исключения.
     **/
    public GatewayClientException(String message, Throwable cause) {
        super(message, cause);
    }
}