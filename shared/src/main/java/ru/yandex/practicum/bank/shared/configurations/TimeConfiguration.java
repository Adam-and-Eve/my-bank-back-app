package ru.yandex.practicum.bank.shared.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * <summary>
 * Конфигурация Spring для предоставления единого источника времени (Clock) в приложении.
 * </summary>
 */
@Configuration
public class TimeConfiguration {

    // region Beans

    /**
     * <summary>
     * Создает бин Clock, настроенный на системное время в часовом поясе UTC.
     * Использование бина Clock (вместо прямого вызова Instant.now()) позволяет управлять временем в приложении и легко мокать его в тестах.
     * </summary>
     * @return Экземпляр Clock в UTC.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    // endregion
}