package ru.yandex.practicum.bank.shared.providers;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import ru.yandex.practicum.bank.shared.exceptions.ServiceClientException;

/**
 * <summary>
 * Провайдер сервисных OAuth2-токенов доступа (Access Token).
 * Отвечает за получение и обновление межсервисного JWT-токена по схеме Client Credentials Grant для безопасных межсервисных вызовов.
 * </summary>
 **/
public class ServiceTokenProvider {

    // region Fields

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    private final String clientRegistrationId;

    // endregion

    // region Constructors

    public ServiceTokenProvider(
            OAuth2AuthorizedClientManager authorizedClientManager,
            String clientRegistrationId) {
        this.authorizedClientManager = authorizedClientManager;
        this.clientRegistrationId = clientRegistrationId;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Запрашивает и возвращает строковое значение сервисного JWT-токена доступа.
     * </summary>
     * <return>
     * @return Строковое значение JWT-токена доступа (Bearer Token).
     * </return>
     * @throws ServiceClientException Если не удалось пройти авторизацию или получить токен.
     **/
    public String getAccessToken() {
        var authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistrationId)
                .principal(clientRegistrationId)
                .build();

        var authorizedClient = authorizedClientManager.authorize(authorizeRequest);

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new ServiceClientException("Service token request failed for: " + clientRegistrationId);
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    // endregion
}