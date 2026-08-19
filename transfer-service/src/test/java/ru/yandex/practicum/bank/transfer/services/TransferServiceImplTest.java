package ru.yandex.practicum.bank.transfer.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.bank.shared.interfaces.NotificationClient;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountException;
import ru.yandex.practicum.bank.transfer.exceptions.InvalidAmountScaleException;
import ru.yandex.practicum.bank.transfer.exceptions.SelfTransferForbiddenException;
import ru.yandex.practicum.bank.transfer.interfaces.TransferExecutor;
import ru.yandex.practicum.bank.shared.viewmodels.NotificationRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferOperationViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResultViewModel;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Модульные тесты для сервиса выполнения переводов TransferServiceImpl.
 * Проверяют бизнес-логику валидации суммы, запрет переводов самому себе,
 * вызов исполнителя операции, отправку уведомлений и форматирование ответа.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class TransferServiceImplTest {

    // region Fields

    @Mock
    private TransferExecutor transferExecutor;

    @Mock
    private NotificationClient notificationClient;

    private TransferServiceImpl transferService;

    // endregion

    // region Setup

    @BeforeEach
    public void setUp() {
        transferService = new TransferServiceImpl(transferExecutor, notificationClient);
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешный перевод средств: валидацию, вызов исполнителя, отправку уведомления с единым operationId и возврат ответа.
     * </summary>
     **/
    @Test
    public void shouldExecuteTransferSuccessfully() {
        var senderLogin = "dmitry";

        var recipientLogin = "alexey";

        var amount = new BigDecimal("200.00");

        var currency = CurrencyEnumModel.RUB;

        var request = new TransferRequestViewModel(recipientLogin, amount, currency);

        var executorResult = new TransferResultViewModel(senderLogin, recipientLogin, new BigDecimal("800.00"), "RUB");

        when(transferExecutor.execute(any(TransferOperationViewModel.class))).thenReturn(executorResult);

        var response = transferService.transfer(senderLogin, request);

        assertThat(response).isNotNull();

        assertThat(response.senderLogin()).isEqualTo(senderLogin);

        assertThat(response.recipientLogin()).isEqualTo(recipientLogin);

        assertThat(response.senderBalance()).isEqualTo(new BigDecimal("800.00"));

        assertThat(response.currency()).isEqualTo("RUB");

        assertThat(response.message()).isEqualTo("Transfer completed");

        var operationCaptor = ArgumentCaptor.forClass(TransferOperationViewModel.class);

        verify(transferExecutor).execute(operationCaptor.capture());

        var capturedOperation = operationCaptor.getValue();

        assertThat(capturedOperation.senderLogin()).isEqualTo(senderLogin);

        assertThat(capturedOperation.recipientLogin()).isEqualTo(recipientLogin);

        assertThat(capturedOperation.amount()).isEqualTo(amount);

        assertThat(capturedOperation.currency()).isEqualTo(currency);

        assertThat(capturedOperation.operationId()).isNotNull();

        var notificationCaptor = ArgumentCaptor.forClass(NotificationRequestViewModel.class);

        verify(notificationClient).notify(notificationCaptor.capture());

        var capturedNotification = notificationCaptor.getValue();

        assertThat(capturedNotification.recipientLogin()).isEqualTo(senderLogin);

        assertThat(capturedNotification.type()).isEqualTo("TRANSFER_COMPLETED");

        assertThat(capturedNotification.message()).isEqualTo("Transfer completed to alexey: 200.00 RUB");

        assertThat(capturedNotification.operationId()).isEqualTo(capturedOperation.operationId());
    }

    /**
     * <summary>
     * Проверяет выброс InvalidAmountException, если сумма перевода равна нулю.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountExceptionWhenAmountIsZero() {
        var request = new TransferRequestViewModel("alexey", BigDecimal.ZERO, CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> transferService.transfer("dmitry", request))
                .isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(transferExecutor, notificationClient);
    }

    /**
     * <summary>
     * Проверяет выброс InvalidAmountException, если сумма перевода отрицательная.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountExceptionWhenAmountIsNegative() {
        var request = new TransferRequestViewModel("alexey", new BigDecimal("-50.00"), CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> transferService.transfer("dmitry", request))
                .isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(transferExecutor, notificationClient);
    }

    /**
     * <summary>
     * Проверяет выброс InvalidAmountScaleException, если количество знаков после запятой в сумме больше 2.
     * </summary>
     **/
    @Test
    public void shouldThrowInvalidAmountScaleExceptionWhenScaleExceedsTwo() {
        var request = new TransferRequestViewModel("alexey", new BigDecimal("100.123"), CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> transferService.transfer("dmitry", request))
                .isInstanceOf(InvalidAmountScaleException.class);

        verifyNoInteractions(transferExecutor, notificationClient);
    }

    /**
     * <summary>
     * Проверяет выброс SelfTransferForbiddenException при попытке перевода самому себе.
     * </summary>
     **/
    @Test
    public void shouldThrowSelfTransferForbiddenExceptionWhenSenderEqualsRecipient() {
        var request = new TransferRequestViewModel("dmitry", new BigDecimal("100.00"), CurrencyEnumModel.RUB);

        assertThatThrownBy(() -> transferService.transfer("dmitry", request))
                .isInstanceOf(SelfTransferForbiddenException.class);

        verifyNoInteractions(transferExecutor, notificationClient);
    }

    /**
     * <summary>
     * Проверяет, что при сбое в исполнителе перевода (transferExecutor) отправка уведомления не вызывается.
     * </summary>
     **/
    @Test
    public void shouldNotNotifyWhenTransferExecutorFails() {
        var senderLogin = "dmitry";

        var request = new TransferRequestViewModel("alexey", new BigDecimal("200.00"), CurrencyEnumModel.RUB);

        when(transferExecutor.execute(any())).thenThrow(new RuntimeException("Account service error"));

        assertThatThrownBy(() -> transferService.transfer(senderLogin, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account service error");

        verifyNoInteractions(notificationClient);
    }

    // endregion
}