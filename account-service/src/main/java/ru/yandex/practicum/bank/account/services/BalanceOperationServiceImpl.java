package ru.yandex.practicum.bank.account.services;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

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

    private static final Logger log = LoggerFactory.getLogger(BalanceOperationServiceImpl.class);

    // endregion

    // region Constructors

    /**
     * <summary>
     * Инициализирует сервис балансовых операций.
     * </summary>
     * @param accountRepository Репозиторий для работы с банковскими счетами.
     **/
    public BalanceOperationServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // endregion

    // region Public Methods

    /**
     * <summary>
     * Выполняет операцию пополнения баланса счета.
     * Открывает новую изолированную транзакцию (REQUIRES_NEW).
     * </summary>
     * @param request Данные запроса на пополнение (логин, сумма, валюта).
     * @return Обновленное состояние баланса счета.
     **/
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BalanceResponseViewModel deposit(BalanceOperationRequestViewModel request) {
        Objects.requireNonNull(request, "Deposit request must not be null");

        validateAmount(request.amount(), request.operationId(), "DEPOSIT", request.currency().name());

        var account = findAccount(request.login());

        validateCurrency(account, request.currency(), request.operationId(), "DEPOSIT");

        account.setBalance(account.getBalance().add(request.amount()));

        return toBalanceResponse(accountRepository.save(account));
    }

    /**
     * <summary>
     * Выполняет операцию снятия средств с баланса счета.
     * Открывает новую изолированную транзакцию (REQUIRES_NEW) и проверяет наличие достаточных средств.
     * </summary>
     * @param request Данные запроса на снятие (логин, сумма, валюта).
     * @return Обновленное состояние баланса счета.
     **/
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BalanceResponseViewModel withdraw(BalanceOperationRequestViewModel request) {
        Objects.requireNonNull(request, "Withdraw request must not be null");

        validateAmount(request.amount(), request.operationId(), "WITHDRAW", request.currency().name());

        var account = findAccount(request.login());

        validateCurrency(account, request.currency(), request.operationId(), "WITHDRAW");

        withdraw(account, request.amount(), request.operationId(), "WITHDRAW", request.currency().name());

        return toBalanceResponse(accountRepository.save(account));
    }

    /**
     * <summary>
     * Выполняет операцию перевода средств между двумя счетами.
     * Открывает новую транзакцию (REQUIRES_NEW). Проверяет валидность сумм, совпадение валют
     * и предотвращает потенциальные взаимные блокировки базы данных (Deadlocks) за счет детерминированного сохранения.
     * </summary>
     * @param request Данные перевода (отправитель, получатель, суммы отправки и зачисления, валюты).
     * @return Детали выполненного перевода (баланс отправителя).
     * @throws SelfTransferForbiddenException Если логин отправителя совпадает с получателем.
     * @throws RecipientNotFoundException Если счет получателя не найден в базе.
     **/
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransferBalanceResponseViewModel transfer(TransferBalanceRequestViewModel request) {
        Objects.requireNonNull(request, "Transfer request must not be null");

        if (request.senderLogin().equals(request.recipientLogin())) {
            log.warn(
                    "Balance operation rejected operationId={} operationType=TRANSFER currency={} status=rejected errorCode=SELF_TRANSFER_FORBIDDEN source=account-service",
                    request.operationId(),
                    request.currency()
            );

            throw new SelfTransferForbiddenException();
        }

        validateAmount(request.amount(), request.operationId(), "TRANSFER", request.currency().name());

        validateAmount(
                request.resolvedRecipientAmount(),
                request.operationId(),
                "TRANSFER",
                request.resolvedRecipientCurrency().name()
        );

        var sender = findAccount(request.senderLogin());

        var recipient = accountRepository.findByLogin(request.recipientLogin())
                .orElseThrow(() -> new RecipientNotFoundException(request.recipientLogin()));

        validateCurrency(sender, request.currency(), request.operationId(), "TRANSFER");

        validateCurrency(recipient, request.resolvedRecipientCurrency(), request.operationId(), "TRANSFER");

        withdraw(sender, request.amount(), request.operationId(), "TRANSFER", request.currency().name());

        recipient.setBalance(recipient.getBalance().add(request.resolvedRecipientAmount()));

        saveInDeterministicOrder(sender, recipient);

        return new TransferBalanceResponseViewModel(
                sender.getLogin(),
                recipient.getLogin(),
                sender.getBalance(),
                sender.getCurrency().name()
        );
    }

    // endregion

    // region Private Methods

    /**
     * <summary>
     * Проверяет, что запрашиваемая сумма строго больше нуля и имеет допустимую точность (не более 2 знаков после запятой).
     * </summary>
     * @param amount Сумма для валидации.
     * @param operationId Уникальный идентификатор операции для логирования.
     * @param operationType Тип операции (DEPOSIT, WITHDRAW, TRANSFER).
     * @param currency Текстовое представление валюты.
     * @throws InvalidAmountException Если сумма меньше или равна нулю.
     * @throws InvalidAmountScaleException Если точность суммы превышает 2 знака.
     **/
    private void validateAmount(BigDecimal amount, String operationId, String operationType, String currency) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn(
                    "Balance operation rejected operationId={} operationType={} currency={} status=rejected errorCode=INVALID_AMOUNT source=account-service",
                    operationId,
                    operationType,
                    currency
            );

            throw new InvalidAmountException();
        }
        if (amount.scale() > 2) {
            log.warn(
                    "Balance operation rejected operationId={} operationType={} currency={} status=rejected errorCode=INVALID_AMOUNT_SCALE source=account-service",
                    operationId,
                    operationType,
                    currency
            );

            throw new InvalidAmountScaleException();
        }
    }

    /**
     * <summary>
     * Проверяет совпадение заявленной валюты операции с фактической валютой счета.
     * </summary>
     * @param account Модель банковского счета пользователя.
     * @param operationCurrency Валюта, в которой была запрошена операция.
     * @param operationId Уникальный идентификатор операции.
     * @param operationType Тип операции.
     * @throws CurrencyMismatchException Если валюты не совпадают.
     **/
    private void validateCurrency(AccountModel account, CurrencyEnumModel operationCurrency, String operationId, String operationType) {
        if (account.getCurrency() != operationCurrency) {
            log.warn(
                    "Balance operation rejected operationId={} operationType={} currency={} status=rejected errorCode=CURRENCY_MISMATCH source=account-service",
                    operationId,
                    operationType,
                    operationCurrency.name()
            );

            throw new CurrencyMismatchException();
        }
    }

    /**
     * <summary>
     * Внутренняя реализация списания средств, проверяющая, достаточно ли денег на балансе.
     * </summary>
     * @param account Модель счета для списания.
     * @param amount Сумма списания.
     * @param operationId Идентификатор операции.
     * @param operationType Тип операции.
     * @param currency Валюта операции.
     * @throws InsufficientFundsException Если баланс меньше запрашиваемой суммы.
     **/
    private void withdraw(AccountModel account, BigDecimal amount, String operationId, String operationType, String currency) {
        if (account.getBalance().compareTo(amount) < 0) {
            log.warn(
                    "Balance operation rejected operationId={} operationType={} currency={} status=rejected errorCode=INSUFFICIENT_FUNDS source=account-service",
                    operationId,
                    operationType,
                    currency
            );

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
     **/
    private AccountModel findAccount(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }

    /**
     * <summary>
     * Сохраняет две модели аккаунтов в детерминированном порядке по их ID для предотвращения
     * потенциальных взаимных блокировок (Deadlocks) на уровне базы данных.
     * Гарантирует, что транзакции всегда захватывают строки БД в одинаковой последовательности.
     * </summary>
     * @param first Первая модель аккаунта.
     * @param second Вторая модель аккаунта.
     **/
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
     **/
    private BalanceResponseViewModel toBalanceResponse(AccountModel account) {
        return new BalanceResponseViewModel(
                account.getLogin(),
                account.getBalance(),
                account.getCurrency().name()
        );
    }

    // endregion
}