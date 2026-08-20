package ru.yandex.practicum.bank.frontui.helpers;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import ru.yandex.practicum.bank.frontui.interfaces.GatewayClient;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayClientException;
import ru.yandex.practicum.bank.frontui.viewmodels.*;
import ru.yandex.practicum.bank.shared.viewmodels.ExchangeRateResponseViewModel;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * <summary>
 * Вспомогательная фабрика для наполнения модели Spring MVC данными для UI-шаблона.
 * </summary>
 **/
@Component
public class HomeModelFactoryHelper {

    // region Fields

    private final GatewayClient gatewayClient;

    private final SecurityUserContextHelper securityUserContext;

    // endregion

    // region Constructors

    public HomeModelFactoryHelper(GatewayClient gatewayClient, SecurityUserContextHelper securityUserContext) {
        this.gatewayClient = gatewayClient;
        this.securityUserContext = securityUserContext;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Заполняет UI-модель данными пользователя, информацией о счете, списке получателей и формами по умолчанию.
     * </summary>
     **/
    public void populateMainPageModel(Model model, Principal principal, Authentication authentication) {
        String accessToken = securityUserContext.getAccessToken(authentication);

        model.addAttribute("username", securityUserContext.getUsername(principal));

        addAccountData(model, accessToken);

        addRecipientsData(model, accessToken);

        addExchangeRates(model, accessToken);

        addDefaultForms(model);
    }

    private void addAccountData(Model model, String accessToken) {
        try {
            var account = gatewayClient.getAccount(accessToken);

            if (!model.containsAttribute("accountForm")) {
                model.addAttribute("accountForm", new AccountFormViewModel(account.name(), account.birthdate()));
            }

            model.addAttribute("balance", account.balance());

            model.addAttribute("currency", account.currency());
        } catch (GatewayClientException exception) {
            if (!model.containsAttribute("accountForm")) {
                model.addAttribute("accountForm", new AccountFormViewModel("", null));
            }

            model.addAttribute("balance", "");

            model.addAttribute("currency", "RUB");

            model.addAttribute("accountLoadError", exception.getMessage());
        }
    }

    private void addRecipientsData(Model model, String accessToken) {
        try {
            var recipients = gatewayClient.getRecipients(accessToken);

            model.addAttribute("recipients", recipients);
        } catch (GatewayClientException exception) {
            model.addAttribute("recipients", List.of());

            model.addAttribute("recipientsLoadError", exception.getMessage());
        }
    }

    private void addExchangeRates(Model model, String accessToken) {
        try {
            var rates = gatewayClient.getExchangeRates(accessToken);

            model.addAttribute("exchangeRates", rates);
        } catch (GatewayClientException exception) {
            model.addAttribute("exchangeRates", List.of());

            model.addAttribute("exchangeRatesLoadError", exception.getMessage());
        }
    }

    private void addDefaultForms(Model model) {
        if (!model.containsAttribute("cashForm")) {
            model.addAttribute("cashForm", new CashFormViewModel(
                    new BigDecimal("100.00"),
                    "RUB",
                    UUID.randomUUID().toString()
            ));
        }

        if (!model.containsAttribute("transferForm")) {
            var accountCurrency = model.getAttribute("currency");

            var sourceCurrency = accountCurrency == null ? "RUB" : accountCurrency.toString();

            model.addAttribute("transferForm", new TransferFormViewModel(
                    "",
                    new BigDecimal("100.00"),
                    "RUB",
                    sourceCurrency,
                    UUID.randomUUID().toString()
            ));
        }
    }
}