package ru.yandex.practicum.bank.frontui.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayClientException;
import ru.yandex.practicum.bank.frontui.helpers.HomeModelFactoryHelper;
import ru.yandex.practicum.bank.frontui.helpers.SecurityUserContextHelper;
import ru.yandex.practicum.bank.frontui.interfaces.GatewayClient;
import ru.yandex.practicum.bank.frontui.viewmodels.AccountFormViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.CashFormViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.CashOperationResponseViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.TransferFormViewModel;
import ru.yandex.practicum.bank.frontui.viewmodels.TransferResponseViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * <summary>
 * Тесты HomeController.
 * Проверяют отображение главной страницы, обновление данных аккаунта,
 * операции с наличными средствами и переводы между счетами.
 * </summary>
 **/
@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class HomeControllerTest {

    // region Constants

    private static final String ACCESS_TOKEN = "test-access-token";

    private static final String USERNAME = "dmitry";

    // endregion

    // region Fields

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GatewayClient gatewayClient;

    @MockitoBean
    private SecurityUserContextHelper securityUserContext;

    @MockitoBean
    private HomeModelFactoryHelper modelFactory;

    // endregion

    // region Setup

    @BeforeEach
    void setUp() {
        when(securityUserContext.getAccessToken(any())).thenReturn(ACCESS_TOKEN);

        when(gatewayClient.getAccount(any())).thenReturn(mock());

        when(gatewayClient.getRecipients(any())).thenReturn(Collections.emptyList());

        when(gatewayClient.getExchangeRates(any())).thenReturn(Collections.emptyList());

        doAnswer((Answer<Void>) invocation -> {
            Model model = invocation.getArgument(0);

            model.addAttribute("username", USERNAME);
            model.addAttribute("balance", "1000.00");
            model.addAttribute("currency", "RUB");
            model.addAttribute("recipients", Collections.emptyList());

            if (!model.containsAttribute("accountForm")) {
                model.addAttribute("accountForm", new AccountFormViewModel("Dmitry", LocalDate.of(1995, 5, 15)));
            }
            if (!model.containsAttribute("cashForm")) {
                model.addAttribute("cashForm", new CashFormViewModel(new BigDecimal("100.00"), "RUB"));
            }
            if (!model.containsAttribute("transferForm")) {
                model.addAttribute("transferForm", new TransferFormViewModel("", new BigDecimal("100.00"), "RUB"));
            }

            model.addAttribute("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken(
                    "X-CSRF-TOKEN", "_csrf", "test-csrf-token"
            ));

            return null;
        }).when(modelFactory).populateMainPageModel(any(Model.class), any(), any());
    }

    // endregion

    // region Tests - Main page

    /**
     * <summary>
     * Проверяет отображение главной страницы.
     * </summary>
     **/
    @Test
    public void shouldShowMainPage() throws Exception {
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn(USERNAME);

        mockMvc.perform(get("/").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        verify(modelFactory).populateMainPageModel(any(), eq(authentication), eq(authentication));
    }

    // endregion

    // region Tests - Account

    /**
     * <summary>
     * Проверяет успешное обновление данных аккаунта.
     * </summary>
     **/
    @Test
    public void shouldUpdateAccountSuccessfully() throws Exception {
        Authentication authentication = mock(Authentication.class);

        mockMvc.perform(post("/account")
                        .with(csrf())
                        .principal(authentication)
                        .param("name", "Dmitry")
                        .param("birthdate", "1995-05-15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("successMessage", "Данные аккаунта сохранены"));

        verify(securityUserContext).getAccessToken(authentication);

        verify(gatewayClient).updateAccount(eq(ACCESS_TOKEN), any());
    }

    /**
     * <summary>
     * Проверяет обработку ошибки GatewayClient при обновлении аккаунта.
     * </summary>
     **/
    @Test
    public void shouldHandleGatewayErrorWhenUpdatingAccount() throws Exception {
        Authentication authentication = mock(Authentication.class);

        doThrow(new GatewayClientException("Ошибка обновления аккаунта"))
                .when(gatewayClient).updateAccount(eq(ACCESS_TOKEN), any());

        mockMvc.perform(post("/account")
                        .with(csrf())
                        .principal(authentication)
                        .param("name", "Dmitry")
                        .param("birthdate", "1995-05-15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", "Ошибка обновления аккаунта"));

        verify(gatewayClient).updateAccount(eq(ACCESS_TOKEN), any());

        verify(modelFactory, never()).populateMainPageModel(any(), any(), any());
    }

    /**
     * <summary>
     * Проверяет обработку ошибки валидации данных аккаунта.
     * </summary>
     **/
    @Test
    public void shouldReturnMainPageWhenAccountValidationFails() throws Exception {
        Authentication authentication = mock(Authentication.class);

        mockMvc.perform(post("/account")
                        .with(csrf())
                        .principal(authentication)
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("errorMessage", "Заполните имя и дату рождения"));

        verify(modelFactory).populateMainPageModel(any(), eq(authentication), eq(authentication));

        verify(gatewayClient, never()).updateAccount(any(), any());

        verify(securityUserContext, never()).getAccessToken(any());
    }

    // endregion

    // region Tests - Cash operations

    /**
     * <summary>
     * Проверяет успешное пополнение счёта.
     * </summary>
     **/
    @Test
    public void shouldDepositCashSuccessfully() throws Exception {
        Authentication authentication = mock(Authentication.class);

        CashOperationResponseViewModel response = mock(CashOperationResponseViewModel.class);

        when(response.message()).thenReturn("Счёт пополнен");

        when(gatewayClient.deposit(eq(ACCESS_TOKEN), any())).thenReturn(response);

        mockMvc.perform(post("/cash")
                .with(csrf())
                .principal(authentication)
                .param("amount", "100.00")
                .param("currency", "RUB")
                .param("action", "deposit")
                .param("idempotencyKey", "test-deposit-123"));

        verify(securityUserContext).getAccessToken(authentication);

        verify(gatewayClient).deposit(eq(ACCESS_TOKEN), any());

        verify(gatewayClient, never()).withdraw(any(), any());
    }

    /**
     * <summary>
     * Проверяет успешное снятие средств.
     * </summary>
     **/
    @Test
    public void shouldWithdrawCashSuccessfully() throws Exception {
        Authentication authentication = mock(Authentication.class);

        CashOperationResponseViewModel response =
                mock(CashOperationResponseViewModel.class);

        when(securityUserContext.getAccessToken(authentication))
                .thenReturn(ACCESS_TOKEN);

        when(response.message())
                .thenReturn("Средства сняты");

        when(gatewayClient.withdraw(eq(ACCESS_TOKEN), any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/cash")
                                .with(csrf())
                                .principal(authentication)
                                .param("amount", "100.00")
                                .param("currency", "RUB")
                                .param("action", "withdraw")
                                .param("idempotencyKey", "test-idempotency-key")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "Средства сняты"
                ));

        verify(securityUserContext).getAccessToken(authentication);

        verify(gatewayClient).withdraw(
                eq(ACCESS_TOKEN),
                any()
        );

        verify(gatewayClient, never()).deposit(
                any(),
                any()
        );
    }

    /**
     * <summary>
     * Проверяет обработку неизвестного действия операции с наличными.
     * </summary>
     **/
    @Test
    public void shouldHandleUnknownCashAction() throws Exception {
        Authentication authentication = mock(Authentication.class);

        when(securityUserContext.getAccessToken(authentication))
                .thenReturn(ACCESS_TOKEN);

        mockMvc.perform(
                        post("/cash")
                                .with(csrf())
                                .principal(authentication)
                                .param("amount", "100.00")
                                .param("currency", "RUB")
                                .param("action", "unknown")
                                .param("idempotencyKey", "test-idempotency-key")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "Unknown cash action: unknown"
                ));

        verify(gatewayClient, never()).deposit(
                any(),
                any()
        );

        verify(gatewayClient, never()).withdraw(
                any(),
                any()
        );
    }

    /**
     * <summary>
     * Проверяет обработку ошибки GatewayClient при операции с наличными.
     * </summary>
     **/
    @Test
    public void shouldHandleGatewayErrorWhenCashOperationFails() throws Exception {
        Authentication authentication = mock(Authentication.class);

        when(securityUserContext.getAccessToken(authentication))
                .thenReturn(ACCESS_TOKEN);

        doThrow(new GatewayClientException("Недостаточно средств"))
                .when(gatewayClient)
                .withdraw(eq(ACCESS_TOKEN), any());

        mockMvc.perform(
                        post("/cash")
                                .with(csrf())
                                .principal(authentication)
                                .param("amount", "100.00")
                                .param("currency", "RUB")
                                .param("action", "withdraw")
                                .param("idempotencyKey", "test-idempotency-key")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "Недостаточно средств"
                ));
    }

    /**
     * <summary>
     * Проверяет обработку ошибки валидации операции с наличными.
     * </summary>
     **/
    @Test
    public void shouldReturnMainPageWhenCashValidationFails() throws Exception {
        Authentication authentication = mock(Authentication.class);

        mockMvc.perform(
                        post("/cash")
                                .with(csrf())
                                .principal(authentication)
                                .param("amount", "-100.00")
                                .param("currency", "RUB")
                                .param("action", "deposit")
                                .param("idempotencyKey", "test-idempotency-key")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "Заполните положительную сумму"
                ));

        verify(modelFactory).populateMainPageModel(
                any(),
                eq(authentication),
                eq(authentication)
        );

        verify(gatewayClient, never()).deposit(
                any(),
                any()
        );

        verify(gatewayClient, never()).withdraw(
                any(),
                any()
        );
    }

    // endregion

    // region Tests - Transfers

    /**
     * <summary>
     * Проверяет успешное выполнение перевода.
     * </summary>
     **/
    @Test
    public void shouldTransferSuccessfully() throws Exception {
        Authentication authentication = mock(Authentication.class);

        TransferResponseViewModel response =
                mock(TransferResponseViewModel.class);

        when(securityUserContext.getAccessToken(authentication))
                .thenReturn(ACCESS_TOKEN);

        when(gatewayClient.transfer(eq(ACCESS_TOKEN), any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/transfers")
                                .with(csrf())
                                .principal(authentication)
                                .param("recipientLogin", "alex")
                                .param("amount", "100.00")
                                .param("currency", "RUB")
                                .param("idempotencyKey", "test-idempotency-key")
                                .param("sourceCurrency", "RUB")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "Перевод выполнен"
                ))
                .andExpect(flash().attribute(
                        "transferResponse",
                        response
                ));

        verify(securityUserContext).getAccessToken(authentication);

        verify(gatewayClient).transfer(
                eq(ACCESS_TOKEN),
                any()
        );
    }

    /**
     * <summary>
     * Проверяет обработку ошибки GatewayClient при выполнении перевода.
     * </summary>
     **/
    @Test
    public void shouldHandleGatewayErrorWhenTransferFails() throws Exception {
        Authentication authentication = mock(Authentication.class);

        when(securityUserContext.getAccessToken(authentication))
                .thenReturn(ACCESS_TOKEN);

        doThrow(new GatewayClientException("Недостаточно средств"))
                .when(gatewayClient)
                .transfer(eq(ACCESS_TOKEN), any());

        mockMvc.perform(
                        post("/transfers")
                                .with(csrf())
                                .principal(authentication)
                                .param("recipientLogin", "alex")
                                .param("amount", "100.00")
                                .param("currency", "RUB")
                                .param("idempotencyKey", "test-idempotency-key")
                                .param("sourceCurrency", "RUB")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "Недостаточно средств"
                ));

        verify(gatewayClient).transfer(
                eq(ACCESS_TOKEN),
                any()
        );
    }

    /**
     * <summary>
     * Проверяет обработку ошибки валидации перевода.
     * </summary>
     **/
    @Test
    public void shouldReturnMainPageWhenTransferValidationFails() throws Exception {
        Authentication authentication = mock(Authentication.class);

        mockMvc.perform(
                        post("/transfers")
                                .with(csrf())
                                .principal(authentication)
                                .param("recipientLogin", "")
                                .param("currency", "")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "Заполните получателя, сумму и валюту"
                ));

        verify(modelFactory).populateMainPageModel(
                any(),
                eq(authentication),
                eq(authentication)
        );

        verify(gatewayClient, never()).transfer(
                any(),
                any()
        );

        verify(securityUserContext, never()).getAccessToken(any());
    }

    // endregion
}