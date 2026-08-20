package ru.yandex.practicum.bank.frontui.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.frontui.mappers.GatewayRequestMapper;
import ru.yandex.practicum.bank.frontui.viewmodels.*;
import ru.yandex.practicum.bank.shared.clients.ResilientExecutorClient;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * <summary>
 * Юнит-тесты REST-клиента API Gateway (RestGatewayClient).
 * Проверяют выполнение HTTP-запросов к API Gateway, передачу Bearer-токена,
 * передачу Idempotency-Key, преобразование запросов и обработку ответов.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class RestGatewayClientTest {

    // region Constants

    private static final String GATEWAY_BASE_URL = "http://localhost:8080";

    private static final String ACCESS_TOKEN = "test-access-token";

    private static final String IDEMPOTENCY_KEY = "test-idempotency-key";

    // endregion

    // region Fields

    @Mock
    private ResilientExecutorClient clientExecutor;

    @Mock
    private GatewayRequestMapper gatewayRequestMapper;

    private MockRestServiceServer mockServer;

    private RestGatewayClient gatewayClient;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        var restClientBuilder = RestClient.builder();

        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        gatewayClient = new RestGatewayClient(
                restClientBuilder,
                GATEWAY_BASE_URL,
                clientExecutor,
                gatewayRequestMapper,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    // endregion

    // region Tests - transfer

    /**
     * <summary>
     * Проверяет успешное выполнение перевода денежных средств.
     * Убеждается, что используется POST-запрос с корректным URI,
     * Bearer-токеном и Idempotency-Key, а форма преобразуется
     * в DTO запроса через GatewayRequestMapper.
     * </summary>
     **/
    @Test
    public void shouldTransferMoneySuccessfully() {
        var form = new TransferFormViewModel(
                "alexey",
                new BigDecimal("500.00"),
                "USD",
                "RUB",
                IDEMPOTENCY_KEY
        );

        var request = new TransferRequestViewModel(
                "alexey",
                new BigDecimal("500.00"),
                "USD",
                "RUB"
        );

        var expectedResponse = new TransferResponseViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("1500.00"),
                "RUB",
                "Перевод выполнен"
        );

        when(gatewayRequestMapper.toTransferRequest(form))
                .thenReturn(request);

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/transfer"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {
                            "senderLogin": "dmitry",
                            "recipientLogin": "alexey",
                            "senderBalance": 1500.00,
                            "currency": "RUB",
                            "message": "Перевод выполнен"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = gatewayClient.transfer(ACCESS_TOKEN, form);

        assertThat(response).isEqualTo(expectedResponse);

        verify(gatewayRequestMapper).toTransferRequest(form);

        mockServer.verify();
    }

    // endregion

    // region Tests - getAccount

    /**
     * <summary>
     * Проверяет успешное получение данных текущего аккаунта.
     * </summary>
     **/
    @Test
    public void shouldReturnAccountSuccessfully() {
        var expectedResponse = new AccountResponseViewModel(
                "dmitry",
                "Дмитрий Волков",
                LocalDate.of(1999, 10, 19),
                new BigDecimal("1000.00"),
                "RUB"
        );

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/account/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("""
                        {
                            "login": "dmitry",
                            "name": "Дмитрий Волков",
                            "birthdate": "1999-10-19",
                            "balance": 1000.00,
                            "currency": "RUB"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = gatewayClient.getAccount(ACCESS_TOKEN);

        assertThat(response).isEqualTo(expectedResponse);

        mockServer.verify();
    }

    // endregion

    // region Tests - updateAccount

    /**
     * <summary>
     * Проверяет успешное обновление данных аккаунта.
     * Убеждается, что форма преобразуется в DTO запроса
     * через GatewayRequestMapper и отправляется PUT-запросом
     * с корректным Bearer-токеном.
     * </summary>
     **/
    @Test
    public void shouldUpdateAccountSuccessfully() {
        var form = new AccountFormViewModel(
                "Дмитрий Обновлённый",
                LocalDate.of(1999, 10, 19)
        );

        var request = new UpdateAccountRequestViewModel(
                "Дмитрий Обновлённый",
                LocalDate.of(1999, 10, 19)
        );

        var expectedResponse = new AccountResponseViewModel(
                "dmitry",
                "Дмитрий Обновлённый",
                LocalDate.of(1999, 10, 19),
                new BigDecimal("1000.00"),
                "RUB"
        );

        when(gatewayRequestMapper.toUpdateAccountRequest(form))
                .thenReturn(request);

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/account/me"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {
                            "login": "dmitry",
                            "name": "Дмитрий Обновлённый",
                            "birthdate": "1999-10-19",
                            "balance": 1000.00,
                            "currency": "RUB"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = gatewayClient.updateAccount(ACCESS_TOKEN, form);

        assertThat(response).isEqualTo(expectedResponse);

        verify(gatewayRequestMapper).toUpdateAccountRequest(form);

        mockServer.verify();
    }

    // endregion

    // region Tests - getRecipients

    /**
     * <summary>
     * Проверяет успешное получение списка получателей.
     * </summary>
     **/
    @Test
    public void shouldReturnRecipientsSuccessfully() {
        var expectedRecipients = List.of(
                new RecipientResponseViewModel("alexey", "Алексей Морозов"),
                new RecipientResponseViewModel("ivan", "Иван Петров")
        );

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/account/recipients"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("""
                        [
                            {
                                "login": "alexey",
                                "name": "Алексей Морозов"
                            },
                            {
                                "login": "ivan",
                                "name": "Иван Петров"
                            }
                        ]
                        """, MediaType.APPLICATION_JSON));

        var recipients = gatewayClient.getRecipients(ACCESS_TOKEN);

        assertThat(recipients)
                .containsExactlyElementsOf(expectedRecipients);

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет возвращение пустого списка получателей,
     * если Gateway возвращает пустой JSON-массив.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyListWhenThereAreNoRecipients() {
        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/account/recipients"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var recipients = gatewayClient.getRecipients(ACCESS_TOKEN);

        assertThat(recipients).isEmpty();

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет возвращение пустого списка, если Gateway вернул null.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyListWhenGatewayReturnsNullRecipients() {
        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/account/recipients"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        var recipients = gatewayClient.getRecipients(ACCESS_TOKEN);

        assertThat(recipients).isEmpty();

        mockServer.verify();
    }

    // endregion

    // region Tests - exchange rates

    /**
     * <summary>
     * Проверяет успешное получение курсов валют.
     * </summary>
     **/
    @Test
    public void shouldReturnExchangeRatesSuccessfully() {
        var updatedAt = Instant.parse("2026-08-20T10:00:00Z");

        var expectedRates = List.of(
                new ExchangeRateResponseViewModel(
                        CurrencyEnumModel.USD,
                        new BigDecimal("80.00"),
                        new BigDecimal("82.00"),
                        updatedAt
                ),
                new ExchangeRateResponseViewModel(
                        CurrencyEnumModel.CNY,
                        new BigDecimal("92.00"),
                        new BigDecimal("94.00"),
                        updatedAt
                )
        );

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/exchange/rates"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("""
                        [
                            {
                                "currency": "USD",
                                "buyRate": "80.00",
                                "sellRate": "82.00",
                                "updatedAt": "2026-08-20T10:00:00Z"
                            },
                            {
                                "currency": "CNY",
                                "buyRate": "92.00",
                                "sellRate": "94.00",
                                "updatedAt": "2026-08-20T10:00:00Z"
                            }
                        ]
                        """, MediaType.APPLICATION_JSON));

        var rates = gatewayClient.getExchangeRates(ACCESS_TOKEN);

        assertThat(rates).containsExactlyElementsOf(expectedRates);

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет возвращение пустого списка курсов,
     * если Gateway возвращает пустой JSON-массив.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyListWhenThereAreNoExchangeRates() {
        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/exchange/rates"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var rates = gatewayClient.getExchangeRates(ACCESS_TOKEN);

        assertThat(rates).isEmpty();

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет возвращение пустого списка курсов,
     * если Gateway возвращает null вместо массива.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyListWhenGatewayReturnsNullExchangeRates() {
        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/exchange/rates"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        var rates = gatewayClient.getExchangeRates(ACCESS_TOKEN);

        assertThat(rates).isEmpty();

        mockServer.verify();
    }

    // endregion

    // region Tests - deposit

    /**
     * <summary>
     * Проверяет успешное пополнение счёта.
     * Убеждается, что запрос отправляется на правильный endpoint
     * с корректным Bearer-токеном и Idempotency-Key,
     * а форма преобразуется в DTO запроса через GatewayRequestMapper.
     * </summary>
     **/
    @Test
    public void shouldDepositMoneySuccessfully() {
        var form = new CashFormViewModel(
                new BigDecimal("500.00"),
                "RUB",
                IDEMPOTENCY_KEY
        );

        var request = new CashOperationRequestViewModel(
                new BigDecimal("500.00"),
                "RUB"
        );

        var expectedResponse = new CashOperationResponseViewModel(
                new BigDecimal("1500.00"),
                "RUB",
                "Счёт пополнен"
        );

        when(gatewayRequestMapper.toCashOperationRequest(form))
                .thenReturn(request);

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/cash/deposit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {
                            "balance": 1500.00,
                            "currency": "RUB",
                            "message": "Счёт пополнен"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = gatewayClient.deposit(ACCESS_TOKEN, form);

        assertThat(response).isEqualTo(expectedResponse);

        verify(gatewayRequestMapper).toCashOperationRequest(form);

        mockServer.verify();
    }

    // endregion

    // region Tests - withdraw

    /**
     * <summary>
     * Проверяет успешное снятие денежных средств со счёта.
     * Убеждается, что запрос отправляется на правильный endpoint
     * с корректным Bearer-токеном и Idempotency-Key,
     * а форма преобразуется в DTO запроса через GatewayRequestMapper.
     * </summary>
     **/
    @Test
    public void shouldWithdrawMoneySuccessfully() {
        var form = new CashFormViewModel(
                new BigDecimal("500.00"),
                "RUB",
                IDEMPOTENCY_KEY
        );

        var request = new CashOperationRequestViewModel(
                new BigDecimal("500.00"),
                "RUB"
        );

        var expectedResponse = new CashOperationResponseViewModel(
                new BigDecimal("500.00"),
                "RUB",
                "Счёт снят"
        );

        when(gatewayRequestMapper.toCashOperationRequest(form))
                .thenReturn(request);

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/cash/withdraw"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {
                            "balance": 500.00,
                            "currency": "RUB",
                            "message": "Счёт снят"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = gatewayClient.withdraw(ACCESS_TOKEN, form);

        assertThat(response).isEqualTo(expectedResponse);

        verify(gatewayRequestMapper).toCashOperationRequest(form);

        mockServer.verify();
    }

    // endregion

    // region Tests - CircuitBreaker

    /**
     * <summary>
     * Проверяет, что выполнение REST-запроса передаётся ResilientExecutorClient.
     * </summary>
     **/
    @Test
    public void shouldExecuteRequestThroughCircuitBreaker() {
        var expectedResponse = new AccountResponseViewModel(
                "dmitry",
                "Дмитрий Волков",
                LocalDate.of(1999, 10, 19),
                new BigDecimal("1000.00"),
                "RUB"
        );

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/account/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("""
                        {
                            "login": "dmitry",
                            "name": "Дмитрий Волков",
                            "birthdate": "1999-10-19",
                            "balance": 1000.00,
                            "currency": "RUB"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = gatewayClient.getAccount(ACCESS_TOKEN);

        assertThat(response).isEqualTo(expectedResponse);

        verify(clientExecutor).execute(any(), any());

        mockServer.verify();
    }

    // endregion

    // region Helper Methods

    private void executeCircuitBreakerNormally() {
        when(clientExecutor.execute(any(), any()))
                .thenAnswer(invocation -> {
                    var call = invocation.getArgument(0, Supplier.class);

                    try {
                        return call.get();
                    } catch (Throwable exception) {
                        var fallback = invocation.getArgument(1, Function.class);

                        return fallback.apply(exception);
                    }
                });
    }

    // endregion
}