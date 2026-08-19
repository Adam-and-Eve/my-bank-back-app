package ru.yandex.practicum.bank.account.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.account.exceptions.MissingPreferredUsernameException;
import ru.yandex.practicum.bank.account.interfaces.AccountService;
import ru.yandex.practicum.bank.account.viewmodels.AccountResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.RecipientResponseViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Интеграционные тесты для REST-контроллера AccountController.
 * Проверяют обработку HTTP-запросов получения и обновления профиля пользователя,
 * списка получателей, валидацию DTO и работу логики извлечения preferred_username из JWT.
 * </summary>
 **/
@SpringBootTest
@AutoConfigureMockMvc
public class AccountControllerTest {

    // region Constants

    private static final String ME_URL = "/api/account/me";
    private static final String RECIPIENTS_URL = "/api/account/recipients";

    private static final String VALID_UPDATE_BODY = """
            {
                "name": "Дмитрий Волков",
                "birthdate": "1999-09-19"
            }
            """;

    private static final String INVALID_UPDATE_BODY = """
            {
                "name": "",
                "birthdate": null
            }
            """;

    // endregion

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешное получение информации о текущем счёте пользователя (HTTP 200 OK).
     * </summary>
     **/
    @Test
    public void shouldReturnCurrentAccountWhenAuthenticated() throws Exception {
        var response = new AccountResponseViewModel(
                "dmitry",
                "Дмитрий Волков",
                LocalDate.of(1999, 9, 19),
                new BigDecimal("1000000.00"),
                "RUB"
        );

        when(accountService.getCurrentAccount("dmitry")).thenReturn(response);

        mockMvc.perform(get(ME_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_READ")
                                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("dmitry"))
                .andExpect(jsonPath("$.name").value("Дмитрий Волков"))
                .andExpect(jsonPath("$.birthdate[0]").value(1999))
                .andExpect(jsonPath("$.birthdate[1]").value(9))
                .andExpect(jsonPath("$.birthdate[2]").value(19))
                .andExpect(jsonPath("$.balance").value(1000000.00))
                .andExpect(jsonPath("$.currency").value("RUB"));

        verify(accountService).getCurrentAccount("dmitry");
    }

    /**
     * <summary>
     * Проверяет выброс исключения MissingPreferredUsernameException, если в JWT отсутствует claim preferred_username.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenGetMeWithoutPreferredUsername() throws Exception {
        mockMvc.perform(get(ME_URL)
                        .with(jwt()
                                .jwt(token -> { /* claim preferred_username не задан */ })
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_READ")
                                )))
                .andExpect(result -> assertInstanceOf(MissingPreferredUsernameException.class, result.getResolvedException()));
    }

    /**
     * <summary>
     * Проверяет выброс исключения MissingPreferredUsernameException, если claim preferred_username содержит пустую строку.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenGetMeWithBlankPreferredUsername() throws Exception {
        mockMvc.perform(get(ME_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "   "))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_READ")
                                )))
                .andExpect(result -> assertInstanceOf(MissingPreferredUsernameException.class, result.getResolvedException()));
    }

    /**
     * <summary>
     * Проверяет успешное обновление данных профиля пользователя (HTTP 200 OK).
     * </summary>
     **/
    @Test
    public void shouldUpdateCurrentAccountWhenValidRequest() throws Exception {
        var response = new AccountResponseViewModel(
                "dmitry",
                "Дмитрий Волков",
                LocalDate.of(1999, 9, 19),
                new BigDecimal("1000000.00"),
                "RUB"
        );

        when(accountService.updateCurrentAccount(eq("dmitry"), any())).thenReturn(response);

        mockMvc.perform(put(ME_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("dmitry"))
                .andExpect(jsonPath("$.name").value("Дмитрий Волков"));

        verify(accountService).updateCurrentAccount(eq("dmitry"), any());
    }

    /**
     * <summary>
     * Проверяет отклонение запроса на обновление профиля (HTTP 400 Bad Request) при невалидном теле запроса.
     * </summary>
     **/
    @Test
    public void shouldRejectUpdateCurrentAccountWhenInvalidRequestBody() throws Exception {
        mockMvc.perform(put(ME_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_WRITE")
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_UPDATE_BODY))
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет успешное получение списка доступных получателей денежных переводов (HTTP 200 OK).
     * </summary>
     **/
    @Test
    public void shouldReturnRecipientsListWhenAuthenticated() throws Exception {
        var recipients = List.of(
                new RecipientResponseViewModel("alexey", "Алексей Морозов"),
                new RecipientResponseViewModel("elena", "Елена Кузнецова")
        );

        when(accountService.getRecipients("dmitry")).thenReturn(recipients);

        mockMvc.perform(get(RECIPIENTS_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "dmitry"))
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_ACCOUNT_READ")
                                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].login").value("alexey"))
                .andExpect(jsonPath("$[0].name").value("Алексей Морозов"))
                .andExpect(jsonPath("$[1].login").value("elena"))
                .andExpect(jsonPath("$[1].name").value("Елена Кузнецова"));

        verify(accountService).getRecipients("dmitry");
    }

    // endregion
}