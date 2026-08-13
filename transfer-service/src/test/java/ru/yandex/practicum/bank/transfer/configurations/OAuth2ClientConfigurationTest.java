package ru.yandex.practicum.bank.transfer.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * <summary>
 * Тесты конфигурации OAuth2ClientConfiguration.
 * Проверяют корректность создания и регистрации бина OAuth2AuthorizedClientManager для управления авторизованными OAuth2-клиентами.
 * </summary>
 **/
@SpringBootTest
public class OAuth2ClientConfigurationTest {

    // region Fields

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что бин OAuth2AuthorizedClientManager успешно создается, регистрируется в контексте Spring
     * и является экземпляром AuthorizedClientServiceOAuth2AuthorizedClientManager.
     * </summary>
     **/
    @Test
    public void shouldRegisterAuthorizedClientManagerBean() {
        assertThat(authorizedClientManager).isNotNull();

        var bean = applicationContext.getBean(OAuth2AuthorizedClientManager.class);

        assertThat(bean).isSameAs(authorizedClientManager);

        assertThat(bean).isInstanceOf(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);
    }

    /**
     * <summary>
     * Модульный тест фабричного метода конфигурации: проверяет создание и корректную инициализацию
     * AuthorizedClientServiceOAuth2AuthorizedClientManager при передаче зависимостей.
     * </summary>
     **/
    @Test
    public void shouldCreateAuthorizedClientManagerWithDependencies() {
        var configuration = new OAuth2ClientConfiguration();

        var repositoryMock = mock(ClientRegistrationRepository.class);

        var serviceMock = mock(OAuth2AuthorizedClientService.class);

        var manager = configuration.authorizedClientManager(repositoryMock, serviceMock);

        assertThat(manager).isNotNull();

        assertThat(manager).isInstanceOf(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);
    }

    // endregion
}