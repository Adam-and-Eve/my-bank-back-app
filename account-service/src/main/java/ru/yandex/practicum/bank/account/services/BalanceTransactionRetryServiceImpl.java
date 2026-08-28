package ru.yandex.practicum.bank.account.services;

import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.account.interfaces.BalanceTransactionRetryService;

import java.util.function.Supplier;

/**
 * <summary>
 * Реализация сервиса для выполнения балансовых транзакций с поддержкой автоматических повторных попыток (retry).
 * Использует механизм Spring Retry для перехвата исключений конкурентного обновления базы данных
 * и повторного запуска транзакции с заданными интервалами.
 * </summary>
 **/
@Service
public class BalanceTransactionRetryServiceImpl implements BalanceTransactionRetryService {

    // region Public Methods

    /**
     * <summary>
     * Выполняет переданную транзакционную логику (Supplier).
     * В случае конфликта версионирования данных (OptimisticLockException или ObjectOptimisticLockingFailureException)
     * метод автоматически повторяет выполнение. Количество попыток и задержка (backoff)
     * берутся из параметров конфигурации (bank.balance.retry).
     * </summary>
     * @param transaction Поставщик (Supplier), содержащий логику транзакции, которую необходимо выполнить.
     * @param <T> Тип возвращаемого значения.
     * @return Результат успешного выполнения транзакции.
     **/
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttemptsExpression = "${bank.balance.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${bank.balance.retry.backoff-ms}")
    )
    @Override
    public <T> T execute(Supplier<T> transaction) {
        return transaction.get();
    }

    // endregion
}