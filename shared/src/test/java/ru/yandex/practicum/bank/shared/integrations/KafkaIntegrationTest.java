package ru.yandex.practicum.bank.shared.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationSourceEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;
import ru.yandex.practicum.bank.shared.publishers.KafkaNotificationEventPublisher;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Интеграционные тесты для проверки публикации и потребления уведомлений через Kafka.
 * Проверяют отправку сообщений издателем KafkaNotificationEventPublisher и их успешную обработку консьюмером.
 * </summary>
 **/
@SpringBootTest(classes = KafkaIntegrationTest.TestConfiguration.class)
@Testcontainers
class KafkaIntegrationTest {

    private static final String TOPIC = "my.bank.notification";

    @Container
    static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );
        registry.add(
                "bank.kafka.notification-topic",
                () -> TOPIC
        );
    }

    @Autowired
    private KafkaNotificationEventPublisher eventPublisher;

    @Autowired
    private TestKafkaListener testKafkaListener;

    @Test
    void shouldPublishAndConsumeNotificationEventSuccessfully()
            throws InterruptedException {

        var eventId = UUID.randomUUID();
        var operationId = UUID.randomUUID();
        var recipientLogin = "alexey";

        var event = new NotificationEventModel(
                eventId,
                operationId,
                NotificationSourceEnumModel.ACCOUNT,
                NotificationTypeEnumModel.ACCOUNT_UPDATED,
                recipientLogin,
                "Данные профиля обновлены",
                Instant.parse("2026-08-27T12:00:00Z"),
                null,
                null
        );

        eventPublisher.publish(event);

        var consumedMessage = testKafkaListener
                .getMessagesQueue()
                .poll(10, TimeUnit.SECONDS);

        assertThat(consumedMessage).isNotNull();
        assertThat(consumedMessage.eventId()).isEqualTo(eventId);
        assertThat(consumedMessage.operationId()).isEqualTo(operationId);
        assertThat(consumedMessage.recipientLogin()).isEqualTo(recipientLogin);
        assertThat(consumedMessage.source())
                .isEqualTo(NotificationSourceEnumModel.ACCOUNT);
        assertThat(consumedMessage.type())
                .isEqualTo(NotificationTypeEnumModel.ACCOUNT_UPDATED);
    }

    @Configuration
    @EnableKafka
    static class TestConfiguration {

        @Bean
        NewTopic notificationTopic() {
            return TopicBuilder
                    .name(TOPIC)
                    .partitions(3)
                    .replicas(1)
                    .build();
        }

        @Bean
        ProducerFactory<String, NotificationEventModel> producerFactory() {
            Map<String, Object> properties = new HashMap<>();

            properties.put(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    kafka.getBootstrapServers()
            );
            properties.put(
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class
            );
            properties.put(
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    JsonSerializer.class
            );

            return new DefaultKafkaProducerFactory<>(properties);
        }

        @Bean
        KafkaTemplate<String, NotificationEventModel> kafkaTemplate(
                ProducerFactory<String, NotificationEventModel> producerFactory
        ) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        ConsumerFactory<String, NotificationEventModel> consumerFactory(
                ObjectMapper objectMapper
        ) {
            JsonDeserializer<NotificationEventModel> valueDeserializer =
                    new JsonDeserializer<>(
                            NotificationEventModel.class,
                            objectMapper
                    );

            valueDeserializer.setUseTypeHeaders(false);

            Map<String, Object> properties = new HashMap<>();

            properties.put(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    kafka.getBootstrapServers()
            );
            properties.put(
                    ConsumerConfig.GROUP_ID_CONFIG,
                    "test-notification-group"
            );
            properties.put(
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer.class
            );
            properties.put(
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    JsonDeserializer.class
            );
            properties.put(
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                    "earliest"
            );

            return new DefaultKafkaConsumerFactory<>(
                    properties,
                    new StringDeserializer(),
                    valueDeserializer
            );
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, NotificationEventModel>
        kafkaListenerContainerFactory(
                ConsumerFactory<String, NotificationEventModel> consumerFactory
        ) {
            var factory =
                    new ConcurrentKafkaListenerContainerFactory<
                            String,
                            NotificationEventModel
                            >();

            factory.setConsumerFactory(consumerFactory);

            return factory;
        }

        @Bean
        TestKafkaListener testKafkaListener() {
            return new TestKafkaListener();
        }

        @Bean
        io.micrometer.core.instrument.MeterRegistry meterRegistry() {
            return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        }

        @Bean
        KafkaNotificationEventPublisher notificationEventPublisher(
                KafkaTemplate<String, NotificationEventModel> kafkaTemplate,
                io.micrometer.core.instrument.MeterRegistry meterRegistry
        ) {
            return new KafkaNotificationEventPublisher(
                    kafkaTemplate,
                    meterRegistry,
                    TOPIC
            );
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    static class TestKafkaListener {

        private final BlockingQueue<NotificationEventModel> messagesQueue =
                new LinkedBlockingQueue<>();

        @KafkaListener(
                topics = TOPIC,
                groupId = "test-notification-group",
                containerFactory = "kafkaListenerContainerFactory"
        )
        void listen(
                @Payload NotificationEventModel payload,
                @Header(KafkaHeaders.RECEIVED_KEY) String key,
                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition
        ) {
            assertThat(key).isEqualTo(payload.recipientLogin());
            messagesQueue.add(payload);
        }

        BlockingQueue<NotificationEventModel> getMessagesQueue() {
            return messagesQueue;
        }
    }
}