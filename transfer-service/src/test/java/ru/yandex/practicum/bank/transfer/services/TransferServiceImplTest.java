package ru.yandex.practicum.bank.transfer.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.shared.interfaces.BlockerClient;
import ru.yandex.practicum.bank.shared.interfaces.ExchangeClient;
import ru.yandex.practicum.bank.shared.interfaces.NotificationEventPublisher;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Модульные тесты для сервиса выполнения переводов TransferServiceImpl.
 * Проверяют валидацию суммы, запрет перевода самому себе,
 * проверку операций через Blocker Service (с предварительной нормализацией в RUB),
 * конвертацию валют, вызов исполнителя и отправку парных уведомлений в Kafka.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class TransferServiceImplTest {

    // region Constants

    private static final UUID TEST_OPERATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-27T12:00:00Z");

    // endregion

    // region Fields

    @Mock
    private TransferExecutor transferExecutor;

    @Mock
    private BlockerClient blockerClient;

    @Mock
    private ExchangeClient exchangeClient;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private Clock clock;

    private TransferServiceImpl transferService;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        clock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));

        transferService = new TransferServiceImpl(
                transferExecutor,
                blockerClient,
                exchangeClient,
                notificationEventPublisher,
                clock
        );
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешный перевод в национальной валюте (RUB).
     * Валидирует обращение к Blocker Service, выполнение через TransferExecutor,
     * и отправку двух событий в Kafka (отправителю и получателю).
     * </summary>
     **/
    @Test
    public void shouldExecuteTransferSuccessfullyInRubles() {
        var senderLogin = "dmitry";

        var recipientLogin = "alexey";

        var amount = new BigDecimal("200.00");

        var currency = CurrencyEnumModel.RUB;

        var request = new TransferRequestViewModel(recipientLogin, amount, currency);

        when(blockerClient.check(any(OperationCheckRequestViewModel.class)))
                .thenReturn(new OperationCheckResponseViewModel(true, null));

        when(transferExecutor.execute(any(TransferOperationViewModel.class)))
                .thenReturn(new TransferResultViewModel(senderLogin, recipientLogin, new BigDecimal("800.00"), "RUB"));

        var response = transferService.transfer(senderLogin, request, TEST_OPERATION_ID);

        assertThat(response).isNotNull();

        assertThat(response.senderLogin()).isEqualTo(senderLogin);

        assertThat(response.recipientLogin()).isEqualTo(recipientLogin);

        assertThat(response.senderBalance()).isEqualByComparingTo("800.00");

        assertThat(response.currency()).isEqualTo("RUB");

        assertThat(response.message()).isEqualTo("Transfer completed");

        var blockerCaptor = ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(blockerCaptor.capture());

        var blockerRequest = blockerCaptor.getValue();

        assertThat(blockerRequest.operationId()).isEqualTo(TEST_OPERATION_ID.toString());

        assertThat(blockerRequest.operationType()).isEqualTo(OperationTypeEnumModel.TRANSFER);

        assertThat(blockerRequest.sender()).isEqualTo(senderLogin);

        assertThat(blockerRequest.recipient()).isEqualTo(recipientLogin);

        assertThat(blockerRequest.amount()).isEqualByComparingTo(amount);

        assertThat(blockerRequest.normalizedAmount()).isEqualByComparingTo(amount);

        var operationCaptor = ArgumentCaptor.forClass(TransferOperationViewModel.class);

        verify(transferExecutor).execute(operationCaptor.capture());

        var operation = operationCaptor.getValue();

        assertThat(operation.senderLogin()).isEqualTo(senderLogin);

        assertThat(operation.recipientLogin()).isEqualTo(recipientLogin);

        assertThat(operation.amount()).isEqualByComparingTo(amount);

        assertThat(operation.recipientAmount()).isEqualByComparingTo(amount);

        assertThat(operation.operationId()).isEqualTo(TEST_OPERATION_ID.toString());

        verifyNoInteractions(exchangeClient);
    }

    /**
     * <summary>
     * Проверяет, что при несовпадении валют происходит обращение к Exchange Service,
     * а суммы/валюты корректно проставляются в уведомлениях для отправителя и получателя.
     * </summary>
     **/
    @Test
    public void shouldConvertCurrencyAndSendAccurateNotifications() {
        var senderLogin = "dmitry";

        var recipientLogin = "alexey";

        var amount = new BigDecimal("100.00");

        var request = new TransferRequestViewModel(recipientLogin, amount, CurrencyEnumModel.USD, CurrencyEnumModel.RUB);

        var conversion = new ConversionResponseViewModel(
                CurrencyEnumModel.USD, CurrencyEnumModel.RUB, amount,
                new BigDecimal("9200.00"), new BigDecimal("92.00"), null
        );

        when(exchangeClient.convert(CurrencyEnumModel.USD, CurrencyEnumModel.RUB, amount)).thenReturn(conversion);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(true, null));

        when(transferExecutor.execute(any())).thenReturn(new TransferResultViewModel(senderLogin, recipientLogin, new BigDecimal("800.00"), "USD"));

        transferService.transfer(senderLogin, request, TEST_OPERATION_ID);

        var notificationCaptor = ArgumentCaptor.forClass(NotificationEventModel.class);

        verify(notificationEventPublisher, times(2)).publish(notificationCaptor.capture());

        var notifications = notificationCaptor.getAllValues();

        var outgoing = notifications.get(0);

        var incoming = notifications.get(1);

        assertThat(outgoing.recipientLogin()).isEqualTo(senderLogin);
        assertThat(outgoing.type()).isEqualTo(NotificationTypeEnumModel.TRANSFER_OUTGOING);

        assertThat(outgoing.message()).isEqualTo("Перевод пользователю alexey: 100.00 USD");

        assertThat(outgoing.amount()).isEqualByComparingTo("100.00");

        assertThat(outgoing.currency()).isEqualTo(CurrencyEnumModel.USD);

        assertThat(incoming.recipientLogin()).isEqualTo(recipientLogin);

        assertThat(incoming.type()).isEqualTo(NotificationTypeEnumModel.TRANSFER_INCOMING);

        assertThat(incoming.message()).isEqualTo("Получен перевод от dmitry: 9200.00 RUB");

        assertThat(incoming.amount()).isEqualByComparingTo("9200.00");

        assertThat(incoming.currency()).isEqualTo(CurrencyEnumModel.RUB);
    }

    /**
     * <summary>
     * Проверяет, что при отправке USD -> USD конвертация для получателя пропускается,
     * но нормализация суммы для Blocker Service в RUB выполняется.
     * </summary>
     **/
    @Test
    public void shouldNormalizeForeignCurrencyForBlockerButSkipConversionIfTargetIsSame() {
        var request = new TransferRequestViewModel("alexey", new BigDecimal("10.00"), CurrencyEnumModel.USD, CurrencyEnumModel.USD);

        var normalizedConversion = new ConversionResponseViewModel(
                CurrencyEnumModel.USD, CurrencyEnumModel.RUB, new BigDecimal("10.00"),
                new BigDecimal("920.00"), new BigDecimal("92.00"), null
        );

        when(exchangeClient.convert(CurrencyEnumModel.USD, CurrencyEnumModel.RUB, new BigDecimal("10.00")))
                .thenReturn(normalizedConversion);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(true, null));

        when(transferExecutor.execute(any())).thenReturn(new TransferResultViewModel("dmitry", "alexey", new BigDecimal("80.00"), "USD"));

        transferService.transfer("dmitry", request, TEST_OPERATION_ID);

        var blockerCaptor = ArgumentCaptor.forClass(OperationCheckRequestViewModel.class);

        verify(blockerClient).check(blockerCaptor.capture());

        assertThat(blockerCaptor.getValue().normalizedAmount()).isEqualByComparingTo("920.00");

        verify(exchangeClient, never()).convert(CurrencyEnumModel.USD, CurrencyEnumModel.USD, new BigDecimal("10.00"));
    }

    /**
     * <summary>
     * Проверяет выброс InvalidAmountException, если сумма перевода равна нулю.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountExceptionWhenAmountIsZero() {
        var request = new TransferRequestViewModel("alexey", BigDecimal.ZERO, CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> transferService.transfer("dmitry", request, TEST_OPERATION_ID))
                .isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(transferExecutor, blockerClient, exchangeClient, notificationEventPublisher);
    }

    /**
     * <summary>
     * Проверяет выброс InvalidAmountException, если сумма перевода отрицательная.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountExceptionWhenAmountIsNegative() {
        var request = new TransferRequestViewModel("alexey", new BigDecimal("-50.00"), CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> transferService.transfer("dmitry", request, TEST_OPERATION_ID))
                .isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(transferExecutor, blockerClient, exchangeClient, notificationEventPublisher);
    }

    /**
     * <summary>
     * Проверяет выброс InvalidAmountScaleException, если количество знаков после запятой > 2.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountScaleExceptionWhenScaleExceedsTwo() {
        var request = new TransferRequestViewModel("alexey", new BigDecimal("100.123"), CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> transferService.transfer("dmitry", request, TEST_OPERATION_ID))
                .isInstanceOf(InvalidAmountScaleException.class);

        verifyNoInteractions(transferExecutor, blockerClient, exchangeClient, notificationEventPublisher);
    }

    /**
     * <summary>
     * Проверяет выброс SelfTransferForbiddenException при попытке перевода самому себе.
     * </summary>
     **/
    @Test
    public void shouldThrowSelfTransferForbiddenExceptionWhenSenderEqualsRecipient() {
        var request = new TransferRequestViewModel("dmitry", new BigDecimal("100.00"), CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> transferService.transfer("dmitry", request, TEST_OPERATION_ID))
                .isInstanceOf(SelfTransferForbiddenException.class);

        verifyNoInteractions(transferExecutor, blockerClient, exchangeClient, notificationEventPublisher);
    }

    /**
     * <summary>
     * Проверяет прерывание операции с OperationBlockedException, если Blocker Service запретил перевод.
     * </summary>
     **/
    @Test
    public void shouldThrowOperationBlockedExceptionWhenOperationIsBlocked() {
        var request = new TransferRequestViewModel("alexey", new BigDecimal("200.00"), CurrencyEnumModel.RUB);

        when(blockerClient.check(any(OperationCheckRequestViewModel.class)))
                .thenReturn(new OperationCheckResponseViewModel(false, "Подозрительный получатель"));

        assertThatThrownBy(() -> transferService.transfer("dmitry", request, TEST_OPERATION_ID))
                .isInstanceOf(OperationBlockedException.class)
                .hasMessage("Подозрительный получатель");

        verify(transferExecutor, never()).execute(any());

        verifyNoInteractions(notificationEventPublisher);
    }

    /**
     * <summary>
     * Проверяет, что при сбое в сервисе счетов (исключение в TransferExecutor)
     * уведомления не публикуются.
     * </summary>
     **/
    @Test
    public void shouldNotNotifyWhenTransferExecutorFails() {
        var request = new TransferRequestViewModel("alexey", new BigDecimal("200.00"), CurrencyEnumModel.RUB);

        when(blockerClient.check(any())).thenReturn(new OperationCheckResponseViewModel(true, null));

        when(transferExecutor.execute(any())).thenThrow(new RuntimeException("Account service unavailable"));

        assertThatThrownBy(() -> transferService.transfer("dmitry", request, TEST_OPERATION_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account service unavailable");

        verifyNoInteractions(notificationEventPublisher);
    }

    // endregion
}