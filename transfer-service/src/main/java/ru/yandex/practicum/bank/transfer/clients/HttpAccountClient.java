package ru.yandex.practicum.bank.transfer.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;
import ru.yandex.practicum.bank.transfer.exceptions.AccountClientException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferExecutor;
import ru.yandex.practicum.bank.transfer.mappers.AccountTransferMapper;
import ru.yandex.practicum.bank.transfer.providers.ServiceTokenProvider;
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
    private final SimpleCircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;
    private final AccountTransferMapper  accountTransferMapper;

    // endregion

    // region Constructors

    @Autowired
    public HttpAccountClient(
            RestClient.Builder restClientBuilder,
            @Value("${bank.services.account.base-url}") String accountBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ObjectMapper objectMapper,
            AccountTransferMapper accountTransferMapper
    ) {
        this(
                restClientBuilder,
                accountBaseUrl,
                serviceTokenProvider,
                SimpleCircuitBreaker.withDefaults("accountService"),
                objectMapper,
                accountTransferMapper
        );
    }

    HttpAccountClient(
            RestClient.Builder restClientBuilder,
            String accountBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            SimpleCircuitBreaker circuitBreaker,
            ObjectMapper objectMapper,
            AccountTransferMapper accountTransferMapper
    ) {
        this.serviceTokenProvider = serviceTokenProvider;
        this.circuitBreaker = circuitBreaker;
        this.objectMapper = objectMapper;
        this.accountTransferMapper = accountTransferMapper;
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
        return circuitBreaker.execute(
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
            var response = restClient.post()
                    .uri("/api/account/internal/balance/transfer")
                    .headers(headers -> headers.setBearerAuth(serviceTokenProvider.getAccessToken()))
                    .body(accountTransferMapper.toAccountRequest(operation))
                    .retrieve()
                    .body(AccountTransferResponseViewModel.class);

            assert response != null;

            return new TransferResultViewModel(
                    response.senderLogin(),
                    response.recipientLogin(),
                    response.senderBalance(),
                    response.currency()
            );
        } catch (RestClientResponseException exception) {
            throw new AccountClientException(extractMessage(exception), exception);
        } catch (RestClientException exception) {
            throw new AccountClientException("Запрос на обслуживание учетной записи не удался.", exception);
        }
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
        if (exception instanceof AccountClientException accountsClientException) {
            throw accountsClientException;
        }

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
    private String extractMessage(RestClientResponseException exception) {
        try {
            var error = objectMapper.readValue(exception.getResponseBodyAsString(), ApiErrorResponseViewModel.class);

            if (error.message() != null && !error.message().isBlank()) {
                return error.message();
            }
        } catch (JsonProcessingException ignored) {
            /*
             * Используйте стабильный резервный вариант,
             * если нижестоящий поставщик не возвращает ожидаемое тело ошибки.
             */
        }

        return "Запрос на обслуживание учетной записи не удался.";
    }

    // endregion
}