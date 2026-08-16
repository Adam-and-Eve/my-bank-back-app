package ru.yandex.practicum.bank.account.services;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.practicum.bank.account.exceptions.*;
import ru.yandex.practicum.bank.account.interfaces.BalanceOperationService;
import ru.yandex.practicum.bank.account.models.AccountModel;
import ru.yandex.practicum.bank.account.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.account.repositories.AccountRepository;
import ru.yandex.practicum.bank.account.viewmodels.BalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceRequestViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <summary>
 * Юнит-тесты и интеграционные тесты механизмов повтора (Retry) для сервиса BalanceOperationServiceImpl.
 * Проверяют корректность пополнения, снятия, переводов, детерминированного сохранения сущностей,
 * валидации сумм и работы аннотации @Retryable при OptimisticLockException.
 * </summary>
 **/
public class BalanceOperationServiceImplTest {

    // region Constants

    private static final String LOGIN_DMITRY = "dmitry";

    private static final String LOGIN_ALEXEY = "alexey";

    private static final String LOGIN_UNKNOWN = "unknown";

    private static final String OPERATION_ID = "op-uuid-12345";

    // endregion

    // region Unit Tests (Pure Mockito)

    @Nested
    @ExtendWith(MockitoExtension.class)
    class UnitTests {

        @Mock
        private AccountRepository accountRepository;

        private BalanceOperationServiceImpl balanceOperationService;

        @BeforeEach
        public void setUp() {
            balanceOperationService = new BalanceOperationServiceImpl(accountRepository);
        }

        // region Tests - Deposit

        /**
         * <summary>
         * Проверяет успешное пополнение баланса счета.
         * </summary>
         **/
        @Test
        public void shouldDepositBalanceSuccessfully() {
            var account = createAccount(1L, LOGIN_DMITRY, new BigDecimal("1000.00"));

            var request = new BalanceOperationRequestViewModel(
                    LOGIN_DMITRY,
                    new BigDecimal("500.00"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
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

        /**
         * <summary>
         * Проверяет выброс AccountNotFoundException при попытке пополнения несуществующего счета.
         * </summary>
         **/
        @Test
        public void shouldThrowAccountNotFoundExceptionWhenDepositingToUnknownAccount() {
            var request = new BalanceOperationRequestViewModel(
                    LOGIN_UNKNOWN,
                    new BigDecimal("100.00"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );

            when(accountRepository.findByLogin(LOGIN_UNKNOWN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> balanceOperationService.deposit(request))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        // endregion

        // region Tests - Withdraw

        /**
         * <summary>
         * Проверяет успешное списание денежных средств со счета.
         * </summary>
         **/
        @Test
        public void shouldWithdrawBalanceSuccessfully() {
            var account = createAccount(1L, LOGIN_DMITRY, new BigDecimal("1000.00"));

            var request = new BalanceOperationRequestViewModel(
                    LOGIN_DMITRY,
                    new BigDecimal("400.00"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );

            when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(account));

            when(accountRepository.save(any(AccountModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var response = balanceOperationService.withdraw(request);

            assertThat(response).isNotNull();

            assertThat(response.balance()).isEqualByComparingTo("600.00");

            verify(accountRepository).save(account);
        }

        /**
         * <summary>
         * Проверяет выброс InsufficientFundsException, если сумма списания превышает доступный баланс.
         * </summary>
         **/
        @Test
        public void shouldThrowInsufficientFundsExceptionWhenBalanceIsInsufficient() {
            var account = createAccount(1L, LOGIN_DMITRY, new BigDecimal("100.00"));

            var request = new BalanceOperationRequestViewModel(
                    LOGIN_DMITRY,
                    new BigDecimal("150.00"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );

            when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> balanceOperationService.withdraw(request))
                    .isInstanceOf(InsufficientFundsException.class);

            verify(accountRepository, never()).save(any());
        }

        // endregion

        // region Tests - Transfer & Deterministic Order

        /**
         * <summary>
         * Проверяет успешный перевод средств между счетами и детерминированный порядок сохранения
         * моделей по возрастанию ID для предотвращения взаимных блокировок (Deadlocks).
         * ID Отправителя (2L) > ID Получателя (1L), поэтому первым должен сохраниться Получатель (1L).
         * </summary>
         **/
        @Test
        public void shouldTransferBalanceAndSaveAccountsInDeterministicOrderById() {
            var sender = createAccount(2L, LOGIN_DMITRY, new BigDecimal("1000.00"));

            var recipient = createAccount(1L, LOGIN_ALEXEY, new BigDecimal("500.00"));

            var request = new TransferBalanceRequestViewModel(
                    LOGIN_DMITRY,
                    LOGIN_ALEXEY,
                    new BigDecimal("300.00"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );

            when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(sender));

            when(accountRepository.findByLogin(LOGIN_ALEXEY)).thenReturn(Optional.of(recipient));

            var response = balanceOperationService.transfer(request);

            assertThat(response).isNotNull();

            assertThat(response.senderLogin()).isEqualTo(LOGIN_DMITRY);

            assertThat(response.recipientLogin()).isEqualTo(LOGIN_ALEXEY);

            assertThat(response.senderBalance()).isEqualByComparingTo("700.00");

            assertThat(recipient.getBalance()).isEqualByComparingTo("800.00");

            InOrder inOrder = Mockito.inOrder(accountRepository);

            inOrder.verify(accountRepository).save(recipient);

            inOrder.verify(accountRepository).save(sender);
        }

        /**
         * <summary>
         * Проверяет выброс SelfTransferForbiddenException при попытке сделать перевод самому себе.
         * </summary>
         **/
        @Test
        public void shouldThrowSelfTransferForbiddenExceptionWhenTransferringToSelf() {
            var request = new TransferBalanceRequestViewModel(
                    LOGIN_DMITRY,
                    LOGIN_DMITRY,
                    new BigDecimal("100.00"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );

            assertThatThrownBy(() -> balanceOperationService.transfer(request))
                    .isInstanceOf(SelfTransferForbiddenException.class);

            verify(accountRepository, never()).findByLogin(any());
        }

        /**
         * <summary>
         * Проверяет выброс RecipientNotFoundException, если получатель перевода не найден.
         * </summary>
         **/
        @Test
        public void shouldThrowRecipientNotFoundExceptionWhenRecipientDoesNotExist() {
            var sender = createAccount(1L, LOGIN_DMITRY, new BigDecimal("1000.00"));

            var request = new TransferBalanceRequestViewModel(
                    LOGIN_DMITRY,
                    LOGIN_UNKNOWN,
                    new BigDecimal("100.00"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );

            when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(sender));

            when(accountRepository.findByLogin(LOGIN_UNKNOWN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> balanceOperationService.transfer(request))
                    .isInstanceOf(RecipientNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        // endregion

        // region Tests - Validation

        /**
         * <summary>
         * Проверяет выброс InvalidAmountException, если сумма нулевая или отрицательная.
         * </summary>
         **/
        @Test
        public void shouldThrowInvalidAmountExceptionForZeroOrNegativeAmount() {
            var zeroRequest = new BalanceOperationRequestViewModel(
                    LOGIN_DMITRY,
                    BigDecimal.ZERO,
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );
            var negativeRequest = new BalanceOperationRequestViewModel(
                    LOGIN_DMITRY,
                    new BigDecimal("-10.00"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );

            assertThatThrownBy(() -> balanceOperationService.deposit(zeroRequest))
                    .isInstanceOf(InvalidAmountException.class);

            assertThatThrownBy(() -> balanceOperationService.deposit(negativeRequest))
                    .isInstanceOf(InvalidAmountException.class);
        }

        /**
         * <summary>
         * Проверяет выброс InvalidAmountScaleException, если сумма имеет более 2 знаков после запятой.
         * </summary>
         **/
        @Test
        public void shouldThrowInvalidAmountScaleExceptionWhenScaleExceedsTwo() {
            var invalidScaleRequest = new BalanceOperationRequestViewModel(
                    LOGIN_DMITRY,
                    new BigDecimal("10.555"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );

            assertThatThrownBy(() -> balanceOperationService.deposit(invalidScaleRequest))
                    .isInstanceOf(InvalidAmountScaleException.class);
        }

        // endregion
    }

    // endregion

    // region Integration Tests for @Retryable (Spring Context)

    @Nested
    @SpringJUnitConfig
    @ContextConfiguration(classes = {
            BalanceOperationServiceImpl.class,
            RetryTests.RetryTestConfig.class
    })
    class RetryTests {

        @EnableRetry
        static class RetryTestConfig {
        }

        @MockitoBean
        private AccountRepository accountRepository;

        @Autowired
        private BalanceOperationService balanceOperationService;

        /**
         * <summary>
         * Проверяет успешный повтор операции (Retry) после выброса OptimisticLockException.
         * Первая попытка выбрасывает исключение, вторая проходит успешно.
         * </summary>
         **/
        @Test
        public void shouldRetryAndSucceedWhenOptimisticLockExceptionOccurs() {
            var request = new BalanceOperationRequestViewModel(
                    LOGIN_DMITRY,
                    new BigDecimal("200.00"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );

            when(accountRepository.findByLogin(LOGIN_DMITRY))
                    .thenAnswer(invocation -> Optional.of(createAccount(1L, LOGIN_DMITRY, new BigDecimal("1000.00"))));

            when(accountRepository.save(any(AccountModel.class)))
                    .thenThrow(new OptimisticLockException("Conflict"))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            var response = balanceOperationService.deposit(request);

            assertThat(response).isNotNull();

            assertThat(response.balance()).isEqualByComparingTo("1200.00");

            verify(accountRepository, times(2)).findByLogin(LOGIN_DMITRY);

            verify(accountRepository, times(2)).save(any(AccountModel.class));
        }

        /**
         * <summary>
         * Проверяет превышение максимального лимита попыток (3 попытки) при постоянных сбоях оптимистичной блокировки.
         * </summary>
         **/
        @Test
        public void shouldExhaustRetriesAndThrowExceptionAfterThreeAttempts() {
            var account = createAccount(1L, LOGIN_DMITRY, new BigDecimal("1000.00"));

            var request = new BalanceOperationRequestViewModel(
                    LOGIN_DMITRY,
                    new BigDecimal("200.00"),
                    CurrencyEnumModel.RUB,
                    OPERATION_ID
            );

            when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(account));

            when(accountRepository.save(any(AccountModel.class)))
                    .thenThrow(new ObjectOptimisticLockingFailureException(AccountModel.class, 1L));

            assertThatThrownBy(() -> balanceOperationService.deposit(request))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);

            verify(accountRepository, times(3)).save(account);
        }
    }

    // endregion

    // region Helper Methods

    private static AccountModel createAccount(Long id, String login, BigDecimal balance) {
        var account = new AccountModel(
                login,
                "Тестовый Пользователь",
                LocalDate.of(1995, 5, 15),
                balance,
                CurrencyEnumModel.RUB
        );

        ReflectionTestUtils.setField(account, "id", id);

        return account;
    }

    // endregion
}