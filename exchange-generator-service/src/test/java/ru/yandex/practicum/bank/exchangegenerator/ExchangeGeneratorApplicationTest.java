package ru.yandex.practicum.bank.exchangegenerator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.bank.exchangegenerator.interfaces.ExchangeClient;

@SpringBootTest
@ActiveProfiles("test")
public class ExchangeGeneratorApplicationTest {

    // region Fields

    @MockitoBean
    private ExchangeClient exchangeClient;

    // endregion

    // region Tests

    @Test
    void contextLoads() {
    }

    // endregion
}