package ru.yandex.practicum.bank.shared.clients;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.bank.shared.models.StateModel;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Юнит-тесты для проверки корректности переключения состояний и логики работы SimpleCircuitBreaker.
 * </summary>
 **/
public class SimpleCircuitBreakerTest {

    // region Tests

    /**
     * <summary>
     * Проверяет, что при достижении лимита ошибок цепь размыкается (OPEN),
     * а последующие вызовы сразу возвращают fallback без выполнения действия.
     * </summary>
     **/
    @Test
    @DisplayName("Должен размыкать цепь после превышения порога ошибок и возвращать fallback без вызова action")
    void shouldOpenAfterFailureThresholdAndUseFallbackWithoutActionCall() {
        var circuitBreaker = SimpleCircuitBreaker.withDefaults("accountsService");

        var calls = new AtomicInteger();

        assertThat(circuitBreaker.getState()).isEqualTo(StateModel.CLOSED);

        for (int attempt = 0; attempt < 3; attempt++) {
            var result = circuitBreaker.execute(
                    () -> {
                        calls.incrementAndGet();
                        throw new IllegalStateException("Service unavailable");
                    },
                    exception -> "fallback"
            );

            assertThat(result).isEqualTo("fallback");
        }

        assertThat(circuitBreaker.getState()).isEqualTo(StateModel.OPEN);

        assertThat(circuitBreaker.getFailures()).isEqualTo(3);

        var result = circuitBreaker.execute(
                () -> {
                    calls.incrementAndGet();

                    return "real response";
                },
                exception -> "fallback"
        );

        assertThat(result).isEqualTo("fallback");

        assertThat(calls).hasValue(3);
    }

    /**
     * <summary>
     * Проверяет, что успешный вызов в закрытом состоянии (CLOSED) сбрасывает накопленный счетчик ошибок.
     * </summary>
     **/
    @Test
    @DisplayName("Должен сбрасывать счетчик ошибок при успешном ответе в состоянии CLOSED")
    void shouldResetFailuresOnSuccessInClosedState() {
        var circuitBreaker = SimpleCircuitBreaker.withDefaults("cashService");

        for (int i = 0; i < 2; i++) {
            circuitBreaker.execute(
                    () -> { throw new RuntimeException("Error"); },
                    ex -> "fallback"
            );
        }
        assertThat(circuitBreaker.getFailures()).isEqualTo(2);

        var result = circuitBreaker.execute(() -> "success", ex -> "fallback");

        assertThat(result).isEqualTo("success");

        assertThat(circuitBreaker.getFailures()).isEqualTo(0);

        assertThat(circuitBreaker.getState()).isEqualTo(StateModel.CLOSED);
    }

    /**
     * <summary>
     * Проверяет повторное размыкание цепи (HALF_OPEN -> OPEN), если пробный запрос завершился ошибкой.
     * </summary>
     **/
    @Test
    @DisplayName("Должен снова размыкать цепь (OPEN), если пробный запрос в состоянии HALF_OPEN завершился ошибкой")
    void shouldReopenIfTrialRequestFailsInHalfOpenState() {
        var startInstant = Instant.parse("2026-08-13T10:00:00Z");

        var testClock = new MutableClock(startInstant, ZoneId.systemDefault());

        var circuitBreaker = new SimpleCircuitBreaker("cardService", 2, Duration.ofSeconds(5), testClock);

        for (int i = 0; i < 2; i++) {
            circuitBreaker.execute(() -> { throw new RuntimeException("Fail"); }, ex -> "fallback");
        }

        testClock.addSeconds(6);

        String result = circuitBreaker.execute(
                () -> { throw new RuntimeException("Still broken"); },
                ex -> "fallback"
        );

        assertThat(result).isEqualTo("fallback");

        assertThat(circuitBreaker.getState()).isEqualTo(StateModel.OPEN);
    }

    /**
     * <summary>
     * Проверяет переход OPEN -> HALF_OPEN -> CLOSED после истечения таймаута ожидания при успешном пробном запросе.
     * </summary>
     **/
    @Test
    @DisplayName("Должен переходить в HALF_OPEN по истечении таймаута и замыкать цепь (CLOSED) при успешном пробном запросе")
    void shouldTransitionToHalfOpenAndCloseOnSuccessAfterTimeout() {
        var startInstant = Instant.parse("2026-08-13T10:00:00Z");

        var testClock = new MutableClock(startInstant, ZoneId.systemDefault());

        var circuitBreaker = new SimpleCircuitBreaker("transferService", 2, Duration.ofSeconds(5), testClock);

        for (int i = 0; i < 2; i++) {
            circuitBreaker.execute(() -> { throw new RuntimeException("Fail"); }, ex -> "fallback");
        }

        assertThat(circuitBreaker.getState()).isEqualTo(StateModel.OPEN);

        testClock.addSeconds(6);

        String result = circuitBreaker.execute(() -> "recovered response", ex -> "fallback");

        assertThat(result).isEqualTo("recovered response");

        assertThat(circuitBreaker.getState()).isEqualTo(StateModel.CLOSED);

        assertThat(circuitBreaker.getFailures()).isEqualTo(0);
    }

    // endregion

    // region Objects

    /**
     * <summary>
     * Управляемые часы для симуляции хода времени в тестах без вызова Thread.sleep().
     * </summary>
     **/
    private static class MutableClock extends Clock {

        // region Fields

        private Instant instant;
        private final ZoneId zone;

        // endregion

        // region Constructors

        public MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        // endregion

        // region Properties

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        // endregion

        // region Methods

        public void addSeconds(long seconds) {
            this.instant = this.instant.plusSeconds(seconds);
        }

        // endregion

    }

    // endregion
}