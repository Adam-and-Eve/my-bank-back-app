package ru.yandex.practicum.bank.account.services;

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

    // endregion

    // region Constructors

    public BalanceServiceImpl(
            IdempotencyService idempotencyService,
            BalanceOperationService operationService) {
        this.idempotencyService = idempotencyService;
        this.operationService = operationService;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Выполняет идемпотентное пополнение счёта.
     * </summary>
     * @param request ViewModel с параметрами пополнения и уникальным идентификатором операции.
     * @return ViewModel с результатом балансовой операции {@link BalanceResponseViewModel}.
     */
    @Override
    public BalanceResponseViewModel deposit(BalanceOperationRequestViewModel request) {
        Objects.requireNonNull(request, "Deposit request must not be null");

        return idempotencyService.execute(
                request.operationId(),
                "DEPOSIT",
                request,
                BalanceResponseViewModel.class,
                () -> operationService.deposit(request)
        );
    }

    /**
     * <summary>
     * Выполняет идемпотентное списание средств со счёта.
     * </summary>
     * @param request ViewModel с параметрами снятия и уникальным идентификатором операции.
     * @return ViewModel с результатом балансовой операции {@link BalanceResponseViewModel}.
     */
    @Override
    public BalanceResponseViewModel withdraw(BalanceOperationRequestViewModel request) {
        Objects.requireNonNull(request, "Withdraw request must not be null");

        return idempotencyService.execute(
                request.operationId(),
                "WITHDRAW",
                request,
                BalanceResponseViewModel.class,
                () -> operationService.withdraw(request)
        );
    }

    /**
     * <summary>
     * Выполняет идемпотентный перевод денежных средств между счетами.
     * </summary>
     * @param request ViewModel с параметрами перевода и уникальным идентификатором операции.
     * @return ViewModel с результатом перевода {@link TransferBalanceResponseViewModel}.
     */
    @Override
    public TransferBalanceResponseViewModel transfer(TransferBalanceRequestViewModel request) {
        Objects.requireNonNull(request, "Transfer request must not be null");

        return idempotencyService.execute(
                request.operationId(),
                "TRANSFER",
                request,
                TransferBalanceResponseViewModel.class,
                () -> operationService.transfer(request)
        );
    }

    // endregion
}