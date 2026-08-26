package ru.yandex.practicum.bank.cash.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityReturnValueHandler;
import ru.yandex.practicum.bank.shared.exceptions.BlockerClientException;
import ru.yandex.practicum.bank.shared.exceptions.ExchangeClientException;
import ru.yandex.practicum.bank.shared.exceptions.NotificationClientException;
import ru.yandex.practicum.bank.shared.viewmodels.ApiErrorResponseViewModel;

/**
 * <summary>
 * Глобальный обработчик исключений контроллеров сервиса наличных (Cash Service).
 * Перехватывает ошибки валидации, доменные исключения и сбои внешних интеграций,
 * преобразуя их в единый формат ответа ApiErrorResponseViewModel с соответствующим HTTP-статусом.
 * </summary>
 **/
@RestControllerAdvice
public class CashExceptionHandler {

    // region Fields

    private static final Logger log = LoggerFactory.getLogger(CashExceptionHandler.class);

    // endregion

    // region Methods

    /**
     * <summary>
     * Обрабатывает ошибку указания недопустимой суммы операции (например, отрицательной или равной нулю).
     * Возвращает HTTP 400 Bad Request.
     * </summary>
     * @param exception Перехваченное исключение InvalidAmountException.
     * @return Модель стандартизированного ответа об ошибке.
     */
    @ExceptionHandler(InvalidAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmount(InvalidAmountException exception) {
        log.warn("Cash request rejected status=bad_request errorCode=INVALID_AMOUNT source=cash-service");
        return new ApiErrorResponseViewModel("INVALID_AMOUNT", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибку неверной размерности суммы (например, слишком много знаков после запятой).
     * Возвращает HTTP 400 Bad Request.
     * </summary>
     * @param exception Перехваченное исключение InvalidAmountScaleException.
     * @return Модель стандартизированного ответа об ошибке.
     */
    @ExceptionHandler(InvalidAmountScaleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmountScale(InvalidAmountScaleException exception) {
        log.warn("Cash request rejected status=bad_request errorCode=INVALID_AMOUNT_SCALE source=cash-service");

        return new ApiErrorResponseViewModel("INVALID_AMOUNT_SCALE", exception.getMessage());
    }

    /**
     * <summary>
     * Перехватывает ошибки валидации входных аргументов REST-контроллера (например, нарушения @NotNull, @Positive).
     * Возвращает HTTP 400 Bad Request.
     * </summary>
     * @param exception Исключение MethodArgumentNotValidException, выбрасываемое Spring.
     * @return Модель стандартизированного ответа об ошибке.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleValidation(MethodArgumentNotValidException exception) {
        log.warn("Cash request rejected status=bad_request errorCode=VALIDATION_ERROR source=cash-service");

        return new ApiErrorResponseViewModel("VALIDATION_ERROR", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает отсутствие имени пользователя в контексте запроса (например, не передан токен авторизации).
     * Возвращает HTTP 401 Unauthorized.
     * </summary>
     * @param exception Перехваченное исключение MissingPreferredUsernameException.
     * @return Модель стандартизированного ответа об ошибке.
     */
    @ExceptionHandler(MissingPreferredUsernameException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponseViewModel handleMissingPreferredUsername(MissingPreferredUsernameException exception) {
        log.warn("Cash request rejected status=unauthorized errorCode=UNAUTHORIZED source=cash-service");

        return new ApiErrorResponseViewModel("UNAUTHORIZED", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает отклонение (блокировку) кассовой операции доменной логикой или внешними сервисами.
     * Возвращает HTTP 422 Unprocessable Entity.
     * </summary>
     * @param exception Перехваченное исключение OperationBlockedException.
     * @return Модель стандартизированного ответа об ошибке.
     */
    @ExceptionHandler(OperationBlockedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponseViewModel handleOperationBlocked(OperationBlockedException exception) {
        log.warn("Cash request rejected status=unprocessable_entity errorCode=OPERATION_BLOCKED source=cash-service");

        return new ApiErrorResponseViewModel("OPERATION_BLOCKED", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибки взаимодействия с микросервисом счетов (Account Service).
     * Клиентские ошибки (4xx) транслируются клиенту с сохранением статуса,
     * а серверные и сетевые сбои (5xx) оборачиваются в HTTP 502 Bad Gateway.
     * </summary>
     * @param exception Перехваченное исключение интеграции AccountClientException.
     * @return Ответ с нужным HTTP-статусом и моделью ошибки.
     */
    @ExceptionHandler(AccountClientException.class)
    public ResponseEntity<ApiErrorResponseViewModel> handleAccountsClient(AccountClientException exception) {
        if (exception.getStatusCode().is4xxClientError()) {
            log.warn(
                    "Cash downstream request rejected status={} errorCode={} source=cash-service targetService=account-service",
                    exception.getStatusCode().value(),
                    exception.getCode()
            );
            return ResponseEntity.status(exception.getStatusCode())
                    .body(new ApiErrorResponseViewModel(exception.getCode(), exception.getMessage()));
        }

        log.error(
                "Cash downstream request failed status={} errorCode={} errorCategory=downstream_unavailable errorType={} source=cash-service targetService=account-service",
                exception.getStatusCode().value(),
                exception.getCode(),
                exception.getClass().getSimpleName()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponseViewModel("ACCOUNT_SERVICE_UNAVAILABLE", exception.getMessage()));
    }

    /**
     * <summary>
     * Обрабатывает ошибки недоступности микросервиса блокировок (Blocker Service).
     * Возвращает HTTP 502 Bad Gateway.
     * </summary>
     * @param exception Перехваченное исключение интеграции BlockerClientException.
     * @return Модель стандартизированного ответа об ошибке.
     */
    @ExceptionHandler(BlockerClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponseViewModel handleBlockerClient(BlockerClientException exception) {
        log.error(
                "Cash downstream request failed status=502 errorCode=BLOCKER_SERVICE_UNAVAILABLE errorCategory=downstream_unavailable errorType={} source=cash-service targetService=blocker-service",
                exception.getClass().getSimpleName()
        );

        return new ApiErrorResponseViewModel("BLOCKER_SERVICE_UNAVAILABLE", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибки недоступности микросервиса курсов валют (Exchange Service).
     * Возвращает HTTP 502 Bad Gateway.
     * </summary>
     * @param exception Перехваченное исключение интеграции ExchangeClientException.
     * @return Модель стандартизированного ответа об ошибке.
     */
    @ExceptionHandler(ExchangeClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponseViewModel handleExchangeClient(ExchangeClientException exception) {
        log.error(
                "Cash downstream request failed status=502 errorCode=EXCHANGE_SERVICE_UNAVAILABLE errorCategory=downstream_unavailable errorType={} source=cash-service targetService=exchange-service",
                exception.getClass().getSimpleName()
        );

        return new ApiErrorResponseViewModel("EXCHANGE_SERVICE_UNAVAILABLE", exception.getMessage());
    }

    // endregion
}