package ru.yandex.practicum.bank.transfer.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.bank.shared.exceptions.BlockerClientException;
import ru.yandex.practicum.bank.shared.exceptions.ExchangeClientException;
import ru.yandex.practicum.bank.shared.viewmodels.ApiErrorResponseViewModel;

/**
 * <summary>
 * Глобальный обработчик исключений (Controller Advice) для REST-контроллеров сервиса переводов (Transfer Service).
 * Перехватывает исключения бизнес-логики, валидации и межсервисного взаимодействия,
 * преобразуя их в унифицированные DTO ответов об ошибках ApiErrorResponseViewModel с соответствующими HTTP-статусами.
 * </summary>
 **/
@RestControllerAdvice
public class TransferExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TransferExceptionHandler.class);

    @ExceptionHandler(InvalidAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmount(InvalidAmountException exception) {
        log.warn("Transfer request rejected status=bad_request errorCode=INVALID_AMOUNT source=transfer-service");

        return new ApiErrorResponseViewModel("INVALID_AMOUNT", exception.getMessage());
    }

    @ExceptionHandler(InvalidAmountScaleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleInvalidAmountScale(InvalidAmountScaleException exception) {
        log.warn("Transfer request rejected status=bad_request errorCode=INVALID_AMOUNT_SCALE source=transfer-service");

        return new ApiErrorResponseViewModel("INVALID_AMOUNT_SCALE", exception.getMessage());
    }

    @ExceptionHandler(SelfTransferForbiddenException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponseViewModel handleSelfTransfer(SelfTransferForbiddenException exception) {
        log.warn("Transfer request rejected status=unprocessable_entity errorCode=SELF_TRANSFER_FORBIDDEN source=transfer-service");

        return new ApiErrorResponseViewModel("SELF_TRANSFER_FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(OperationBlockedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponseViewModel handleOperationBlocked(OperationBlockedException exception) {
        log.warn("Transfer request rejected status=unprocessable_entity errorCode=OPERATION_BLOCKED source=transfer-service");

        return new ApiErrorResponseViewModel("OPERATION_BLOCKED", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponseViewModel handleValidation(MethodArgumentNotValidException exception) {
        log.warn("Transfer request rejected status=bad_request errorCode=VALIDATION_ERROR source=transfer-service");

        return new ApiErrorResponseViewModel("VALIDATION_ERROR", exception.getMessage());
    }

    @ExceptionHandler(MissingPreferredUsernameException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponseViewModel handleMissingPreferredUsername(MissingPreferredUsernameException exception) {
        log.warn("Transfer request rejected status=unauthorized errorCode=UNAUTHORIZED source=transfer-service");

        return new ApiErrorResponseViewModel("UNAUTHORIZED", exception.getMessage());
    }

    @ExceptionHandler(AccountClientException.class)
    public ResponseEntity<ApiErrorResponseViewModel> handleAccountsClient(AccountClientException exception) {
        if (exception.getStatusCode().is4xxClientError()) {
            log.warn(
                    "Transfer downstream request rejected status={} errorCode={} source=transfer-service targetService=account-service",
                    exception.getStatusCode().value(),
                    exception.getCode()
            );

            return ResponseEntity.status(exception.getStatusCode())
                    .body(new ApiErrorResponseViewModel(exception.getCode(), exception.getMessage()));
        }

        log.error(
                "Transfer downstream request failed status={} errorCode={} errorCategory=downstream_unavailable errorType={} source=transfer-service targetService=account-service",
                exception.getStatusCode().value(),
                exception.getCode(),
                exception.getClass().getSimpleName()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponseViewModel("ACCOUNT_SERVICE_UNAVAILABLE", exception.getMessage()));
    }

    @ExceptionHandler(BlockerClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponseViewModel handleBlockerClient(BlockerClientException exception) {
        log.error(
                "Transfer downstream request failed status=502 errorCode=BLOCKER_SERVICE_UNAVAILABLE errorCategory=downstream_unavailable errorType={} source=transfer-service targetService=blocker-service",
                exception.getClass().getSimpleName()
        );
        return new ApiErrorResponseViewModel("BLOCKER_SERVICE_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(ExchangeClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponseViewModel handleExchangeClient(ExchangeClientException exception) {
        log.error(
                "Transfer downstream request failed status=502 errorCode=EXCHANGE_SERVICE_UNAVAILABLE errorCategory=downstream_unavailable errorType={} source=transfer-service targetService=exchange-service",
                exception.getClass().getSimpleName()
        );

        return new ApiErrorResponseViewModel("EXCHANGE_SERVICE_UNAVAILABLE", exception.getMessage());
    }
}