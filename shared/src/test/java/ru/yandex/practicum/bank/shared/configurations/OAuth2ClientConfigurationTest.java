package ru.yandex.practicum.bank.shared.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * <summary>
 * Тесты конфигурации OAuth2ClientConfiguration.
 * Проверяют корректность создания и регистрации бина OAuth2AuthorizedClientManager
 * для управления авторизованными OAuth2-клиентами.
 * </summary>
 **/
public class OAuth2ClientConfigurationTest {

    // region Tests

    /**
     * <summary>
     * Проверяет, что бин OAuth2AuthorizedClientManager успешно создается
     * и регистрируется в Spring-контексте.
     * </summary>
     **/
    @Test
    public void shouldRegisterAuthorizedClientManagerBean() {
        var context = new AnnotationConfigApplicationContext();

        var repositoryMock = mock(ClientRegistrationRepository.class);

        var serviceMock = mock(OAuth2AuthorizedClientService.class);

        context.registerBean(ClientRegistrationRepository.class, () -> repositoryMock);

        context.registerBean(OAuth2AuthorizedClientService.class, () -> serviceMock);

        context.register(OAuth2ClientConfiguration.class);

        context.refresh();

        var manager = context.getBean(OAuth2AuthorizedClientManager.class);

        assertThat(manager).isNotNull();

        assertThat(manager)
                .isInstanceOf(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);

        context.close();
    }

    /**
     * <summary>
     * Проверяет создание AuthorizedClientServiceOAuth2AuthorizedClientManager
     * при передаче необходимых зависимостей в фабричный метод конфигурации.
     * </summary>
     **/
    @Test
    public void shouldCreateAuthorizedClientManagerWithDependencies() {
        var configuration = new OAuth2ClientConfiguration();

        var repositoryMock = mock(ClientRegistrationRepository.class);

        var serviceMock = mock(OAuth2AuthorizedClientService.class);

        var manager = configuration.authorizedClientManager(
                repositoryMock,
                serviceMock
        );

        assertThat(manager).isNotNull();

        assertThat(manager)
                .isInstanceOf(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);
    }

    // endregion

}