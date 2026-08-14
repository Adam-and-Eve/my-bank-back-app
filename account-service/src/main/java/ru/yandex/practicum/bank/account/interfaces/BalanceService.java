package ru.yandex.practicum.bank.account.interfaces;

import ru.yandex.practicum.bank.account.viewmodels.BalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.BalanceResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceResponseViewModel;

/**
 * <summary>
 * Контракт сервиса управления финансовыми операциями (BalanceServiceImpl).
 * </summary>
 **/
public interface BalanceService {

    // region Methods

    /**
     * <summary>
     * Выполняет идемпотентное пополнение счёта.
     * </summary>
     * @param request ViewModel с параметрами пополнения и уникальным идентификатором операции.
     * @return ViewModel с результатом балансовой операции {@link BalanceResponseViewModel}.
     */
    public BalanceResponseViewModel deposit(BalanceOperationRequestViewModel request);

    /**
     * <summary>
     * Выполняет идемпотентное списание средств со счёта.
     * </summary>
     * @param request ViewModel с параметрами снятия и уникальным идентификатором операции.
     * @return ViewModel с результатом балансовой операции {@link BalanceResponseViewModel}.
     */
    public BalanceResponseViewModel withdraw(BalanceOperationRequestViewModel request);

    /**
     * <summary>
     * Выполняет идемпотентный перевод денежных средств между счетами.
     * </summary>
     * @param request ViewModel с параметрами перевода и уникальным идентификатором операции.
     * @return ViewModel с результатом перевода {@link TransferBalanceResponseViewModel}.
     */
    public TransferBalanceResponseViewModel transfer(TransferBalanceRequestViewModel request);

    // endregion
}