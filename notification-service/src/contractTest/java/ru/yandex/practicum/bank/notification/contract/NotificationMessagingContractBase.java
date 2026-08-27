package ru.yandex.practicum.bank.notification.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;
import ru.yandex.practicum.bank.notification.listeners.NotificationEventListener;
import ru.yandex.practicum.bank.notification.services.NotificationServiceImpl;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * <summary>
 * Базовый класс для автосгенерированных Spring Cloud Contract тестов асинхронного взаимодействия (messaging)
 * в модуле notification-service. Настраивает окружение для проверки потребления
 * и отправки сообщений через брокер с использованием каналов Spring Integration.
 * </summary>
 **/
@SpringBootTest(classes = NotificationMessagingContractBase.MessagingConfiguration.class)
@AutoConfigureMessageVerifier
public abstract class NotificationMessagingContractBase {

    // region Constants

    private static final String EVENT_JSON = """
            {
              "eventId": "33333333-3333-3333-3333-333333333333",
              "operationId": "44444444-4444-4444-4444-444444444444",
              "source": "CASH",
              "type": "CASH_DEPOSITED",
              "recipientLogin": "alexey",
              "message": "Счёт пополнен на 100.00 RUB",
              "occurredAt": "2026-08-27T05:01:00Z",
              "amount": "100.00",
              "currency": "RUB"
            }
            """;

    // endregion

    // region Fields

    @Autowired
    @Qualifier("bank.notification")
    private MessageChannel notificationsChannel;

    // endregion

    // region Methods

    /**
     * <summary>
     * Метод-триггер для Spring Cloud Contract, симулирующий входящее событие пополнения счета (CASH_DEPOSITED).
     * Выполняет десериализацию JSON, запуск валидации и слушателя, а также отправку сообщения
     * в тестовый канал Spring Integration для верификации контракта.
     * </summary>
     * @throws Exception Если произошла ошибка десериализации или отправки сообщения в канал.
     **/
    public void notificationReceived() throws Exception {
        var objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        var event = objectMapper.readValue(EVENT_JSON, NotificationEventModel.class);

        var notificationService = mock(NotificationServiceImpl.class);

        var listener = new NotificationEventListener(
                notificationService,
                Validation.buildDefaultValidatorFactory().getValidator()
        );

        listener.listen(new ConsumerRecord<>(
                "bank.notification",
                0,
                0L,
                event.recipientLogin(),
                event
        ));

        verify(notificationService).notify(event);

        var payload = objectMapper.readValue(EVENT_JSON, new TypeReference<>() {});

        notificationsChannel.send(MessageBuilder.withPayload(payload)
                .setHeader("kafka_messageKey", event.recipientLogin())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .build());
    }

    // endregion

    // region Configuration

    /**
     * <summary>
     * Внутренний конфигурационный класс для поднятия тестовых каналов Spring Integration,
     * используемых верификатором messaging-контрактов.
     * </summary>
     **/
    @Configuration
    static class MessagingConfiguration {

        @Bean(name = "bank.notification")
        QueueChannel notificationsChannel() {
            return new QueueChannel();
        }
    }

    // endregion
}