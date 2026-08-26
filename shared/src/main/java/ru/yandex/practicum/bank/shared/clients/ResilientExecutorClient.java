package ru.yandex.practicum.bank.shared.clients;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import java.util.function.Function;
import java.util.function.Supplier;

public class ResilientExecutorClient {

    // region Fields

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    // endregion

    // region Constructors

    ResilientExecutorClient(CircuitBreaker circuitBreaker, Retry retry) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    // endregion

    // region Methods

    public static ResilientExecutorClient opened(String name) {
        var factory = ResilientFactoryClient.withDefaults();

        var executor = factory.create(name);

        executor.circuitBreaker.transitionToOpenState();

        return executor;
    }

    public <T> T execute(Supplier<T> action, Function<Throwable, T> fallback) {
        try {
            Supplier<T> decorated = Retry.decorateSupplier(retry, action);

            decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);

            return decorated.get();
        } catch (Exception exception) {
            return fallback.apply(exception);
        }
    }

    // endregion
}