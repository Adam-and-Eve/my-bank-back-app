package ru.yandex.practicum.bank.cash.mappers;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationRequestViewModel;

/**
 * <summary>
 * Маппер для преобразования входных моделей запросов операций с наличностью
 * в модели запросов изменения баланса для сервиса счетов (Accounts Service).
 * </summary>
 **/
@Component
public class AccountBalanceMapper {

    // region Methods

    /**
     * <summary>
     * Преобразует модель запроса операции с наличностью (CashOperationRequestViewModel)
     * в модель запроса изменения баланса счета (AccountBalanceOperationRequestViewModel).
     * </summary>
     * @param login Логин пользователя, выполняющего операцию.
     * @param request Входящая модель запроса операции с наличностью.
     * @param operationId Уникальный идентификатор операции для обеспечения идемпотентности.
     * <return>
     * @return Модель запроса изменения баланса счета AccountBalanceOperationRequestViewModel.
     * </return>
     **/
    public AccountBalanceOperationRequestViewModel toAccountsRequest(
            String login,
            CashOperationRequestViewModel request,
            String operationId
    ) {
        return new AccountBalanceOperationRequestViewModel(
                login,
                request.amount(),
                request.currency(),
                operationId
        );
    }

    // endregion
}