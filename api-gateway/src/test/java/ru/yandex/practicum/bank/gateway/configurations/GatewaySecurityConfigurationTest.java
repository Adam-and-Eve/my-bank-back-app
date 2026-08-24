package ru.yandex.practicum.bank.gateway.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Интеграционные тесты для проверки корректности загрузки конфигурации маршрутов Spring Cloud Gateway
 * и наличия фильтров трансляции токенов (JwtTokenRelay) и блокировки внутренних эндпоинтов.
 * </summary>
 **/
@SpringBootTest
public class GatewaySecurityConfigurationTest {

    // region Fields

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что backend-маршруты корректно направляют запросы на соответствующие сервисы
     * и содержат фильтр JwtTokenRelay.
     * </summary>
     **/
    @Test
    void shouldRouteBackendRequestsToServiceDnsTargetsWithTokenRelay() {
        var routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();

        Map<String, String> expectedUris = Map.of(
                "account-service", "http://account-service:8081",
                "cash-service", "http://cash-service:8082",
                "transfer-service", "http://transfer-service:8083",
                "exchange-service", "http://exchange-service:8086"
        );

        expectedUris.forEach((routeId, uri) -> assertThat(routes)
                .filteredOn(route -> routeId.equals(route.getId()))
                .singleElement()
                .satisfies(route -> {
                    assertThat(route.getUri().toString()).isEqualTo(uri);
                    assertThat(route.getUri().getScheme()).isEqualTo("http");
                    assertThat(route.getFilters())
                            .anySatisfy(filter ->
                                    assertThat(filter.getName()).isEqualTo("JwtTokenRelay"));
                }));
    }

    /**
     * <summary>
     * Проверяет, что маршрут transfer-service использует точный путь /api/transfer
     * без wildcard-варианта и что внутренний API аккаунтов заблокирован,
     * имеет целевой URI no://op и возвращает статус 404.
     * </summary>
     **/
    @Test
    void shouldKeepTransferRouteExactAndBlockAccountInternalApi() {
        var routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();

        assertThat(routes)
                .filteredOn(route -> "transfer-service".equals(route.getId()))
                .singleElement()
                .satisfies(route -> assertThat(route.getPredicates())
                        .anySatisfy(predicate -> assertThat(predicate.getArgs())
                                .containsValue("/api/transfer")
                                .doesNotContainValue("/api/transfer/**")));

        assertThat(routes)
                .filteredOn(route -> "block-account-internal-api".equals(route.getId()))
                .singleElement()
                .satisfies(route -> {
                    assertThat(route.getUri().toString()).isEqualTo("no://op");
                    assertThat(route.getPredicates())
                            .anySatisfy(predicate -> assertThat(predicate.getArgs())
                                    .containsValue("/api/account/internal/**"));
                    assertThat(route.getFilters())
                            .anySatisfy(filter ->
                                    assertThat(filter.getName()).isEqualTo("SetStatus"));
                });
    }

    // endregion
}