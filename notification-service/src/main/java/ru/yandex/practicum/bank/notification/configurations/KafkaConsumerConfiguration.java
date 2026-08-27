package ru.yandex.practicum.bank.notification.configurations;

import jakarta.validation.ConstraintViolationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import ru.yandex.practicum.bank.notification.metrics.NotificationDeliveryFailureRecoverer;
import ru.yandex.practicum.bank.shared.configurations.NotificationTopicsConfiguration;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG;

/**
 * <summary>
 * Конфигурация консьюмера Kafka для сервиса уведомлений (Notification Service).
 * Настраивает глобальный обработчик ошибок (Error Handler), политики повторных попыток (Retry)
 * и механизм маршрутизации проблемных сообщений в Dead Letter Topic (DLT).
 * </summary>
 **/
@Configuration
@Import(NotificationTopicsConfiguration.class)
public class KafkaConsumerConfiguration {

    // region Beans

    /**
     * <summary>
     * Создает фабрику продюсеров для публикации "мертвых" сообщений в DLT.
     * Использует DelegatingByTypeSerializer для корректной сериализации как сырых байтов
     * (при ошибках десериализации), так и типизированных объектов NotificationEventModel
     * (при ошибках бизнес-логики или валидации).
     * </summary>
     **/
    @Bean
    ProducerFactory<Object, Object> dltProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> properties = new HashMap<>();

        properties.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        properties.put(ACKS_CONFIG, "all");

        properties.put(ENABLE_IDEMPOTENCE_CONFIG, true);

        properties.put(DELIVERY_TIMEOUT_MS_CONFIG, 120_000);

        var keySerializer = new DelegatingByTypeSerializer(Map.of(
                byte[].class, new ByteArraySerializer(),
                String.class, new StringSerializer()
        ));

        var valueSerializer = new DelegatingByTypeSerializer(Map.of(
                byte[].class, new ByteArraySerializer(),
                NotificationEventModel.class, new JsonSerializer<>()
        ));

        return new DefaultKafkaProducerFactory<>(properties, keySerializer, valueSerializer);
    }

    /**
     * <summary>
     * Создает шаблон KafkaTemplate для выполнения отправок сообщений в DLT
     * с включенной поддержкой Observability (сбор метрик и трейсинг).
     * </summary>
     **/
    @Bean
    KafkaTemplate<Object, Object> dltKafkaTemplate(ProducerFactory<Object, Object> dltProducerFactory) {
        var kafkaTemplate = new KafkaTemplate<>(dltProducerFactory);

        kafkaTemplate.setObservationEnabled(true);

        return kafkaTemplate;
    }

    /**
     * <summary>
     * Настраивает рекаверер, который перенаправляет окончательно упавшие сообщения в DLT-топик.
     * Сохраняет привязку к исходной партиции и пробрасывает исключение, если отправка в DLT завершилась ошибкой.
     * </summary>
     **/
    @Bean
    DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<Object, Object> dltKafkaTemplate,
            @Value("${bank.kafka.notification-dlt-topic}") String dltTopic
    ) {
        var recoverer = new DeadLetterPublishingRecoverer(
                dltKafkaTemplate,
                (record, exception) -> new TopicPartition(dltTopic, record.partition())
        );

        recoverer.setFailIfSendResultIsError(true);

        return recoverer;
    }

    /**
     * <summary>
     * Регистрирует кастомный рекаверер для консьюмера, который оборачивает базовую логику DLT,
     * добавляя регистрацию инцидентов (метрик отказов) в MeterRegistry перед публикацией.
     * </summary>
     **/
    @Bean
    ConsumerRecordRecoverer notificationDeliveryFailureRecoverer(
            DeadLetterPublishingRecoverer recoverer,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper
    ) {
        return new NotificationDeliveryFailureRecoverer(recoverer, meterRegistry, objectMapper);
    }

    /**
     * <summary>
     * Настраивает глобальный обработчик ошибок консьюмера с фиксированной задержкой (FixedBackOff).
     * Исключает неисправимые ошибки (десериализация, валидация) из цикла повторных попыток,
     * немедленно отправляя их в DLT, а также подтверждает смещение (ack) после успешной отработки рекаверера.
     * </summary>
     **/
    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            @Qualifier("notificationDeliveryFailureRecoverer") ConsumerRecordRecoverer recoverer
    ) {
        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));

        errorHandler.addNotRetryableExceptions(
                DeserializationException.class,
                SerializationException.class,
                ConstraintViolationException.class
        );

        errorHandler.setAckAfterHandle(true);

        errorHandler.setResetStateOnRecoveryFailure(true);

        return errorHandler;
    }

    // endregion
}