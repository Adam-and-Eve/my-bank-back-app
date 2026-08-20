package ru.yandex.practicum.bank.exchangegenerator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.bank.exchangegenerator.interfaces.InternalExchangeClient;

@SpringBootTest
@ActiveProfiles("test")
public class ExchangeGeneratorApplicationTest {

    // region Fields

    @MockitoBean
    private InternalExchangeClient internalExchangeClient;

    // endregion

    // region Tests

    @Test
    void contextLoads() {
    }

    // endregion
}