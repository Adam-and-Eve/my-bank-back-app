package ru.yandex.practicum.bank.transfer.configurations;

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
import ru.yandex.practicum.bank.transfer.viewmodels.ApiErrorResponseViewModel;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.security.authorization.AuthorizationManagers.allOf;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;

/**
 * <summary>
 * Конфигурация безопасности Spring Security для сервиса переводов (Transfer Service).
 * Настраивает авторизацию HTTP-запросов на основе OAuth2 JWT-токенов, разграничение прав доступа по ролям
 * и кастомную обработку ошибок аутентификации и авторизации.
 * </summary>
 **/
@Configuration
@EnableWebSecurity
public class TransferSecurityConfiguration {

    // region Beans

    /**
     * <summary>
     * Конфигурирует цепочку фильтров безопасности HTTP Security для модуля переводов.
     * </summary>
     * @param http Объект настройки HttpSecurity.
     * @param jwtAuthenticationConverter Конвертер JWT-токена в токен аутентификации Spring Security.
     * @param authenticationEntryPoint Обработчик ошибок неаутентифицированного доступа (401 Unauthorized).
     * @param accessDeniedHandler Обработчик ошибок недостатка прав доступа (403 Forbidden).
     * <return>
     * @return Сконфигурированный экземпляр SecurityFilterChain.
     * </return>
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
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/transfer")
                        .access(allOf(hasRole("USER"), hasRole("TRANSFER_WRITE")))
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
     * Создает конвертер JWT-токена с маппингом ролей из Realm Access в GrantedAuthority.
     * </summary>
     * <return>
     * @return Экземпляр Converter&lt;Jwt, AbstractAuthenticationToken&gt;.
     * </return>
     **/
    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(this::extractRealmRoles);

        return converter;
    }

    /**
     * <summary>
     * Создает обработчик ошибок аутентификации (401 Unauthorized), возвращающий JSON в формате ApiErrorResponseViewModel.
     * </summary>
     * @param objectMapper Экземпляр ObjectMapper для сериализации ответа в JSON.
     * <return>
     * @return Экземпляр AuthenticationEntryPoint.
     * </return>
     **/
    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(
                objectMapper,
                response,
                HttpStatus.UNAUTHORIZED,
                new ApiErrorResponseViewModel("UNAUTHORIZED", "Authorization is required")
        );
    }

    /**
     * <summary>
     * Создает обработчик ошибок отказа в доступе (403 Forbidden), возвращающий JSON в формате ApiErrorResponseViewModel.
     * </summary>
     * @param objectMapper Экземпляр ObjectMapper для сериализации ответа в JSON.
     * <return>
     * @return Экземпляр AccessDeniedHandler.
     * </return>
     **/
    @Bean
    AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(
                objectMapper,
                response,
                HttpStatus.FORBIDDEN,
                new ApiErrorResponseViewModel("FORBIDDEN", "Not enough permissions")
        );
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Извлекает роли из структуры realm_access.roles JWT-токена и преобразует их в GrantedAuthority с префиксом ROLE_.
     * </summary>
     * @param jwt Декодированный JWT-токен.
     * <return>
     * @return Коллекция предоставленных прав GrantedAuthority.
     * </return>
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
     * Записывает объект ошибки ApiErrorResponseViewModel в HTTP-ответ в формате JSON с заданным кодом состояния.
     * </summary>
     * @param objectMapper Экземпляр ObjectMapper для сериализации ответа.
     * @param response Объект HttpServletResponse.
     * @param status HTTP-статус ответа.
     * @param error Объект ошибки ApiErrorResponseViewModel.
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