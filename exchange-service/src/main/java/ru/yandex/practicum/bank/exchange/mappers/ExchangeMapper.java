package ru.yandex.practicum.bank.exchange.mappers;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.exchange.models.ExchangeRateSnapshotModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;

/**
 * <summary>
 * Компонент для преобразования моделей обменных курсов
 * в модели ответов API.
 * </summary>
 */
@Component
public class ExchangeMapper {

    // region Methods

    /**
     * <summary>
     * Преобразует снимок курса валюты в модель ответа с информацией о курсе.
     * </summary>
     * @param snapshot Снимок курса валюты.
     * @return Модель ответа с информацией о курсе валюты.
     */
    public ExchangeRateResponseViewModel toExchangeRateResponseViewModel(ExchangeRateSnapshotModel snapshot) {
        return new ExchangeRateResponseViewModel(
                snapshot.currency(),
                snapshot.buyRate(),
                snapshot.sellRate(),
                snapshot.updatedAt()
        );
    }

    // endregion
}