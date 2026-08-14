package ru.yandex.practicum.bank.frontui.helpers;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayClientException;

import java.security.Principal;

/**
 * <summary>
 * Вспомогательный компонент для извлечения данных пользователя и токенов из контекста безопасности.
 * </summary>
 **/
@Component
public class SecurityUserContextHelper {

    // region Fields

    private static final String CLIENT_REGISTRATION_ID = "front-ui-service";

    private final OAuth2AuthorizedClientService authorizedClientService;

    // endregion

    // region Constructors

    public SecurityUserContextHelper(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Извлекает OAuth2 Access Token для текущего аутентифицированного пользователя.
     * </summary>
     **/
    public String getAccessToken(Authentication authentication) {
        if (authentication == null) {
            throw new GatewayClientException("Пользователь не аутентифицирован");
        }

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                CLIENT_REGISTRATION_ID,
                authentication.getName()
        );

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new GatewayClientException("OAuth2 клиент не авторизован");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    /**
     * <summary>
     * Извлекает отображаемое имя пользователя из Principal / OidcUser.
     * </summary>
     **/
    public String getUsername(Principal principal) {
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getPreferredUsername();
        }

        if (principal instanceof Authentication authentication && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getPreferredUsername();
        }

        return principal == null ? "" : principal.getName();
    }

    // endregion
}