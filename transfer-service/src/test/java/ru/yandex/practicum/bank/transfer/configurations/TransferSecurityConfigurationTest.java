package ru.yandex.practicum.bank.transfer.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.transfer.exceptions.MissingPreferredUsernameException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResponseViewModel;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Интеграционные тесты конфигурации безопасности Transfer Service.
 * Проверяют корректность настройки прав доступа (RBAC),
 * а также кастомную обработку ошибок 401 Unauthorized и 403 Forbidden.
 * </summary>
 **/
@SpringBootTest
@AutoConfigureMockMvc
public class TransferSecurityConfigurationTest {

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет анонимный доступ без JWT-токена к эндпоинту проверки состояния Actuator (/actuator/health).
     * </summary>
     **/
    @Test
    public void shouldAllowHealthWithoutJwt() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    /**
     * <summary>
     * Проверяет отклонение запроса на перевод (401 Unauthorized) при отсутствии JWT-токена.
     * </summary>
     **/
    @Test
    public void shouldRejectTransferEndpointWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/transfer")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authorization is required"));
    }

    /**
     * <summary>
     * Проверяет отклонение запроса на перевод (403 Forbidden) при отсутствии требуемой роли ROLE_TRANSFER_WRITE.
     * </summary>
     **/
    @Test
    public void shouldRejectTransferEndpointWithoutWriteRole() throws Exception {
        mockMvc.perform(post("/api/transfer")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Not enough permissions"));
    }

    /**
     * <summary>
     * Проверяет успешную обработку перевода (200 OK) при наличии всех необходимых ролей доступа.
     * </summary>
     **/
    @Test
    public void shouldAllowTransferWithRequiredRoles() throws Exception {
        when(transferService.transfer(any(), any(), any())).thenReturn(new TransferResponseViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("800.00"),
                "RUB",
                "Transfer completed"
        ));

        mockMvc.perform(post("/api/transfer")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderBalance").value("800.00"));
    }

    /**
     * <summary>
     * Проверяет генерацию ответа 401 Unauthorized при отсутствии обязательного claim preferred_username.
     * </summary>
     **/
    @Test
    public void shouldRejectTransferEndpointWhenPreferredUsernameIsMissing() throws Exception {
        when(transferService.transfer(any(), any(), any()))
                .thenThrow(new MissingPreferredUsernameException());

        mockMvc.perform(post("/api/transfer")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .with(jwt()
                                .jwt(token -> token.claim("email", "dmitry@example.com"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_TRANSFER_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Вспомогательный метод формирования валидного JSON-тела запроса перевода.
     * </summary>
     * @return JSON-строка запроса TransferRequestViewModel.
     **/
    private String transferRequest() {
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