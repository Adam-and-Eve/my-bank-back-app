package ru.yandex.practicum.bank.shared.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.yandex.practicum.bank.shared.interfaces.ValidNotificationEvent;
import ru.yandex.practicum.bank.shared.models.NotificationEventModel;
import ru.yandex.practicum.bank.shared.models.NotificationTypeEnumModel;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

/**
 * <summary>
 * Валидатор для проверки корректности данных модели события уведомления (применяется через аннотацию @ValidNotificationEvent).
 * Гарантирует, что для уведомлений, связанных с движением средств, обязательно указаны корректная сумма и валюта.
 * </summary>
 */
public class NotificationEventValidator implements ConstraintValidator<ValidNotificationEvent, NotificationEventModel> {

    // region Methods

    /**
     * <summary>
     * Набор типов уведомлений, которые подразумевают финансовые операции и требуют обязательного указания суммы и валюты.
     * </summary>
     */
    private static final Set<NotificationTypeEnumModel> MONEY_EVENT_TYPES = EnumSet.of(
            NotificationTypeEnumModel.CASH_DEPOSITED,
            NotificationTypeEnumModel.CASH_WITHDRAWN,
            NotificationTypeEnumModel.TRANSFER_OUTGOING,
            NotificationTypeEnumModel.TRANSFER_INCOMING
    );

    /**
     * <summary>
     * Выполняет проверку переданного события уведомления.
     * </summary>
     * @param event Событие уведомления для валидации.
     * @param context Контекст валидатора для построения сообщений об ошибках.
     * @return true, если событие валидно, иначе false.
     */
    @Override
    public boolean isValid(NotificationEventModel event, ConstraintValidatorContext context) {
        if (event == null || event.type() == null) {
            return true;
        }

        if (!MONEY_EVENT_TYPES.contains(event.type())) {
            return true;
        }

        boolean valid = true;

        context.disableDefaultConstraintViolation();

        if (event.amount() == null) {
            addViolation(context, "amount is required for money notification events", "amount");

            valid = false;
        } else if (event.amount().compareTo(BigDecimal.ZERO) <= 0) {
            addViolation(context, "amount must be positive for money notification events", "amount");

            valid = false;
        }

        if (event.currency() == null) {
            addViolation(context, "currency is required for money notification events", "currency");

            valid = false;
        }

        return valid;
    }

    /**
     * <summary>
     * Вспомогательный метод для привязки сообщения об ошибке валидации к конкретному полю (свойству) объекта.
     * </summary>
     * @param context Контекст валидатора.
     * @param message Текст сообщения об ошибке.
     * @param property Имя поля, к которому относится ошибка.
     */
    private void addViolation(ConstraintValidatorContext context, String message, String property) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
    }

    // endregion
}