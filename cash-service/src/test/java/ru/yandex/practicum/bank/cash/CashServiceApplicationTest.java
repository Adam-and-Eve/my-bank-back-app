package ru.yandex.practicum.bank.cash;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.bank.shared.interfaces.NotificationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Тестовый класс для проверки успешной загрузки Spring-контекста микросервиса кассовых операций (Cash Service).
 * Выполняет проверку наличия ключевых бинов и корректности базовых конфигурационных свойств, в частности для Kafka.
 * </summary>
 */
@SpringBootTest
public class CashServiceApplicationTest {

    // region Fields

    @Autowired
    private Environment environment;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private NotificationEventPublisher notificationEventPublisher;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что контекст приложения успешно поднимается, необходимые бины (например, NotificationEventPublisher)
     * инициализируются, а базовые свойства Kafka загружаются из конфигурации (bootstrap-servers, топик уведомлений).
     * </summary>
     */
    @Test
    void contextLoads() {
        assertThat(environment.getProperty("spring.kafka.bootstrap-servers"))
                .isEqualTo("localhost:9092");
        assertThat(environment.getProperty("bank.kafka.notification-topic"))
                .isEqualTo("bank.notification");
        assertThat(notificationEventPublisher).isNotNull();

        assertProducerPolicy();
    }

    // endregion

    // region Helpers

    /**
     * <summary>
     * Проверяет политики надежности и сериализации Kafka Producer, заданные в конфигурации.
     * Убеждается, что включена идемпотентность, подтверждение от всех реплик (acks=all),
     * настроены таймауты и используются правильные классы-сериализаторы (StringSerializer и JsonSerializer).
     * </summary>
     */
    private void assertProducerPolicy() {
        var properties = kafkaProperties.buildProducerProperties(null);

        assertThat(properties)
                .containsEntry(ProducerConfig.ACKS_CONFIG, "all")
                .containsEntry(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
                .containsEntry(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE)
                .containsEntry(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "120000")
                .containsEntry(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000")
                .containsEntry(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5")
                .containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    }

    // endregion
}