package ru.yandex.practicum.bank.blocker.contract;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.bank.blocker.configurations.properties.BlockerProperties;
import ru.yandex.practicum.bank.blocker.controllers.BlockerController;
import ru.yandex.practicum.bank.blocker.exceptions.BlockerExceptionHandler;
import ru.yandex.practicum.bank.blocker.services.BlockerServiceImpl;

import java.math.BigDecimal;

/**
 * <summary>
 * Базовый класс для контрактных тестов сервиса блокировки подозрительных операций.
 * Настраивает REST-контроллер, сервис, обработчик исключений и JSON-конвертер
 * для выполнения контрактных тестов через RestAssuredMockMvc.
 * </summary>
 */
public class BlockerContractBase {

    // region Setup

    /**
     * <summary>
     * Настраивает MockMvc с контроллером сервиса блокировки,
     * реальным экземпляром сервиса и обработчиком исключений.
     * </summary>
     */
    @BeforeEach
    void setUp() {
        var blockerService = new BlockerServiceImpl(new BlockerProperties(new BigDecimal("100000.00")));

        var mockMvc = MockMvcBuilders.standaloneSetup(new BlockerController(blockerService))
                .setControllerAdvice(new BlockerExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(JsonMapper.builder().build()))
                .build();

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    // endregion
}