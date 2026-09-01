package ru.yandex.practicum.bank.account.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * <summary>
 * Сущность для хранения событий уведомлений (паттерн Transactional Outbox).
 * Гарантирует доставку сообщений в брокер (Kafka) даже в случае сбоев приложения
 * между сохранением бизнес-операции и отправкой сообщения.
 * </summary>
 **/
@Entity
@Table(name = "outbox_notifications")
public class OutboxNotificationModel {

    // region Fields

    /**
     * <summary>
     * Уникальный идентификатор записи в Outbox.
     * </summary>
     **/
    @Id
    private UUID id;

    /**
     * <summary>
     * Идентификатор события уведомления, передаваемый в брокер.
     * </summary>
     **/
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    /**
     * <summary>
     * Идентификатор бизнес-операции, породившей событие.
     * </summary>
     **/
    @Column(name = "operation_id", nullable = false, length = 128)
    private String operationId;

    /**
     * <summary>
     * Сериализованное представление события (JSON), готовое к отправке.
     * </summary>
     **/
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * <summary>
     * Текущий статус отправки события (PENDING, SENT, FAILED).
     * </summary>
     **/
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatusEnumModel status;

    /**
     * <summary>
     * Количество неудачных попыток отправки сообщения.
     * </summary>
     **/
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    /**
     * <summary>
     * Время следующей попытки отправки.
     * </summary>
     **/
    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    /**
     * <summary>
     * Текст последней ошибки при отправке (если была).
     * </summary>
     **/
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * <summary>
     * Дата и время создания записи.
     * </summary>
     **/
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * <summary>
     * Дата и время последнего обновления записи.
     * </summary>
     **/
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // endregion

    // region Constructors

    protected OutboxNotificationModel() {
    }

    public OutboxNotificationModel(
            UUID id,
            UUID eventId,
            String operationId,
            String payload,
            LocalDateTime now) {

        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.eventId = Objects.requireNonNull(eventId, "Event ID must not be null");
        this.operationId = Objects.requireNonNull(operationId, "Operation ID must not be null");
        this.payload = Objects.requireNonNull(payload, "Payload must not be null");
        this.createdAt = Objects.requireNonNull(now, "Created at timestamp must not be null");

        this.status = OutboxStatusEnumModel.PENDING;
        this.attemptCount = 0;
        this.updatedAt = now;
        this.nextAttemptAt = now;
    }

    // endregion

    // region Properties

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatusEnumModel getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Отмечает сообщение как успешно отправленное.
     * </summary>
     * @param now Текущее время для фиксации обновления.
     */
    public void markAsSent(LocalDateTime now) {
        this.status = OutboxStatusEnumModel.SENT;
        this.updatedAt = Objects.requireNonNull(now, "Updated at timestamp must not be null");
    }

    /**
     * <summary>
     * Фиксирует неудачную попытку отправки сообщения и планирует следующую попытку.
     * При превышении лимита попыток переводит сообщение в статус FAILED.
     * </summary>
     * @param error Описание произошедшей ошибки.
     * @param now Текущее время.
     * @param nextAttemptAt Время следующей попытки отправки.
     */
    public void recordFailure(String error, LocalDateTime now, LocalDateTime nextAttemptAt) {
        this.attemptCount++;
        this.lastError = error;
        this.updatedAt = Objects.requireNonNull(now, "Updated at timestamp must not be null");
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "Next attempt timestamp must not be null");

        if (this.attemptCount >= 5) {
            this.status = OutboxStatusEnumModel.FAILED;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof OutboxNotificationModel that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // endregion
}