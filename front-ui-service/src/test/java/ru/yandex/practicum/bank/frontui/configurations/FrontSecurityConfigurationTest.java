package ru.yandex.practicum.bank.frontui.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Интеграционные тесты конфигурации безопасности пользовательского интерфейса
 * (FrontSecurityConfiguration).
 * Проверяют правила доступа к публичным и защищённым ресурсам,
 * OAuth2/OIDC-аутентификацию и корректность RP-Initiated Logout
 * с передачей id_token_hint в OIDC Provider.
 * </summary>
 **/
@SpringBootTest
@AutoConfigureMockMvc
public class FrontSecurityConfigurationTest {

    // region Constants

    private static final String HOME_URL = "/";

    private static final String CSS_URL = "/css/main.css";

    private static final String HEALTH_URL = "/actuator/health";

    private static final String LOGOUT_URL = "/logout";

    private static final String PROTECTED_URL = "/protected-test";

    private static final String END_SESSION_URI =
            "http://localhost:8180/realms/my-bank-realm/protocol/openid-connect/logout";

    private static final String ID_TOKEN = "test-id-token";

    // endregion

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    // endregion

    // region Tests - Public endpoints

    /**
     * <summary>
     * Проверяет, что главная страница требует аутентификацию.
     * </summary>
     **/
    @Test
    public void shouldRequireAuthenticationForHome() throws Exception {
        mockMvc.perform(get(HOME_URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.containsString("/oauth2/authorization/")
                ));
    }

    /**
     * <summary>
     * Проверяет, что CSS-ресурсы разрешены без аутентификации.
     * Сам ресурс может отсутствовать, поэтому ожидается 404,
     * а не редирект на страницу авторизации.
     * </summary>
     **/
    @Test
    public void shouldAllowAccessToCssWithoutAuthentication() throws Exception {
        mockMvc.perform(get(CSS_URL))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();

                    assertThat(status)
                            .isNotEqualTo(401)
                            .isNotEqualTo(403);

                    assertThat(status)
                            .isNotEqualTo(302);
                });
    }

    /**
     * <summary>
     * Проверяет, что endpoint health разрешён без аутентификации.
     * </summary>
     **/
    @Test
    public void shouldAllowHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get(HEALTH_URL))
                .andExpect(status().isOk());
    }

    // endregion

    // region Tests - Authentication

    /**
     * <summary>
     * Проверяет, что защищённый неизвестный endpoint требует аутентификацию.
     * </summary>
     **/
    @Test
    public void shouldRequireAuthenticationForUnknownEndpoint() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.containsString("/oauth2/authorization/")
                ));
    }

    // endregion

    // region Tests - Logout

    /**
     * <summary>
     * Проверяет успешный выход пользователя и перенаправление
     * на OIDC Provider End Session Endpoint.
     * </summary>
     **/
    @Test
    public void shouldRedirectToOidcEndSessionEndpointOnLogout() throws Exception {
        mockMvc.perform(post(LOGOUT_URL)
                        .with(SecurityMockMvcRequestPostProcessors.user("dmitry"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.containsString(END_SESSION_URI)
                ));
    }

    /**
     * <summary>
     * Проверяет наличие post_logout_redirect_uri в запросе
     * к OIDC Provider при выходе из системы.
     * </summary>
     **/
    @Test
    public void shouldIncludePostLogoutRedirectUriOnLogout() throws Exception {
        mockMvc.perform(post(LOGOUT_URL)
                        .with(SecurityMockMvcRequestPostProcessors.user("dmitry"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.containsString(
                                "post_logout_redirect_uri=http"
                        )
                ));
    }

    /**
     * <summary>
     * Проверяет передачу id_token_hint при выходе пользователя,
     * авторизованного через OIDC.
     * </summary>
     **/
    @Test
    public void shouldIncludeIdTokenHintOnOidcLogout() throws Exception {
        var idToken = OidcIdToken.withTokenValue(ID_TOKEN)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(claims -> claims.put("sub", "dmitry"))
                .build();

        var oidcUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                idToken
        );

        var authentication = new UsernamePasswordAuthenticationToken(
                oidcUser,
                null,
                oidcUser.getAuthorities()
        );

        mockMvc.perform(post(LOGOUT_URL)
                        .with(authentication(authentication))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.containsString(
                                "id_token_hint=" + ID_TOKEN
                        )
                ));
    }

    /**
     * <summary>
     * Проверяет, что при выходе пользователя без OIDC-аутентификации
     * параметр id_token_hint не добавляется в redirect URI.
     * </summary>
     **/
    @Test
    public void shouldNotIncludeIdTokenHintForNonOidcAuthentication() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                "dmitry",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        var result = mockMvc.perform(post(LOGOUT_URL)
                        .with(authentication(authentication))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        var location = result.getResponse().getHeader("Location");

        assertThat(location).isNotNull();
        assertThat(location).contains(END_SESSION_URI);
        assertThat(location).contains("post_logout_redirect_uri");
        assertThat(location).doesNotContain("id_token_hint");
    }

    /**
     * <summary>
     * Проверяет, что logout без аутентификации также не проходит
     * как обычный защищённый запрос.
     * </summary>
     **/
    @Test
    public void shouldHandleLogoutWithoutAuthentication() throws Exception {
        mockMvc.perform(post(LOGOUT_URL)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // endregion
}