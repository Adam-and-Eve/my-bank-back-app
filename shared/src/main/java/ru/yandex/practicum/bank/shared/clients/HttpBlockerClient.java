package ru.yandex.practicum.bank.shared.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.bank.shared.exceptions.BlockerClientException;
import ru.yandex.practicum.bank.shared.interfaces.BlockerClient;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckResponseViewModel;

/**
 * <summary>
 * HTTP-клиент для взаимодействия с сервисом блокировки операций (Blocker Service).
 * </summary>
 **/
@Component
public class HttpBlockerClient implements BlockerClient {

    // region Fields

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;
    private final SimpleCircuitBreaker circuitBreaker;

    // endregion

    // region Constructors

    @Autowired
    public HttpBlockerClient(
            RestClient.Builder restClientBuilder,
            @Value("${bank.services.blocker-service.base-url}") String blockerBaseUrl,
            ServiceTokenProvider serviceTokenProvider
    ) {
        this(
                restClientBuilder,
                blockerBaseUrl,
                serviceTokenProvider,
                SimpleCircuitBreaker.withDefaults("blockerService")
        );
    }

    HttpBlockerClient (
            RestClient.Builder restClientBuilder,
            String blockerBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            SimpleCircuitBreaker circuitBreaker
    ) {
        this.serviceTokenProvider = serviceTokenProvider;
        this.circuitBreaker = circuitBreaker;
        this.restClient = restClientBuilder
                .baseUrl(blockerBaseUrl)
                .build();
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Отправляет запрос на проверку операции в Blocker Service
     * с использованием паттерна Circuit Breaker.
     * </summary>
     * @param request Данные операции для проверки.
     * @return Результат проверки операции.
     * @throws BlockerClientException При ошибке взаимодействия с Blocker Service.
     **/
    @Override
    public OperationCheckResponseViewModel check(OperationCheckRequestViewModel request) {
        return circuitBreaker.execute(
                () -> checkWithoutCircuitBreaker(request),
                this::blockerFallback
        );
    }

    /**
     * <summary>
     * Отправляет POST-запрос с токеном авторизации Bearer в Blocker Service
     * без обертки Circuit Breaker.
     * </summary>
     * @param request Данные операции для проверки.
     * @return Результат проверки операции.
     * @throws BlockerClientException При сетевых сбоях или пустом ответе от Blocker Service.
     **/
    private OperationCheckResponseViewModel checkWithoutCircuitBreaker(OperationCheckRequestViewModel request) {
        try {
            var response = restClient.post()
                    .uri("/api/blocker/check")
                    .headers(headers -> headers.setBearerAuth(serviceTokenProvider.getAccessToken()))
                    .body(request)
                    .retrieve()
                    .body(OperationCheckResponseViewModel.class);

            if (response == null) {
                throw new BlockerClientException("Blocker service returned empty response");
            }

            return response;
        } catch (RestClientException exception) {
            throw new BlockerClientException("Blocker service request failed", exception);
        }
    }

    /**
     * <summary>
     * Обрабатывает сбой Circuit Breaker и преобразует исключение
     * в исключение клиента Blocker Service.
     * </summary>
     * @param exception Исключение, возникшее при выполнении запроса.
     * @return Результат проверки операции.
     * @throws BlockerClientException Если произошла ошибка взаимодействия с Blocker Service
     * или сервис временно недоступен.
     **/
    private OperationCheckResponseViewModel blockerFallback(Throwable exception) {
        if (exception instanceof BlockerClientException blockerClientException) {
            throw blockerClientException;
        }

        throw new BlockerClientException("Сервис проверки операций временно недоступен", exception);
    }

    // endregion
}