package ru.yandex.practicum.bank.account.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при попытке выполнить транзакцию или операцию в валюте,
 * которая не совпадает с базовой валютой счета (например, пополнение рублевого счета в долларах).
 * </summary>
 **/
public class CurrencyMismatchException extends RuntimeException {

    // region Constructors

    /**
     * <summary>
     * Инициализирует исключение со стандартным сообщением об ошибке несовпадения валют.
     * </summary>
     **/
    public CurrencyMismatchException() {
        super("Валюта операции не совпадает с валютой счёта");
    }

    // endregion
}