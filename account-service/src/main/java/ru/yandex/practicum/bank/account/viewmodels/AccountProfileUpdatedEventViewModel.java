package ru.yandex.practicum.bank.account.viewmodels;

import java.time.Instant;
import java.util.UUID;

/**
 * <summary>
 * Модель события (Event/ViewModel), сигнализирующая об успешном обновлении профиля пользователя.
 * Используется внутренними слушателями приложения (например, AccountProfileUpdatedNotificationListener)
 * для формирования и асинхронной отправки уведомлений клиенту.
 * </summary>
 * @param operationId Уникальный идентификатор бизнес-операции, в рамках которой произошло обновление.
 * @param recipientLogin Логин пользователя, чей профиль был изменен (используется как адресат для уведомлений).
 * @param occurredAt Точная временная метка совершения операции обновления.
 **/
public record AccountProfileUpdatedEventViewModel(
        UUID operationId,
        String recipientLogin,
        Instant occurredAt
) {
}