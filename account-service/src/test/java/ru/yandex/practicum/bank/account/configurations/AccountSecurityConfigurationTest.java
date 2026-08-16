package ru.yandex.practicum.bank.account.configurations;

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
import ru.yandex.practicum.bank.account.interfaces.AccountService;
import ru.yandex.practicum.bank.account.interfaces.BalanceService;
import ru.yandex.practicum.bank.account.viewmodels.AccountResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.BalanceResponseViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Интеграционные тесты для конфигурации безопасности AccountSecurityConfiguration.
 * Проверяют корректность настройки прав доступа (RBAC), извлечение ролей из JWT-токена (realm_access),
 * а также кастомную обработку ошибок 401 Unauthorized и 403 Forbidden.
 * </summary>
 **/
@SpringBootTest
@AutoConfigureMockMvc
public class AccountSecurityConfigurationTest {

    // region Constants

    private static final String ME_URL = "/api/account/me";

    private static final String RECIPIENTS_URL = "/api/account/recipients";

    private static final String INTERNAL_BALANCE_DEPOSIT_URL = "/api/account/internal/balance/deposit";

    private static final String HEALTH_URL = "/actuator/health";

    private static final String UNKNOWN_URL = "/api/account/unknown";

    private static final String UPDATE_ME_REQUEST_BODY = """
            {
                "name": "Дмитрий Волков",
                "birthdate": "1999-10-19"
            }
            """;

    private static final String BALANCE_OPERATION_REQUEST_BODY = """
            {
                "operationId": "op-12345",
                "login": "dmitry",
                "amount": "5000.00",
                "currency": "RUB"
            }
            """;

    // endregion

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private BalanceService balanceService;

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
     * Проверяет отклонение запроса на получение профиля (401 Unauthorized) с кастомным JSON при отсутствии JWT-токена.
     * </summary>
     **/
    @Test
    public void shouldRejectGetMeEndpointWithoutJwt() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Требуется авторизация"));
    }

    /**
     * <summary>
     * Проверяет отклонение запроса на получение профиля (403 Forbidden), если у пользователя отсутствует роль ROLE_ACCOUNT_READ.
     * </summary>
     **/
    @Test
    public void shouldRejectGetMeEndpointWithoutAccountReadRole() throws Exception {
        mockMvc.perform(get(ME_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Недостаточно прав для выполнения операции"));
    }

    /**
     * <summary>
     * Проверяет отклонение запроса на получение профиля (403 Forbidden), если у пользователя отсутствует роль ROLE_USER.
     * </summary>
     **/
    @Test
    public void shouldRejectGetMeEndpointWithoutUserRole() throws Exception {
        mockMvc.perform(get(ME_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ACCOUNT_READ"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Недостаточно прав для выполнения операции"));
    }

    /**
     * <summary>
     * Проверяет успешную обработку получения профиля (200 OK) при наличии ролей ROLE_USER и ROLE_ACCOUNT_READ.
     * </summary>
     **/
    @Test
    public void shouldAllowGetMeWithRequiredRoles() throws Exception {
        when(accountService.getCurrentAccount("dmitry")).thenReturn(new AccountResponseViewModel(
                "dmitry",
                "Дмитрий Волков",
                LocalDate.of(1999, 10, 19),
                new BigDecimal("1000000.00"),
                "RUB"
        ));

        mockMvc.perform(get(ME_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_READ")
                                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("dmitry"))
                .andExpect(jsonPath("$.name").value("Дмитрий Волков"));
    }

    /**
     * <summary>
     * Проверяет успешный доступ к получению списка получателей переводов при наличии ролей ROLE_USER и ROLE_ACCOUNT_READ.
     * </summary>
     **/
    @Test
    public void shouldAllowGetRecipientsWithRequiredRoles() throws Exception {
        when(accountService.getRecipients("dmitry")).thenReturn(List.of());

        mockMvc.perform(get(RECIPIENTS_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_READ")
                                )))
                .andExpect(status().isOk());
    }

    /**
     * <summary>
     * Проверяет отклонение запроса на обновление профиля (403 Forbidden), если у пользователя роль ROLE_ACCOUNT_READ вместо ROLE_ACCOUNT_WRITE.
     * </summary>
     **/
    @Test
    public void shouldRejectUpdateMeEndpointWithoutWriteRole() throws Exception {
        mockMvc.perform(put(ME_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_READ")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_ME_REQUEST_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    /**
     * <summary>
     * Проверяет успешное обновление профиля (200 OK) при наличии ролей ROLE_USER и ROLE_ACCOUNT_WRITE.
     * </summary>
     **/
    @Test
    public void shouldAllowUpdateMeWithRequiredRoles() throws Exception {
        when(accountService.updateCurrentAccount(any(), any())).thenReturn(new AccountResponseViewModel(
                "dmitry",
                "Дмитрий Волков",
                LocalDate.of(1999, 10, 19),
                new BigDecimal("1000000.00"),
                "RUB"
        ));

        mockMvc.perform(put(ME_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_ME_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("dmitry"));
    }

    /**
     * <summary>
     * Проверяет успешный доступ к внутренним межсервисным эндпоинтам (/api/account/internal/balance/**)
     * при наличии служебных ролей ROLE_SERVICE и ROLE_ACCOUNT_INTERNAL.
     * </summary>
     **/
    @Test
    public void shouldAllowInternalBalanceEndpointWithServiceRoles() throws Exception {
        when(balanceService.deposit(any())).thenReturn(new BalanceResponseViewModel(
                "dmitry",
                new BigDecimal("1005000.00"),
                "RUB"
        ));

        mockMvc.perform(post(INTERNAL_BALANCE_DEPOSIT_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "service-account"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SERVICE"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_INTERNAL")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BALANCE_OPERATION_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1005000.00));
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
                .claim("preferred_username", "dmitry")
                .claim("realm_access", Map.of("roles", List.of("USER", "ACCOUNT_READ", "ACCOUNT_WRITE")))
                .build();

        AbstractAuthenticationToken authenticationToken = jwtAuthenticationConverter.convert(jwt);

        assertThat(authenticationToken).isNotNull();

        assertThat(authenticationToken.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ACCOUNT_READ", "ROLE_ACCOUNT_WRITE");
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
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_READ")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // endregion
}