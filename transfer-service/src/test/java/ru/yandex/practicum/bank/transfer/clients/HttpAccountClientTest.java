package ru.yandex.practicum.bank.transfer.clients;

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
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;
import ru.yandex.practicum.bank.transfer.exceptions.AccountClientException;
import ru.yandex.practicum.bank.transfer.mappers.AccountTransferMapper;
import ru.yandex.practicum.bank.transfer.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.transfer.viewmodels.AccountTransferRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferOperationViewModel;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
 * Проверяют корректность отправки HTTP-запросов, передачи токена аутентификации,
 * обработки ошибок сервера (4xx/5xx), сбоев сети и работы Circuit Breaker.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class HttpAccountClientTest {

    // region Constants

    private static final String BASE_URL = "http://account-service";
    private static final String TRANSFER_URI = BASE_URL + "/api/account/internal/balance/transfer";
    private static final String TEST_TOKEN = "secret-service-jwt-token";

    // endregion

    // region Fields

    @Mock
    private ServiceTokenProvider serviceTokenProvider;

    @Mock
    private AccountTransferMapper accountTransferMapper;

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

        lenient().when(accountTransferMapper.toAccountRequest(any())).thenReturn(
                new AccountTransferRequestViewModel("dmitry", "alexey", new BigDecimal("500.00"), CurrencyEnumModel.RUB, "op-123")
        );

        accountClient = new HttpAccountClient(
                restClientBuilder,
                BASE_URL,
                serviceTokenProvider,
                circuitBreaker,
                objectMapper,
                accountTransferMapper
        );
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную отправку POST-запроса с Bearer-токеном и корректный маппинг ответа от сервиса счетов.
     * </summary>
     **/
    @Test
    public void shouldExecuteTransferSuccessfully() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        String jsonResponse = """
                {
                    "senderLogin": "dmitry",
                    "recipientLogin": "alexey",
                    "senderBalance": "1500.00",
                    "currency": "RUB"
                }
                """;

        mockServer.expect(requestTo(TRANSFER_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        var operation = new TransferOperationViewModel("dmitry", "alexey", new BigDecimal("500.00"), CurrencyEnumModel.RUB, "op-123");

        var result = accountClient.execute(operation);

        mockServer.verify();

        assertThat(result).isNotNull();

        assertThat(result.senderLogin()).isEqualTo("dmitry");

        assertThat(result.recipientLogin()).isEqualTo("alexey");

        assertThat(result.senderBalance()).isEqualTo(new BigDecimal("1500.00"));

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
                    "message": "Недостаточно средств на счете отправителя"
                }
                """;

        mockServer.expect(requestTo(TRANSFER_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body(errorResponseJson)
                        .contentType(MediaType.APPLICATION_JSON));

        var operation = new TransferOperationViewModel("dmitry", "alexey", new BigDecimal("50000.00"), CurrencyEnumModel.RUB, "op-123");

        assertThatThrownBy(() -> accountClient.execute(operation))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Недостаточно средств на счете отправителя");
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

        mockServer.expect(requestTo(TRANSFER_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponseJson)
                        .contentType(MediaType.APPLICATION_JSON));

        var operation = new TransferOperationViewModel("dmitry", "alexey", new BigDecimal("100.00"), CurrencyEnumModel.RUB, "op-123");

        assertThatThrownBy(() -> accountClient.execute(operation))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Запрос на обслуживание учетной записи не удался.");
    }

    /**
     * <summary>
     * Проверяет использование сообщения по умолчанию, если сервис счетов вернул невалидный JSON в теле ошибки.
     * </summary>
     **/
    @Test
    public void shouldFallbackToDefaultMessageWhenResponseBodyIsMalformedJson() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(TRANSFER_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Internal Server Error (Html or plain text)")
                        .contentType(MediaType.TEXT_PLAIN));

        var operation = new TransferOperationViewModel("dmitry", "alexey", new BigDecimal("100.00"), CurrencyEnumModel.RUB, "op-123");

        assertThatThrownBy(() -> accountClient.execute(operation))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Запрос на обслуживание учетной записи не удался.");
    }

    /**
     * <summary>
     * Проверяет обработку таймаута или сетевого сбоя при выполнении запроса (RestClientException).
     * </summary>
     **/
    @Test
    public void shouldHandleNetworkFailure() {
        when(serviceTokenProvider.getAccessToken()).thenReturn(TEST_TOKEN);

        mockServer.expect(requestTo(TRANSFER_URI))
                .andRespond(withException(new IOException("Connection refused")));

        var operation = new TransferOperationViewModel("dmitry", "alexey", new BigDecimal("100.00"), CurrencyEnumModel.RUB, "op-123");

        assertThatThrownBy(() -> accountClient.execute(operation))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Запрос на обслуживание учетной записи не удался.");
    }

    /**
     * <summary>
     * Проверяет срабатывание резервного метода (fallback) при размыкании цепи Circuit Breaker или непредвиденных исключениях.
     * </summary>
     **/
    @Test
    public void shouldThrowUnavailableExceptionWhenCircuitBreakerTriggersOnGenericError() {
        when(serviceTokenProvider.getAccessToken()).thenThrow(new RuntimeException("OAuth2 Identity Provider unavailable"));

        var operation = new TransferOperationViewModel("dmitry", "alexey", new BigDecimal("100.00"), CurrencyEnumModel.RUB, "op-123");

        assertThatThrownBy(() -> accountClient.execute(operation))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Сервис счетов временно недоступен");
    }

    // endregion
}