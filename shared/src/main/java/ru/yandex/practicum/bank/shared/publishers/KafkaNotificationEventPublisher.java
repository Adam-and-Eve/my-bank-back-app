package ru.yandex.practicum.bank.shared.publishers;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.bank.shared.interfaces.NotificationEventPublisher;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

import java.util.Locale;

/**
 * <summary>
 * Реализация издателя событий уведомлений, осуществляющая отправку сообщений в брокер Apache Kafka.
 * Включает логирование результатов отправки и сбор метрик ошибок.
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
     * @param kafkaTemplate Шаблон Kafka для асинхронной отправки сообщений.
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
     * Асинхронно публикует событие уведомления в Kafka-топик.
     * В случае успешной отправки логирует метаданные (партицию, смещение), в случае ошибки — фиксирует сбой.
     * </summary>
     * @param event Модель события уведомления NotificationEventModel для публикации.
     */
    @Override
    public void publish(NotificationEventModel event) {
        try {
            // В качестве ключа сообщения используется логин получателя (recipientLogin),
            // чтобы гарантировать порядок обработки уведомлений для одного пользователя.
            kafkaTemplate.send(topic, event.recipientLogin(), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            logFailure(event, exception);

                            return;
                        }

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
                    });
        } catch (RuntimeException exception) {
            logFailure(event, exception);
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
                "bank.kafka.publication.failures",
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
     * Классифицирует ошибку на основе типа исходного исключения (таймаут, сериализация, безопасность или другое).
     * </summary>
     * @param exception Перехваченное исключение.
     * @return Строковое название категории ошибки.
     */
    private String errorCategory(Throwable exception) {
        var cause = rootCause(exception);

        var type = cause.getClass().getSimpleName().toLowerCase(Locale.ROOT);

        if (type.contains("timeout")) {
            return "timeout";
        }

        if (type.contains("serializ")) {
            return "serialization";
        }

        if (type.contains("authoriz") || type.contains("authenticat")) {
            return "security";
        }

        return "other";
    }

    /**
     * <summary>
     * Извлекает первопричину (root cause) из иерархии вложенных исключений.
     * </summary>
     * @param exception Верхнеуровневое исключение.
     * @return Исходное исключение, спровоцировавшее сбой.
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