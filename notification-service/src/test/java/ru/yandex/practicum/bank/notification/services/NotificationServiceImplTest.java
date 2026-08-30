package ru.yandex.practicum.bank.notification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationSourceEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Модульные тесты для реализации сервиса уведомлений NotificationServiceImpl.
 * Проверяют корректную работу логирования входящих событий.
 * </summary>
 **/
@ExtendWith(OutputCaptureExtension.class)
public class NotificationServiceImplTest {

    // region Fields

    private NotificationServiceImpl notificationService;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        notificationService = new NotificationServiceImpl();
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что при передаче события уведомления сервис успешно формирует
     * и записывает в лог (INFO) строку с ключевыми метаданными:
     * eventId, operationId, source и type.
     * </summary>
     **/
    @Test
    public void shouldLogNotificationDetails(CapturedOutput output) {
        var eventId = UUID.randomUUID();

        var operationId = UUID.randomUUID();

        var event = new NotificationEventModel(
                eventId,
                operationId,
                NotificationSourceEnumModel.TRANSFER,
                NotificationTypeEnumModel.TRANSFER_INCOMING,
                "dmitry",
                "Получен перевод",
                Instant.now(),
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB
        );

        notificationService.notify(event);

        assertThat(output.getOut())
                .contains("Notification accepted: eventId=" + eventId)
                .contains("operationId=" + operationId)
                .contains("source=TRANSFER")
                .contains("type=TRANSFER_INCOMING");
    }

    // endregion
}