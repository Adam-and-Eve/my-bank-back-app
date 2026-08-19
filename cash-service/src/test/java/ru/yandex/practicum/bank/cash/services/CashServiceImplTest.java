package ru.yandex.practicum.bank.cash.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.cash.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.cash.interfaces.AccountClient;
import ru.yandex.practicum.bank.shared.interfaces.NotificationClient;
import ru.yandex.practicum.bank.cash.mappers.AccountBalanceMapper;
import ru.yandex.practicum.bank.cash.viewmodels.AccountBalanceResponseViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.NotificationRequestViewModel;

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
 * корректное преобразование данных маппером и генерацию/сквозную передачу operationId.
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
    private NotificationClient notificationClient;

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    @InjectMocks
    private CashServiceImpl cashService;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную операцию пополнения счета, включая валидацию суммы,
     * обращение к клиенту счетов и отправку события в сервис уведомлений.
     * </summary>
     **/
    @Test
    public void shouldDepositSuccessfully() {
        var amount = new BigDecimal("500.00");

        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.RUB);

        var mockBalanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(mockBalanceResponse.balance()).thenReturn(new BigDecimal("1500.00"));

        when(mockBalanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.deposit(any())).thenReturn(mockBalanceResponse);

        CashOperationResponseViewModel response = cashService.deposit(TEST_LOGIN, request);

        assertThat(response).isNotNull();

        assertThat(response.balance()).isEqualTo(new BigDecimal("1500.00"));

        assertThat(response.currency()).isEqualTo(CURRENCY);

        assertThat(response.message()).isEqualTo("Счёт пополнен");

        var operationIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(accountBalanceMapper).toAccountsRequest(eq(TEST_LOGIN), eq(request), operationIdCaptor.capture());

        var notificationCaptor = ArgumentCaptor.forClass(NotificationRequestViewModel.class);

        verify(notificationClient).notify(notificationCaptor.capture());

        NotificationRequestViewModel sentNotification = notificationCaptor.getValue();

        assertThat(sentNotification.type()).isEqualTo("CASH_DEPOSIT");

        assertThat(sentNotification.message()).isEqualTo("Счёт пополнен на 500.00 RUB");

        assertThat(sentNotification.operationId()).isEqualTo(operationIdCaptor.getValue());
    }

    /**
     * <summary>
     * Проверяет успешную операцию снятия средств со счета, включая корректное формирование
     * типа уведомления CASH_WITHDRAW и передачу сгенерированного operationId.
     * </summary>
     **/
    @Test
    public void shouldWithdrawSuccessfully() {
        var amount = new BigDecimal("200.00");

        var request = new CashOperationRequestViewModel(amount, CurrencyEnumModel.RUB);

        var mockBalanceResponse = mock(AccountBalanceResponseViewModel.class);

        when(mockBalanceResponse.balance()).thenReturn(new BigDecimal("800.00"));

        when(mockBalanceResponse.currency()).thenReturn(CURRENCY);

        when(accountClient.withdraw(any())).thenReturn(mockBalanceResponse);

        CashOperationResponseViewModel response = cashService.withdraw(TEST_LOGIN, request);

        assertThat(response).isNotNull();

        assertThat(response.balance()).isEqualTo(new BigDecimal("800.00"));

        assertThat(response.currency()).isEqualTo(CURRENCY);

        assertThat(response.message()).isEqualTo("Деньги сняты со счёта");

        var operationIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(accountBalanceMapper).toAccountsRequest(eq(TEST_LOGIN), eq(request), operationIdCaptor.capture());

        var notificationCaptor = ArgumentCaptor.forClass(NotificationRequestViewModel.class);

        verify(notificationClient).notify(notificationCaptor.capture());

        NotificationRequestViewModel sentNotification = notificationCaptor.getValue();

        assertThat(sentNotification.type()).isEqualTo("CASH_WITHDRAW");

        assertThat(sentNotification.message()).isEqualTo("Со счёта снято 200.00 RUB");

        assertThat(sentNotification.operationId()).isEqualTo(operationIdCaptor.getValue());
    }

    /**
     * <summary>
     * Проверяет выброс исключения InvalidAmountException при попытке передать нулевую или отрицательную сумму.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountExceptionWhenAmountIsZeroOrNegative() {
        var zeroRequest = new CashOperationRequestViewModel(BigDecimal.ZERO, CurrencyEnumModel.RUB);

        var negativeRequest = new CashOperationRequestViewModel(new BigDecimal("-100.00"), CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> cashService.deposit(TEST_LOGIN, zeroRequest))
                .isInstanceOf(InvalidAmountException.class);

        assertThatThrownBy(() -> cashService.withdraw(TEST_LOGIN, negativeRequest))
                .isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(accountClient, notificationClient, accountBalanceMapper);
    }

    /**
     * <summary>
     * Проверяет выброс исключения InvalidAmountScaleException при попытке передать сумму,
     * содержащую более двух знаков после запятой (дробных копеек).
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountScaleExceptionWhenScaleExceedsTwo() {
        var invalidScaleRequest = new CashOperationRequestViewModel(new BigDecimal("100.555"), CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> cashService.deposit(TEST_LOGIN, invalidScaleRequest))
                .isInstanceOf(InvalidAmountScaleException.class);

        assertThatThrownBy(() -> cashService.withdraw(TEST_LOGIN, invalidScaleRequest))
                .isInstanceOf(InvalidAmountScaleException.class);

        verifyNoInteractions(accountClient, notificationClient, accountBalanceMapper);
    }

    // endregion
}