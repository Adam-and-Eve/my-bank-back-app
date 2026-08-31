package ru.yandex.practicum.bank.shared.publishers;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.TimeoutException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.bank.shared.interfaces.NotificationEventPublisher;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * <summary>
 * Реализация издателя событий уведомлений, осуществляющая отправку сообщений в брокер Apache Kafka.
 * Включает логирование результатов отправки, сбор метрик ошибок и синхронную обработку исключений для паттерна Outbox.
 * </summary>
 */
public class KafkaNotificationEventPublisher implements NotificationEventPublisher {

    // region Fields

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationEventPublisher.class);
    private final KafkaTemplate<String, NotificationEventModel> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final String topic;

    // endregion

    // region Constructors

    /**
     * <summary>
     * Создает новый экземпляр издателя событий для Kafka.
     * </summary>
     * @param kafkaTemplate Шаблон Kafka для отправки сообщений.
     * @param meterRegistry Реестр для записи метрик (в частности, счетчиков ошибок).
     * @param topic Целевой топик Kafka, в который будут отправляться уведомления.
     */
    public KafkaNotificationEventPublisher(
            KafkaTemplate<String, NotificationEventModel> kafkaTemplate,
            MeterRegistry meterRegistry,
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.topic = topic;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Синхронно публикует событие уведомления в Kafka-топик.
     * Ожидает подтверждения (ack) от брокера. В случае ошибки выбрасывает RuntimeException,
     * чтобы вызывающий код (Relay-процесс) мог корректно отработать сбой.
     * </summary>
     * @param event Модель события уведомления NotificationEventModel для публикации.
     */
    @Override
    public void publish(NotificationEventModel event) {
        try {
            var result = kafkaTemplate.send(topic, event.recipientLogin(), event)
                    .get(10, TimeUnit.SECONDS);

            var metadata = result.getRecordMetadata();

            log.info(
                    "Notification event sent: eventId={}, operationId={}, source={}, topic={}, partition={}, offset={}",
                    event.eventId(),
                    event.operationId(),
                    event.source(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );
        } catch (ExecutionException exception) {
            var cause = exception.getCause();

            logFailure(event, cause);

            throw new RuntimeException("Kafka send failed for event: " + event.eventId(), cause);
        } catch (InterruptedException | java.util.concurrent.TimeoutException exception) {
            logFailure(event, exception);

            Thread.currentThread().interrupt();

            throw new RuntimeException("Kafka send interrupted or timed out for event: " + event.eventId(), exception);
        } catch (RuntimeException exception) {
            logFailure(event, exception);

            throw exception;
        }
    }

    /**
     * <summary>
     * Обрабатывает и логирует сбой публикации события, а также инкрементирует соответствующий счетчик метрик.
     * </summary>
     * @param event Событие, которое не удалось отправить.
     * @param exception Исключение, возникшее в процессе отправки.
     */
    private void logFailure(NotificationEventModel event, Throwable exception) {
        var errorCategory = errorCategory(exception);

        meterRegistry.counter(
                "my.bank.kafka.publication.failures",
                "source", event.source().name(),
                "topic", topic,
                "error_category", errorCategory
        ).increment();

        log.error(
                "Notification event send failed: eventId={}, operationId={}, source={}, topic={}, errorCategory={}, errorType={}",
                event.eventId(),
                event.operationId(),
                event.source(),
                topic,
                errorCategory,
                rootCause(exception).getClass().getSimpleName()
        );
    }

    /**
     * <summary>
     * Классифицирует ошибку на основе типа исходного исключения.
     * </summary>
     * @param exception Перехваченное исключение.
     * @return Строковое название категории ошибки.
     */
    private String errorCategory(Throwable exception) {
        var cause = rootCause(exception);

        if (cause instanceof TimeoutException ||
                cause instanceof java.util.concurrent.TimeoutException) {
            return "timeout";
        }

        if (cause instanceof SerializationException ||
                cause instanceof JsonProcessingException ||
                cause instanceof java.io.NotSerializableException) {
            return "serialization";
        }

        if (cause instanceof AuthenticationException ||
                cause instanceof AuthorizationException ||
                cause instanceof javax.naming.AuthenticationException) {
            return "security";
        }

        return "other";
    }

    /**
     * <summary>
     * Извлекает первопричину (root cause) из иерархии вложенных исключений.
     * </summary>
     * @param exception Верхнеуровневое исключение.
     * @return Исходное исключение.
     */
    private Throwable rootCause(Throwable exception) {
        var cause = exception;

        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause;
    }

    // endregion
}