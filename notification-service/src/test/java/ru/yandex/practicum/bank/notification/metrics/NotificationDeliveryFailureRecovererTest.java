package ru.yandex.practicum.bank.notification.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.support.serializer.DeserializationException;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationSourceEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * <summary>
 * Модульные тесты для рекаверера метрик NotificationDeliveryFailureRecoverer.
 * Проверяют делегирование вызовов, корректное инкрементирование счетчиков в Micrometer
 * и извлечение логина получателя (recipientLogin) из различных типов данных
 * (POJO, String, byte[], DeserializationException).
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class NotificationDeliveryFailureRecovererTest {

    // region Constants

    private static final String TOPIC = "bank.notification";
    private static final int PARTITION = 0;
    private static final long OFFSET = 1L;
    private static final String METRIC_NAME = "bank.notification.delivery.failures";

    // endregion

    // region Fields

    @Mock
    private ConsumerRecordRecoverer delegate;

    private SimpleMeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    private NotificationDeliveryFailureRecoverer recoverer;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();

        recoverer = new NotificationDeliveryFailureRecoverer(
                delegate,
                meterRegistry,
                objectMapper
        );
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешное извлечение логина, если record.value()
     * является десериализованным объектом NotificationEventModel.
     * </summary>
     **/
    @Test
    public void shouldExtractRecipientFromNotificationEventModel() {
        var event = new NotificationEventModel(
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationSourceEnumModel.TRANSFER,
                NotificationTypeEnumModel.TRANSFER_INCOMING,
                "dmitry",
                "Сообщение",
                Instant.now(),
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB
        );
        var record = new ConsumerRecord<Object, Object>(TOPIC, PARTITION, OFFSET, "key", event);

        var exception = new RuntimeException("Test exception");

        recoverer.accept(record, exception);

        verify(delegate).accept(record, exception);

        assertMetricCount("dmitry", 1.0);
    }

    /**
     * <summary>
     * Проверяет извлечение логина получателя из корректного JSON-объекта,
     * представленного в виде массива байт (byte[]).
     * </summary>
     **/
    @Test
    public void shouldExtractRecipientFromJsonByteArray() {
        var jsonBytes = "{\"recipientLogin\": \"dmitry\"}".getBytes(StandardCharsets.UTF_8);

        var record = new ConsumerRecord<Object, Object>(TOPIC, PARTITION, OFFSET, "key", jsonBytes);

        var exception = new RuntimeException("Validation error");

        recoverer.accept(record, exception);

        verify(delegate).accept(record, exception);

        assertMetricCount("dmitry", 1.0);
    }

    /**
     * <summary>
     * Проверяет извлечение логина получателя из корректной JSON-строки.
     * </summary>
     **/
    @Test
    public void shouldExtractRecipientFromJsonString() {
        var jsonString = "{\"recipientLogin\": \"dmitry\"}";

        var record = new ConsumerRecord<Object, Object>(TOPIC, PARTITION, OFFSET, "key", jsonString);

        var exception = new RuntimeException("Some error");

        recoverer.accept(record, exception);

        assertMetricCount("dmitry", 1.0);
    }

    /**
     * <summary>
     * Проверяет, что при ошибке десериализации (когда record.value() не распарсен),
     * логин извлекается из сырых данных, сохраненных внутри DeserializationException.
     * </summary>
     **/
    @Test
    public void shouldExtractRecipientFromNestedDeserializationException() {
        var record = new ConsumerRecord<Object, Object>(TOPIC, PARTITION, OFFSET, "key", new Object());

        var badJsonBytes = "{\"recipientLogin\": \"dmitry\", \"amount\": \"invalid_number\"}".getBytes(StandardCharsets.UTF_8);

        var deserializationException = new DeserializationException(
                "Failed to deserialize",
                badJsonBytes,
                false,
                new RuntimeException()
        );
        var rootException = new RuntimeException("Listener failed", deserializationException);

        recoverer.accept(record, rootException);

        assertMetricCount("dmitry", 1.0);
    }

    /**
     * <summary>
     * Проверяет установку тега recipient_login="unknown", если JSON некорректен
     * и парсинг завершается с IOException.
     * </summary>
     **/
    @Test
    public void shouldFallbackToUnknownWhenJsonIsMalformed() {
        var malformedJsonBytes = "not a json string".getBytes(StandardCharsets.UTF_8);

        var record = new ConsumerRecord<Object, Object>(TOPIC, PARTITION, OFFSET, "key", malformedJsonBytes);

        var exception = new RuntimeException("Error");

        recoverer.accept(record, exception);

        assertMetricCount("unknown", 1.0);
    }

    /**
     * <summary>
     * Проверяет установку тега recipient_login="unknown", если поле recipientLogin
     * отсутствует в валидном JSON или пустое (blank).
     * </summary>
     **/
    @Test
    public void shouldFallbackToUnknownWhenRecipientIsBlankOrMissing() {
        var missingRecipientJson = "{\"amount\": 100.00}".getBytes(StandardCharsets.UTF_8);

        var record = new ConsumerRecord<Object, Object>(TOPIC, PARTITION, OFFSET, "key", missingRecipientJson);

        var exception = new RuntimeException("Error");

        recoverer.accept(record, exception);

        assertMetricCount("unknown", 1.0);
    }

    /**
     * <summary>
     * Проверяет установку тега recipient_login="unknown", если тип значения
     * в записи Kafka неизвестен и в цепочке исключений нет DeserializationException.
     * </summary>
     **/
    @Test
    public void shouldFallbackToUnknownWhenTypeIsUnrecognized() {
        var record = new ConsumerRecord<Object, Object>(TOPIC, PARTITION, OFFSET, "key", 12345);

        var exception = new RuntimeException("Generic error");

        recoverer.accept(record, exception);

        assertMetricCount("unknown", 1.0);
    }

    // endregion

    // region Private Methods

    /**
     * <summary>
     * Вспомогательный метод для проверки значения счетчика метрик с заданным тегом.
     * </summary>
     * @param expectedRecipient Ожидаемое значение тега recipient_login.
     * @param expectedCount Ожидаемое значение счетчика.
     **/
    private void assertMetricCount(String expectedRecipient, double expectedCount) {
        var counter = meterRegistry.find(METRIC_NAME)
                .tag("recipient_login", expectedRecipient)
                .counter();

        assertThat(counter).isNotNull();

        assertThat(counter.count()).isEqualTo(expectedCount);
    }

    // endregion
}