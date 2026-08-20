package ru.yandex.practicum.bank.frontui.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayClientException;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayExceptionHandler;
import ru.yandex.practicum.bank.frontui.exceptions.RestGatewayClientException;
import ru.yandex.practicum.bank.frontui.interfaces.GatewayClient;
import ru.yandex.practicum.bank.frontui.mappers.GatewayRequestMapper;
import ru.yandex.practicum.bank.frontui.viewmodels.*;
import ru.yandex.practicum.bank.shared.clients.ResilientExecutorClient;
import ru.yandex.practicum.bank.shared.clients.ResilientFactoryClient;
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * <summary>
 * REST-реализация клиента API Gateway с поддержкой CircuitBreaker,
 * авторизации через Bearer token и централизованной обработкой ошибок.
 * </summary>
 **/
@Component
public class RestGatewayClient implements GatewayClient {

    // region Fields

    private final RestClient accountsClient;

    private final RestClient cashClient;

    private final RestClient transferClient;

    private final RestClient exchangeClient;

    private final ResilientExecutorClient clientExecutor;

    private final GatewayRequestMapper gatewayRequestMapper;

    private final ObjectMapper objectMapper;

    // endregion

    // region Constructors

    /**
     * <summary>
     * Основной конструктор Spring для создания клиента API Gateway.
     * Автоматически создает CircuitBreaker с настройками по умолчанию.
     * </summary>
     * @param restClientBuilder Билдер для настройки и сборки {@link RestClient}.
     * @param gatewayBaseUrl Базовый URL API Gateway из конфигурации приложений.
     **/
    @Autowired
    public RestGatewayClient(
            RestClient.Builder restClientBuilder,
            @Value("${api.gateway.base-url}") String gatewayBaseUrl,
            ResilientFactoryClient resilientClientFactory,
            GatewayRequestMapper gatewayRequestMapper,
            ObjectMapper objectMapper
    ) {
        this(
                restClientBuilder,
                gatewayBaseUrl,
                resilientClientFactory.create("apiServices", RestGatewayClient::isRecoverable),
                gatewayRequestMapper,
                objectMapper
        );
    }

    /**
     * <summary>
     * Конструктор с возможностью явного указания экземпляра CircuitBreaker (используется в тестах).
     * </summary>
     * @param restClientBuilder Билдер для настройки и сборки {@link RestClient}.
     * @param gatewayBaseUrl Базовый URL API Gateway.
     **/
    RestGatewayClient(
            RestClient.Builder restClientBuilder,
            String gatewayBaseUrl,
            ResilientExecutorClient clientExecutor,
            GatewayRequestMapper gatewayRequestMapper,
            ObjectMapper objectMapper
    ) {
        this.accountsClient = restClientBuilder
                .clone()
                .baseUrl(gatewayBaseUrl)
                .build();

        this.cashClient = restClientBuilder
                .clone()
                .baseUrl(gatewayBaseUrl)
                .build();

        this.transferClient = restClientBuilder
                .clone()
                .baseUrl(gatewayBaseUrl)
                .build();

        this.exchangeClient = restClientBuilder
                .clone()
                .baseUrl(gatewayBaseUrl)
                .build();

        this.clientExecutor = clientExecutor;

        this.gatewayRequestMapper = gatewayRequestMapper;

        this.objectMapper = objectMapper;
    }

    // endregion

    // region Public Methods

    /**
     * <summary>
     * Выполняет перевод денежных средств через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма перевода денежных средств.
     * @return Результат выполнения перевода.
     **/
    public TransferResponseViewModel transfer(String accessToken, TransferFormViewModel form) {
        return runWithCircuitBreaker(() -> transferWithoutCircuitBreaker(accessToken, form));
    }

    /**
     * <summary>
     * Выполняет прямой REST-вызов API Gateway для перевода денежных средств без CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма перевода денежных средств.
     * @return Результат выполнения перевода.
     **/
    private TransferResponseViewModel transferWithoutCircuitBreaker(
            String accessToken,
            TransferFormViewModel form) {
        try {
            return transferClient.post()
                    .uri("/api/transfer")
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.set("Idempotency-Key", form.idempotencyKey());
                    })
                    .body(gatewayRequestMapper.toTransferRequest(form))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response))
                    .body(TransferResponseViewModel.class);
        } catch (RestClientException exception) {
            throw new GatewayClientException("Gateway request failed", exception);
        }
    }

    /**
     * <summary>
     * Получает данные текущего счета через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @return Данные текущего счета.
     **/
    public AccountResponseViewModel getAccount(String accessToken) {
        return runWithCircuitBreaker(() -> getAccountWithoutCircuitBreaker(accessToken));
    }

    /**
     * <summary>
     * Получает текущие курсы валют через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @return Список текущих курсов валют.
     **/
    public List<ExchangeRateResponseViewModel> getExchangeRates(String accessToken) {
        return runWithCircuitBreaker(() -> getExchangeRatesWithoutCircuitBreaker(accessToken));
    }

    /**
     * <summary>
     * Выполняет прямой REST-вызов API Gateway для получения курсов валют без CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @return Список текущих курсов валют.
     **/
    private List<ExchangeRateResponseViewModel> getExchangeRatesWithoutCircuitBreaker(String accessToken) {
        try {
            ExchangeRateResponseViewModel[] rates = exchangeClient.get()
                    .uri("/api/exchange/rates")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response))
                    .body(ExchangeRateResponseViewModel[].class);
            return rates == null ? List.of() : Arrays.asList(rates);
        } catch (RestClientException exception) {
            throw new GatewayClientException("Gateway request failed", exception);
        }
    }

    /**
     * <summary>
     * Выполняет прямой REST-вызов API Gateway для получения данных текущего счета без CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @return Данные текущего счета.
     **/
    private AccountResponseViewModel getAccountWithoutCircuitBreaker(String accessToken) {
        try {
            return accountsClient.get()
                    .uri("/api/account/me")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response))
                    .body(AccountResponseViewModel.class);
        } catch (RestClientException exception) {
            throw new GatewayClientException("Gateway request failed", exception);
        }
    }

    /**
     * <summary>
     * Обновляет данные текущего счета через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма с обновляемыми данными счета.
     * @return Обновленные данные счета.
     **/
    public AccountResponseViewModel updateAccount(
            String accessToken,
            AccountFormViewModel form) {
        return runWithCircuitBreaker(() -> updateAccountWithoutCircuitBreaker(accessToken, form));
    }

    /**
     * <summary>
     * Выполняет прямой REST-вызов API Gateway для обновления данных счета без CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма с обновляемыми данными счета.
     * @return Обновленные данные счета.
     **/
    private AccountResponseViewModel updateAccountWithoutCircuitBreaker(
            String accessToken,
            AccountFormViewModel form) {
        try {
            return accountsClient.put()
                    .uri("/api/account/me")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(gatewayRequestMapper.toUpdateAccountRequest(form))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response))
                    .body(AccountResponseViewModel.class);
        } catch (RestClientException exception) {
            throw new GatewayClientException("Gateway request failed", exception);
        }
    }

    /**
     * <summary>
     * Получает список получателей через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @return Список доступных получателей.
     **/
    public List<RecipientResponseViewModel> getRecipients(String accessToken) {
        return runWithCircuitBreaker(() -> getRecipientsWithoutCircuitBreaker(accessToken));
    }

    /**
     * <summary>
     * Прямой вызов REST API для получения списка получателей.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @return Список объектов получателей.
     **/
    private List<RecipientResponseViewModel> getRecipientsWithoutCircuitBreaker(String accessToken) {
        try {
            RecipientResponseViewModel[] recipients = accountsClient.get()
                    .uri("/api/account/recipients")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response))
                    .body(RecipientResponseViewModel[].class);

            return recipients == null ? List.of() : Arrays.asList(recipients);
        } catch (RestClientException exception) {
            throw new GatewayClientException("Gateway request failed", exception);
        }
    }

    /**
     * <summary>
     * Выполняет депозит через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма кассовой операции.
     * @return Результат выполнения депозита.
     **/
    public CashOperationResponseViewModel deposit(
            String accessToken,
            CashFormViewModel form) {
        return cashOperation(accessToken, "/api/cash/deposit", form);
    }

    /**
     * <summary>
     * Выполняет снятие денежных средств через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма кассовой операции.
     * @return Результат выполнения снятия денежных средств.
     **/
    public CashOperationResponseViewModel withdraw(
            String accessToken,
            CashFormViewModel form) {
        return cashOperation(accessToken, "/api/cash/withdraw", form);
    }

    /**
     * <summary>
     * Обертка выполнения кассовых операций (депозит/снятие) через CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param uri URI конечной точки кассовой операции.
     * @param form Форма кассовой операции.
     * @return Результат выполнения операции.
     **/
    private CashOperationResponseViewModel cashOperation(
            String accessToken,
            String uri,
            CashFormViewModel form) {
        return runWithCircuitBreaker(() -> cashOperationWithoutCircuitBreaker(accessToken, uri, form));
    }

    /**
     * <summary>
     * Прямой вызов REST API для проведения кассовой операции.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param uri URI конечной точки кассовой операции.
     * @param form Форма кассовой операции.
     * @return Результат выполнения операции.
     **/
    private CashOperationResponseViewModel cashOperationWithoutCircuitBreaker(
            String accessToken,
            String uri,
            CashFormViewModel form) {
        try {

            return cashClient.post()
                    .uri(uri)
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.set("Idempotency-Key", form.idempotencyKey());
                    })
                    .body(gatewayRequestMapper.toCashOperationRequest(form))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> handleError(response))
                    .body(CashOperationResponseViewModel.class);
        } catch (RestClientException exception) {
            throw new GatewayClientException("Gateway request failed", exception);
        }
    }

    /**
     * <summary>
     * Выполняет сетевой запрос в рамках предохранителя CircuitBreaker.
     * </summary>
     * @param <T> Тип возвращаемого ответа.
     * @param call Функциональный вызов REST-клиента.
     * @return Результат выполнения запроса.
     **/
    private <T> T runWithCircuitBreaker(ClientCall<T> call) {
        return clientExecutor.execute(call::execute, this::gatewayFallback);
    }

    /**
     * <summary>
     * Определяет, является ли исключение техническим и подлежащим обработке CircuitBreaker.
     * Бизнес-ошибки не считаются восстанавливаемыми, тогда как технические ошибки
     * допускают повторную обработку и переход к резервному сценарию.
     * </summary>
     * @param exception Исключение, возникшее при выполнении запроса.
     * @return {@code true}, если исключение является восстанавливаемым техническим сбоем.
     **/
    static boolean isRecoverable(Throwable exception) {
        return !(exception instanceof RestGatewayClientException gatewayClientException)
                || gatewayClientException.isTechnical();
    }

    /**
     * <summary>
     * Фолбэк-метод (запасной вариант), вызываемый CircuitBreaker при сбоях или перегрузках.
     * </summary>
     * @param <T> Тип возвращаемого ответа.
     * @param exception Перехваченное исключение.
     * @return Ничего не возвращает, генерирует скомпонованный {@link GatewayClientException}.
     * @throws GatewayClientException Повторно пробрасывает исходное бизнес-исключение или общую ошибку недоступности.
     **/
    private <T> T gatewayFallback(Throwable exception) {
        if (exception instanceof GatewayClientException gatewayClientException) {
            throw gatewayClientException;
        }

        throw new GatewayClientException("Банковские сервисы временно недоступны", exception);
    }

    /**
     * <summary>
     * Обрабатывает ошибочный HTTP-ответ API Gateway и преобразует его
     * в исключение клиента с сообщением из тела ответа или HTTP-статусом.
     * </summary>
     * @param response HTTP-ответ с ошибкой.
     * @throws IOException Если не удалось прочитать тело ответа.
     * @throws GatewayClientException Если Gateway вернул ошибочный HTTP-ответ.
     **/
    private void handleError(ClientHttpResponse response) throws IOException {
        var body = response.getBody().readAllBytes();

        var message = extractMessage(body);

        if (message != null) {
            throw new GatewayClientException(message);
        }

        throw new GatewayClientException("Gateway request failed: " + response.getStatusCode());
    }

    /**
     * <summary>
     * Извлекает сообщение об ошибке из тела ответа API Gateway.
     * </summary>
     * @param body Тело HTTP-ответа в виде массива байтов.
     * @return Сообщение об ошибке или {@code null}, если сообщение отсутствует
     * или тело ответа не соответствует ожидаемому формату.
     **/
    private String extractMessage(byte[] body) {
        if (body.length == 0) {
            return null;
        }
        try {
            ApiErrorResponseViewModel error = objectMapper.readValue(body, ApiErrorResponseViewModel.class);

            if (error.message() != null && !error.message().isBlank()) {
                return error.message();
            }
        } catch (IOException ignored) {
            // В случае, если шлюз не возвращает ожидаемое тело ошибки, следует вернуться к HTTP-статусу.
        }
        return null;
    }

    private String cashOperationType(String uri) {
        return uri.endsWith("/deposit") ? "DEPOSIT" : "WITHDRAW";
    }

    /**
     * <summary>
     * Внутренний функциональный интерфейс для обертки REST-вызовов.
     * </summary>
     * @param <T> Тип возвращаемого значения.
     **/
    @FunctionalInterface
    private interface ClientCall<T> {
        T execute();
    }

    // endregion
}