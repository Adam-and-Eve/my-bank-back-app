package ru.yandex.practicum.bank.account.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import ru.yandex.practicum.bank.account.factories.AccountUpdatedNotificationFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * <summary>
 * Базовый класс для проверки контрактного тестирования сообщений (Spring Cloud Contract)
 * отправки уведомлений об обновлении аккаунта в брокер сообщений.
 * Настраивает тестовый контекст, сериализацию и каналы обмена сообщениями.
 * </summary>
 **/
@SpringBootTest(classes = AccountNotificationMessagingContractBase.MessagingConfiguration.class)
@AutoConfigureMessageVerifier
public abstract class AccountNotificationMessagingContractBase {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final AccountUpdatedNotificationFactory NOTIFICATION_FACTORY =
            new AccountUpdatedNotificationFactory();

    @Autowired
    @Qualifier("bank.notification")
    private MessageChannel notificationsChannel;

    /**
     * <summary>
     * Метод-триггер для контрактных тестов. Генерирует тестовое уведомление об обновлении профиля,
     * сериализует его в JSON и отправляет в канал обмена сообщениями с установкой заголовков (ключ Kafka и Content-Type).
     * </summary>
     * @throws Exception Если возникают ошибки при сериализации или отправке сообщения.
     **/
    public void accountUpdated() throws Exception {
        var event = NOTIFICATION_FACTORY.create(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "alexey",
                Instant.parse("2026-08-27T05:00:00Z")
        );

        var payload = OBJECT_MAPPER.readValue(
                OBJECT_MAPPER.writeValueAsString(event),
                new TypeReference<Object>() {
                }
        );

        notificationsChannel.send(MessageBuilder.withPayload(payload)
                .setHeader("kafka_messageKey", event.recipientLogin())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .build());
    }

    /**
     * <summary>
     * Внутренняя конфигурация Spring для контрактных тестов.
     * Предоставляет очередь сообщений (QueueChannel) для перехвата и проверки отправленных нотификаций.
     * </summary>
     **/
    @Configuration
    static class MessagingConfiguration {

        @Bean(name = "bank.notification")
        QueueChannel notificationsChannel() {
            return new QueueChannel();
        }
    }
}