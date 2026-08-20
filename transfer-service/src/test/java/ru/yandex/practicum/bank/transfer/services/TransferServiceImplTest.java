package ru.yandex.practicum.bank.transfer.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.shared.interfaces.BlockerClient;
import ru.yandex.practicum.bank.shared.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.shared.interfaces.NotificationClient;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.NotificationRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckResponseViewModel;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.transfer.exceptions.OperationBlockedException;
import ru.yandex.practicum.bank.transfer.exceptions.SelfTransferForbiddenException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferExecutor;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferOperationViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResultViewModel;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Модульные тесты для сервиса выполнения переводов TransferServiceImpl.
 * Проверяют валидацию суммы, запрет переводов самому себе,
 * проверку операций через Blocker Service, конвертацию валют,
 * вызов исполнителя перевода, отправку уведомлений и формирование ответа.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class TransferServiceImplTest {

    // region Fields

    @Mock
    private TransferExecutor transferExecutor;

    @Mock
    private BlockerClient blockerClient;

    @Mock
    private ExchangeClient exchangeClient;

    @Mock
    private NotificationClient notificationClient;

    private TransferServiceImpl transferService;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        transferService = new TransferServiceImpl(
                transferExecutor,
                blockerClient,
                exchangeClient,
                notificationClient
        );
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешный перевод в одной валюте: проверку операции через Blocker Service,
     * вызов исполнителя перевода, формирование операции с единым operationId,
     * отправку уведомления и возврат корректного ответа.
     * </summary>
     **/
    @Test
    public void shouldExecuteTransferSuccessfully() {
        var senderLogin = "dmitry";
        var recipientLogin = "alexey";
        var amount = new BigDecimal("200.00");
        var currency = CurrencyEnumModel.RUB;

        var request = new TransferRequestViewModel(
                recipientLogin,
                amount,
                currency
        );

        when(blockerClient.check(any(OperationCheckRequestViewModel.class)))
                .thenReturn(createAllowedBlockerResponse());

        var executorResult = new TransferResultViewModel(
                senderLogin,
                recipientLogin,
                new BigDecimal("800.00"),
                "RUB"
        );

        when(transferExecutor.execute(any(TransferOperationViewModel.class)))
                .thenReturn(executorResult);

        var response = transferService.transfer(senderLogin, request);

        assertThat(response).isNotNull();
        assertThat(response.senderLogin()).isEqualTo(senderLogin);
        assertThat(response.recipientLogin()).isEqualTo(recipientLogin);
        assertThat(response.senderBalance())
                .isEqualByComparingTo("800.00");
        assertThat(response.currency()).isEqualTo("RUB");
        assertThat(response.message()).isEqualTo("Transfer completed");

        var blockerCaptor =
                ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(blockerCaptor.capture());

        var blockerRequest = blockerCaptor.getValue();

        assertThat(blockerRequest.operationId()).isNotBlank();
        assertThat(blockerRequest.operationType())
                .isEqualTo(OperationTypeEnumModel.TRANSFER);
        assertThat(blockerRequest.login()).isNull();
        assertThat(blockerRequest.sender()).isEqualTo(senderLogin);
        assertThat(blockerRequest.recipient()).isEqualTo(recipientLogin);
        assertThat(blockerRequest.amount())
                .isEqualByComparingTo(amount);
        assertThat(blockerRequest.currency())
                .isEqualTo(currency);
        assertThat(blockerRequest.normalizedAmount())
                .isEqualByComparingTo(amount);
        assertThat(blockerRequest.baseCurrency())
                .isEqualTo(CurrencyEnumModel.RUB);

        var operationCaptor =
                ArgumentCaptor.forClass(TransferOperationViewModel.class);

        verify(transferExecutor).execute(operationCaptor.capture());

        var operation = operationCaptor.getValue();

        assertThat(operation.senderLogin()).isEqualTo(senderLogin);
        assertThat(operation.recipientLogin()).isEqualTo(recipientLogin);
        assertThat(operation.amount())
                .isEqualByComparingTo(amount);
        assertThat(operation.currency())
                .isEqualTo(currency);
        assertThat(operation.recipientAmount())
                .isEqualByComparingTo(amount);
        assertThat(operation.recipientCurrency())
                .isEqualTo(currency);
        assertThat(operation.operationId())
                .isEqualTo(blockerRequest.operationId());

        var notificationCaptor =
                ArgumentCaptor.forClass(NotificationRequestViewModel.class);

        verify(notificationClient).notify(notificationCaptor.capture());

        var notification = notificationCaptor.getValue();

        assertThat(notification.recipientLogin())
                .isEqualTo(senderLogin);
        assertThat(notification.type())
                .isEqualTo("TRANSFER_COMPLETED");
        assertThat(notification.message())
                .isEqualTo("Transfer completed to alexey: 200.00 RUB");
        assertThat(notification.operationId())
                .isEqualTo(operation.operationId());

        verifyNoInteractions(exchangeClient);
    }

    /**
     * <summary>
     * Проверяет выброс InvalidAmountException, если сумма перевода равна нулю.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountExceptionWhenAmountIsZero() {
        var request = new TransferRequestViewModel(
                "alexey",
                BigDecimal.ZERO,
                CurrencyEnumModel.RUB
        );

        assertThatThrownBy(() ->
                transferService.transfer("dmitry", request))
                .isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(
                transferExecutor,
                blockerClient,
                exchangeClient,
                notificationClient
        );
    }

    /**
     * <summary>
     * Проверяет выброс InvalidAmountException, если сумма перевода отрицательная.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountExceptionWhenAmountIsNegative() {
        var request = new TransferRequestViewModel(
                "alexey",
                new BigDecimal("-50.00"),
                CurrencyEnumModel.RUB
        );

        assertThatThrownBy(() ->
                transferService.transfer("dmitry", request))
                .isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(
                transferExecutor,
                blockerClient,
                exchangeClient,
                notificationClient
        );
    }

    /**
     * <summary>
     * Проверяет выброс InvalidAmountScaleException, если количество знаков
     * после запятой в сумме перевода превышает два.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountScaleExceptionWhenScaleExceedsTwo() {
        var request = new TransferRequestViewModel(
                "alexey",
                new BigDecimal("100.123"),
                CurrencyEnumModel.RUB
        );

        assertThatThrownBy(() ->
                transferService.transfer("dmitry", request))
                .isInstanceOf(InvalidAmountScaleException.class);

        verifyNoInteractions(
                transferExecutor,
                blockerClient,
                exchangeClient,
                notificationClient
        );
    }

    /**
     * <summary>
     * Проверяет выброс SelfTransferForbiddenException при попытке
     * перевода денежных средств самому себе.
     * </summary>
     **/
    @Test
    public void shouldThrowSelfTransferForbiddenExceptionWhenSenderEqualsRecipient() {
        var request = new TransferRequestViewModel(
                "dmitry",
                new BigDecimal("100.00"),
                CurrencyEnumModel.RUB
        );

        assertThatThrownBy(() ->
                transferService.transfer("dmitry", request))
                .isInstanceOf(SelfTransferForbiddenException.class);

        verifyNoInteractions(
                transferExecutor,
                blockerClient,
                exchangeClient,
                notificationClient
        );
    }

    /**
     * <summary>
     * Проверяет, что запрещённая Blocker Service операция прерывает перевод
     * и не передаётся исполнителю и сервису уведомлений.
     * </summary>
     **/
    @Test
    public void shouldThrowOperationBlockedExceptionWhenOperationIsBlocked() {
        var request = new TransferRequestViewModel(
                "alexey",
                new BigDecimal("200.00"),
                CurrencyEnumModel.RUB
        );

        when(blockerClient.check(any(OperationCheckRequestViewModel.class)))
                .thenReturn(new OperationCheckResponseViewModel(
                        false,
                        "Подозрительная операция"
                ));

        assertThatThrownBy(() ->
                transferService.transfer("dmitry", request))
                .isInstanceOf(OperationBlockedException.class)
                .hasMessage("Подозрительная операция");

        verify(blockerClient)
                .check(any(OperationCheckRequestViewModel.class));

        verifyNoInteractions(
                transferExecutor,
                notificationClient,
                exchangeClient
        );
    }

    /**
     * <summary>
     * Проверяет передачу нормализованной суммы иностранной валюты
     * в Blocker Service в базовой валюте RUB.
     * </summary>
     **/
    @Test
    public void shouldNormalizeForeignCurrencyAmountForBlocker() {
        var amount = new BigDecimal("100.00");

        var request = new TransferRequestViewModel(
                "alexey",
                amount,
                CurrencyEnumModel.USD
        );

        when(exchangeClient.convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                amount
        )).thenReturn(new ConversionResponseViewModel(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                amount,
                new BigDecimal("9000.00"),
                new BigDecimal("90.00"),
                null
        ));

        when(blockerClient.check(any(OperationCheckRequestViewModel.class)))
                .thenReturn(createAllowedBlockerResponse());

        when(transferExecutor.execute(any(TransferOperationViewModel.class)))
                .thenReturn(new TransferResultViewModel(
                        "dmitry",
                        "alexey",
                        new BigDecimal("800.00"),
                        "USD"
                ));

        transferService.transfer("dmitry", request);

        var captor =
                ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(captor.capture());

        var blockerRequest = captor.getValue();

        assertThat(blockerRequest.amount())
                .isEqualByComparingTo("100.00");

        assertThat(blockerRequest.currency())
                .isEqualTo(CurrencyEnumModel.USD);

        assertThat(blockerRequest.normalizedAmount())
                .isEqualByComparingTo("9000.00");

        assertThat(blockerRequest.baseCurrency())
                .isEqualTo(CurrencyEnumModel.RUB);
    }

    /**
     * <summary>
     * Проверяет конвертацию суммы перевода из исходной валюты
     * в валюту получателя и передачу результата в TransferExecutor.
     * </summary>
     **/
    @Test
    public void shouldConvertAmountToRecipientCurrency() {
        var amount = new BigDecimal("100.00");

        var request = new TransferRequestViewModel(
                "alexey",
                amount,
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB
        );

        var conversion = new ConversionResponseViewModel(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                amount,
                new BigDecimal("92.00"),
                new BigDecimal("0.92"),
                null
        );

        when(exchangeClient.convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                amount
        )).thenReturn(conversion);

        when(blockerClient.check(any(OperationCheckRequestViewModel.class)))
                .thenReturn(createAllowedBlockerResponse());

        when(transferExecutor.execute(any(TransferOperationViewModel.class)))
                .thenReturn(new TransferResultViewModel(
                        "dmitry",
                        "alexey",
                        new BigDecimal("800.00"),
                        "RUB"
                ));

        transferService.transfer("dmitry", request);

        var captor =
                ArgumentCaptor.forClass(TransferOperationViewModel.class);

        verify(transferExecutor).execute(captor.capture());

        var operation = captor.getValue();

        assertThat(operation.amount())
                .isEqualByComparingTo("100.00");

        assertThat(operation.currency())
                .isEqualTo(CurrencyEnumModel.USD);

        assertThat(operation.recipientAmount())
                .isEqualByComparingTo("92.00");

        assertThat(operation.recipientCurrency())
                .isEqualTo(CurrencyEnumModel.RUB);
    }

    /**
     * <summary>
     * Проверяет формирование уведомления с исходной и сконвертированной
     * суммами при переводе между разными валютами.
     * </summary>
     **/
    @Test
    public void shouldSendNotificationWithConversionDetails() {
        var amount = new BigDecimal("100.00");

        var request = new TransferRequestViewModel(
                "alexey",
                amount,
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB
        );

        var conversion = new ConversionResponseViewModel(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                amount,
                new BigDecimal("92.00"),
                new BigDecimal("0.92"),
                null
        );

        when(exchangeClient.convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                amount
        )).thenReturn(conversion);

        when(blockerClient.check(any(OperationCheckRequestViewModel.class)))
                .thenReturn(createAllowedBlockerResponse());

        when(transferExecutor.execute(any(TransferOperationViewModel.class)))
                .thenReturn(new TransferResultViewModel(
                        "dmitry",
                        "alexey",
                        new BigDecimal("800.00"),
                        "RUB"
                ));

        transferService.transfer("dmitry", request);

        var captor =
                ArgumentCaptor.forClass(NotificationRequestViewModel.class);

        verify(notificationClient).notify(captor.capture());

        var notification = captor.getValue();

        assertThat(notification.recipientLogin())
                .isEqualTo("dmitry");

        assertThat(notification.type())
                .isEqualTo("TRANSFER_COMPLETED");

        assertThat(notification.message())
                .isEqualTo(
                        "Transfer completed to alexey: "
                                + "100.00 USD -> 92.00 RUB"
                );
    }

    /**
     * <summary>
     * Проверяет, что при совпадении исходной и целевой валюты
     * Exchange Service не вызывается для конвертации суммы получателя.
     * При этом для проверки операции через Blocker Service
     * выполняется нормализация исходной суммы в RUB.
     * </summary>
     **/
    @Test
    public void shouldNotConvertWhenSourceAndTargetCurrenciesAreEqual() {
        var amount = new BigDecimal("100.00");

        var request = new TransferRequestViewModel(
                "alexey",
                amount,
                CurrencyEnumModel.USD,
                CurrencyEnumModel.USD
        );

        when(exchangeClient.convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                amount
        )).thenReturn(new ConversionResponseViewModel(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                amount,
                new BigDecimal("9000.00"),
                new BigDecimal("90.00"),
                null
        ));

        when(blockerClient.check(any(OperationCheckRequestViewModel.class)))
                .thenReturn(createAllowedBlockerResponse());

        when(transferExecutor.execute(any(TransferOperationViewModel.class)))
                .thenReturn(new TransferResultViewModel(
                        "dmitry",
                        "alexey",
                        new BigDecimal("800.00"),
                        "USD"
                ));

        transferService.transfer("dmitry", request);

        verify(exchangeClient, never()).convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.USD,
                amount
        );

        var captor =
                ArgumentCaptor.forClass(TransferOperationViewModel.class);

        verify(transferExecutor).execute(captor.capture());

        var operation = captor.getValue();

        assertThat(operation.amount())
                .isEqualByComparingTo(amount);

        assertThat(operation.currency())
                .isEqualTo(CurrencyEnumModel.USD);

        assertThat(operation.recipientAmount())
                .isEqualByComparingTo(amount);

        assertThat(operation.recipientCurrency())
                .isEqualTo(CurrencyEnumModel.USD);
    }

    /**
     * <summary>
     * Проверяет, что при сбое исполнителя перевода уведомление
     * пользователю не отправляется.
     * </summary>
     **/
    @Test
    public void shouldNotNotifyWhenTransferExecutorFails() {
        var request = new TransferRequestViewModel(
                "alexey",
                new BigDecimal("200.00"),
                CurrencyEnumModel.RUB
        );

        when(blockerClient.check(any(OperationCheckRequestViewModel.class)))
                .thenReturn(createAllowedBlockerResponse());

        when(transferExecutor.execute(any(TransferOperationViewModel.class)))
                .thenThrow(new RuntimeException("Account service error"));

        assertThatThrownBy(() ->
                transferService.transfer("dmitry", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account service error");

        verifyNoInteractions(notificationClient);
    }

    /**
     * <summary>
     * Проверяет, что при запрете операции исполнение перевода
     * не передаётся TransferExecutor и уведомление не отправляется.
     * </summary>
     **/
    @Test
    public void shouldNotExecuteTransferWhenOperationIsBlocked() {
        var request = new TransferRequestViewModel(
                "alexey",
                new BigDecimal("200.00"),
                CurrencyEnumModel.RUB
        );

        when(blockerClient.check(any(OperationCheckRequestViewModel.class)))
                .thenReturn(new OperationCheckResponseViewModel(
                        false,
                        "Подозрительная операция"
                ));

        assertThatThrownBy(() ->
                transferService.transfer("dmitry", request))
                .isInstanceOf(OperationBlockedException.class);

        verify(transferExecutor, never())
                .execute(any(TransferOperationViewModel.class));

        verify(notificationClient, never())
                .notify(any(NotificationRequestViewModel.class));
    }

    // endregion

    // region Private Methods

    private OperationCheckResponseViewModel createAllowedBlockerResponse() {
        return new OperationCheckResponseViewModel(
                true,
                null
        );
    }

    // endregion
}