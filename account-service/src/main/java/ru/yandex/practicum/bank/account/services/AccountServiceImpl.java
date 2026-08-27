package ru.yandex.practicum.bank.account.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.account.exceptions.AccountNotFoundException;
import ru.yandex.practicum.bank.account.exceptions.InvalidBirthdateException;
import ru.yandex.practicum.bank.account.interfaces.AccountService;
import ru.yandex.practicum.bank.account.mappers.AccountMapper;
import ru.yandex.practicum.bank.account.models.AccountModel;
import ru.yandex.practicum.bank.account.repositories.AccountRepository;
import ru.yandex.practicum.bank.account.viewmodels.AccountProfileUpdatedEventViewModel;
import ru.yandex.practicum.bank.account.viewmodels.AccountResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.RecipientResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.UpdateAccountRequestViewModel;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    // endregion

    // region Constructors

    /**
     * <summary>
     * Инициализирует сервис с необходимыми зависимостями.
     * </summary>
     * @param accountRepository Репозиторий для работы с сущностями счетов.
     * @param accountMapper Маппер для преобразования между моделями и DTO.
     * @param clock Часы для получения текущего времени (упрощает тестирование).
     * @param applicationEventPublisher Публикатор событий Spring для рассылки доменных событий.
     **/
    public AccountServiceImpl(
            AccountRepository accountRepository,
            AccountMapper accountMapper,
            Clock clock,
            ApplicationEventPublisher applicationEventPublisher) {

        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
        this.clock = clock;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    // endregion

    // region Public Methods

    /**
     * <summary>
     * Возвращает информацию о текущем счёте пользователя по его логину.
     * </summary>
     * @param login Логин пользователя.
     * @return ViewModel с данными счета {@link AccountResponseViewModel}.
     * @throws AccountNotFoundException Если счет с таким логином не найден.
     **/
    @Override
    @Transactional(readOnly = true)
    public AccountResponseViewModel getCurrentAccount(String login) {
        var response = accountMapper.toResponse(findAccount(login));

        log.info("Account profile loaded status=success source=account-service");

        return response;
    }

    /**
     * <summary>
     * Обновляет профиль пользователя (имя и дату рождения) с проверкой достижения совершеннолетия.
     * Генерирует доменное событие обновления профиля после успешного сохранения.
     * </summary>
     * @param login Логин пользователя.
     * @param request ViewModel с новыми данными профиля.
     * @return ViewModel с обновленными данными счета {@link AccountResponseViewModel}.
     * @throws InvalidBirthdateException Если пользователь несовершеннолетний (младше 18 лет).
     * @throws AccountNotFoundException Если счет с таким логином не найден.
     **/
    @Override
    @Transactional
    public AccountResponseViewModel updateCurrentAccount(String login, UpdateAccountRequestViewModel request) {
        if (!isAdult(request.birthdate())) {
            throw new InvalidBirthdateException();
        }

        var account = findAccount(login);

        account.updateProfile(request.name(), request.birthdate());

        var savedAccount = accountRepository.save(account);

        applicationEventPublisher.publishEvent(new AccountProfileUpdatedEventViewModel(
                UUID.randomUUID(),
                savedAccount.getLogin(),
                Instant.now(clock)
        ));

        log.info("Account profile updated status=success source=account-service");

        return accountMapper.toResponse(savedAccount);
    }

    /**
     * <summary>
     * Возвращает список всех доступных получателей перевода, исключая текущего пользователя.
     * </summary>
     * @param currentLogin Логин текущего пользователя.
     * @return Список ViewModel получателей {@link RecipientResponseViewModel}.
     **/
    @Override
    @Transactional(readOnly = true)
    public List<RecipientResponseViewModel> getRecipients(String currentLogin) {
        return accountRepository.findAllByLoginNot(currentLogin).stream()
                .map(accountMapper::toRecipientResponse)
                .toList();
    }

    // endregion

    // region Private Methods

    /**
     * <summary>
     * Находит счет пользователя в базе данных по логину.
     * </summary>
     * @param login Логин пользователя.
     * @return Найденная сущность {@link AccountModel}.
     * @throws AccountNotFoundException Если счет не существует.
     **/
    private AccountModel findAccount(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }

    /**
     * <summary>
     * Проверяет, исполнилось ли пользователю 18 лет относительно текущего времени.
     * </summary>
     * @param birthdate Дата рождения для проверки.
     * @return {@code true}, если пользователь совершеннолетний, иначе {@code false}.
     **/
    private boolean isAdult(LocalDate birthdate) {
        return !birthdate.plusYears(18).isAfter(LocalDate.now(clock));
    }

    // endregion
}