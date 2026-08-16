package ru.yandex.practicum.bank.shared.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.shared.viewmodels.NotificationRequestViewModel;

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
 * Проверяют корректность отправки POST-запросов, передачи Bearer-токена,
 * а также гарантированное отсутствие выброса исключений при сетевых сбоях и ошибках сервера (fail-silent поведение).
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class HttpNotificationClientTest {

    // region Constants

    private static final String BASE_URL = "http://notification-service";

    private static final String NOTIFICATION_URI = BASE_URL + "/api/notification";

    private static final String TEST_TOKEN = "secret-service-jwt-token";

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
     * Проверяет успешную отправку POST-запроса на создание уведомления с корректным Bearer-токеном и заголовками.
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
                "dmitry",
                "TRANSFER_COMPLETED",
                "Transfer completed to alexey: 500.00 RUB",
                "op-123"
        );

        assertThatCode(() -> notificationClient.notify(request))
                .doesNotThrowAnyException();

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет, что при ответе 500 Internal Server Error от сервиса уведомлений ошибка поглощается (fail-silent)
     * и не выбрасывается исключение в вызывающий слой.
     * </summary>
     **/
    @Test
    public void shouldSwallowExceptionOnHttpServerError() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(NOTIFICATION_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Internal Server Error")
                        .contentType(MediaType.TEXT_PLAIN));

        var request = new NotificationRequestViewModel(
                "dmitry",
                "TRANSFER_COMPLETED",
                "Transfer completed to alexey: 500.00 RUB",
                "op-123"
        );

        assertThatCode(() -> notificationClient.notify(request))
                .doesNotThrowAnyException();

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет, что при сетевом сбое (IOException / Connection Refused) клиент мягко гасит ошибку
     * и позволяет основному процессу перевода завершиться без сбоя.
     * </summary>
     **/
    @Test
    public void shouldSwallowExceptionOnNetworkFailure() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(NOTIFICATION_URI))
                .andRespond(withException(new IOException("Connection refused")));

        var request = new NotificationRequestViewModel(
                "dmitry",
                "TRANSFER_COMPLETED",
                "Transfer completed to alexey: 500.00 RUB",
                "op-123"
        );

        assertThatCode(() -> notificationClient.notify(request))
                .doesNotThrowAnyException();

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет, что при ошибке провайдера токенов (например, недоступен Keycloak/OAuth2 Identity Provider)
     * Circuit Breaker отрабатывает fallback и мягко поглощает исключение.
     * </summary>
     **/
    @Test
    public void shouldSwallowExceptionWhenTokenProviderFails() {
        when(serviceTokenProvider.getAccessToken()).thenThrow(new RuntimeException("OAuth2 Provider unavailable"));

        var request = new NotificationRequestViewModel(
                "dmitry",
                "TRANSFER_COMPLETED",
                "Transfer completed to alexey: 500.00 RUB",
                "op-123"
        );

        assertThatCode(() -> notificationClient.notify(request))
                .doesNotThrowAnyException();
    }

    // endregion

}