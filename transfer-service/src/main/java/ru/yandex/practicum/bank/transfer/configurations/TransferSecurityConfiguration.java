package ru.yandex.practicum.bank.transfer.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import ru.yandex.practicum.bank.shared.helpers.ResourceServerSecurityHelper;

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
     * <return>
     * @return Сконфигурированный экземпляр SecurityFilterChain.
     * </return>
     **/
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper
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
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(ResourceServerSecurityHelper.authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(ResourceServerSecurityHelper.accessDeniedHandler(objectMapper))
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                ResourceServerSecurityHelper.jwtAuthenticationConverter()
                        ))
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }

    // endregion
}