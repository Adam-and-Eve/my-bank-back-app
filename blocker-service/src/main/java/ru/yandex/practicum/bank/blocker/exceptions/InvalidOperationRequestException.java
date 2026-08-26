package ru.yandex.practicum.bank.blocker.exceptions;

/**
 * <summary>
 * Исключение, возникающее при получении некорректных данных
 * для проверки банковской операции.
 * </summary>
 */
public class InvalidOperationRequestException extends RuntimeException {

    /**
     * <summary>
     * Создает исключение с указанным сообщением.
     * </summary>
     * @param message Сообщение с описанием причины некорректности запроса.
     */
    public InvalidOperationRequestException(String message) {
        super(message);
    }
}