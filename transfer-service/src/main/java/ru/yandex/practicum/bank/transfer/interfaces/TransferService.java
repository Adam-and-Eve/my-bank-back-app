package ru.yandex.practicum.bank.transfer.interfaces;

import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.transfer.exceptions.SelfTransferForbiddenException;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResponseViewModel;

/**
 * <summary>
 * Контракт сервиса выполнения операций перевода денежных средств между пользователями.
 * </summary>
 **/
public interface TransferService {

    // region Methods

    /**
     * <summary>
     * Выполняет перевод средств от отправителя к получателю.
     * </summary>
     * @param senderLogin Уникальный логин пользователя-отправителя.
     * @param request Запрос на проведение перевода.
     * <return>
     * @return Ответ с информацией о результатах выполненного перевода.
     * </return>
     * @throws InvalidAmountException Если сумма перевода не превышает ноль.
     * @throws InvalidAmountScaleException Если количество знаков после запятой превышает 2.
     * @throws SelfTransferForbiddenException Если попытка перевода производится на собственный аккаунт.
     **/
    public TransferResponseViewModel transfer(
            String senderLogin,
            TransferRequestViewModel request);

    // endregion
}