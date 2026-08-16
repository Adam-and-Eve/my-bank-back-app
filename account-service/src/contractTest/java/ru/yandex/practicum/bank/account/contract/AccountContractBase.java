package ru.yandex.practicum.bank.account.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.bank.account.controllers.AccountController;
import ru.yandex.practicum.bank.account.controllers.InternalBalanceController;
import ru.yandex.practicum.bank.account.exceptions.AccountExceptionHandler;
import ru.yandex.practicum.bank.account.interfaces.AccountService;
import ru.yandex.practicum.bank.account.interfaces.BalanceService;
import ru.yandex.practicum.bank.account.viewmodels.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * <summary>
 * Базовый класс для Spring Cloud Contract тестов account-service.
 * Настраивает MockMvc с замоканными AccountService и BalanceService,
 * JWT-аутентификацией пользователя dmitry и JSON-конвертером.
 * Данные согласованы с seed-скриптом:
 * dmitry (1 000 000.00), alexey (25 000.00), elena (5 500.00).
 * </summary>
 **/
public class AccountContractBase {

    // region Setup

    /**
     * <summary>
     * Инициализирует MockMvc, JWT-principal и стабы сервисов перед каждым контрактным тестом.
     * </summary>
     **/
    @BeforeEach
    void setUp() {
        var accountService = mock(AccountService.class);

        var balanceService = mock(BalanceService.class);

        when(accountService.getCurrentAccount("dmitry")).thenReturn(account());

        when(accountService.updateCurrentAccount(eq("dmitry"), any(UpdateAccountRequestViewModel.class)))
                .thenReturn(updatedAccount());

        when(accountService.getRecipients("dmitry")).thenReturn(List.of(
                new RecipientResponseViewModel("alexey", "Алексей Морозов"),
                new RecipientResponseViewModel("elena", "Елена Кузнецова")
        ));

        when(balanceService.deposit(any())).thenReturn(new BalanceResponseViewModel(
                "dmitry",
                new BigDecimal("1000250.00"),
                "RUB"
        ));

        when(balanceService.withdraw(any())).thenReturn(new BalanceResponseViewModel(
                "dmitry",
                new BigDecimal("999900.00"),
                "RUB"
        ));

        when(balanceService.transfer(any())).thenReturn(new TransferBalanceResponseViewModel(
                "dmitry",
                "alexey",
                new BigDecimal("999850.00"),
                "RUB"
        ));

        var mockMvc = MockMvcBuilders.standaloneSetup(
                        new AccountController(accountService),
                        new InternalBalanceController(balanceService)
                )
                .setControllerAdvice(new AccountExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper()))
                .defaultRequest(get("/").principal(jwtAuthentication("dmitry")))
                .build();

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * ObjectMapper с JavaTimeModule и без timestamp-дат.
     * </summary>
     **/
    private JsonMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    /**
     * <summary>
     * Ответ GET /api/account/me — данные dmitry из seed.
     * </summary>
     **/
    private AccountResponseViewModel account() {
        return new AccountResponseViewModel(
                "dmitry",
                "Дмитрий Волков",
                LocalDate.of(1999, 9, 19),
                new BigDecimal("1000000.00"),
                "RUB"
        );
    }

    /**
     * <summary>
     * Ответ PUT /api/account/me — обновлённые имя и дата рождения, баланс без изменений.
     * </summary>
     **/
    private AccountResponseViewModel updatedAccount() {
        return new AccountResponseViewModel(
                "dmitry",
                "Дмитрий Обновлённый",
                LocalDate.of(1999, 9, 19),
                new BigDecimal("1000000.00"),
                "RUB"
        );
    }

    /**
     * <summary>
     * JWT с preferred_username = login.
     * </summary>
     **/
    private JwtAuthenticationToken jwtAuthentication(String login) {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-06-13T00:00:00Z"))
                .claim("preferred_username", login)
                .build();

        return new JwtAuthenticationToken(jwt);
    }

    // endregion
}