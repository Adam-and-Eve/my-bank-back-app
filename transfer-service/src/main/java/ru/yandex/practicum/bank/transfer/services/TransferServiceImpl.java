package ru.yandex.practicum.bank.transfer.services;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.transfer.exceptions.SelfTransferForbiddenException;
import ru.yandex.practicum.bank.transfer.interfaces.NotificationClient;
import ru.yandex.practicum.bank.transfer.interfaces.TransferExecutor;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;
import ru.yandex.practicum.bank.transfer.viewmodels.NotificationRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferOperationViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResponseViewModel;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * <summary>
 * Сервис выполнения операций перевода денежных средств между пользователями.
 * Выполняет валидацию параметров перевода, вызывает исполнителя операции и отправляет уведомление.
 * </summary>
 **/
@Service
public class TransferServiceImpl implements TransferService {

    // region Fields

    private final TransferExecutor transferExecutor;
    private final NotificationClient notificationClient;

    // endregion

    // region Constructors

    public TransferServiceImpl(
            TransferExecutor transferExecutor,
            NotificationClient notificationClient) {
        this.transferExecutor = transferExecutor;
        this.notificationClient = notificationClient;
    }

    // endregion

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
    @Override
    public TransferResponseViewModel transfer(
            String senderLogin,
            TransferRequestViewModel request) {
        validateAmount(request.amount());

        if (senderLogin.equals(request.recipientLogin())) {
            throw new SelfTransferForbiddenException();
        }

        var operationId = UUID.randomUUID().toString();

        var result = transferExecutor.execute(new TransferOperationViewModel(
                senderLogin,
                request.recipientLogin(),
                request.amount(),
                request.currency(),
                operationId
        ));

        notificationClient.notify(new NotificationRequestViewModel(
                senderLogin,
                "TRANSFER_COMPLETED",
                "Transfer completed to " + request.recipientLogin() + ": " + request.amount() + " " + request.currency(),
                operationId
        ));

        return new TransferResponseViewModel(
                result.senderLogin(),
                result.recipientLogin(),
                result.senderBalance(),
                result.currency(),
                "Transfer completed"
        );
    }

    /**
     * <summary>
     * Валидирует сумму перевода на положительное значение и допустимое количество знаков после запятой.
     * </summary>
     * @param amount Сумма перевода для проверки.
     * @throws InvalidAmountException Если сумма меньше или равна нулю.
     * @throws InvalidAmountScaleException Если количество знаков после запятой больше 2.
     **/
    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException();
        }

        if (amount.scale() > 2) {
            throw new InvalidAmountScaleException();
        }
    }

    // endregion
}