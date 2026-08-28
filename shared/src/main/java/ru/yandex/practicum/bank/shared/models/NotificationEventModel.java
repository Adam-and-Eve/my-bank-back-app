package ru.yandex.practicum.bank.shared.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.yandex.practicum.bank.shared.interfaces.ValidNotificationEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * <summary>
 * Модель события уведомления, содержащая данные об операции для отправки сообщения клиенту.
 * </summary>
 */
@ValidNotificationEvent
public record NotificationEventModel (

        /**
         * <summary>
         * Уникальный идентификатор события уведомления.
         * </summary>
         */
        @NotNull
        UUID eventId,

        /**
         * <summary>
         * Уникальный идентификатор банковской операции, инициировавшей уведомление.
         * </summary>
         */
        @NotNull
        UUID operationId,

        /**
         * <summary>
         * Источник возникновения уведомления (касса, счет, перевод).
         * </summary>
         */
        @NotNull
        NotificationSourceEnumModel source,

        /**
         * <summary>
         * Тип события, по которому сформировано уведомление.
         * </summary>
         */
        @NotNull
        NotificationTypeEnumModel type,

        /**
         * <summary>
         * Логин клиента, которому адресовано данное уведомление.
         * </summary>
         */
        @NotBlank
        String recipientLogin,

        /**
         * <summary>
         * Текстовое сообщение уведомления для отображения пользователю.
         * </summary>
         */
        @NotBlank
        String message,

        /**
         * <summary>
         * Метка времени (дата и время), когда произошло событие.
         * </summary>
         */
        @NotNull
        Instant occurredAt,

        /**
         * <summary>
         * Сумма денежных средств, фигурирующая в операции (если применимо).
         * </summary>
         */
        @Positive
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal amount,

        /**
         * <summary>
         * Валюта операции, связанной с уведомлением (если применимо).
         * </summary>
         */
        CurrencyEnumModel currency
) {
}