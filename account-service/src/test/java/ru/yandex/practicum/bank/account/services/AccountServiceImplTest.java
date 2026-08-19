package ru.yandex.practicum.bank.account.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.account.exceptions.AccountNotFoundException;
import ru.yandex.practicum.bank.account.exceptions.InvalidBirthdateException;
import ru.yandex.practicum.bank.account.mappers.AccountMapper;
import ru.yandex.practicum.bank.account.models.AccountModel;
import ru.yandex.practicum.bank.account.repositories.AccountRepository;
import ru.yandex.practicum.bank.account.viewmodels.AccountResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.RecipientResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.UpdateAccountRequestViewModel;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Юнит-тесты бизнес-логики сервиса управления счетами (AccountServiceImpl).
 * Проверяют получение профиля, обновление данных с контролем совершеннолетия (18+),
 * выборку получателей и корректность обработки ошибок.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {

    // region Constants

    private static final String LOGIN_DMITRY = "dmitry";

    private static final String LOGIN_ALEXEY = "alexey";

    private static final String LOGIN_UNKNOWN = "unknown";

    private static final LocalDate FIXED_CURRENT_DATE = LocalDate.of(2026, 8, 14);

    // endregion

    // region Fields

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    private Clock fixedClock;

    private AccountServiceImpl accountService;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        var fixedInstant = FIXED_CURRENT_DATE.atStartOfDay(ZoneId.of("UTC")).toInstant();

        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        accountService = new AccountServiceImpl(accountRepository, accountMapper, fixedClock);
    }

    // endregion

    // region Tests - getCurrentAccount

    /**
     * <summary>
     * Проверяет успешное получение информации о текущем счёте пользователя.
     * </summary>
     **/
    @Test
    public void shouldReturnAccountResponseWhenAccountExists() {
        var account = createAccount(LOGIN_DMITRY, "Дмитрий Волков");

        var expectedResponse = new AccountResponseViewModel(
                LOGIN_DMITRY,
                "Дмитрий Волков",
                account.getBirthdate(),
                account.getBalance(),
                account.getCurrency().name()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(account));

        when(accountMapper.toResponse(account)).thenReturn(expectedResponse);

        var response = accountService.getCurrentAccount(LOGIN_DMITRY);

        assertThat(response).isNotNull();

        assertThat(response.login()).isEqualTo(LOGIN_DMITRY);

        assertThat(response.name()).isEqualTo("Дмитрий Волков");

        verify(accountRepository).findByLogin(LOGIN_DMITRY);

        verify(accountMapper).toResponse(account);
    }

    /**
     * <summary>
     * Проверяет выбрасывание AccountNotFoundException при попытке запросить несуществующий счёт.
     * </summary>
     **/
    @Test
    public void shouldThrowAccountNotFoundExceptionWhenAccountDoesNotExist() {
        when(accountRepository.findByLogin(LOGIN_UNKNOWN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getCurrentAccount(LOGIN_UNKNOWN))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository).findByLogin(LOGIN_UNKNOWN);

        verify(accountMapper, never()).toResponse(any());
    }

    // endregion

    // region Tests - updateCurrentAccount

    /**
     * <summary>
     * Проверяет успешное обновление данных профиля, если возраст ровно 18 лет или старше.
     * </summary>
     **/
    @Test
    public void shouldUpdateAccountWhenUserIsAdult() {
        var validBirthdate = LocalDate.of(2008, 8, 14);

        var request = new UpdateAccountRequestViewModel("Дмитрий Обновлённый", validBirthdate);

        var account = createAccount(LOGIN_DMITRY, "Дмитрий Старый");

        var updatedResponse = new AccountResponseViewModel(
                LOGIN_DMITRY,
                "Дмитрий Обновлённый",
                validBirthdate,
                account.getBalance(),
                account.getCurrency().name()
        );

        when(accountRepository.findByLogin(LOGIN_DMITRY)).thenReturn(Optional.of(account));

        when(accountRepository.save(account)).thenReturn(account);

        when(accountMapper.toResponse(account)).thenReturn(updatedResponse);

        var response = accountService.updateCurrentAccount(LOGIN_DMITRY, request);

        assertThat(response).isNotNull();

        assertThat(response.name()).isEqualTo("Дмитрий Обновлённый");

        verify(accountRepository).findByLogin(LOGIN_DMITRY);

        verify(accountRepository).save(account);

        verify(accountMapper).toResponse(account);
    }

    /**
     * <summary>
     * Проверяет выброс InvalidBirthdateException, если пользователю ещё нет 18 лет (несовершеннолетний на 1 день).
     * Убеждается, что вызовы к репозиторию для сохранения не производятся.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidBirthdateExceptionWhenUserIsUnderage() {
        var underageBirthdate = LocalDate.of(2008, 8, 15);

        var request = new UpdateAccountRequestViewModel("Молодой Игрок", underageBirthdate);

        assertThatThrownBy(() -> accountService.updateCurrentAccount(LOGIN_DMITRY, request))
                .isInstanceOf(InvalidBirthdateException.class);

        verify(accountRepository, never()).findByLogin(any());

        verify(accountRepository, never()).save(any());
    }

    /**
     * <summary>
     * Проверяет выбрасывание AccountNotFoundException при попытке обновить несуществующий аккаунт.
     * </summary>
     **/
    @Test
    public void shouldThrowAccountNotFoundExceptionWhenUpdatingNonExistentAccount() {
        var validBirthdate = LocalDate.of(2000, 1, 1);

        var request = new UpdateAccountRequestViewModel("Новое Имя", validBirthdate);

        when(accountRepository.findByLogin(LOGIN_UNKNOWN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.updateCurrentAccount(LOGIN_UNKNOWN, request))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository).findByLogin(LOGIN_UNKNOWN);

        verify(accountRepository, never()).save(any());
    }

    // endregion

    // region Tests - getRecipients

    /**
     * <summary>
     * Проверяет успешное получение списка получателей перевода с исключением текущего пользователя.
     * </summary>
     **/
    @Test
    public void shouldReturnRecipientsExceptCurrentLogin() {
        var alexeyAccount = createAccount(LOGIN_ALEXEY, "Алексей Морозов");

        var alexeyRecipientVm = new RecipientResponseViewModel(LOGIN_ALEXEY, "Алексей Морозов");

        when(accountRepository.findAllByLoginNot(LOGIN_DMITRY)).thenReturn(List.of(alexeyAccount));

        when(accountMapper.toRecipientResponse(alexeyAccount)).thenReturn(alexeyRecipientVm);

        var recipients = accountService.getRecipients(LOGIN_DMITRY);

        assertThat(recipients)
                .hasSize(1)
                .extracting(RecipientResponseViewModel::login)
                .containsExactly(LOGIN_ALEXEY);

        verify(accountRepository).findAllByLoginNot(LOGIN_DMITRY);

        verify(accountMapper).toRecipientResponse(alexeyAccount);
    }

    /**
     * <summary>
     * Проверяет возвращение пустого списка получателей, когда других пользователей в системе нет.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyListWhenNoOtherRecipientsExist() {
        when(accountRepository.findAllByLoginNot(LOGIN_DMITRY)).thenReturn(List.of());

        var recipients = accountService.getRecipients(LOGIN_DMITRY);

        assertThat(recipients).isEmpty();

        verify(accountRepository).findAllByLoginNot(LOGIN_DMITRY);

        verify(accountMapper, never()).toRecipientResponse(any());
    }

    // endregion

    // region Helper Methods

    private AccountModel createAccount(String login, String name) {
        return new AccountModel(
                login,
                name,
                LocalDate.of(1999, 9, 19),
                new BigDecimal("1000.00"),
                CurrencyEnumModel.RUB
        );
    }

    // endregion
}