package ru.yandex.practicum.bank.transfer.mappers;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.transfer.viewmodels.AccountTransferRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferOperationViewModel;

/**
 * <summary>
 * Маппер для преобразования моделей данных при взаимодействии с сервисом счетов (Accounts Service).
 * </summary>
 **/
@Component
public class AccountTransferMapper {

    // region Methods

    /**
     * <summary>
     * Преобразует общую модель операции перевода TransferOperationViewModel в DTO запроса для сервиса счетов.
     * </summary>
     * @param operation Данные о выполняемой операции перевода.
     * <return>
     * @return Экземпляр DTO AccountTransferRequestViewModel.
     * </return>
     **/
    public AccountTransferRequestViewModel toAccountRequest(TransferOperationViewModel operation) {
        return new AccountTransferRequestViewModel(
                operation.senderLogin(),
                operation.recipientLogin(),
                operation.amount(),
                operation.currency(),
                operation.operationId()
        );
    }

    // endregion
}