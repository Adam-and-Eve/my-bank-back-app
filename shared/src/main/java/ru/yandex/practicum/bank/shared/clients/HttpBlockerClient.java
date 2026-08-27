package ru.yandex.practicum.bank.shared.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;
    private final ResilientExecutorClient clientExecutor;

    private static final Logger log = LoggerFactory.getLogger(HttpBlockerClient.class);

    @Autowired
    public HttpBlockerClient(
            RestClient.Builder restClientBuilder,
            @Value("${bank.services.blocker-service.base-url}") String blockerBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ResilientFactoryClient resilientClientFactory
    ) {
        this(
                restClientBuilder,
                blockerBaseUrl,
                serviceTokenProvider,
                resilientClientFactory.create("blockerService")
        );
    }

    HttpBlockerClient(
            RestClient.Builder restClientBuilder,
            String blockerBaseUrl,
            ServiceTokenProvider serviceTokenProvider
    ) {
        this(
                restClientBuilder,
                blockerBaseUrl,
                serviceTokenProvider,
                ResilientFactoryClient.withDefaults()
        );
    }

    HttpBlockerClient(
            RestClient.Builder restClientBuilder,
            String blockerBaseUrl,
            ServiceTokenProvider serviceTokenProvider,
            ResilientExecutorClient clientExecutor
    ) {
        this.serviceTokenProvider = serviceTokenProvider;
        this.clientExecutor = clientExecutor;
        this.restClient = restClientBuilder
                .baseUrl(blockerBaseUrl)
                .build();
    }

    @Override
    public OperationCheckResponseViewModel check(OperationCheckRequestViewModel request) {
        return clientExecutor.execute(
                () -> checkWithoutCircuitBreaker(request),
                this::blockerFallback
        );
    }

    private OperationCheckResponseViewModel checkWithoutCircuitBreaker(OperationCheckRequestViewModel request) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Blocker downstream request prepared operationId={} operationType={} currency={} source=transfer-service targetService=blocker-service",
                        request.operationId(),
                        request.operationType(),
                        request.currency()
                );
            }

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
            log.error(
                    "Blocker downstream request failed operationId={} operationType={} currency={} status=error errorCategory=downstream_unavailable errorType={} source=transfer-service targetService=blocker-service",
                    request.operationId(),
                    request.operationType(),
                    request.currency(),
                    exception.getClass().getSimpleName()
            );

            throw new BlockerClientException("Blocker service request failed", exception);
        }
    }

    private OperationCheckResponseViewModel blockerFallback(Throwable exception) {
        if (exception instanceof BlockerClientException blockerClientException) {
            throw blockerClientException;
        }

        log.error(
                "Blocker downstream retries exhausted status=error errorCategory=downstream_unavailable errorType={} source=transfer-service targetService=blocker-service",
                exception.getClass().getSimpleName()
        );

        throw new BlockerClientException("Сервис проверки операций временно недоступен", exception);
    }
}