package ru.yandex.practicum.bank.shared.interfaces;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.yandex.practicum.bank.shared.validators.NotificationEventValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <summary>
 * Аннотация для валидации модели события уведомления (проверяет консистентность данных в зависимости от типа и источника уведомления).
 * </summary>
 */
@Documented
@Constraint(validatedBy = NotificationEventValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNotificationEvent {

    // region Methods

    /**
     * <summary>
     * Сообщение об ошибке по умолчанию, возвращаемое в случае неудачной валидации.
     * </summary>
     * @return Строка с сообщением об ошибке.
     */
    public String message() default "notification event does not match its type";

    /**
     * <summary>
     * Группы валидации, к которым применяется данное ограничение.
     * </summary>
     * @return Массив классов групп валидации.
     */
    public Class<?>[] groups() default {};

    /**
     * <summary>
     * Полезная нагрузка (метаданные), ассоциированная с ограничением.
     * </summary>
     * @return Массив классов полезной нагрузки.
     */
    public Class<? extends Payload>[] payload() default {};

    // endregion
}