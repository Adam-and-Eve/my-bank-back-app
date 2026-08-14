package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при отсутствии обязательного утверждения (claim) preferred_username в JWT-токене (MissingPreferredUsernameException).
 * </summary>
 **/
public class MissingPreferredUsernameException extends RuntimeException {

    /**
     * Конструктор исключения.
     */
    public MissingPreferredUsernameException() {
        super("JWT preferred_username claim is required");
    }
}