package ru.yandex.practicum.bank.frontui.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при ошибках сетевого взаимодействия фронтенд-сервиса (front-ui-service)
 * с шлюзом (gateway-service) или внешними микросервисами.
 * </summary>
 **/
public class GatewayClientException extends RuntimeException {

    // region Fields

    private final boolean technical;

    // endregion

    // region Constructors

    /**
     * <summary>
     * Инициализирует исключение бизнес-ошибки шлюза с заданным сообщением.
     * Флаг технической ошибки устанавливается в false.
     * </summary>
     * @param message Текст сообщения об ошибке.
     **/
    public GatewayClientException(String message) {
        super(message);
        this.technical = false;
    }

    /**
     * <summary>
     * Инициализирует исключение технической ошибки шлюза с заданным сообщением и исходной причиной.
     * Флаг технической ошибки устанавливается в true.
     * </summary>
     * @param message Текст сообщения об ошибке.
     * @param cause Первопричина исключения (Throwable).
     **/
    public GatewayClientException(String message, Throwable cause) {
        super(message, cause);
        this.technical = true;
    }

    // endregion

    // region Public Methods

    /**
     * <summary>
     * Возвращает признак технической ошибки (true, если сбой сетевой/инфраструктурный, иначе false).
     * </summary>
     * @return Логическое значение признака технического сбоя.
     **/
    public boolean isTechnical() {
        return technical;
    }

    // endregion
}