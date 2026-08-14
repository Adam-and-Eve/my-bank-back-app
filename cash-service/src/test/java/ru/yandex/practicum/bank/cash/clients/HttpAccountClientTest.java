package ru.yandex.practicum.bank.cash.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.yandex.practicum.bank.cash.exceptions.AccountClientException;
import ru.yandex.practicum.bank.cash.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.cash.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * <summary>
 * Модульные тесты для HTTP-клиента сервиса счетов HttpAccountClient.
 * Проверяют корректность отправки HTTP-запросов на пополнение и списание средств,
 * передачи токена аутентификации, обработки ошибок сервера (4xx/5xx), сбоев сети и работы Circuit Breaker.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class HttpAccountClientTest {

    // region Constants

    private static final String BASE_URL = "http://account-service";
    private static final String DEPOSIT_URI = BASE_URL + "/api/account/internal/balance/deposit";
    private static final String WITHDRAW_URI = BASE_URL + "/api/account/internal/balance/withdraw";
    private static final String TEST_TOKEN = "secret-cash-service-jwt-token";

    // endregion

    // region Fields

    @Mock
    private ServiceTokenProvider serviceTokenProvider;

    private MockRestServiceServer mockServer;

    private ObjectMapper objectMapper;

    private HttpAccountClient accountClient;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        var restClientBuilder = RestClient.builder();

        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        objectMapper = new ObjectMapper();

        var circuitBreaker = SimpleCircuitBreaker.withDefaults("accountService");

        accountClient = new HttpAccountClient(
                restClientBuilder,
                BASE_URL,
                serviceTokenProvider,
                circuitBreaker,
                objectMapper
        );
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную отправку POST-запроса на пополнение счета (deposit) с Bearer-токеном и маппинг ответа.
     * </summary>
     **/
    @Test
    public void shouldExecuteDepositSuccessfully() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        String jsonResponse = """
                {
                    "login": "alexey",
                    "balance": "1500.00",
                    "currency": "RUB"
                }
                """;

        mockServer.expect(requestTo(DEPOSIT_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        var request = new AccountBalanceOperationRequestViewModel("alexey", new BigDecimal("500.00"), CurrencyEnumModel.RUB, "op-123");

        var result = accountClient.deposit(request);

        mockServer.verify();

        assertThat(result).isNotNull();

        assertThat(result.login()).isEqualTo("alexey");

        assertThat(result.balance()).isEqualTo(new BigDecimal("1500.00"));

        assertThat(result.currency()).isEqualTo("RUB");
    }

    /**
     * <summary>
     * Проверяет успешную отправку POST-запроса на списание средств (withdraw) с Bearer-токеном и маппинг ответа.
     * </summary>
     **/
    @Test
    public void shouldExecuteWithdrawSuccessfully() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        String jsonResponse = """
                {
                    "login": "alexey",
                    "balance": "1000.00",
                    "currency": "RUB"
                }
                """;

        mockServer.expect(requestTo(WITHDRAW_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        var request = new AccountBalanceOperationRequestViewModel("alexey", new BigDecimal("500.00"), CurrencyEnumModel.RUB, "op-124");

        var result = accountClient.withdraw(request);

        mockServer.verify();

        assertThat(result).isNotNull();

        assertThat(result.login()).isEqualTo("alexey");

        assertThat(result.balance()).isEqualTo(new BigDecimal("1000.00"));

        assertThat(result.currency()).isEqualTo("RUB");
    }

    /**
     * <summary>
     * Проверяет парсинг кастомного сообщения об ошибке из JSON-тела ответа сервиса счетов при статусе 400 Bad Request.
     * </summary>
     **/
    @Test
    public void shouldExtractErrorMessageFromJsonResponseOnHttpError() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        String errorResponseJson = """
                {
                    "code": "INSUFFICIENT_FUNDS",
                    "message": "Недостаточно средств на счете"
                }
                """;

        mockServer.expect(requestTo(WITHDRAW_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body(errorResponseJson)
                        .contentType(MediaType.APPLICATION_JSON));

        var request = new AccountBalanceOperationRequestViewModel("alexey", new BigDecimal("50000.00"), CurrencyEnumModel.RUB, "op-125");

        assertThatThrownBy(() -> accountClient.withdraw(request))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Недостаточно средств на счете");
    }

    /**
     * <summary>
     * Проверяет использование сообщения по умолчанию, если JSON-тело ошибки не содержит текстового поля message.
     * </summary>
     **/
    @Test
    public void shouldFallbackToDefaultMessageWhenErrorMessageIsBlank() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        String errorResponseJson = """
                {
                    "code": "INTERNAL_ERROR",
                    "message": "   "
                }
                """;

        mockServer.expect(requestTo(DEPOSIT_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponseJson)
                        .contentType(MediaType.APPLICATION_JSON));

        var request = new AccountBalanceOperationRequestViewModel("alexey", new BigDecimal("100.00"), CurrencyEnumModel.RUB, "op-126");

        assertThatThrownBy(() -> accountClient.deposit(request))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Account service request failed");
    }

    /**
     * <summary>
     * Проверяет использование сообщения по умолчанию, если сервис счетов вернул невалидный JSON в теле ошибки.
     * </summary>
     **/
    @Test
    public void shouldFallbackToDefaultMessageWhenResponseBodyIsMalformedJson() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(DEPOSIT_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Internal Server Error (Html or plain text)")
                        .contentType(MediaType.TEXT_PLAIN));

        var request = new AccountBalanceOperationRequestViewModel("alexey", new BigDecimal("100.00"), CurrencyEnumModel.RUB, "op-127");

        assertThatThrownBy(() -> accountClient.deposit(request))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Account service request failed");
    }

    /**
     * <summary>
     * Проверяет обработку таймаута или сетевого сбоя при выполнении запроса (RestClientException).
     * </summary>
     **/
    @Test
    public void shouldHandleNetworkFailure() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(DEPOSIT_URI))
                .andRespond(withException(new IOException("Connection refused")));

        var request = new AccountBalanceOperationRequestViewModel("alexey", new BigDecimal("100.00"), CurrencyEnumModel.RUB, "op-128");

        assertThatThrownBy(() -> accountClient.deposit(request))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Account service request failed");
    }

    /**
     * <summary>
     * Проверяет срабатывание резервного метода (fallback) при размыкании цепи Circuit Breaker или непредвиденных исключениях.
     * </summary>
     **/
    @Test
    public void shouldThrowUnavailableExceptionWhenCircuitBreakerTriggersOnGenericError() {
        when(serviceTokenProvider.getAccessToken()).thenThrow(new RuntimeException("OAuth2 Identity Provider unavailable"));

        var request = new AccountBalanceOperationRequestViewModel("alexey", new BigDecimal("100.00"), CurrencyEnumModel.RUB, "op-129");

        assertThatThrownBy(() -> accountClient.deposit(request))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Сервис счетов временно недоступен");
    }

    // endregion
}