package ru.yandex.practicum.bank.notification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import ru.yandex.practicum.bank.notification.viewmodels.NotificationRequestViewModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * <summary>
 * Модульные тесты для сервиса NotificationServiceImpl.
 * Проверяет выполнение бизнес-логики приёма уведомлений и корректность логирования входящих данных.
 * </summary>
 **/
@ExtendWith(OutputCaptureExtension.class)
public class NotificationServiceImplTest {

    // region Fields

    private NotificationServiceImpl notificationService;

    // endregion

    // region Setup

    /**
     * <summary>
     * Выполняет инициализацию тестируемого сервиса перед каждым тестовым сценарием.
     * </summary>
     **/
    @BeforeEach
    public void setUp() {
        notificationService = new NotificationServiceImpl();
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что метод notify корректно принимает объект запроса и фиксирует все его параметры в логах.
     * </summary>
     * @param output Перехваченный вывод системы логирования для проверки содержащихся сообщений.
     **/
    @Test
    @DisplayName("Должен успешно логировать детали принятого уведомления")
    public void shouldLogNotificationDetails(CapturedOutput output) {
        var request = new NotificationRequestViewModel(
                "dmitry",
                "CASH_DEPOSIT",
                "Счёт пополнен на 250.00 RUB",
                "operation-1"
        );

        assertDoesNotThrow(() -> notificationService.notify(request));

        assertThat(output.getOut())
                .contains("Notification accepted: recipientLogin=dmitry, type=CASH_DEPOSIT, operationId=operation-1, message=Счёт пополнен на 250.00 RUB");
    }

    // endregion
}