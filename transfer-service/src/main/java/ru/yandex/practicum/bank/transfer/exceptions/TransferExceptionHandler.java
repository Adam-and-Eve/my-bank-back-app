package ru.yandex.practicum.bank.transfer.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.bank.transfer.viewmodels.ApiErrorResponseViewModel;

/**
 * <summary>
 * Глобальный обработчик исключений (Controller Advice) для REST-контроллеров сервиса переводов (Transfer Service).
 * Перехватывает исключения бизнес-логики, валидации и межсервисного взаимодействия,
 * преобразуя их в унифицированные DTO ответов об ошибках ApiErrorResponseViewModel с соответствующими HTTP-статусами.
 * </summary>
 **/
@RestControllerAdvice
public class TransferExceptionHandler {

    /**
     * <summary>
     * Обрабатывает исключение некорректной (неположительной) суммы перевода.
     * </summary>
     * @param exception Исключение InvalidAmountException.
     * <return>
     * @return DTO ответа об ошибке с кодом INVALID_AMOUNT и HTTP-статусом 400 Bad Request.
     * </return>
     **/
    @ExceptionHandler(InvalidAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmount(InvalidAmountException exception) {
        return new ApiErrorResponseViewModel("INVALID_AMOUNT", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает исключение превышения количества знаков после запятой в сумме перевода.
     * </summary>
     * @param exception Исключение InvalidAmountScaleException.
     * <return>
     * @return DTO ответа об ошибке с кодом INVALID_AMOUNT_SCALE и HTTP-статусом 400 Bad Request.
     * </return>
     **/
    @ExceptionHandler(InvalidAmountScaleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmountScale(InvalidAmountScaleException exception) {
        return new ApiErrorResponseViewModel("INVALID_AMOUNT_SCALE", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает исключение при попытке выполнения самоперевода (перевода самому себе).
     * </summary>
     * @param exception Исключение SelfTransferForbiddenException.
     * <return>
     * @return DTO ответа об ошибке с кодом SELF_TRANSFER_FORBIDDEN и HTTP-статусом 422 Unprocessable Entity.
     * </return>
     **/
    @ExceptionHandler(SelfTransferForbiddenException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponseViewModel handleSelfTransfer(SelfTransferForbiddenException exception) {
        return new ApiErrorResponseViewModel("SELF_TRANSFER_FORBIDDEN", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибки валидации аргументов входящих DTO-запросов.
     * </summary>
     * @param exception Исключение MethodArgumentNotValidException.
     * <return>
     * @return DTO ответа об ошибке с кодом VALIDATION_ERROR и HTTP-статусом 400 Bad Request.
     * </return>
     **/
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleValidation(MethodArgumentNotValidException exception) {
        return new ApiErrorResponseViewModel("VALIDATION_ERROR", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает исключение отсутствия обязательного имени пользователя (preferred_username) в JWT-токене.
     * </summary>
     * @param exception Исключение MissingPreferredUsernameException.
     * <return>
     * @return DTO ответа об ошибке с кодом UNAUTHORIZED и HTTP-статусом 401 Unauthorized.
     * </return>
     **/
    @ExceptionHandler(MissingPreferredUsernameException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponseViewModel handleMissingPreferredUsername(MissingPreferredUsernameException exception) {
        return new ApiErrorResponseViewModel("UNAUTHORIZED", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает исключения сбоев при вызове внешнего сервиса счетов (Accounts Service).
     * </summary>
     * @param exception Исключение AccountClientException.
     * <return>
     * @return DTO ответа об ошибке с кодом ACCOUNT_SERVICE_UNAVAILABLE и HTTP-статусом 502 Bad Gateway.
     * </return>
     **/
    @ExceptionHandler(AccountClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponseViewModel handleAccountsClient(AccountClientException exception) {
        return new ApiErrorResponseViewModel("ACCOUNT_SERVICE_UNAVAILABLE", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает исключения сбоев при вызове внешнего сервиса уведомлений (Notification Service).
     * </summary>
     * @param exception Исключение NotificationClientException.
     * <return>
     * @return DTO ответа об ошибке с кодом NOTIFICATION_SERVICE_UNAVAILABLE и HTTP-статусом 502 Bad Gateway.
     * </return>
     **/
    @ExceptionHandler(NotificationClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponseViewModel handleNotificationsClient(NotificationClientException exception) {
        return new ApiErrorResponseViewModel("NOTIFICATION_SERVICE_UNAVAILABLE", exception.getMessage());
    }
}