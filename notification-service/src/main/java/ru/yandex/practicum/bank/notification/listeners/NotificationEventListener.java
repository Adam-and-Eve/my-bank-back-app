package ru.yandex.practicum.bank.notification.listeners;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.notification.interfaces.NotificationService;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

/**
 * <summary>
 * Слушатель (консьюмер) событий Kafka для обработки входящих асинхронных уведомлений.
 * Выполняет чтение сообщений из топика, их JSR-380 валидацию и передачу в слой бизнес-логики.
 * </summary>
 **/
@Component
public class NotificationEventListener {

    // region Fields

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final Validator validator;

    // endregion

    // region Constructors

    public NotificationEventListener(NotificationService notificationService, Validator validator) {
        this.notificationService = notificationService;
        this.validator = validator;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Обрабатывает входящее сообщение из Kafka-топика уведомлений.
     * Валидирует payload, логирует метаданные (партицию, смещение) и передает событие в сервис.
     * </summary>
     * @param record Запись из Kafka, содержащая строковый ключ и значение NotificationEventModel.
     * @throws ConstraintViolationException Если входящее сообщение содержит некорректные данные
     *         (перехватывается глобальным DefaultErrorHandler и отправляется в DLT без ретраев).
     **/
    @KafkaListener(topics = "${bank.kafka.notification-topic}")
    public void listen(ConsumerRecord<String, NotificationEventModel> record) {
        var event = record.value();

        var violations = validator.validate(event);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        log.info(
                "Kafka notification received: topic={}, partition={}, offset={}, eventId={}",
                record.topic(),
                record.partition(),
                record.offset(),
                event.eventId()
        );

        notificationService.notify(event);
    }

    // endregion
}