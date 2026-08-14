package ru.yandex.practicum.bank.cash.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.cash.interfaces.CashService;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Интеграционные тесты для конфигурации безопасности CashSecurityConfiguration.
 * Проверяют корректность настройки прав доступа (RBAC), извлечение ролей из JWT-токена (realm_access),
 * а также кастомную обработку ошибок 401 Unauthorized и 403 Forbidden.
 * </summary>
 **/
@SpringBootTest
@AutoConfigureMockMvc
public class CashSecurityConfigurationTest {

    // region Constants

    private static final String DEPOSIT_URL = "/api/cash/deposit";
    private static final String WITHDRAW_URL = "/api/cash/withdraw";
    private static final String HEALTH_URL = "/actuator/health";
    private static final String UNKNOWN_URL = "/api/cash/unknown";

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

    @Autowired
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет анонимный доступ без JWT-токена к эндпоинту проверки состояния Actuator (/actuator/health).
     * </summary>
     **/
    @Test
    public void shouldAllowHealthWithoutJwt() throws Exception {
        mockMvc.perform(get(HEALTH_URL))
                .andExpect(status().isOk());
    }

    /**
     * <summary>
     * Проверяет отклонение запроса на пополнение (401 Unauthorized) с кастомным JSON при отсутствии JWT-токена.
     * </summary>
     **/
    @Test
    public void shouldRejectDepositEndpointWithoutJwt() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Требуется авторизация"));
    }

    /**
     * <summary>
     * Проверяет отклонение запроса (403 Forbidden), если у пользователя отсутствует роль ROLE_CASH_WRITE.
     * </summary>
     **/
    @Test
    public void shouldRejectDepositEndpointWithoutWriteRole() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "alexey"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Недостаточно прав для выполнения операции"));
    }

    /**
     * <summary>
     * Проверяет отклонение запроса (403 Forbidden), если у пользователя отсутствует роль ROLE_USER.
     * </summary>
     **/
    @Test
    public void shouldRejectDepositEndpointWithoutUserRole() throws Exception {
        mockMvc.perform(post(DEPOSIT_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "alexey"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CASH_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Недостаточно прав для выполнения операции"));
    }

    /**
     * <summary>
     * Проверяет успешную обработку пополнения (200 OK) при наличии ролей ROLE_USER и ROLE_CASH_WRITE.
     * </summary>
     **/
    @Test
    public void shouldAllowDepositWithRequiredRoles() throws Exception {
        when(cashService.deposit(any(), any())).thenReturn(new CashOperationResponseViewModel(
                new BigDecimal("1500.00"),
                "RUB",
                "Счёт пополнен"
        ));

        mockMvc.perform(post(DEPOSIT_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "alexey"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CASH_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Счёт пополнен"));
    }

    /**
     * <summary>
     * Проверяет работу кастомного JwtAuthenticationConverter при извлечении ролей из realm_access claim.
     * </summary>
     **/
    @Test
    public void shouldExtractRolesFromRealmAccessClaim() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("preferred_username", "alexey")
                .claim("realm_access", Map.of("roles", List.of("USER", "CASH_WRITE")))
                .build();

        AbstractAuthenticationToken authenticationToken = jwtAuthenticationConverter.convert(jwt);

        assertThat(authenticationToken).isNotNull();

        assertThat(authenticationToken.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_CASH_WRITE");
    }

    /**
     * <summary>
     * Проверяет блокировку доступа к незарегистрированным эндпоинтам согласно правилу anyRequest().denyAll().
     * </summary>
     **/
    @Test
    public void shouldDenyAccessToUnknownEndpoint() throws Exception {
        mockMvc.perform(post(UNKNOWN_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "alexey"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CASH_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // endregion
}