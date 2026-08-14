package ru.yandex.practicum.bank.cash.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.bank.cash.controllers.CashController;
import ru.yandex.practicum.bank.cash.exceptions.CashExceptionHandler;
import ru.yandex.practicum.bank.cash.interfaces.CashService;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * <summary>
 * Базовый класс для контрактных тестов (Spring Cloud Contract) сервиса операций с наличностью.
 * Инициализирует автономное окружение RestAssuredMockMvc с заслепленным сервисом CashService
 * и предустановленным авторизованным принципалом.
 * </summary>
 **/
public class CashContractBase {

    // region Setup

    /**
     * <summary>
     * Подготавливает тестовый контекст RestAssuredMockMvc перед выполнением каждого контрактного теста.
     * Настраивает мок-поведение для операций пополнения и снятия наличных, регистрирует обработчик ошибок
     * и подставляет тестовый JWT-токен по умолчанию.
     * </summary>
     **/
    @BeforeEach
    void setUp() {
        var cashService = mock(CashService.class);

        when(cashService.deposit(eq("dmitry"), any(CashOperationRequestViewModel.class)))
                .thenReturn(new CashOperationResponseViewModel(
                        new BigDecimal("1250.00"),
                        "RUB",
                        "Счёт пополнен"
                ));

        when(cashService.withdraw(eq("dmitry"), any(CashOperationRequestViewModel.class)))
                .thenReturn(new CashOperationResponseViewModel(
                        new BigDecimal("900.00"),
                        "RUB",
                        "Деньги сняты со счёта"
                ));

        var mockMvc = MockMvcBuilders.standaloneSetup(new CashController(cashService))
                .setControllerAdvice(new CashExceptionHandler())
                .defaultRequest(get("/").principal(jwtAuthentication("dmitry")))
                .build();

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Генерирует объект JwtAuthenticationToken с указанным логином пользователя в claim preferred_username.
     * </summary>
     * @param login Логин пользователя для подстановки в JWT-токен.
     * <return>
     * @return Сформированный экземпляр JwtAuthenticationToken.
     * </return>
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