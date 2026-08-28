package ru.yandex.practicum.bank.shared.configurations;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.bank.shared.configurations.properties.NotificationTopicsProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Тесты для проверки логики конфигурационного класса NotificationTopicsConfiguration.
 * Гарантирует правильное программное создание Kafka-топиков с нужными параметрами (партиции, реплики, retention).
 * </summary>
 */
public class NotificationTopicsConfigurationTest {

    // region Fields

    private NotificationTopicsConfiguration configuration;

    private NotificationTopicsProperties properties;

    // endregion

    // region Setup

    @BeforeEach
    void setUp() {
        configuration = new NotificationTopicsConfiguration();

        properties = mock(NotificationTopicsProperties.class);
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет корректное создание бина основного топика уведомлений.
     * Убеждается, что имя, количество партиций и фактор репликации берутся из properties.
     * </summary>
     */
    @Test
    void shouldCreateNotificationsTopic() {
        when(properties.getNotificationTopic()).thenReturn("test.bank.notification");

        when(properties.getNotificationPartitions()).thenReturn(3);

        when(properties.getNotificationReplicationFactor()).thenReturn((short) 2);

        NewTopic topic = configuration.notificationsTopic(properties);

        assertThat(topic.name()).isEqualTo("test.bank.notification");

        assertThat(topic.numPartitions()).isEqualTo(3);

        assertThat(topic.replicationFactor()).isEqualTo((short) 2);

        assertThat(topic.configs()).isNullOrEmpty();
    }

    /**
     * <summary>
     * Проверяет корректное создание бина DLT-топика (Dead Letter Topic).
     * Убеждается, что помимо имени, партиций и реплик, также правильно настраивается
     * время хранения сообщений (RETENTION_MS_CONFIG).
     * </summary>
     */
    @Test
    void shouldCreateNotificationsDltTopic() {
        when(properties.getNotificationDltTopic()).thenReturn("test.bank.notification.dlt");

        when(properties.getNotificationDltPartitions()).thenReturn(1);

        when(properties.getNotificationReplicationFactor()).thenReturn((short) 1);

        when(properties.getNotificationDltRetentionMs()).thenReturn(86400000L);

        NewTopic topic = configuration.notificationsDltTopic(properties);

        assertThat(topic.name()).isEqualTo("test.bank.notification.dlt");

        assertThat(topic.numPartitions()).isEqualTo(1);

        assertThat(topic.replicationFactor()).isEqualTo((short) 1);

        assertThat(topic.configs())
                .isNotNull()
                .containsEntry(TopicConfig.RETENTION_MS_CONFIG, "86400000");
    }

    // endregion
}