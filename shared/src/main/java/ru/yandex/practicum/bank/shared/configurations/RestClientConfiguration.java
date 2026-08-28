package ru.yandex.practicum.bank.shared.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import ru.yandex.practicum.bank.shared.clients.ResilientFactoryClient;

import java.time.Duration;

/**
 * <summary>
 * Общая конфигурация RestClient с настройкой таймаутов и отказоустойчивости (Resilience).
 * </summary>
 **/
@Configuration
public class RestClientConfiguration {

    // region Beans

    /**
     * <summary>
     * Создает кастомизатор для RestClient, который глобально задает таймауты
     * на подключение и чтение данных через SimpleClientHttpRequestFactory.
     * </summary>
     * @param connectTimeout Таймаут на установку соединения (по умолчанию 2s).
     * @param readTimeout Таймаут на чтение ответа (по умолчанию 5s).
     * @return Настроенный RestClientCustomizer.
     */
    @Bean
    RestClientCustomizer restClientCustomizer(
            @Value("${bank.http-client.connect-timeout:2s}") Duration connectTimeout,
            @Value("${bank.http-client.read-timeout:5s}") Duration readTimeout
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return builder -> builder.requestFactory(requestFactory);
    }

    /**
     * <summary>
     * Создает фабрику для построения отказоустойчивых HTTP-клиентов с поддержкой
     * паттернов Circuit Breaker и Retry.
     * </summary>
     * @param failureThreshold Количество неудачных запросов для открытия Circuit Breaker (по умолчанию 3).
     * @param openDuration Время, на которое открывается Circuit Breaker перед повторной проверкой (по умолчанию 5s).
     * @param maxAttempts Максимальное количество попыток Retry (по умолчанию 2).
     * @param backoff Задержка между попытками Retry (по умолчанию 100ms).
     * @return Экземпляр ResilientFactoryClient с заданными параметрами.
     */
    @Bean
    ResilientFactoryClient resilientClientFactory(
            @Value("${bank.http-client.circuit-breaker.failure-threshold:3}") int failureThreshold,
            @Value("${bank.http-client.circuit-breaker.open-duration:5s}") Duration openDuration,
            @Value("${bank.http-client.retry.max-attempts:2}") int maxAttempts,
            @Value("${bank.http-client.retry.backoff:100ms}") Duration backoff
    ) {
        return new ResilientFactoryClient(failureThreshold, openDuration, maxAttempts, backoff);
    }

    // endregion
}