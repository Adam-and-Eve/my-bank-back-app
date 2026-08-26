package ru.yandex.practicum.bank.shared.configurations;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.bank.shared.interfaces.NotificationEventPublisher;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.publishers.KafkaNotificationEventPublisher;

/**
 * <summary>
 * Конфигурация Spring для настройки и создания компонентов (продюсеров), отвечающих за публикацию событий уведомлений.
 * </summary>
 */
@Configuration(proxyBeanMethods = false)
@Import(NotificationTopicsConfiguration.class)
public class NotificationProducerConfiguration {

    // region Beans

    /**
     * <summary>
     * Создает ленивый (lazy) бин издателя событий уведомлений для отправки сообщений в Kafka.
     * </summary>
     * @param kafkaTemplate Шаблон Kafka для отправки сообщений.
     * @param meterRegistry Реестр метрик Micrometer для мониторинга процесса отправки.
     * @param topic Название Kafka-топика, в который будут отправляться уведомления.
     * @return Экземпляр NotificationEventPublisher (реализация KafkaNotificationEventPublisher).
     */
    @Bean
    @Lazy
    NotificationEventPublisher notificationEventPublisher(
            KafkaTemplate<String, NotificationEventModel> kafkaTemplate,
            MeterRegistry meterRegistry,
            @Value("${bank.kafka.notification-topic}") String topic
    ) {
        return new KafkaNotificationEventPublisher(kafkaTemplate, meterRegistry, topic);
    }

    // endregion
}