package ru.yandex.practicum.bank.blocker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootApplication
public class BlockerServiceApplicationTest {

    // region Fields

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    @Test
    void contextLoads() {
    }

    // endregion
}