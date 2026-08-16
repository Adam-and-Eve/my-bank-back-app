package ru.yandex.practicum.bank.cash.interfaces;

import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceResponseViewModel;

/**
 * <summary>
 * Контракт клиента для взаимодействия с сервисом счетов (Accounts Service).
 * </summary>
 **/
public interface AccountClient {

    // region Methods

    /**
     * <summary>
     * Выполняет операцию пополнения счета пользователя на указанную сумму.
     * </summary>
     * @param request Модель запроса на изменение баланса AccountBalanceOperationRequestViewModel.
     * <return>
     * @return Модель ответа AccountBalanceResponseViewModel с обновленным балансом счета.
     * </return>
     **/
    public AccountBalanceResponseViewModel deposit(AccountBalanceOperationRequestViewModel request);

    /**
     * <summary>
     * Выполняет операцию списания средств со счета пользователя.
     * </summary>
     * @param request Модель запроса на изменение баланса AccountBalanceOperationRequestViewModel.
     * <return>
     * @return Модель ответа AccountBalanceResponseViewModel с обновленным балансом счета.
     * </return>
     **/
    public AccountBalanceResponseViewModel withdraw(AccountBalanceOperationRequestViewModel request);

    // endregion
}