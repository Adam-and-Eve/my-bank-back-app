package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при отсутствии аккаунта с указанным логином (AccountNotFoundException).
 * </summary>
 **/
public class AccountNotFoundException extends RuntimeException {

    /**
     * Конструктор исключения.
     * @param login Логин пользователя, аккаунт которого не был найден.
     */
    public AccountNotFoundException(String login) {
        super("Account not found: " + login);
    }
}