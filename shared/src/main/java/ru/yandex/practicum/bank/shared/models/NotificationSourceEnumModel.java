package ru.yandex.practicum.bank.shared.models;

/**
 * <summary>
 * Перечисление источников возникновения уведомлений в банковской системе.
 * </summary>
 */
public enum NotificationSourceEnumModel {

    /**
     * <summary>
     * Источник уведомления, связанный с изменениями состояния или настроек банковского счета.
     * </summary>
     */
    ACCOUNT,

    /**
     * <summary>
     * Источник уведомления, связанный с кассовыми операциями (внесение или снятие наличных).
     * </summary>
     */
    CASH,

    /**
     * <summary>
     * Источник уведомления, связанный с переводами денежных средств между счетами.
     * </summary>
     */
    TRANSFER
}