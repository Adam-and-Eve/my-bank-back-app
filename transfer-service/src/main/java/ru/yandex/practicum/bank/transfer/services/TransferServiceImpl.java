package ru.yandex.practicum.bank.transfer.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.shared.interfaces.BlockerClient;
import ru.yandex.practicum.bank.shared.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.shared.interfaces.NotificationEventPublisher;
import ru.yandex.practicum.bank.shared.models.*;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.transfer.exceptions.OperationBlockedException;
import ru.yandex.practicum.bank.transfer.exceptions.SelfTransferForbiddenException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferExecutor;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferOperationViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResponseViewModel;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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
    private final NotificationEventPublisher notificationEventPublisher;
    private final Clock clock;
    private static final Logger log = LoggerFactory.getLogger(TransferServiceImpl.class);

    // endregion

    // region Constructors

    public TransferServiceImpl(
            TransferExecutor transferExecutor,
            BlockerClient blockerClient,
            ExchangeClient exchangeClient,
            NotificationEventPublisher notificationEventPublisher,
            Clock clock) {
        this.transferExecutor = transferExecutor;
        this.blockerClient = blockerClient;
        this.exchangeClient = exchangeClient;
        this.notificationEventPublisher = notificationEventPublisher;
        this.clock = clock;
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
            TransferRequestViewModel request,
            UUID operationId) {
        validateAmount(request.amount(), operationId, request.currency(), senderLogin, request.recipientLogin());

        if (senderLogin.equals(request.recipientLogin())) {
            log.warn(
                    "Transfer operation rejected operationId={} operationType=TRANSFER currency={} status=rejected errorCode=SELF_TRANSFER_FORBIDDEN source=transfer-service",
                    operationId,
                    request.currency()
            );

            throw new SelfTransferForbiddenException();
        }

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
                operationId.toString()
        ));

        publishNotifications(senderLogin, request, conversion, operationId);

        log.info(
                "Transfer operation completed operationId={} operationType=TRANSFER currency={} status=success source=transfer-service targetService=account-service",
                operationId,
                request.currency()
        );

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
    private void validateAmount(
            BigDecimal amount,
            UUID operationId,
            CurrencyEnumModel currency,
            String senderLogin,
            String recipientLogin) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn(
                    "Transfer operation rejected operationId={} operationType=TRANSFER currency={} status=rejected errorCode=INVALID_AMOUNT source=transfer-service",
                    operationId,
                    currency
            );

            throw new InvalidAmountException();
        }

        if (amount.scale() > 2) {
            log.warn(
                    "Transfer operation rejected operationId={} operationType=TRANSFER currency={} status=rejected errorCode=INVALID_AMOUNT_SCALE source=transfer-service",
                    operationId,
                    currency
            );

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
            UUID operationId,
            BigDecimal normalizedAmount
    ) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Transfer blocker check prepared operationId={} operationType=TRANSFER currency={} source=transfer-service targetService=blocker-service",
                    operationId,
                    request.currency()
            );
        }

        var response = blockerClient.check(new OperationCheckRequestViewModel(
                operationId.toString(),
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
            log.warn(
                    "Transfer operation rejected operationId={} operationType=TRANSFER currency={} status=blocked errorCode=OPERATION_BLOCKED source=transfer-service targetService=blocker-service",
                    operationId,
                    request.currency()
            );

            throw new OperationBlockedException(response.reason());
        }
    }

    private BigDecimal normalizeForBlocker(TransferRequestViewModel request) {
        if (request.currency() == CurrencyEnumModel.RUB) {
            return request.amount();
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Transfer amount normalization prepared operationType=EXCHANGE currency={} targetCurrency=RUB source=transfer-service targetService=exchange-service",
                    request.currency()
            );
        }

        var conversion = exchangeClient.convert(request.currency(), CurrencyEnumModel.RUB, request.amount());

        return conversion.targetAmount();
    }

    private ConversionResponseViewModel convert(TransferRequestViewModel request) {
        if (request.currency() == request.resolvedTargetCurrency()) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Transfer currency conversion skipped operationType=TRANSFER currency={} targetCurrency={} source=transfer-service",
                        request.currency(),
                        request.resolvedTargetCurrency()
                );
            }

            return new ConversionResponseViewModel(
                    request.currency(),
                    request.currency(),
                    request.amount(),
                    request.amount(),
                    BigDecimal.ONE,
                    null
            );
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Transfer currency conversion prepared operationType=EXCHANGE currency={} targetCurrency={} source=transfer-service targetService=exchange-service",
                    request.currency(),
                    request.resolvedTargetCurrency()
            );
        }

        return exchangeClient.convert(request.currency(), request.resolvedTargetCurrency(), request.amount());
    }

    private void publishNotifications(
            String senderLogin,
            TransferRequestViewModel request,
            ConversionResponseViewModel conversion,
            UUID operationId
    ) {
        var occurredAt = Instant.now(clock);

        notificationEventPublisher.publish(new NotificationEventModel(
                UUID.randomUUID(),
                operationId,
                NotificationSourceEnumModel.TRANSFER,
                NotificationTypeEnumModel.TRANSFER_OUTGOING,
                senderLogin,
                "Перевод пользователю " + request.recipientLogin() + ": "
                        + conversion.sourceAmount() + " " + conversion.sourceCurrency(),
                occurredAt,
                conversion.sourceAmount(),
                conversion.sourceCurrency()
        ));

        notificationEventPublisher.publish(new NotificationEventModel(
                UUID.randomUUID(),
                operationId,
                NotificationSourceEnumModel.TRANSFER,
                NotificationTypeEnumModel.TRANSFER_INCOMING,
                request.recipientLogin(),
                "Получен перевод от " + senderLogin + ": "
                        + conversion.targetAmount() + " " + conversion.targetCurrency(),
                occurredAt,
                conversion.targetAmount(),
                conversion.targetCurrency()
        ));
    }

    // endregion
}