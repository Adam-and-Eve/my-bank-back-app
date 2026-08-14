package ru.yandex.practicum.bank.account;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * <summary>
 * Интеграционный тест для проверки корректности загрузки контекста Spring-приложения (AccountServiceApplicationTests).
 * Подтверждает успешную инициализацию всех бинов и конфигураций микросервиса account-service.
 * </summary>
 **/
@SpringBootTest
public class AccountServiceApplicationTests {

    // region Fields

    /**
     * <summary>
     * Мок-объект {@link JwtDecoder} для имитации работы OAuth2 Resource Server
     * и исключения внешних вызовов к Identity Provider (Keycloak) при поддомене контекста.
     * </summary>
     **/
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что контекст Spring-приложения успешно поднимается без ошибок конфигурации и сбоев инициализации бинов.
     * </summary>
     **/
    @Test
    public void contextLoads() {

    }

    // endregion
}