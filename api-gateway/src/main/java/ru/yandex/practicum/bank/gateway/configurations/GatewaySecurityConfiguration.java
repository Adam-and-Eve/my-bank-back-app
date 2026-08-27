package ru.yandex.practicum.bank.gateway.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.bank.gateway.viewmodels.ApiErrorResponseViewModel;

/**
 * <summary>
 * Конфигурационный класс Spring Security WebFlux для API-шлюза (api-gateway).
 * Отвечает за настройку правил доступа к маршрутам, валидацию JWT-токенов через OAuth2 Resource Server,
 * а также переопределение кастомных обработчиков ошибок аутентификации и авторизации.
 * </summary>
 **/
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    // region Beans

    /**
     * <summary>
     * Настраивает реактивную цепочку фильтров безопасности Spring Security (SecurityWebFilterChain).
     * Отключает CSRF, разрешает публичный доступ к Actuator-эндпоинтам, требует авторизацию для маршрутов /api/**,
     * блокирует остальные запросы, кастомизирует обработку ошибок 401/403 и подключает проверку JWT-токенов.
     * </summary>
     * @param http Построитель конфигурации реактивной HTTP-безопасности ServerHttpSecurity.
     * @param authenticationEntryPoint Обработчик ошибок отсутствия или недействительности аутентификации (401).
     * @param accessDeniedHandler Обработчик ошибок недостаточности прав доступа (403).
     * <return>
     * @return Сконфигурированная и собранная цепочка фильтров SecurityWebFilterChain.
     * </return>
     **/
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ServerAuthenticationEntryPoint authenticationEntryPoint,
            ServerAccessDeniedHandler accessDeniedHandler
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().denyAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * <summary>
     * Создает бин ServerAuthenticationEntryPoint для обработка ошибок аутентификации (HTTP 401 Unauthorized).
     * Формирует унифицированный JSON-ответ с кодом UNAUTHORIZED.
     * </summary>
     * @param objectMapper Объект ObjectMapper для сериализации модели ошибки в JSON.
     * <return>
     * @return Реализация ServerAuthenticationEntryPoint, возвращающая кастомный JSON-ответ об ошибке.
     * </return>
     **/
    @Bean
    public ServerAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (exchange, exception) -> writeError(
                exchange,
                objectMapper,
                HttpStatus.UNAUTHORIZED,
                new ApiErrorResponseViewModel("UNAUTHORIZED", "Требуется авторизация")
        );
    }

    /**
     * <summary>
     * Создает бин ServerAccessDeniedHandler для обработки ошибок доступа (HTTP 403 Forbidden).
     * Формирует унифицированный JSON-ответ с кодом FORBIDDEN.
     * </summary>
     * @param objectMapper Объект ObjectMapper для сериализации модели ошибки в JSON.
     * <return>
     * @return Реализация ServerAccessDeniedHandler, возвращающая кастомный JSON-ответ об ошибке.
     * </return>
     **/
    @Bean
    public ServerAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (exchange, exception) -> writeError(
                exchange,
                objectMapper,
                HttpStatus.FORBIDDEN,
                new ApiErrorResponseViewModel("FORBIDDEN", "Недостаточно прав для выполнения операции")
        );
    }

    /**
     * <summary>
     * Вспомогательный метод для асинхронной записи структуры ошибки ApiErrorResponseViewModel
     * в тело HTTP-ответа в формате JSON.
     * </summary>
     * @param exchange Реактивный контекст ServerWebExchange.
     * @param objectMapper Сериализатор JSON.
     * @param status Устанавливаемый HTTP-статус ответа.
     * @param error Модель ответа об ошибке ApiErrorResponseViewModel.
     * <return>
     * @return Mono&lt;Void&gt;, завершаемый после записи данных в буфер ответа.
     * </return>
     **/
    private Mono<Void> writeError(
            ServerWebExchange exchange,
            ObjectMapper objectMapper,
            HttpStatus status,
            ApiErrorResponseViewModel error
    ) {
        var response = exchange.getResponse();

        response.setStatusCode(status);

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            var bytes = objectMapper.writeValueAsBytes(error);

            var buffer = response.bufferFactory().wrap(bytes);

            return response.writeWith(Mono.just(buffer));

        } catch (Exception ex) {

            return response.setComplete();
        }
    }

    // endregion
}