package ru.yandex.practicum.bank.shared.validators;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationSourceEnumModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <summary>
 * Тесты для проверки логики валидатора NotificationEventValidator.
 * Покрывают сценарии с пустыми значениями, нефинансовыми событиями, а также проверку сумм и валют для денежных операций.
 * </summary>
 */
public class NotificationEventValidatorTest {

    // region Fields

    private NotificationEventValidator validator;

    private ConstraintValidatorContext context;

    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    // endregion

    // region Setup

    @BeforeEach
    void setUp() {
        validator = new NotificationEventValidator();

        context = mock(ConstraintValidatorContext.class);

        violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        nodeBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
    }

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что валидатор возвращает true, если само событие равно null.
     * </summary>
     */
    @Test
    void shouldReturnTrueWhenEventIsNull() {
        var isValid = validator.isValid(null, context);

        assertThat(isValid).isTrue();
    }

    /**
     * <summary>
     * Проверяет, что валидатор возвращает true, если тип события равен null.
     * </summary>
     */
    @Test
    void shouldReturnTrueWhenEventTypeIsNull() {
        var event = createEvent(null, BigDecimal.TEN, CurrencyEnumModel.RUB);

        var isValid = validator.isValid(event, context);

        assertThat(isValid).isTrue();
    }

    /**
     * <summary>
     * Проверяет, что для нефинансовых событий (например, ACCOUNT_UPDATED) валидация проходит успешно
     * даже без указания суммы и валюты.
     * </summary>
     */
    @Test
    void shouldReturnTrueWhenEventTypeIsNotMoneyRelated() {
        var event = createEvent(NotificationTypeEnumModel.ACCOUNT_UPDATED, null, null);

        var isValid = validator.isValid(event, context);

        assertThat(isValid).isTrue();
    }

    /**
     * <summary>
     * Проверяет, что для финансовых событий сумма (amount) обязательна.
     * Ожидается false и добавление ошибки к полю "amount".
     * </summary>
     */
    @Test
    void shouldReturnFalseWhenAmountIsNullForMoneyEvent() {
        var event = createEvent(NotificationTypeEnumModel.CASH_DEPOSITED, null, CurrencyEnumModel.RUB);

        var isValid = validator.isValid(event, context);

        assertThat(isValid).isFalse();

        verify(context).disableDefaultConstraintViolation();

        verify(context).buildConstraintViolationWithTemplate("amount is required for money notification events");

        verify(violationBuilder).addPropertyNode("amount");

        verify(nodeBuilder).addConstraintViolation();
    }

    /**
     * <summary>
     * Проверяет, что для финансовых событий сумма (amount) должна быть строго больше нуля.
     * Тестируется нулевое значение.
     * </summary>
     */
    @Test
    void shouldReturnFalseWhenAmountIsZeroForMoneyEvent() {
        var event = createEvent(NotificationTypeEnumModel.CASH_WITHDRAWN, BigDecimal.ZERO, CurrencyEnumModel.USD);

        var isValid = validator.isValid(event, context);

        assertThat(isValid).isFalse();

        verify(context).buildConstraintViolationWithTemplate("amount must be positive for money notification events");

        verify(violationBuilder).addPropertyNode("amount");
    }

    /**
     * <summary>
     * Проверяет, что для финансовых событий сумма (amount) должна быть строго больше нуля.
     * Тестируется отрицательное значение.
     * </summary>
     */
    @Test
    void shouldReturnFalseWhenAmountIsNegativeForMoneyEvent() {
        var event = createEvent(NotificationTypeEnumModel.TRANSFER_OUTGOING, new BigDecimal("-100.50"), CurrencyEnumModel.RUB);

        var isValid = validator.isValid(event, context);

        assertThat(isValid).isFalse();

        verify(context).buildConstraintViolationWithTemplate("amount must be positive for money notification events");

        verify(violationBuilder).addPropertyNode("amount");
    }

    /**
     * <summary>
     * Проверяет, что для финансовых событий валюта (currency) обязательна.
     * Ожидается false и добавление ошибки к полю "currency".
     * </summary>
     */
    @Test
    void shouldReturnFalseWhenCurrencyIsNullForMoneyEvent() {
        var event = createEvent(NotificationTypeEnumModel.TRANSFER_INCOMING, BigDecimal.TEN, null);

        var isValid = validator.isValid(event, context);

        assertThat(isValid).isFalse();

        verify(context).buildConstraintViolationWithTemplate("currency is required for money notification events");

        verify(violationBuilder).addPropertyNode("currency");
    }

    /**
     * <summary>
     * Проверяет, что если в финансовом событии указаны корректная сумма и валюта,
     * оно успешно проходит валидацию.
     * </summary>
     */
    @Test
    void shouldReturnTrueWhenMoneyEventIsValid() {
        var event = createEvent(NotificationTypeEnumModel.CASH_DEPOSITED, new BigDecimal("500.00"), CurrencyEnumModel.RUB);

        var isValid = validator.isValid(event, context);

        assertThat(isValid).isTrue();
    }

    // endregion

    // region Helpers

    /**
     * <summary>
     * Вспомогательный метод для создания тестовой модели события.
     * </summary>
     */
    private NotificationEventModel createEvent(NotificationTypeEnumModel type, BigDecimal amount, CurrencyEnumModel currency) {
        return new NotificationEventModel(
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationSourceEnumModel.CASH,
                type,
                "testUser",
                "Тестовое сообщение",
                Instant.now(),
                amount,
                currency
        );
    }

    // endregion
}