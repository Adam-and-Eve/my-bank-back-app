package ru.yandex.practicum.bank.account.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.account.interfaces.BalanceOperationService;
import ru.yandex.practicum.bank.account.interfaces.IdempotencyService;
import ru.yandex.practicum.bank.account.viewmodels.BalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.BalanceResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceResponseViewModel;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Юнит-тесты высокоуровневого сервиса балансовых операций (BalanceServiceImpl).
 * Проверяют делегирование в IdempotencyService с корректными operationType,
 * передачу бизнес-логики в BalanceOperationService и валидацию null-запросов.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class BalanceServiceImplTest {

    // region Constants

    private static final String OPERATION_ID = "op-balance-001";
    private static final String LOGIN = "dmitry";
    private static final String RECIPIENT_LOGIN = "alexey";
    private static final BigDecimal AMOUNT = new BigDecimal("150.50");
    private static final CurrencyEnumModel CURRENCY = CurrencyEnumModel.RUB;

    // endregion

    // region Fields

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private BalanceOperationService operationService;

    private BalanceServiceImpl balanceService;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        balanceService = new BalanceServiceImpl(idempotencyService, operationService);
    }

    // endregion

    // region Tests - deposit

    /**
     * <summary>
     * Проверяет успешное идемпотентное пополнение: вызывается IdempotencyService
     * с типом DEPOSIT, а бизнес-логика делегируется в BalanceOperationService.deposit.
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldDepositThroughIdempotencyService() {
        var request = createBalanceOperationRequest();

        var expectedResponse = new BalanceResponseViewModel(
                LOGIN,
                new BigDecimal("1150.50"),
                CURRENCY.name()
        );

        when(idempotencyService.execute(
                eq(OPERATION_ID),
                eq("DEPOSIT"),
                eq(request),
                eq(BalanceResponseViewModel.class),
                any(Supplier.class)
        )).thenAnswer(invocation -> {
            Supplier<BalanceResponseViewModel> supplier = invocation.getArgument(4);
            return supplier.get();
        });

        when(operationService.deposit(request)).thenReturn(expectedResponse);

        var actual = balanceService.deposit(request);

        assertThat(actual).isEqualTo(expectedResponse);

        verify(idempotencyService).execute(
                eq(OPERATION_ID),
                eq("DEPOSIT"),
                eq(request),
                eq(BalanceResponseViewModel.class),
                any(Supplier.class)
        );

        verify(operationService).deposit(request);
    }

    /**
     * <summary>
     * Проверяет выброс NullPointerException при null-запросе на пополнение
     * (исключение возникает при попытке получить request.operationId() до передачи в IdempotencyService).
     * </summary>
     **/
    @Test
    public void shouldThrowNullPointerExceptionWhenDepositRequestIsNull() {
        assertThatThrownBy(() -> balanceService.deposit(null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(idempotencyService, operationService);
    }

    // endregion

    // region Tests - withdraw

    /**
     * <summary>
     * Проверяет успешное идемпотентное списание: вызывается IdempotencyService
     * с типом WITHDRAW, бизнес-логика делегируется в BalanceOperationService.withdraw.
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldWithdrawThroughIdempotencyService() {
        var request = createBalanceOperationRequest();

        var expectedResponse = new BalanceResponseViewModel(
                LOGIN,
                new BigDecimal("849.50"),
                CURRENCY.name()
        );

        when(idempotencyService.execute(
                eq(OPERATION_ID),
                eq("WITHDRAW"),
                eq(request),
                eq(BalanceResponseViewModel.class),
                any(Supplier.class)
        )).thenAnswer(invocation -> {
            Supplier<BalanceResponseViewModel> supplier = invocation.getArgument(4);
            return supplier.get();
        });

        when(operationService.withdraw(request)).thenReturn(expectedResponse);

        var actual = balanceService.withdraw(request);

        assertThat(actual).isEqualTo(expectedResponse);

        verify(idempotencyService).execute(
                eq(OPERATION_ID),
                eq("WITHDRAW"),
                eq(request),
                eq(BalanceResponseViewModel.class),
                any(Supplier.class)
        );

        verify(operationService).withdraw(request);
    }

    /**
     * <summary>
     * Проверяет выброс NullPointerException при null-запросе на списание.
     * </summary>
     **/
    @Test
    public void shouldThrowNullPointerExceptionWhenWithdrawRequestIsNull() {
        assertThatThrownBy(() -> balanceService.withdraw(null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(idempotencyService, operationService);
    }

    // endregion

    // region Tests - transfer

    /**
     * <summary>
     * Проверяет успешный идемпотентный перевод: вызывается IdempotencyService
     * с типом TRANSFER, бизнес-логика делегируется в BalanceOperationService.transfer.
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldTransferThroughIdempotencyService() {
        var request = createTransferRequest();

        var expectedResponse = new TransferBalanceResponseViewModel(
                LOGIN,
                RECIPIENT_LOGIN,
                new BigDecimal("849.50"),
                CURRENCY.name()
        );

        when(idempotencyService.execute(
                eq(OPERATION_ID),
                eq("TRANSFER"),
                eq(request),
                eq(TransferBalanceResponseViewModel.class),
                any(Supplier.class)
        )).thenAnswer(invocation -> {
            Supplier<TransferBalanceResponseViewModel> supplier = invocation.getArgument(4);
            return supplier.get();
        });

        when(operationService.transfer(request)).thenReturn(expectedResponse);

        var actual = balanceService.transfer(request);

        assertThat(actual).isEqualTo(expectedResponse);

        verify(idempotencyService).execute(
                eq(OPERATION_ID),
                eq("TRANSFER"),
                eq(request),
                eq(TransferBalanceResponseViewModel.class),
                any(Supplier.class)
        );

        verify(operationService).transfer(request);
    }

    /**
     * <summary>
     * Проверяет выброс NullPointerException при null-запросе на перевод.
     * </summary>
     **/
    @Test
    public void shouldThrowNullPointerExceptionWhenTransferRequestIsNull() {
        assertThatThrownBy(() -> balanceService.transfer(null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(idempotencyService, operationService);
    }

    // endregion

    // region Tests - operation type verification

    /**
     * <summary>
     * Убеждается, что для deposit в IdempotencyService передаётся именно "DEPOSIT".
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldPassDepositOperationType() {
        var request = createBalanceOperationRequest();

        var response = new BalanceResponseViewModel(LOGIN, AMOUNT, CURRENCY.name());

        when(idempotencyService.execute(
                any(), any(), any(), any(), any(Supplier.class)
        )).thenReturn(response);

        balanceService.deposit(request);

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);

        verify(idempotencyService).execute(
                eq(OPERATION_ID),
                typeCaptor.capture(),
                eq(request),
                eq(BalanceResponseViewModel.class),
                any(Supplier.class)
        );

        assertThat(typeCaptor.getValue()).isEqualTo("DEPOSIT");
    }

    /**
     * <summary>
     * Убеждается, что для withdraw в IdempotencyService передаётся именно "WITHDRAW".
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldPassWithdrawOperationType() {
        var request = createBalanceOperationRequest();

        var response = new BalanceResponseViewModel(LOGIN, AMOUNT, CURRENCY.name());

        when(idempotencyService.execute(
                any(), any(), any(), any(), any(Supplier.class)
        )).thenReturn(response);

        balanceService.withdraw(request);

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);

        verify(idempotencyService).execute(
                eq(OPERATION_ID),
                typeCaptor.capture(),
                eq(request),
                eq(BalanceResponseViewModel.class),
                any(Supplier.class)
        );

        assertThat(typeCaptor.getValue()).isEqualTo("WITHDRAW");
    }

    /**
     * <summary>
     * Убеждается, что для transfer в IdempotencyService передаётся именно "TRANSFER".
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldPassTransferOperationType() {
        var request = createTransferRequest();

        var response = new TransferBalanceResponseViewModel(LOGIN, RECIPIENT_LOGIN, AMOUNT, CURRENCY.name());

        when(idempotencyService.execute(
                any(), any(), any(), any(), any(Supplier.class)
        )).thenReturn(response);

        balanceService.transfer(request);

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);

        verify(idempotencyService).execute(
                eq(OPERATION_ID),
                typeCaptor.capture(),
                eq(request),
                eq(TransferBalanceResponseViewModel.class),
                any(Supplier.class)
        );

        assertThat(typeCaptor.getValue()).isEqualTo("TRANSFER");
    }

    // endregion

    // region Helper Methods

    private BalanceOperationRequestViewModel createBalanceOperationRequest() {
        return new BalanceOperationRequestViewModel(
                LOGIN,
                AMOUNT,
                CURRENCY,
                OPERATION_ID
        );
    }

    private TransferBalanceRequestViewModel createTransferRequest() {
        return new TransferBalanceRequestViewModel(
                LOGIN,
                RECIPIENT_LOGIN,
                AMOUNT,
                CURRENCY,
                OPERATION_ID
        );
    }

    // endregion
}