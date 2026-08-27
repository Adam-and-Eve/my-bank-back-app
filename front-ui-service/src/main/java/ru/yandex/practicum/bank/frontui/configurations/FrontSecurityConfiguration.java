package ru.yandex.practicum.bank.frontui.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * <summary>
 * Конфигурационный класс безопасности веб-интерфейса (FrontSecurityConfiguration).
 * Настраивает правила доступа к ресурсам, авторизацию через OAuth2/OIDC и
 * процедуру выхода из системы (RP-Initiated Logout).
 * </summary>
 **/
@Configuration
@EnableWebSecurity
public class FrontSecurityConfiguration {

    // region Beans

    /**
     * <summary>
     * Создает и настраивает цепочку фильтров безопасности (SecurityFilterChain).
     * </summary>
     * @param http Объект настройки веб-безопасности Spring Security.
     * @param endSessionUri URI конечной точки завершения сессии OIDC провайдера.
     * @return Сконфигурированная цепочка фильтров безопасности.
     * @throws Exception Если возникла ошибка при сборке конфигурации.
     **/
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${bank.public-base-url}") URI publicBaseUrl,
            @Value("${bank.security.logout.end-session-uri}") URI endSessionUri
    ) throws Exception {
        var loginEntryPoint =
                new LoginUrlAuthenticationEntryPoint(
                        "/oauth2/authorization/front-ui-service"
                );

        loginEntryPoint.setFavorRelativeUris(true);

        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                                "/css/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(loginEntryPoint)
                )
                .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl("/", true))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(publicBaseUrl, endSessionUri))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                )
                .build();
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Формирует обработчик успешного выхода из системы (LogoutSuccessHandler),
     * перенаправляющий пользователя на OIDC Provider End Session Endpoint с передачей id_token_hint.
     * </summary>
     * @param endSessionUri URI завершения сессии авторизационного сервера.
     * @return Кастомный обработчик выхода из системы.
     **/
    private LogoutSuccessHandler oidcLogoutSuccessHandler(
            URI publicBaseUrl,
            URI endSessionUri) {
        return (request, response, authentication) -> {
            var postLogoutRedirectUri = ServletUriComponentsBuilder
                    .fromUri(publicBaseUrl)
                    .path("/")
                    .build()
                    .toUriString();

            var logoutUri = UriComponentsBuilder.fromUri(endSessionUri)
                    .queryParam("post_logout_redirect_uri", postLogoutRedirectUri);

            var idToken = idToken(authentication);

            if (StringUtils.hasText(idToken)) {
                logoutUri.queryParam("id_token_hint", idToken);
            }

            response.sendRedirect(logoutUri.build().encode().toUriString());
        };
    }

    /**
     * <summary>
     * Извлекает строковое значение ID Token из текущего объекта аутентификации.
     * </summary>
     * @param authentication Объект аутентификации пользователя.
     * @return Значение id_token или {@code null}, если пользователь не аутентифицирован через OIDC.
     **/
    private String idToken(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getIdToken().getTokenValue();
        }

        return null;
    }

    // endregion
}