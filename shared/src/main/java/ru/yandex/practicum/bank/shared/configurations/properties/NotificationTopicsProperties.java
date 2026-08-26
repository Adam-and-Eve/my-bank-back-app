package ru.yandex.practicum.bank.shared.configurations.properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * <summary>
 * Класс конфигурационных свойств для настройки Kafka-топиков, используемых в процессе отправки уведомлений.
 * </summary>
 */
@ConfigurationProperties(prefix = "bank.kafka")
public class NotificationTopicsProperties implements InitializingBean {

    // region Constants

    /**
     * <summary>
     * Количество партиций в топике по умолчанию.
     * </summary>
     */
    public static final int DEFAULT_PARTITION_COUNT = 3;

    /**
     * <summary>
     * Фактор репликации для топика по умолчанию.
     * </summary>
     */
    public static final short DEFAULT_REPLICATION_FACTOR = 1;

    /**
     * <summary>
     * Время хранения сообщений (в миллисекундах) в DLT (Dead Letter Topic) по умолчанию.
     * </summary>
     */
    public static final long DEFAULT_DLT_RETENTION_MS = 604_800_000L;

    // endregion

    // region Fields

    /**
     * <summary>
     * Название основного топика для отправки уведомлений.
     * </summary>
     */
    private String notificationTopic;

    /**
     * <summary>
     * Название DLT-топика для недоставленных или ошибочных уведомлений.
     * </summary>
     */
    private String notificationDltTopic;

    /**
     * <summary>
     * Количество партиций для основного топика уведомлений.
     * </summary>
     */
    private int notificationPartitions = DEFAULT_PARTITION_COUNT;

    /**
     * <summary>
     * Количество партиций для DLT-топика уведомлений.
     * </summary>
     */
    private int notificationDltPartitions = DEFAULT_PARTITION_COUNT;

    /**
     * <summary>
     * Фактор репликации для топиков уведомлений.
     * </summary>
     */
    private short notificationReplicationFactor = DEFAULT_REPLICATION_FACTOR;

    /**
     * <summary>
     * Время хранения сообщений (в миллисекундах) в DLT-топике уведомлений.
     * </summary>
     */
    private long notificationDltRetentionMs = DEFAULT_DLT_RETENTION_MS;

    // endregion

    // region Methods

    /**
     * <summary>
     * Метод жизненного цикла Spring-бина, вызываемый после установки всех свойств. Инициирует валидацию конфигурации.
     * </summary>
     */
    @Override
    public void afterPropertiesSet() {
        validate();
    }

    /**
     * <summary>
     * Проверяет корректность заданных конфигурационных свойств и выбрасывает исключение при невалидных данных.
     * </summary>
     */
    void validate() {
        if (!StringUtils.hasText(notificationTopic)) {
            throw new IllegalStateException("bank.kafka.notification-topic must not be blank");
        }

        if (!StringUtils.hasText(notificationDltTopic)) {
            throw new IllegalStateException("bank.kafka.notification-dlt-topic must not be blank");
        }

        if (notificationPartitions < 1) {
            throw new IllegalStateException("bank.kafka.notification-partitions must be positive");
        }

        if (notificationDltPartitions < 1) {
            throw new IllegalStateException("bank.kafka.notification-dlt-partitions must be positive");
        }

        if (notificationDltPartitions < notificationPartitions) {
            throw new IllegalStateException(
                    "bank.kafka.notification-dlt-partitions must be greater than or equal to "
                            + "bank.kafka.notification-partitions"
            );
        }

        if (notificationReplicationFactor < 1) {
            throw new IllegalStateException("bank.kafka.notification-replication-factor must be positive");
        }

        if (notificationDltRetentionMs < 1) {
            throw new IllegalStateException("bank.kafka.notification-dlt-retention-ms must be positive");
        }
    }

    /**
     * <summary>
     * Получает название основного топика уведомлений.
     * </summary>
     */
    public String getNotificationTopic() {
        return notificationTopic;
    }

    /**
     * <summary>
     * Устанавливает название основного топика уведомлений.
     * </summary>
     */
    public void setNotificationTopic(String notificationTopic) {
        this.notificationTopic = notificationTopic;
    }

    /**
     * <summary>
     * Получает название DLT-топика уведомлений.
     * </summary>
     */
    public String getNotificationDltTopic() {
        return notificationDltTopic;
    }

    /**
     * <summary>
     * Устанавливает название DLT-топика уведомлений.
     * </summary>
     */
    public void setNotificationDltTopic(String notificationDltTopic) {
        this.notificationDltTopic = notificationDltTopic;
    }

    /**
     * <summary>
     * Получает количество партиций для основного топика уведомлений.
     * </summary>
     */
    public int getNotificationPartitions() {
        return notificationPartitions;
    }

    /**
     * <summary>
     * Устанавливает количество партиций для основного топика уведомлений.
     * </summary>
     */
    public void setNotificationPartitions(int notificationPartitions) {
        this.notificationPartitions = notificationPartitions;
    }

    /**
     * <summary>
     * Получает количество партиций для DLT-топика уведомлений.
     * </summary>
     */
    public int getNotificationDltPartitions() {
        return notificationDltPartitions;
    }

    /**
     * <summary>
     * Устанавливает количество партиций для DLT-топика уведомлений.
     * </summary>
     */
    public void setNotificationsDltPartitions(int notificationDltPartitions) {
        this.notificationDltPartitions = notificationDltPartitions;
    }

    /**
     * <summary>
     * Получает фактор репликации для топиков уведомлений.
     * </summary>
     */
    public short getNotificationReplicationFactor() {
        return notificationReplicationFactor;
    }

    /**
     * <summary>
     * Устанавливает фактор репликации для топиков уведомлений.
     * </summary>
     */
    public void setNotificationReplicationFactor(short notificationReplicationFactor) {
        this.notificationReplicationFactor = notificationReplicationFactor;
    }

    /**
     * <summary>
     * Получает время хранения сообщений в DLT-топике уведомлений.
     * </summary>
     */
    public long getNotificationDltRetentionMs() {
        return notificationDltRetentionMs;
    }

    /**
     * <summary>
     * Устанавливает время хранения сообщений в DLT-топике уведомлений.
     * </summary>
     */
    public void setNotificationDltRetentionMs(long notificationDltRetentionMs) {
        this.notificationDltRetentionMs = notificationDltRetentionMs;
    }

    // endregion
}