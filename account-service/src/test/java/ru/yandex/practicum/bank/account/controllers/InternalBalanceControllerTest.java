package ru.yandex.practicum.bank.account.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.account.interfaces.BalanceService;
import ru.yandex.practicum.bank.account.viewmodels.BalanceResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.TransferBalanceResponseViewModel;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Интеграционные тесты для внутреннего REST-контроллера InternalBalanceController.
 * Проверяют обработку межсервисных эндпоинтов пополнения, списания и перевода денежных средств,
 * валидацию входных DTO запросов и делегирование вызовов в {@link BalanceService}.
 * </summary>
 **/
@SpringBootTest
@AutoConfigureMockMvc
public class InternalBalanceControllerTest {

    // region Constants

    private static final String DEPOSIT_URL = "/api/account/internal/balance/deposit";

    private static final String WITHDRAW_URL = "/api/account/internal/balance/withdraw";

    private static final String TRANSFER_URL = "/api/account/internal/balance/transfer";

    private static final String VALID_BALANCE_OPERATION_BODY = """
            {
                "operationId": "op-deposit-100",
                "login": "dmitry",
                "amount": "5000.00",
                "currency": "RUB"
            }
            """;

    private static final String INVALID_BALANCE_OPERATION_BODY = """
            {
                "operationId": null,
                "login": "",
                "amount": null,
                "currency": null
            }
            """;

    private static final String VALID_TRANSFER_BODY = """
        {
            "operationId": "op-transfer-200",
            "senderLogin": "dmitry",
            "recipientLogin": "alexey",
            "amount": "1500.00",
            "currency": "RUB"
        }
        """;

    private static final String INVALID_TRANSFER_BODY = """
        {
            "operationId": "",
            "senderLogin": "",
            "recipientLogin": "",
            "amount": null,
            "currency": null
        }
        """;

    // endregion

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BalanceService balanceService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную обработку операции пополнения счета (HTTP 200 OK).
     * </summary>
     **/
    @Test
    public void shouldDepositSuccessfullyWhenValidRequest() throws Exception {
        var response = new BalanceResponseViewModel(
                "dmitry",
                new BigDecimal("1005000.00"),
                "RUB"
        );

        when(balanceService.deposit(any())).thenReturn(response);

        mockMvc.perform(post(DEPOSIT_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "service-account"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SERVICE"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_INTERNAL")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BALANCE_OPERATION_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("dmitry"))
                .andExpect(jsonPath("$.balance").value(1005000.00))
                .andExpect(jsonPath("$.currency").value("RUB"));

        verify(balanceService).deposit(any());
    }

    /**
     * <summary>
     * Проверяет отклонение запроса на пополнение (HTTP 400 Bad Request) при невалидном теле запроса.
     * </summary>
     **/
    @Test
    public void shouldRejectDepositWhenInvalidRequestBody() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "service-account"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SERVICE"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_INTERNAL")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_BALANCE_OPERATION_BODY))
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет успешную обработку операции списания средств со счета (HTTP 200 OK).
     * </summary>
     **/
    @Test
    public void shouldWithdrawSuccessfullyWhenValidRequest() throws Exception {
        var response = new BalanceResponseViewModel(
                "dmitry",
                new BigDecimal("995000.00"),
                "RUB"
        );

        when(balanceService.withdraw(any())).thenReturn(response);

        mockMvc.perform(post(WITHDRAW_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "service-account"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SERVICE"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_INTERNAL")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BALANCE_OPERATION_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("dmitry"))
                .andExpect(jsonPath("$.balance").value(995000.00))
                .andExpect(jsonPath("$.currency").value("RUB"));

        verify(balanceService).withdraw(any());
    }

    /**
     * <summary>
     * Проверяет отклонение запроса на списание (HTTP 400 Bad Request) при невалидном теле запроса.
     * </summary>
     **/
    @Test
    public void shouldRejectWithdrawWhenInvalidRequestBody() throws Exception {
        mockMvc.perform(post(WITHDRAW_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "service-account"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SERVICE"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_INTERNAL")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_BALANCE_OPERATION_BODY))
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет успешную обработку операции перевода между счетами (HTTP 200 OK).
     * </summary>
     **/
    @Test
    public void shouldTransferSuccessfullyWhenValidRequest() throws Exception {
        var response = new TransferBalanceResponseViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("1500.00"),
                "RUB"
        );

        when(balanceService.transfer(any())).thenReturn(response);

        mockMvc.perform(post(TRANSFER_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "service-account"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SERVICE"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_INTERNAL")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRANSFER_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderLogin").value("dmitry"))
                .andExpect(jsonPath("$.recipientLogin").value("alexey"))
                .andExpect(jsonPath("$.senderBalance").value(1500.00))
                .andExpect(jsonPath("$.currency").value("RUB"));

        verify(balanceService).transfer(any());
    }

    /**
     * <summary>
     * Проверяет отклонение запроса на перевод (HTTP 400 Bad Request) при невалидном теле запроса.
     * </summary>
     **/
    @Test
    public void shouldRejectTransferWhenInvalidRequestBody() throws Exception {
        mockMvc.perform(post(TRANSFER_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "service-account"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SERVICE"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_INTERNAL")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_TRANSFER_BODY))
                .andExpect(status().isBadRequest());
    }

    // endregion
}