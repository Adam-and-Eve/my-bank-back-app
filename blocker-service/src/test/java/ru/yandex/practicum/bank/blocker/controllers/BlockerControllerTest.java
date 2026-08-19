package ru.yandex.practicum.bank.blocker.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.blocker.interfaces.BlockerService;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckResponseViewModel;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Тесты REST-контроллера сервиса блокировки подозрительных операций.
 * Проверяет обработку HTTP-запросов на проверку банковских операций
 * и валидацию входных данных.
 * </summary>
 */
@WebMvcTest(BlockerController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BlockerControllerTest {

    // region Fields

    /**
     * <summary>
     * MockMvc для выполнения HTTP-запросов к контроллеру.
     * </summary>
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * <summary>
     * ObjectMapper для сериализации моделей запросов в JSON.
     * </summary>
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * <summary>
     * Mock сервиса блокировки операций.
     * </summary>
     */
    @MockitoBean
    private BlockerService blockerService;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную проверку операции, разрешенной сервисом блокировки.
     * </summary>
     */
    @Test
    void checkShouldReturnAllowedOperation() throws Exception {
        var request = new OperationCheckRequestViewModel(
                "operation-123",
                OperationTypeEnumModel.TRANSFER,
                null,
                "sender-login",
                "recipient-login",
                new BigDecimal("1000.00"),
                CurrencyEnumModel.RUB,
                new BigDecimal("1000.00"),
                CurrencyEnumModel.RUB
        );

        var response = new OperationCheckResponseViewModel(
                true,
                null
        );

        when(blockerService.check(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.reason").doesNotExist());

        verify(blockerService).check(any());
    }

    /**
     * <summary>
     * Проверяет успешную проверку операции, заблокированной из-за превышения
     * максимально допустимой суммы.
     * </summary>
     */
    @Test
    void checkShouldReturnBlockedOperation() throws Exception {
        var request = new OperationCheckRequestViewModel(
                "operation-123",
                OperationTypeEnumModel.TRANSFER,
                null,
                "sender-login",
                "recipient-login",
                new BigDecimal("150000.00"),
                CurrencyEnumModel.RUB,
                new BigDecimal("150000.00"),
                CurrencyEnumModel.RUB
        );

        var response = new OperationCheckResponseViewModel(
                false,
                "Operation amount exceeds blocker limit"
        );

        when(blockerService.check(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.reason")
                        .value("Operation amount exceeds blocker limit"));

        verify(blockerService).check(any());
    }

    /**
     * <summary>
     * Проверяет, что запрос без идентификатора операции
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void checkShouldRejectBlankOperationId() throws Exception {
        var json = """
                {
                    "operationId": "",
                    "operationType": "TRANSFER",
                    "login": null,
                    "sender": "sender-login",
                    "recipient": "recipient-login",
                    "amount": 1000.00,
                    "currency": "RUB",
                    "normalizedAmount": 1000.00,
                    "baseCurrency": "RUB"
                }
                """;

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос без типа операции
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void checkShouldRejectNullOperationType() throws Exception {
        var json = """
                {
                    "operationId": "operation-123",
                    "operationType": null,
                    "login": "user-login",
                    "sender": null,
                    "recipient": null,
                    "amount": 1000.00,
                    "currency": "RUB",
                    "normalizedAmount": 1000.00,
                    "baseCurrency": "RUB"
                }
                """;

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос без суммы операции
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void checkShouldRejectNullAmount() throws Exception {
        var json = """
                {
                    "operationId": "operation-123",
                    "operationType": "TRANSFER",
                    "login": null,
                    "sender": "sender-login",
                    "recipient": "recipient-login",
                    "amount": null,
                    "currency": "RUB",
                    "normalizedAmount": 1000.00,
                    "baseCurrency": "RUB"
                }
                """;

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос с неположительной суммой
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void checkShouldRejectNonPositiveAmount() throws Exception {
        var json = """
                {
                    "operationId": "operation-123",
                    "operationType": "TRANSFER",
                    "login": null,
                    "sender": "sender-login",
                    "recipient": "recipient-login",
                    "amount": 0,
                    "currency": "RUB",
                    "normalizedAmount": 1000.00,
                    "baseCurrency": "RUB"
                }
                """;

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос без валюты операции
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void checkShouldRejectNullCurrency() throws Exception {
        var json = """
                {
                    "operationId": "operation-123",
                    "operationType": "TRANSFER",
                    "login": null,
                    "sender": "sender-login",
                    "recipient": "recipient-login",
                    "amount": 1000.00,
                    "currency": null,
                    "normalizedAmount": 1000.00,
                    "baseCurrency": "RUB"
                }
                """;

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос без нормализованной суммы
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void checkShouldRejectNullNormalizedAmount() throws Exception {
        var json = """
                {
                    "operationId": "operation-123",
                    "operationType": "TRANSFER",
                    "login": null,
                    "sender": "sender-login",
                    "recipient": "recipient-login",
                    "amount": 1000.00,
                    "currency": "RUB",
                    "normalizedAmount": null,
                    "baseCurrency": "RUB"
                }
                """;

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос с неположительной нормализованной суммой
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void checkShouldRejectNonPositiveNormalizedAmount() throws Exception {
        var json = """
                {
                    "operationId": "operation-123",
                    "operationType": "TRANSFER",
                    "login": null,
                    "sender": "sender-login",
                    "recipient": "recipient-login",
                    "amount": 1000.00,
                    "currency": "RUB",
                    "normalizedAmount": 0,
                    "baseCurrency": "RUB"
                }
                """;

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос без базовой валюты
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void checkShouldRejectNullBaseCurrency() throws Exception {
        var json = """
                {
                    "operationId": "operation-123",
                    "operationType": "TRANSFER",
                    "login": null,
                    "sender": "sender-login",
                    "recipient": "recipient-login",
                    "amount": 1000.00,
                    "currency": "RUB",
                    "normalizedAmount": 1000.00,
                    "baseCurrency": null
                }
                """;

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос с некорректным значением типа операции
     * завершается ошибкой.
     * </summary>
     */
    @Test
    void checkShouldRejectInvalidOperationType() throws Exception {
        var json = """
                {
                    "operationId": "operation-123",
                    "operationType": "INVALID",
                    "login": "user-login",
                    "sender": null,
                    "recipient": null,
                    "amount": 1000.00,
                    "currency": "RUB",
                    "normalizedAmount": 1000.00,
                    "baseCurrency": "RUB"
                }
                """;

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос с некорректным значением валюты
     * завершается ошибкой.
     * </summary>
     */
    @Test
    void checkShouldRejectInvalidCurrency() throws Exception {
        var json = """
                {
                    "operationId": "operation-123",
                    "operationType": "TRANSFER",
                    "login": null,
                    "sender": "sender-login",
                    "recipient": "recipient-login",
                    "amount": 1000.00,
                    "currency": "EUR",
                    "normalizedAmount": 1000.00,
                    "baseCurrency": "RUB"
                }
                """;

        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос без тела
     * завершается ошибкой.
     * </summary>
     */
    @Test
    void checkShouldRejectMissingRequestBody() throws Exception {
        mockMvc.perform(
                        post("/api/blocker/check")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    // endregion
}