package ru.yandex.practicum.bank.cash.providers;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.cash.exceptions.AccountClientException;

/**
 * <summary>
 * Провайдер сервисных OAuth2-токенов доступа (Access Tokens).
 * Отвечает за получение токена безопасности от имени cash-service
 * для выполнения авторизованных межсервисных запросов по схеме Client Credentials.
 * </summary>
 **/
@Component
public class ServiceTokenProvider {

    // region Constants

    private static final String CLIENT_REGISTRATION_ID = "cash-service";
    private static final String PRINCIPAL_NAME = "cash-service";

    // endregion

    // region Fields

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
     * Запрашивает и возвращает актуальный OAuth2 Access Token для взаимодействия с другими сервисами.
     * </summary>
     * <return>
     * @return Строковое значение токена доступа (JWT).
     * </return>
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