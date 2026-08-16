package ru.yandex.practicum.bank.account.controllers;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.bank.account.interfaces.BalanceService;
import ru.yandex.practicum.bank.account.viewmodels.BalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.BalanceResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceResponseViewModel;

/**
 * <summary>
 * Внутренний REST-контроллер для проведения балансовых операций (InternalBalanceController).
 * </summary>
 **/
@RestController
@RequestMapping("/api/account/internal/balance")
public class InternalBalanceController {

    // region Fields

    private final BalanceService balanceService;

    // endregion

    // region Constructors

    public InternalBalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    // endregion

    // region Actions

    /**
     * <summary>
     * Выполняет внутреннюю операцию пополнения баланса счёта.
     * </summary>
     * @param request ViewModel с параметрами пополнения и ключом идемпотентности.
     * @return ViewModel ответа с обновленным балансом {@link BalanceResponseViewModel}.
     */
    @PostMapping("/deposit")
    public BalanceResponseViewModel deposit(@Valid @RequestBody BalanceOperationRequestViewModel request) {
        return balanceService.deposit(request);
    }

    /**
     * <summary>
     * Выполняет внутреннюю операцию списания средств со счёта.
     * </summary>
     * @param request ViewModel с параметрами списания и ключом идемпотентности.
     * @return ViewModel ответа с обновленным балансом {@link BalanceResponseViewModel}.
     */
    @PostMapping("/withdraw")
    public BalanceResponseViewModel withdraw(@Valid @RequestBody BalanceOperationRequestViewModel request) {
        return balanceService.withdraw(request);
    }

    /**
     * <summary>
     * Выполняет внутреннюю операцию перевода средств между счетами.
     * </summary>
     * @param request ViewModel с параметрами перевода и ключом идемпотентности.
     * @return ViewModel ответа с результатом перевода {@link TransferBalanceResponseViewModel}.
     */
    @PostMapping("/transfer")
    public TransferBalanceResponseViewModel transfer(@Valid @RequestBody TransferBalanceRequestViewModel request) {
        return balanceService.transfer(request);
    }

    // endregion
}