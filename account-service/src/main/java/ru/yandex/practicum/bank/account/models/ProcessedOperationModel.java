package ru.yandex.practicum.bank.account.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * <summary>
 * Сущность для отслеживания идемпотентных операций (ProcessedOperationModel).
 * Сохраняет статус выполнения, хеш запроса и закэшированный JSON-ответ для предотвращения дублирования операций.
 * </summary>
 **/
@Entity
@Table(name = "processed_operations")
public class ProcessedOperationModel {

    // region Fields

    /**
     * <summary>
     * Уникальный идентификатор операции (ключ идемпотентности).
     * </summary>
     **/
    @Id
    @Column(name = "operation_id", nullable = false, length = 128)
    private String operationId;

    /**
     * <summary>
     * Тип выполняемой операции.
     * </summary>
     **/
    @Column(name = "operation_type", nullable = false, length = 32)
    private String operationType;

    /**
     * <summary>
     * Хеш тела/параметров запроса для проверки неизменности полезной нагрузки при повторных вызовах.
     * </summary>
     **/
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    /**
     * <summary>
     * Текущий статус обработки операции (PROCESSING, COMPLETED, FAILED).
     * </summary>
     **/
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProcessedOperationStatusEnumModel status;

    /**
     * <summary>
     * Закэшированный ответ операции в формате JSON (заполняется при успешном завершении).
     * </summary>
     **/
    @Column(name = "response_json")
    private String responseJson;

    /**
     * <summary>
     * Дата и время создания записи (начала обработки).
     * </summary>
     **/
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * <summary>
     * Дата и время последнего обновления статуса операции.
     * </summary>
     **/
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // endregion

    // region Constructors

    protected ProcessedOperationModel() {
    }

    public ProcessedOperationModel(
            String operationId,
            String operationType,
            String requestHash,
            LocalDateTime now) {

        this.operationId = Objects.requireNonNull(operationId, "Operation ID must not be null");
        this.operationType = Objects.requireNonNull(operationType, "Operation type must not be null");
        this.requestHash = Objects.requireNonNull(requestHash, "Request hash must not be null");
        this.createdAt = Objects.requireNonNull(now, "Created at timestamp must not be null");
        this.updatedAt = now;
        this.status = ProcessedOperationStatusEnumModel.PROCESSING;
    }

    // endregion

    // region Properties

    /**
     * @return Идентификатор операции.
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * @return Тип операции.
     */
    public String getOperationType() {
        return operationType;
    }

    /**
     * @return Хеш запроса.
     */
    public String getRequestHash() {
        return requestHash;
    }

    /**
     * @return Текущий статус операции.
     */
    public ProcessedOperationStatusEnumModel getStatus() {
        return status;
    }

    /**
     * @return Закэшированный JSON ответа.
     */
    public String getResponseJson() {
        return responseJson;
    }

    /**
     * @return Время создания записи.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * @return Время последнего обновления.
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Переводит операцию в статус COMPLETED и сохраняет результат ответа.
     * </summary>
     *
     * @param responseJson Сформированный JSON-ответ.
     * @param now Время завершения операции.
     */
    public void complete(String responseJson, LocalDateTime now) {
        this.status = ProcessedOperationStatusEnumModel.COMPLETED;
        this.responseJson = responseJson;
        this.updatedAt = Objects.requireNonNull(now, "Updated at timestamp must not be null");
    }

    /**
     * <summary>
     * Переводит операцию в статус FAILED.
     * </summary>
     *
     * @param now Время перехода в статус ошибки.
     */
    public void fail(LocalDateTime now) {
        this.status = ProcessedOperationStatusEnumModel.FAILED;
        this.updatedAt = Objects.requireNonNull(now, "Updated at timestamp must not be null");
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ProcessedOperationModel that)) {
            return false;
        }

        return Objects.equals(operationId, that.operationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationId);
    }

    // endregion
}