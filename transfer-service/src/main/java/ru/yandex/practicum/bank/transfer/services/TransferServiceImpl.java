package ru.yandex.practicum.bank.transfer.services;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.shared.interfaces.BlockerClient;
import ru.yandex.practicum.bank.shared.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.shared.interfaces.NotificationClient;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.transfer.exceptions.OperationBlockedException;
import ru.yandex.practicum.bank.transfer.exceptions.SelfTransferForbiddenException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferExecutor;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;
import ru.yandex.practicum.bank.shared.viewmodels.NotificationRequestViewModel;
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
    private final BlockerClient blockerClient;
    private final ExchangeClient exchangeClient;
    private final NotificationClient notificationClient;

    // endregion

    // region Constructors

    public TransferServiceImpl(
            TransferExecutor transferExecutor,
            BlockerClient blockerClient,
            ExchangeClient exchangeClient,
            NotificationClient notificationClient) {
        this.transferExecutor = transferExecutor;
        this.blockerClient = blockerClient;
        this.exchangeClient = exchangeClient;
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

        var normalizedAmount = normalizeForBlocker(request);

        checkOperation(senderLogin, request, operationId, normalizedAmount);

        var conversion = convert(request);

        var result = transferExecutor.execute(new TransferOperationViewModel(
                senderLogin,
                request.recipientLogin(),
                request.amount(),
                request.currency(),
                conversion.targetAmount(),
                conversion.targetCurrency(),
                operationId
        ));

        notificationClient.notify(new NotificationRequestViewModel(
                senderLogin,
                "TRANSFER_COMPLETED",
                notificationMessage(request, conversion),
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

    /**
     * <summary>
     * Проверяет перевод через сервис блокировки подозрительных операций.
     * Формирует запрос с данными отправителя, получателя, исходной и нормализованной
     * суммой и прерывает выполнение перевода, если операция запрещена.
     * </summary>
     * @param senderLogin Логин пользователя-отправителя.
     * @param request Запрос на проведение перевода.
     * @param operationId Уникальный идентификатор операции.
     * @param normalizedAmount Сумма перевода, нормализованная в базовую валюту.
     * @throws OperationBlockedException Если операция признана подозрительной.
     **/
    private void checkOperation(
            String senderLogin,
            TransferRequestViewModel request,
            String operationId,
            BigDecimal normalizedAmount) {

        var response = blockerClient.check(new OperationCheckRequestViewModel(
                operationId,
                OperationTypeEnumModel.TRANSFER,
                null,
                senderLogin,
                request.recipientLogin(),
                request.amount(),
                request.currency(),
                normalizedAmount,
                CurrencyEnumModel.RUB
        ));
        if (!response.allowed()) {
            throw new OperationBlockedException(response.reason());
        }
    }

    /**
     * <summary>
     * Выполняет конвертацию суммы перевода из исходной валюты
     * в целевую валюту получателя.
     * Если исходная и целевая валюты совпадают, возвращает результат
     * без фактического обращения к Exchange Service.
     * </summary>
     * @param request Запрос на проведение перевода.
     * @return Результат конвертации суммы перевода.
     **/
    private ConversionResponseViewModel convert(TransferRequestViewModel request) {
        if (request.currency() == request.resolvedTargetCurrency()) {
            return new ConversionResponseViewModel(
                    request.currency(),
                    request.currency(),
                    request.amount(),
                    request.amount(),
                    BigDecimal.ONE,
                    null
            );
        }

        return exchangeClient.convert(request.currency(), request.resolvedTargetCurrency(), request.amount());
    }

    /**
     * <summary>
     * Нормализует сумму перевода в базовую валюту RUB для последующей
     * проверки операции сервисом блокировки.
     * Для переводов в RUB конвертация не выполняется.
     * </summary>
     * @param request Запрос на проведение перевода.
     * @return Сумма перевода, выраженная в RUB.
     **/
    private BigDecimal normalizeForBlocker(TransferRequestViewModel request) {
        if (request.currency() == CurrencyEnumModel.RUB) {
            return request.amount();
        }

        var conversion = exchangeClient.convert(request.currency(), CurrencyEnumModel.RUB, request.amount());

        return conversion.targetAmount();
    }

    /**
     * <summary>
     * Формирует текст уведомления о завершённом переводе.
     * Для перевода между одинаковыми валютами возвращает сообщение
     * с исходной суммой, а для конвертации дополнительно указывает
     * исходную и целевую суммы и валюты.
     * </summary>
     * @param request Запрос на проведение перевода.
     * @param conversion Результат конвертации суммы перевода.
     * @return Текст уведомления о выполненном переводе.
     **/
    private String notificationMessage(TransferRequestViewModel request, ConversionResponseViewModel conversion) {
        if (conversion.sourceCurrency() == conversion.targetCurrency()) {
            return "Transfer completed to " + request.recipientLogin() + ": "
                    + request.amount() + " " + request.currency();
        }
        return "Transfer completed to " + request.recipientLogin() + ": "
                + conversion.sourceAmount() + " " + conversion.sourceCurrency()
                + " -> " + conversion.targetAmount() + " " + conversion.targetCurrency();
    }

    // endregion
}