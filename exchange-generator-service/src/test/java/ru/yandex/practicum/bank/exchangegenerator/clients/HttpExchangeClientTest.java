package ru.yandex.practicum.bank.exchangegenerator.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;

/**
 * <summary>
 * Unit-тесты HTTP-клиента для взаимодействия с сервисом курсов валют.
 * </summary>
 **/
public class HttpExchangeClientTest {

    // region Fields

    /**
     * <summary>
     * Базовый URL сервиса курсов валют, используемый в тестах.
     * </summary>
     **/
    private static final String BASE_URL = "http://exchange-service";

    /**
     * <summary>
     * Тестовый токен сервисной аутентификации.
     * </summary>
     **/
    private static final String ACCESS_TOKEN = "exchange-generator-service";

    /**
     * <summary>
     * Конструктор для создания экземпляра RestClient.
     * </summary>
     **/
    private RestClient.Builder restClientBuilder;

    /**
     * <summary>
     * Mock-сервер для проверки HTTP-запросов, отправляемых RestClient.
     * </summary>
     **/
    private MockRestServiceServer mockServer;

    /**
     * <summary>
     * Mock-провайдер сервисного OAuth2-токена.
     * </summary>
     **/
    private ServiceTokenProvider serviceTokenProvider;

    /**
     * <summary>
     * Circuit Breaker, используемый HTTP-клиентом в тестах.
     * </summary>
     **/
    private SimpleCircuitBreaker circuitBreaker;

    /**
     * <summary>
     * Тестируемый HTTP-клиент сервиса курсов валют.
     * </summary>
     **/
    private HttpExchangeClient exchangeClient;

    // endregion

    // region Setup

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();

        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        serviceTokenProvider = mock(ServiceTokenProvider.class);

        when(serviceTokenProvider.getAccessToken()).thenReturn(ACCESS_TOKEN);

        circuitBreaker = SimpleCircuitBreaker.withDefaults("exchangeServiceTest");

        exchangeClient = new HttpExchangeClient(
                restClientBuilder,
                BASE_URL,
                circuitBreaker,
                serviceTokenProvider
        );
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет отправку PUT-запроса на обновление курсов валют
     * с использованием Bearer-токена авторизации.
     * </summary>
     **/
    @Test
    void updateRates_shouldSendPutRequestWithBearerToken() {
        var request = new ExchangeRatesUpdateRequestViewModel(List.of());

        mockServer.expect(requestTo(BASE_URL + "/api/exchange/rates"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andExpect(header(AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(content().json("""
                        {
                            "rates": []
                        }
                        """))
                .andRespond(withNoContent());

        assertDoesNotThrow(() -> exchangeClient.updateRates(request));

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет обработку ошибки сервиса курсов валют через Circuit Breaker
     * без выбрасывания исключения наружу.
     * </summary>
     **/
    @Test
    void updateRates_shouldNotThrowWhenExchangeServiceReturnsError() {
        var request = new ExchangeRatesUpdateRequestViewModel(List.of());

        mockServer.expect(requestTo(BASE_URL + "/api/exchange/rates"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andExpect(header(AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andRespond(withServerError());

        assertDoesNotThrow(() -> exchangeClient.updateRates(request));

        mockServer.verify();
    }

    // endregion
}