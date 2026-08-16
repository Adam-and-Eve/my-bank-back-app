package ru.yandex.practicum.bank.transfer.exceptions;

/**
 * <summary>
 * Исключение, возникающее при отсутствии обязательного имени пользователя (preferred_username) в контексте безопасности или JWT-токене.
 * </summary>
 **/
public class MissingPreferredUsernameException extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения со стандартным сообщением о необходимости наличия имени пользователя.
     * </summary>
     **/
    public MissingPreferredUsernameException() {
        super("Preferred username is required");
    }
}