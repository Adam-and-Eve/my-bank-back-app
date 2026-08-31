package ru.yandex.practicum.bank.account.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.account.repositories.OutboxNotificationRepository;
import ru.yandex.practicum.bank.shared.interfaces.NotificationEventPublisher;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * <summary>
 * Фоновый процесс (Relay) для отправки сообщений из Outbox в брокер (Kafka).
 * Работает по расписанию, извлекает PENDING записи и пытается их опубликовать.
 * </summary>
 **/
@Service
public class OutboxRelayService {

    // region Fields

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxNotificationRepository repository;
    private final NotificationEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration retryBackoff;

    // endregion

    // region Constructors

    public OutboxRelayService(
            OutboxNotificationRepository repository,
            NotificationEventPublisher publisher,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${bank.outbox.relay.retry-backoff:5s}") Duration retryBackoff
    ) {
        this.repository = repository;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.retryBackoff = retryBackoff;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Периодически сканирует таблицу Outbox на наличие неотправленных сообщений
     * и синхронно отправляет их через паблишер.
     * </summary>
     */
    @Scheduled(fixedDelayString = "${bank.outbox.relay.delay:2000}")
    @Transactional
    public void processOutbox() {
        var now = LocalDateTime.now(clock);

        var messages = repository.findPendingNotifications(now, 50);

        if (messages.isEmpty()) {
            return;
        }

        for (var message : messages) {
            try {
                var event = objectMapper.readValue(message.getPayload(), NotificationEventModel.class);

                publisher.publish(event);

                message.markAsSent(LocalDateTime.now(clock));

                if (log.isDebugEnabled()) {
                    log.debug("Outbox message sent successfully: id={}", message.getId());
                }
            } catch (Exception exception) {
                log.error("Failed to send outbox message: id={}", message.getId(), exception);

                var nextAttempt = LocalDateTime.now(clock).plus(retryBackoff);

                message.recordFailure(exception.getMessage(), LocalDateTime.now(clock), nextAttempt);
            }
        }

        repository.saveAll(messages);
    }

    // endregion
}