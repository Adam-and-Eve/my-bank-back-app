package ru.yandex.practicum.bank.account.mappers;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.account.models.AccountModel;
import ru.yandex.practicum.bank.account.viewmodels.AccountResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.RecipientResponseViewModel;

/**
 * <summary>
 * Маппер для преобразования моделей счёта (AccountModel) в DTO/ViewModel представления.
 * </summary>
 **/
@Component
public class AccountMapper {

    /**
     * Преобразует модель счёта в полная представление информации об аккаунте.
     * @param account Модель счёта.
     * @return DTO с полной информацией об аккаунте.
     */
    public AccountResponseViewModel toResponse(AccountModel account) {
        return new AccountResponseViewModel(
                account.getLogin(),
                account.getName(),
                account.getBirthdate(),
                account.getBalance(),
                account.getCurrency().name()
        );
    }

    /**
     * Преобразует модель счёта в краткое представление информации о получателе.
     * @param account Модель счёта.
     * @return DTO с базовой информацией о получателе.
     */
    public RecipientResponseViewModel toRecipientResponse(AccountModel account) {
        return new RecipientResponseViewModel(
                account.getLogin(),
                account.getName()
        );
    }
}