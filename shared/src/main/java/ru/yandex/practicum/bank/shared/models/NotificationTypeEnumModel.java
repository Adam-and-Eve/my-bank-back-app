package ru.yandex.practicum.bank.shared.models;

/**
 * <summary>
 * Перечисление типов уведомлений по операциям и состоянию банковского счета.
 * </summary>
 */
public enum NotificationTypeEnumModel {

    /**
     * <summary>
     * Уведомление об изменении состояния или данных банковского счета.
     * </summary>
     */
    ACCOUNT_UPDATED,

    /**
     * <summary>
     * Уведомление о внесении наличных денежных средств на счет.
     * </summary>
     */
    CASH_DEPOSITED,

    /**
     * <summary>
     * Уведомление о снятии наличных денежных средств со счета.
     * </summary>
     */
    CASH_WITHDRAWN,

    /**
     * <summary>
     * Уведомление об исходящем переводе денежных средств на другой счет.
     * </summary>
     */
    TRANSFER_OUTGOING,

    /**
     * <summary>
     * Уведомление о входящем переводе денежных средств от другого счета.
     * </summary>
     */
    TRANSFER_INCOMING
}