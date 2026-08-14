package ru.yandex.practicum.bank.account.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import ru.yandex.practicum.bank.account.viewmodels.ApiErrorResponseViewModel;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.security.authorization.AuthorizationManagers.allOf;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;

/**
 * <summary>
 * Конфигурация безопасности Spring Security для сервиса аккаунтов (account-service).
 * Настраивает правила авторизации запросов на основе ролей OAuth2/Keycloak,
 * stateless-сессии, а также кастомную обработку ошибок 401 (Unauthorized) и 403 (Forbidden).
 * </summary>
 **/
@Configuration
@EnableWebSecurity
public class AccountSecurityConfiguration {

    // Beans

    /**
     * <summary>
     * Настраивает цепочку фильтров безопасности HTTP.
     * Определяет ролевую модель для публичных, пользовательских и межсервисных (internal) эндпоинтов,
     * отключает CSRF и сессии, подключает OAuth2 Resource Server и обработчики ошибок.
     * </summary>
     * @param http Объект настройки HTTP-безопасности.
     * @param jwtAuthenticationConverter Конвертер JWT-токена в объект аутентификации Spring Security.
     * @param authenticationEntryPoint Обработчик ошибок неаутентифицированного доступа (401).
     * @param accessDeniedHandler Обработчик ошибок недостатка прав (403).
     * @return Сформированная цепочка фильтров {@link SecurityFilterChain}.
     * @throws Exception Если возникла ошибка при сборке конфигурации.
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/account/me")
                        .access(allOf(hasRole("USER"), hasRole("ACCOUNT_READ")))
                        .requestMatchers(HttpMethod.GET, "/api/account/recipients")
                        .access(allOf(hasRole("USER"), hasRole("ACCOUNT_READ")))
                        .requestMatchers(HttpMethod.PUT, "/api/account/me")
                        .access(allOf(hasRole("USER"), hasRole("ACCOUNT_WRITE")))
                        .requestMatchers("/api/account/internal/balance/**")
                        .access(allOf(hasRole("SERVICE"), hasRole("ACCOUNT_INTERNAL")))
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }

    /**
     * <summary>
     * Создает конвертер JWT-токена с кастомным извлечением ролей из claim 'realm_access'.
     * </summary>
     * @return Экземпляр {@link Converter} для превращения {@link Jwt} в {@link AbstractAuthenticationToken}.
     */
    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(this::extractRealmRoles);

        return converter;
    }

    /**
     * <summary>
     * Формирует точка входа для обработки ошибок неаутентифицированного запроса (HTTP 401 Unauthorized).
     * Возвращает JSON с моделью {@link ApiErrorResponseViewModel}.
     * </summary>
     * @param objectMapper Сериализатор JSON.
     * @return Реализация {@link AuthenticationEntryPoint}.
     */
    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(
                objectMapper,
                response,
                HttpStatus.UNAUTHORIZED,
                new ApiErrorResponseViewModel("UNAUTHORIZED", "Требуется авторизация")
        );
    }

    /**
     * <summary>
     * Формирует обработчик ошибок для случаев недостатка прав доступа (HTTP 403 Forbidden).
     * Возвращает JSON с моделью {@link ApiErrorResponseViewModel}.
     * </summary>
     * @param objectMapper Сериализатор JSON.
     * @return Реализация {@link AccessDeniedHandler}.
     */
    @Bean
    AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(
                objectMapper,
                response,
                HttpStatus.FORBIDDEN,
                new ApiErrorResponseViewModel("FORBIDDEN", "Недостаточно прав для выполнения операции")
        );
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Извлекает список ролей из объекта claim 'realm_access' JWT-токена Keycloak
     * и преобразует их в объекты {@link GrantedAuthority} с префиксом 'ROLE_'.
     * </summary>
     * @param jwt Декодированный JWT-токен.
     * @return Коллекция прав авторизации {@link GrantedAuthority}.
     */
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null) {
            return Collections.emptyList();
        }

        var  roles = realmAccess.get("roles");

        if (!(roles instanceof List<?> roleList)) {
            return Collections.emptyList();
        }

        return roleList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    /**
     * <summary>
     * Записывает объект ошибки {@link ApiErrorResponseViewModel} в поток вывода HTTP-ответа
     * с указанным статусом и заголовком Content-Type: application/json.
     * </summary>
     * @param objectMapper Сериализатор JSON.
     * @param response Объект HTTP-ответа.
     * @param status Устанавливаемый HTTP-статус.
     * @param error Модель информации об ошибке.
     * @throws IOException В случае ошибки записи в поток вывода ответа.
     */
    private void writeError(
            ObjectMapper objectMapper,
            jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status,
            ApiErrorResponseViewModel error
    ) throws IOException {
        response.setStatus(status.value());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), error);
    }

    // endregion
}