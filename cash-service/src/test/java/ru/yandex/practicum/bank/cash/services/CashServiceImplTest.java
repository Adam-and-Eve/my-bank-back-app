package ru.yandex.practicum.bank.cash.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.cash.exceptions.OperationBlockedException;
import ru.yandex.practicum.bank.cash.interfaces.AccountClient;
import ru.yandex.practicum.bank.cash.mappers.AccountBalanceMapper;
import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceResponseViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;
import ru.yandex.practicum.bank.shared.interfaces.BlockerClient;
import ru.yandex.practicum.bank.shared.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationSourceEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckResponseViewModel;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Модульные тесты для реализации сервиса CashServiceImpl.
 * Проверяют бизнес-логику пополнения и снятия средств, валидацию входящих сумм,
 * проверку операций через Blocker Service, конвертацию валют через Exchange Service
 * и корректную передачу сформированных уведомлений мапперу.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class CashServiceImplTest {

    // region Constants

    private static final String TEST_LOGIN = "alexey";
    private static final String CURRENCY = "RUB";
    private static final UUID TEST_OPERATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-27T12:00:00Z");

    // endregion

    // region Fields

    @Mock
    private AccountClient accountClient;

    @Mock
    private BlockerClient blockerClient;

    @Mock
    private ExchangeClient exchangeClient;

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    private Clock clock;

    private CashServiceImpl cashService;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        clock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));

        cashService = new CashServiceImpl(
                accountClient,
                blockerClient,
                exchangeClient,
                accountBalanceMapper,
                clock
        );
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную операцию пополнения счета в RUB после разрешения операции
     * сервисом блокировки и корректную сборку уведомления.
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldDepositSuccessfully() {
        var amount = new BigDecimal("500.00");

        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.RUB);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(true, null));

        var balanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(balanceResponse.balance()).thenReturn(new BigDecimal("1500.00"));

        when(balanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.deposit(any())).thenReturn(balanceResponse);

        var response = cashService.deposit(TEST_LOGIN, request, TEST_OPERATION_ID);

        assertThat(response).isEqualTo(new CashOperationResponseViewModel(
                new BigDecimal("1500.00"),
                CURRENCY,
                "Счёт пополнен"
        ));

        verifyNoInteractions(exchangeClient);

        var blockerCaptor = ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(blockerCaptor.capture());

        var blockerRequest = blockerCaptor.getValue();

        assertThat(blockerRequest.operationId()).isEqualTo(TEST_OPERATION_ID.toString());

        assertThat(blockerRequest.operationType()).isEqualTo(OperationTypeEnumModel.DEPOSIT);

        assertThat(blockerRequest.login()).isEqualTo(TEST_LOGIN);

        assertThat(blockerRequest.amount()).isEqualByComparingTo(amount);

        assertThat(blockerRequest.currency()).isEqualTo(CurrencyEnumModel.RUB);

        ArgumentCaptor<List<NotificationEventModel>> notificationCaptor = ArgumentCaptor.forClass(List.class);

        verify(accountBalanceMapper).toAccountsRequest(eq(TEST_LOGIN), eq(request), eq(TEST_OPERATION_ID), notificationCaptor.capture());

        var notifications = notificationCaptor.getValue();

        assertThat(notifications).hasSize(1);

        var notification = notifications.getFirst();

        assertThat(notification.recipientLogin()).isEqualTo(TEST_LOGIN);

        assertThat(notification.type()).isEqualTo(NotificationTypeEnumModel.CASH_DEPOSITED);

        assertThat(notification.source()).isEqualTo(NotificationSourceEnumModel.CASH);

        assertThat(notification.message()).isEqualTo("Счёт пополнен на 500.00 RUB");

        assertThat(notification.operationId()).isEqualTo(TEST_OPERATION_ID);

        assertThat(notification.amount()).isEqualByComparingTo(amount);

        assertThat(notification.currency()).isEqualTo(CurrencyEnumModel.RUB);
    }

    /**
     * <summary>
     * Проверяет успешную операцию снятия средств в RUB после разрешения операции
     * сервисом блокировки и корректную сборку уведомления.
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldWithdrawSuccessfully() {
        var amount = new BigDecimal("200.00");

        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.RUB);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(true, null));

        var balanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(balanceResponse.balance()).thenReturn(new BigDecimal("800.00"));

        when(balanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.withdraw(any())).thenReturn(balanceResponse);

        var response = cashService.withdraw(TEST_LOGIN, request, TEST_OPERATION_ID);

        assertThat(response).isEqualTo(new CashOperationResponseViewModel(
                new BigDecimal("800.00"),
                CURRENCY,
                "Деньги сняты со счёта"
        ));

        verifyNoInteractions(exchangeClient);

        var blockerCaptor = ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(blockerCaptor.capture());

        var blockerRequest = blockerCaptor.getValue();

        assertThat(blockerRequest.operationId()).isEqualTo(TEST_OPERATION_ID.toString());

        assertThat(blockerRequest.operationType()).isEqualTo(OperationTypeEnumModel.WITHDRAW);

        ArgumentCaptor<List<NotificationEventModel>> notificationCaptor = ArgumentCaptor.forClass(List.class);

        verify(accountBalanceMapper).toAccountsRequest(eq(TEST_LOGIN), eq(request), eq(TEST_OPERATION_ID), notificationCaptor.capture());

        var notifications = notificationCaptor.getValue();

        assertThat(notifications).hasSize(1);

        var notification = notifications.getFirst();

        assertThat(notification.recipientLogin()).isEqualTo(TEST_LOGIN);

        assertThat(notification.type()).isEqualTo(NotificationTypeEnumModel.CASH_WITHDRAWN);

        assertThat(notification.source()).isEqualTo(NotificationSourceEnumModel.CASH);

        assertThat(notification.message()).isEqualTo("Со счёта снято 200.00 RUB");

        assertThat(notification.operationId()).isEqualTo(TEST_OPERATION_ID);
    }

    /**
     * <summary>
     * Проверяет конвертацию иностранной валюты в RUB перед проверкой операции
     * сервисом блокировки при пополнении счета.
     * </summary>
     **/
    @Test
    public void shouldNormalizeForeignCurrencyDepositAmountBeforeBlockerCheck() {
        var amount = new BigDecimal("100.00");

        var normalizedAmount = new BigDecimal("9500.00");

        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.USD);

        var conversion = mock(ConversionResponseViewModel.class);

        when(conversion.targetAmount()).thenReturn(normalizedAmount);

        when(exchangeClient.convert(CurrencyEnumModel.USD, CurrencyEnumModel.RUB, amount)).thenReturn(conversion);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(true, null));

        var balanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(balanceResponse.balance()).thenReturn(new BigDecimal("15000.00"));

        when(balanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.deposit(any())).thenReturn(balanceResponse);

        cashService.deposit(TEST_LOGIN, request, TEST_OPERATION_ID);

        var captor = ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(captor.capture());

        var blockerRequest = captor.getValue();

        assertThat(blockerRequest.amount()).isEqualByComparingTo(amount);

        assertThat(blockerRequest.currency()).isEqualTo(CurrencyEnumModel.USD);

        assertThat(blockerRequest.normalizedAmount()).isEqualByComparingTo(normalizedAmount);

        assertThat(blockerRequest.baseCurrency()).isEqualTo(CurrencyEnumModel.RUB);

        verify(exchangeClient).convert(CurrencyEnumModel.USD, CurrencyEnumModel.RUB, amount);
    }

    /**
     * <summary>
     * Проверяет конвертацию иностранной валюты в RUB перед проверкой операции
     * сервисом блокировки при снятии средств.
     * </summary>
     **/
    @Test
    public void shouldNormalizeForeignCurrencyWithdrawAmountBeforeBlockerCheck() {
        var amount = new BigDecimal("200.00");

        var normalizedAmount = new BigDecimal("19000.00");

        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.USD);

        var conversion = mock(ConversionResponseViewModel.class);

        when(conversion.targetAmount()).thenReturn(normalizedAmount);

        when(exchangeClient.convert(CurrencyEnumModel.USD, CurrencyEnumModel.RUB, amount)).thenReturn(conversion);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(true, null));

        var balanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(balanceResponse.balance()).thenReturn(new BigDecimal("5000.00"));

        when(balanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.withdraw(any())).thenReturn(balanceResponse);

        cashService.withdraw(TEST_LOGIN, request, TEST_OPERATION_ID);

        var captor = ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(captor.capture());

        var blockerRequest = captor.getValue();

        assertThat(blockerRequest.normalizedAmount()).isEqualByComparingTo(normalizedAmount);

        verify(exchangeClient).convert(CurrencyEnumModel.USD, CurrencyEnumModel.RUB, amount);
    }

    /**
     * <summary>
     * Проверяет, что конвертация и проверка операции выполняются
     * до изменения баланса.
     * </summary>
     **/
    @Test
    public void shouldCheckAndNormalizeOperationBeforeChangingBalance() {
        var amount = new BigDecimal("100.00");

        var normalizedAmount = new BigDecimal("9500.00");

        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.USD);

        var conversion = mock(ConversionResponseViewModel.class);

        when(conversion.targetAmount()).thenReturn(normalizedAmount);

        when(exchangeClient.convert(CurrencyEnumModel.USD, CurrencyEnumModel.RUB, amount)).thenReturn(conversion);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(true, null));

        var balanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(balanceResponse.balance()).thenReturn(new BigDecimal("10000.00"));

        when(balanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.deposit(any())).thenReturn(balanceResponse);

        cashService.deposit(TEST_LOGIN, request, TEST_OPERATION_ID);

        InOrder inOrder = inOrder(
                exchangeClient,
                blockerClient,
                accountBalanceMapper,
                accountClient
        );

        inOrder.verify(exchangeClient).convert(CurrencyEnumModel.USD, CurrencyEnumModel.RUB, amount);

        inOrder.verify(blockerClient).check(any(OperationCheckRequestViewModel.class));

        inOrder.verify(accountBalanceMapper).toAccountsRequest(eq(TEST_LOGIN), eq(request), eq(TEST_OPERATION_ID), anyList());

        inOrder.verify(accountClient).deposit(any());
    }

    /**
     * <summary>
     * Проверяет выброс OperationBlockedException, если сервис блокировки
     * запрещает операцию пополнения счета в RUB.
     * </summary>
     **/
    @Test
    public void shouldThrowOperationBlockedExceptionWhenDepositIsBlocked() {
        var request = new CashOperationRequestViewModel(new BigDecimal("500.00"), CurrencyEnumModel.RUB);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(false, "Подозрительная операция"));

        assertThatThrownBy(() -> cashService.deposit(TEST_LOGIN, request, TEST_OPERATION_ID))
                .isInstanceOf(OperationBlockedException.class)
                .hasMessage("Подозрительная операция");

        verify(blockerClient).check(any(OperationCheckRequestViewModel.class));

        verifyNoInteractions(accountClient, exchangeClient, accountBalanceMapper);
    }

    /**
     * <summary>
     * Проверяет выброс OperationBlockedException, если сервис блокировки
     * запрещает операцию снятия средств в RUB.
     * </summary>
     **/
    @Test
    public void shouldThrowOperationBlockedExceptionWhenWithdrawIsBlocked() {
        var request = new CashOperationRequestViewModel(new BigDecimal("200.00"), CurrencyEnumModel.RUB);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(false, "Подозрительная операция"));

        assertThatThrownBy(() -> cashService.withdraw(TEST_LOGIN, request, TEST_OPERATION_ID))
                .isInstanceOf(OperationBlockedException.class)
                .hasMessage("Подозрительная операция");

        verify(blockerClient).check(any(OperationCheckRequestViewModel.class));

        verifyNoInteractions(accountClient, exchangeClient, accountBalanceMapper);
    }

    /**
     * <summary>
     * Проверяет блокировку валютной операции после её нормализации в RUB.
     * При этом изменение баланса и отправка уведомления не выполняются.
     * </summary>
     **/
    @Test
    public void shouldBlockForeignCurrencyDepositUsingNormalizedAmount() {
        var amount = new BigDecimal("100.00");

        var normalizedAmount = new BigDecimal("9500.00");

        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.USD);

        var conversion = mock(ConversionResponseViewModel.class);

        when(conversion.targetAmount()).thenReturn(normalizedAmount);

        when(exchangeClient.convert(CurrencyEnumModel.USD, CurrencyEnumModel.RUB, amount)).thenReturn(conversion);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(false, "Подозрительная операция"));

        assertThatThrownBy(() -> cashService.deposit(TEST_LOGIN, request, TEST_OPERATION_ID))
                .isInstanceOf(OperationBlockedException.class)
                .hasMessage("Подозрительная операция");

        var captor = ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(captor.capture());

        var blockerRequest = captor.getValue();

        assertThat(blockerRequest.amount()).isEqualByComparingTo(amount);

        assertThat(blockerRequest.normalizedAmount()).isEqualByComparingTo(normalizedAmount);

        verify(exchangeClient).convert(CurrencyEnumModel.USD, CurrencyEnumModel.RUB, amount);

        verifyNoInteractions(accountClient, accountBalanceMapper);
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

        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.RUB);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(true, null));

        var balanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(balanceResponse.balance()).thenReturn(new BigDecimal("1500.00"));

        when(balanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.deposit(any())).thenReturn(balanceResponse);

        cashService.deposit(TEST_LOGIN, request, TEST_OPERATION_ID);

        var captor = ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(captor.capture());

        var blockerRequest = captor.getValue();

        assertThat(blockerRequest.operationId()).isEqualTo(TEST_OPERATION_ID.toString());

        assertThat(blockerRequest.operationType()).isEqualTo(OperationTypeEnumModel.DEPOSIT);

        assertThat(blockerRequest.login()).isEqualTo(TEST_LOGIN);

        assertThat(blockerRequest.sender()).isNull();

        assertThat(blockerRequest.recipient()).isNull();

        verifyNoInteractions(exchangeClient);
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

        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.RUB);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(true, null));

        var balanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(balanceResponse.balance()).thenReturn(new BigDecimal("800.00"));

        when(balanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.withdraw(any())).thenReturn(balanceResponse);

        cashService.withdraw(TEST_LOGIN, request, TEST_OPERATION_ID);

        var captor = ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(captor.capture());

        var blockerRequest = captor.getValue();

        assertThat(blockerRequest.operationId()).isEqualTo(TEST_OPERATION_ID.toString());

        assertThat(blockerRequest.operationType()).isEqualTo(OperationTypeEnumModel.WITHDRAW);

        assertThat(blockerRequest.login()).isEqualTo(TEST_LOGIN);
    }

    /**
     * <summary>
     * Проверяет, что нулевая и отрицательная сумма отклоняются
     * до обращения к внешним сервисам.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountExceptionWhenAmountIsZeroOrNegative() {
        var zeroRequest = new CashOperationRequestViewModel(BigDecimal.ZERO, CurrencyEnumModel.RUB);

        var negativeRequest = new CashOperationRequestViewModel(new BigDecimal("-100.00"), CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> cashService.deposit(TEST_LOGIN, zeroRequest, TEST_OPERATION_ID))
                .isInstanceOf(InvalidAmountException.class);

        assertThatThrownBy(() -> cashService.withdraw(TEST_LOGIN, negativeRequest, TEST_OPERATION_ID))
                .isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(accountClient, blockerClient, exchangeClient, accountBalanceMapper);
    }

    /**
     * <summary>
     * Проверяет, что сумма с более чем двумя знаками после запятой
     * отклоняется до обращения к внешним сервисам.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountScaleExceptionWhenScaleExceedsTwo() {
        var invalidScaleRequest = new CashOperationRequestViewModel(new BigDecimal("100.555"), CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> cashService.deposit(TEST_LOGIN, invalidScaleRequest, TEST_OPERATION_ID))
                .isInstanceOf(InvalidAmountScaleException.class);

        assertThatThrownBy(() -> cashService.withdraw(TEST_LOGIN, invalidScaleRequest, TEST_OPERATION_ID))
                .isInstanceOf(InvalidAmountScaleException.class);

        verifyNoInteractions(accountClient, blockerClient, exchangeClient, accountBalanceMapper);
    }

    // endregion
}