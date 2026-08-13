package ru.yandex.practicum.bank.transfer.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;


/**
 * <summary>
 * Конфигурация клиентского взаимодействия по протоколу OAuth2 для сервиса переводов (Transfer Service).
 * Настраивает менеджер авторизованных клиентов OAuth2 для автоматического получения и обновления сервисных JWT-токенов
 * по схеме Client Credentials Grant при межсервисных вызовах.
 * </summary>
 **/
@Configuration
public class OAuth2ClientConfiguration {

    // region Beans

    /**
     * <summary>
     * Создает и конфигурирует менеджер OAuth2AuthorizedClientManager с поддержкой Client Credentials Provider.
     * </summary>
     * @param clientRegistrationRepository Репозиторий регистраций OAuth2-клиентов.
     * @param authorizedClientService Сервис хранения и управления авторизованными OAuth2-клиентами.
     * <return>
     * @return Сконфигурированный экземпляр OAuth2AuthorizedClientManager.
     * </return>
     **/
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService
        );

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    // endregion
}