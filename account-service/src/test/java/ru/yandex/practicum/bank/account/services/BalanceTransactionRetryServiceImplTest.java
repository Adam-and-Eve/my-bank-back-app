package ru.yandex.practicum.bank.account.services;

import org.junit.jupiter.api.Test;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <summary>
 * Юнит-тесты сервиса повторного выполнения транзакций (BalanceTransactionRetryServiceImpl).
 * Проверяют корректность выполнения Supplier-логики в штатном режиме и отсутствие препятствий для работы Spring Retry.
 * </summary>
 **/
public class BalanceTransactionRetryServiceImplTest {

    // region Fields

    private final BalanceTransactionRetryServiceImpl retryService = new BalanceTransactionRetryServiceImpl();

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешное выполнение переданной транзакции с первой попытки без возникновения исключений.
     * </summary>
     **/
    @Test
    public void shouldExecuteSuccessfullyOnFirstAttempt() {
        Supplier<String> transaction = () -> "success";

        var result = retryService.execute(transaction);

        assertThat(result).isEqualTo("success");
    }

    /**
     * <summary>
     * Проверяет, что неконтролируемые исключения (не связанные с Optimistic Locking)
     * пробрасываются наверх без перехвата и повторных попыток.
     * </summary>
     **/
    @Test
    public void shouldPropagateNonRetryableExceptionImmediately() {
        Supplier<String> transaction = () -> {
            throw new IllegalArgumentException("Invalid argument");
        };

        assertThatThrownBy(() -> retryService.execute(transaction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid argument");
    }

    /**
     * <summary>
     * Проверяет, что механизм выполнения корректно обрабатывает и возвращает числовые и сложные результаты.
     * </summary>
     **/
    @Test
    public void shouldReturnResultOfGenericType() {
        Supplier<Integer> transaction = () -> 42;

        var result = retryService.execute(transaction);

        assertThat(result).isEqualTo(42);
    }

    // endregion
}