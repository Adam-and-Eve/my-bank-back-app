package ru.yandex.practicum.bank.account.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.account.interfaces.BalanceOperationService;
import ru.yandex.practicum.bank.account.interfaces.BalanceService;
import ru.yandex.practicum.bank.account.interfaces.IdempotencyService;
import ru.yandex.practicum.bank.account.viewmodels.BalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.BalanceResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceResponseViewModel;

import java.util.Objects;

/**
 * <summary>
 * Высокоуровневый сервис управления финансовыми операциями (BalanceServiceImpl).
 * Координирует выполнение операций пополнения, снятия и перевода, делегируя обработку идемпотентности
 * {@link IdempotencyService} и целевую бизнес-логику {@link BalanceOperationService}.
 * </summary>
 **/
@Service
public class BalanceServiceImpl implements BalanceService {

    // region Fields

    private final IdempotencyService idempotencyService;
    private final BalanceOperationService operationService;

    private static final Logger log = LoggerFactory.getLogger(BalanceServiceImpl.class);

    // endregion

    // region Constructors

    /**
     * <summary>
     * Инициализирует сервис координации балансовых операций.
     * </summary>
     * @param idempotencyService Сервис для контроля идемпотентности (исключает двойные списания/начисления).
     * @param operationService Сервис выполнения низкоуровневых бизнес-правил по транзакциям.
     **/
    public BalanceServiceImpl(
            IdempotencyService idempotencyService,
            BalanceOperationService operationService) {
        this.idempotencyService = idempotencyService;
        this.operationService = operationService;
    }

    // endregion

    // region Public Methods

    /**
     * <summary>
     * Выполняет идемпотентное пополнение счёта.
     * </summary>
     * @param request ViewModel с параметрами пополнения и уникальным идентификатором операции.
     * @return ViewModel с результатом балансовой операции {@link BalanceResponseViewModel}.
     **/
    @Override
    public BalanceResponseViewModel deposit(BalanceOperationRequestViewModel request) {
        var response = idempotencyService.execute(
                request.operationId(),
                "DEPOSIT",
                request,
                BalanceResponseViewModel.class,
                () -> operationService.deposit(request)
        );

        log.info(
                "Balance operation completed operationId={} operationType=DEPOSIT currency={} status=success source=account-service",
                request.operationId(),
                request.currency()
        );

        return response;
    }

    /**
     * <summary>
     * Выполняет идемпотентное списание средств со счёта.
     * </summary>
     * @param request ViewModel с параметрами снятия и уникальным идентификатором операции.
     * @return ViewModel с результатом балансовой операции {@link BalanceResponseViewModel}.
     **/
    @Override
    public BalanceResponseViewModel withdraw(BalanceOperationRequestViewModel request) {
        var response = idempotencyService.execute(
                request.operationId(),
                "WITHDRAW",
                request,
                BalanceResponseViewModel.class,
                () -> operationService.withdraw(request)
        );

        log.info(
                "Balance operation completed operationId={} operationType=WITHDRAW currency={} status=success source=account-service",
                request.operationId(),
                request.currency()
        );

        return response;
    }

    /**
     * <summary>
     * Выполняет идемпотентный перевод денежных средств между счетами.
     * </summary>
     * @param request ViewModel с параметрами перевода и уникальным идентификатором операции.
     * @return ViewModel с результатом перевода {@link TransferBalanceResponseViewModel}.
     **/
    @Override
    public TransferBalanceResponseViewModel transfer(TransferBalanceRequestViewModel request) {
        var response = idempotencyService.execute(
                request.operationId(),
                "TRANSFER",
                request,
                TransferBalanceResponseViewModel.class,
                () -> operationService.transfer(request)
        );

        log.info(
                "Balance operation completed operationId={} operationType=TRANSFER currency={} status=success source=account-service",
                request.operationId(),
                request.currency()
        );

        return response;
    }

    // endregion
}