package ru.yandex.practicum.bank.shared.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.yandex.practicum.bank.shared.viewmodels.ApiErrorResponseViewModel;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Тесты ResourceServerSecurityHelper.
 * Проверяют преобразование JWT-ролей и формирование JSON-ответов для ошибок аутентификации и авторизации.
 * </summary>
 **/
public class ResourceServerSecurityHelperTest {

    // region Constants

    private static final String ROLE_USER = "USER";

    private static final String ROLE_ADMIN = "ADMIN";

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что роли из realm_access.roles преобразуются в GrantedAuthority
     * с префиксом ROLE_.
     * </summary>
     **/
    @Test
    public void shouldExtractRealmRoles() {
        var jwt = createJwt(Map.of(
                "realm_access",
                Map.of("roles", List.of(ROLE_USER, ROLE_ADMIN))
        ));

        var authorities = ResourceServerSecurityHelper.extractRealmRoles(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(
                        "ROLE_" + ROLE_USER,
                        "ROLE_" + ROLE_ADMIN
                );
    }

    /**
     * <summary>
     * Проверяет, что при отсутствии roles внутри realm_access возвращается пустая коллекция.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyAuthoritiesWhenRolesAreMissing() {
        var jwt = createJwt(Map.of(
                "realm_access",
                Map.of()
        ));

        var authorities = ResourceServerSecurityHelper.extractRealmRoles(jwt);

        assertThat(authorities).isEmpty();
    }

    /**
     * <summary>
     * Проверяет, что при некорректном типе значения roles возвращается пустая коллекция.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyAuthoritiesWhenRolesHaveInvalidType() {
        var jwt = createJwt(Map.of(
                "realm_access",
                Map.of("roles", "USER")
        ));

        var authorities = ResourceServerSecurityHelper.extractRealmRoles(jwt);

        assertThat(authorities).isEmpty();
    }

    /**
     * <summary>
     * Проверяет, что из списка ролей игнорируются значения, которые не являются строками.
     * </summary>
     **/
    @Test
    public void shouldIgnoreNonStringRoles() {
        var jwt = createJwt(Map.of(
                "realm_access",
                Map.of("roles", List.of(
                        ROLE_USER,
                        123,
                        true,
                        ROLE_ADMIN
                ))
        ));

        var authorities = ResourceServerSecurityHelper.extractRealmRoles(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(
                        "ROLE_" + ROLE_USER,
                        "ROLE_" + ROLE_ADMIN
                );
    }

    /**
     * <summary>
     * Проверяет, что jwtAuthenticationConverter создает конвертер,
     * использующий маппинг realm_access.roles.
     * </summary>
     **/
    @Test
    public void shouldCreateJwtAuthenticationConverter() {
        var converter = ResourceServerSecurityHelper.jwtAuthenticationConverter();

        var jwt = createJwt(Map.of(
                "realm_access",
                Map.of("roles", List.of(ROLE_USER))
        ));

        var authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + ROLE_USER);
    }

    /**
     * <summary>
     * Проверяет, что authenticationEntryPoint возвращает HTTP 401
     * и JSON с кодом и сообщением об отсутствии авторизации.
     * </summary>
     **/
    @Test
    public void shouldHandleAuthenticationError() throws Exception {
        var objectMapper = new ObjectMapper();

        var response = mock(HttpServletResponse.class);

        var outputStream = new ByteArrayOutputStream();

        when(response.getOutputStream()).thenReturn(
                new jakarta.servlet.ServletOutputStream() {
                    @Override
                    public void write(int value) {
                        outputStream.write(value);
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(
                            jakarta.servlet.WriteListener writeListener
                    ) {
                    }
                }
        );

        var entryPoint = ResourceServerSecurityHelper.authenticationEntryPoint(objectMapper);

        entryPoint.commence(
                null,
                response,
                mock(org.springframework.security.core.AuthenticationException.class)
        );

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());

        verify(response).setContentType("application/json");

        var error = objectMapper.readValue(
                outputStream.toByteArray(),
                ApiErrorResponseViewModel.class
        );

        assertThat(error.code()).isEqualTo("UNAUTHORIZED");

        assertThat(error.message()).isEqualTo("Требуется авторизация");
    }

    /**
     * <summary>
     * Проверяет, что accessDeniedHandler возвращает HTTP 403
     * и JSON с кодом и сообщением об отсутствии необходимых прав.
     * </summary>
     **/
    @Test
    public void shouldHandleAccessDeniedError() throws Exception {
        var objectMapper = new ObjectMapper();

        var response = mock(HttpServletResponse.class);

        var outputStream = new ByteArrayOutputStream();

        when(response.getOutputStream()).thenReturn(
                new jakarta.servlet.ServletOutputStream() {
                    @Override
                    public void write(int value) {
                        outputStream.write(value);
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(
                            jakarta.servlet.WriteListener writeListener
                    ) {
                    }
                }
        );

        var accessDeniedHandler = ResourceServerSecurityHelper.accessDeniedHandler(objectMapper);

        accessDeniedHandler.handle(
                null,
                response,
                mock(org.springframework.security.access.AccessDeniedException.class)
        );

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());

        verify(response).setContentType("application/json");

        var error = objectMapper.readValue(
                outputStream.toByteArray(),
                ApiErrorResponseViewModel.class
        );

        assertThat(error.code()).isEqualTo("FORBIDDEN");
        assertThat(error.message())
                .isEqualTo("Недостаточно прав для выполнения операции");
    }

    // endregion

    // region Helpers

    /**
     * <summary>
     * Создает JWT с указанными claims.
     * </summary>
     **/
    private Jwt createJwt(Map<String, Object> claims) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                claims
        );
    }

    // endregion
}