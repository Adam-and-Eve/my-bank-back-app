package ru.yandex.practicum.bank.gateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * <summary>
 * Глобальный фильтр Spring Cloud Gateway для безопасного и структурированного логирования проксируемых HTTP-запросов.
 * Отслеживает маршрутизацию (routeId), целевые микросервисы, статусы ответов и перехватывает ошибки,
 * обеспечивая единый формат логов для систем мониторинга и трассировки.
 * </summary>
 **/
@Component
public class SafeGatewayLoggingFilter implements GlobalFilter, Ordered {

    // region Constants

    private static final Logger log = LoggerFactory.getLogger(SafeGatewayLoggingFilter.class);

    // endregion

    // region Methods

    /**
     * <summary>
     * Устанавливает минимальный приоритет выполнения фильтра (LOWEST_PRECEDENCE).
     * Это гарантирует, что фильтр отработает последним на этапе pre-filter (когда маршрут уже точно определен)
     * и самым первым на этапе post-filter (при возврате ответа от микросервиса).
     * </summary>
     **/
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * <summary>
     * Основной метод перехвата запросов. Логирует метаданные входящего запроса на уровне DEBUG
     * и подписывается на события завершения (успех/ошибка) для финального логирования результата работы шлюза.
     * </summary>
     **/
    @Override
    public reactor.core.publisher.Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        String routeId = route == null ? "unmatched" : route.getId();

        String method = exchange.getRequest().getMethod().name();

        String path = exchange.getRequest().getPath().pathWithinApplication().value();

        if (log.isDebugEnabled()) {
            log.debug(
                    "Gateway route selected routeId={} method={} path={} source=api-gateway targetService={}",
                    routeId,
                    method,
                    path,
                    targetService(routeId)
            );
        }

        return chain.filter(exchange)
                .doOnSuccess(ignored -> logCompleted(exchange, routeId, method, path, null))
                .doOnError(error -> logCompleted(exchange, routeId, method, path, error));
    }

    // endregion

    // region Private Methods

    /**
     * <summary>
     * Анализирует результат выполнения запроса и записывает лог с соответствующим уровнем (INFO, WARN, ERROR).
     * Отдельно классифицирует серверные ошибки (5xx), клиентские ошибки (4xx) и
     * попытки несанкционированного доступа к защищенным внутренним API.
     * </summary>
     **/
    private void logCompleted(
            org.springframework.web.server.ServerWebExchange exchange,
            String routeId,
            String method,
            String path,
            Throwable error
    ) {
        int status = exchange.getResponse().getStatusCode() == null
                ? HttpStatus.OK.value()
                : exchange.getResponse().getStatusCode().value();

        String targetService = targetService(routeId);

        if (error != null || status >= 500) {
            log.error(
                    "Gateway request failed routeId={} method={} path={} targetService={} status={} errorCategory=backend_failure errorType={} source=api-gateway",
                    routeId,
                    method,
                    path,
                    targetService,
                    status,
                    error == null ? "Backend5xx" : error.getClass().getSimpleName()
            );
            return;
        }

        if ("block-account-internal-api".equals(routeId)) {
            log.warn(
                    "Gateway internal api access rejected routeId={} method={} path={} targetService={} status={} errorCode=INTERNAL_API_PUBLIC_ACCESS source=api-gateway",
                    routeId,
                    method,
                    path,
                    targetService,
                    status
            );
            return;
        }

        if (status >= 400) {
            log.warn(
                    "Gateway backend request rejected routeId={} method={} path={} targetService={} status={} errorCategory=backend_4xx source=api-gateway",
                    routeId,
                    method,
                    path,
                    targetService,
                    status
            );
            return;
        }

        log.info(
                "Gateway request relayed routeId={} method={} path={} targetService={} status={} source=api-gateway",
                routeId,
                method,
                path,
                targetService,
                status
        );
    }

    /**
     * <summary>
     * Утилитный метод для преобразования идентификатора маршрута (routeId) в понятное имя целевого микросервиса.
     * </summary>
     **/
    private String targetService(String routeId) {
        return switch (routeId) {
            case "account-service" -> "account-service";
            case "cash-service" -> "cash-service";
            case "transfer-service" -> "transfer-service";
            case "exchange-service" -> "exchange-service";
            case "block-account-internal-api" -> "account-service";
            default -> "unknown";
        };
    }

    // endregion
}