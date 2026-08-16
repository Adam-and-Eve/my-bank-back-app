package ru.yandex.practicum.bank.shared.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import ru.yandex.practicum.bank.shared.viewmodels.ApiErrorResponseViewModel;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <summary>
 * Общая поддержка Resource Server: JWT → roles, 401/403 JSON-ответы.
 * </summary>
 **/
public final class ResourceServerSecurityHelper {

    // region Constructors

    private ResourceServerSecurityHelper() {
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Создает конвертер JWT-токена с маппингом ролей из Realm Access в GrantedAuthority.
     * </summary>
     * <return>
     * @return Экземпляр Converter&lt;Jwt, AbstractAuthenticationToken&gt;.
     * </return>
     **/
    public static Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(ResourceServerSecurityHelper::extractRealmRoles);

        return converter;
    }

    /**
     * <summary>
     * Извлекает роли из структуры realm_access.roles JWT-токена и преобразует их в GrantedAuthority с префиксом ROLE_.
     * </summary>
     * @param jwt Декодированный JWT-токен.
     * <return>
     * @return Коллекция предоставленных прав GrantedAuthority.
     * </return>
     **/
    public static Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
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
     * Создает обработчик ошибок аутентификации (401 Unauthorized), возвращающий JSON в формате ApiErrorResponseViewModel.
     * </summary>
     * @param objectMapper Экземпляр ObjectMapper для сериализации ответа в JSON.
     * <return>
     * @return Экземпляр AuthenticationEntryPoint.
     * </return>
     **/
    public static AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(
                objectMapper,
                response,
                HttpStatus.UNAUTHORIZED,
                new ApiErrorResponseViewModel("UNAUTHORIZED", "Требуется авторизация")
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
    public static AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(
                objectMapper,
                response,
                HttpStatus.FORBIDDEN,
                new ApiErrorResponseViewModel("FORBIDDEN", "Недостаточно прав для выполнения операции")
        );
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
    private static void writeError(
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