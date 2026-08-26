package ru.yandex.practicum.bank.shared.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.shared.exceptions.BlockerClientException;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;

import java.io.IOException;
import java.math.BigDecimal;

import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * <summary>
 * Модульные тесты для HTTP-клиента сервиса блокировки операций HttpBlockerClient.
 * Проверяют корректность отправки HTTP-запросов на проверку операций,
 * передачи токена аутентификации, маппинга ответов, обработки пустых ответов,
 * сетевых ошибок и работы Circuit Breaker.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class HttpBlockerClientTest {

    // region Constants

    private static final String BASE_URL = "http://blocker-service";

    private static final String CHECK_URI = BASE_URL + "/api/blocker/check";

    private static final String TEST_TOKEN = "secret-cash-service-jwt-token";

    // endregion

    // region Fields

    @Mock
    private ServiceTokenProvider serviceTokenProvider;

    private MockRestServiceServer mockServer;

    private HttpBlockerClient blockerClient;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        var restClientBuilder = RestClient.builder();

        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        var circuitBreaker = SimpleCircuitBreaker.withDefaults("blockerService");

        blockerClient = new HttpBlockerClient(
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
     * Проверяет успешную отправку POST-запроса на проверку операции
     * с Bearer-токеном и корректный маппинг ответа Blocker Service.
     * </summary>
     **/
    @Test
    public void shouldCheckOperationSuccessfully() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        String jsonResponse = """
                {
                    "allowed": true,
                    "reason": "Операция разрешена"
                }
                """;

        mockServer.expect(requestTo(CHECK_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + TEST_TOKEN
                ))
                .andRespond(withSuccess(
                        jsonResponse,
                        MediaType.APPLICATION_JSON
                ));

        var request = createRequest();

        var result = blockerClient.check(request);

        mockServer.verify();

        assertThat(result).isNotNull();
        assertThat(result.allowed()).isTrue();
        assertThat(result.reason())
                .isEqualTo("Операция разрешена");
    }

    /**
     * <summary>
     * Проверяет корректную передачу всех данных операции в теле POST-запроса
     * в Blocker Service, включая исходную и нормализованную суммы.
     * </summary>
     **/
    @Test
    public void shouldSendCorrectRequestBody() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        String jsonResponse = """
                {
                    "allowed": true,
                    "reason": null
                }
                """;

        mockServer.expect(requestTo(CHECK_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + TEST_TOKEN
                ))
                .andExpect(content().json("""
                        {
                            "operationId": "op-123",
                            "operationType": "DEPOSIT",
                            "login": "alexey",
                            "sender": null,
                            "recipient": null,
                            "amount": 500.00,
                            "currency": "USD",
                            "normalizedAmount": 47500.00,
                            "baseCurrency": "RUB"
                        }
                        """))
                .andRespond(withSuccess(
                        jsonResponse,
                        MediaType.APPLICATION_JSON
                ));

        blockerClient.check(createRequest());

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет успешную передачу операции в базовой валюте,
     * когда исходная и нормализованная валюты совпадают.
     * </summary>
     **/
    @Test
    public void shouldSendRubOperationWithoutCurrencyConversion() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        var request = new OperationCheckRequestViewModel(
                "op-rub-123",
                OperationTypeEnumModel.WITHDRAW,
                "alexey",
                null,
                null,
                new BigDecimal("500.00"),
                CurrencyEnumModel.RUB,
                new BigDecimal("500.00"),
                CurrencyEnumModel.RUB
        );

        mockServer.expect(requestTo(CHECK_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + TEST_TOKEN
                ))
                .andExpect(content().json("""
                        {
                            "operationId": "op-rub-123",
                            "operationType": "WITHDRAW",
                            "login": "alexey",
                            "sender": null,
                            "recipient": null,
                            "amount": 500.00,
                            "currency": "RUB",
                            "normalizedAmount": 500.00,
                            "baseCurrency": "RUB"
                        }
                        """))
                .andRespond(withSuccess(
                        """
                        {
                            "allowed": true,
                            "reason": null
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var result = blockerClient.check(request);

        mockServer.verify();

        assertThat(result.allowed()).isTrue();
        assertThat(result.reason()).isNull();
    }

    /**
     * <summary>
     * Проверяет передачу операции в иностранной валюте с нормализованной
     * суммой в базовой валюте RUB.
     * </summary>
     **/
    @Test
    public void shouldSendForeignCurrencyOperationWithNormalizedAmount() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        var request = new OperationCheckRequestViewModel(
                "op-usd-123",
                OperationTypeEnumModel.DEPOSIT,
                "alexey",
                null,
                null,
                new BigDecimal("100.00"),
                CurrencyEnumModel.USD,
                new BigDecimal("9500.00"),
                CurrencyEnumModel.RUB
        );

        mockServer.expect(requestTo(CHECK_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + TEST_TOKEN
                ))
                .andExpect(content().json("""
                        {
                            "operationId": "op-usd-123",
                            "operationType": "DEPOSIT",
                            "login": "alexey",
                            "sender": null,
                            "recipient": null,
                            "amount": 100.00,
                            "currency": "USD",
                            "normalizedAmount": 9500.00,
                            "baseCurrency": "RUB"
                        }
                        """))
                .andRespond(withSuccess(
                        """
                        {
                            "allowed": true,
                            "reason": null
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var result = blockerClient.check(request);

        mockServer.verify();

        assertThat(result).isNotNull();
        assertThat(result.allowed()).isTrue();
        assertThat(result.reason()).isNull();
    }

    /**
     * <summary>
     * Проверяет обработку ответа Blocker Service, запрещающего выполнение операции.
     * </summary>
     **/
    @Test
    public void shouldReturnBlockedOperationResponse() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        String jsonResponse = """
                {
                    "allowed": false,
                    "reason": "Подозрительная операция"
                }
                """;

        mockServer.expect(requestTo(CHECK_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        jsonResponse,
                        MediaType.APPLICATION_JSON
                ));

        var result = blockerClient.check(createRequest());

        mockServer.verify();

        assertThat(result).isNotNull();
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason())
                .isEqualTo("Подозрительная операция");
    }

    /**
     * <summary>
     * Проверяет выброс BlockerClientException, если Blocker Service
     * возвращает пустое тело ответа.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenResponseIsEmpty() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(CHECK_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        assertThatThrownBy(() -> blockerClient.check(createRequest()))
                .isInstanceOf(BlockerClientException.class)
                .hasMessage("Blocker service returned empty response");

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет обработку HTTP-ошибки, возвращенной Blocker Service.
     * </summary>
     **/
    @Test
    public void shouldHandleHttpError() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(CHECK_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> blockerClient.check(createRequest()))
                .isInstanceOf(BlockerClientException.class)
                .hasMessage("Blocker service request failed");

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет обработку сетевого сбоя при выполнении запроса
     * к Blocker Service.
     * </summary>
     **/
    @Test
    public void shouldHandleNetworkFailure() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(CHECK_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(
                        new IOException("Connection refused")
                ));

        assertThatThrownBy(() -> blockerClient.check(createRequest()))
                .isInstanceOf(BlockerClientException.class)
                .hasMessage("Blocker service request failed");

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет срабатывание fallback Circuit Breaker при возникновении
     * непредвиденного исключения во время получения сервисного токена.
     * </summary>
     **/
    @Test
    public void shouldThrowUnavailableExceptionWhenCircuitBreakerTriggersOnGenericError() {
        when(serviceTokenProvider.getAccessToken())
                .thenThrow(new RuntimeException(
                        "OAuth2 Identity Provider unavailable"
                ));

        assertThatThrownBy(() -> blockerClient.check(createRequest()))
                .isInstanceOf(BlockerClientException.class)
                .hasMessage("Сервис проверки операций временно недоступен");
    }

    // endregion

    // region Private Methods

    private OperationCheckRequestViewModel createRequest() {
        return new OperationCheckRequestViewModel(
                "op-123",
                OperationTypeEnumModel.DEPOSIT,
                "alexey",
                null,
                null,
                new BigDecimal("500.00"),
                CurrencyEnumModel.USD,
                new BigDecimal("47500.00"),
                CurrencyEnumModel.RUB
        );
    }

    // endregion
}