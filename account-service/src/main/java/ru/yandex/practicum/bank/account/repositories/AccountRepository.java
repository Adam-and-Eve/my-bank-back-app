package ru.yandex.practicum.bank.account.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.bank.account.models.AccountModel;

import java.util.List;
import java.util.Optional;

/**
 * <summary>
 * Репозиторий Spring Data JPA для работы с сущностями банковских счетов (AccountModel).
 * </summary>
 **/
@Repository
public interface AccountRepository extends JpaRepository<AccountModel, Long> {

    // region Methods

    /**
     * <summary>
     * Выполняет поиск счёта по уникальному логину пользователя.
     * </summary>
     * @param login Логин пользователя.
     * @return {@link Optional} с найденным счётом или пустой, если счёт не найден.
     */
    public Optional<AccountModel> findByLogin(String login);

    /**
     * <summary>
     * Возвращает список всех счетов, за исключением счёта с указанным логином.
     * Используется для получения списка возможных получателей перевода.
     * </summary>
     * @param login Логин текущего пользователя, который исключается из выборки.
     * @return Список счетов остальных пользователей.
     */
    public List<AccountModel> findAllByLoginNot(String login);

    // endregion
}