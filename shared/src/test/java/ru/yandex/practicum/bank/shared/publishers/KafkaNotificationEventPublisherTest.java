package ru.yandex.practicum.bank.shared.publishers;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
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

import javax.naming.AuthenticationException;
import java.io.NotSerializableException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Тесты для проверки логики издателя событий KafkaNotificationEventPublisher.
 * Проверяет успешную отправку, обработку сбоев, корректность логирования и сбор метрик.
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
     * Проверяет успешную отправку события.
     * Убеждается, что логин пользователя используется в качестве ключа маршрутизации Kafka,
     * а шаблон Kafka вызывается с нужными аргументами.
     * </summary>
     */
    @Test
    void shouldSendEventWithRecipientLoginAsMessageKey() {
        var event = event();

        var future = new CompletableFuture<SendResult<String, NotificationEventModel>>();

        var producerRecord = new ProducerRecord<>(TOPIC, event.recipientLogin(), event);

        var metadata = new RecordMetadata(new TopicPartition(TOPIC, 1), 0, 0, 0, 0, 0);

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenReturn(future);

        publisher.publish(event);

        future.complete(new SendResult<>(producerRecord, metadata));

        verify(kafkaTemplate).send(TOPIC, event.recipientLogin(), event);
    }

    /**
     * <summary>
     * Проверяет обработку асинхронной ошибки (например, сбой сети после отправки).
     * Убеждается, что исключение подавляется и регистрируется в метриках (категория "other").
     * </summary>
     */
    @Test
    void shouldNotPropagateAsynchronousSendFailure() {
        var event = event();

        var future = new CompletableFuture<SendResult<String, NotificationEventModel>>();

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenReturn(future);

        assertThatCode(() -> {
            publisher.publish(event);
            future.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        }).doesNotThrowAnyException();

        assertThat(failureCount("other")).isEqualTo(1);
    }

    /**
     * <summary>
     * Проверяет обработку синхронной ошибки (например, ошибка сериализации до отправки).
     * Убеждается, что исключение подавляется и метрика инкрементируется.
     * </summary>
     */
    @Test
    void shouldNotPropagateSynchronousSendFailure() {
        var event = event();

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event))
                .thenThrow(new IllegalStateException("Kafka unavailable"));

        assertThatCode(() -> publisher.publish(event))
                .doesNotThrowAnyException();

        assertThat(failureCount("other")).isEqualTo(1);
    }

    /**
     * <summary>
     * Убеждается, что при логировании ошибки не утекают конфиденциальные данные (тело исключения),
     * а пишется только имя класса исключения.
     * </summary>
     */
    @Test
    void shouldNotLogSensitiveFailureMessage(CapturedOutput output) {
        var event = event();

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event))
                .thenThrow(new IllegalStateException("Authorization: Bearer secret.jwt.value"));

        publisher.publish(event);

        assertThat(output)
                .contains("errorType=IllegalStateException")
                .doesNotContain("secret.jwt.value")
                .doesNotContain("Authorization: Bearer");
    }

    /**
     * <summary>
     * Проверяет, что первопричина ошибки типа Timeout корректно извлекается
     * и метрика классифицируется как "timeout".
     * </summary>
     */
    @Test
    void shouldClassifyTimeoutExceptionCorrectly() {
        var event = event();

        var rootCause = new TimeoutException("Connection timed out");

        var exception = new RuntimeException("Wrapper", rootCause);

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenThrow(exception);

        publisher.publish(event);

        assertThat(failureCount("timeout")).isEqualTo(1);

        assertThat(failureCount("other")).isEqualTo(0);
    }

    /**
     * <summary>
     * Проверяет классификацию ошибки сериализации.
     * Метрика должна получить тег "serialization".
     * </summary>
     */
    @Test
    void shouldClassifySerializationExceptionCorrectly() {
        var event = event();

        var exception = new RuntimeException(new NotSerializableException("Cannot serialize object"));

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenThrow(exception);

        publisher.publish(event);

        assertThat(failureCount("serialization")).isEqualTo(1);
    }

    /**
     * <summary>
     * Проверяет классификацию ошибки авторизации/аутентификации.
     * Метрика должна получить тег "security".
     * </summary>
     */
    @Test
    void shouldClassifySecurityExceptionCorrectly() {
        var event = event();

        var exception = new RuntimeException(new AuthenticationException("Invalid credentials"));

        when(kafkaTemplate.send(TOPIC, event.recipientLogin(), event)).thenThrow(exception);

        publisher.publish(event);

        assertThat(failureCount("security")).isEqualTo(1);
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
        var counter = meterRegistry.find("bank.kafka.publication.failures")
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