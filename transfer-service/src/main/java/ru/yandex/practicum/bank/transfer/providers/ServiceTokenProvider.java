package ru.yandex.practicum.bank.transfer.providers;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.transfer.exceptions.AccountClientException;

/**
 * <summary>
 * Провайдер сервисных OAuth2-токенов доступа (Access Token).
 * Отвечает за получение и обновление межсервисного JWT-токена по схеме Client Credentials Grant для безопасных межсервисных вызовов.
 * </summary>
 **/
@Component
public class ServiceTokenProvider {

    // region Fields

    private static final String CLIENT_REGISTRATION_ID = "transfer-service";
    private static final String PRINCIPAL_NAME = "transfer-service";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    // endregion

    // region Constructors

    public ServiceTokenProvider(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
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
     * @throws AccountClientException Если не удалось пройти авторизацию или получить токен.
     **/
    public String getAccessToken() {
        var authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(CLIENT_REGISTRATION_ID)
                .principal(PRINCIPAL_NAME)
                .build();

        var authorizedClient = authorizedClientManager.authorize(authorizeRequest);

        if (authorizedClient == null) {
            throw new AccountClientException("Service token request failed");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    // endregion
}