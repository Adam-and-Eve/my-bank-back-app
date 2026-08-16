package ru.yandex.practicum.bank.account.services;

import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.account.exceptions.*;
import ru.yandex.practicum.bank.account.interfaces.BalanceOperationService;
import ru.yandex.practicum.bank.account.models.AccountModel;
import ru.yandex.practicum.bank.account.repositories.AccountRepository;
import ru.yandex.practicum.bank.account.viewmodels.BalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.BalanceResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceResponseViewModel;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <summary>
 * Реализация сервиса проведения балансовых операций и денежных переводов (BalanceOperationServiceImpl).
 * Обеспечивает выполнение пополнения, снятия и междусчётных переводов с обработкой оптимистичных блокировок.
 * </summary>
 **/
@Service
public class BalanceOperationServiceImpl implements BalanceOperationService {

    // region Fields

    private final AccountRepository accountRepository;

    // endregion

    // region Constructors

    public BalanceOperationServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Выполняет пополнение баланса указанного счёта.
     * В случае конфликта оптимистичной блокировки автоматически повторяет попытку до 3 раз.
     * </summary>
     * @param request ViewModel с параметрами пополнения счета.
     * @return ViewModel с обновленным балансом {@link BalanceResponseViewModel}.
     */
    @Override
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BalanceResponseViewModel deposit(BalanceOperationRequestViewModel request) {
        Objects.requireNonNull(request, "Deposit request must not be null");

        validateAmount(request.amount());

        var account = findAccount(request.login());

        account.setBalance(account.getBalance().add(request.amount()));

        return toBalanceResponse(accountRepository.save(account));
    }

    /**
     * <summary>
     * Выполняет снятие денежных средств со счёта.
     * В случае конфликта оптимистичной блокировки автоматически повторяет попытку до 3 раз.
     * </summary>
     * @param request ViewModel с параметрами снятия средств.
     * @return ViewModel с обновленным балансом {@link BalanceResponseViewModel}.
     * @throws InsufficientFundsException Если на счёте недостаточно средств.
     */
    @Override
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BalanceResponseViewModel withdraw(BalanceOperationRequestViewModel request) {
        Objects.requireNonNull(request, "Withdraw request must not be null");

        validateAmount(request.amount());

        var account = findAccount(request.login());

        withdraw(account, request.amount());

        return toBalanceResponse(accountRepository.save(account));
    }

    /**
     * <summary>
     * Выполняет перевод денежных средств между счетами отправителя и получателя.
     * В случае конфликта оптимистичной блокировки повторит попытку до 3 раз.
     * </summary>
     * @param request ViewModel с параметрами перевода средств.
     * @return ViewModel с результатами перевода {@link TransferBalanceResponseViewModel}.
     * @throws SelfTransferForbiddenException Если логин отправителя и получателя совпадают.
     * @throws RecipientNotFoundException Если счёт получателя не найден.
     * @throws InsufficientFundsException Если у отправителя недостаточно средств.
     */
    @Override
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransferBalanceResponseViewModel transfer(TransferBalanceRequestViewModel request) {
        Objects.requireNonNull(request, "Transfer request must not be null");

        if (request.senderLogin().equals(request.recipientLogin())) {
            throw new SelfTransferForbiddenException();
        }

        validateAmount(request.amount());

        var sender = findAccount(request.senderLogin());

        var recipient = accountRepository.findByLogin(request.recipientLogin())
                .orElseThrow(() -> new RecipientNotFoundException(request.recipientLogin()));

        withdraw(sender, request.amount());

        recipient.setBalance(recipient.getBalance().add(request.amount()));

        saveInDeterministicOrder(sender, recipient);

        return new TransferBalanceResponseViewModel(
                sender.getLogin(),
                recipient.getLogin(),
                sender.getBalance(),
                sender.getCurrency().name()
        );
    }

    /**
     * <summary>
     * Проверяет корректность суммы денежной операции (должна быть не null, > 0 и иметь не более 2 знаков после запятой).
     * </summary>
     * @param amount Сумма для проверки.
     * @throws InvalidAmountException Если сумма null или <= 0.
     * @throws InvalidAmountScaleException Если число десятичных знаков больше 2.
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException();
        }

        if (amount.scale() > 2) {
            throw new InvalidAmountScaleException();
        }
    }

    /**
     * <summary>
     * Снимает указанную сумму с баланса аккаунта с проверкой на достаточность средств.
     * </summary>
     * @param account Модель аккаунта.
     * @param amount Сумма списания.
     * @throws InsufficientFundsException Если средств недостаточно.
     */
    private void withdraw(AccountModel account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        account.setBalance(account.getBalance().subtract(amount));
    }

    /**
     * <summary>
     * Находит счёт пользователя по логину или выбрасывает исключение {@link AccountNotFoundException}.
     * </summary>
     * @param login Логин аккаунта.
     * @return Найденная модель аккаунта {@link AccountModel}.
     */
    private AccountModel findAccount(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }

    /**
     * <summary>
     * Сохраняет две модели аккаунтов в детерминированном порядке по их ID для предотвращения потенциальных взаимных блокировок (Deadlock) на уровне БД.
     * </summary>
     * @param first Первая модель аккаунта.
     * @param second Вторая модель аккаунта.
     */
    private void saveInDeterministicOrder(AccountModel first, AccountModel second) {
        Stream.of(first, second)
                .sorted(Comparator.comparing(AccountModel::getId, Comparator.nullsLast(Long::compareTo)))
                .forEach(accountRepository::save);
    }

    /**
     * <summary>
     * Преобразует модель аккаунта в ViewModel ответа о текущем балансе.
     * </summary>
     * @param account Модель аккаунта.
     * @return ViewModel ответа {@link BalanceResponseViewModel}.
     */
    private BalanceResponseViewModel toBalanceResponse(AccountModel account) {
        return new BalanceResponseViewModel(
                account.getLogin(),
                account.getBalance(),
                account.getCurrency().name()
        );
    }

    // endregion
}