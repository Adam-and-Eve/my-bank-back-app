package ru.yandex.practicum.bank.exchange.models;

import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateSnapshotModel (
        CurrencyEnumModel currency,
        BigDecimal buyRate,
        BigDecimal sellRate,
        Instant updatedAt
) {
}