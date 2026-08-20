package ru.yandex.practicum.bank.shared.viewmodels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель запроса на проверку банковской операции.
 * Содержит идентификатор и тип операции, данные об участниках,
 * сумму операции, валюту операции, нормализованную сумму и базовую валюту.
 * </summary>
 * @param operationId Уникальный идентификатор операции.
 * @param operationType Тип банковской операции.
 * @param login Логин пользователя, инициировавшего операцию.
 * @param sender Отправитель денежных средств.
 * @param recipient Получатель денежных средств.
 * @param amount Сумма операции в исходной валюте.
 * @param currency Валюта операции.
 */
public record OperationCheckRequestViewModel (
        @NotBlank
        String operationId,

        @NotNull
        OperationTypeEnumModel operationType,

        String login,

        String sender,

        String recipient,

        @NotNull
        @Positive
        BigDecimal amount,

        @NotNull
        CurrencyEnumModel currency,

        @NotNull
        @Positive
        BigDecimal normalizedAmount,

        @NotNull
        CurrencyEnumModel baseCurrency
) {
}