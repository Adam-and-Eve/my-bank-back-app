package ru.yandex.practicum.bank.frontui.interfaces;

import ru.yandex.practicum.bank.frontui.viewmodels.*;

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
     * Выполняет перевод денежных средств между счетами.
     * </summary>
     * @param accessToken OAuth2 Access Token пользователя.
     * @param form Данные формы перевода.
     * @return Результат выполнения перевода.
     **/
    public TransferResponseViewModel transfer(String accessToken, TransferFormViewModel form);

    /**
     * <summary>
     * Запрашивает информацию о текущем аккаунте пользователя.
     * </summary>
     * @param accessToken OAuth2 Access Token пользователя.
     * @return Информация об аккаунте.
     **/
    public AccountResponseViewModel getAccount(String accessToken);

    /**
     * <summary>
     * Обновляет профиль аккаунта пользователя.
     * </summary>
     * @param accessToken OAuth2 Access Token пользователя.
     * @param form Форма с новыми данными профиля.
     * @return Обновленная информация об аккаунте.
     **/
    public AccountResponseViewModel updateAccount(String accessToken, AccountFormViewModel form);

    /**
     * <summary>
     * Запрашивает список доступных получателей переводов.
     * </summary>
     * @param accessToken OAuth2 Access Token пользователя.
     * @return Список получателей.
     **/
    public List<RecipientResponseViewModel> getRecipients(String accessToken);

    /**
     * <summary>
     * Выполняет операцию внесения наличных на счет (депозит).
     * </summary>
     * @param accessToken OAuth2 Access Token пользователя.
     * @param form Форма внесения наличных.
     * @return Результат кассовой операции.
     **/
    public CashOperationResponseViewModel deposit(String accessToken, CashFormViewModel form);

    /**
     * <summary>
     * Выполняет операцию снятия наличных со счета.
     * </summary>
     * @param accessToken OAuth2 Access Token пользователя.
     * @param form Форма снятия наличных.
     * @return Результат кассовой операции.
     **/
    public CashOperationResponseViewModel withdraw(String accessToken, CashFormViewModel form);

    // endregion
}