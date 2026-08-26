package ru.yandex.practicum.bank.frontui.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.bank.shared.clients.ResilientFactoryClient;

import java.time.Duration;

@Configuration
public class FrontUiConfiguration {

    // region Beans

    @Bean
    ResilientFactoryClient resilientFactoryClient(
            @Value("${bank.services.front-ui-service.http-client.circuit-breaker.failure-threshold:3}") int failureThreshold,
            @Value("${bank.services.front-ui-service.http-client.circuit-breaker.open-duration:5s}") Duration openDuration,
            @Value("${bank.services.front-ui-service.http-client.retry.max-attempts:2}") int maxAttempts,
            @Value("${bank.services.front-ui-service.http-client.retry.backoff:100ms}") Duration backoff
    ) {
        return new ResilientFactoryClient(
                failureThreshold,
                openDuration,
                maxAttempts,
                backoff);
    }

    // endregion
}