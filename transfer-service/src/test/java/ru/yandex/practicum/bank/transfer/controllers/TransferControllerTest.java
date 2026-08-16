package ru.yandex.practicum.bank.transfer.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.transfer.exceptions.MissingPreferredUsernameException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;
import ru.yandex.practicum.bank.transfer.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResponseViewModel;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * <summary>
 * Модульные тесты REST-контроллера переводов TransferController.
 * Проверяют обработку HTTP POST-запросов, извлечение логина (preferred_username) из JWT-токена,
 * передачу параметров в TransferService и обработку граничных случаев с отсутствующими авторизационными данными.
 * </summary>
 **/
@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc
public class TransferControllerTest {

    // region Constants

    private static final String TRANSFER_ENDPOINT = "/api/transfer";

    // endregion

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную обработку запроса перевода с корректным JWT-токеном и вызов бизнес-сервиса.
     * </summary>
     **/
    @Test
    public void shouldProcessTransferRequestSuccessfully() throws Exception {
        var expectedResponse = new TransferResponseViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("800.00"),
                "RUB",
                "Transfer completed"
        );

        when(transferService.transfer(eq("dmitry"), any())).thenReturn(expectedResponse);

        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "dmitry"))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderLogin").value("dmitry"))
                .andExpect(jsonPath("$.recipientLogin").value("alexey"))
                .andExpect(jsonPath("$.senderBalance").value("800.00"))
                .andExpect(jsonPath("$.currency").value("RUB"))
                .andExpect(jsonPath("$.message").value("Transfer completed"));

        verify(transferService).transfer(eq("dmitry"), any());
    }

    /**
     * <summary>
     * Проверяет выброс MissingPreferredUsernameException, если в JWT-токене отсутствует claim preferred_username.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenPreferredUsernameIsMissing() throws Exception {
        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("email", "dmitry@example.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferRequest()))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingPreferredUsernameException.class));

        verifyNoInteractions(transferService);
    }

    /**
     * <summary>
     * Проверяет выброс MissingPreferredUsernameException, если claim preferred_username пуст или содержит только пробелы.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenPreferredUsernameIsBlank() throws Exception {
        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "   "))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferRequest()))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingPreferredUsernameException.class));

        verifyNoInteractions(transferService);
    }

    /**
     * <summary>
     * Проверяет отклонение запроса с кодом 400 Bad Request при некорректной или неполной структуре JSON в теле запроса.
     * </summary>
     **/
    @Test
    public void shouldReturnBadRequestWhenRequestBodyIsInvalid() throws Exception {
        var invalidJson = """
                {
                  "recipientLogin": "",
                  "amount": null
                }
                """;

        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "dmitry"))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferService);
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Вспомогательный метод формирования валидного JSON-тела запроса.
     * </summary>
     * <return>
     * @return JSON-строка TransferRequestViewModel.
     * </return>
     **/
    private String validTransferRequest() {
        return """
                {
                  "recipientLogin": "alexey",
                  "amount": "200.00",
                  "currency": "RUB"
                }
                """;
    }

    // endregion
}