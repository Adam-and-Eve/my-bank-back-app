package ru.yandex.practicum.bank.account.viewmodels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.bank.account.models.CurrencyEnumModel;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель запроса для межсервисного перевода средств между аккаунтами (TransferBalanceRequestViewModel).
 * Содержит данные отправителя, получателя, параметры суммы и уникальный идентификатор для обеспечения идемпотентности.
 * </summary>
 * @param senderLogin Логин пользователя, со счёта которого списываются средства.
 * @param recipientLogin Логин пользователя, на счёт которого зачисляются средства.
 * @param amount Сумма перевода.
 * @param currency Валюта операции перевода.
 * @param operationId Уникальный идентификатор операции для обеспечения идемпотентности.
 **/
public record TransferBalanceRequestViewModel (
        @NotBlank
        String senderLogin,

        @NotBlank
        String recipientLogin,

        @NotNull
        BigDecimal amount,

        @NotNull
        CurrencyEnumModel currency,

        @NotBlank
        String operationId
) {
}