package ru.yandex.practicum.bank.account.exceptions;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.bank.account.viewmodels.ApiErrorResponseViewModel;

/**
 * <summary>
 * Глобальный обработчик исключений REST-контроллеров микросервиса аккаунтов (AccountExceptionHandler).
 * Перехватывает доменные исключения и транслирует их в стандартизированные ответы {@link ApiErrorResponseViewModel}
 * с соответствующими HTTP-статусами.
 * </summary>
 **/
@RestControllerAdvice
public class AccountExceptionHandler {

    // region Constants

    private static final Logger log = LoggerFactory.getLogger(AccountExceptionHandler.class);

    // endregion

    // region Exception Handlers

    /**
     * <summary>
     * Обрабатывает ошибку отсутствия счета.
     * Возвращает статус 404 (Not Found).
     * </summary>
     * @param exception Перехваченное исключение AccountNotFoundException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponseViewModel handleAccountNotFound(AccountNotFoundException exception) {
        log.warn("Account request rejected status=not_found errorCode=ACCOUNT_NOT_FOUND source=account-service");

        return new ApiErrorResponseViewModel("ACCOUNT_NOT_FOUND", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибку отсутствия получателя (например, при попытке перевода).
     * Возвращает статус 404 (Not Found).
     * </summary>
     * @param exception Перехваченное исключение RecipientNotFoundException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(RecipientNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponseViewModel handleRecipientNotFound(RecipientNotFoundException exception) {
        log.warn("Account request rejected status=not_found errorCode=RECIPIENT_NOT_FOUND source=account-service");

        return new ApiErrorResponseViewModel("RECIPIENT_NOT_FOUND", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибку передачи некорректной суммы операции (например, отрицательной).
     * Возвращает статус 400 (Bad Request).
     * </summary>
     * @param exception Перехваченное исключение InvalidAmountException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(InvalidAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmount(InvalidAmountException exception) {
        log.warn("Account request rejected status=bad_request errorCode=INVALID_AMOUNT source=account-service");

        return new ApiErrorResponseViewModel("INVALID_AMOUNT", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибку нарушения допустимой точности (дробной части) при передаче суммы.
     * Возвращает статус 400 (Bad Request).
     * </summary>
     * @param exception Перехваченное исключение InvalidAmountScaleException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(InvalidAmountScaleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmountScale(InvalidAmountScaleException exception) {
        log.warn("Account request rejected status=bad_request errorCode=INVALID_AMOUNT_SCALE source=account-service");

        return new ApiErrorResponseViewModel("INVALID_AMOUNT_SCALE", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает бизнес-ошибку попытки перевода средств на свой же счет.
     * Возвращает статус 422 (Unprocessable Entity).
     * </summary>
     * @param exception Перехваченное исключение SelfTransferForbiddenException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(SelfTransferForbiddenException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponseViewModel handleSelfTransfer(SelfTransferForbiddenException exception) {
        log.warn("Account request rejected status=unprocessable_entity errorCode=SELF_TRANSFER_FORBIDDEN source=account-service");

        return new ApiErrorResponseViewModel("SELF_TRANSFER_FORBIDDEN", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибку недостатка средств на балансе счета для проведения операции.
     * Возвращает статус 422 (Unprocessable Entity).
     * </summary>
     * @param exception Перехваченное исключение InsufficientFundsException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponseViewModel handleInsufficientFunds(InsufficientFundsException exception) {
        log.warn("Account request rejected status=unprocessable_entity errorCode=INSUFFICIENT_FUNDS source=account-service");

        return new ApiErrorResponseViewModel("INSUFFICIENT_FUNDS", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибку несовпадения валют (например, перевод между счетами в разных валютах без конвертации).
     * Возвращает статус 422 (Unprocessable Entity).
     * </summary>
     * @param exception Перехваченное исключение CurrencyMismatchException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(CurrencyMismatchException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponseViewModel handleCurrencyMismatch(CurrencyMismatchException exception) {
        log.warn("Account request rejected status=unprocessable_entity errorCode=CURRENCY_MISMATCH source=account-service");

        return new ApiErrorResponseViewModel("CURRENCY_MISMATCH", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает системную ошибку конкурентного изменения данных (Optimistic Locking).
     * Возвращает статус 409 (Conflict).
     * </summary>
     * @param exception Перехваченное исключение JPA (ObjectOptimisticLockingFailureException или OptimisticLockException).
     * @return Модель ответа с кодом ошибки и стандартным сообщением.
     **/
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponseViewModel handleConcurrentUpdate(RuntimeException exception) {
        log.warn("Account request rejected status=conflict errorCode=CONCURRENT_UPDATE source=account-service");

        return new ApiErrorResponseViewModel("CONCURRENT_UPDATE", "Данные были изменены другим запросом");
    }

    /**
     * <summary>
     * Обрабатывает ошибку конфликта при использовании ключа идемпотентности.
     * Возвращает статус 409 (Conflict).
     * </summary>
     * @param exception Перехваченное исключение IdempotencyConflictException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponseViewModel handleIdempotencyConflict(IdempotencyConflictException exception) {
        log.warn("Account request rejected status=conflict errorCode=IDEMPOTENCY_CONFLICT source=account-service");

        return new ApiErrorResponseViewModel("IDEMPOTENCY_CONFLICT", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибку попытки запуска операции, которая уже находится в процессе обработки.
     * Возвращает статус 409 (Conflict).
     * </summary>
     * @param exception Перехваченное исключение OperationInProgressException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(OperationInProgressException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponseViewModel handleOperationInProgress(OperationInProgressException exception) {
        log.warn("Account request rejected status=conflict errorCode=OPERATION_IN_PROGRESS source=account-service");

        return new ApiErrorResponseViewModel("OPERATION_IN_PROGRESS", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибку попытки возобновления операции, которая ранее завершилась неудачей.
     * Возвращает статус 409 (Conflict).
     * </summary>
     * @param exception Перехваченное исключение OperationAlreadyFailedException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(OperationAlreadyFailedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponseViewModel handleOperationAlreadyFailed(OperationAlreadyFailedException exception) {
        log.warn("Account request rejected status=conflict errorCode=OPERATION_ALREADY_FAILED source=account-service");

        return new ApiErrorResponseViewModel("OPERATION_ALREADY_FAILED", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибки JSR-380 валидации (MethodArgumentNotValidException) и специфичные ошибки дат (InvalidBirthdateException).
     * Возвращает статус 400 (Bad Request).
     * </summary>
     * @param exception Перехваченное исключение валидации.
     * @return Модель ответа с кодом ошибки и деталями нарушения валидации.
     **/
    @ExceptionHandler({InvalidBirthdateException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleValidation(Exception exception) {
        log.warn("Account request rejected status=bad_request errorCode=VALIDATION_ERROR source=account-service");

        return new ApiErrorResponseViewModel("VALIDATION_ERROR", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибку отсутствия обязательного claim'а (preferred_username) в JWT токене.
     * Возвращает статус 401 (Unauthorized).
     * </summary>
     * @param exception Перехваченное исключение MissingPreferredUsernameException.
     * @return Модель ответа с кодом ошибки и сообщением.
     **/
    @ExceptionHandler(MissingPreferredUsernameException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponseViewModel handleMissingPreferredUsername(MissingPreferredUsernameException exception) {
        log.warn("Account request rejected status=unauthorized errorCode=UNAUTHORIZED source=account-service");

        return new ApiErrorResponseViewModel("UNAUTHORIZED", exception.getMessage());
    }

    // endregion
}