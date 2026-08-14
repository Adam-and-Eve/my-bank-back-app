package ru.yandex.practicum.bank.cash.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.bank.cash.exceptions.NotificationClientException;
import ru.yandex.practicum.bank.cash.interfaces.NotificationClient;
import ru.yandex.practicum.bank.cash.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.cash.viewmodels.NotificationRequestViewModel;
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;

/**
 * <summary>
 * Клиент для взаимодействия с сервисом счетов (Accounts Service).
 * Определяет контракт для выполнения финансовых операций пополнения и списания средств со счетов пользователей.
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
            @Value("${bank.services.notification.base-url}") String notificationBaseUrl,
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
     * Отправляет запрос на создание уведомления с оберткой в Circuit Breaker.
     * При сбоях сервиса уведомлений ошибка глушится (fallback возвращает null), чтобы не блокировать основную бизнес-операцию.
     * </summary>
     * @param request Модель запроса на отправку уведомления NotificationRequestViewModel.
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
     * Выполняет прямой HTTP POST-запрос к сервису уведомлений с добавлением OAuth2-токена авторизации.
     * </summary>
     * @param request Модель запроса на отправку уведомления NotificationRequestViewModel.
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
            throw new NotificationClientException("Notification service request failed", exception);
        }
    }

    // endregion
}