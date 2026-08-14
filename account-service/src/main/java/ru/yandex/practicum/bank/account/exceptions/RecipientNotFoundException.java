package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при отсутствии получателя перевода с указанным логином (RecipientNotFoundException).
 * </summary>
 **/
public class RecipientNotFoundException extends  RuntimeException {

    /**
     * Конструктор исключения.
     * @param login Логин получателя, который не был найден.
     */
    public RecipientNotFoundException(String login) {
        super("Recipient not found: " + login);
    }
}