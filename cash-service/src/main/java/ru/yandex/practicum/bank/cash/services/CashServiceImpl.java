package ru.yandex.practicum.bank.cash.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.cash.exceptions.OperationBlockedException;
import ru.yandex.practicum.bank.cash.interfaces.AccountClient;
import ru.yandex.practicum.bank.shared.interfaces.BlockerClient;
import ru.yandex.practicum.bank.cash.interfaces.CashService;
import ru.yandex.practicum.bank.shared.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.cash.mappers.AccountBalanceMapper;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;
import ru.yandex.practicum.bank.shared.models.*;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
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
    private final AccountBalanceMapper accountBalanceMapper;
    private final Clock clock;

    private static final Logger log =  LoggerFactory.getLogger(CashServiceImpl.class);

    // endregion

    // region Constructors

    /**
     * <summary>
     * Создает экземпляр сервиса с внедрением всех необходимых зависимостей.
     * </summary>
     * @param accountClient Клиент для взаимодействия с сервисом счетов.
     * @param blockerClient Клиент для проверки операций на блокировку.
     * @param exchangeClient Клиент для получения курсов обмена валют.
     * @param accountBalanceMapper Маппер для преобразования моделей запросов.
     * @param clock Источник времени.
     */
    public CashServiceImpl(AccountClient accountClient,
                           BlockerClient blockerClient,
                           ExchangeClient exchangeClient,
                           AccountBalanceMapper accountBalanceMapper,
                           Clock clock) {
        this.accountClient = accountClient;
        this.blockerClient = blockerClient;
        this.exchangeClient = exchangeClient;
        this.accountBalanceMapper = accountBalanceMapper;
        this.clock = clock;
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
     * @param operationId Идентификатор операции.
     * @return Модель ответа CashOperationResponseViewModel с обновленным балансом и статусом.
     **/
    @Override
    public CashOperationResponseViewModel deposit(
            String login,
            CashOperationRequestViewModel request,
            UUID operationId) {
        validateAmount(request.amount(), operationId, OperationTypeEnumModel.DEPOSIT, request.currency(), login);

        checkOperation(login, request, operationId, OperationTypeEnumModel.DEPOSIT);

        var notifications = buildNotifications(login, request, operationId, NotificationTypeEnumModel.CASH_DEPOSITED);

        var balance = accountClient.deposit(accountBalanceMapper.toAccountsRequest(login, request, operationId, notifications));

        log.info(
                "Cash operation completed operationId={} operationType=DEPOSIT currency={} status=success source=cash-service targetService=accounts-service",
                operationId,
                request.currency()
        );

        return new CashOperationResponseViewModel(balance.balance(), balance.currency(), "Счёт пополнен");
    }

    /**
     * <summary>
     * Выполняет операцию снятия наличности со счета пользователя.
     * Валидирует сумму, запрашивает списание у сервиса счетов и инициирует отправку уведомления.
     * </summary>
     * @param login Логин пользователя, выполняющего снятие.
     * @param request Модель запроса на операцию с наличностью.
     * @param operationId Идентификатор операции.
     * @return Модель ответа CashOperationResponseViewModel с обновленным балансом и статусом.
     **/
    @Override
    public CashOperationResponseViewModel withdraw(
            String login,
            CashOperationRequestViewModel request,
            UUID operationId) {
        validateAmount(request.amount(), operationId, OperationTypeEnumModel.WITHDRAW, request.currency(), login);

        checkOperation(login, request, operationId, OperationTypeEnumModel.WITHDRAW);

        var notifications = buildNotifications(login, request, operationId, NotificationTypeEnumModel.CASH_WITHDRAWN);

        var balance = accountClient.withdraw(accountBalanceMapper.toAccountsRequest(login, request, operationId, notifications));

        log.info(
                "Cash operation completed operationId={} operationType=WITHDRAW currency={} status=success source=cash-service targetService=accounts-service",
                operationId,
                request.currency()
        );

        return new CashOperationResponseViewModel(balance.balance(), balance.currency(), "Деньги сняты со счёта");
    }

    /**
     * <summary>
     * Проверяет корректность суммы финансовой операции.
     * Гарантирует, что сумма строго больше нуля и имеет не более двух знаков после запятой (копейки).
     * </summary>
     * @param amount Проверяемая сумма операции BigDecimal.
     * @param operationId Идентификатор операции.
     * @param operationType Тип операции (пополнение или снятие).
     * @param currency Валюта операции.
     **/
    private void validateAmount(BigDecimal amount,
                                UUID operationId,
                                OperationTypeEnumModel operationType,
                                CurrencyEnumModel currency,
                                String login) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn(
                    "Cash operation rejected operationId={} operationType={} currency={} status=rejected errorCode=INVALID_AMOUNT source=cash-service",
                    operationId,
                    operationType,
                    currency
            );

            throw new InvalidAmountException();
        }
        if (amount.scale() > 2) {
            log.warn(
                    "Cash operation rejected operationId={} operationType={} currency={} status=rejected errorCode=INVALID_AMOUNT_SCALE source=cash-service",
                    operationId,
                    operationType,
                    currency
            );

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
            UUID operationId,
            OperationTypeEnumModel operationType
    ) {
        var normalizedAmount = normalizeForBlocker(request);

        if (log.isDebugEnabled()) {
            log.debug(
                    "Cash blocker check prepared operationId={} operationType={} currency={} source=cash-service targetService=blocker-service",
                    operationId,
                    operationType,
                    request.currency()
            );
        }

        var response = blockerClient.check(new OperationCheckRequestViewModel(
                operationId.toString(),
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
            log.warn(
                    "Cash operation rejected operationId={} operationType={} currency={} status=blocked errorCode=OPERATION_BLOCKED source=cash-service targetService=blocker-service",
                    operationId,
                    operationType,
                    request.currency()
            );

            throw new OperationBlockedException(response.reason());
        }
    }

    /**
     * <summary>
     * Нормализует сумму операции, конвертируя её в базовую валюту (RUB) для унификации
     * проверок лимитов и правил в сервисе блокировок (Blocker Service).
     * </summary>
     * @param request Модель запроса на кассовую операцию.
     * @return Эквивалент суммы операции в RUB.
     */
    private BigDecimal normalizeForBlocker(CashOperationRequestViewModel request) {
        if (request.currency() == CurrencyEnumModel.RUB) {
            return request.amount();
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Cash amount normalization prepared operationType=EXCHANGE currency={} targetCurrency=RUB source=cash-service targetService=exchange-service",
                    request.currency()
            );
        }

        var conversion = exchangeClient.convert(request.currency(), CurrencyEnumModel.RUB, request.amount());

        return conversion.targetAmount();
    }

    /**
     * <summary>
     * Формирует список событий уведомления о результатах кассовой операции
     * для последующего атомарного сохранения в Outbox сервиса счетов.
     * </summary>
     */
    private List<NotificationEventModel> buildNotifications(
            String login,
            CashOperationRequestViewModel request,
            UUID operationId,
            NotificationTypeEnumModel type
    ) {
        String message = type == NotificationTypeEnumModel.CASH_DEPOSITED
                ? "Счёт пополнен на " + request.amount() + " " + request.currency()
                : "Со счёта снято " + request.amount() + " " + request.currency();

        return List.of(new NotificationEventModel(
                UUID.randomUUID(),
                operationId,
                NotificationSourceEnumModel.CASH,
                type,
                login,
                message,
                Instant.now(clock),
                request.amount(),
                request.currency()
        ));
    }

    // endregion
}