package ru.yandex.practicum.bank.transfer.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferRequestViewModel;
import ru.yandex.practicum.bank.transfer.viewmodels.TransferResponseViewModel;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;
import ru.yandex.practicum.bank.transfer.controllers.TransferController;
import ru.yandex.practicum.bank.transfer.exceptions.TransferExceptionHandler;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class TransferContractBase {

    @BeforeEach
    void setUp() {
        var transferService = mock(TransferService.class);
        when(transferService.transfer(eq("dmitry"), any(TransferRequestViewModel.class)))
                .thenReturn(new TransferResponseViewModel(
                        "dmitry",
                        "alexey",
                        new BigDecimal("850.00"),
                        "RUB",
                        "Transfer completed"
                ));

        var mockMvc = MockMvcBuilders.standaloneSetup(new TransferController(transferService))
                .setControllerAdvice(new TransferExceptionHandler())
                .defaultRequest(get("/").principal(jwtAuthentication("dmitry")))
                .build();

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    private JwtAuthenticationToken jwtAuthentication(String login) {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-06-13T00:00:00Z"))
                .claim("preferred_username", login)
                .build();

        return new JwtAuthenticationToken(jwt);
    }
}