package ru.yandex.practicum.bank.blocker.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.bank.shared.viewmodels.ApiErrorResponseViewModel;

/**
 * <summary>
 * Глобальный обработчик исключений сервиса блокировки банковских операций.
 * Преобразует исключения приложения и ошибки валидации
 * в стандартизированные модели ответов API.
 * </summary>
 */
@RestControllerAdvice
public class BlockerExceptionHandler {

    /**
     * <summary>
     * Обрабатывает исключение, возникающее при получении некорректного
     * запроса на проверку банковской операции.
     * </summary>
     * @param exception Исключение с описанием причины некорректности запроса.
     * @return Модель ответа с кодом и сообщением об ошибке.
     */
    @ExceptionHandler(InvalidOperationRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidOperationRequest(
            InvalidOperationRequestException exception) {

        return new ApiErrorResponseViewModel(
                "INVALID_OPERATION_REQUEST",
                exception.getMessage()
        );
    }

    /**
     * <summary>
     * Обрабатывает ошибки валидации и некорректного формата входного запроса.
     * </summary>
     * @param exception Исключение, возникшее при обработке входного запроса.
     * @return Модель ответа с кодом и сообщением об ошибке валидации.
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleValidation(Exception exception) {

        return new ApiErrorResponseViewModel(
                "VALIDATION_ERROR",
                exception.getMessage()
        );
    }
}