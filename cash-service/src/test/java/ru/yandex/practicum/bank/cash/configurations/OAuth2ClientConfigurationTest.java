package ru.yandex.practicum.bank.cash.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.bank.cash.interfaces.CashService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Интеграционные тесты конфигурации OAuth2-клиента OAuth2ClientConfiguration.
 * Проверяют корректность поднятия контекста Spring, создание и настройку бина OAuth2AuthorizedClientManager.
 * </summary>
 **/
@SpringBootTest
public class OAuth2ClientConfigurationTest {

    // region Fields

    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private CashService cashService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что бин OAuth2AuthorizedClientManager успешно создается в контексте Spring
     * и инициализируется конкретной реализацией AuthorizedClientServiceOAuth2AuthorizedClientManager.
     * </summary>
     **/
    @Test
    public void shouldCreateAuthorizedClientManagerBean() {
        assertThat(authorizedClientManager).isNotNull();

        assertThat(authorizedClientManager)
                .isInstanceOf(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);
    }

    // endregion
}