package ru.yandex.practicum.bank.account.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.practicum.bank.account.exceptions.*;
import ru.yandex.practicum.bank.account.models.AccountModel;
import ru.yandex.practicum.bank.account.repositories.AccountRepository;
import ru.yandex.practicum.bank.account.repositories.OutboxNotificationRepository;
import ru.yandex.practicum.bank.account.viewmodels.BalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceRequestViewModel;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <summary>
 * Юнит-тесты бизнес-логики балансовых операций (BalanceOperationServiceImpl).
 * Проверяют корректность пополнения, снятия, переводов, переводов с конвертацией валют,
 * детерминированного сохранения сущностей и строгую валидацию сумм и валют.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class BalanceOperationServiceImplTest {

    // region Constants

    private static final String LOGIN_DMITRY = "dmitry";
    private static final String LOGIN_ALEXEY = "alexey";
    private static final String LOGIN_UNKNOWN = "unknown";
    private static final String OPERATION_ID = "op-uuid-12345";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-27T12:00:00Z");

    // endregion

    // region Fields

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OutboxNotificationRepository outboxRepository;

    private ObjectMapper objectMapper;
    private Clock clock;

    private BalanceOperationServiceImpl balanceOperationService;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        clock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));

        balanceOperationService = new BalanceOperationServiceImpl(
                accountRepository,
                outboxRepository,
                objectMapper,
                clock
        );
    }

    // endregion

    // region Tests - Deposit

    @Test
    public void shouldDepositBalanceSuccessfully() {
        var account = createAccount(1L, LOGIN_DMITRY, new BigDecimal("1000.00"), CurrencyEnumModel.RUB);

        var request = new BalanceOperationRequestViewModel(
                LOGIN_DMITRY,
                new BigDecimal("500.00"),
                CurrencyEnumModel.RUB,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(account));

        when(accountRepository.save(any(AccountModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = balanceOperationService.deposit(request);

        assertThat(response).isNotNull();

        assertThat(response.login()).isEqualTo(LOGIN_DMITRY);

        assertThat(response.balance()).isEqualByComparingTo("1500.00");

        assertThat(response.currency()).isEqualTo("RUB");

        verify(accountRepository).findByLogin(LOGIN_DMITRY);

        verify(accountRepository).save(account);
    }

    @Test
    public void shouldThrowAccountNotFoundExceptionWhenDepositingToUnknownAccount() {
        var request = new BalanceOperationRequestViewModel(
                LOGIN_UNKNOWN,
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_UNKNOWN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceOperationService.deposit(request))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    public void shouldThrowCurrencyMismatchExceptionWhenDepositCurrencyDiffers() {
        var account = createAccount(1L, LOGIN_DMITRY, new BigDecimal("1000.00"), CurrencyEnumModel.RUB);

        var request = new BalanceOperationRequestViewModel(
                LOGIN_DMITRY,
                new BigDecimal("500.00"),
                CurrencyEnumModel.USD,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> balanceOperationService.deposit(request))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(accountRepository, never()).save(any());
    }

    // endregion

    // region Tests - Withdraw

    @Test
    public void shouldWithdrawBalanceSuccessfully() {
        var account = createAccount(1L, LOGIN_DMITRY, new BigDecimal("1000.00"), CurrencyEnumModel.RUB);

        var request = new BalanceOperationRequestViewModel(
                LOGIN_DMITRY,
                new BigDecimal("400.00"),
                CurrencyEnumModel.RUB,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(account));

        when(accountRepository.save(any(AccountModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = balanceOperationService.withdraw(request);

        assertThat(response).isNotNull();

        assertThat(response.balance()).isEqualByComparingTo("600.00");

        verify(accountRepository).save(account);
    }

    @Test
    public void shouldThrowInsufficientFundsExceptionWhenBalanceIsInsufficient() {
        var account = createAccount(1L, LOGIN_DMITRY, new BigDecimal("100.00"), CurrencyEnumModel.RUB);

        var request = new BalanceOperationRequestViewModel(
                LOGIN_DMITRY,
                new BigDecimal("150.00"),
                CurrencyEnumModel.RUB,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> balanceOperationService.withdraw(request))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    public void shouldThrowCurrencyMismatchExceptionWhenWithdrawCurrencyDiffers() {
        var account = createAccount(1L, LOGIN_DMITRY, new BigDecimal("1000.00"), CurrencyEnumModel.RUB);

        var request = new BalanceOperationRequestViewModel(
                LOGIN_DMITRY,
                new BigDecimal("400.00"),
                CurrencyEnumModel.CNY,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> balanceOperationService.withdraw(request))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(accountRepository, never()).save(any());
    }

    // endregion

    // region Tests - Transfer

    @Test
    public void shouldTransferBalanceSuccessfully() {
        var sender = createAccount(2L, LOGIN_DMITRY, new BigDecimal("1000.00"), CurrencyEnumModel.RUB);

        var recipient = createAccount(1L, LOGIN_ALEXEY, new BigDecimal("500.00"), CurrencyEnumModel.RUB);

        var request = new TransferBalanceRequestViewModel(
                LOGIN_DMITRY,
                LOGIN_ALEXEY,
                new BigDecimal("300.00"),
                CurrencyEnumModel.RUB,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(sender));

        when(accountRepository.findByLogin(LOGIN_ALEXEY)).thenReturn(Optional.of(recipient));

        var response = balanceOperationService.transfer(request);

        assertThat(response).isNotNull();

        assertThat(response.senderLogin()).isEqualTo(LOGIN_DMITRY);

        assertThat(response.recipientLogin()).isEqualTo(LOGIN_ALEXEY);

        assertThat(response.senderBalance()).isEqualByComparingTo("700.00");

        assertThat(sender.getBalance()).isEqualByComparingTo("700.00");

        assertThat(recipient.getBalance()).isEqualByComparingTo("800.00");

        InOrder inOrder = Mockito.inOrder(accountRepository);

        inOrder.verify(accountRepository).save(recipient);

        inOrder.verify(accountRepository).save(sender);
    }

    @Test
    public void shouldTransferBalanceWithCurrencyConversion() {
        var sender = createAccount(2L, LOGIN_DMITRY, new BigDecimal("1000.00"), CurrencyEnumModel.RUB);

        var recipient = createAccount(1L, LOGIN_ALEXEY, new BigDecimal("500.00"), CurrencyEnumModel.USD);

        var request = new TransferBalanceRequestViewModel(
                LOGIN_DMITRY,
                LOGIN_ALEXEY,
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB,
                new BigDecimal("1.10"),
                CurrencyEnumModel.USD,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(sender));

        when(accountRepository.findByLogin(LOGIN_ALEXEY)).thenReturn(Optional.of(recipient));

        var response = balanceOperationService.transfer(request);

        assertThat(response).isNotNull();

        assertThat(response.senderBalance()).isEqualByComparingTo("900.00");

        assertThat(sender.getBalance()).isEqualByComparingTo("900.00");

        assertThat(recipient.getBalance()).isEqualByComparingTo("501.10");

        InOrder inOrder = Mockito.inOrder(accountRepository);

        inOrder.verify(accountRepository).save(recipient);

        inOrder.verify(accountRepository).save(sender);
    }

    @Test
    public void shouldUseSourceAmountAndCurrencyWhenRecipientValuesAreNull() {
        var sender = createAccount(2L, LOGIN_DMITRY, new BigDecimal("1000.00"), CurrencyEnumModel.RUB);

        var recipient = createAccount(1L, LOGIN_ALEXEY, new BigDecimal("500.00"), CurrencyEnumModel.RUB);

        var request = new TransferBalanceRequestViewModel(
                LOGIN_DMITRY,
                LOGIN_ALEXEY,
                new BigDecimal("300.00"),
                CurrencyEnumModel.RUB,
                null,
                null,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(sender));

        when(accountRepository.findByLogin(LOGIN_ALEXEY)).thenReturn(Optional.of(recipient));

        balanceOperationService.transfer(request);

        assertThat(sender.getBalance()).isEqualByComparingTo("700.00");

        assertThat(recipient.getBalance()).isEqualByComparingTo("800.00");
    }

    @Test
    public void shouldThrowSelfTransferForbiddenExceptionWhenTransferringToSelf() {
        var request = new TransferBalanceRequestViewModel(
                LOGIN_DMITRY,
                LOGIN_DMITRY,
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB,
                OPERATION_ID,
                List.of()
        );

        assertThatThrownBy(() -> balanceOperationService.transfer(request))
                .isInstanceOf(SelfTransferForbiddenException.class);

        verify(accountRepository, never()).findByLogin(any());
    }

    @Test
    public void shouldThrowRecipientNotFoundExceptionWhenRecipientDoesNotExist() {
        var sender = createAccount(1L, LOGIN_DMITRY, new BigDecimal("1000.00"), CurrencyEnumModel.RUB);

        var request = new TransferBalanceRequestViewModel(
                LOGIN_DMITRY,
                LOGIN_UNKNOWN,
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(sender));

        when(accountRepository.findByLogin(LOGIN_UNKNOWN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceOperationService.transfer(request))
                .isInstanceOf(RecipientNotFoundException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    public void shouldThrowInsufficientFundsExceptionWhenTransferAmountExceedsSenderBalance() {
        var sender = createAccount(2L, LOGIN_DMITRY, new BigDecimal("100.00"), CurrencyEnumModel.RUB);

        var recipient = createAccount(1L, LOGIN_ALEXEY, new BigDecimal("500.00"), CurrencyEnumModel.RUB);

        var request = new TransferBalanceRequestViewModel(
                LOGIN_DMITRY,
                LOGIN_ALEXEY,
                new BigDecimal("150.00"),
                CurrencyEnumModel.RUB,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(sender));

        when(accountRepository.findByLogin(LOGIN_ALEXEY)).thenReturn(Optional.of(recipient));

        assertThatThrownBy(() -> balanceOperationService.transfer(request))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");

        assertThat(recipient.getBalance()).isEqualByComparingTo("500.00")
        ;
        verify(accountRepository, never()).save(any());
    }

    @Test
    public void shouldThrowCurrencyMismatchExceptionWhenSenderCurrencyDiffers() {
        var sender = createAccount(2L, LOGIN_DMITRY, new BigDecimal("1000.00"), CurrencyEnumModel.RUB);

        var recipient = createAccount(1L, LOGIN_ALEXEY, new BigDecimal("500.00"), CurrencyEnumModel.USD);

        var request = new TransferBalanceRequestViewModel(
                LOGIN_DMITRY,
                LOGIN_ALEXEY,
                new BigDecimal("100.00"),
                CurrencyEnumModel.CNY,
                new BigDecimal("1.10"),
                CurrencyEnumModel.USD,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(sender));

        when(accountRepository.findByLogin(LOGIN_ALEXEY)).thenReturn(Optional.of(recipient));

        assertThatThrownBy(() -> balanceOperationService.transfer(request))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    public void shouldThrowCurrencyMismatchExceptionWhenRecipientCurrencyDiffers() {
        var sender = createAccount(2L, LOGIN_DMITRY, new BigDecimal("1000.00"), CurrencyEnumModel.RUB);

        var recipient = createAccount(1L, LOGIN_ALEXEY, new BigDecimal("500.00"), CurrencyEnumModel.USD);

        var request = new TransferBalanceRequestViewModel(
                LOGIN_DMITRY,
                LOGIN_ALEXEY,
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB,
                new BigDecimal("1.10"),
                CurrencyEnumModel.CNY,
                OPERATION_ID,
                List.of()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(sender));
        when(accountRepository.findByLogin(LOGIN_ALEXEY)).thenReturn(Optional.of(recipient));

        assertThatThrownBy(() -> balanceOperationService.transfer(request))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(accountRepository, never()).save(any());
    }

    // endregion

    // region Tests - Validation

    @Test
    public void shouldThrowInvalidAmountExceptionForZeroOrNegativeAmount() {
        var zeroRequest = new BalanceOperationRequestViewModel(
                LOGIN_DMITRY, BigDecimal.ZERO, CurrencyEnumModel.RUB, OPERATION_ID, List.of()
        );

        var negativeRequest = new BalanceOperationRequestViewModel(
                LOGIN_DMITRY, new BigDecimal("-10.00"), CurrencyEnumModel.RUB, OPERATION_ID, List.of()
        );

        assertThatThrownBy(() -> balanceOperationService.deposit(zeroRequest))
                .isInstanceOf(InvalidAmountException.class);

        assertThatThrownBy(() -> balanceOperationService.deposit(negativeRequest))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenAmountIsNull() {
        var request = new BalanceOperationRequestViewModel(
                LOGIN_DMITRY, null, CurrencyEnumModel.RUB, OPERATION_ID, List.of()
        );

        assertThatThrownBy(() -> balanceOperationService.deposit(request))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldThrowInvalidAmountScaleExceptionWhenScaleExceedsTwo() {
        var invalidScaleRequest = new BalanceOperationRequestViewModel(
                LOGIN_DMITRY, new BigDecimal("10.555"), CurrencyEnumModel.RUB, OPERATION_ID, List.of()
        );

        assertThatThrownBy(() -> balanceOperationService.deposit(invalidScaleRequest))
                .isInstanceOf(InvalidAmountScaleException.class);
    }

    @Test
    public void shouldThrowInvalidAmountExceptionWhenRecipientAmountIsInvalid() {
        var request = new TransferBalanceRequestViewModel(
                LOGIN_DMITRY, LOGIN_ALEXEY, new BigDecimal("100.00"), CurrencyEnumModel.RUB,
                BigDecimal.ZERO, CurrencyEnumModel.USD, OPERATION_ID, List.of()
        );

        assertThatThrownBy(() -> balanceOperationService.transfer(request))
                .isInstanceOf(InvalidAmountException.class);

        verify(accountRepository, never()).findByLogin(any());
    }

    @Test
    public void shouldThrowInvalidAmountScaleExceptionWhenRecipientAmountScaleExceedsTwo() {
        var request = new TransferBalanceRequestViewModel(
                LOGIN_DMITRY, LOGIN_ALEXEY, new BigDecimal("100.00"), CurrencyEnumModel.RUB,
                new BigDecimal("1.555"), CurrencyEnumModel.USD, OPERATION_ID, List.of()
        );

        assertThatThrownBy(() -> balanceOperationService.transfer(request))
                .isInstanceOf(InvalidAmountScaleException.class);

        verify(accountRepository, never()).findByLogin(any());
    }

    @Test
    public void shouldThrowExceptionWhenTransferRequestIsNull() {
        assertThatThrownBy(() -> balanceOperationService.transfer(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Transfer request must not be null");

        verifyNoInteractions(accountRepository);
    }

    @Test
    public void shouldThrowExceptionWhenDepositRequestIsNull() {
        assertThatThrownBy(() -> balanceOperationService.deposit(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Deposit request must not be null");

        verifyNoInteractions(accountRepository);
    }

    @Test
    public void shouldThrowExceptionWhenWithdrawRequestIsNull() {
        assertThatThrownBy(() -> balanceOperationService.withdraw(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Withdraw request must not be null");

        verifyNoInteractions(accountRepository);
    }

    // endregion

    // region Helper Methods

    private static AccountModel createAccount(
            Long id,
            String login,
            BigDecimal balance,
            CurrencyEnumModel currency
    ) {
        var account = new AccountModel(
                login,
                "Тестовый Пользователь",
                LocalDate.of(1995, 5, 15),
                balance,
                currency
        );

        ReflectionTestUtils.setField(account, "id", id);

        return account;
    }

    // endregion
}