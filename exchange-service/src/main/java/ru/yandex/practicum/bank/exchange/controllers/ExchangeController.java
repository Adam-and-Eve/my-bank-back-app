package ru.yandex.practicum.bank.exchange.controllers;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.bank.exchange.interfaces.ExchangeService;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

import java.math.BigDecimal;
import java.util.List;

/**
 * <summary>
 * REST-контроллер для получения и обновления курсов валют,
 * а также выполнения операций конвертации валют.
 * </summary>
 */
@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {

    // region Fields

    /**
     * <summary>
     * Сервис для выполнения операций с курсами валют и конвертации.
     * </summary>
     */
    private final ExchangeService exchangeService;

    // endregion

    // region Constructors

    public ExchangeController(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
    }

    // endregion

    // region Actions

    // region Actions

    /**
     * <summary>
     * Возвращает текущие курсы всех поддерживаемых валют.
     * </summary>
     * @return Список текущих курсов обмена валют.
     */
    @GetMapping("/rates")
    public List<ExchangeRateResponseViewModel> getRates() {
        return exchangeService.getRates();
    }

    /**
     * <summary>
     * Обновляет курсы указанных валют и возвращает актуальные курсы всех поддерживаемых валют.
     * </summary>
     * @param request Запрос с данными для обновления курсов валют.
     * @return Список актуальных курсов обмена валют.
     */
    @PutMapping("/rates")
    public List<ExchangeRateResponseViewModel> updateRates(
            @Valid @RequestBody ExchangeRatesUpdateRequestViewModel request) {

        return exchangeService.updateRates(request);
    }

    /**
     * <summary>
     * Выполняет конвертацию указанной суммы из одной валюты в другую
     * по текущим курсам обмена.
     * </summary>
     * @param sourceCurrency Исходная валюта.
     * @param targetCurrency Целевая валюта.
     * @param amount Сумма для конвертации.
     * @return Результат конвертации с информацией о примененном курсе.
     */
    @GetMapping("/conversion")
    public ConversionResponseViewModel convert(
            @RequestParam CurrencyEnumModel sourceCurrency,
            @RequestParam CurrencyEnumModel targetCurrency,
            @RequestParam BigDecimal amount) {

        return exchangeService.convert(
                sourceCurrency,
                targetCurrency,
                amount
        );
    }

    // endregion
}