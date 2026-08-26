package ru.yandex.practicum.bank.shared.viewmodels;

/**
 * <summary>
 * Модель ответа с результатом проверки банковской операции.
 * Содержит признак разрешения операции и причину принятого решения.
 * </summary>
 * @param allowed Признак того, разрешено ли выполнение операции.
 * @param reason Причина разрешения или отказа в выполнении операции.
 */
public record OperationCheckResponseViewModel (
        boolean allowed,
        String reason
) {
}