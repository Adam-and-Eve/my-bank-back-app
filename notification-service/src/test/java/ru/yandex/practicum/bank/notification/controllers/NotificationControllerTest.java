package ru.yandex.practicum.bank.notification.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.notification.interfaces.NotificationService;
import ru.yandex.practicum.bank.notification.viewmodels.NotificationRequestViewModel;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Модульные тесты веб-слоя для REST-контроллера NotificationController.
 * Проверяет корректность обработки HTTP-запросов, валидацию входных данных,
 * взаимодействие с NotificationService и формат ответа при ошибках.
 * </summary>
 **/
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class NotificationControllerTest {

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешный приём запроса на отправку уведомления при передаче валидного JSON.
     * Удостоверяется в возврате статуса 202 ACCEPTED и вызове метода сервиса notificationService.notify(...).
     * </summary>
     **/
    @Test
    @DisplayName("Должен принимать запрос на уведомление и возвращать статус 202 ACCEPTED")
    public void shouldAcceptNotification() throws Exception {
        mockMvc.perform(post("/api/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientLogin": "dmitry",
                                  "type": "CASH_DEPOSIT",
                                  "message": "Счёт пополнен на 250.00 RUB",
                                  "operationId": "operation-1"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(notificationService).notify(new NotificationRequestViewModel(
                "dmitry",
                "CASH_DEPOSIT",
                "Счёт пополнен на 250.00 RUB",
                "operation-1"
        ));
    }

    /**
     * <summary>
     * Проверяет работу валидации входящего запроса при наличии пустых обязательных полей.
     * Удостоверяется в возврате статуса 400 BAD_REQUEST и ошибке с кодом VALIDATION_ERROR.
     * </summary>
     **/
    @Test
    @DisplayName("Должен возвращать 400 BAD_REQUEST и код VALIDATION_ERROR при невалидных данных")
    public void shouldReturnValidationError() throws Exception {
        mockMvc.perform(post("/api/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientLogin": "",
                                  "type": "CASH_DEPOSIT",
                                  "message": "Счёт пополнен на 250.00 RUB",
                                  "operationId": "operation-1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // endregion
}