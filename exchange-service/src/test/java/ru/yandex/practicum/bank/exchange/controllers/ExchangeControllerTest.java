package ru.yandex.practicum.bank.exchange.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.exchange.interfaces.ExchangeService;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateUpdateRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRatesUpdateRequestViewModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Тесты REST-контроллера операций обмена валют.
 * Проверяет обработку HTTP-запросов на получение и обновление курсов валют,
 * а также выполнение конвертации валют.
 * </summary>
 */
@WebMvcTest(ExchangeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ExchangeControllerTest {

    // region Fields

    /**
     * <summary>
     * MockMvc для выполнения HTTP-запросов к контроллеру.
     * </summary>
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * <summary>
     * ObjectMapper для сериализации моделей запросов в JSON.
     * </summary>
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * <summary>
     * Mock сервиса обмена валют.
     * </summary>
     */
    @MockitoBean
    private ExchangeService exchangeService;

    // endregion

    // region Setup

    /**
     * <summary>
     * Подготавливает mock-сервис перед выполнением каждого теста.
     * </summary>
     */
    @BeforeEach
    void setUp() {
        // Mock-сервис настраивается непосредственно в тестах.
    }

    // endregion

    // region getRates

    /**
     * <summary>
     * Проверяет успешное получение текущих курсов валют.
     * </summary>
     */
    @Test
    void getRatesShouldReturnRates() throws Exception {
        var updatedAt = Instant.parse("2026-08-19T10:00:00Z");

        var rates = List.of(
                new ExchangeRateResponseViewModel(
                        CurrencyEnumModel.RUB,
                        new BigDecimal("1.0000"),
                        new BigDecimal("1.0000"),
                        updatedAt
                ),
                new ExchangeRateResponseViewModel(
                        CurrencyEnumModel.USD,
                        new BigDecimal("90.0000"),
                        new BigDecimal("92.0000"),
                        updatedAt
                ),
                new ExchangeRateResponseViewModel(
                        CurrencyEnumModel.CNY,
                        new BigDecimal("12.4000"),
                        new BigDecimal("12.8000"),
                        updatedAt
                )
        );

        when(exchangeService.getRates()).thenReturn(rates);

        mockMvc.perform(get("/api/exchange/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].currency").value("RUB"))
                .andExpect(jsonPath("$[0].buyRate").value("1.0000"))
                .andExpect(jsonPath("$[0].sellRate").value("1.0000"))
                .andExpect(jsonPath("$[1].currency").value("USD"))
                .andExpect(jsonPath("$[1].buyRate").value("90.0000"))
                .andExpect(jsonPath("$[1].sellRate").value("92.0000"))
                .andExpect(jsonPath("$[2].currency").value("CNY"))
                .andExpect(jsonPath("$[2].buyRate").value("12.4000"))
                .andExpect(jsonPath("$[2].sellRate").value("12.8000"))
                .andExpect(jsonPath("$[0].updatedAt").value("2026-08-19T10:00:00Z"));

        verify(exchangeService).getRates();
    }

    // endregion

    // region updateRates

    /**
     * <summary>
     * Проверяет успешное обновление курса валюты через REST-запрос.
     * </summary>
     */
    @Test
    void updateRatesShouldReturnUpdatedRates() throws Exception {
        var updatedAt = Instant.parse("2026-08-19T11:00:00Z");

        var request = new ExchangeRatesUpdateRequestViewModel(
                List.of(
                        new ExchangeRateUpdateRequestViewModel(
                                CurrencyEnumModel.USD,
                                new BigDecimal("91.0000"),
                                new BigDecimal("93.0000")
                        )
                )
        );

        var response = List.of(
                new ExchangeRateResponseViewModel(
                        CurrencyEnumModel.RUB,
                        new BigDecimal("1.0000"),
                        new BigDecimal("1.0000"),
                        updatedAt
                ),
                new ExchangeRateResponseViewModel(
                        CurrencyEnumModel.USD,
                        new BigDecimal("91.0000"),
                        new BigDecimal("93.0000"),
                        updatedAt
                ),
                new ExchangeRateResponseViewModel(
                        CurrencyEnumModel.CNY,
                        new BigDecimal("12.4000"),
                        new BigDecimal("12.8000"),
                        updatedAt
                )
        );

        when(exchangeService.updateRates(any()))
                .thenReturn(response);

        mockMvc.perform(
                        put("/api/exchange/rates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].currency").value("USD"))
                .andExpect(jsonPath("$[1].buyRate").value("91.0000"))
                .andExpect(jsonPath("$[1].sellRate").value("93.0000"));

        verify(exchangeService).updateRates(any());
    }

    /**
     * <summary>
     * Проверяет, что запрос на обновление курсов без списка валют
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void updateRatesShouldRejectEmptyRates() throws Exception {
        var request = new ExchangeRatesUpdateRequestViewModel(
                List.of()
        );

        mockMvc.perform(
                        put("/api/exchange/rates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос на обновление курса с отсутствующей валютой
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void updateRatesShouldRejectNullCurrency() throws Exception {
        var json = """
                {
                    "rates": [
                        {
                            "currency": null,
                            "buyRate": 90.0000,
                            "sellRate": 92.0000
                        }
                    ]
                }
                """;

        mockMvc.perform(
                        put("/api/exchange/rates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос на обновление курса с отсутствующим курсом покупки
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void updateRatesShouldRejectNullBuyRate() throws Exception {
        var json = """
                {
                    "rates": [
                        {
                            "currency": "USD",
                            "buyRate": null,
                            "sellRate": 92.0000
                        }
                    ]
                }
                """;

        mockMvc.perform(
                        put("/api/exchange/rates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос на обновление курса с отсутствующим курсом продажи
     * завершается ошибкой валидации.
     * </summary>
     */
    @Test
    void updateRatesShouldRejectNullSellRate() throws Exception {
        var json = """
                {
                    "rates": [
                        {
                            "currency": "USD",
                            "buyRate": 90.0000,
                            "sellRate": null
                        }
                    ]
                }
                """;

        mockMvc.perform(
                        put("/api/exchange/rates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }

    // endregion

    // region convert

    /**
     * <summary>
     * Проверяет успешное выполнение конвертации валют через REST-запрос.
     * </summary>
     */
    @Test
    void convertShouldReturnConversionResult() throws Exception {
        var updatedAt = Instant.parse("2026-08-19T10:00:00Z");

        var response = new ConversionResponseViewModel(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                new BigDecimal("100.00"),
                new BigDecimal("9200.00"),
                new BigDecimal("92.000000"),
                updatedAt
        );

        when(exchangeService.convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                new BigDecimal("100.00")
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/exchange/conversion")
                                .param("sourceCurrency", "USD")
                                .param("targetCurrency", "RUB")
                                .param("amount", "100.00")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceCurrency").value("USD"))
                .andExpect(jsonPath("$.targetCurrency").value("RUB"))
                .andExpect(jsonPath("$.sourceAmount").value("100.00"))
                .andExpect(jsonPath("$.targetAmount").value("9200.00"))
                .andExpect(jsonPath("$.rate").value("92.000000"))
                .andExpect(jsonPath("$.updatedAt").value("2026-08-19T10:00:00Z"));

        verify(exchangeService).convert(
                CurrencyEnumModel.USD,
                CurrencyEnumModel.RUB,
                new BigDecimal("100.00")
        );
    }

    /**
     * <summary>
     * Проверяет, что запрос на конвертацию без исходной валюты
     * завершается ошибкой.
     * </summary>
     */
    @Test
    void convertShouldRejectMissingSourceCurrency() throws Exception {
        mockMvc.perform(
                        get("/api/exchange/conversion")
                                .param("targetCurrency", "RUB")
                                .param("amount", "100.00")
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос на конвертацию без целевой валюты
     * завершается ошибкой.
     * </summary>
     */
     @Test
     void convertShouldRejectMissingTargetCurrency() throws Exception {
     mockMvc.perform(
     get("/api/exchange/conversion")
     .param("sourceCurrency", "USD")
     .param("amount", "100.00")
     )
     .andExpect(status().isBadRequest());
     }

     /**
      * <summary>
      * Проверяет, что запрос на конвертацию без суммы
      * завершается ошибкой.
      * </summary>
     */
    @Test
    void convertShouldRejectMissingAmount() throws Exception {
        mockMvc.perform(
                        get("/api/exchange/conversion")
                                .param("sourceCurrency", "USD")
                                .param("targetCurrency", "RUB")
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос на конвертацию с некорректным значением валюты
     * завершается ошибкой.
     * </summary>
     */
    @Test
    void convertShouldRejectInvalidCurrency() throws Exception {
        mockMvc.perform(
                        get("/api/exchange/conversion")
                                .param("sourceCurrency", "EUR")
                                .param("targetCurrency", "RUB")
                                .param("amount", "100.00")
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * <summary>
     * Проверяет, что запрос на конвертацию с некорректным числовым значением суммы
     * завершается ошибкой.
     * </summary>
     */
    @Test
    void convertShouldRejectInvalidAmountFormat() throws Exception {
        mockMvc.perform(
                        get("/api/exchange/conversion")
                                .param("sourceCurrency", "USD")
                                .param("targetCurrency", "RUB")
                                .param("amount", "invalid")
                )
                .andExpect(status().isBadRequest());
    }

    // endregion
}