package ru.yandex.practicum.bank.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Модульные и интеграционные тесты для фабрики фильтров JwtTokenRelayGatewayFilterFactory.
 * Проверяют извлечение JWT-токена из заголовков HTTP-запроса и реактивного контекста безопасности Spring Security,
 * а также корректную трансляцию Bearer-токена downstream-сервисам.
 * </summary>
 **/
@SpringBootTest
public class JwtTokenRelayGatewayFilterFactoryTest {

    // region Fields

    private final JwtTokenRelayGatewayFilterFactory factory = new JwtTokenRelayGatewayFilterFactory();

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет извлечение и трансляцию Bearer-токена из заголовка Authorization входящего HTTP-запроса,
     * если реактивный контекст безопасности пуст.
     * </summary>
     **/
    @Test
    void shouldRelayBearerTokenFromAuthorizationHeader() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/account/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                .build());
        var relayedHeader = new AtomicReference<String>();

        factory.apply(new Object())
                .filter(exchange, filteredExchange -> {
                    relayedHeader.set(filteredExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
                    return filteredExchange.getResponse().setComplete();
                })
                .block();

        assertThat(relayedHeader).hasValue("Bearer user-token");
    }

    /**
     * <summary>
     * Проверяет приоритетное извлечение токена из реактивного контекста безопасности ReactiveSecurityContextHolder
     * и его подстановку в заголовок Authorization формата Bearer для нижележащих микросервисов.
     * </summary>
     **/
    @Test
    void shouldRelayTokenFromSecurityContext() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/account/me").build());
        var relayedHeader = new AtomicReference<String>();

        var jwt = Jwt.withTokenValue("security-context-token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .build();
        var authentication = new JwtAuthenticationToken(jwt);

        factory.apply(new Object())
                .filter(exchange, filteredExchange -> {
                    relayedHeader.set(filteredExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
                    return filteredExchange.getResponse().setComplete();
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        assertThat(relayedHeader).hasValue("Bearer security-context-token");
    }

    /**
     * <summary>
     * Проверяет, что при отсутствии токена как в контексте безопасности, так и в заголовках,
     * HTTP-запрос передается далее по цепочке фильтров без изменений.
     * </summary>
     **/
    @Test
    void shouldPassExchangeUnchangedWhenNoTokenPresent() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/account/me").build());

        var relayedHeader = new AtomicReference<String>();

        factory.apply(new Object())
                .filter(exchange, filteredExchange -> {
                    relayedHeader.set(filteredExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
                    return filteredExchange.getResponse().setComplete();
                })
                .block();

        assertThat(relayedHeader.get()).isNull();
    }

    /**
     * <summary>
     * Проверяет, что заголовки авторизации отличных от Bearer схем (например, Basic)
     * не модифицируются при отсутствии токена в контексте безопасности.
     * </summary>
     **/
    @Test
    void shouldIgnoreNonBearerAuthorizationHeader() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/account/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build());
        var relayedHeader = new AtomicReference<String>();

        factory.apply(new Object())
                .filter(exchange, filteredExchange -> {
                    relayedHeader.set(filteredExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
                    return filteredExchange.getResponse().setComplete();
                })
                .block();

        assertThat(relayedHeader).hasValue("Basic dXNlcjpwYXNz");
    }

    // endregion
}