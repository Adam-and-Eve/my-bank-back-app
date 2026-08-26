package ru.yandex.practicum.bank.shared.models;

/**
 * <summary>
 * Перечисление типов операций, выполняемых с банковским счетом.
 * </summary>
 */
public enum OperationTypeEnumModel {
    /**
    * <summary>
    * Операция пополнения банковского счета.
    * </summary>
    */
    DEPOSIT,

    /**
    * <summary>
    * Операция снятия денежных средств с банковского счета.
    * </summary>
    */
    WITHDRAW,

    /**
    * <summary>
    * Операция перевода денежных средств между банковскими счетами.
    * </summary>
    */
    TRANSFER
}