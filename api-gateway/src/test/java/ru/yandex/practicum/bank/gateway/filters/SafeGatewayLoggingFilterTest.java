package ru.yandex.practicum.bank.gateway.filters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Тесты для глобального фильтра логирования SafeGatewayLoggingFilter.
 * Использует OutputCaptureExtension для перехвата и верификации текстового вывода (логов).
 * Проверяет форматирование, отсутствие утечек чувствительных данных (токенов) и корректность
 * уровней логирования для разных сценариев (200, 4xx, 5xx, блокировка внутреннего API).
 * </summary>
 **/
@ExtendWith(OutputCaptureExtension.class)
public class SafeGatewayLoggingFilterTest {

    // region Fields

    private final SafeGatewayLoggingFilter filter = new SafeGatewayLoggingFilter();

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что фильтр выполняется с самым низким приоритетом (самым последним).
     * </summary>
     **/
    @Test
    void shouldReturnLowestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    /**
     * <summary>
     * Проверяет успешную обработку (200 OK), правильное формирование лога и то,
     * что чувствительные заголовки (Authorization, Bearer) не попадают в консоль.
     * </summary>
     **/
    @Test
    void shouldLogSuccessfulRelayWithoutSensitiveHeaders(CapturedOutput output) {
        var request = MockServerHttpRequest.get("/api/account/123")
                .header(HttpHeaders.AUTHORIZATION, "Bearer super-secret-user-token")
                .build();

        var exchange = MockServerWebExchange.from(request);

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route("account-service"));

        GatewayFilterChain chain = filteredExchange -> {
            filteredExchange.getResponse().setStatusCode(HttpStatus.OK);
            return filteredExchange.getResponse().setComplete();
        };

        filter.filter(exchange, chain).block();

        assertThat(output.getOut())
                .contains("Gateway request relayed")
                .contains("routeId=account-service")
                .contains("method=GET")
                .contains("path=/api/account/123")
                .contains("targetService=account-service")
                .contains("status=200")
                .doesNotContain("super-secret-user-token")
                .doesNotContain("Authorization")
                .doesNotContain("Bearer")
                .doesNotContain("password")
                .doesNotContain("client_secret");
    }

    /**
     * <summary>
     * Проверяет, что при попытке обратиться к внутреннему API (маршрут block-account-internal-api)
     * пишется лог уровня WARN с соответствующим кодом ошибки.
     * </summary>
     **/
    @Test
    void shouldWarnWhenPublicRouteTargetsInternalApi(CapturedOutput output) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/internal/block").build());

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route("block-account-internal-api"));

        GatewayFilterChain chain = filteredExchange -> {
            filteredExchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return filteredExchange.getResponse().setComplete();
        };

        filter.filter(exchange, chain).block();

        assertThat(output.getOut())
                .contains("Gateway internal api access rejected")
                .contains("routeId=block-account-internal-api")
                .contains("targetService=account-service")
                .contains("status=403")
                .contains("errorCode=INTERNAL_API_PUBLIC_ACCESS");
    }

    /**
     * <summary>
     * Проверяет логирование клиентских ошибок (4xx) на уровне WARN.
     * </summary>
     **/
    @Test
    void shouldLog4xxClientErrorAsWarn(CapturedOutput output) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/cash/limits").build());

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route("cash-service"));

        GatewayFilterChain chain = filteredExchange -> {
            filteredExchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return filteredExchange.getResponse().setComplete();
        };

        filter.filter(exchange, chain).block();

        assertThat(output.getOut())
                .contains("Gateway backend request rejected")
                .contains("routeId=cash-service")
                .contains("status=400")
                .contains("errorCategory=backend_4xx");
    }

    /**
     * <summary>
     * Проверяет логирование серверных ошибок и исключений (5xx) на уровне ERROR.
     * Убеждается, что имя исключения корректно попадает в поле errorType.
     * </summary>
     **/
    @Test
    void shouldLogExceptionAsServerError(CapturedOutput output) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/exchange/rates").build());

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route("exchange-service"));

        GatewayFilterChain chain = filteredExchange -> Mono.error(new IllegalStateException("Connection refused"));

        try {
            filter.filter(exchange, chain).block();
        } catch (Exception ignored) {

        }

        assertThat(output.getOut())
                .contains("Gateway request failed")
                .contains("routeId=exchange-service")
                .contains("targetService=exchange-service")
                .contains("errorCategory=backend_failure")
                .contains("errorType=IllegalStateException");
    }

    /**
     * <summary>
     * Проверяет безопасную обработку запроса, для которого не найден маршрут.
     * (Атрибут GATEWAY_ROUTE_ATTR пуст).
     * </summary>
     **/
    @Test
    void shouldHandleMissingRouteIdSafely(CapturedOutput output) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unknown-path").build());

        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);

        GatewayFilterChain chain = filteredExchange -> filteredExchange.getResponse().setComplete();

        filter.filter(exchange, chain).block();

        assertThat(output.getOut())
                .contains("routeId=unmatched")
                .contains("targetService=unknown")
                .contains("status=404");
    }

    // endregion

    // region Private Helpers

    /**
     * <summary>
     * Вспомогательный метод для генерации объекта маршрута (Route).
     * </summary>
     **/
    private Route route(String routeId) {
        return Route.async()
                .id(routeId)
                .uri(URI.create("http://" + routeId))
                .predicate(exchange -> true)
                .build();
    }

    // endregion
}