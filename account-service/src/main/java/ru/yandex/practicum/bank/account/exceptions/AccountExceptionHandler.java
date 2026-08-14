package ru.yandex.practicum.bank.account.exceptions;

import jakarta.persistence.OptimisticLockException;
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

    /**
     * <summary>
     * Обрабатывает исключения отсутствия счёта пользователя.
     * </summary>
     *
     * @param exception Исключение {@link AccountNotFoundException}.
     * @return DTO ошибки со статусом 404 NOT_FOUND.
     */
    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponseViewModel handleAccountNotFound(AccountNotFoundException exception) {
        return new ApiErrorResponseViewModel("ACCOUNT_NOT_FOUND", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает исключения отсутствия счёта получателя перевода.
     * </summary>
     *
     * @param exception Исключение {@link RecipientNotFoundException}.
     * @return DTO ошибки со статусом 404 NOT_FOUND.
     */
    @ExceptionHandler(RecipientNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponseViewModel handleRecipientNotFound(RecipientNotFoundException exception) {
        return new ApiErrorResponseViewModel("RECIPIENT_NOT_FOUND", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибки невалидной суммы операции (сумма <= 0 или null).
     * </summary>
     *
     * @param exception Исключение {@link InvalidAmountException}.
     * @return DTO ошибки со статусом 400 BAD_REQUEST.
     */
    @ExceptionHandler(InvalidAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmount(InvalidAmountException exception) {
        return new ApiErrorResponseViewModel("INVALID_AMOUNT", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибки некорректной точности суммы (более 2 знаков после запятой).
     * </summary>
     *
     * @param exception Исключение {@link InvalidAmountScaleException}.
     * @return DTO ошибки со статусом 400 BAD_REQUEST.
     */
    @ExceptionHandler(InvalidAmountScaleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmountScale(InvalidAmountScaleException exception) {
        return new ApiErrorResponseViewModel("INVALID_AMOUNT_SCALE", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает попытку перевода денежных средств самому себе.
     * </summary>
     *
     * @param exception Исключение {@link SelfTransferForbiddenException}.
     * @return DTO ошибки со статусом 422 UNPROCESSABLE_ENTITY.
     */
    @ExceptionHandler(SelfTransferForbiddenException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponseViewModel handleSelfTransfer(SelfTransferForbiddenException exception) {
        return new ApiErrorResponseViewModel("SELF_TRANSFER_FORBIDDEN", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает случаи нехватки средств на счёте для совершения операции.
     * </summary>
     *
     * @param exception Исключение {@link InsufficientFundsException}.
     * @return DTO ошибки со статусом 422 UNPROCESSABLE_ENTITY.
     */
    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponseViewModel handleInsufficientFunds(InsufficientFundsException exception) {
        return new ApiErrorResponseViewModel("INSUFFICIENT_FUNDS", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает конфликты параллельного обновления данных (Optimistic Locking Failure).
     * </summary>
     *
     * @param exception Исключения оптимистичной блокировки JPA / Spring.
     * @return DTO ошибки со статусом 409 CONFLICT.
     */
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponseViewModel handleConcurrentUpdate(RuntimeException exception) {
        return new ApiErrorResponseViewModel("CONCURRENT_UPDATE", "Данные были изменены другим запросом");
    }

    /**
     * <summary>
     * Обрабатывает конфликт идемпотентности (повторный запрос с тем же operationId, но другими параметрами).
     * </summary>
     *
     * @param exception Исключение {@link IdempotencyConflictException}.
     * @return DTO ошибки со статусом 409 CONFLICT.
     */
    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponseViewModel handleIdempotencyConflict(IdempotencyConflictException exception) {
        return new ApiErrorResponseViewModel("IDEMPOTENCY_CONFLICT", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает попытку повторного вызова операции, которая всё ещё находится в процессе обработки.
     * </summary>
     *
     * @param exception Исключение {@link OperationInProgressException}.
     * @return DTO ошибки со статусом 409 CONFLICT.
     */
    @ExceptionHandler(OperationInProgressException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponseViewModel handleOperationInProgress(OperationInProgressException exception) {
        return new ApiErrorResponseViewModel("OPERATION_IN_PROGRESS", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает попытку повторного выполнения операции, ранее завершившейся ошибкой.
     * </summary>
     *
     * @param exception Исключение {@link OperationAlreadyFailedException}.
     * @return DTO ошибки со статусом 409 CONFLICT.
     */
    @ExceptionHandler(OperationAlreadyFailedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponseViewModel handleOperationAlreadyFailed(OperationAlreadyFailedException exception) {
        return new ApiErrorResponseViewModel("OPERATION_ALREADY_FAILED", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает ошибки валидации параметров запроса и ограничений возраста.
     * </summary>
     *
     * @param exception Исключение валидации {@link InvalidBirthdateException} или {@link MethodArgumentNotValidException}.
     * @return DTO ошибки со статусом 400 BAD_REQUEST.
     */
    @ExceptionHandler({InvalidBirthdateException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleValidation(Exception exception) {
        return new ApiErrorResponseViewModel("VALIDATION_ERROR", exception.getMessage());
    }

    /**
     * <summary>
     * Обрабатывает отсутствие имени пользователя / заголовок авторизации в запросе.
     * </summary>
     *
     * @param exception Исключение {@link MissingPreferredUsernameException}.
     * @return DTO ошибки со статусом 401 UNAUTHORIZED.
     */
    @ExceptionHandler(MissingPreferredUsernameException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponseViewModel handleMissingPreferredUsername(MissingPreferredUsernameException exception) {
        return new ApiErrorResponseViewModel("UNAUTHORIZED", exception.getMessage());
    }
}