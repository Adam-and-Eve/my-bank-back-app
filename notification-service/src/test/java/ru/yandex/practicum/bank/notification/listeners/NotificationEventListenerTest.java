package ru.yandex.practicum.bank.notification.listeners;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.notification.interfaces.NotificationService;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationSourceEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Модульные тесты для слушателя Kafka событий NotificationEventListener.
 * Проверяют корректность взаимодействия консьюмера с JSR-380 валидатором
 * и передачу управления в NotificationService.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class NotificationEventListenerTest {

    // region Constants

    private static final String TOPIC = "bank.notification";
    private static final int PARTITION = 0;
    private static final long OFFSET = 123L;

    // endregion

    // region Fields

    @Mock
    private NotificationService notificationService;

    @Mock
    private Validator validator;

    @InjectMocks
    private NotificationEventListener listener;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        listener = new NotificationEventListener(notificationService, validator);
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что при получении валидного сообщения из Kafka (без нарушений ограничений),
     * слушатель успешно передает его в NotificationService.
     * </summary>
     **/
    @Test
    public void shouldProcessValidEventSuccessfully() {
        var event = createDummyEvent();

        var record = new ConsumerRecord<>(TOPIC, PARTITION, OFFSET, "key", event);

        when(validator.validate(event)).thenReturn(Collections.emptySet());

        listener.listen(record);

        verify(validator).validate(event);

        verify(notificationService).notify(event);
    }

    /**
     * <summary>
     * Проверяет, что при получении невалидного сообщения, слушатель выбрасывает
     * ConstraintViolationException и прерывает обработку (не вызывает NotificationService).
     * </summary>
     **/
    @Test
    @SuppressWarnings("unchecked")
    public void shouldThrowConstraintViolationExceptionWhenEventIsInvalid() {
        var event = createDummyEvent();

        var record = new ConsumerRecord<>(TOPIC, PARTITION, OFFSET, "key", event);

        ConstraintViolation<NotificationEventModel> violation = mock(ConstraintViolation.class);

        when(validator.validate(event)).thenReturn(Set.of(violation));

        assertThatThrownBy(() -> listener.listen(record))
                .isInstanceOf(ConstraintViolationException.class);

        verify(validator).validate(event);

        verifyNoInteractions(notificationService);
    }

    // endregion

    // region Private Methods

    /**
     * <summary>
     * Создает тестовый объект NotificationEventModel со случайными данными.
     * </summary>
     * @return Заполненная модель события уведомления.
     **/
    private NotificationEventModel createDummyEvent() {
        return new NotificationEventModel(
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationSourceEnumModel.TRANSFER,
                NotificationTypeEnumModel.TRANSFER_INCOMING,
                "dmitry",
                "Получен перевод",
                Instant.now(),
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB
        );
    }

    // endregion
}