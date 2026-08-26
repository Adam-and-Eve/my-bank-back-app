package ru.yandex.practicum.bank.account.viewmodels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель запроса для межсервисного перевода средств между аккаунтами (TransferBalanceRequestViewModel).
 * Содержит данные отправителя и получателя, сумму и валюту списания,
 * сумму и валюту зачисления, а также уникальный идентификатор для обеспечения идемпотентности.
 * </summary>
 * @param senderLogin Логин пользователя, со счёта которого списываются средства.
 * @param recipientLogin Логин пользователя, на счёт которого зачисляются средства.
 * @param amount Сумма списания со счёта отправителя.
 * @param currency Валюта списания со счёта отправителя.
 * @param recipientAmount Сумма зачисления на счёт получателя после конвертации.
 * @param recipientCurrency Валюта зачисления на счёт получателя после конвертации.
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

        BigDecimal recipientAmount,

        CurrencyEnumModel recipientCurrency,

        @NotBlank
        String operationId
) {

        public TransferBalanceRequestViewModel(
                String senderLogin,
                String recipientLogin,
                BigDecimal amount,
                CurrencyEnumModel currency,
                String operationId
        ) {
                this(senderLogin, recipientLogin, amount, currency, amount, currency, operationId);
        }

        /**
         * <summary>
         * Возвращает сумму, которая должна быть зачислена получателю.
         * Если сумма получателя не задана, используется исходная сумма перевода.
         * </summary>
         * @return Сумма зачисления на счёт получателя.
         */
        public BigDecimal resolvedRecipientAmount() {
                return recipientAmount == null ? amount : recipientAmount;
        }

        /**
         * <summary>
         * Возвращает валюту, в которой должна быть выполнена операция зачисления получателю.
         * Если валюта получателя не задана, используется исходная валюта перевода.
         * </summary>
         * @return Валюта зачисления на счёт получателя.
         */
        public CurrencyEnumModel resolvedRecipientCurrency() {
                return recipientCurrency == null ? currency : recipientCurrency;
        }
}