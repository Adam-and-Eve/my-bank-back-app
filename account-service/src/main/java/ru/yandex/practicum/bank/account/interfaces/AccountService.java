package ru.yandex.practicum.bank.account.interfaces;

import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.bank.account.exceptions.AccountNotFoundException;
import ru.yandex.practicum.bank.account.exceptions.InvalidBirthdateException;
import ru.yandex.practicum.bank.account.viewmodels.AccountResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.RecipientResponseViewModel;
import ru.yandex.practicum.bank.account.viewmodels.UpdateAccountRequestViewModel;

import java.util.List;

/**
 * <summary>
 * Контракт сервиса управления банковскими счетами пользователей (AccountServiceImpl).
 * </summary>
 **/
public interface AccountService {

    // region Methods

    /**
     * <summary>
     * Возвращает информацию о текущем счёте пользователя по его логину.
     * </summary>
     * @param login Логин пользователя.
     * @return ViewModel с данными счета {@link AccountResponseViewModel}.
     * @throws AccountNotFoundException Если счет с таким логином не найден.
     */
    public AccountResponseViewModel getCurrentAccount(String login);

    /**
     * <summary>
     * Обновляет профиль пользователя (имя и дату рождения) с проверкой достижения совершеннолетия.
     * </summary>
     * @param login Логин пользователя.
     * @param request ViewModel с новыми данными профиля.
     * @return ViewModel с данными счета {@link AccountResponseViewModel}.
     * @throws InvalidBirthdateException Если пользователь несовершеннолетний (< 18 лет).
     * @throws AccountNotFoundException Если счет с таким логином не найден.
     */
    public AccountResponseViewModel updateCurrentAccount(String login, UpdateAccountRequestViewModel request);

    /**
     * <summary>
     * Возвращает список всех доступных получателей перевода, исключая текущего пользователя.
     * </summary>
     * @param currentLogin Логин текущего пользователя.
     * @return Список ViewModel получателей {@link RecipientResponseViewModel}.
     */
    public List<RecipientResponseViewModel> getRecipients(String currentLogin);

    // endregion
}