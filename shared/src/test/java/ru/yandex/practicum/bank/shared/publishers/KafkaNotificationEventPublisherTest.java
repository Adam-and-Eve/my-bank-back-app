package ru.yandex.practicum.bank.shared.publishers;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationSourceEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Модульные тесты для проверки логики издателя событий KafkaNotificationEventPublisher.
 * Проверяет успешную синхронную отправку, проброс исключений (для Outbox Relay),
 * корректность логирования, отсутствие утечек конфиденциальных данных и сбор метрик.
 * </summary>
 */
@SuppressWarnings("unchecked")
@ExtendWith(OutputCaptureExtension.class)
class KafkaNotificationEventPublisherTest {

    // region Constants

    private static final String TOPIC = "bank.notification";

    // endregion

    // region Fields

    private final KafkaTemplate<String, NotificationEventModel> kafkaTemplate = mock(KafkaTemplate.class);

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final KafkaNotificationEventPublisher publisher =
            new KafkaNotificationEventPublisher(kafkaTemplate, meterRegistry, TOPIC);

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную синхронную отправку события.
     * Убеждается, что логин пользователя используется в качестве ключа маршрутизации Kafka,
     * а исключений не возникает при успешном .get().
     * </summary>
     */
    @Test
    void shouldSendEventWithRecipientLoginAsMessageKey() {
        var event = event();

        var producerRecord = new ProducerRecord<>(TOPIC, event.recipientLogin(), event);

        var metadata = new RecordMetadata(new TopicPartition(TOPIC, 1), 0, 0, 0, 0, 0);

        var future = CompletableFuture.completedFuture(new SendResult<>(producerRecord, metadata));

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenReturn(future);

        publisher.publish(event);

        verify(kafkaTemplate).send(TOPIC, event.recipientLogin(), event);
    }

    /**
     * <summary>
     * Убеждается, что при логировании ошибки не утекают конфиденциальные данные (тело исключения),
     * а пишется только имя класса исключения. Исключение пробрасывается дальше для Relay.
     * </summary>
     */
    @Test
    void shouldNotLogSensitiveFailureMessage(CapturedOutput output) {
        var event = event();

        var future = new CompletableFuture<SendResult<String, NotificationEventModel>>();

        future.completeExceptionally(new IllegalStateException("Authorization: Bearer secret.jwt.value"));

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenReturn(future);

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class);

        assertThat(output)
                .contains("errorType=IllegalStateException")
                .doesNotContain("secret.jwt.value")
                .doesNotContain("Authorization: Bearer");
    }

    /**
     * <summary>
     * Проверяет, что ошибка org.apache.kafka.common.errors.TimeoutException
     * корректно перехватывается, метрика получает тег "timeout", а ошибка пробрасывается наверх.
     * </summary>
     */
    @Test
    void shouldClassifyTimeoutExceptionCorrectly() {
        var event = event();

        var future = new CompletableFuture<SendResult<String, NotificationEventModel>>();

        future.completeExceptionally(new TimeoutException("Broker connection timeout"));

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenReturn(future);

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(TimeoutException.class);

        assertThat(failureCount("timeout")).isEqualTo(1.0);

        assertThat(failureCount("other")).isEqualTo(0.0);
    }

    /**
     * <summary>
     * Проверяет классификацию ошибки сериализации (org.apache.kafka.common.errors.SerializationException).
     * Метрика должна получить тег "serialization".
     * </summary>
     */
    @Test
    void shouldClassifySerializationExceptionCorrectly() {
        var event = event();

        var future = new CompletableFuture<SendResult<String, NotificationEventModel>>();

        future.completeExceptionally(new SerializationException("Cannot serialize JSON"));

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenReturn(future);

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(SerializationException.class);

        assertThat(failureCount("serialization")).isEqualTo(1.0);
    }

    /**
     * <summary>
     * Проверяет классификацию ошибки авторизации (org.apache.kafka.common.errors.SaslAuthenticationException).
     * Метрика должна получить тег "security".
     * </summary>
     */
    @Test
    void shouldClassifySecurityExceptionCorrectly() {
        var event = event();

        var future = new CompletableFuture<SendResult<String, NotificationEventModel>>();

        future.completeExceptionally(new SaslAuthenticationException("SASL authentication failed"));

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenReturn(future);

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(SaslAuthenticationException.class);

        assertThat(failureCount("security")).isEqualTo(1.0);
    }

    /**
     * <summary>
     * Проверяет классификацию любой другой ошибки.
     * Метрика должна получить тег "other".
     * </summary>
     */
    @Test
    void shouldClassifyGenericExceptionAsOther() {
        var event = event();

        var future = new CompletableFuture<SendResult<String, NotificationEventModel>>();

        future.completeExceptionally(new IllegalStateException("Kafka unavailable"));

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenReturn(future);

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        assertThat(failureCount("other")).isEqualTo(1.0);
    }

    // endregion

    // region Helpers

    /**
     * <summary>
     * Вспомогательный метод для получения значения счетчика ошибок по конкретной категории.
     * </summary>
     * @param category Категория ошибки (timeout, serialization, security, other)
     * @return Количество зарегистрированных ошибок.
     */
    private double failureCount(String category) {
        var counter = meterRegistry.find("my.bank.kafka.publication.failures")
                .tag("source", NotificationSourceEnumModel.CASH.name())
                .tag("topic", TOPIC)
                .tag("error_category", category)
                .counter();

        return counter != null ? counter.count() : 0.0;
    }

    /**
     * <summary>
     * Вспомогательный метод для создания тестовой модели события уведомления.
     * </summary>
     * @return Сгенерированное событие NotificationEventModel.
     */
    private NotificationEventModel event() {
        return new NotificationEventModel(
                UUID.fromString("10cb8eb2-b488-4f62-b139-a07314cc3ef4"),
                UUID.fromString("3e3a3fec-843e-44e2-bcf5-3bea12845327"),
                NotificationSourceEnumModel.CASH,
                NotificationTypeEnumModel.CASH_DEPOSITED,
                "ivan",
                "Счёт пополнен",
                Instant.parse("2026-06-30T05:00:00Z"),
                new BigDecimal("250.00"),
                CurrencyEnumModel.RUB
        );
    }

    // endregion
}