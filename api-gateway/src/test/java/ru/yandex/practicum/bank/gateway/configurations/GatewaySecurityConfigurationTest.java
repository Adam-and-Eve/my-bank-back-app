package ru.yandex.practicum.bank.gateway.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

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
     * Проверяет, что внутренний эндпоинт аккаунтов заблокирован:
     * перехватывает путь /api/account/internal/**, имеет целевой URI no://op и возвращает статус 404 (SetStatus).
     * </summary>
     **/
    @Test
    void shouldBlockInternalAccountApiWithStatus404() {
        var routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();

        assertThat(routes)
                .filteredOn(route -> "block-account-internal-api".equals(route.getId()))
                .singleElement()
                .satisfies(route -> {
                    assertThat(route.getUri().toString()).isEqualTo("no://op");
                    assertThat(route.getPredicates())
                            .anySatisfy(predicate -> assertThat(predicate.getArgs())
                                    .containsValue("/api/account/internal/**"));
                    assertThat(route.getFilters())
                            .anySatisfy(filter -> assertThat(filter.getName()).isEqualTo("SetStatus"));
                });
    }

    /**
     * <summary>
     * Проверяет, что маршрут для account-service корректно сконфигурирован:
     * направляет трафик на lb://account-service, обрабатывает путь /api/account/**
     * и содержит фильтр JwtTokenRelay.
     * </summary>
     **/
    @Test
    void shouldRouteAccountToAccountServiceWithTokenRelay() {
        var routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();

        assertThat(routes)
                .filteredOn(route -> "account-service".equals(route.getId()))
                .singleElement()
                .satisfies(route -> {
                    assertThat(route.getUri().toString()).isEqualTo("lb://account-service");
                    assertThat(route.getPredicates())
                            .anySatisfy(predicate -> assertThat(predicate.getArgs())
                                    .containsValue("/api/account/**"));
                    assertThat(route.getFilters())
                            .anySatisfy(filter -> assertThat(filter.getName()).isEqualTo("JwtTokenRelay"));
                });
    }

    /**
     * <summary>
     * Проверяет, что маршрут для cash-service корректно сконфигурирован:
     * направляет трафик на lb://cash-service, обрабатывает путь /api/cash/**
     * и содержит фильтр JwtTokenRelay.
     * </summary>
     **/
    @Test
    void shouldRouteCashToCashServiceWithTokenRelay() {
        var routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();

        assertThat(routes)
                .filteredOn(route -> "cash-service".equals(route.getId()))
                .singleElement()
                .satisfies(route -> {
                    assertThat(route.getUri().toString()).isEqualTo("lb://cash-service");
                    assertThat(route.getPredicates())
                            .anySatisfy(predicate -> assertThat(predicate.getArgs())
                                    .containsValue("/api/cash/**"));
                    assertThat(route.getFilters())
                            .anySatisfy(filter -> assertThat(filter.getName()).isEqualTo("JwtTokenRelay"));
                });
    }

    /**
     * <summary>
     * Проверяет, что маршрут для transfer-service корректно сконфигурирован:
     * направляет трафик на lb://transfer-service, перехватывает точный путь /api/transfer
     * и содержит фильтр JwtTokenRelay.
     * </summary>
     **/
    @Test
    void shouldRouteTransfersToTransferServiceWithTokenRelay() {
        var routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();

        assertThat(routes)
                .filteredOn(route -> "transfer-service".equals(route.getId()))
                .singleElement()
                .satisfies(route -> {
                    assertThat(route.getUri().toString()).isEqualTo("lb://transfer-service");
                    assertThat(route.getPredicates())
                            .anySatisfy(predicate -> assertThat(predicate.getArgs())
                                    .containsValue("/api/transfer")
                                    .doesNotContainValue("/api/transfer/**"));
                    assertThat(route.getFilters())
                            .anySatisfy(filter -> assertThat(filter.getName()).isEqualTo("JwtTokenRelay"));
                });
    }

    // endregion
}