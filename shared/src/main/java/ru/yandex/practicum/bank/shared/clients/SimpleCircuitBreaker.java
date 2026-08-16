package ru.yandex.practicum.bank.shared.clients;

import ru.yandex.practicum.bank.shared.models.StateModel;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <summary>
 * Простая и легкая реализация паттерна Circuit Breaker (Предохранитель).
 * Предотвращает каскадные сбои в микросервисной архитектуре при недоступности внешних сервисов.
 * </summary>
 **/
public class SimpleCircuitBreaker {

    // region Constants

    private static final int DEFAULT_FAILURE_THRESHOLD = 3;
    private static final Duration DEFAULT_OPEN_DURATION = Duration.ofSeconds(5);

    // endregion

    // region Fields

    private final String name;
    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;

    private StateModel state = StateModel.CLOSED;
    private int failures;
    private Instant openedAt;

    // endregion

    // region Constructors

    SimpleCircuitBreaker(
            String name,
            int failureThreshold,
            Duration openDuration,
            Clock clock) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    // endregion

    // region Properties

    /**
     * <summary>
     * Возвращает имя текущего предохранителя.
     * </summary>
     * <return>
     * @return Строковое наименование предохранителя.
     * </return>
     **/
    public String getName() {
        return name;
    }

    /**
     * <summary>
     * Возвращает текущее состояние предохранителя.
     * </summary>
     * <return>
     * @return Значение enum StateModel (CLOSED, OPEN, HALF_OPEN).
     * </return>
     **/
    public synchronized StateModel getState() {
        return state;
    }

    /**
     * <summary>
     * Возвращает текущее количество подряд зафиксированных ошибок.
     * </summary>
     * <return>
     * @return Число ошибок.
     * </return>
     **/
    public synchronized int getFailures() {
        return failures;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Создает экземпляр SimpleCircuitBreaker с настраиваемыми параметрами порога ошибок и таймаута.
     * </summary>
     * @param name Уникальное наименование предохранителя.
     * @param failureThreshold Порог количества ошибок подряд для размыкания цепи.
     * @param openDuration Длительность нахождения цепи в состоянии OPEN перед пробным запросом.
     * <return>
     * @return Экземпляр SimpleCircuitBreaker с пользовательскими параметрами.
     * </return>
     **/
    public static SimpleCircuitBreaker create(
            String name,
            int failureThreshold,
            Duration openDuration) {
        return new SimpleCircuitBreaker(name, failureThreshold, openDuration, Clock.systemUTC());
    }

    /**
     * <summary>
     * Создает экземпляр SimpleCircuitBreaker с параметрами по умолчанию (3 ошибки, 5 секунд паузы).
     * </summary>
     * @param name Уникальное наименование предохранителя.
     * <return>
     * @return Экземпляр SimpleCircuitBreaker с дефолтными параметрами.
     * </return>
     **/
    public static SimpleCircuitBreaker withDefaults(String name) {
        return new SimpleCircuitBreaker(name, DEFAULT_FAILURE_THRESHOLD, DEFAULT_OPEN_DURATION, Clock.systemUTC());
    }

    /**
     * <summary>
     * Создает экземпляр SimpleCircuitBreaker, изначально находящийся в состоянии OPEN.
     * </summary>
     * @param name Уникальное наименование предохранителя.
     * <return>
     * @return Экземпляр SimpleCircuitBreaker в состоянии OPEN.
     * </return>
     **/
    public static SimpleCircuitBreaker opened(String name) {
        var circuitBreaker = withDefaults(name);

        circuitBreaker.open();

        return circuitBreaker;
    }

    /**
     * <summary>
     * Выполняет целевую внешнюю операцию под защитой предохранителя.
     * </summary>
     * @param action Целевая внешняя операция.
     * @param fallback Резервная функция, вызываемая при возникновении исключения или открытом состоянии цепи.
     * <return>
     * @return Результат выполнения действия action или резервной функции fallback.
     * </return>
     **/
    public <T> T execute(Supplier<T> action, Function<Throwable, T> fallback) {
        synchronized (this) {
            if (state == StateModel.OPEN && !isReadyForRetry()) {
                return fallback.apply(new IllegalStateException("Circuit breaker is open: " + name));
            }
            if (state == StateModel.OPEN) {
                state = StateModel.HALF_OPEN;
            }
        }

        try {
            T result = action.get();

            synchronized (this) {
                close();
            }

            return result;
        } catch (Throwable exception) {
            synchronized (this) {
                recordFailure();
            }

            return fallback.apply(exception);
        }
    }

    /**
     * <summary>
     * Проверяет, истек ли таймаут ожидания для перевода предохранителя в состояние HALF_OPEN.
     * </summary>
     * <return>
     * @return true, если с момента размыкания прошло достаточно времени для повторной попытки.
     * </return>
     **/
    private boolean isReadyForRetry() {
        return openedAt != null && !clock.instant().isBefore(openedAt.plus(openDuration));
    }

    /**
     * <summary>
     * Фиксирует сбой при выполнении операции и при достижении лимита переводит предохранитель в состояние OPEN.
     * </summary>
     **/
    private void recordFailure() {
        if (state == StateModel.HALF_OPEN) {
            open();

            return;
        }

        failures++;

        if (failures >= failureThreshold) {
            open();
        }
    }

    /**
     * <summary>
     * Переводит предохранитель в состояние OPEN и запоминает текущую метку времени.
     * </summary>
     **/
    private void open() {
        state = StateModel.OPEN;

        openedAt = clock.instant();
    }

    /**
     * <summary>
     * Сбрасывает предохранитель в исходное состояние CLOSED и обнуляет счетчики ошибок.
     * </summary>
     **/
    private void close() {
        state = StateModel.CLOSED;

        failures = 0;

        openedAt = null;
    }

    // endregion
}