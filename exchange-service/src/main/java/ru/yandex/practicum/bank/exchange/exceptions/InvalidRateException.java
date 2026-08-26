package ru.yandex.practicum.bank.exchange.exceptions;

/**
 * <summary>
 * Исключение, выбрасываемое при указании некорректного курса валюты
 * (InvalidRateException).
 * </summary>
 **/
public class InvalidRateException extends RuntimeException {


    /**
     * Конструктор исключения.
     */
    public InvalidRateException() {
        super("Rates must be positive and have scale no more than 4");
    }
}
