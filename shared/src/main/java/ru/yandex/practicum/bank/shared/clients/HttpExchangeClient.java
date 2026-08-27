package ru.yandex.practicum.bank.shared.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.bank.shared.exceptions.ExchangeClientException;
import ru.yandex.practicum.bank.shared.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

import java.math.BigDecimal;

@Component
public class HttpExchangeClient  implements ExchangeClient {

    // region Fields

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;
    private final ResilientExecutorClient clientExecutor;

    // endregion

    // region Constructors

    @Autowired
    public HttpExchangeClient(
            RestClient.Builder restClientBuilder,
            @Value("${bank.services.exchange-service.base-url}") String exchangeServiceBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ResilientFactoryClient factoryClient
    ) {
        this(
                restClientBuilder,
                exchangeServiceBaseUrl,
                serviceTokenProvider,
                factoryClient.create("exchangeService")
        );
    }

    HttpExchangeClient(
            RestClient.Builder restClientBuilder,
            String exchangeServiceBaseUrl,
            ServiceTokenProvider serviceTokenProvider
    ) {
        this(
                restClientBuilder,
                exchangeServiceBaseUrl,
                serviceTokenProvider,
                ResilientFactoryClient.withDefaults()
        );
    }

    HttpExchangeClient(
            RestClient.Builder restClientBuilder,
            String exchangeBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ResilientExecutorClient executorClient
    ) {
        this.serviceTokenProvider = serviceTokenProvider;
        this.clientExecutor = executorClient;
        this.restClient = restClientBuilder
                .baseUrl(exchangeBaseUrl)
                .build();
    }

    // endregion

    // region Methods

    @Override
    public void updateRates(ExchangeRatesUpdateRequestViewModel request) {
        clientExecutor.execute(
                () -> {
                    updateRatesWithoutCircuitBreaker(request);

                    return null;
                },
                exception -> null
        );
    }

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