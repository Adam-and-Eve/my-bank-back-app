package ru.yandex.practicum.bank.account.models;

/**
 * <summary>
 * Перечисление статусов обработки идемпотентных операций (ProcessedOperationStatusEnumModel).
 * </summary>
 **/
public enum ProcessedOperationStatusEnumModel {

    /**
     * <summary>
     * Операция находится в процессе выполнения.
     * </summary>
     **/
    PROCESSING,

    /**
     * <summary>
     * Операция успешно завершена.
     * </summary>
     **/
    COMPLETED,

    /**
     * <summary>
     * Операция завершилась с ошибкой.
     * </summary>
     **/
    FAILED
}