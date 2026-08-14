package ru.yandex.practicum.bank.account.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * <summary>
 * Конфигурация компонентов работы со временем для сервиса аккаунтов (account-service).
 * Предоставляет бин {@link Clock} для внедрения зависимостей, связанных с получением текущего времени,
 * что позволяет изолировать системные часы и упрощает модульное тестирование.
 * </summary>
 **/
@Configuration
public class TimeConfiguration {

    // region Beans

    /**
     * <summary>
     * Создает и предоставляет бин {@link Clock}, настроенный на системный часовой пояс по умолчанию.
     * </summary>
     * @return Экземпляр {@link Clock} для работы с системным временем.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    // endregion
}