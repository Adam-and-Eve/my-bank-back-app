package ru.yandex.practicum.bank.cash.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при отсутствии обязательного утверждения (claim) preferred_username в JWT-токене.
 * Возникает, если из входящего токена аутентификации невозможно извлечь имя пользователя.
 * </summary>
 **/
public class MissingPreferredUsernameException extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения MissingPreferredUsernameException с дефолтным сообщением об ошибке.
     * </summary>
     **/
    public MissingPreferredUsernameException() {
        super("JWT preferred_username claim is required");
    }
}