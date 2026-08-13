package ru.yandex.practicum.bank.notification.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.bank.notification.interfaces.NotificationService;
import ru.yandex.practicum.bank.notification.viewmodels.NotificationRequestViewModel;
import ru.yandex.practicum.bank.notification.viewmodels.NotificationResponseViewModel;

/**
 * <summary>
 * REST-контроллер для обработки входящих запросов на отправку уведомлений.
 * </summary>
 **/
@RestController
public class NotificationController {

    // region Fields

    private final NotificationService notificationService;

    // endregion

    // region Constructors

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // endregion

    // region Actions

    /**
     * <summary>
     * Принимает и отправляет в обработку запрос на создание нового уведомления.
     * </summary>
     * @param request Модель данных запроса на отправку уведомления.
     * <return>
     * @return Модель ответа NotificationResponseViewModel со статусом ACCEPTED.
     * </return>
     **/
    @PostMapping("/api/notification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationResponseViewModel createNotification(
            @Valid @RequestBody NotificationRequestViewModel request) {
        notificationService.notify(request);

        return new NotificationResponseViewModel("ACCEPTED");
    }

    // endregion
}