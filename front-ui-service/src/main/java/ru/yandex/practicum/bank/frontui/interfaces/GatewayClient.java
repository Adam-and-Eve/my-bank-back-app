package ru.yandex.practicum.bank.frontui.interfaces;

import ru.yandex.practicum.bank.frontui.viewmodels.*;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;

import java.util.List;

/**
 * <summary>
 * Контракт клиента для взаимодействия с API Gateway.
 * </summary>
 **/
public interface GatewayClient {

    // region Methods

    /**
     * <summary>
     * Выполняет перевод денежных средств через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма перевода денежных средств.
     * @return Результат выполнения перевода.
     **/
    public TransferResponseViewModel transfer(String accessToken, TransferFormViewModel form);

    /**
     * <summary>
     * Получает данные текущего счета через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @return Данные текущего счета.
     **/
    public AccountResponseViewModel getAccount(String accessToken);

    /**
     * <summary>
     * Получает текущие курсы валют через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @return Список текущих курсов валют.
     **/
    public List<ExchangeRateResponseViewModel> getExchangeRates(String accessToken);

    /**
     * <summary>
     * Обновляет данные текущего счета через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма с обновляемыми данными счета.
     * @return Обновленные данные счета.
     **/
    public AccountResponseViewModel updateAccount(
            String accessToken,
            AccountFormViewModel form);

    /**
     * <summary>
     * Получает список получателей через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @return Список доступных получателей.
     **/
    public List<RecipientResponseViewModel> getRecipients(String accessToken);

    /**
     * <summary>
     * Выполняет депозит через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма кассовой операции.
     * @return Результат выполнения депозита.
     **/
    public CashOperationResponseViewModel deposit(
            String accessToken,
            CashFormViewModel form);

    /**
     * <summary>
     * Выполняет снятие денежных средств через API Gateway с использованием CircuitBreaker.
     * </summary>
     * @param accessToken Bearer токен аутентификации.
     * @param form Форма кассовой операции.
     * @return Результат выполнения снятия денежных средств.
     **/
    public CashOperationResponseViewModel withdraw(
            String accessToken,
            CashFormViewModel form);

    // endregion
}