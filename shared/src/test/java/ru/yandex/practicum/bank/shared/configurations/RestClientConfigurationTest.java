package ru.yandex.practicum.bank.shared.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import ru.yandex.practicum.bank.shared.clients.ResilientFactoryClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Интеграционные тесты конфигурации RestClientConfiguration.
 * Проверяют создание и регистрацию RestClientCustomizer и ResilientFactoryClient
 * с использованием заданных параметров таймаутов, повторных попыток и Circuit Breaker.
 * </summary>
 **/
@SpringBootTest(classes = RestClientConfiguration.class)
@TestPropertySource(properties = {
        "bank.http-client.connect-timeout=5s",
        "bank.http-client.read-timeout=5s",
        "bank.http-client.circuit-breaker.failure-threshold=5",
        "bank.http-client.circuit-breaker.open-duration=10s",
        "bank.http-client.retry.max-attempts=3",
        "bank.http-client.retry.backoff=200ms"
})
class RestClientConfigurationTest {

    @Autowired
    private RestClientConfiguration configuration;

    @Autowired
    private ResilientFactoryClient resilientClientFactory;

    @Test
    void shouldRegisterRestClientCustomizerBean() {
        var customizer = configuration.restClientCustomizer(
                Duration.ofSeconds(5),
                Duration.ofSeconds(5)
        );

        assertThat(customizer).isNotNull();
    }

    @Test
    void shouldRegisterResilientClientFactoryBean() {
        assertThat(resilientClientFactory).isNotNull();
    }
}