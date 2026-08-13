package ru.yandex.practicum.bank.notification.configurations;

import org.junit.jupiter.api.DisplayName;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Интеграционные тесты для проверки конфигурации безопасности NotificationSecurityConfiguration.
 * Проверяет доступность публичных эндпоинтов, разграничение прав доступа по JWT-ролям,
 * формат кастомных ответов при ошибках 401/403 и логику конвертации realm-ролей.
 * </summary>
 **/
@SpringBootTest
@AutoConfigureMockMvc
public class NotificationSecurityConfigurationTest {

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что публичный эндпоинт проверки состояния /actuator/health доступен без передачи JWT-токена.
     * </summary>
     **/
    @Test
    @DisplayName("Должен разрешать доступ к /actuator/health без JWT")
    public void shouldAllowHealthWithoutJwt() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    /**
     * <summary>
     * Проверяет, что неаутентифицированный запрос к /api/notification отклоняется со статусом 401 Unauthorized
     * и структурированной ошибкой ApiErrorResponseViewModel.
     * </summary>
     **/
    @Test
    @DisplayName("Должен отклонять запрос к /api/notification без JWT с кодом 401 UNAUTHORIZED")
    public void shouldRejectNotificationWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Требуется авторизация"));
    }

    /**
     * <summary>
     * Проверяет, что запрос с неполным набором ролей (отсутствует ROLE_NOTIFICATION_WRITE)
     * отклоняется со статусом 403 Forbidden.
     * </summary>
     **/
    @Test
    @DisplayName("Должен отклонять запрос к /api/notification при отсутствии роли NOTIFICATION_WRITE с кодом 403 FORBIDDEN")
    public void shouldRejectNotificationWithoutWriteRole() throws Exception {
        mockMvc.perform(post("/api/notification")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Недостаточно прав для выполнения операции"));
    }

    /**
     * <summary>
     * Проверяет успешное выполнение запроса со статусом 202 Accepted при наличии обеих необходимых ролей (ROLE_SERVICE и ROLE_NOTIFICATION_WRITE).
     * </summary>
     **/
    @Test
    @DisplayName("Должен разрешать отправку уведомления со статусом 202 ACCEPTED при наличии ролей SERVICE и NOTIFICATION_WRITE")
    public void shouldAllowNotificationWithServiceRoles() throws Exception {
        mockMvc.perform(post("/api/notification")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("ROLE_NOTIFICATION_WRITE")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationRequest()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    /**
     * <summary>
     * Проверяет корректность извлечения и конвертации списка ролей из структуры realm_access.roles JWT-токена
     * в объекты GrantedAuthority с префиксом ROLE_.
     * </summary>
     **/
    @Test
    @DisplayName("Должен корректно конвертировать realm_access.roles в GrantedAuthority с префиксом ROLE_")
    public void shouldConvertRealmRolesToAuthorities() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-06-13T00:00:00Z"))
                .claim("realm_access", Map.of("roles", List.of("SERVICE", "NOTIFICATION_WRITE")))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_SERVICE", "ROLE_NOTIFICATION_WRITE");
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Вспомогательный метод для формирования тела валидного JSON-запроса на создание уведомления.
     * </summary>
     * <return>
     * @return JSON-строка запроса.
     * </return>
     **/
    private String notificationRequest() {
        return """
                {
                  "recipientLogin": "dmitry",
                  "type": "CASH_DEPOSIT",
                  "message": "Счёт пополнен на 250.00 RUB",
                  "operationId": "operation-1"
                }
                """;
    }

    // endregion
}