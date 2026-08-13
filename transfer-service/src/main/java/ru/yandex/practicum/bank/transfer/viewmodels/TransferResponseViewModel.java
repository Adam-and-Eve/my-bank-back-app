package ru.yandex.practicum.bank.transfer.viewmodels;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

/**
 * <summary>
 * Модель представления (ViewModel) ответа на успешный перевод денежных средств.
 * </summary>
 * @param senderLogin Логин пользователя-отправителя.
 * @param recipientLogin Логин пользователя-получателя.
 * @param senderBalance Обновленный баланс счета отправителя после выполнения перевода.
 * @param currency Валюта операции.
 * @param message Информационное сообщение о результате выполнения перевода.
 **/
public record TransferResponseViewModel (
        String senderLogin,
        String recipientLogin,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal senderBalance,

        String currency,
        String message
) {
}