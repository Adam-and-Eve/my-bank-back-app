package ru.yandex.practicum.bank.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <summary>
 * Фабрика кастомных фильтров Spring Cloud Gateway для трансляции JWT-токенов (Token Relay).
 * Извлекает JWT-токен из реактивного контекста безопасности или заголовков входящего запроса
 * и пробрасывает его в заголовке Authorization к нижележащим микросервисам.
 * </summary>
 **/
@Component
public class JwtTokenRelayGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    // region Constructors

    public JwtTokenRelayGatewayFilterFactory() {
        super(Object.class);
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Создает и возвращает экземпляр GatewayFilter, выполняющий трансляцию токена для текущего запроса.
     * </summary>
     * @param config Объект конфигурации фильтра.
     * <return>
     * @return Экземпляр GatewayFilter для обработки и проброса JWT-токена в цепочке фильтров.
     * </return>
     **/
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> extractToken(exchange)
                .map(token -> addToken(exchange, token))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    /**
     * <summary>
     * Извлекает строковое значение JWT-токена из реактивного контекста безопасности ReactiveSecurityContextHolder.
     * Если контекст пуст, выполняется резервное извлечение из заголовка Authorization входящего HTTP-запроса.
     * </summary>
     * @param exchange Контекст текущего серверного HTTP-запроса и ответа ServerWebExchange.
     * <return>
     * @return Mono&lt;String&gt;, содержащий извлеченный JWT-токен, или пустой Mono, если токен не найден.
     * </return>
     **/
    private Mono<String> extractToken(ServerWebExchange exchange) {
        var tokenFromContext = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> authentication.getToken().getTokenValue());

        var tokenFromHeader = Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));

        return tokenFromContext.switchIfEmpty(tokenFromHeader);
    }

    /**
     * <summary>
     * Модифицирует текущий HTTP-запрос, подставляя переданный JWT-токен в заголовок Authorization (Bearer token).
     * </summary>
     * @param exchange Текущий реактивный контекст ServerWebExchange.
     * @param token Строковое значение JWT-токена.
     * <return>
     * @return Новый экземпляр ServerWebExchange с обновленным заголовком Authorization в запросе.
     * </return>
     **/
    private ServerWebExchange addToken(ServerWebExchange exchange, String token) {
        var request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.setBearerAuth(token))
                .build();

        return exchange.mutate()
                .request(request)
                .build();
    }

    // endregion
}