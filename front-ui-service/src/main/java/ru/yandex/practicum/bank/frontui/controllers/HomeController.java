package ru.yandex.practicum.bank.frontui.controllers;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.bank.frontui.interfaces.GatewayClient;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayClientException;
import ru.yandex.practicum.bank.frontui.helpers.HomeModelFactoryHelper;
import ru.yandex.practicum.bank.frontui.helpers.SecurityUserContextHelper;
import ru.yandex.practicum.bank.frontui.viewmodels.*;

import java.security.Principal;

/**
 * <summary>
 * Контроллер главной страницы пользовательского интерфейса (HomeController).
 * Обрабатывает отображение главной страницы, обновление данных аккаунта,
 * операции пополнения и снятия денежных средств, а также переводы между счетами.
 * Выполняет валидацию входных данных, взаимодействует с GatewayClient
 * и передаёт результаты операций в модель или flash-атрибуты для отображения пользователю.
 * </summary>
 **/
@Controller
public class HomeController {

    // region Fields

    private final GatewayClient gatewayClient;

    private final SecurityUserContextHelper securityUserContext;

    private final HomeModelFactoryHelper modelFactory;

    // endregion

    // region Constructors

    public HomeController(
            GatewayClient gatewayClient,
            SecurityUserContextHelper securityUserContext,
            HomeModelFactoryHelper modelFactory
    ) {
        this.gatewayClient = gatewayClient;
        this.securityUserContext = securityUserContext;
        this.modelFactory = modelFactory;
    }

    // endregion

    // region Actions

    /**
     * <summary>
     * Отображает главную страницу пользовательского интерфейса.
     * Заполняет модель данными текущего пользователя и его аккаунта.
     * </summary>
     **/
    @GetMapping("/")
    public String showMainPage(Model model, Principal principal, Authentication authentication) {
        modelFactory.populateMainPageModel(model, principal, authentication);

        return "index";
    }

    /**
     * <summary>
     * Обновляет данные текущего аккаунта.
     * При наличии ошибок валидации повторно отображает главную страницу
     * с сообщением об ошибке. При успешном обновлении или ошибке GatewayClient
     * передаёт соответствующее сообщение через flash-атрибуты и выполняет редирект
     * на главную страницу.
     * </summary>
     **/
    @PostMapping("/account")
    public String updateAccount(
            @Valid @ModelAttribute AccountFormViewModel accountForm,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            modelFactory.populateMainPageModel(model, principal, authentication);

            model.addAttribute("errorMessage", "Заполните имя и дату рождения");

            return "index";
        }

        try {
            var accessToken = securityUserContext.getAccessToken(authentication);

            gatewayClient.updateAccount(accessToken, accountForm);

            redirectAttributes.addFlashAttribute("successMessage", "Данные аккаунта сохранены");
        } catch (GatewayClientException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/";
    }

    /**
     * <summary>
     * Выполняет операцию с наличными средствами.
     * В зависимости от параметра action выполняет пополнение или снятие средств.
     * При ошибке валидации повторно отображает главную страницу,
     * а результат операции или сообщение об ошибке передаёт через flash-атрибуты.
     * </summary>
     **/
    @PostMapping("/cash")
    public String cashOperation(
            @Valid @ModelAttribute CashFormViewModel cashForm,
            BindingResult bindingResult,
            @RequestParam String action,
            Model model,
            Principal principal,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            modelFactory.populateMainPageModel(model, principal, authentication);

            model.addAttribute("errorMessage", "Заполните положительную сумму");

            return "index";
        }

        try {
            var accessToken = securityUserContext.getAccessToken(authentication);

            CashOperationResponseViewModel response = switch (action) {
                case "deposit" -> gatewayClient.deposit(accessToken, cashForm);
                case "withdraw" -> gatewayClient.withdraw(accessToken, cashForm);
                default -> throw new GatewayClientException("Unknown cash action: " + action);
            };

            redirectAttributes.addFlashAttribute("successMessage", response.message());
        } catch (GatewayClientException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/";
    }

    /**
     * <summary>
     * Выполняет перевод денежных средств на счёт другого пользователя.
     * При ошибке валидации повторно отображает главную страницу,
     * а при успешном переводе сохраняет сообщение и результат операции
     * в flash-атрибутах и выполняет редирект на главную страницу.
     * </summary>
     **/
    @PostMapping("/transfers")
    public String transfer(
            @Valid @ModelAttribute TransferFormViewModel transferForm,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            modelFactory.populateMainPageModel(model, principal, authentication);

            model.addAttribute("errorMessage", "Заполните получателя, сумму и валюту");

            return "index";
        }

        try {
            var accessToken = securityUserContext.getAccessToken(authentication);

            var response = gatewayClient.transfer(accessToken, transferForm);

            redirectAttributes.addFlashAttribute("successMessage", "Перевод выполнен");

            redirectAttributes.addFlashAttribute("transferResponse", response);
        } catch (GatewayClientException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/";
    }

    // endregion
}