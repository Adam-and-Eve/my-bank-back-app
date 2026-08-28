package ru.yandex.practicum.bank.shared.configurations;

import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import ru.yandex.practicum.bank.shared.configurations.properties.NotificationTopicsProperties;

/**
 * <summary>
 * Конфигурация Spring для программного создания и настройки Kafka-топиков, связанных с уведомлениями.
 * </summary>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationTopicsProperties.class)
public class NotificationTopicsConfiguration {

    // region Beans

    /**
     * <summary>
     * Создает бин основного Kafka-топика для отправки уведомлений на основе заданных свойств конфигурации.
     * </summary>
     * @param properties Свойства конфигурации топиков уведомлений NotificationTopicsProperties.
     * @return Настроенный экземпляр NewTopic для основного топика.
     */
    @Bean
    NewTopic notificationsTopic(NotificationTopicsProperties properties) {
        return TopicBuilder.name(properties.getNotificationTopic())
                .partitions(properties.getNotificationPartitions())
                .replicas(properties.getNotificationReplicationFactor())
                .build();
    }

    /**
     * <summary>
     * Создает бин DLT-топика (Dead Letter Topic) для недоставленных или ошибочных уведомлений
     * с настройкой времени хранения сообщений.
     * </summary>
     * @param properties Свойства конфигурации топиков уведомлений NotificationTopicsProperties.
     * @return Настроенный экземпляр NewTopic для DLT-топика.
     */
    @Bean
    NewTopic notificationsDltTopic(NotificationTopicsProperties properties) {
        return TopicBuilder.name(properties.getNotificationDltTopic())
                .partitions(properties.getNotificationDltPartitions())
                .replicas(properties.getNotificationReplicationFactor())
                .config(TopicConfig.RETENTION_MS_CONFIG, Long.toString(properties.getNotificationDltRetentionMs()))
                .build();
    }

    // endregion
}