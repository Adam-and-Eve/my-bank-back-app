package ru.yandex.practicum.bank.cash.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.cash.exceptions.OperationBlockedException;
import ru.yandex.practicum.bank.cash.interfaces.AccountClient;
import ru.yandex.practicum.bank.cash.interfaces.BlockerClient;
import ru.yandex.practicum.bank.cash.mappers.AccountBalanceMapper;
import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceResponseViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;
import ru.yandex.practicum.bank.shared.interfaces.NotificationClient;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.NotificationRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckResponseViewModel;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Модульные тесты для реализации сервиса CashServiceImpl.
 * Проверяют бизнес-логику пополнения и снятия средств, валидацию входящих сумм,
 * проверку операций через Blocker Service, корректное преобразование данных маппером
 * и передачу operationId между сервисами.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class CashServiceImplTest {

    // region Constants

    private static final String TEST_LOGIN = "alexey";
    private static final String CURRENCY = "RUB";

    // endregion

    // region Fields

    @Mock
    private AccountClient accountClient;

    @Mock
    private BlockerClient blockerClient;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    @InjectMocks
    private CashServiceImpl cashService;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную операцию пополнения счета после разрешения операции
     * сервисом блокировки.
     * </summary>
     **/
    @Test
    public void shouldDepositSuccessfully() {
        var amount = new BigDecimal("500.00");
        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.RUB);

        when(blockerClient.check(any()))
                .thenReturn(new OperationCheckResponseViewModel(true, null));

        var mockBalanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(mockBalanceResponse.balance()).thenReturn(new BigDecimal("1500.00"));
        when(mockBalanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.deposit(any())).thenReturn(mockBalanceResponse);

        CashOperationResponseViewModel response = cashService.deposit(TEST_LOGIN, request);

        assertThat(response).isNotNull();
        assertThat(response.balance()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(response.currency()).isEqualTo(CURRENCY);
        assertThat(response.message()).isEqualTo("Счёт пополнен");

        var blockerCaptor =
                ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(blockerCaptor.capture());

        var blockerRequest = blockerCaptor.getValue();

        assertThat(blockerRequest.operationId()).isNotBlank();
        assertThat(blockerRequest.operationType())
                .isEqualTo(OperationTypeEnumModel.DEPOSIT);
        assertThat(blockerRequest.login()).isEqualTo(TEST_LOGIN);
        assertThat(blockerRequest.amount()).isEqualTo(amount);
        assertThat(blockerRequest.currency())
                .isEqualTo(CurrencyEnumModel.RUB);

        var operationId = blockerRequest.operationId();

        var operationIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(accountBalanceMapper)
                .toAccountsRequest(
                        eq(TEST_LOGIN),
                        eq(request),
                        operationIdCaptor.capture()
                );

        assertThat(operationIdCaptor.getValue()).isEqualTo(operationId);

        var notificationCaptor =
                ArgumentCaptor.forClass(NotificationRequestViewModel.class);

        verify(notificationClient).notify(notificationCaptor.capture());

        var notification = notificationCaptor.getValue();

        assertThat(notification.type()).isEqualTo("CASH_DEPOSIT");
        assertThat(notification.message())
                .isEqualTo("Счёт пополнен на 500.00 RUB");
        assertThat(notification.operationId()).isEqualTo(operationId);
    }

    /**
     * <summary>
     * Проверяет успешную операцию снятия средств после разрешения операции
     * сервисом блокировки.
     * </summary>
     **/
    @Test
    public void shouldWithdrawSuccessfully() {
        var amount = new BigDecimal("200.00");
        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.RUB);

        when(blockerClient.check(any()))
                .thenReturn(new OperationCheckResponseViewModel(true, null));

        var mockBalanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(mockBalanceResponse.balance()).thenReturn(new BigDecimal("800.00"));
        when(mockBalanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.withdraw(any())).thenReturn(mockBalanceResponse);

        CashOperationResponseViewModel response = cashService.withdraw(TEST_LOGIN, request);

        assertThat(response).isNotNull();
        assertThat(response.balance()).isEqualTo(new BigDecimal("800.00"));
        assertThat(response.currency()).isEqualTo(CURRENCY);
        assertThat(response.message()).isEqualTo("Деньги сняты со счёта");

        var blockerCaptor =
                ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(blockerCaptor.capture());

        var blockerRequest = blockerCaptor.getValue();

        assertThat(blockerRequest.operationId()).isNotBlank();
        assertThat(blockerRequest.operationType())
                .isEqualTo(OperationTypeEnumModel.WITHDRAW);
        assertThat(blockerRequest.login()).isEqualTo(TEST_LOGIN);
        assertThat(blockerRequest.amount()).isEqualTo(amount);
        assertThat(blockerRequest.currency())
                .isEqualTo(CurrencyEnumModel.RUB);

        var operationId = blockerRequest.operationId();

        var operationIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(accountBalanceMapper)
                .toAccountsRequest(
                        eq(TEST_LOGIN),
                        eq(request),
                        operationIdCaptor.capture()
                );

        assertThat(operationIdCaptor.getValue()).isEqualTo(operationId);

        var notificationCaptor =
                ArgumentCaptor.forClass(NotificationRequestViewModel.class);

        verify(notificationClient).notify(notificationCaptor.capture());

        var notification = notificationCaptor.getValue();

        assertThat(notification.type()).isEqualTo("CASH_WITHDRAW");
        assertThat(notification.message())
                .isEqualTo("Со счёта снято 200.00 RUB");
        assertThat(notification.operationId()).isEqualTo(operationId);
    }

    /**
     * <summary>
     * Проверяет выброс OperationBlockedException, если сервис блокировки
     * запрещает операцию пополнения счета.
     * </summary>
     **/
    @Test
    public void shouldThrowOperationBlockedExceptionWhenDepositIsBlocked() {
        var request = new CashOperationRequestViewModel(
                new BigDecimal("500.00"),
                CurrencyEnumModel.RUB
        );

        when(blockerClient.check(any()))
                .thenReturn(new OperationCheckResponseViewModel(
                        false,
                        "Подозрительная операция"
                ));

        assertThatThrownBy(() -> cashService.deposit(TEST_LOGIN, request))
                .isInstanceOf(OperationBlockedException.class)
                .hasMessage("Подозрительная операция");

        verify(blockerClient).check(any());

        verifyNoInteractions(
                accountClient,
                accountBalanceMapper,
                notificationClient
        );
    }

    /**
     * <summary>
     * Проверяет выброс OperationBlockedException, если сервис блокировки
     * запрещает операцию снятия средств.
     * </summary>
     **/
    @Test
    public void shouldThrowOperationBlockedExceptionWhenWithdrawIsBlocked() {
        var request = new CashOperationRequestViewModel(
                new BigDecimal("200.00"),
                CurrencyEnumModel.RUB
        );

        when(blockerClient.check(any()))
                .thenReturn(new OperationCheckResponseViewModel(
                        false,
                        "Подозрительная операция"
                ));

        assertThatThrownBy(() -> cashService.withdraw(TEST_LOGIN, request))
                .isInstanceOf(OperationBlockedException.class)
                .hasMessage("Подозрительная операция");

        verify(blockerClient).check(any());

        verifyNoInteractions(
                accountClient,
                accountBalanceMapper,
                notificationClient
        );
    }

    /**
     * <summary>
     * Проверяет корректное формирование запроса на проверку пополнения
     * для Blocker Service.
     * </summary>
     **/
    @Test
    public void shouldSendCorrectDepositRequestToBlocker() {
        var amount = new BigDecimal("500.00");
        var request = new CashOperationRequestViewModel(
                amount,
                CurrencyEnumModel.RUB
        );

        when(blockerClient.check(any()))
                .thenReturn(new OperationCheckResponseViewModel(true, null));

        var balanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(balanceResponse.balance()).thenReturn(new BigDecimal("1500.00"));

        when(balanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.deposit(any())).thenReturn(balanceResponse);

        cashService.deposit(TEST_LOGIN, request);

        var captor =
                ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(captor.capture());

        var blockerRequest = captor.getValue();

        assertThat(blockerRequest.operationId()).isNotBlank();

        assertThat(blockerRequest.operationType())
                .isEqualTo(OperationTypeEnumModel.DEPOSIT);

        assertThat(blockerRequest.login()).isEqualTo(TEST_LOGIN);

        assertThat(blockerRequest.sender()).isNull();

        assertThat(blockerRequest.recipient()).isNull();

        assertThat(blockerRequest.amount()).isEqualTo(amount);

        assertThat(blockerRequest.currency())
                .isEqualTo(CurrencyEnumModel.RUB);
    }

    /**
     * <summary>
     * Проверяет корректное формирование запроса на проверку снятия
     * для Blocker Service.
     * </summary>
     **/
    @Test
    public void shouldSendCorrectWithdrawRequestToBlocker() {
        var amount = new BigDecimal("200.00");

        var request = new CashOperationRequestViewModel(
                amount,
                CurrencyEnumModel.RUB
        );

        when(blockerClient.check(any()))
                .thenReturn(new OperationCheckResponseViewModel(true, null));

        var balanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(balanceResponse.balance()).thenReturn(new BigDecimal("800.00"));

        when(balanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.withdraw(any())).thenReturn(balanceResponse);

        cashService.withdraw(TEST_LOGIN, request);

        var captor =
                ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(captor.capture());

        var blockerRequest = captor.getValue();

        assertThat(blockerRequest.operationId()).isNotBlank();

        assertThat(blockerRequest.operationType())
                .isEqualTo(OperationTypeEnumModel.WITHDRAW);

        assertThat(blockerRequest.login()).isEqualTo(TEST_LOGIN);

        assertThat(blockerRequest.sender()).isNull();

        assertThat(blockerRequest.recipient()).isNull();

        assertThat(blockerRequest.amount()).isEqualTo(amount);

        assertThat(blockerRequest.currency())
                .isEqualTo(CurrencyEnumModel.RUB);
    }

    /**
     * <summary>
     * Проверяет, что нулевая и отрицательная сумма отклоняются
     * до обращения к Blocker Service.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountExceptionWhenAmountIsZeroOrNegative() {
        var zeroRequest = new CashOperationRequestViewModel(
                BigDecimal.ZERO,
                CurrencyEnumModel.RUB
        );

        var negativeRequest = new CashOperationRequestViewModel(
                new BigDecimal("-100.00"),
                CurrencyEnumModel.RUB
        );

        assertThatThrownBy(() ->
                cashService.deposit(TEST_LOGIN, zeroRequest))
                .isInstanceOf(InvalidAmountException.class);

        assertThatThrownBy(() ->
                cashService.withdraw(TEST_LOGIN, negativeRequest))
                .isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(
                accountClient,
                blockerClient,
                notificationClient,
                accountBalanceMapper
        );
    }

    /**
     * <summary>
     * Проверяет, что сумма с более чем двумя знаками после запятой
     * отклоняется до обращения к Blocker Service.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountScaleExceptionWhenScaleExceedsTwo() {
        var invalidScaleRequest = new CashOperationRequestViewModel(
                new BigDecimal("100.555"),
                CurrencyEnumModel.RUB
        );

        assertThatThrownBy(() ->
                cashService.deposit(TEST_LOGIN, invalidScaleRequest))
                .isInstanceOf(InvalidAmountScaleException.class);

        assertThatThrownBy(() ->
                cashService.withdraw(TEST_LOGIN, invalidScaleRequest))
                .isInstanceOf(InvalidAmountScaleException.class);

        verifyNoInteractions(
                accountClient,
                blockerClient,
                notificationClient,
                accountBalanceMapper
        );
    }

    // endregion

    // endregion
}