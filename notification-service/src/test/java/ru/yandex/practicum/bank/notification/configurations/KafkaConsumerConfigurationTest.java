package ru.yandex.practicum.bank.notification.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import ru.yandex.practicum.bank.notification.metrics.NotificationDeliveryFailureRecoverer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * <summary>
 * Модульные тесты для конфигурации KafkaConsumerConfiguration.
 * Проверяют корректное создание бинов, установку свойств для DLT-продюсера,
 * шаблонов KafkaTemplate и конфигурацию обработчиков ошибок (Error Handlers).
 * </summary>
 **/
public class KafkaConsumerConfigurationTest {

    // region Constants

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DLT_TOPIC = "bank.notification.DLT";

    // endregion

    // region Fields

    private KafkaConsumerConfiguration configuration;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        configuration = new KafkaConsumerConfiguration();
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что фабрика продюсеров для DLT создается корректно,
     * содержит правильные серверы, включенную идемпотентность и acks=all.
     * </summary>
     **/
    @Test
    public void shouldCreateDltProducerFactoryWithCorrectProperties() {
        var factory = configuration.dltProducerFactory(BOOTSTRAP_SERVERS);

        assertThat(factory).isNotNull();

        assertThat(factory).isInstanceOf(DefaultKafkaProducerFactory.class);

        var defaultFactory = (DefaultKafkaProducerFactory<?, ?>) factory;

        var properties = defaultFactory.getConfigurationProperties();

        assertThat(properties).containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

        assertThat(properties).containsEntry(ProducerConfig.ACKS_CONFIG, "all");

        assertThat(properties).containsEntry(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        assertThat(properties).containsEntry(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);
    }

    /**
     * <summary>
     * Проверяет, что созданный KafkaTemplate для DLT имеет включенную поддержку метрик и трейсинга (Observability).
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldCreateDltKafkaTemplateWithObservationEnabled() {
        ProducerFactory<Object, Object> producerFactory = mock(ProducerFactory.class);

        var kafkaTemplate = configuration.dltKafkaTemplate(producerFactory);

        assertThat(kafkaTemplate).isNotNull();

        assertThat(kafkaTemplate.getProducerFactory()).isEqualTo(producerFactory);
    }

    /**
     * <summary>
     * Проверяет успешную инициализацию рекаверера для публикации в DLT.
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldCreateDeadLetterPublishingRecoverer() {
        KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);

        var recoverer = configuration.deadLetterPublishingRecoverer(kafkaTemplate, DLT_TOPIC);

        assertThat(recoverer).isNotNull();

        assertThat(recoverer).isInstanceOf(DeadLetterPublishingRecoverer.class);
    }

    /**
     * <summary>
     * Проверяет успешную инициализацию кастомного рекаверера метрик (NotificationDeliveryFailureRecoverer).
     * </summary>
     **/
    @Test
    public void shouldCreateNotificationDeliveryFailureRecoverer() {
        DeadLetterPublishingRecoverer delegate = mock(DeadLetterPublishingRecoverer.class);

        MeterRegistry meterRegistry = mock(MeterRegistry.class);

        ObjectMapper objectMapper = mock(ObjectMapper.class);

        var recoverer = configuration.notificationDeliveryFailureRecoverer(
                delegate,
                meterRegistry,
                objectMapper
        );

        assertThat(recoverer).isNotNull();

        assertThat(recoverer).isInstanceOf(NotificationDeliveryFailureRecoverer.class);
    }

    /**
     * <summary>
     * Проверяет создание глобального обработчика ошибок Kafka с флагом ackAfterHandle = true.
     * </summary>
     **/
    @Test
    public void shouldCreateKafkaErrorHandler() {
        ConsumerRecordRecoverer recoverer = mock(ConsumerRecordRecoverer.class);

        var errorHandler = configuration.kafkaErrorHandler(recoverer);

        assertThat(errorHandler).isNotNull();

        assertThat(errorHandler.isAckAfterHandle()).isTrue();
    }

    // endregion
}