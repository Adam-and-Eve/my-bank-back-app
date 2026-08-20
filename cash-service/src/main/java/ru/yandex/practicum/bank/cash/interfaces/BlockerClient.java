package ru.yandex.practicum.bank.cash.interfaces;

import ru.yandex.practicum.bank.cash.exceptions.BlockerClientException;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckResponseViewModel;

/**
 * <summary>
 * Контракт HTTP-клиента для взаимодействия с сервисом блокировки операций (Blocker Service).
 * </summary>
 **/
public interface BlockerClient {

    // region Methods

    /**
     * <summary>
     * Отправляет запрос на проверку операции в Blocker Service
     * с использованием паттерна Circuit Breaker.
     * </summary>
     * @param request Данные операции для проверки.
     * @return Результат проверки операции.
     * @throws BlockerClientException При ошибке взаимодействия с Blocker Service.
     **/
    OperationCheckResponseViewModel check(OperationCheckRequestViewModel request);

    // endregion
}