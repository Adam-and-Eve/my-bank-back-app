package ru.yandex.practicum.bank.account.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.account.exceptions.*;
import ru.yandex.practicum.bank.account.interfaces.BalanceOperationService;
import ru.yandex.practicum.bank.account.models.AccountModel;
import ru.yandex.practicum.bank.account.models.OutboxNotificationModel;
import ru.yandex.practicum.bank.account.repositories.AccountRepository;
import ru.yandex.practicum.bank.account.repositories.OutboxNotificationRepository;
import ru.yandex.practicum.bank.account.viewmodels.BalanceOperationRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.BalanceResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceRequestViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceResponseViewModel;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * <summary>
 * Реализация сервиса проведения балансовых операций и денежных переводов (BalanceOperationServiceImpl).
 * Обеспечивает выполнение пополнения, снятия и междусчётных переводов с обработкой оптимистичных блокировок
 * и атомарным сохранением уведомлений через паттерн Transactional Outbox.
 * </summary>
 **/
@Service
public class BalanceOperationServiceImpl implements BalanceOperationService {

    // region Fields

    private final AccountRepository accountRepository;
    private final OutboxNotificationRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private static final Logger log = LoggerFactory.getLogger(BalanceOperationServiceImpl.class);

    // endregion

    // region Constructors

    /**
     * <summary>
     * Инициализирует сервис балансовых операций.
     * </summary>
     * @param accountRepository Репозиторий для работы с банковскими счетами.
     * @param outboxRepository Репозиторий для сохранения событий уведомлений (Outbox).
     * @param objectMapper Маппер для сериализации событий в JSON.
     * @param clock Часы для получения текущего времени.
     **/
    public BalanceOperationServiceImpl(
            AccountRepository accountRepository,
            OutboxNotificationRepository outboxRepository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    // endregion

    // region Public Methods

    /**
     * <summary>
     * Выполняет операцию пополнения баланса счета.
     * Открывает новую изолированную транзакцию (REQUIRES_NEW).
     * </summary>
     * @param request Данные запроса на пополнение (логин, сумма, валюта, уведомления).
     * @return Обновленное состояние баланса счета.
     **/
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BalanceResponseViewModel deposit(BalanceOperationRequestViewModel request) {
        Objects.requireNonNull(request, "Deposit request must not be null");

        validateAmount(request.amount(), request.operationId(), "DEPOSIT", request.currency().name());

        var account = findAccount(request.login());

        validateCurrency(account, request.currency(), request.operationId(), "DEPOSIT");

        account.setBalance(account.getBalance().add(request.amount()));

        var savedAccount = accountRepository.save(account);

        saveNotificationsToOutbox(request.notifications(), request.operationId());

        return toBalanceResponse(savedAccount);
    }

    /**
     * <summary>
     * Выполняет операцию снятия средств с баланса счета.
     * Открывает новую изолированную транзакцию (REQUIRES_NEW) и проверяет наличие достаточных средств.
     * </summary>
     * @param request Данные запроса на снятие (логин, сумма, валюта, уведомления).
     * @return Обновленное состояние баланса счета.
     **/
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BalanceResponseViewModel withdraw(BalanceOperationRequestViewModel request) {
        Objects.requireNonNull(request, "Withdraw request must not be null");

        validateAmount(request.amount(), request.operationId(), "WITHDRAW", request.currency().name());

        var account = findAccount(request.login());

        validateCurrency(account, request.currency(), request.operationId(), "WITHDRAW");

        withdraw(account, request.amount(), request.operationId(), "WITHDRAW", request.currency().name());

        var savedAccount = accountRepository.save(account);

        saveNotificationsToOutbox(request.notifications(), request.operationId());

        return toBalanceResponse(savedAccount);
    }

    /**
     * <summary>
     * Выполняет операцию перевода средств между двумя счетами.
     * Открывает новую транзакцию (REQUIRES_NEW). Проверяет валидность сумм, совпадение валют
     * и предотвращает потенциальные взаимные блокировки базы данных (Deadlocks) за счет детерминированного сохранения.
     * </summary>
     * @param request Данные перевода (отправитель, получатель, суммы отправки и зачисления, валюты, уведомления).
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

        saveNotificationsToOutbox(request.notifications(), request.operationId());

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

    /**
     * <summary>
     * Сериализует и атомарно сохраняет модели уведомлений в таблицу Outbox.
     * В случае ошибки сериализации выбрасывает RuntimeException для отката транзакции.
     * </summary>
     * @param notifications Список событий уведомлений, которые нужно отправить.
     * @param operationId Уникальный идентификатор операции.
     **/
    private void saveNotificationsToOutbox(List<NotificationEventModel> notifications, String operationId) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        var now = LocalDateTime.now(clock);

        var outboxEntities = notifications.stream()
                .map(event -> {
                    try {
                        return new OutboxNotificationModel(
                                UUID.randomUUID(),
                                event.eventId(),
                                operationId,
                                objectMapper.writeValueAsString(event),
                                now
                        );
                    } catch (JsonProcessingException exception) {
                        log.error("Failed to serialize notification eventId={}", event.eventId(), exception);

                        throw new RuntimeException("Outbox serialization failed", exception);
                    }
                })
                .toList();

        outboxRepository.saveAll(outboxEntities);
    }

    // endregion
}