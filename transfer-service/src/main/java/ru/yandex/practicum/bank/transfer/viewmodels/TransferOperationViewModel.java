package ru.yandex.practicum.bank.transfer.viewmodels;

import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;

import java.math.BigDecimal;
import java.util.List;

/**
 * <summary>
 * Модель представления (ViewModel) для передачи полных данных о выполняемой операции перевода.
 * </summary>
 * @param senderLogin Уникальный логин пользователя-отправителя перевода.
 * @param recipientLogin Уникальный логин пользователя-получателя перевода.
 * @param amount Сумма перевода.
 * @param currency Валюта выполнения операции.
 * @param operationId Уникальный идентификатор операции для обеспечения идемпотентности.
 **/
public record TransferOperationViewModel (
        String senderLogin,
        String recipientLogin,
        BigDecimal amount,
        CurrencyEnumModel currency,
        BigDecimal recipientAmount,
        CurrencyEnumModel recipientCurrency,
        String operationId,
        List<NotificationEventModel> notifications
) {
    public TransferOperationViewModel(
            String senderLogin,
            String recipientLogin,
            BigDecimal amount,
            CurrencyEnumModel currency,
            String operationId,
            List<NotificationEventModel> notifications
    ) {
        this(senderLogin, recipientLogin, amount, currency, amount, currency, operationId, notifications);
    }
}