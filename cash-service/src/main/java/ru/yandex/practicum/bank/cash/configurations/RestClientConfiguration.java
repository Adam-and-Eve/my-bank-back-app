package ru.yandex.practicum.bank.cash.configurations;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * <summary>
 * Конфигурация RestClient для сервиса наличных операций (Cash Service).
 * Предоставляет строитель RestClient.Builder с поддержкой клиентской балансировки
 * нагрузки для межсервисного взаимодействия через Eureka.
 * </summary>
 **/
@Configuration
public class RestClientConfiguration {

    // region Beans

    /**
     * <summary>
     * Создает бин RestClient.Builder с аннотацией @LoadBalanced для выполнения HTTP-запросов
     * к другим микросервисам по их логическим именам в реестре Eureka.
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