package ru.yandex.practicum.bank.cash.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.cash.interfaces.CashService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Интеграционные тесты конфигурации RestClientConfiguration.
 * Проверяют корректность инициализации бина RestClient.Builder с поддержкой балансировки нагрузки в контексте Spring Boot.
 * </summary>
 **/
@SpringBootTest
public class RestClientConfigurationTest {

    // region Fields

    @Autowired
    private RestClient.Builder restClientBuilder;

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
     * Проверяет, что бин RestClient.Builder успешно создается и внедряется из контекста Spring.
     * </summary>
     **/
    @Test
    public void shouldCreateRestClientBuilderBean() {
        assertThat(restClientBuilder).isNotNull();
    }

    /**
     * <summary>
     * Проверяет, что внедренный RestClient.Builder корректно создает готовый экземпляр RestClient.
     * </summary>
     **/
    @Test
    public void shouldBuildRestClientInstance() {
        RestClient restClient = restClientBuilder.build();

        assertThat(restClient).isNotNull();
    }

    // endregion
}