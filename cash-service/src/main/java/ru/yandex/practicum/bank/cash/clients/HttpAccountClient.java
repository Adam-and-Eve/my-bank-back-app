package ru.yandex.practicum.bank.cash.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.yandex.practicum.bank.cash.exceptions.AccountClientException;
import ru.yandex.practicum.bank.cash.interfaces.AccountClient;
import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceResponseViewModel;
import ru.yandex.practicum.bank.shared.clients.ResilientExecutorClient;
import ru.yandex.practicum.bank.shared.clients.ResilientFactoryClient;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.shared.viewmodels.ApiErrorResponseViewModel;

/**
 * <summary>
 * HTTP-клиент для взаимодействия с сервисом счетов (Accounts Service).
 * Реализует контракт AccountClient, используя RestClient для HTTP-запросов,
 * авторизацию по OAuth2-токену и паттерн Circuit Breaker для устойчивости к сбоям.
 * </summary>
 **/
@Component
public class HttpAccountClient implements AccountClient {

    // region Fields

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;
    private final ResilientExecutorClient executorClient;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(HttpAccountClient.class);

    // endregion

    // region Constructors

    /**
     * <summary>
     * Основной конструктор для создания HTTP-клиента сервиса счетов с поддержкой отказоустойчивости (Resilience).
     * </summary>
     * @param restClientBuilder Билдер для настройки базового RestClient.
     * @param accountBaseUrl Базовый URL целевого сервиса счетов.
     * @param serviceTokenProvider Провайдер для получения сервисных OAuth2 токенов.
     * @param resilientFactoryClient Фабрика для создания клиента с поддержкой Circuit Breaker и Retry.
     * @param objectMapper Маппер для работы с JSON.
     */
    @Autowired
    public HttpAccountClient(
            RestClient.Builder restClientBuilder,
            @Value("${bank.services.account-service.base-url}") String accountBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ResilientFactoryClient resilientFactoryClient,
            ObjectMapper objectMapper
    ) {
        this(
                restClientBuilder,
                accountBaseUrl,
                serviceTokenProvider,
                resilientFactoryClient.create("accountService", HttpAccountClient::isRecoverable),
                objectMapper
        );
    }

    /**
     * <summary>
     * Вспомогательный конструктор, использующий настройки Resilience по умолчанию.
     * </summary>
     */
    HttpAccountClient(
            RestClient.Builder restClientBuilder,
            String accountsBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ObjectMapper objectMapper
    ) {
        this(
                restClientBuilder,
                accountsBaseUrl,
                serviceTokenProvider,
                ResilientFactoryClient.withDefaults(),
                objectMapper
        );
    }

    /**
     * <summary>
     * Базовый конструктор для инициализации клиента с внедрением всех зависимостей напрямую (удобно для тестов).
     * </summary>
     */
    HttpAccountClient(
            RestClient.Builder restClientBuilder,
            String accountsBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ResilientExecutorClient executorClient,
            ObjectMapper objectMapper
    ) {
        this.serviceTokenProvider = serviceTokenProvider;
        this.executorClient = executorClient;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(accountsBaseUrl)
                .build();
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Отправляет запрос на пополнение баланса счета в сервис счетов.
     * </summary>
     * @param request Модель запроса на изменение баланса AccountBalanceOperationRequestViewModel.
     * @return Модель ответа AccountBalanceResponseViewModel с обновленным балансом.
     **/
    @Override
    public AccountBalanceResponseViewModel deposit(AccountBalanceOperationRequestViewModel request) {
        return post("/api/account/internal/balance/deposit", request);
    }

    /**
     * <summary>
     * Отправляет запрос на списание средств со счета в сервис счетов.
     * </summary>
     * @param request Модель запроса на изменение баланса AccountBalanceOperationRequestViewModel.
     * @return Модель ответа AccountBalanceResponseViewModel с обновленным балансом.
     **/
    @Override
    public AccountBalanceResponseViewModel withdraw(AccountBalanceOperationRequestViewModel request) {
        return post("/api/account/internal/balance/withdraw", request);
    }

    /**
     * <summary>
     * Выполняет POST-запрос с оберткой в Circuit Breaker.
     * </summary>
     * @param uri Относительный URI эндпоинта.
     * @param request Объект запроса.
     * @return Ответ от внешнего сервиса.
     **/
    private AccountBalanceResponseViewModel post(String uri, AccountBalanceOperationRequestViewModel request) {
        return executorClient.execute(
                () -> postWithoutCircuitBreaker(uri, request),
                this::accountFallback
        );
    }

    /**
     * <summary>
     * Выполняет прямой HTTP POST-запрос к сервису счетов с добавлением OAuth2-токена авторизации.
     * </summary>
     * @param uri Относительный URI эндпоинта.
     * @param request Объект запроса.
     * @return Ответ от внешнего сервиса.
     **/
    private AccountBalanceResponseViewModel postWithoutCircuitBreaker(String uri, AccountBalanceOperationRequestViewModel request) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Account downstream request prepared operationId={} operationType={} currency={} source=cash-service targetService=accounts-service",
                        request.operationId(),
                        operationType(uri),
                        request.currency()
                );
            }

            return restClient.post()
                    .uri(uri)
                    .headers(headers -> headers.setBearerAuth(serviceTokenProvider.getAccessToken()))
                    .body(request)
                    .retrieve()
                    .body(AccountBalanceResponseViewModel.class);
        } catch (RestClientResponseException exception) {
            var error = extractError(exception);

            throw new AccountClientException(
                    error.message(),
                    exception.getStatusCode(),
                    error.code(),
                    exception
            );
        } catch (RestClientException exception) {
            log.error(
                    "Account downstream request failed operationId={} operationType={} currency={} status=error errorCategory=downstream_unavailable errorType={} source=cash-service targetService=accounts-service",
                    request.operationId(),
                    operationType(uri),
                    request.currency(),
                    exception.getClass().getSimpleName()
            );
            throw new AccountClientException("Account service request failed", exception);
        }
    }

    /**
     * <summary>
     * Определяет, является ли перехваченное исключение восстановимым (recoverable).
     * Ошибки сервера (5xx) считаются восстановимыми и могут быть повторены механизмом Retry.
     * </summary>
     * @param exception Исключение для проверки.
     * @return true, если вызов можно безопасно повторить, иначе false.
     */
    private static boolean isRecoverable(Throwable exception) {
        if (exception instanceof AccountClientException accountClientException) {
            return accountClientException.getStatusCode().is5xxServerError();
        }

        return true;
    }

    /**
     * <summary>
     * Фолбэк-метод, вызываемый при срабатывании Circuit Breaker или исчерпании попыток (retries).
     * </summary>
     * @param exception Исключение, вызвавшее сбой.
     * @return Не возвращает значение, всегда выбрасывает AccountClientException.
     **/
    private AccountBalanceResponseViewModel accountFallback(Throwable exception) {
        if (exception instanceof AccountClientException accountClientException) {
            if (accountClientException.getStatusCode().is5xxServerError()) {
                log.error(
                        "Account downstream retries exhausted status={} errorCode={} errorCategory=downstream_unavailable errorType={} source=cash-service targetService=accounts-service",
                        accountClientException.getStatusCode().value(),
                        accountClientException.getCode(),
                        accountClientException.getClass().getSimpleName()
                );
            }

            throw accountClientException;
        }

        log.error(
                "Account downstream retries exhausted status=error errorCategory=downstream_unavailable errorType={} source=cash-service targetService=accounts-service",
                exception.getClass().getSimpleName()
        );

        throw new AccountClientException("Сервис счетов временно недоступен", exception);
    }

    /**
     * <summary>
     * Извлекает понятное сообщение об ошибке из тела ответа внешнего сервиса (ApiErrorResponseViewModel).
     * </summary>
     * @param exception Исключение ответа HTTP-клиента RestClientResponseException.
     * @return Текст ошибки из ответа сервиса или дефолтное сообщение при неуспешном парсинге.
     **/
    private ApiErrorResponseViewModel extractError(RestClientResponseException exception) {
        try {
            var error = objectMapper.readValue(exception.getResponseBodyAsString(), ApiErrorResponseViewModel.class);

            if (isNotBlank(error.code()) && isNotBlank(error.message())) {
                return error;
            }
        } catch (JsonProcessingException ignored) {
            /*
             * Используйте стабильный резервный вариант,
             * если нижестоящий поставщик не возвращает ожидаемое тело ошибки.
             */
        }
        return new ApiErrorResponseViewModel("ACCOUNT_SERVICE_ERROR", "Account service request failed");
    }

    /**
     * <summary>
     * Проверяет, что строка не равна null и не состоит только из пробельных символов.
     * </summary>
     * @param value Проверяемая строка.
     * @return true, если строка содержит полезную нагрузку.
     */
    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * <summary>
     * Определяет тип выполняемой операции на основе URI запроса для логирования.
     * </summary>
     * @param uri URI запроса.
     * @return Строковое представление типа операции (DEPOSIT или WITHDRAW).
     */
    private String operationType(String uri) {
        return uri.endsWith("/deposit") ? "DEPOSIT" : "WITHDRAW";
    }

    // endregion
}