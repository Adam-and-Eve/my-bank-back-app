package ru.yandex.practicum.bank.exchange.contract;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.bank.exchange.controllers.ExchangeController;
import ru.yandex.practicum.bank.exchange.exceptions.ExchangeExceptionHandler;
import ru.yandex.practicum.bank.exchange.mappers.ExchangeMapper;
import ru.yandex.practicum.bank.exchange.services.ExchangeServiceImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * <summary>
 * Базовый класс для контрактных тестов REST API сервиса обмена валют.
 * Настраивает MockMvc, контроллер, сервис обмена валют и обработчик исключений.
 * </summary>
 */
public class ExchangeContractBase {

    // region Fields

    /**
     * <summary>
     * Создает и настраивает ObjectMapper для сериализации и десериализации JSON.
     * Поддерживает работу с типами Java Time и сериализует даты в строковом формате.
     * </summary>
     * @return Настроенный ObjectMapper.
     */
    private JsonMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    // endregion

    // region Setup

    /**
     * <summary>
     * Настраивает MockMvc и RestAssured перед выполнением каждого контрактного теста.
     * </summary>
     */
    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(
                Instant.parse("2026-06-25T10:00:00Z"),
                ZoneOffset.UTC
        );

        var exchangeMapper = new ExchangeMapper();

        var exchangeService = new ExchangeServiceImpl(
                clock,
                exchangeMapper
        );

        var mockMvc = MockMvcBuilders
                .standaloneSetup(new ExchangeController(exchangeService))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper())
                )
                .build();

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    // endregion
}