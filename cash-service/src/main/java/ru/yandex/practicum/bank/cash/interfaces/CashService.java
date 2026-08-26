package ru.yandex.practicum.bank.cash.interfaces;

import ru.yandex.practicum.bank.cash.viewmodels.CashOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;

import java.util.UUID;

/**
 * <summary>
 * Контракт сервиса операций с наличностью (Cash Service).
 * </summary>
 **/
public interface CashService {

    // region Methods

    /**
     * <summary>
     * Выполняет операцию пополнения счета пользователя наличностью.
     * Валидирует сумму, запрашивает пополнение у сервиса счетов и инициирует отправку уведомления.
     * </summary>
     * @param login Логин пользователя, выполняющего пополнение.
     * @param request Модель запроса на операцию с наличностью.
     * <return>
     * @return Модель ответа CashOperationResponseViewModel с обновленным балансом и статусом.
     * </return>
     **/
    public CashOperationResponseViewModel deposit(String login, CashOperationRequestViewModel request, UUID operationId);

    /**
     * <summary>
     * Выполняет операцию снятия наличности со счета пользователя.
     * Валидирует сумму, запрашивает списание у сервиса счетов и инициирует отправку уведомления.
     * </summary>
     * @param login Логин пользователя, выполняющего снятие.
     * @param request Модель запроса на операцию с наличностью.
     * <return>
     * @return Модель ответа CashOperationResponseViewModel с обновленным балансом и статусом.
     * </return>
     **/
    public CashOperationResponseViewModel withdraw(String login, CashOperationRequestViewModel request, UUID operationId);

    // endregion
}