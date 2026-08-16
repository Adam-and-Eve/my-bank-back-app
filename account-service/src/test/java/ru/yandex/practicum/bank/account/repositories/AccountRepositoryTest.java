package ru.yandex.practicum.bank.account.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import ru.yandex.practicum.bank.account.models.AccountModel;
import ru.yandex.practicum.bank.account.models.CurrencyEnumModel;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Тесты уровня данных (Data JPA) для репозитория AccountRepository.
 * Проверяют корректность выполнения HQL/SQL запросов поиска счёта по логину
 * и выборки всех счетов за исключением заданного логина.
 * </summary>
 **/
@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class AccountRepositoryTest {

    // region Constants

    private static final String LOGIN_DMITRY = "dmitry";

    private static final String LOGIN_ALEXEY = "alexey";

    private static final String LOGIN_ELENA = "elena";

    // endregion

    // region Fields

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешный поиск счёта по логину, когда запись существует в БД.
     * </summary>
     **/
    @Test
    public void shouldFindByLoginWhenAccountExists() {
        var account = createAccount(LOGIN_DMITRY, "Дмитрий Волков");

        entityManager.persistAndFlush(account);

        var found = accountRepository.findByLogin(LOGIN_DMITRY);

        assertThat(found).isPresent();

        assertThat(found.get().getLogin()).isEqualTo(LOGIN_DMITRY);

        assertThat(found.get().getName()).isEqualTo("Дмитрий Волков");
    }

    /**
     * <summary>
     * Проверяет возвращение пустой Optional при поиске по несуществующему логину.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyWhenLoginDoesNotExist() {
        var found = accountRepository.findByLogin("nonexistent_user");

        assertThat(found).isEmpty();
    }

    /**
     * <summary>
     * Проверяет получения списка всех пользователей счетов за исключением текущего логина.
     * </summary>
     **/
    @Test
    public void shouldReturnAllAccountsExceptExcludedLogin() {
        var dmitry = createAccount(LOGIN_DMITRY, "Дмитрий Волков");

        var alexey = createAccount(LOGIN_ALEXEY, "Алексей Морозов");

        var elena = createAccount(LOGIN_ELENA, "Елена Кузнецова");

        entityManager.persist(dmitry);

        entityManager.persist(alexey);

        entityManager.persist(elena);

        entityManager.flush();

        var recipients = accountRepository.findAllByLoginNot(LOGIN_DMITRY);

        assertThat(recipients)
                .hasSize(2)
                .extracting(AccountModel::getLogin)
                .containsExactlyInAnyOrder(LOGIN_ALEXEY, LOGIN_ELENA);
    }

    /**
     * <summary>
     * Проверяет, что при исключении логина, которого нет в базе, возвращаются все существующие счета.
     * </summary>
     **/
    @Test
    public void shouldReturnAllAccountsWhenExcludedLoginDoesNotExist() {
        var alexey = createAccount(LOGIN_ALEXEY, "Алексей Морозов");

        var elena = createAccount(LOGIN_ELENA, "Елена Кузнецова");

        entityManager.persist(alexey);

        entityManager.persist(elena);

        entityManager.flush();

        var recipients = accountRepository.findAllByLoginNot(LOGIN_DMITRY);

        assertThat(recipients)
                .hasSize(2)
                .extracting(AccountModel::getLogin)
                .containsExactlyInAnyOrder(LOGIN_ALEXEY, LOGIN_ELENA);
    }

    /**
     * <summary>
     * Проверяет возвращение пустого списка, если в БД существует только исключаемый аккаунт.
     * </summary>
     **/
    @Test
    public void shouldReturnEmptyListWhenOnlyExcludedAccountExists() {
        var dmitry = createAccount(LOGIN_DMITRY, "Дмитрий Волков");

        entityManager.persistAndFlush(dmitry);

        var recipients = accountRepository.findAllByLoginNot(LOGIN_DMITRY);

        assertThat(recipients).isEmpty();
    }

    // endregion

    // region Helper Methods

    private AccountModel createAccount(String login, String name) {
        return new AccountModel(
                login,
                name,
                LocalDate.of(1999, 9, 19),
                new BigDecimal("1000.00"),
                CurrencyEnumModel.RUB
        );
    }

    // endregion
}