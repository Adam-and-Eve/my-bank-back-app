package ru.yandex.practicum.bank.cash.services;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.cash.exceptions.OperationBlockedException;
import ru.yandex.practicum.bank.cash.interfaces.AccountClient;
import ru.yandex.practicum.bank.shared.interfaces.BlockerClient;
import ru.yandex.practicum.bank.cash.interfaces.CashService;
import ru.yandex.practicum.bank.shared.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.shared.interfaces.NotificationClient;
import ru.yandex.practicum.bank.cash.mappers.AccountBalanceMapper;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.NotificationRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * <summary>
 * Реализация сервиса операций с наличностью (Cash Service).
 * Обеспечивает бизнес-логику пополнения и снятия денежных средств,
 * валидацию сумм, проверку операций на подозрительность,
 * взаимодействие с сервисом счетов и отправку уведомлений.
 * </summary>
 **/
@Service
public class CashServiceImpl implements CashService {

    // region Fields

    private final AccountClient accountClient;
    private final BlockerClient blockerClient;
    private final ExchangeClient exchangeClient;
    private final NotificationClient notificationClient;
    private final AccountBalanceMapper accountBalanceMapper;

    // endregion

    // region Constructors

    public CashServiceImpl(AccountClient accountClient,
                       BlockerClient blockerClient,
                       ExchangeClient exchangeClient,
                       NotificationClient notificationClient,
                       AccountBalanceMapper accountBalanceMapper) {
        this.accountClient = accountClient;
        this.blockerClient = blockerClient;
        this.exchangeClient = exchangeClient;
        this.notificationClient = notificationClient;
        this.accountBalanceMapper = accountBalanceMapper;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Выполняет операцию пополнения счета пользователя наличностью.
     * Валидирует сумму, запрашивает пополнение у сервиса счетов и инициирует отправку уведомления.
     * </summary>
     * @param login Логин пользователя, выполняющего пополнение.
     * @param request Модель запроса на операцию с наличностью.
     * <return>
     * @return Модель ответа CashOperationResponseViewModel с обновленным балансом и статусом.
     * </return>
     **/
    @Override
    public CashOperationResponseViewModel deposit(String login, CashOperationRequestViewModel request) {
        validateAmount(request.amount());

        var operationId = UUID.randomUUID().toString();

        checkOperation(login, request, operationId, OperationTypeEnumModel.DEPOSIT);

        var balance = accountClient.deposit(accountBalanceMapper.toAccountsRequest(login, request, operationId));

        notificationClient.notify(new NotificationRequestViewModel(
                login,
                "CASH_DEPOSIT",
                "Счёт пополнен на " + request.amount() + " " + request.currency(),
                operationId
        ));

        return new CashOperationResponseViewModel(balance.balance(), balance.currency(), "Счёт пополнен");
    }

    /**
     * <summary>
     * Выполняет операцию снятия наличности со счета пользователя.
     * Валидирует сумму, запрашивает списание у сервиса счетов и инициирует отправку уведомления.
     * </summary>
     * @param login Логин пользователя, выполняющего снятие.
     * @param request Модель запроса на операцию с наличностью.
     * <return>
     * @return Модель ответа CashOperationResponseViewModel с обновленным балансом и статусом.
     * </return>
     **/
    @Override
    public CashOperationResponseViewModel withdraw(String login, CashOperationRequestViewModel request) {
        validateAmount(request.amount());

        var operationId = UUID.randomUUID().toString();

        checkOperation(login, request, operationId, OperationTypeEnumModel.WITHDRAW);

        var balance = accountClient.withdraw(accountBalanceMapper.toAccountsRequest(login, request, operationId));

        notificationClient.notify(new NotificationRequestViewModel(
                login,
                "CASH_WITHDRAW",
                "Со счёта снято " + request.amount() + " " + request.currency(),
                operationId
        ));

        return new CashOperationResponseViewModel(
                balance.balance(),
                balance.currency(), "Деньги сняты со счёта");
    }

    /**
     * <summary>
     * Проверяет корректность суммы финансовой операции.
     * Гарантирует, что сумма строго больше нуля и имеет не более двух знаков после запятой (копейки).
     * </summary>
     * @param amount Проверяемая сумма операции BigDecimal.
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
     * Проверяет финансовую операцию через сервис блокировки подозрительных операций.
     * Если операция запрещена, выполнение прерывается с соответствующим исключением.
     * </summary>
     * @param login Логин пользователя, выполняющего операцию.
     * @param request Модель запроса на операцию с наличностью.
     * @param operationId Уникальный идентификатор операции.
     * @param operationType Тип финансовой операции.
     * @throws OperationBlockedException Если операция признана подозрительной.
     **/
    private void checkOperation(
            String login,
            CashOperationRequestViewModel request,
            String operationId,
            OperationTypeEnumModel operationType
    ) {
        var normalizedAmount = normalizeForBlocker(request);

        var response = blockerClient.check(new OperationCheckRequestViewModel(
                operationId,
                operationType,
                login,
                null,
                null,
                request.amount(),
                request.currency(),
                normalizedAmount,
                CurrencyEnumModel.RUB
        ));

        if (!response.allowed()) {
            throw new OperationBlockedException(response.reason());
        }
    }

    private BigDecimal normalizeForBlocker(CashOperationRequestViewModel request) {
        if (request.currency() == CurrencyEnumModel.RUB) {
            return request.amount();
        }

        var conversion = exchangeClient.convert(
                request.currency(),
                CurrencyEnumModel.RUB,
                request.amount());

        return conversion.targetAmount();
    }

    // endregion
}