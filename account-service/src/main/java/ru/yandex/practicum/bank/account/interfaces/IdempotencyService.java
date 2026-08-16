package ru.yandex.practicum.bank.account.interfaces;

import ru.yandex.practicum.bank.account.exceptions.IdempotencyConflictException;
import ru.yandex.practicum.bank.account.exceptions.OperationAlreadyFailedException;
import ru.yandex.practicum.bank.account.exceptions.OperationInProgressException;

import java.util.function.Supplier;

/**
 * <summary>
 * Контракт сервиса обеспечения идемпотентности выполняемых операций (IdempotencyServiceImpl).
 * </summary>
 **/
public interface IdempotencyService {

    // region Methods

    /**
     * <summary>
     * Выполняет бизнес-операцию с обеспечением гарантии идемпотентности.
     * При повторном вызове с тем же operationId возвращает сохраненный результат или выбрасывает соответствующее исключение.
     * </summary>
     * @param operationId Уникальный идентификатор операции (ключ идемпотентности).
     * @param operationType Наименование типа операции.
     * @param request Объект запроса для расчета SHA-256 хеша.
     * @param responseType Класс ожидаемого результата.
     * @param businessOperation Функция выполнения бизнес-логики.
     * @param <T> Тип возвращаемого результата.
     * @return Результат выполнения бизнес-операции или ранее сохранённый ответ.
     * @throws IdempotencyConflictException Если операция с таким ID выполнялась с другими параметрами.
     * @throws OperationInProgressException Если операция с таким ID сейчас находится в процессе выполнения.
     * @throws OperationAlreadyFailedException Если операция с таким ID ранее завершилась ошибкой.
     */
    public <T> T execute(
            String operationId,
            String operationType,
            Object request,
            Class<T> responseType,
            Supplier<T> businessOperation
    );

    // endregion
}