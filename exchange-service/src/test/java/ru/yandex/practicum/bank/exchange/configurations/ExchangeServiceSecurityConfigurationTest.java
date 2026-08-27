package ru.yandex.practicum.bank.exchange.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.bank.exchange.interfaces.ExchangeService;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.ConversionResponseViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <summary>
 * Тесты конфигурации безопасности ExchangeServiceSecurityConfiguration.
 * Проверяют маршрутизацию, правила RBAC-авторизации, а также кастомный конвертер ролей из JWT.
 * </summary>
 **/
@SpringBootTest
@AutoConfigureMockMvc
public class ExchangeServiceSecurityConfigurationTest {

    // region Constants

    private static final Instant UPDATED_AT = Instant.parse("2026-08-27T10:00:00Z");

    // endregion

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    @MockitoBean
    private ExchangeService exchangeService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет доступность эндпоинта получения курсов без JWT-токена.
     * </summary>
     **/
    @Test
    void shouldAllowRatesWithoutJwt() throws Exception {
        when(exchangeService.getRates())
                .thenReturn(List.of(rate(CurrencyEnumModel.USD, "90.0000", "92.0000")));

        mockMvc.perform(get("/api/exchange/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    /**
     * <summary>
     * Проверяет доступность эндпоинта конвертации валют без JWT-токена.
     * </summary>
     **/
    @Test
    void shouldAllowConversionWithoutJwt() throws Exception {
        when(exchangeService.convert(CurrencyEnumModel.USD, CurrencyEnumModel.CNY, new BigDecimal("100.00")))
                .thenReturn(new ConversionResponseViewModel(
                        CurrencyEnumModel.USD,
                        CurrencyEnumModel.CNY,
                        new BigDecimal("100.00"),
                        new BigDecimal("741.94"),
                        new BigDecimal("7.419355"),
                        UPDATED_AT
                ));

        mockMvc.perform(get("/api/exchange/conversion")
                        .param("sourceCurrency", "USD")
                        .param("targetCurrency", "CNY")
                        .param("amount", "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCurrency").value("CNY"));
    }

    /**
     * <summary>
     * Проверяет возврат ошибки 401 при попытке обновить курсы без токена.
     * </summary>
     **/
    @Test
    void shouldRejectRateUpdateWithoutJwt() throws Exception {
        mockMvc.perform(put("/api/exchange/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Требуется авторизация"));
    }

    /**
     * <summary>
     * Проверяет возврат ошибки 403 при наличии только роли пользователя.
     * </summary>
     **/
    @Test
    void shouldRejectRateUpdateWithoutServiceRole() throws Exception {
        mockMvc.perform(put("/api/exchange/rates")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Недостаточно прав для выполнения операции"));
    }

    /**
     * <summary>
     * Проверяет возврат ошибки 403 при отсутствии роли генератора курсов.
     * </summary>
     **/
    @Test
    void shouldRejectRateUpdateWithoutExchangeGeneratorRole() throws Exception {
        mockMvc.perform(put("/api/exchange/rates")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    /**
     * <summary>
     * Проверяет успешное обновление курсов при наличии обеих необходимых ролей.
     * </summary>
     **/
    @Test
    void shouldAllowRateUpdateForExchangeGeneratorService() throws Exception {
        when(exchangeService.updateRates(any()))
                .thenReturn(List.of(rate(CurrencyEnumModel.USD, "91.0000", "93.0000")));

        mockMvc.perform(put("/api/exchange/rates")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("ROLE_EXCHANGE_GENERATOR")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    /**
     * <summary>
     * Проверяет корректную конвертацию Keycloak-ролей в права Spring Security.
     * </summary>
     **/
    @Test
    void shouldConvertRealmRolesToAuthorities() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-08-27T00:00:00Z"))
                .claim("realm_access", Map.of("roles", List.of("SERVICE", "EXCHANGE_GENERATOR")))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_SERVICE", "ROLE_EXCHANGE_GENERATOR");
    }

    // endregion

    // region Private Methods

    private ExchangeRateResponseViewModel rate(CurrencyEnumModel currency, String buyRate, String sellRate) {
        return new ExchangeRateResponseViewModel(
                currency,
                new BigDecimal(buyRate),
                new BigDecimal(sellRate),
                UPDATED_AT
        );
    }

    private String updateRequest() {
        return """
                {
                  "rates": [
                    {
                      "currency": "USD",
                      "buyRate": "91.0000",
                      "sellRate": "93.0000"
                    }
                  ]
                }
                """;
    }

    // endregion
}