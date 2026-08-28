package ru.yandex.practicum.bank.shared.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(HttpExchangeClient.class);

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

    @Override
    public ConversionResponseViewModel convert(CurrencyEnumModel sourceCurrency, CurrencyEnumModel targetCurrency, BigDecimal amount) {
        return clientExecutor.execute(
                () -> convertWithoutCircuitBreaker(sourceCurrency, targetCurrency, amount),
                this::exchangeFallback
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

    private ConversionResponseViewModel convertWithoutCircuitBreaker(
            CurrencyEnumModel sourceCurrency,
            CurrencyEnumModel targetCurrency,
            BigDecimal amount
    ) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Exchange downstream request prepared operationType=EXCHANGE currency={} targetCurrency={} source=cash-service targetService=exchange-service",
                        sourceCurrency,
                        targetCurrency
                );
            }
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/exchange/conversion")
                            .queryParam("sourceCurrency", sourceCurrency)
                            .queryParam("targetCurrency", targetCurrency)
                            .queryParam("amount", amount)
                            .build())
                    .headers(headers -> headers.setBearerAuth(serviceTokenProvider.getAccessToken()))
                    .retrieve()
                    .body(ConversionResponseViewModel.class);

            if (response == null) {
                throw new ExchangeClientException("Exchange service returned empty response");
            }

            return response;
        } catch (RestClientException exception) {
            log.error(
                    "Exchange downstream request failed operationType=EXCHANGE currency={} targetCurrency={} status=error errorCategory=downstream_unavailable errorType={} source=cash-service targetService=exchange-service",
                    sourceCurrency,
                    targetCurrency,
                    exception.getClass().getSimpleName()
            );

            throw new ExchangeClientException("Exchange service request failed", exception);
        }
    }

    private ConversionResponseViewModel exchangeFallback(Throwable exception) {
        if (exception instanceof ExchangeClientException exchangeClientException) {
            throw exchangeClientException;
        }

        log.error(
                "Exchange downstream retries exhausted status=error errorCategory=downstream_unavailable errorType={} source=cash-service targetService=exchange-service",
                exception.getClass().getSimpleName()
        );

        throw new ExchangeClientException("Exchange service is temporarily unavailable", exception);
    }

    // endregion
}