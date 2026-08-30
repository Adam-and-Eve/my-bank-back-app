package ru.yandex.practicum.bank.notification.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.notification.interfaces.NotificationService;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

/**
 * <summary>
 * Реализация сервиса управления и отправки уведомлений.
 * На данный момент выполняет роль обработчика, фиксируя метаданные
 * входящих асинхронных событий в системных логах для их последующего аудита.
 * </summary>
 **/
@Service
public class NotificationServiceImpl implements NotificationService {

    // region Fields

    private final MeterRegistry meterRegistry;

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    // endregion

    // region Constructors

    public NotificationServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Принимает событие уведомления и надежно фиксирует его атрибуты в логах системы.
     * </summary>
     * @param event Модель события уведомления, извлеченная из Kafka (содержит идентификаторы, источник и тип).
     **/
    @Override
    public void notify(NotificationEventModel event) {
        try {
            if (event == null || event.recipientLogin() == null) {
                recordFailure("unknown");

                log.warn("Notification rejected due to invalid payload");

                return;
            }

            log.info(
                    "Notification accepted: eventId={}, operationId={}, source={}, type={}",
                    event.eventId(),
                    event.operationId(),
                    event.source(),
                    event.type()
            );
        } catch (Exception ex) {
            recordFailure(event != null ? event.recipientLogin() : "unknown");

            log.error("Notification processing failed: eventId={}", event != null ? event.eventId() : null, ex);

            throw ex;
        }
    }

    private void recordFailure(String recipientLogin) {
        Counter.builder("my.bank.notification.delivery.failures")
                .tag("application", "notification-service")
                .tag("recipient_login", recipientLogin)
                .register(meterRegistry)
                .increment();
    }

    // endregion
}