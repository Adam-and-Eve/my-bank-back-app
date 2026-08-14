package ru.yandex.practicum.bank.account.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <summary>
 * Модульные тесты для доменной сущности AccountModel.
 * Проверяют корректность работы конструктора, геттеров, бизнес-методов обновления профиля и баланса,
 * а также реализацию методов equals и hashCode по бизнес-ключу login.
 * </summary>
 **/
public class AccountModelTest {

    // region Constants

    private static final String LOGIN = "dmitry";

    private static final String NAME = "Дмитрий Волков";

    private static final LocalDate BIRTHDATE = LocalDate.of(1999, 9, 19);

    private static final BigDecimal BALANCE = new BigDecimal("1000000.00");

    private static final CurrencyEnumModel CURRENCY = CurrencyEnumModel.RUB;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет успешную инициализацию сущности через конструктор и возвращение корректных значений из геттеров.
     * </summary>
     **/
    @Test
    public void shouldCreateAccountModelAndReturnCorrectFields() {
        AccountModel account = new AccountModel(LOGIN, NAME, BIRTHDATE, BALANCE, CURRENCY);

        assertThat(account.getLogin()).isEqualTo(LOGIN);

        assertThat(account.getName()).isEqualTo(NAME);

        assertThat(account.getBirthdate()).isEqualTo(BIRTHDATE);

        assertThat(account.getBalance()).isEqualTo(BALANCE);

        assertThat(account.getCurrency()).isEqualTo(CURRENCY);

        assertThat(account.getId()).isNull();

        assertThat(account.getVersion()).isNull();
    }

    /**
     * <summary>
     * Проверяет выброс NullPointerException с соответствующими сообщениями при передаче null в конструктор.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenConstructorArgsAreNull() {
        assertThatThrownBy(() -> new AccountModel(null, NAME, BIRTHDATE, BALANCE, CURRENCY))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Login must not be null");

        assertThatThrownBy(() -> new AccountModel(LOGIN, null, BIRTHDATE, BALANCE, CURRENCY))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Name must not be null");

        assertThatThrownBy(() -> new AccountModel(LOGIN, NAME, null, BALANCE, CURRENCY))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Birthdate must not be null");

        assertThatThrownBy(() -> new AccountModel(LOGIN, NAME, BIRTHDATE, null, CURRENCY))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Balance must not be null");

        assertThatThrownBy(() -> new AccountModel(LOGIN, NAME, BIRTHDATE, BALANCE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Currency must not be null");
    }

    /**
     * <summary>
     * Проверяет успешное обновление профильных данных (имени и даты рождения).
     * </summary>
     **/
    @Test
    public void shouldUpdateProfileSuccessfully() {
        AccountModel account = new AccountModel(LOGIN, NAME, BIRTHDATE, BALANCE, CURRENCY);

        String newName = "Дмитрий Игоревич";

        LocalDate newBirthdate = LocalDate.of(1999, 10, 20);

        account.updateProfile(newName, newBirthdate);

        assertThat(account.getName()).isEqualTo(newName);

        assertThat(account.getBirthdate()).isEqualTo(newBirthdate);
    }

    /**
     * <summary>
     * Проверяет выброс NullPointerException при попытке обновить профиль с null-значениями.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenUpdateProfileWithNull() {
        AccountModel account = new AccountModel(LOGIN, NAME, BIRTHDATE, BALANCE, CURRENCY);

        assertThatThrownBy(() -> account.updateProfile(null, BIRTHDATE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Name must not be null");

        assertThatThrownBy(() -> account.updateProfile(NAME, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Birthdate must not be null");
    }

    /**
     * <summary>
     * Проверяет успешное изменение текущего баланса счёта.
     * </summary>
     **/
    @Test
    public void shouldSetBalanceSuccessfully() {
        AccountModel account = new AccountModel(LOGIN, NAME, BIRTHDATE, BALANCE, CURRENCY);

        BigDecimal newBalance = new BigDecimal("2000000.00");

        account.setBalance(newBalance);

        assertThat(account.getBalance()).isEqualTo(newBalance);
    }

    /**
     * <summary>
     * Проверяет выброс NullPointerException при попытке установить null в качестве баланса.
     * </summary>
     **/
    @Test
    public void shouldThrowExceptionWhenSetBalanceWithNull() {
        AccountModel account = new AccountModel(LOGIN, NAME, BIRTHDATE, BALANCE, CURRENCY);

        assertThatThrownBy(() -> account.setBalance(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Balance must not be null");
    }

    /**
     * <summary>
     * Проверяет равенство объектов по бизнес-ключу login и совпадение их hashCode.
     * </summary>
     **/
    @Test
    public void shouldBeEqualWhenSameLogin() {
        AccountModel account1 = new AccountModel(LOGIN, NAME, BIRTHDATE, BALANCE, CURRENCY);

        AccountModel account2 = new AccountModel(LOGIN, "Другое Имя", LocalDate.of(2000, 1, 1), BigDecimal.ZERO, CURRENCY);

        assertThat(account1).isEqualTo(account2);

        assertThat(account1.hashCode()).isEqualTo(account2.hashCode());
    }

    /**
     * <summary>
     * Проверяет неравенство объектов при отличающихся логинах.
     * </summary>
     **/
    @Test
    public void shouldNotBeEqualWhenDifferentLogin() {
        AccountModel account1 = new AccountModel("dmitry", NAME, BIRTHDATE, BALANCE, CURRENCY);

        AccountModel account2 = new AccountModel("alexey", NAME, BIRTHDATE, BALANCE, CURRENCY);

        assertThat(account1).isNotEqualTo(account2);
    }

    /**
     * <summary>
     * Проверяет граничные случаи работы метода equals (рефлексивность, сравнение с null и с объектом другого класса).
     * </summary>
     **/
    @Test
    public void shouldHandleEqualsEdgeCases() {
        AccountModel account = new AccountModel(LOGIN, NAME, BIRTHDATE, BALANCE, CURRENCY);

        assertThat(account).isEqualTo(account);

        assertThat(account).isNotEqualTo(null);

        assertThat(account).isNotEqualTo("other_type_object");
    }

    // endregion
}