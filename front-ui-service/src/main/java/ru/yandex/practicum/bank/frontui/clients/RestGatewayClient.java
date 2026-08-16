package ru.yandex.practicum.bank.frontui.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayClientException;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayExceptionHandler;
import ru.yandex.practicum.bank.frontui.interfaces.GatewayClient;
import ru.yandex.practicum.bank.frontui.mappers.GatewayRequestMapper;
import ru.yandex.practicum.bank.frontui.viewmodels.*;
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;

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

    private final RestClient restClient;

    private final SimpleCircuitBreaker circuitBreaker;

    private final GatewayRequestMapper requestMapper;

    // endregion

    // region Constructors

    /**
     * <summary>
     * Основной конструктор Spring для создания клиента API Gateway.
     * Автоматически создает CircuitBreaker с настройками по умолчанию.
     * </summary>
     * @param restClientBuilder Билдер для настройки и сборки {@link RestClient}.
     * @param gatewayBaseUrl Базовый URL API Gateway из конфигурации приложений.
     * @param errorHandler Компонент централизованной обработки HTTP-ошибок.
     * @param requestMapper Маппер экранных форм в DTO-запросы.
     **/
    @Autowired
    public RestGatewayClient(
            RestClient.Builder restClientBuilder,
            @Value("${api.gateway.base-url}") String gatewayBaseUrl,
            GatewayExceptionHandler errorHandler,
            GatewayRequestMapper requestMapper
    ) {
        this(
                restClientBuilder,
                gatewayBaseUrl,
                SimpleCircuitBreaker.withDefaults("apiGateway"),
                errorHandler,
                requestMapper
        );
    }

    /**
     * <summary>
     * Конструктор с возможностью явного указания экземпляра CircuitBreaker (используется в тестах).
     * </summary>
     * @param restClientBuilder Билдер для настройки и сборки {@link RestClient}.
     * @param gatewayBaseUrl Базовый URL API Gateway.
     * @param circuitBreaker Предохранитель (CircuitBreaker) для устойчивости к сбоям.
     * @param errorHandler Компонент централизованной обработки HTTP-ошибок.
     * @param requestMapper Маппер экранных форм в DTO-запросы.
     **/
    RestGatewayClient(
            RestClient.Builder restClientBuilder,
            String gatewayBaseUrl,
            SimpleCircuitBreaker circuitBreaker,
            GatewayExceptionHandler errorHandler,
            GatewayRequestMapper requestMapper
    ) {
        this.circuitBreaker = circuitBreaker;
        this.requestMapper = requestMapper;
        this.restClient = restClientBuilder
                .baseUrl(gatewayBaseUrl)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> errorHandler.handleError(response))
                .build();
    }

    // endregion

    // region Public Methods

    /**
     * <summary>
     * Выполняет денежный перевод между счетами с защитой CircuitBreaker.
     * </summary>
     * @param accessToken OAuth2 Access Token авторизованного пользователя.
     * @param form Данные формы перевода.
     * @return Ответ от сервера с результатом перевода.
     **/
    @Override
    public TransferResponseViewModel transfer(String accessToken, TransferFormViewModel form) {
        return runWithCircuitBreaker(() -> transferWithoutCircuitBreaker(accessToken, form));
    }

    /**
     * <summary>
     * Запрашивает профиль текущего пользователя с защитой CircuitBreaker.
     * </summary>
     * @param accessToken OAuth2 Access Token авторизованного пользователя.
     * @return Информация об аккаунте.
     **/
    @Override
    public AccountResponseViewModel getAccount(String accessToken) {
        return runWithCircuitBreaker(() -> getAccountWithoutCircuitBreaker(accessToken));
    }

    /**
     * <summary>
     * Обновляет личные данные пользователя с защитой CircuitBreaker.
     * </summary>
     * @param accessToken OAuth2 Access Token авторизованного пользователя.
     * @param form Форма с обновляемыми данными аккаунта.
     * @return Обновленная информация об аккаунте.
     **/
    @Override
    public AccountResponseViewModel updateAccount(String accessToken, AccountFormViewModel form) {
        return runWithCircuitBreaker(() -> updateAccountWithoutCircuitBreaker(accessToken, form));
    }

    /**
     * <summary>
     * Запрашивает список получателей переводов с защитой CircuitBreaker.
     * </summary>
     * @param accessToken OAuth2 Access Token авторизованного пользователя.
     * @return Список получателей или пустой список, если данные отсутствуют.
     **/
    @Override
    public List<RecipientResponseViewModel> getRecipients(String accessToken) {
        return runWithCircuitBreaker(() -> getRecipientsWithoutCircuitBreaker(accessToken));
    }

    /**
     * <summary>
     * Выполняет операцию пополнения счета наличными.
     * </summary>
     * @param accessToken OAuth2 Access Token авторизованного пользователя.
     * @param form Форма ввода суммы и валюты депозита.
     * @return Результат кассовой операции.
     **/
    @Override
    public CashOperationResponseViewModel deposit(String accessToken, CashFormViewModel form) {
        return cashOperation(accessToken, "/api/cash/deposit", form);
    }

    /**
     * <summary>
     * Выполняет операцию снятия наличных со счета.
     * </summary>
     * @param accessToken OAuth2 Access Token авторизованного пользователя.
     * @param form Форма ввода суммы и валюты снятия.
     * @return Результат кассовой операции.
     **/
    @Override
    public CashOperationResponseViewModel withdraw(String accessToken, CashFormViewModel form) {
        return cashOperation(accessToken, "/api/cash/withdraw", form);
    }

    // endregion

    // region Private Methods

    /**
     * <summary>
     * Прямой вызов REST API для перевода средств.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма перевода.
     * @return DTO ответа перевода.
     **/
    private TransferResponseViewModel transferWithoutCircuitBreaker(String accessToken, TransferFormViewModel form) {
        try {
            return restClient.post()
                    .uri("/api/transfer")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(requestMapper.toTransferRequest(form))
                    .retrieve()
                    .body(TransferResponseViewModel.class);
        } catch (RestClientException exception) {
            throw new GatewayClientException("Gateway request failed", exception);
        }
    }

    /**
     * <summary>
     * Прямой вызов REST API для получения информации об аккаунте.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @return DTO ответа с данными аккаунта.
     **/
    private AccountResponseViewModel getAccountWithoutCircuitBreaker(String accessToken) {
        try {
            return restClient.get()
                    .uri("/api/account/me")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(AccountResponseViewModel.class);
        } catch (RestClientException exception) {
            throw new GatewayClientException("Gateway request failed", exception);
        }
    }

    /**
     * <summary>
     * Прямой вызов REST API для обновления профиля аккаунта.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма с новыми данными профиля.
     * @return DTO ответа с обновленными данными.
     **/
    private AccountResponseViewModel updateAccountWithoutCircuitBreaker(String accessToken, AccountFormViewModel form) {
        try {
            return restClient.put()
                    .uri("/api/account/me")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(requestMapper.toUpdateAccountRequest(form))
                    .retrieve()
                    .body(AccountResponseViewModel.class);
        } catch (RestClientException exception) {
            throw new GatewayClientException("Gateway request failed", exception);
        }
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
            RecipientResponseViewModel[] recipients = restClient.get()
                    .uri("/api/account/recipients")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(RecipientResponseViewModel[].class);
            return recipients == null ? List.of() : Arrays.asList(recipients);
        } catch (RestClientException exception) {
            throw new GatewayClientException("Gateway request failed", exception);
        }
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
    private CashOperationResponseViewModel cashOperation(String accessToken, String uri, CashFormViewModel form) {
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
    private CashOperationResponseViewModel cashOperationWithoutCircuitBreaker(String accessToken, String uri, CashFormViewModel form) {
        try {
            return restClient.post()
                    .uri(uri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(requestMapper.toCashOperationRequest(form))
                    .retrieve()
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
        return circuitBreaker.execute(call::execute, this::gatewayFallback);
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