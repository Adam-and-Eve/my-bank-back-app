package ru.yandex.practicum.bank.transfer.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.transfer.exceptions.MissingPreferredUsernameException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResponseViewModel;

import java.math.BigDecimal;
import java.time.Instant;
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
 * Интеграционные тесты конфигурации безопасности (Spring Security, OAuth2 Resource Server, JWT Converter).
 * </summary>
 **/
@SpringBootTest
@AutoConfigureMockMvc
public class TransferSecurityConfigurationTest {

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

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
     * Проверяет отклонение запроса на перевод (401 Unauthorized) при отсутствии JWT-токена в заголовках.
     * </summary>
     **/
    @Test
    public void shouldRejectTransferEndpointWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/transfer")
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
        when(transferService.transfer(any(), any())).thenReturn(new TransferResponseViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("800.00"),
                "RUB",
                "Transfer completed"
        ));

        mockMvc.perform(post("/api/transfer")
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
     * Проверяет генерацию ответа 401 Unauthorized при наличии требуемой роли, но отсутствии обязательного claim preferred_username.
     * </summary>
     **/
    @Test
    public void shouldRejectTransferEndpointWhenPreferredUsernameIsMissing() throws Exception {
        when(transferService.transfer(any(), any())).thenThrow(new MissingPreferredUsernameException());

        mockMvc.perform(post("/api/transfer")
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

    /**
     * <summary>
     * Проверяет корректное преобразование списков ролей из realm_access JWT-токена в GrantedAuthority с префиксом ROLE_.
     * </summary>
     **/
    @Test
    public void shouldConvertRealmRolesToAuthorities() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-06-13T00:00:00Z"))
                .claim("realm_access", Map.of("roles", List.of("USER", "TRANSFER_WRITE")))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_TRANSFER_WRITE");
    }

    /**
     * <summary>
     * Проверяет, что конвертер возвращает пустую коллекцию authorities, если в JWT отсутствует секция realm_access.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyAuthoritiesWhenRealmAccessIsMissing() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-06-13T00:00:00Z"))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    /**
     * <summary>
     * Проверяет, что конвертер возвращает пустую коллекцию authorities, если список ролей в realm_access пуст.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyAuthoritiesWhenRolesAreEmpty() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-06-13T00:00:00Z"))
                .claim("realm_access", Map.of("roles", List.of()))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    // endregion

    // region Methods


    /**
     * <summary>
     * Вспомогательный метод формирования валидного JSON-тела запроса перевода.
     * </summary>
     * <return>
     * @return JSON-строка запроса TransferRequestViewModel.
     * </return>
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