package ru.yandex.practicum.bank.account.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.account.exceptions.AccountNotFoundException;
import ru.yandex.practicum.bank.account.exceptions.InvalidBirthdateException;
import ru.yandex.practicum.bank.account.interfaces.AccountService;
import ru.yandex.practicum.bank.account.mappers.AccountMapper;
import ru.yandex.practicum.bank.account.models.AccountModel;
import ru.yandex.practicum.bank.account.repositories.AccountRepository;
import ru.yandex.practicum.bank.account.viewmodels.AccountResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.RecipientResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.UpdateAccountRequestViewModel;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * <summary>
 * Реализация сервиса управления банковскими счетами пользователей (AccountServiceImpl).
 * Предоставляет бизнес-логику чтения и обновления профиля, а также получения списка доступных получателей.
 * </summary>
 **/
@Service
public class AccountServiceImpl implements AccountService {

    // region Fields

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final Clock clock;

    // endregion

    // region Constructors

    public AccountServiceImpl(
            AccountRepository accountRepository,
            AccountMapper accountMapper,
            Clock clock) {

        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
        this.clock = clock;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Возвращает информацию о текущем счёте пользователя по его логину.
     * </summary>
     * @param login Логин пользователя.
     * @return ViewModel с данными счета {@link AccountResponseViewModel}.
     * @throws AccountNotFoundException Если счет с таким логином не найден.
     */
    @Override
    @Transactional(readOnly = true)
    public AccountResponseViewModel getCurrentAccount(String login) {
        return accountMapper.toResponse(findAccount(login));
    }

    /**
     * <summary>
     * Обновляет профиль пользователя (имя и дату рождения) с проверкой достижения совершеннолетия.
     * </summary>
     * @param login Логин пользователя.
     * @param request ViewModel с новыми данными профиля.
     * @return ViewModel с данными счета {@link AccountResponseViewModel}.
     * @throws InvalidBirthdateException Если пользователь несовершеннолетний (< 18 лет).
     * @throws AccountNotFoundException Если счет с таким логином не найден.
     */
    @Override
    @Transactional
    public AccountResponseViewModel updateCurrentAccount(String login, UpdateAccountRequestViewModel request) {
        if (!isAdult(request.birthdate())) {
            throw new InvalidBirthdateException();
        }

        var account = findAccount(login);

        account.updateProfile(request.name(), request.birthdate());

        return accountMapper.toResponse(accountRepository.save(account));
    }


    /**
     * <summary>
     * Возвращает список всех доступных получателей перевода, исключая текущего пользователя.
     * </summary>
     * @param currentLogin Логин текущего пользователя.
     * @return Список ViewModel получателей {@link RecipientResponseViewModel}.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RecipientResponseViewModel> getRecipients(String currentLogin) {
        return accountRepository.findAllByLoginNot(currentLogin).stream()
                .map(accountMapper::toRecipientResponse)
                .toList();
    }

    /**
     * <summary>
     * Находит счет пользователя по логину или выбрасывает исключение {@link AccountNotFoundException}.
     * </summary>
     * @param login Логин пользователя.
     * @return Найденная сущность {@link AccountModel}.
     */
    private AccountModel findAccount(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }

    /**
     * <summary>
     * Проверяет, исполнилось ли пользователю 18 лет относительно текущего времени из {@link Clock}.
     * </summary>
     * @param birthdate Дата рождения для проверки.
     * @return {@code true}, если пользователь совершеннолетний, иначе {@code false}.
     */
    private boolean isAdult(LocalDate birthdate) {
        return !birthdate.plusYears(18).isAfter(LocalDate.now(clock));
    }

    // endregion
}