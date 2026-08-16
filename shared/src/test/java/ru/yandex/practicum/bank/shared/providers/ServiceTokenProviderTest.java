package ru.yandex.practicum.bank.shared.providers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import ru.yandex.practicum.bank.shared.exceptions.ServiceClientException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Тесты ServiceTokenProvider.
 * Проверяют получение сервисного OAuth2-токена через OAuth2AuthorizedClientManager
 * и обработку ошибок при невозможности получить токен.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class ServiceTokenProviderTest {

    // region Constants

    private static final String CLIENT_REGISTRATION_ID = "transfer-service";

    private static final String ACCESS_TOKEN = "test-access-token";

    // endregion

    // region Fields

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешное получение сервисного Access Token.
     * </summary>
     **/
    @Test
    public void shouldReturnAccessToken() {
        var accessToken = createAccessToken();

        when(authorizedClientManager.authorize(org.mockito.ArgumentMatchers.any()))
                .thenReturn(authorizedClient);

        when(authorizedClient.getAccessToken())
                .thenReturn(accessToken);

        var provider = new ServiceTokenProvider(
                authorizedClientManager,
                CLIENT_REGISTRATION_ID
        );

        var result = provider.getAccessToken();

        assertThat(result).isEqualTo(ACCESS_TOKEN);

        verify(authorizedClientManager)
                .authorize(org.mockito.ArgumentMatchers.any());
    }

    /**
     * <summary>
     * Проверяет, что при запросе токена используется переданный clientRegistrationId
     * и в качестве principal используется тот же идентификатор клиента.
     * </summary>
     **/
    @Test
    public void shouldUseClientRegistrationIdAsPrincipal() {
        when(authorizedClientManager.authorize(org.mockito.ArgumentMatchers.any()))
                .thenReturn(authorizedClient);

        when(authorizedClient.getAccessToken())
                .thenReturn(createAccessToken());

        var provider = new ServiceTokenProvider(
                authorizedClientManager,
                CLIENT_REGISTRATION_ID
        );

        provider.getAccessToken();

        var captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);

        verify(authorizedClientManager).authorize(captor.capture());

        var authorizeRequest = captor.getValue();

        assertThat(authorizeRequest.getClientRegistrationId())
                .isEqualTo(CLIENT_REGISTRATION_ID);

        assertThat(authorizeRequest.getPrincipal().getName())
                .isEqualTo(CLIENT_REGISTRATION_ID);
    }

    /**
     * <summary>
     * Проверяет, что при отсутствии авторизованного клиента выбрасывается ServiceClientException.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenAuthorizedClientIsNull() {
        when(authorizedClientManager.authorize(org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);

        var provider = new ServiceTokenProvider(
                authorizedClientManager,
                CLIENT_REGISTRATION_ID
        );

        assertThatThrownBy(provider::getAccessToken)
                .isInstanceOf(ServiceClientException.class)
                .hasMessage("Service token request failed for: " + CLIENT_REGISTRATION_ID);

        verify(authorizedClientManager).authorize(org.mockito.ArgumentMatchers.any());
    }

    /**
     * <summary>
     * Проверяет, что при отсутствии Access Token выбрасывается ServiceClientException.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenAccessTokenIsNull() {
        when(authorizedClientManager.authorize(org.mockito.ArgumentMatchers.any()))
                .thenReturn(authorizedClient);

        when(authorizedClient.getAccessToken())
                .thenReturn(null);

        var provider = new ServiceTokenProvider(
                authorizedClientManager,
                CLIENT_REGISTRATION_ID
        );

        assertThatThrownBy(provider::getAccessToken)
                .isInstanceOf(ServiceClientException.class)
                .hasMessage("Service token request failed for: " + CLIENT_REGISTRATION_ID);

        verify(authorizedClientManager).authorize(org.mockito.ArgumentMatchers.any());

        verify(authorizedClient).getAccessToken();
    }

    // endregion

    // region Helpers

    private OAuth2AccessToken createAccessToken() {
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                ACCESS_TOKEN,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
    }

    // endregion
}