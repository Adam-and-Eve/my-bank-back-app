package ru.yandex.practicum.bank.notification.services;

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

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

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
        log.info(
                "Notification accepted: eventId={}, operationId={}, source={}, type={}",
                event.eventId(),
                event.operationId(),
                event.source(),
                event.type()
        );
    }

    // endregion
}