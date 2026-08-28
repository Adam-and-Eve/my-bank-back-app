package ru.yandex.practicum.bank.cash.controllers;

import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.bank.cash.exceptions.MissingPreferredUsernameException;
import ru.yandex.practicum.bank.cash.interfaces.CashService;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationRequestViewModel;
import ru.yandex.practicum.bank.cash.viewmodels.CashOperationResponseViewModel;

import java.util.UUID;

/**
 * <summary>
 * REST-контроллер для обработки операций с наличностью (Cash Service).
 * Предоставляет HTTP-эндпоинты для пополнения счета и снятия наличных средств
 * с автоматическим извлечением логина пользователя из JWT-токена авторизации.
 * </summary>
 **/
@RestController
public class CashController {

    // region Constants

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    // endregion

    // region Fields

    private final CashService cashService;

    // endregion

    // region Constructors

    public CashController(CashService cashService) {
        this.cashService = cashService;
    }

    // endregion

    // region Actions

    /**
     * <summary>
     * Обрабатывает POST-запрос на пополнение счета наличностью.
     * </summary>
     * @param authentication Токен аутентификации JWT с данными текущего пользователя.
     * @param request Модель запроса операции с наличностью (CashOperationRequestViewModel).
     * <return>
     * @return Модель ответа CashOperationResponseViewModel с обновленным балансом и статусом.
     * </return>
     **/
    @PostMapping("/api/cash/deposit")
    public CashOperationResponseViewModel deposit(
            JwtAuthenticationToken authentication,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) UUID idempotencyKey,
            @Valid @RequestBody CashOperationRequestViewModel request
    ) {
        return cashService.deposit(getLogin(authentication), request, idempotencyKey);
    }

    /**
     * <summary>
     * Обрабатывает POST-запрос на снятие наличности со счета.
     * </summary>
     * @param authentication Токен аутентификации JWT с данными текущего пользователя.
     * @param request Модель запроса операции с наличностью (CashOperationRequestViewModel).
     * <return>
     * @return Модель ответа CashOperationResponseViewModel с обновленным балансом и статусом.
     * </return>
     **/
    @PostMapping("/api/cash/withdraw")
    public CashOperationResponseViewModel withdraw(
            JwtAuthenticationToken authentication,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) UUID idempotencyKey,
            @Valid @RequestBody CashOperationRequestViewModel request
    ) {
        return cashService.withdraw(getLogin(authentication), request, idempotencyKey);
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Извлекает логин пользователя (claim preferred_username) из JWT-токена аутентификации.
     * </summary>
     * @param authentication Токен аутентификации JWT.
     * <return>
     * @return Логин авторизованного пользователя.
     * </return>
     **/
    private String getLogin(JwtAuthenticationToken authentication) {
        if (authentication == null) {
            throw new MissingPreferredUsernameException();
        }

        var login = authentication.getToken().getClaimAsString("preferred_username");

        if (login == null || login.isBlank()) {
            throw new MissingPreferredUsernameException();
        }

        return login;
    }

    // endregion
}