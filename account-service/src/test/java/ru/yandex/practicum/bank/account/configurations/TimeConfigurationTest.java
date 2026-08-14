package ru.yandex.practicum.bank.account.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Модульные тесты для конфигурационного класса TimeConfiguration.
 * Проверяют корректность создания и настройки бина {@link Clock}.
 * </summary>
 **/
public class TimeConfigurationTest {

    // region Fields

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TimeConfiguration.class);

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что бин {@link Clock} корректно регистрируется в контексте Spring
     * и использует системный часовой пояс по умолчанию.
     * </summary>
     **/
    @Test
    public void shouldRegisterClockBeanInSpringContext() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);

            Clock clock = context.getBean(Clock.class);

            assertThat(clock).isNotNull();

            assertThat(clock.getZone()).isEqualTo(ZoneId.systemDefault());
        });
    }

    /**
     * <summary>
     * Проверяет прямой вызов метода создания бина clock() без контекста Spring.
     * </summary>
     **/
    @Test
    public void shouldReturnClockWithSystemDefaultZoneDirectly() {
        TimeConfiguration timeConfiguration = new TimeConfiguration();

        Clock clock = timeConfiguration.clock();

        assertThat(clock).isNotNull();

        assertThat(clock.getZone()).isEqualTo(ZoneId.systemDefault());
    }

    // endregion
}