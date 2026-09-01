package ru.yandex.practicum.bank.notification.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.support.serializer.DeserializationException;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <summary>
 * Кастомный обработчик (recoverer) сбоев доставки Kafka-уведомлений.
 * Оборачивает базовый рекаверер (например, для отправки в DLT), добавляя регистрацию
 * инцидентов (метрик отказов) в Micrometer MeterRegistry.
 * </summary>
 **/
public class NotificationDeliveryFailureRecoverer implements ConsumerRecordRecoverer {

    // region Constants

    static final String METRIC_NAME = "my.bank.notification.delivery.failures";
    private static final String UNKNOWN_RECIPIENT = "unknown";

    // endregion

    // region Fields

    private final ConsumerRecordRecoverer delegate;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    // endregion

    // region Constructors

    public NotificationDeliveryFailureRecoverer(
            ConsumerRecordRecoverer delegate,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper
    ) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Обрабатывает упавшее сообщение: делегирует основную логику вложенному обработчику
     * и увеличивает счетчик метрики сбойных доставок с тегированием по логину получателя.
     * </summary>
     * @param record Запись Kafka, обработка которой завершилась ошибкой.
     * @param exception Исключение, вызвавшее сбой обработки.
     **/
    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        delegate.accept(record, exception);

        meterRegistry.counter(
                METRIC_NAME,
                "recipient_login", recipientLogin(record, exception)
        ).increment();
    }

    // endregion

    // region Private Methods

    /**
     * <summary>
     * Пытается извлечь логин получателя из полезной нагрузки упавшего сообщения или исключения.
     * Поддерживает десериализованные объекты, сырые строки, массивы байт и данные из DeserializationException.
     * </summary>
     * @param record Упавшая запись Kafka.
     * @param exception Исключение десериализации или обработки.
     * @return Логин получателя или значение "unknown", если извлечь данные не удалось.
     **/
    private String recipientLogin(ConsumerRecord<?, ?> record, Exception exception) {
        if (record.value() instanceof NotificationEventModel event) {
            return validRecipient(event.recipientLogin());
        }

        if (record.value() instanceof byte[] data) {
            return recipientFromJson(data);
        }

        if (record.value() instanceof String data) {
            return recipientFromJson(data.getBytes(StandardCharsets.UTF_8));
        }

        var cause = exception;

        while (cause != null) {
            if (cause instanceof DeserializationException deserializationException) {
                return recipientFromJson(deserializationException.getData());
            }

            cause = cause.getCause() instanceof Exception nested ? nested : null;
        }

        return UNKNOWN_RECIPIENT;
    }

    /**
     * <summary>
     * Безопасно парсит массив байт как JSON-дерево для поиска поля recipientLogin.
     * </summary>
     * @param data JSON-данные сообщения в виде массива байт.
     * @return Извлеченный логин получателя или "unknown" при ошибке парсинга.
     **/
    private String recipientFromJson(byte[] data) {
        if (data == null || data.length == 0) {
            return UNKNOWN_RECIPIENT;
        }

        try {
            return validRecipient(objectMapper.readTree(data).path("recipientLogin").asText());
        } catch (IOException exception) {
            return UNKNOWN_RECIPIENT;
        }
    }

    /**
     * <summary>
     * Валидирует извлеченный логин на пустоту и null.
     * </summary>
     * @param recipient Проверяемый логин.
     * @return Исходный логин или "unknown", если значение некорректно.
     **/
    private String validRecipient(String recipient) {
        return recipient == null || recipient.isBlank() ? UNKNOWN_RECIPIENT : recipient;
    }

    // endregion
}