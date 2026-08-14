package ru.yandex.practicum.bank.cash.providers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import ru.yandex.practicum.bank.cash.exceptions.AccountClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Модульные тесты для провайдера сервисных токенов ServiceTokenProvider.
 * Проверяют корректность формирования запроса OAuth2AuthorizeRequest,
 * извлечение токена доступа и обработку ошибок при неудачной авторизации.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class ServiceTokenProviderTest {

    // region Constants

    private static final String CLIENT_REGISTRATION_ID = "cash-service";

    private static final String PRINCIPAL_NAME = "cash-service";

    private static final String EXPECTED_TOKEN_VALUE = "mocked-jwt-access-token";

    // endregion

    // region Fields

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @InjectMocks
    private ServiceTokenProvider serviceTokenProvider;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешное получение токена доступа и точное соответствие параметров запроса
     * (clientRegistrationId и principal) ожидаемым константам.
     * </summary>
     **/
    @Test
    public void shouldReturnAccessTokenSuccessfully() {
        var authorizedClient = mock(OAuth2AuthorizedClient.class);

        var accessToken = mock(OAuth2AccessToken.class);

        when(accessToken.getTokenValue()).thenReturn(EXPECTED_TOKEN_VALUE);

        when(authorizedClient.getAccessToken()).thenReturn(accessToken);

        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient);

        String token = serviceTokenProvider.getAccessToken();

        assertThat(token).isEqualTo(EXPECTED_TOKEN_VALUE);

        var captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);

        verify(authorizedClientManager).authorize(captor.capture());

        OAuth2AuthorizeRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.getClientRegistrationId()).isEqualTo(CLIENT_REGISTRATION_ID);

        assertThat(capturedRequest.getPrincipal().getName()).isEqualTo(PRINCIPAL_NAME);
    }

    /**
     * <summary>
     * Проверяет выброс исключения AccountClientException, если менеджер авторизации
     * возвращает null (клиент не авторизован).
     * </summary>
     **/
    @Test
    public void shouldThrowAccountClientExceptionWhenAuthorizedClientIsNull() {
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> serviceTokenProvider.getAccessToken())
                .isInstanceOf(AccountClientException.class)
                .hasMessage("Service token request failed");
    }

    // endregion
}