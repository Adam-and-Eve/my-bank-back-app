package ru.yandex.practicum.bank.notification.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.bank.notification.interfaces.NotificationService;
import ru.yandex.practicum.bank.notification.controllers.NotificationController;
import ru.yandex.practicum.bank.notification.exceptions.NotificationExceptionHandler;

import static org.mockito.Mockito.mock;

/**
 * <summary>
 * Базовый класс для автосгенерированных Spring Cloud Contract тестов модуля notification-service.
 * Подготавливает изолированный тестовый стенд MockMvc и интегрирует RestAssuredMockMvc для выполнения
 * верификационных запросов без поднятия полного контекста Spring Boot.
 * </summary>
 **/
public class NotificationContractBase {

    // region Setup

    /**
     * <summary>
     * Инициализирует мок-объект NotificationService, создает автономный экземпляр MockMvc
     * с подключенным контроллером NotificationController и обработчиком исключений NotificationExceptionHandler,
     * после чего привязывает его к RestAssuredMockMvc перед каждым запуском контрактных тестов.
     * </summary>
     **/
    @BeforeEach
    public void setUp() {
        var notificationService = mock(NotificationService.class);

        var mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new NotificationExceptionHandler())
                .build();

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    // endregion
}