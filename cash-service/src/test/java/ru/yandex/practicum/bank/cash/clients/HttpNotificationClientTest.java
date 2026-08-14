package ru.yandex.practicum.bank.cash.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.cash.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.cash.viewmodels.NotificationRequestViewModel;
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * <summary>
 * Модульные тесты для HTTP-клиента сервиса уведомлений HttpNotificationClient.
 * Проверяют корректность отправки запросов на уведомление, передачу OAuth2-токена,
 * а также работу Circuit Breaker (fire-and-forget), при котором ошибки глушатся и не ломают вызывающий код.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class HttpNotificationClientTest {

    // region Constants

    private static final String BASE_URL = "http://notification-service";

    private static final String NOTIFICATION_URI = BASE_URL + "/api/notification";

    private static final String TEST_TOKEN = "secret-cash-service-jwt-token";

    // endregion

    // region Fields

    @Mock
    private ServiceTokenProvider serviceTokenProvider;

    private MockRestServiceServer mockServer;

    private HttpNotificationClient notificationClient;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        var restClientBuilder = RestClient.builder();

        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        var circuitBreaker = SimpleCircuitBreaker.withDefaults("notificationService");

        notificationClient = new HttpNotificationClient(
                restClientBuilder,
                BASE_URL,
                serviceTokenProvider,
                circuitBreaker
        );
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную отправку POST-запроса на создание уведомления с Bearer-токеном.
     * </summary>
     **/
    @Test
    public void shouldSendNotificationSuccessfully() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(NOTIFICATION_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
                .andRespond(withSuccess());

        var request = new NotificationRequestViewModel(
                "alexey",
                "CASH_DEPOSIT",
                "Счёт пополнен на 500.00 RUB",
                "op-123"
        );

        assertThatCode(() -> notificationClient.notify(request))
                .doesNotThrowAnyException();

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет, что при HTTP-ошибке 500 от сервиса уведомлений исключение глушится (fire-and-forget fallback).
     * </summary>
     **/
    @Test
    public void shouldNotThrowExceptionWhenNotificationServiceReturnsHttpError() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(NOTIFICATION_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        var request = new NotificationRequestViewModel(
                "alexey",
                "CASH_DEPOSIT",
                "Счёт пополнен на 500.00 RUB",
                "op-123"
        );

        assertThatCode(() -> notificationClient.notify(request))
                .doesNotThrowAnyException();
    }

    /**
     * <summary>
     * Проверяет, что при сетевом сбое (IOException) исключение глушится и не выбрасывается наружу.
     * </summary>
     **/
    @Test
    public void shouldNotThrowExceptionWhenNetworkFailureOccurs() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(NOTIFICATION_URI))
                .andRespond(withException(new IOException("Connection refused")));

        var request = new NotificationRequestViewModel(
                "alexey",
                "CASH_DEPOSIT",
                "Счёт пополнен на 500.00 RUB",
                "op-123"
        );

        assertThatCode(() -> notificationClient.notify(request))
                .doesNotThrowAnyException();
    }

    /**
     * <summary>
     * Проверяет, что при сбое получения OAuth2-токена Circuit Breaker перехватывает ошибку и не ломает выполнение.
     * </summary>
     **/
    @Test
    public void shouldNotThrowExceptionWhenTokenProviderFails() {
        when(serviceTokenProvider.getAccessToken()).thenThrow(new RuntimeException("OAuth2 Identity Provider unavailable"));

        var request = new NotificationRequestViewModel(
                "alexey",
                "CASH_DEPOSIT",
                "Счёт пополнен на 500.00 RUB",
                "op-123"
        );

        assertThatCode(() -> notificationClient.notify(request))
                .doesNotThrowAnyException();
    }

    // endregion
}