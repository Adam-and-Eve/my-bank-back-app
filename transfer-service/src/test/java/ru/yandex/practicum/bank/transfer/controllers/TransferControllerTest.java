package ru.yandex.practicum.bank.transfer.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.transfer.exceptions.MissingPreferredUsernameException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferRequestViewModel;
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
     * Проверяет успешную обработку запроса перевода с корректным JWT-токеном
     * и вызов бизнес-сервиса с логином пользователя и параметрами перевода.
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

        when(transferService.transfer(eq("dmitry"), any(TransferRequestViewModel.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt ->
                                        jwt.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderLogin").value("dmitry"))
                .andExpect(jsonPath("$.recipientLogin").value("alexey"))
                .andExpect(jsonPath("$.senderBalance").value("800.00"))
                .andExpect(jsonPath("$.currency").value("RUB"))
                .andExpect(jsonPath("$.message").value("Transfer completed"));

        var requestCaptor =
                org.mockito.ArgumentCaptor.forClass(TransferRequestViewModel.class);

        verify(transferService).transfer(
                eq("dmitry"),
                requestCaptor.capture()
        );

        var request = requestCaptor.getValue();

        assertThat(request.recipientLogin())
                .isEqualTo("alexey");

        assertThat(request.amount())
                .isEqualByComparingTo("200.00");

        assertThat(request.currency())
                .isEqualTo(CurrencyEnumModel.RUB);

        assertThat(request.targetCurrency())
                .isEqualTo(CurrencyEnumModel.RUB);

        assertThat(request.resolvedTargetCurrency())
                .isEqualTo(CurrencyEnumModel.RUB);
    }

    /**
     * <summary>
     * Проверяет успешную обработку перевода с указанием отдельной целевой валюты.
     * </summary>
     **/
    @Test
    public void shouldPassTargetCurrencyToTransferService() throws Exception {
        var expectedResponse = new TransferResponseViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("800.00"),
                "USD",
                "Transfer completed"
        );

        when(transferService.transfer(eq("dmitry"), any(TransferRequestViewModel.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt ->
                                        jwt.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferRequestWithTargetCurrency()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderLogin").value("dmitry"))
                .andExpect(jsonPath("$.recipientLogin").value("alexey"))
                .andExpect(jsonPath("$.senderBalance").value("800.00"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.message").value("Transfer completed"));

        var requestCaptor =
                org.mockito.ArgumentCaptor.forClass(TransferRequestViewModel.class);

        verify(transferService).transfer(
                eq("dmitry"),
                requestCaptor.capture()
        );

        var request = requestCaptor.getValue();

        assertThat(request.recipientLogin())
                .isEqualTo("alexey");

        assertThat(request.amount())
                .isEqualByComparingTo("200.00");

        assertThat(request.currency())
                .isEqualTo(CurrencyEnumModel.USD);

        assertThat(request.targetCurrency())
                .isEqualTo(CurrencyEnumModel.CNY);

        assertThat(request.resolvedTargetCurrency())
                .isEqualTo(CurrencyEnumModel.CNY);
    }

    /**
     * <summary>
     * Проверяет выброс MissingPreferredUsernameException,
     * если в JWT-токене отсутствует claim preferred_username.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenPreferredUsernameIsMissing() throws Exception {
        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt ->
                                        jwt.claim("email", "dmitry@example.com"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferRequest()))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingPreferredUsernameException.class));

        verifyNoInteractions(transferService);
    }

    /**
     * <summary>
     * Проверяет выброс MissingPreferredUsernameException,
     * если claim preferred_username пуст или содержит только пробелы.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenPreferredUsernameIsBlank() throws Exception {
        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt ->
                                        jwt.claim("preferred_username", "   "))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferRequest()))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingPreferredUsernameException.class));

        verifyNoInteractions(transferService);
    }

    /**
     * <summary>
     * Проверяет отклонение запроса с кодом 400 Bad Request,
     * если обязательные поля TransferRequestViewModel отсутствуют
     * или содержат некорректные значения.
     * </summary>
     **/
    @Test
    public void shouldReturnBadRequestWhenRequestBodyIsInvalid() throws Exception {
        var invalidJson = """
                {
                  "recipientLogin": "",
                  "amount": null,
                  "currency": null
                }
                """;

        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt ->
                                        jwt.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferService);
    }

    /**
     * <summary>
     * Проверяет отклонение запроса с кодом 400 Bad Request,
     * если сумма перевода отсутствует.
     * </summary>
     **/
    @Test
    public void shouldReturnBadRequestWhenAmountIsMissing() throws Exception {
        var invalidJson = """
                {
                  "recipientLogin": "alexey",
                  "currency": "RUB"
                }
                """;

        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt ->
                                        jwt.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferService);
    }

    /**
     * <summary>
     * Проверяет отклонение запроса с кодом 400 Bad Request,
     * если валюта перевода отсутствует.
     * </summary>
     **/
    @Test
    public void shouldReturnBadRequestWhenCurrencyIsMissing() throws Exception {
        var invalidJson = """
                {
                  "recipientLogin": "alexey",
                  "amount": "200.00"
                }
                """;

        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt ->
                                        jwt.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferService);
    }

    /**
     * <summary>
     * Проверяет отклонение запроса с кодом 400 Bad Request,
     * если передана неподдерживаемая валюта.
     * </summary>
     **/
    @Test
    public void shouldReturnBadRequestWhenCurrencyIsInvalid() throws Exception {
        var invalidJson = """
                {
                  "recipientLogin": "alexey",
                  "amount": "200.00",
                  "currency": "EUR"
                }
                """;

        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt ->
                                        jwt.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferService);
    }

    /**
     * <summary>
     * Проверяет, что targetCurrency является необязательным полем
     * и при его отсутствии автоматически устанавливается равной currency.
     * </summary>
     **/
    @Test
    public void shouldUseSourceCurrencyAsTargetCurrencyWhenTargetCurrencyIsMissing()
            throws Exception {

        when(transferService.transfer(eq("dmitry"), any(TransferRequestViewModel.class)))
                .thenReturn(new TransferResponseViewModel(
                        "dmitry",
                        "alexey",
                        new BigDecimal("800.00"),
                        "RUB",
                        "Transfer completed"
                ));

        mockMvc.perform(post(TRANSFER_ENDPOINT)
                        .with(csrf())
                        .with(jwt().jwt(jwt ->
                                        jwt.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferRequest()))
                .andExpect(status().isOk());

        var requestCaptor =
                org.mockito.ArgumentCaptor.forClass(TransferRequestViewModel.class);

        verify(transferService).transfer(
                eq("dmitry"),
                requestCaptor.capture()
        );

        var request = requestCaptor.getValue();

        assertThat(request.currency())
                .isEqualTo(CurrencyEnumModel.RUB);

        assertThat(request.targetCurrency())
                .isEqualTo(CurrencyEnumModel.RUB);

        assertThat(request.resolvedTargetCurrency())
                .isEqualTo(CurrencyEnumModel.RUB);
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Формирует JSON-запрос на перевод в одной валюте.
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

    /**
     * <summary>
     * Формирует JSON-запрос на перевод с отдельной целевой валютой.
     * </summary>
     * <return>
     * @return JSON-строка TransferRequestViewModel с targetCurrency.
     * </return>
     **/
    private String validTransferRequestWithTargetCurrency() {
        return """
                {
                  "recipientLogin": "alexey",
                  "amount": "200.00",
                  "currency": "USD",
                  "targetCurrency": "CNY"
                }
                """;
    }

    // endregion
}