package ru.yandex.practicum.bank.exchange.configurations;

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
 * Конфигурация безопасности (Spring Security) для микросервиса обмена валют (Exchange Service).
 * Настраивает правила авторизации (RBAC), обработку OAuth2/JWT-токенов и кастомные JSON-ответы
 * для ошибок аутентификации (401) и отказа в доступе (403).
 * </summary>
 **/
@Configuration
@EnableWebSecurity
public class ExchangeServiceSecurityConfiguration {

    // region Beans

    /**
     * <summary>
     * Определяет основную цепочку фильтров безопасности (SecurityFilterChain).
     * Отключает CSRF и сессии (Stateless), задает публичный доступ к GET-запросам курсов валют,
     * защищает PUT-запрос ролями SERVICE и EXCHANGE_GENERATOR, а также подключает кастомные обработчики ошибок.
     * </summary>
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
                        .requestMatchers(HttpMethod.GET, "/api/exchange/rates").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/exchange/conversion").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/exchange/rates")
                        .access(allOf(hasRole("SERVICE"), hasRole("EXCHANGE_GENERATOR")))
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
     * Создает конвертер JWT-токенов, который отвечает за извлечение ролей пользователя
     * из специфичных claims (realm_access) и их преобразование в GrantedAuthority.
     * </summary>
     **/
    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(this::extractRealmRoles);

        return converter;
    }

    /**
     * <summary>
     * Настраивает обработчик ошибки 401 (Unauthorized).
     * Возвращает клиенту стандартизированный JSON-ответ ApiErrorResponseViewModel вместо стандартного HTML.
     * </summary>
     **/
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
     * Настраивает обработчик ошибки 403 (Forbidden).
     * Возвращает стандартизированный JSON-ответ при попытке выполнить операцию без необходимых ролей.
     * </summary>
     **/
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

    // region Private Methods

    /**
     * <summary>
     * Вспомогательный метод для парсинга JWT-токена (Keycloak-формат).
     * Извлекает массив ролей из claim 'realm_access' и добавляет к каждой роли префикс 'ROLE_'.
     * </summary>
     * @param jwt Входящий JWT-токен.
     * @return Коллекция прав доступа (GrantedAuthority).
     **/
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
     * Вспомогательный метод для сериализации объекта ApiErrorResponseViewModel в HTTP-ответ.
     * </summary>
     * @param objectMapper Экземпляр ObjectMapper для преобразования объекта в JSON.
     * @param response HTTP-ответ, в который производится запись.
     * @param status HTTP статус-код, который необходимо вернуть (например, 401 или 403).
     * @param error Объект ошибки, содержащий код и сообщение.
     * @throws IOException Если возникает ошибка при записи в поток вывода.
     **/
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