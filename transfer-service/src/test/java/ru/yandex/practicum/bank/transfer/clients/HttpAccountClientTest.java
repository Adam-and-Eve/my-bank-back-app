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
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.transfer.exceptions.AccountClientException;
import ru.yandex.practicum.bank.transfer.mappers.AccountTransferMapper;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.transfer.viewmodels.AccountTransferRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferOperationViewModel;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
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

    private static final String TRANSFER_URI =
            BASE_URL + "/api/account/internal/balance/transfer";

    private static final String TEST_TOKEN =
            "secret-service-jwt-token";

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

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        objectMapper = new ObjectMapper();

        accountClient = new HttpAccountClient(
                restClientBuilder,
                BASE_URL,
                serviceTokenProvider,
                accountTransferMapper,
                objectMapper
        );
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешное выполнение перевода через сервис счетов.
     * Проверяет передачу Bearer-токена, преобразование операции в запрос
     * и корректное преобразование ответа сервиса счетов в TransferResultViewModel.
     * </summary>
     **/
    @Test
    public void shouldExecuteTransferSuccessfully() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        var operation = new TransferOperationViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("500.00"),
                CurrencyEnumModel.RUB,
                new BigDecimal("500.00"),
                CurrencyEnumModel.RUB,
                "op-123",
                List.of()
        );

        var accountRequest = new AccountTransferRequestViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("500.00"),
                CurrencyEnumModel.RUB,
                new BigDecimal("500.00"),
                CurrencyEnumModel.RUB,
                "op-123",
                List.of()
        );

        when(accountTransferMapper.toAccountRequest(operation))
                .thenReturn(accountRequest);

        String jsonResponse = """
                {
                    "senderLogin": "dmitry",
                    "recipientLogin": "alexey",
                    "senderBalance": "1500.00",
                    "currency": "RUB"
                }
                """;

        mockServer.expect(ExpectedCount.once(), requestTo(TRANSFER_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + TEST_TOKEN
                ))
                .andRespond(withSuccess(
                        jsonResponse,
                        MediaType.APPLICATION_JSON
                ));

        var result = accountClient.execute(operation);

        mockServer.verify();

        verify(accountTransferMapper).toAccountRequest(operation);

        assertThat(result).isNotNull();

        assertThat(result.senderLogin())
                .isEqualTo("dmitry");

        assertThat(result.recipientLogin())
                .isEqualTo("alexey");

        assertThat(result.senderBalance())
                .isEqualByComparingTo("1500.00");

        assertThat(result.currency())
                .isEqualTo("RUB");
    }

    /**
     * <summary>
     * Проверяет передачу в сервис счетов сконвертированной суммы получателя
     * и целевой валюты через AccountTransferMapper.
     * </summary>
     **/
    @Test
    public void shouldMapConvertedTransferOperationToAccountRequest() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        var operation = new TransferOperationViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("100.00"),
                CurrencyEnumModel.USD,
                new BigDecimal("9200.00"),
                CurrencyEnumModel.RUB,
                "op-123",
                List.of()
        );

        var accountRequest = new AccountTransferRequestViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("100.00"),
                CurrencyEnumModel.USD,
                new BigDecimal("9200.00"),
                CurrencyEnumModel.RUB,
                "op-123",
                List.of()
        );

        when(accountTransferMapper.toAccountRequest(operation))
                .thenReturn(accountRequest);

        String jsonResponse = """
                {
                    "senderLogin": "dmitry",
                    "recipientLogin": "alexey",
                    "senderBalance": "900.00",
                    "currency": "USD"
                }
                """;

        mockServer.expect(ExpectedCount.once(), requestTo(TRANSFER_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        jsonResponse,
                        MediaType.APPLICATION_JSON
                ));

        var result = accountClient.execute(operation);

        mockServer.verify();

        verify(accountTransferMapper)
                .toAccountRequest(operation);

        assertThat(result.senderLogin())
                .isEqualTo("dmitry");

        assertThat(result.recipientLogin())
                .isEqualTo("alexey");

        assertThat(result.senderBalance())
                .isEqualByComparingTo("900.00");

        assertThat(result.currency())
                .isEqualTo("USD");
    }

    /**
     * <summary>
     * Проверяет извлечение сообщения об ошибке из JSON-ответа сервиса счетов
     * при получении HTTP 400 Bad Request.
     * </summary>
     **/
    @Test
    public void shouldExtractErrorMessageFromJsonResponseOnHttpError() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        var operation = createOperation();

        var accountRequest = createAccountRequest();

        when(accountTransferMapper.toAccountRequest(operation))
                .thenReturn(accountRequest);

        String errorResponseJson = """
                {
                    "code": "INSUFFICIENT_FUNDS",
                    "message": "Недостаточно средств на счете отправителя"
                }
                """;

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(TRANSFER_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body(errorResponseJson)
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> accountClient.execute(operation))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Недостаточно средств на счете отправителя");

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет использование сообщения по умолчанию,
     * если поле message в JSON-ответе об ошибке содержит только пробелы.
     * </summary>
     **/
    @Test
    public void shouldFallbackToDefaultMessageWhenErrorMessageIsBlank() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        var operation = createOperation();

        when(accountTransferMapper.toAccountRequest(operation))
                .thenReturn(createAccountRequest());

        String errorResponseJson = """
                {
                    "code": "INTERNAL_ERROR",
                    "message": "   "
                }
                """;

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(TRANSFER_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponseJson)
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> accountClient.execute(operation))
                .isInstanceOf(AccountClientException.class)
                .hasMessage(
                        "Account service request failed"
                );

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет использование сообщения по умолчанию,
     * если сервис счетов возвращает некорректное JSON-тело ошибки.
     * </summary>
     **/
    @Test
    public void shouldFallbackToDefaultMessageWhenResponseBodyIsMalformedJson() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        var operation = createOperation();

        when(accountTransferMapper.toAccountRequest(operation))
                .thenReturn(createAccountRequest());

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(TRANSFER_URI))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Internal Server Error")
                        .contentType(MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> accountClient.execute(operation))
                .isInstanceOf(AccountClientException.class)
                .hasMessage(
                        "Account service request failed"
                );

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет обработку сетевого сбоя при выполнении HTTP-запроса
     * и преобразование RestClientException в AccountClientException.
     * </summary>
     **/
    @Test
    public void shouldHandleNetworkFailure() {
        when(serviceTokenProvider.getAccessToken())
                .thenReturn(TEST_TOKEN);

        var operation = createOperation();

        when(accountTransferMapper.toAccountRequest(operation))
                .thenReturn(createAccountRequest());

        mockServer.expect(ExpectedCount.manyTimes(), requestTo(TRANSFER_URI))
                .andRespond(withException(
                        new IOException("Connection refused")
                ));

        assertThatThrownBy(() -> accountClient.execute(operation))
                .isInstanceOf(AccountClientException.class)
                .hasMessage(
                        "Account service request failed"
                );

        mockServer.verify();
    }

    /**
     * <summary>
     * Проверяет работу fallback-механизма при возникновении непредвиденного
     * исключения во время выполнения операции.
     * </summary>
     **/
    @Test
    public void shouldThrowUnavailableExceptionWhenUnexpectedErrorOccurs() {
        when(serviceTokenProvider.getAccessToken())
                .thenThrow(new RuntimeException(
                        "OAuth2 Identity Provider unavailable"
                ));

        var operation = createOperation();

        assertThatThrownBy(() -> accountClient.execute(operation))
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Сервис счетов временно недоступен");
    }

    // endregion

    // region Private Methods

    private TransferOperationViewModel createOperation() {
        return new TransferOperationViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB,
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB,
                "op-123",
                List.of()
        );
    }

    private AccountTransferRequestViewModel createAccountRequest() {
        return new AccountTransferRequestViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB,
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB,
                "op-123",
                List.of()
        );
    }

    // endregion
}