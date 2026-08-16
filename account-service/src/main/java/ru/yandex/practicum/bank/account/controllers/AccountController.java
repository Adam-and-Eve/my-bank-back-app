package ru.yandex.practicum.bank.account.controllers;

import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.bank.account.exceptions.MissingPreferredUsernameException;
import ru.yandex.practicum.bank.account.interfaces.AccountService;
import ru.yandex.practicum.bank.account.viewmodels.AccountResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.RecipientResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.UpdateAccountRequestViewModel;

import java.util.List;

/**
 * <summary>
 * REST-контроллер для управления профилем аккаунта и получения списка получателей (AccountController).
 * </summary>
 **/
@RestController
@RequestMapping("/api/account")
public class AccountController {

    // region Fields

    private final AccountService accountService;

    // endregion

    // region Constructors

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // endregion

    // region Actions

    /**
     * <summary>
     * Возвращает данные текущего аутентифицированного пользователя.
     * </summary>
     * @param authentication Токен аутентификации JWT.
     * @return ViewModel с данными счета {@link AccountResponseViewModel}.
     */
    @GetMapping("/me")
    public AccountResponseViewModel getCurrentAccount(JwtAuthenticationToken authentication) {
        return accountService.getCurrentAccount(getLogin(authentication));
    }

    /**
     * <summary>
     * Обновляет профильные данные (имя и дату рождения) текущего пользователя.
     * </summary>
     * @param authentication Токен аутентификации JWT.
     * @param request ViewModel с обновляемыми данными.
     * @return ViewModel с обновленной информацией о счете {@link AccountResponseViewModel}.
     */
    @PutMapping("/me")
    public AccountResponseViewModel updateCurrentAccount(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody UpdateAccountRequestViewModel request
    ) {
        return accountService.updateCurrentAccount(getLogin(authentication), request);
    }

    /**
     * <summary>
     * Возвращает список всех доступных получателей денежных переводов.
     * </summary>
     * @param authentication Токен аутентификации JWT.
     * @return Список ViewModel получателей {@link RecipientResponseViewModel}.
     */
    @GetMapping("/recipients")
    public List<RecipientResponseViewModel> getRecipients(JwtAuthenticationToken authentication) {
        return accountService.getRecipients(getLogin(authentication));
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Извлекает логин пользователя из claim 'preferred_username' JWT-токена.
     * </summary>
     * @param authentication Токен аутентификации JWT.
     * @return Логин пользователя.
     * @throws MissingPreferredUsernameException Если токен отсутствует или claim 'preferred_username' пустой/не задан.
     */
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