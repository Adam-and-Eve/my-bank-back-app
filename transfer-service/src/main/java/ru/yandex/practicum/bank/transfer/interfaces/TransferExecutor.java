package ru.yandex.practicum.bank.transfer.interfaces;

import ru.yandex.practicum.bank.transfer.viewmodels.TransferOperationViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResultViewModel;

/**
 * <summary>
 * Контракт исполнителя операций перевода денежных средств.
 * </summary>
 **/
public interface TransferExecutor {

    // region Methods

    /**
     * <summary>
     * Выполняет операцию перевода денежных средств.
     * </summary>
     * @param operation Данные о выполняемой операции перевода.
     * <return>
     * @return Результат выполнения операции перевода.
     * </return>
     **/
    public TransferResultViewModel execute(TransferOperationViewModel operation);

    // endregion
}