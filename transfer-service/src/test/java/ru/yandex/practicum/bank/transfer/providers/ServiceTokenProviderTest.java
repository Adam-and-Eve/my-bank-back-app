package ru.yandex.practicum.bank.transfer.providers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import ru.yandex.practicum.bank.transfer.exceptions.AccountClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Модульные тесты для провайдера сервисных токенов ServiceTokenProvider.
 * Проверяют корректность формирования OAuth2AuthorizeRequest, извлечение Bearer-токена
 * и обработку ошибок взаимодействия с OAuth2AuthorizedClientManager.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class ServiceTokenProviderTest {

    // region Constants

    private static final String CLIENT_REGISTRATION_ID = "transfer-service";

    private static final String PRINCIPAL_NAME = "transfer-service";

    private static final String EXPECTED_TOKEN = "mock-service-jwt-access-token";

    // endregion

    // region Fields

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    private ServiceTokenProvider serviceTokenProvider;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        serviceTokenProvider = new ServiceTokenProvider(authorizedClientManager);
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешное получение сервисного JWT-токена доступа и валидность параметров запроса авторизации.
     * </summary>
     **/
    @Test
    public void shouldReturnAccessTokenSuccessfully() {
        var accessToken = mock(OAuth2AccessToken.class);

        when(accessToken.getTokenValue()).thenReturn(EXPECTED_TOKEN);

        var authorizedClient = mock(OAuth2AuthorizedClient.class);

        when(authorizedClient.getAccessToken()).thenReturn(accessToken);

        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient);

        var token = serviceTokenProvider.getAccessToken();

        assertThat(token).isEqualTo(EXPECTED_TOKEN);

        var captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);

        verify(authorizedClientManager).authorize(captor.capture());

        var request = captor.getValue();

        assertThat(request.getClientRegistrationId()).isEqualTo(CLIENT_REGISTRATION_ID);

        assertThat(request.getPrincipal().getName()).isEqualTo(PRINCIPAL_NAME);
    }

    /**
     * <summary>
     * Проверяет выброс бизнес-исключения AccountClientException, если менеджер авторизации возвращает null.
     * </summary>
     **/
    @Test
    public void shouldThrowAccountClientExceptionWhenAuthorizedClientIsNull() {
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> serviceTokenProvider.getAccessToken())
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Service token request failed");
    }

    /**
     * <summary>
     * Проверяет проброс исключения, если менеджер авторизации выбрасывает ошибку при сбое Identity Provider (Keycloak / OAuth2 Server).
     * </summary>
     **/
    @Test
    public void shouldPropagateExceptionWhenAuthorizationManagerFails() {
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenThrow(new RuntimeException("OAuth2 authorization server unavailable"));

        assertThatThrownBy(() -> serviceTokenProvider.getAccessToken())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OAuth2 authorization server unavailable");
    }

    // endregion
}