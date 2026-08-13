package ru.yandex.practicum.bank.notification.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.bank.notification.viewmodels.ApiErrorResponseViewModel;

/**
 * <summary>
 * Глобальный обработчик исключений (ControllerAdvice) для модуля уведомлений.
 * Перехватывает ошибки валидации входящих запросов и формирует стандартизированный ответ об ошибке.
 * </summary>
 **/
@RestControllerAdvice
public class NotificationExceptionHandler {

    // region Methods

    /**
     * <summary>
     * Обрабатывает исключения невалидных аргументов метода (ошибки валидации @Valid).
     * </summary>
     * @param exception Исключение MethodArgumentNotValidException, возникшее при валидации тела запроса.
     * <return>
     * @return Объект ApiErrorResponseViewModel со статусом BAD_REQUEST и описанием ошибки.
     * </return>
     **/
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleValidation(MethodArgumentNotValidException exception) {
        return new ApiErrorResponseViewModel("VALIDATION_ERROR", exception.getMessage());
    }

    // endregion
}