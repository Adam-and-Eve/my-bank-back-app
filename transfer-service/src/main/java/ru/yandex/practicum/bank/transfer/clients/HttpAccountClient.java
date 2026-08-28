package ru.yandex.practicum.bank.transfer.clients;

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
import ru.yandex.practicum.bank.shared.clients.ResilientExecutorClient;
import ru.yandex.practicum.bank.shared.clients.ResilientFactoryClient;
import ru.yandex.practicum.bank.shared.viewmodels.ApiErrorResponseViewModel;
import ru.yandex.practicum.bank.transfer.exceptions.AccountClientException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferExecutor;
import ru.yandex.practicum.bank.transfer.mappers.AccountTransferMapper;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.transfer.viewmodels.*;

/**
 * <summary>
 * HTTP-клиент для взаимодействия с сервисом счетов (Accounts Service).
 * Реализует интерфейс TransferExecutor, отправляет запросы на проведение перевода средств через RestClient
 * с использованием паттерна Circuit Breaker и сервисной OAuth2-аутентификации.
 * </summary>
 **/
@Component
public class HttpAccountClient implements TransferExecutor {

    // region Fields

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;
    private final ResilientExecutorClient executorClient;
    private final AccountTransferMapper  accountTransferMapper;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(HttpAccountClient.class);

    // endregion

    // region Constructors

    @Autowired
    public HttpAccountClient(
            RestClient.Builder restClientBuilder,
            @Value("${bank.services.account-service.base-url}") String accountBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ResilientFactoryClient resilientFactoryClient,
            AccountTransferMapper accountTransferMapper,
            ObjectMapper objectMapper
    ) {
        this(
                restClientBuilder,
                accountBaseUrl,
                serviceTokenProvider,
                resilientFactoryClient.create("accountService", HttpAccountClient::isRecoverable),
                accountTransferMapper,
                objectMapper
        );
    }

    HttpAccountClient(
            RestClient.Builder restClientBuilder,
            String accountBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            AccountTransferMapper accountTransferMapper,
            ObjectMapper objectMapper
    ) {
        this(
                restClientBuilder,
                accountBaseUrl,
                serviceTokenProvider,
                ResilientFactoryClient.withDefaults(),
                accountTransferMapper,
                objectMapper
        );
    }

    HttpAccountClient(
            RestClient.Builder restClientBuilder,
            String accountBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ResilientExecutorClient executorClient,
            AccountTransferMapper accountTransferMapper,
            ObjectMapper objectMapper
    ) {
        this.serviceTokenProvider = serviceTokenProvider;
        this.executorClient = executorClient;
        this.accountTransferMapper = accountTransferMapper;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(accountBaseUrl)
                .build();
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Выполняет операцию перевода денежных средств через сервис счетов с использованием паттерна Circuit Breaker.
     * </summary>
     * @param operation Данные о выполняемой операции перевода.
     * <return>
     * @return Результат выполнения операции перевода TransferResultViewModel.
     * </return>
     **/
    @Override
    public TransferResultViewModel execute(TransferOperationViewModel operation) {
        return executorClient.execute(
                () -> executeWithoutCircuitBreaker(operation),
                this::accountsFallback
        );
    }

    /**
     * <summary>
     * Отправляет POST-запрос с токеном авторизации Bearer в сервис счетов без обертки Circuit Breaker.
     * </summary>
     * @param operation Данные о выполняемой операции перевода.
     * <return>
     * @return Результат выполнения операции перевода TransferResultViewModel.
     * </return>
     * @throws AccountClientException При получении ответа с ошибкой от внешнего сервиса или сетевых сбоях.
     **/
    private TransferResultViewModel executeWithoutCircuitBreaker(TransferOperationViewModel operation) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Account downstream request prepared operationId={} operationType=TRANSFER currency={} source=transfer-service targetService=account-service",
                        operation.operationId(),
                        operation.currency()
                );
            }

            var response = restClient.post()
                    .uri("/api/account/internal/balance/transfer")
                    .headers(headers -> headers.setBearerAuth(serviceTokenProvider.getAccessToken()))
                    .body(accountTransferMapper.toAccountRequest(operation))
                    .retrieve()
                    .body(AccountTransferResponseViewModel.class);

            return new TransferResultViewModel(
                    response.senderLogin(),
                    response.recipientLogin(),
                    response.senderBalance(),
                    response.currency()
            );
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
                    "Account downstream request failed operationId={} operationType=TRANSFER currency={} status=error errorCategory=downstream_unavailable errorType={} source=transfer-service targetService=account-service",
                    operation.operationId(),
                    operation.currency(),
                    exception.getClass().getSimpleName()
            );

            throw new AccountClientException("Account service request failed", exception);
        }
    }

    private static boolean isRecoverable(Throwable exception) {
        if (exception instanceof AccountClientException accountClientException) {
            return accountClientException.getStatusCode().is5xxServerError();
        }

        return true;
    }

    /**
     * <summary>
     * Резервный метод (fallback), вызываемый при размыкании цепи Circuit Breaker или критических сбоях взаимодействия.
     * </summary>
     * @param exception Исключение, ставшее причиной срабатывания fallback-механизма.
     * <return>
     * @return Ничего не возвращает; всегда выбрасывает понятное бизнес-исключение.
     * </return>
     * @throws AccountClientException Перевыбрасывает исходное исключение или создаёт новое сообщение о недоступности.
     **/
    private TransferResultViewModel accountsFallback(Throwable exception) {
        if (exception instanceof AccountClientException accountClientException) {
            if (accountClientException.getStatusCode().is5xxServerError()) {
                log.error(
                        "Account downstream retries exhausted status={} errorCode={} errorCategory=downstream_unavailable errorType={} source=transfer-service targetService=account-service",
                        accountClientException.getStatusCode().value(),
                        accountClientException.getCode(),
                        accountClientException.getClass().getSimpleName()
                );
            }

            throw accountClientException;
        }

        log.error(
                "Account downstream retries exhausted status=error errorCategory=downstream_unavailable errorType={} source=transfer-service targetService=account-service",
                exception.getClass().getSimpleName()
        );

        throw new AccountClientException("Сервис счетов временно недоступен", exception);
    }

    /**
     * <summary>
     * Извлекает понятное пользователю сообщение об ошибке из JSON-тела ответа сервиса счетов.
     * </summary>
     * @param exception Исключение HTTP-ответа RestClientResponseException.
     * <return>
     * @return Текст ошибки из структуры ApiErrorResponseViewModel или дефолтное сообщение.
     * </return>
     **/
    private ApiErrorResponseViewModel extractError(RestClientResponseException exception) {
        try {
            var error = objectMapper.readValue(exception.getResponseBodyAsString(), ApiErrorResponseViewModel.class);

            if (isNotBlank(error.code()) && isNotBlank(error.message())) {

                return error;
            }
        } catch (JsonProcessingException ignored) {

        }
        return new ApiErrorResponseViewModel("ACCOUNT_SERVICE_ERROR", "Account service request failed");
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    // endregion
}