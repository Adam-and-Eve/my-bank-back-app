package ru.yandex.practicum.bank.account.interfaces;

import java.util.function.Supplier;

/**
 * <summary>
 * Контракт сервиса для выполнения транзакций, связанных с балансом,
 * с поддержкой механизма автоматических повторных попыток (retry).
 * </summary>
 **/
public interface BalanceTransactionRetryService {

    // region Public Methods

    /**
     * <summary>
     * Выполняет переданную транзакционную логику (Supplier). В случае возникновения
     * восстановимых конфликтов механизм совершает заданное количество повторных попыток
     * выполнения операции с задержкой (backoff).
     * </summary>
     * @param transaction Поставщик (Supplier), содержащий бизнес-логику транзакции.
     * @param <T> Тип возвращаемого значения транзакции.
     * @return Результат успешного выполнения транзакции.
     **/
    public <T> T execute(Supplier<T> transaction);

    // endregion
}