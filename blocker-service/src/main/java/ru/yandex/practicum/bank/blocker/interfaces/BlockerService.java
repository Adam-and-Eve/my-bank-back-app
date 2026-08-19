package ru.yandex.practicum.bank.blocker.interfaces;

import ru.yandex.practicum.bank.blocker.exceptions.InvalidOperationRequestException;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckResponseViewModel;

/**
 * <summary>
 * Контракт сервиса для проверки банковских операций на соответствие ограничениям блокировки.
 * </summary>
 */
public interface BlockerService {

    // region Methods

    /**
     * <summary>
     * Проверяет банковскую операцию на соответствие ограничениям блокировки.
     * Проверяет участников операции, базовую валюту и максимально допустимую сумму.
     * </summary>
     * @param request Запрос с данными проверяемой банковской операции.
     * @return Результат проверки операции с признаком разрешения и причиной отказа.
     * @throws InvalidOperationRequestException Если данные операции некорректны.
     */
    public OperationCheckResponseViewModel check(OperationCheckRequestViewModel request);

    // endregion
}