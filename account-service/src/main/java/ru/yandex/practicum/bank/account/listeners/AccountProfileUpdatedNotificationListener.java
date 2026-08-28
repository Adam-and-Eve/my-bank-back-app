package ru.yandex.practicum.bank.account.listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.yandex.practicum.bank.account.factories.AccountUpdatedNotificationFactory;
import ru.yandex.practicum.bank.account.viewmodels.AccountProfileUpdatedEventViewModel;
import ru.yandex.practicum.bank.shared.interfaces.NotificationEventPublisher;

/**
 * <summary>
 * Слушатель событий обновления профиля аккаунта (AccountProfileUpdatedEventViewModel).
 * Перехватывает внутренние события приложения и инициирует отправку уведомлений
 * пользователю через общую систему нотификаций.
 * </summary>
 **/
@Component
public class AccountProfileUpdatedNotificationListener {

    // region Fields

    private final NotificationEventPublisher notificationEventPublisher;
    private final AccountUpdatedNotificationFactory notificationFactory;

    // endregion

    // region Constructors

    /**
     * <summary>
     * Инициализирует слушатель, внедряя зависимости для создания и публикации уведомлений.
     * </summary>
     * @param notificationEventPublisher Сервис (публикатор) для отправки событий в брокер сообщений (Kafka).
     * @param notificationFactory Фабрика для конструирования объекта уведомления из базового события.
     **/
    public AccountProfileUpdatedNotificationListener(
            NotificationEventPublisher notificationEventPublisher,
            AccountUpdatedNotificationFactory notificationFactory
    ) {
        this.notificationEventPublisher = notificationEventPublisher;
        this.notificationFactory = notificationFactory;
    }

    // endregion

    // region Event Listeners

    /**
     * <summary>
     * Обработчик события обновления профиля. Срабатывает строго после успешного
     * завершения (коммита) транзакции базы данных (phase = AFTER_COMMIT),
     * чтобы исключить отправку ложных уведомлений в случае отката изменений.
     * </summary>
     * @param event Модель события, содержащая данные об обновленном профиле и идентификатор операции.
     **/
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileUpdated(AccountProfileUpdatedEventViewModel event) {
        notificationEventPublisher.publish(notificationFactory.create(event));
    }

    // endregion
}