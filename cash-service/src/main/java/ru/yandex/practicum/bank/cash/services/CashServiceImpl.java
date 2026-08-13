package ru.yandex.practicum.bank.cash.services;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.cash.interfaces.AccountClient;
import ru.yandex.practicum.bank.cash.interfaces.CashService;
import ru.yandex.practicum.bank.cash.interfaces.NotificationClient;
import ru.yandex.practicum.bank.cash.mappers.AccountBalanceMapper;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.NotificationRequestViewModel;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * <summary>
 * Реализация сервиса операций с наличностью (Cash Service).
 * Обеспечивает бизнес-логику пополнения и снятия денежных средств,
 * валидацию сумм, взаимодействие с сервисом счетов и отправку уведомлений.
 * </summary>
 **/
@Service
public class CashServiceImpl implements CashService {

    // region Fields

    private final AccountClient accountClient;
    private final NotificationClient notificationClient;
    private final AccountBalanceMapper accountBalanceMapper;

    // endregion

    // region Constructors

    public CashServiceImpl(AccountClient accountClient,
                       NotificationClient notificationClient,
                       AccountBalanceMapper accountBalanceMapper) {
        this.accountClient = accountClient;
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

        var balance = accountClient.withdraw(accountBalanceMapper.toAccountsRequest(login, request, operationId));

        notificationClient.notify(new NotificationRequestViewModel(
                login,
                "CASH_WITHDRAW",
                "Со счёта снято " + request.amount() + " " + request.currency(),
                operationId
        ));

        return new CashOperationResponseViewModel(balance.balance(), balance.currency(), "Деньги сняты со счёта");
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

    // endregion
}