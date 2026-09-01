package ru.yandex.practicum.bank.account.models;

/**
 * <summary>
 * Перечисление статусов обработки сообщений в Outbox.
 * </summary>
 **/
public enum OutboxStatusEnumModel {

    /**
     * <summary>
     * Сообщение ожидает отправки.
     * </summary>
     **/
    PENDING,

    /**
     * <summary>
     * Сообщение успешно отправлено.
     * </summary>
     **/
    SENT,

    /**
     * <summary>
     * Превышен лимит попыток отправки, сообщение не доставлено.
     * </summary>
     **/
    FAILED
}