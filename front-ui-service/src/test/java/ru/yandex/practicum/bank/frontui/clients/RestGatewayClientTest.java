package ru.yandex.practicum.bank.frontui.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayExceptionHandler;
import ru.yandex.practicum.bank.frontui.mappers.GatewayRequestMapper;
import ru.yandex.practicum.bank.frontui.viewmodels.AccountFormViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.AccountResponseViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.CashFormViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.CashOperationRequestViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.CashOperationResponseViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.RecipientResponseViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.TransferFormViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.TransferRequestViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.TransferResponseViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.UpdateAccountRequestViewModel;
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
 * преобразование экранных форм в DTO-запросы и обработку ответов.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class RestGatewayClientTest {

    // region Constants

    private static final String GATEWAY_BASE_URL = "http://localhost:8080";

    private static final String ACCESS_TOKEN = "test-access-token";

    // endregion

    // region Fields

    @Mock
    private SimpleCircuitBreaker circuitBreaker;

    @Mock
    private GatewayExceptionHandler errorHandler;

    @Mock
    private GatewayRequestMapper requestMapper;

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
                circuitBreaker,
                errorHandler,
                requestMapper
        );
    }

    // endregion

    // region Tests - transfer

    /**
     * <summary>
     * Проверяет успешное выполнение перевода денежных средств.
     * Убеждается, что используется POST-запрос с корректным URI,
     * Bearer-токеном и телом запроса, сформированным маппером.
     * </summary>
     **/
    @Test
    public void shouldTransferMoneySuccessfully() {
        var form = new TransferFormViewModel(
                "alexey",
                new BigDecimal("500.00"),
                "RUB"
        );

        var request = new TransferRequestViewModel(
                "alexey",
                new BigDecimal("500.00"),
                "RUB"
        );

        var expectedResponse = new TransferResponseViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("1500.00"),
                "RUB",
                "Перевод выполнен"
        );

        when(requestMapper.toTransferRequest(form)).thenReturn(request);

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/transfer"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
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

        verify(requestMapper).toTransferRequest(form);

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
     * Убеждается, что форма преобразуется маппером в UpdateAccountRequestViewModel
     * и отправляется POST-запросом с корректным Bearer-токеном.
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

        when(requestMapper.toUpdateAccountRequest(form)).thenReturn(request);

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

        verify(requestMapper).toUpdateAccountRequest(form);

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
     * Проверяет возвращение пустого списка, если Gateway вернул null вместо массива получателей.
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

    // region Tests - deposit

    /**
     * <summary>
     * Проверяет успешное пополнение счёта.
     * Убеждается, что запрос отправляется на правильный endpoint
     * с корректным Bearer-токеном и DTO, сформированным маппером.
     * </summary>
     **/
    @Test
    public void shouldDepositMoneySuccessfully() {
        var form = new CashFormViewModel(
                new BigDecimal("500.00"),
                "RUB"
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

        when(requestMapper.toCashOperationRequest(form)).thenReturn(request);

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/cash/deposit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
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

        verify(requestMapper).toCashOperationRequest(form);

        mockServer.verify();
    }

    // endregion

    // region Tests - withdraw

    /**
     * <summary>
     * Проверяет успешное снятие денежных средств со счёта.
     * </summary>
     **/
    @Test
    public void shouldWithdrawMoneySuccessfully() {
        var form = new CashFormViewModel(
                new BigDecimal("500.00"),
                "RUB"
        );

        var request = new CashOperationRequestViewModel(
                new BigDecimal("500.00"),
                "RUB"
        );

        var expectedResponse = new CashOperationResponseViewModel(
                new BigDecimal("500.00"),
                "RUB",
                "Счёт пополнен"
        );

        when(requestMapper.toCashOperationRequest(form)).thenReturn(request);

        executeCircuitBreakerNormally();

        mockServer.expect(requestTo(GATEWAY_BASE_URL + "/api/cash/withdraw"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {
                            "balance": 500.00,
                            "currency": "RUB",
                            "message": "Счёт пополнен"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = gatewayClient.withdraw(ACCESS_TOKEN, form);

        assertThat(response).isEqualTo(expectedResponse);

        verify(requestMapper).toCashOperationRequest(form);

        mockServer.verify();
    }

    // endregion

    // region Tests - CircuitBreaker

    /**
     * <summary>
     * Проверяет, что выполнение REST-запроса передаётся CircuitBreaker.
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

        verify(circuitBreaker).execute(any(), any());

        mockServer.verify();
    }

    // endregion

    // region Helper Methods

    private void executeCircuitBreakerNormally() {
        when(circuitBreaker.execute(any(), any())).thenAnswer(invocation -> {
            var call = invocation.getArgument(0, java.util.function.Supplier.class);

            try {
                return call.get();
            } catch (Throwable exception) {
                var fallback = invocation.getArgument(1, java.util.function.Function.class);

                return fallback.apply(exception);
            }
        });
    }

    // endregion
}