package ru.yandex.practicum.bank.notification.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.notification.interfaces.NotificationService;
import ru.yandex.practicum.bank.notification.viewmodels.NotificationRequestViewModel;

/**
 * <summary>
 * Реализация сервиса управления и отправки уведомлений.
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
     * Принимает запрос на отправку уведомления и фиксирует его в логах системы.
     * </summary>
     * @param request Модель данных с параметрами отправляемого уведомления.
     **/
    @Override
    public void notify(NotificationRequestViewModel request) {
        log.info(
                "Notification accepted: recipientLogin={}, type={}, operationId={}, message={}",
                request.recipientLogin(),
                request.type(),
                request.operationId(),
                request.message()
        );
    }

    // endregion
}