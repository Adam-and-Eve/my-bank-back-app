package ru.yandex.practicum.bank.account.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <summary>
 * Конфигурационные свойства для настройки механизма повторных попыток (retry)
 * при операциях с балансом счета.
 * </summary>
 * @param maxAttempts Максимальное количество попыток выполнения операции.
 * @param backoffMs Базовая задержка между повторными попытками в миллисекундах (backoff).
 **/
@ConfigurationProperties(prefix = "bank.balance.retry")
public record BalanceRetryProperties(
        int maxAttempts,
        long backoffMs
) {
}