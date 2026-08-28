package ru.yandex.practicum.bank.blocker.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.blocker.configurations.properties.BlockerServiceSecurityConfiguration;
import ru.yandex.practicum.bank.blocker.interfaces.BlockerService;

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
 * Тесты конфигурации безопасности BlockerServiceSecurityConfiguration.
 * Проверяют правила маршрутизации, RBAC-авторизацию (требование роли SERVICE),
 * формирование кастомных ответов (401/403) и логику работы JWT-конвертера.
 * </summary>
 **/
@WebMvcTest
@Import(BlockerServiceSecurityConfiguration.class)
public class BlockerServiceSecurityConfigurationTest {

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    /**
     * <summary>
     * Мок декодера обязателен для успешного поднятия OAuth2 Resource Server
     * в тестовом контексте без обращения к реальному Keycloak.
     * </summary>
     **/
    @MockitoBean
    private JwtDecoder jwtDecoder;

    /**
     * <summary>
     * Мок сервиса блокировок необходим, чтобы Spring MVC смог успешно
     * инстанцировать BlockerController в урезанном тестовом контексте.
     * </summary>
     **/
    @MockitoBean
    private BlockerService blockerService;

    // endregion

    // region Endpoint Authorization Tests

    /**
     * <summary>
     * Проверяет, что публичные actuator-эндпоинты доступны без авторизации.
     * Возвращает 404 Not Found (вместо 401/403), так как фильтр безопасности пройден.
     * </summary>
     **/
    @Test
    public void shouldAllowAccessToPublicEndpointsWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isNotFound());
    }

    /**
     * <summary>
     * Проверяет, что запрос к защищенному эндпоинту без JWT-токена
     * отклоняется с ошибкой 401 и возвращает структуру ApiErrorResponseViewModel.
     * </summary>
     **/
    @Test
    public void shouldReturn401ForProtectedEndpointWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/blocker/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Требуется авторизация"));
    }

    /**
     * <summary>
     * Проверяет, что наличие неверной роли (например, USER вместо SERVICE)
     * отклоняется с ошибкой 403 и возвращает структуру ApiErrorResponseViewModel.
     * </summary>
     **/
    @Test
    public void shouldReturn403WhenMissingRequiredRole() throws Exception {
        mockMvc.perform(post("/api/blocker/check")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Недостаточно прав для выполнения операции"));
    }

    /**
     * <summary>
     * Проверяет, что запрос с валидным токеном и ролью SERVICE
     * успешно проходит фильтр безопасности.
     * </summary>
     **/
    @Test
    public void shouldAllowAccessWhenRequiredRoleIsPresent() throws Exception {
        mockMvc.perform(post("/api/blocker/check")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotIn(401, 403);
                });
    }

    /**
     * <summary>
     * Проверяет правило denyAll(): любой незарегистрированный эндпоинт
     * должен блокироваться даже при наличии авторизации уровня SERVICE.
     * </summary>
     **/
    @Test
    public void shouldDenyAccessToUnknownEndpointsEvenWithAuth() throws Exception {
        mockMvc.perform(get("/api/unknown/endpoint")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE"))))
                .andExpect(status().isForbidden());
    }

    // endregion

    // region JWT Converter Unit Tests

    /**
     * <summary>
     * Проверяет корректное извлечение ролей из структуры Keycloak
     * (claim 'realm_access.roles') и добавление префикса 'ROLE_'.
     * </summary>
     **/
    @Test
    public void shouldExtractRolesFromRealmAccessClaim() {
        var jwt = Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("SERVICE", "ADMIN")))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_SERVICE", "ROLE_ADMIN");
    }

    /**
     * <summary>
     * Проверяет безопасную обработку токена без объекта 'realm_access'.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyAuthoritiesWhenRealmAccessIsMissing() {
        var jwt = Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .claim("other_claim", "value")
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    /**
     * <summary>
     * Проверяет безопасную обработку токена, где в 'realm_access' отсутствует массив 'roles'.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyAuthoritiesWhenRolesAreMissingInRealmAccess() {
        var jwt = Jwt.withTokenValue("dummy-token")
                .header("alg", "none")
                .claim("realm_access", Map.of("some_other_key", "value"))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    // endregion
}