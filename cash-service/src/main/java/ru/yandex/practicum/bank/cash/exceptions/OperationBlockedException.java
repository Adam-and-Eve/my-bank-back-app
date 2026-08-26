package ru.yandex.practicum.bank.cash.exceptions;

public class OperationBlockedException extends RuntimeException {

    public OperationBlockedException(String message) {
        super(message);
    }
}