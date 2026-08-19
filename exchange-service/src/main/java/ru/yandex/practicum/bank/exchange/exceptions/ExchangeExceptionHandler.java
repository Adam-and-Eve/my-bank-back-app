package ru.yandex.practicum.bank.exchange.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.yandex.practicum.bank.shared.viewmodels.ApiErrorResponseViewModel;


/**
 * <summary>
 * Обработчик исключений, возникающих при выполнении операций обмена валют.
 * Преобразует исключения в единый формат ответа с информацией об ошибке.
 * </summary>
 */
@RestControllerAdvice
public class ExchangeExceptionHandler {

    /**
     * <summary>
     * Обрабатывает исключение, возникающее при передаче некорректной суммы для конвертации.
     * </summary>
     * @param exception Исключение с информацией о некорректной сумме.
     * @return Модель ответа с кодом и описанием ошибки.
     */
    @ExceptionHandler(InvalidAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmount(
            InvalidAmountException exception) {

        return new ApiErrorResponseViewModel(
                "INVALID_AMOUNT",
                exception.getMessage()
        );
    }

    /**
     * <summary>
     * Обрабатывает исключение, возникающее при передаче некорректного курса обмена валюты.
     * </summary>
     * @param exception Исключение с информацией о некорректном курсе.
     * @return Модель ответа с кодом и описанием ошибки.
     */
    @ExceptionHandler(InvalidRateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidRate(
            InvalidRateException exception) {

        return new ApiErrorResponseViewModel(
                "INVALID_RATE",
                exception.getMessage()
        );
    }

    /**
     * <summary>
     * Обрабатывает ошибки валидации и некорректные параметры входящего HTTP-запроса.
     * </summary>
     * @param exception Исключение, содержащее информацию об ошибке запроса.
     * @return Модель ответа с кодом и описанием ошибки.
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleValidation(Exception exception) {
        return new ApiErrorResponseViewModel(
                "VALIDATION_ERROR",
                exception.getMessage()
        );
    }
}