package ru.yandex.practicum.bank.cash.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.bank.cash.viewmodels.ApiErrorResponseViewModel;

/**
 * <summary>
 * Глобальный обработчик исключений контроллеров сервиса наличных (Cash Service).
 * Перехватывает ошибки валидации, доменные исключения и сбои внешних интеграций,
 * преобразуя их в единый формат ответа ApiErrorResponseViewModel с соответствующим HTTP-статусом.
 * </summary>
 **/
@RestControllerAdvice
public class CashExceptionHandler {

    /**
     * <summary>
     * Обрабатывает исключение некорректной суммы операции (например, отрицательная или нулевая сумма).
     * </summary>
     * @param exception Исключение InvalidAmountException.
     * <return>
     * @return Модель ошибки ApiErrorResponseViewModel с кодом INVALID_AMOUNT и статусом 400 Bad Request.
     * </return>
     **/
    @ExceptionHandler(InvalidAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmount(InvalidAmountException exception) {
        return new ApiErrorResponseViewModel("INVALID_AMOUNT", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает исключение недопустимой точности суммы (более 2 знаков после запятой).
     * </summary>
     * @param exception Исключение InvalidAmountScaleException.
     * <return>
     * @return Модель ошибки ApiErrorResponseViewModel с кодом INVALID_AMOUNT_SCALE и статусом 400 Bad Request.
     * </return>
     **/
    @ExceptionHandler(InvalidAmountScaleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmountScale(InvalidAmountScaleException exception) {
        return new ApiErrorResponseViewModel("INVALID_AMOUNT_SCALE", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибки валидации аргументов методов (например, нарушения аннотаций @NotNull, @Valid).
     * </summary>
     * @param exception Исключение MethodArgumentNotValidException.
     * <return>
     * @return Модель ошибки ApiErrorResponseViewModel с кодом VALIDATION_ERROR и статусом 400 Bad Request.
     * </return>
     **/
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleValidation(MethodArgumentNotValidException exception) {
        return new ApiErrorResponseViewModel("VALIDATION_ERROR", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает исключение отсутствия обязательного claim'а preferred_username в JWT-токене.
     * </summary>
     * @param exception Исключение MissingPreferredUsernameException.
     * <return>
     * @return Модель ошибки ApiErrorResponseViewModel с кодом UNAUTHORIZED и статусом 401 Unauthorized.
     * </return>
     **/
    @ExceptionHandler(MissingPreferredUsernameException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponseViewModel VhandleMissingPreferredUsername(MissingPreferredUsernameException exception) {
        return new ApiErrorResponseViewModel("UNAUTHORIZED", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибки взаимодействия с внешним сервисом счетов (Accounts Service).
     * </summary>
     * @param exception Исключение AccountClientException.
     * <return>
     * @return Модель ошибки ApiErrorResponseViewModel с кодом ACCOUNT_SERVICE_UNAVAILABLE и статусом 502 Bad Gateway.
     * </return>
     **/
    @ExceptionHandler(AccountClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponseViewModel handleAccountsClient(AccountClientException exception) {
        return new ApiErrorResponseViewModel("ACCOUNT_SERVICE_UNAVAILABLE", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибки взаимодействия с внешним сервисом уведомлений (Notifications Service).
     * </summary>
     * @param exception Исключение NotificationClientException.
     * <return>
     * @return Модель ошибки ApiErrorResponseViewModel с кодом NOTIFICATION_SERVICE_UNAVAILABLE и статусом 502 Bad Gateway.
     * </return>
     **/
    @ExceptionHandler(NotificationClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponseViewModel handleNotificationsClient(NotificationClientException exception) {
        return new ApiErrorResponseViewModel("NOTIFICATION_SERVICE_UNAVAILABLE", exception.getMessage());
    }
}