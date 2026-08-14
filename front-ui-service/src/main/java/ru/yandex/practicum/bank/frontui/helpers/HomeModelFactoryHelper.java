package ru.yandex.practicum.bank.frontui.helpers;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import ru.yandex.practicum.bank.frontui.interfaces.GatewayClient;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayClientException;
import ru.yandex.practicum.bank.frontui.viewmodels.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

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

        addDefaultForms(model);
    }

    private void addAccountData(Model model, String accessToken) {
        try {
            AccountResponseViewModel account = gatewayClient.getAccount(accessToken);

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
            List<RecipientResponseViewModel> recipients = gatewayClient.getRecipients(accessToken);

            model.addAttribute("recipients", recipients);
        } catch (GatewayClientException exception) {
            model.addAttribute("recipients", List.of());

            model.addAttribute("recipientsLoadError", exception.getMessage());
        }
    }

    private void addDefaultForms(Model model) {
        if (!model.containsAttribute("cashForm")) {
            model.addAttribute("cashForm", new CashFormViewModel(new BigDecimal("100.00"), "RUB"));
        }
        if (!model.containsAttribute("transferForm")) {
            model.addAttribute("transferForm", new TransferFormViewModel("", new BigDecimal("100.00"), "RUB"));
        }
    }
}