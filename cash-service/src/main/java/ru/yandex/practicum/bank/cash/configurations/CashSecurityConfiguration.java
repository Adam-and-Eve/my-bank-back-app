package ru.yandex.practicum.bank.cash.configurations;

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
import ru.yandex.practicum.bank.shared.viewmodels.ApiErrorResponseViewModel;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.security.authorization.AuthorizationManagers.allOf;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;

/**
 * <summary>
 * Конфигурация безопасности Spring Security для сервиса наличных операций (Cash Service).
 * Настраивает авторизацию HTTP-запросов на основе OAuth2 JWT-токенов, разграничение прав доступа по ролям
 * и кастомную обработку ошибок аутентификации и авторизации.
 * </summary>
 **/
@Configuration
@EnableWebSecurity
public class CashSecurityConfiguration {

    // region Beans

    /**
     * <summary>
     * Конфигурирует цепочку фильтров безопасности HTTP Security для модуля операций с наличностью.
     * Задает правила доступа к эндпоинтам и отключает управление сессиями (Stateless).
     * </summary>
     * @param http Объект настройки HttpSecurity.
     * @param jwtAuthenticationConverter Конвертер JWT в объект аутентификации Spring.
     * @param authenticationEntryPoint Обработчик ошибок 401.
     * @param accessDeniedHandler Обработчик ошибок 403.
     * @return Сконфигурированный экземпляр SecurityFilterChain.
     **/
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
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cash/deposit")
                        .access(allOf(hasRole("USER"), hasRole("CASH_WRITE")))
                        .requestMatchers(HttpMethod.POST, "/api/cash/withdraw")
                        .access(allOf(hasRole("USER"), hasRole("CASH_WRITE")))
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
     * Создает конвертер для преобразования входящего JWT-токена в объект аутентификации.
     * Настраивает извлечение ролей из специфичного для Keycloak клейма (realm_access).
     * </summary>
     * @return Настроенный конвертер JwtAuthenticationConverter.
     */
    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(this::extractRealmRoles);

        return converter;
    }

    /**
     * <summary>
     * Настраивает обработчик неаутентифицированных запросов (отсутствующий или невалидный токен).
     * Возвращает клиенту стандартизированный ответ 401 Unauthorized в формате JSON.
     * </summary>
     * @param objectMapper Маппер для сериализации объекта ответа.
     * @return Экземпляр AuthenticationEntryPoint.
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
     * Настраивает обработчик отказов в доступе (токен валиден, но не хватает прав/ролей).
     * Возвращает клиенту стандартизированный ответ 403 Forbidden в формате JSON.
     * </summary>
     * @param objectMapper Маппер для сериализации объекта ответа.
     * @return Экземпляр AccessDeniedHandler.
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

    // region Helpers

    /**
     * <summary>
     * Вспомогательный метод для парсинга JWT-токена. Извлекает список ролей из секции "realm_access.roles"
     * и оборачивает их в объекты GrantedAuthority с префиксом "ROLE_".
     * </summary>
     * @param jwt Декодированный JWT-токен.
     * @return Коллекция прав доступа (GrantedAuthority) пользователя.
     */
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null) {
            return Collections.emptyList();
        }

        var roles = realmAccess.get("roles");

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
     * Вспомогательный метод для записи стандартизированной модели ошибки в тело HTTP-ответа.
     * Устанавливает соответствующий HTTP-статус и Content-Type.
     * </summary>
     * @param objectMapper Маппер JSON.
     * @param response Объект HTTP-ответа сервлета.
     * @param status HTTP-статус ответа.
     * @param error Модель ошибки для сериализации.
     * @throws IOException В случае ошибки записи в выходной поток.
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