package ru.yandex.practicum.bank.frontui.exceptions;

public class RestGatewayClientException extends RuntimeException {

    // region Fields

    private final boolean technical;

    // endregion

    // region Constructors

    public RestGatewayClientException(String message) {
        super(message);
        this.technical = false;
    }

    public RestGatewayClientException(String message, Throwable cause) {
        super(message, cause);
        this.technical = true;
    }

    // endregion

    // region Methods

    public boolean isTechnical() {
        return technical;
    }

    // endregion
}