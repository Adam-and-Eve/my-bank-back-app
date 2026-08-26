package ru.yandex.practicum.bank.account.viewmodels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель запроса для проведения внутренней операции с балансом аккаунта (BalanceOperationRequestViewModel).
 * Используется для изменения баланса пользователя в межсервисном взаимодействии.
 * </summary>
 * @param login Логин пользователя, для которого выполняется операция.
 * @param amount Сумма операции с балансом.
 * @param currency Валюта операции.
 * @param operationId Уникальный идентификатор операции для обеспечения идемпотентности.
 **/
public record BalanceOperationRequestViewModel (
        @NotBlank
        String login,

        @NotNull
        BigDecimal amount,

        @NotNull
        CurrencyEnumModel currency,

        @NotBlank
        String operationId
) {
}