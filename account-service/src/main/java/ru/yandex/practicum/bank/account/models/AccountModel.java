package ru.yandex.practicum.bank.account.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * <summary>
 * Cущность банковского счёта пользователя (AccountModel).
 * Хранит персональные данные владельца, текущий баланс, валюту и версию для оптимистичной блокировки.
 * </summary>
 **/
@Entity
@Table(name = "accounts")
public class AccountModel {

    // region Fields

    /**
     * <summary>
     * Уникальный первичный ключ записи в БД.
     * </summary>
     **/
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * <summary>
     * Уникальный логин пользователя (бизнес-ключ сущности).
     * </summary>
     **/
    @Column(nullable = false, unique = true, length = 64)
    private String login;

    /**
     * <summary>
     * Отображаемое имя владельца счёта.
     * </summary>
     **/
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * <summary>
     * Дата рождения владельца счёта.
     * </summary>
     **/
    @Column(nullable = false)
    private LocalDate birthdate;

    /**
     * <summary>
     * Текущий остаток средств на счёте.
     * </summary>
     **/
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /**
     * <summary>
     * Валюта счёта.
     * </summary>
     **/
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyEnumModel currency;

    /**
     * <summary>
     * Версия записи для оптимистичной блокировки (Optimistic Locking).
     * Защищает от неверного параллельного обновления баланса.
     * </summary>
     **/
    @Version
    @Column(nullable = false)
    private Long version;

    // endregion

    // region Constructors

    protected AccountModel() {
    }

    public AccountModel(
            String login,
            String name,
            LocalDate birthdate,
            BigDecimal balance,
            CurrencyEnumModel currency) {

        this.login = Objects.requireNonNull(login, "Login must not be null");
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.birthdate = Objects.requireNonNull(birthdate, "Birthdate must not be null");
        this.balance = Objects.requireNonNull(balance, "Balance must not be null");
        this.currency = Objects.requireNonNull(currency, "Currency must not be null");
    }

    // endregion

    // region Properties

    /**
     * @return Уникальный ID записи.
     */
    public Long getId() {
        return id;
    }

    /**
     * @return Логин владельца счёта.
     */
    public String getLogin() {
        return login;
    }

    /**
     * @return Имя владельца.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Дата рождения владельца.
     */
    public LocalDate getBirthdate() {
        return birthdate;
    }

    /**
     * @return Текущий баланс.
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * @return Валюта счёта.
     */
    public CurrencyEnumModel getCurrency() {
        return currency;
    }

    /**
     * @return Версия оптимистичной блокировки.
     */
    public Long getVersion() {
        return version;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Обновляет профильные данные владельца счёта (имя и дату рождения).
     * </summary>
     * @param name Новое имя владельца.
     * @param birthdate Новая дата рождения.
     */
    public void updateProfile(String name, LocalDate birthdate) {
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.birthdate = Objects.requireNonNull(birthdate, "Birthdate must not be null");
    }

    /**
     * <summary>
     * Устанавливает новое значение баланса.
     * </summary>
     * @param balance Новый баланс.
     */
    public void setBalance(BigDecimal balance) {
        this.balance = Objects.requireNonNull(balance, "Balance must not be null");
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AccountModel account)) {
            return false;
        }

        return Objects.equals(login, account.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login);
    }

    // endregion
}