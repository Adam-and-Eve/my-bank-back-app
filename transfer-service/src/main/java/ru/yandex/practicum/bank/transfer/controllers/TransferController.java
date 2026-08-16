package ru.yandex.practicum.bank.transfer.controllers;

import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.bank.transfer.exceptions.MissingPreferredUsernameException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResponseViewModel;

/**
 * <summary>
 * REST-контроллер для обработки входящих запросов на выполнение денежных переводов.
 * </summary>
 **/
@RestController
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * <summary>
     * Обрабатывает POST-запрос на проведение денежного перевода между пользователями.
     * </summary>
     * @param authentication Токен аутентификации JWT для извлечения логина текущего пользователя.
     * @param request Запрос на перевод денег с валидируемыми полями.
     * <return>
     * @return Результат выполнения перевода TransferResponseViewModel.
     * </return>
     **/
    @PostMapping("/api/transfer")
    public TransferResponseViewModel transfer(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody TransferRequestViewModel request
    ) {
        return transferService.transfer(getLogin(authentication), request);
    }

    /**
     * <summary>
     * Извлекает имя пользователя (preferred_username) из JWT-токена аутентификации.
     * </summary>
     * @param authentication Токен аутентификации JWT.
     * <return>
     * @return Логин авторизованного пользователя.
     * </return>
     * @throws MissingPreferredUsernameException Если токен отсутствует или preferred_username не задан.
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
}