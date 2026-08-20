package ru.yandex.practicum.bank.blocker.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * <summary>
 * Конфигурационные свойства сервиса блокировки банковских операций.
 * Содержит максимальную допустимую сумму операции.
 * </summary>
 * @param maxAmount Максимальная допустимая сумма банковской операции.
 */
@ConfigurationProperties(prefix = "bank.services.blocker-service.blocker")
public record BlockerProperties (
        BigDecimal maxAmount
) {

    /**
     * <summary>
     * Создает конфигурационные свойства с заданным максимальным размером операции.
     * Если значение максимальной суммы не задано, устанавливается значение
     * по умолчанию 100000.00.
     * </summary>
     * @param maxAmount Максимальная допустимая сумма операции.
     */
    public BlockerProperties {
        if (maxAmount == null) {
            maxAmount = new BigDecimal("100000.00");
        }
    }
}