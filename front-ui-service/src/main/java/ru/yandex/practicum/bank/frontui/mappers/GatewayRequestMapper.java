package ru.yandex.practicum.bank.frontui.mappers;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.frontui.viewmodels.*;

/**
 * <summary>
 * Маппер для преобразования форм веб-интерфейса (FormViewModel)
 * в соответствущие DTO-запросы к API Gateway.
 * </summary>
 **/
@Component
public class GatewayRequestMapper {

    /**
     * <summary>
     * Преобразует форму перевода в DTO запроса к API.
     * </summary>
     * @param form Форма перевода средств.
     * @return DTO запроса перевода.
     **/
    public TransferRequestViewModel toTransferRequest(TransferFormViewModel form) {
        return new TransferRequestViewModel(form.recipientLogin(), form.amount(), form.currency());
    }

    /**
     * <summary>
     * Преобразует форму профиля в DTO запроса обновления аккаунта.
     * </summary>
     * @param form Форма редактирования аккаунта.
     * @return DTO запроса обновления.
     **/
    public UpdateAccountRequestViewModel toUpdateAccountRequest(AccountFormViewModel form) {
        return new UpdateAccountRequestViewModel(form.name(), form.birthdate());
    }

    /**
     * <summary>
     * Преобразует форму кассовой операции в DTO запроса.
     * </summary>
     * @param form Форма ввода суммы и валюты.
     * @return DTO запроса кассовой операции.
     **/
    public CashOperationRequestViewModel toCashOperationRequest(CashFormViewModel form) {
        return new CashOperationRequestViewModel(form.amount(), form.currency());
    }
}