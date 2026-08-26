package ru.yandex.practicum.bank.blocker.services;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.bank.blocker.configurations.properties.BlockerProperties;
import ru.yandex.practicum.bank.blocker.exceptions.InvalidOperationRequestException;
import ru.yandex.practicum.bank.blocker.interfaces.BlockerService;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckResponseViewModel;

/**
 * <summary>
 * Сервис для проверки банковских операций на соответствие ограничениям блокировки.
 * Проверяет участников операции, базовую валюту и максимально допустимую сумму.
 * </summary>
 */
@Service
public class BlockerServiceImpl implements BlockerService {

    // region Fields

    /**
     * <summary>
     * Конфигурационные свойства сервиса блокировки операций.
     * </summary>
     */
    private final BlockerProperties properties;

    // endregion

    // region Constructors

    public BlockerServiceImpl(BlockerProperties properties) {
        this.properties = properties;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Проверяет банковскую операцию на соответствие ограничениям блокировки.
     * Проверяет участников операции, базовую валюту и максимально допустимую сумму.
     * </summary>
     * @param request Запрос с данными проверяемой банковской операции.
     * @return Результат проверки операции с признаком разрешения и причиной отказа.
     * @throws InvalidOperationRequestException Если данные операции некорректны.
     */
    public OperationCheckResponseViewModel check(OperationCheckRequestViewModel request) {
        validateParticipants(request);

        validateBaseCurrency(request);

        if (request.normalizedAmount().compareTo(properties.maxAmount()) > 0) {
            return new OperationCheckResponseViewModel(false, "Operation amount exceeds blocker limit");
        }

        return new OperationCheckResponseViewModel(true, null);
    }

    /**
     * <summary>
     * Проверяет наличие обязательных участников операции.
     * Для перевода проверяет отправителя и получателя, а для операций
     * пополнения и снятия денежных средств — логин пользователя.
     * </summary>
     * @param request Запрос с данными проверяемой банковской операции.
     * @throws InvalidOperationRequestException Если обязательный участник операции не указан.
     */
    private void validateParticipants(OperationCheckRequestViewModel request) {
        if (request.operationType() == OperationTypeEnumModel.TRANSFER) {
            requireText(request.sender(), "sender is required for transfer");

            requireText(request.recipient(), "recipient is required for transfer");

            return;
        }

        requireText(request.login(), "login is required for cash operation");
    }

    private void validateBaseCurrency(OperationCheckRequestViewModel request) {
        if (request.baseCurrency() != CurrencyEnumModel.RUB) {
            throw new InvalidOperationRequestException("baseCurrency must be RUB");
        }
    }

    /**
     * <summary>
     * Проверяет, что переданное текстовое значение заполнено.
     * </summary>
     * @param value Проверяемое текстовое значение.
     * @param message Сообщение об ошибке, если значение отсутствует.
     * @throws InvalidOperationRequestException Если значение равно null
     *                                          или содержит только пробелы.
     */
    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidOperationRequestException(message);
        }
    }

    // endregion
}