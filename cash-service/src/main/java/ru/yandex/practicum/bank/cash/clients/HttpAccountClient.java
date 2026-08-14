package ru.yandex.practicum.bank.cash.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.yandex.practicum.bank.cash.exceptions.AccountClientException;
import ru.yandex.practicum.bank.cash.interfaces.AccountClient;
import ru.yandex.practicum.bank.cash.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceResponseViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.ApiErrorResponseViewModel;
import ru.yandex.practicum.bank.shared.clients.SimpleCircuitBreaker;

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
    private final SimpleCircuitBreaker circuitBreaker;
    private final ObjectMapper objectMapper;

    // endregion

    // region Constructors

    @Autowired
    public HttpAccountClient(
            RestClient.Builder restClientBuilder,
            @Value("${bank.services.account.base-url}") String accountBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ObjectMapper objectMapper
    ) {
        this(
                restClientBuilder,
                accountBaseUrl,
                serviceTokenProvider,
                SimpleCircuitBreaker.withDefaults("accountService"),
                objectMapper
        );
    }

    HttpAccountClient(
            RestClient.Builder restClientBuilder,
            String accountBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            SimpleCircuitBreaker circuitBreaker,
            ObjectMapper objectMapper
    ) {
        this.serviceTokenProvider = serviceTokenProvider;
        this.circuitBreaker = circuitBreaker;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(accountBaseUrl)
                .build();
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Отправляет запрос на пополнение баланса счета в сервис счетов.
     * </summary>
     * @param request Модель запроса на изменение баланса AccountBalanceOperationRequestViewModel.
     * <return>
     * @return Модель ответа AccountBalanceResponseViewModel с обновленным балансом.
     * </return>
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
     * <return>
     * @return Модель ответа AccountBalanceResponseViewModel с обновленным балансом.
     * </return>
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
     * <return>
     * @return Ответ от внешнего сервиса.
     * </return>
     **/
    private AccountBalanceResponseViewModel post(String uri, AccountBalanceOperationRequestViewModel request) {
        return circuitBreaker.execute(
                () -> postWithoutCircuitBreaker(uri, request),
                this::accountsFallback
        );
    }

    /**
     * <summary>
     * Выполняет прямой HTTP POST-запрос к сервису счетов с добавлением OAuth2-токена авторизации.
     * </summary>
     * @param uri Относительный URI эндпоинта.
     * @param request Объект запроса.
     * <return>
     * @return Ответ от внешнего сервиса.
     * </return>
     **/
    private AccountBalanceResponseViewModel postWithoutCircuitBreaker(String uri, AccountBalanceOperationRequestViewModel request) {
        try {
            return restClient.post()
                    .uri(uri)
                    .headers(headers -> headers.setBearerAuth(serviceTokenProvider.getAccessToken()))
                    .body(request)
                    .retrieve()
                    .body(AccountBalanceResponseViewModel.class);
        } catch (RestClientResponseException exception) {
            throw new AccountClientException(extractMessage(exception), exception);
        } catch (RestClientException exception) {
            throw new AccountClientException("Account service request failed", exception);
        }
    }

    /**
     * <summary>
     * Фолбэк-метод, вызываемый при срабатывании Circuit Breaker или ошибках вызова.
     * </summary>
     * @param exception Исключение, вызвавшее сбой.
     * <return>
     * @return Не возвращает значение, всегда выбрасывает AccountClientException.
     * </return>
     **/
    private AccountBalanceResponseViewModel accountsFallback(Throwable exception) {
        if (exception instanceof AccountClientException accountsClientException) {
            throw accountsClientException;
        }

        throw new AccountClientException("Сервис счетов временно недоступен", exception);
    }

    /**
     * <summary>
     * Извлекает понятное сообщение об ошибке из тела ответа внешнего сервиса (ApiErrorResponseViewModel).
     * </summary>
     * @param exception Исключение ответа HTTP-клиента RestClientResponseException.
     * <return>
     * @return Текст ошибки из ответа сервиса или дефолтное сообщение при неуспешном парсинге.
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
        return "Account service request failed";
    }

    // endregion
}