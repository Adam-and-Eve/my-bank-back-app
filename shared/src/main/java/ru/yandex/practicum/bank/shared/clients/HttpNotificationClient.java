package ru.yandex.practicum.bank.shared.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.bank.shared.exceptions.NotificationClientException;
import ru.yandex.practicum.bank.shared.interfaces.NotificationClient;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.shared.viewmodels.NotificationRequestViewModel;

/**
 * <summary>
 * HTTP-клиент для взаимодействия с сервисом уведомлений (Notification Service).
 * </summary>
 **/
@Component
public class HttpNotificationClient implements NotificationClient {

    // region Fields

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;
    private final SimpleCircuitBreaker circuitBreaker;

    // endregion

    // region Constructors

    @Autowired
    public HttpNotificationClient(
            RestClient.Builder restClientBuilder,
            @Value("${bank.services.notification-service.base-url}") String notificationBaseUrl,
            ServiceTokenProvider serviceTokenProvider
    ) {
        this(
                restClientBuilder,
                notificationBaseUrl,
                serviceTokenProvider,
                SimpleCircuitBreaker.withDefaults("notificationService")
        );
    }

    HttpNotificationClient(
            RestClient.Builder restClientBuilder,
            String notificationBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            SimpleCircuitBreaker circuitBreaker
    ) {
        this.serviceTokenProvider = serviceTokenProvider;
        this.circuitBreaker = circuitBreaker;
        this.restClient = restClientBuilder
                .baseUrl(notificationBaseUrl)
                .build();
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Отправляет запрос на создание уведомления с использованием паттерна Circuit Breaker.
     * Сбои при отправке уведомлений мягко глушатся в fallback, чтобы не ломать основной бизнес-процесс перевода.
     * </summary>
     * @param request Данные запроса на отправку уведомления.
     **/
    @Override
    public void notify(NotificationRequestViewModel request) {
        circuitBreaker.execute(
                () -> {
                    notifyWithoutCircuitBreaker(request);

                    return null;
                },
                exception -> null
        );
    }

    /**
     * <summary>
     * Отправляет POST-запрос с токеном авторизации Bearer в сервис уведомлений без обертки Circuit Breaker.
     * </summary>
     * @param request Данные запроса на отправку уведомления.
     * @throws NotificationClientException При сетевых сбоях или ошибках взаимодействия с внешним сервисом.
     **/
    private void notifyWithoutCircuitBreaker(NotificationRequestViewModel request) {
        try {
            restClient.post()
                    .uri("/api/notification")
                    .headers(headers -> headers.setBearerAuth(serviceTokenProvider.getAccessToken()))
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new NotificationClientException("Notifications service request failed", exception);
        }
    }

    // endregion
}