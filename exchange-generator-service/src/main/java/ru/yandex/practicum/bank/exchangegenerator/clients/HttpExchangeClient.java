package ru.yandex.practicum.bank.exchangegenerator.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.bank.exchangegenerator.exceptions.ExchangeClientException;
import ru.yandex.practicum.bank.exchangegenerator.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

/**
 * <summary>
 * HTTP-клиент для взаимодействия с сервисом курсов валют (Exchange Service).
 * </summary>
 **/
@Component
public class HttpExchangeClient implements ExchangeClient {

    // region Fields

    private final RestClient restClient;

    private final SimpleCircuitBreaker circuitBreaker;

    private final ServiceTokenProvider serviceTokenProvider;

    // endregion

    // region Constructors

    @Autowired
    public HttpExchangeClient(
        RestClient.Builder restClientBuilder,
        @Value("${bank.services.exchange.base-url}") String exchangeBaseUrl,
        ServiceTokenProvider serviceTokenProvider
    ) {
        this (
                restClientBuilder,
                exchangeBaseUrl,
                SimpleCircuitBreaker.withDefaults("exchangeService"),
                serviceTokenProvider
        );
    }

    HttpExchangeClient(
            RestClient.Builder restClientBuilder,
            String exchangeBaseUrl,
            SimpleCircuitBreaker circuitBreaker,
            ServiceTokenProvider serviceTokenProvider
    ) {
        this.restClient = restClientBuilder
                .baseUrl(exchangeBaseUrl)
                .build();
        this.circuitBreaker = circuitBreaker;

        this.serviceTokenProvider = serviceTokenProvider;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Отправляет запрос на обновление курсов валют с использованием паттерна Circuit Breaker.
     * Сбои при обновлении курсов мягко глушатся в fallback.
     * </summary>
     * @param request Данные запроса на обновление курсов валют.
     **/
    @Override
    public void updateRates(ExchangeRatesUpdateRequestViewModel request) {
        circuitBreaker.execute(
                () -> {
                    updateRatesWithoutCircuitBreaker(request);
                    return null;
                },
                exception -> null
        );
    }

    /**
     * <summary>
     * Отправляет PUT-запрос с токеном авторизации Bearer в сервис курсов валют
     * без обертки Circuit Breaker.
     * </summary>
     * @param request Данные запроса на обновление курсов валют.
     * @throws ExchangeClientException При сетевых сбоях или ошибках взаимодействия с сервисом курсов валют.
     **/
    private void updateRatesWithoutCircuitBreaker(ExchangeRatesUpdateRequestViewModel request) {
        try {
            restClient.put()
                    .uri("/api/exchange/rates")
                    .headers(headers -> headers.setBearerAuth(serviceTokenProvider.getAccessToken()))
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ExchangeClientException("Exchange service request failed", exception);
        }
    }

    // endregion
}