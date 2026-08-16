package ru.yandex.practicum.bank.transfer.exceptions;

/**
 * <summary>
 * Исключение, возникающее при попытке выполнить перевод с некорректной (неположительной) суммой.
 * </summary>
 **/
public class InvalidAmountException  extends RuntimeException {

    /**
     * <summary>
     * Создает новый экземпляр исключения со стандартным сообщением о необходимости суммы больше нуля.
     * </summary>
     **/
    public InvalidAmountException() {
        super("Amount must be greater than zero");
    }
}