package ru.yandex.practicum.bank.cash.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.cash.exceptions.MissingPreferredUsernameException;
import ru.yandex.practicum.bank.cash.exceptions.OperationBlockedException;
import ru.yandex.practicum.bank.cash.interfaces.CashService;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Интеграционные тесты для REST-контроллера CashController.
 * Проверяют корректность обработки HTTP-запросов на пополнение и снятие наличных,
 * передачу ключа идемпотентности, извлечение логина из JWT-токена и валидацию присутствия preferred_username.
 * </summary>
 **/
@SpringBootTest
@AutoConfigureMockMvc
public class CashControllerTest {

    // region Constants

    private static final String DEPOSIT_URL = "/api/cash/deposit";
    private static final String WITHDRAW_URL = "/api/cash/withdraw";
    private static final String TEST_USER = "alexey";
    private static final UUID TEST_IDEMPOTENCY_KEY = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    private static final String VALID_REQUEST_BODY = """
            {
                "amount": "100.00",
                "currency": "RUB"
            }
            """;

    // endregion

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CashService cashService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную обработку запроса на пополнение счета наличностью при наличии корректного JWT-токена
     * и заголовка ключа идемпотентности.
     * </summary>
     **/
    @Test
    public void shouldDepositSuccessfully() throws Exception {
        var expectedResponse = new CashOperationResponseViewModel(
                new BigDecimal("1100.00"),
                "RUB",
                "Счёт успешно пополнен"
        );

        when(cashService.deposit(eq(TEST_USER), any(), eq(TEST_IDEMPOTENCY_KEY))).thenReturn(expectedResponse);

        mockMvc.perform(post(DEPOSIT_URL)
                        .header("Idempotency-Key", TEST_IDEMPOTENCY_KEY.toString())
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", TEST_USER))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CASH_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1100.00))
                .andExpect(jsonPath("$.currency").value("RUB"))
                .andExpect(jsonPath("$.message").value("Счёт успешно пополнен"));

        verify(cashService).deposit(eq(TEST_USER), any(), eq(TEST_IDEMPOTENCY_KEY));
    }

    /**
     * <summary>
     * Проверяет успешную обработку запроса на снятие наличности при наличии корректного JWT-токена
     * и заголовка ключа идемпотентности.
     * </summary>
     **/
    @Test
    public void shouldWithdrawSuccessfully() throws Exception {
        var expectedResponse = new CashOperationResponseViewModel(
                new BigDecimal("900.00"),
                "RUB",
                "Средства успешно сняты"
        );

        when(cashService.withdraw(eq(TEST_USER), any(), eq(TEST_IDEMPOTENCY_KEY))).thenReturn(expectedResponse);

        mockMvc.perform(post(WITHDRAW_URL)
                        .header("Idempotency-Key", TEST_IDEMPOTENCY_KEY.toString())
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", TEST_USER))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CASH_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(900.00))
                .andExpect(jsonPath("$.currency").value("RUB"))
                .andExpect(jsonPath("$.message").value("Средства успешно сняты"));

        verify(cashService).withdraw(eq(TEST_USER), any(), eq(TEST_IDEMPOTENCY_KEY));
    }

    /**
     * <summary>
     * Проверяет выброс исключения MissingPreferredUsernameException, если в JWT-токене отсутствует claim preferred_username.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenPreferredUsernameClaimIsMissing() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL)
                        .header("Idempotency-Key", TEST_IDEMPOTENCY_KEY.toString())
                        .with(jwt()
                                .jwt(token -> token.claims(claims -> claims.remove("preferred_username")))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CASH_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingPreferredUsernameException.class));
    }

    /**
     * <summary>
     * Проверяет выброс исключения MissingPreferredUsernameException, если claim preferred_username пустой или состоит из пробелов.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenPreferredUsernameClaimIsBlank() throws Exception {
        mockMvc.perform(post(WITHDRAW_URL)
                        .header("Idempotency-Key", TEST_IDEMPOTENCY_KEY.toString())
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "   "))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CASH_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingPreferredUsernameException.class));
    }

    /**
     * <summary>
     * Проверяет возврат ошибки 400 Bad Request, если заголовок Idempotency-Key отсутствует.
     * </summary>
     **/
    @Test
    public void shouldReturnBadRequestWhenIdempotencyKeyIsMissing() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", TEST_USER))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CASH_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет возврат ошибки 422 Unprocessable Entity, если операция
     * заблокирована сервисом проверки подозрительных операций.
     * </summary>
     **/
    @Test
    public void shouldReturnUnprocessableEntityWhenOperationIsBlocked() throws Exception {
        var exception = new OperationBlockedException("Подозрительная операция");

        when(cashService.deposit(eq(TEST_USER), any(), eq(TEST_IDEMPOTENCY_KEY)))
                .thenThrow(exception);

        mockMvc.perform(post(DEPOSIT_URL)
                        .header("Idempotency-Key", TEST_IDEMPOTENCY_KEY.toString())
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", TEST_USER))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CASH_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OPERATION_BLOCKED"))
                .andExpect(jsonPath("$.message").value("Подозрительная операция"));

        verify(cashService).deposit(eq(TEST_USER), any(), eq(TEST_IDEMPOTENCY_KEY));
    }

    /**
     * <summary>
     * Проверяет возврат ошибки 422 Unprocessable Entity, если операция снятия
     * заблокирована сервисом проверки подозрительных операций.
     * </summary>
     **/
    @Test
    public void shouldReturnUnprocessableEntityWhenWithdrawOperationIsBlocked() throws Exception {
        var exception = new OperationBlockedException("Снятие средств заблокировано");

        when(cashService.withdraw(eq(TEST_USER), any(), eq(TEST_IDEMPOTENCY_KEY)))
                .thenThrow(exception);

        mockMvc.perform(post(WITHDRAW_URL)
                        .header("Idempotency-Key", TEST_IDEMPOTENCY_KEY.toString())
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", TEST_USER))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CASH_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OPERATION_BLOCKED"))
                .andExpect(jsonPath("$.message").value("Снятие средств заблокировано"));

        verify(cashService).withdraw(eq(TEST_USER), any(), eq(TEST_IDEMPOTENCY_KEY));
    }

    // endregion
}