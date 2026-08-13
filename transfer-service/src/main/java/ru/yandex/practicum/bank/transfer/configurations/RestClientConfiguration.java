package ru.yandex.practicum.bank.transfer.configurations;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * <summary>
 * Конфигурация RestClient для выполнения межсервисных HTTP-запросов в сервисе переводов (Transfer Service).
 * Настраивает билдер HTTP-клиента с поддержкой клиентской балансировки нагрузки.
 * </summary>
 **/
@Configuration
public class RestClientConfiguration {

    // region Beans

    /**
     * <summary>
     * Создает билдер RestClient.Builder с аннотацией @LoadBalanced для автоматической интеграции с Service Discovery (Eureka)
     * и клиентской балансировкой нагрузки при обращении к микросервисам по их именам.
     * </summary>
     * <return>
     * @return Экземпляр RestClient.Builder с поддержкой балансировки нагрузки.
     * </return>
     **/
    @Bean
    @LoadBalanced
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // endregion
}