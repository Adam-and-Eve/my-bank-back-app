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

import java.math.BigDecimal;

@Component
public class HttpExchangeClient  implements ExchangeClient {

    // region Fields

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;
    private final SimpleCircuitBreaker circuitBreaker;

    // endregion

    // region Constructors

    @Autowired
    public HttpExchangeClient(
            RestClient.Builder restClientBuilder,
            @Value("${bank.services.exchange-service.base-url}") String exchangeServiceBaseUrl,
            ServiceTokenProvider serviceTokenProvider
    ) {
        this(
                restClientBuilder,
                exchangeServiceBaseUrl,
                serviceTokenProvider,
                SimpleCircuitBreaker.withDefaults("exchangeService")
        );
    }

    HttpExchangeClient(
            RestClient.Builder restClientBuilder,
            String exchangeServiceBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            SimpleCircuitBreaker circuitBreaker
    ) {
        this.serviceTokenProvider = serviceTokenProvider;
        this.circuitBreaker = circuitBreaker;
        this.restClient = restClientBuilder
                .baseUrl(exchangeServiceBaseUrl)
                .build();
    }

    // endregion

    // region Methods

    @Override
    public ConversionResponseViewModel convert(CurrencyEnumModel sourceCurrency, CurrencyEnumModel targetCurrency, BigDecimal amount) {
        return circuitBreaker.execute(
                () -> convertWithoutCircuitBreaker(sourceCurrency, targetCurrency, amount),
                this::exchangeFallback
        );
    }

    private ConversionResponseViewModel convertWithoutCircuitBreaker(
            CurrencyEnumModel sourceCurrency,
            CurrencyEnumModel targetCurrency,
            BigDecimal amount
    ) {
        try {
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
            throw new ExchangeClientException("Exchange service request failed", exception);
        }
    }

    private ConversionResponseViewModel exchangeFallback(Throwable exception) {
        if (exception instanceof ExchangeClientException exchangeClientException) {
            throw exchangeClientException;
        }

        throw new ExchangeClientException("Сервис курсов валют временно недоступен", exception);
    }

    // endregion
}