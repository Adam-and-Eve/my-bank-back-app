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
     * Преобразует модель выполняемой операции перевода в модель запроса,
     * предназначенную для отправки в сервис счетов.
     * </summary>
     * @param operation Модель выполняемой операции перевода.
     * @return Модель HTTP-запроса для сервиса счетов.
     **/
    public AccountTransferRequestViewModel toAccountRequest(
            TransferOperationViewModel operation) {

        return new AccountTransferRequestViewModel(
                operation.senderLogin(),
                operation.recipientLogin(),
                operation.amount(),
                operation.currency(),
                operation.recipientAmount(),
                operation.recipientCurrency(),
                operation.operationId()
        );
    }

    // endregion
}