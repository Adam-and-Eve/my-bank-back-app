package ru.yandex.practicum.bank.account.interfaces;

import ru.yandex.practicum.bank.account.exceptions.InsufficientFundsException;
import ru.yandex.practicum.bank.account.exceptions.RecipientNotFoundException;
import ru.yandex.practicum.bank.account.exceptions.SelfTransferForbiddenException;
import ru.yandex.practicum.bank.account.viewmodels.BalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.BalanceResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceResponseViewModel;

/**
 * <summary>
 * Контракт сервиса проведения балансовых операций и денежных переводов (BalanceOperationServiceImpl).
 * </summary>
 **/
public interface BalanceOperationService {

    // region Methods

    /**
     * <summary>
     * Выполняет пополнение баланса указанного счёта.
     * В случае конфликта оптимистичной блокировки автоматически повторяет попытку до 3 раз.
     * </summary>
     * @param request ViewModel с параметрами пополнения счета.
     * @return ViewModel с обновленным балансом {@link BalanceResponseViewModel}.
     */
    public BalanceResponseViewModel deposit(BalanceOperationRequestViewModel request);

    /**
     * <summary>
     * Выполняет снятие денежных средств со счёта.
     * В случае конфликта оптимистичной блокировки автоматически повторяет попытку до 3 раз.
     * </summary>
     * @param request ViewModel с параметрами снятия средств.
     * @return ViewModel с обновленным балансом {@link BalanceResponseViewModel}.
     * @throws InsufficientFundsException Если на счёте недостаточно средств.
     */
    public BalanceResponseViewModel withdraw(BalanceOperationRequestViewModel request);

    /**
     * <summary>
     * Выполняет перевод денежных средств между счетами отправителя и получателя.
     * В случае конфликта оптимистичной блокировки повторит попытку до 3 раз.
     * </summary>
     * @param request ViewModel с параметрами перевода средств.
     * @return ViewModel с результатами перевода {@link TransferBalanceResponseViewModel}.
     * @throws SelfTransferForbiddenException Если логин отправителя и получателя совпадают.
     * @throws RecipientNotFoundException Если счёт получателя не найден.
     * @throws InsufficientFundsException Если у отправителя недостаточно средств.
     */
    public TransferBalanceResponseViewModel transfer(TransferBalanceRequestViewModel request);

    // endregion
}