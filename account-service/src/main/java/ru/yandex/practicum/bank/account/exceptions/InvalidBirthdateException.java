package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при передаче недопустимой даты рождения (InvalidBirthdateException).
 * Возникает, если возраст владельца аккаунта менее 18 лет.
 * </summary>
 **/
public class InvalidBirthdateException extends RuntimeException {

    /**
     * Конструктор исключения.
     */
    public InvalidBirthdateException() {
        super("Account owner must be at least 18 years old");
    }
}