package ru.yandex.practicum.bank.blocker.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.bank.blocker.configurations.properties.BlockerProperties;
import ru.yandex.practicum.bank.blocker.exceptions.InvalidOperationRequestException;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckResponseViewModel;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <summary>
 * Unit-тесты сервиса блокировки банковских операций.
 * Проверяют валидацию участников и базовой валюты операции,
 * а также ограничение максимальной суммы операции.
 * </summary>
 */
public class BlockerServiceImplTest {

    // region Constants

    /**
     * <summary>
     * Максимальная сумма нормализованной операции,
     * используемая в тестах.
     * </summary>
     */
    private static final BigDecimal MAX_AMOUNT =
            new BigDecimal("100000.00");

    /**
     * <summary>
     * Сообщение об отказе при превышении максимальной суммы операции.
     * </summary>
     */
    private static final String AMOUNT_LIMIT_MESSAGE =
            "Operation amount exceeds blocker limit";

    /**
     * <summary>
     * Сообщение об ошибке при отсутствии базовой валюты RUB.
     * </summary>
     */
    private static final String BASE_CURRENCY_MESSAGE =
            "baseCurrency must be RUB";

    // endregion

    // region Fields

    /**
     * <summary>
     * Сервис блокировки операций, тестируемый в текущем классе.
     * </summary>
     */
    private BlockerServiceImpl blockerService;

    // endregion

    // region Setup

    /**
     * <summary>
     * Создает сервис блокировки с фиксированным лимитом максимальной
     * нормализованной суммы перед выполнением каждого теста.
     * </summary>
     */
    @BeforeEach
    void setUp() {
        var properties = new BlockerProperties(MAX_AMOUNT);

        blockerService = new BlockerServiceImpl(properties);
    }

    // endregion

    // region Successful operations

    /**
     * <summary>
     * Проверяет успешное разрешение операции пополнения счета,
     * если логин указан, базовая валюта равна RUB,
     * а нормализованная сумма не превышает установленный лимит.
     * </summary>
     */
    @Test
    void checkShouldAllowDepositWhenRequestIsValid() {
        var request = createRequest(
                OperationTypeEnumModel.DEPOSIT,
                "user",
                null,
                null,
                "50000.00",
                CurrencyEnumModel.RUB,
                "50000.00",
                CurrencyEnumModel.RUB
        );

        var result = blockerService.check(request);

        assertAllowed(result);
    }

    /**
     * <summary>
     * Проверяет успешное разрешение операции снятия денежных средств,
     * если логин указан, базовая валюта равна RUB,
     * а нормализованная сумма не превышает установленный лимит.
     * </summary>
     */
    @Test
    void checkShouldAllowWithdrawWhenRequestIsValid() {
        var request = createRequest(
                OperationTypeEnumModel.WITHDRAW,
                "user",
                null,
                null,
                "50000.00",
                CurrencyEnumModel.RUB,
                "50000.00",
                CurrencyEnumModel.RUB
        );

        var result = blockerService.check(request);

        assertAllowed(result);
    }

    /**
     * <summary>
     * Проверяет успешное разрешение операции перевода,
     * если отправитель и получатель указаны, базовая валюта равна RUB,
     * а нормализованная сумма не превышает установленный лимит.
     * </summary>
     */
    @Test
    void checkShouldAllowTransferWhenRequestIsValid() {
        var request = createRequest(
                OperationTypeEnumModel.TRANSFER,
                null,
                "sender",
                "recipient",
                "50000.00",
                CurrencyEnumModel.RUB,
                "50000.00",
                CurrencyEnumModel.RUB
        );

        var result = blockerService.check(request);

        assertAllowed(result);
    }

    /**
     * <summary>
     * Проверяет, что операция с нормализованной суммой,
     * равной максимальному лимиту, разрешается.
     * </summary>
     */
    @Test
    void checkShouldAllowOperationWhenNormalizedAmountEqualsMaxAmount() {
        var request = createRequest(
                OperationTypeEnumModel.DEPOSIT,
                "user",
                null,
                null,
                "100000.00",
                CurrencyEnumModel.RUB,
                "100000.00",
                CurrencyEnumModel.RUB
        );

        var result = blockerService.check(request);

        assertAllowed(result);
    }

    /**
     * <summary>
     * Проверяет успешное разрешение операции в иностранной валюте,
     * если ее нормализованная сумма в RUB не превышает лимит.
     * </summary>
     */
    @Test
    void checkShouldAllowOperationWhenNormalizedForeignCurrencyAmountIsWithinLimit() {
        var request = createRequest(
                OperationTypeEnumModel.DEPOSIT,
                "user",
                null,
                null,
                "1000.00",
                CurrencyEnumModel.USD,
                "92000.00",
                CurrencyEnumModel.RUB
        );

        var result = blockerService.check(request);

        assertAllowed(result);
    }

    /**
     * <summary>
     * Проверяет, что лимит определяется по нормализованной сумме,
     * а не по исходной сумме операции.
     * </summary>
     */
    @Test
    void checkShouldAllowOperationWhenOriginalAmountIsLargeButNormalizedAmountIsWithinLimit() {
        var request = createRequest(
                OperationTypeEnumModel.DEPOSIT,
                "user",
                null,
                null,
                "1000000.00",
                CurrencyEnumModel.USD,
                "92000.00",
                CurrencyEnumModel.RUB
        );

        var result = blockerService.check(request);

        assertAllowed(result);
    }

    /**
     * <summary>
     * Проверяет блокировку операции, если нормализованная сумма
     * превышает установленный максимальный лимит.
     * </summary>
     */
    @Test
    void checkShouldBlockOperationWhenNormalizedAmountExceedsMaxAmount() {
        var request = createRequest(
                OperationTypeEnumModel.DEPOSIT,
                "user",
                null,
                null,
                "1000.00",
                CurrencyEnumModel.USD,
                "100000.01",
                CurrencyEnumModel.RUB
        );

        var result = blockerService.check(request);

        assertFalse(result.allowed());
        assertEquals(AMOUNT_LIMIT_MESSAGE, result.reason());
    }

    /**
     * <summary>
     * Проверяет, что операция блокируется по нормализованной сумме,
     * даже если исходная сумма не превышает лимит.
     * </summary>
     */
    @Test
    void checkShouldBlockOperationWhenNormalizedAmountExceedsLimitButOriginalAmountDoesNot() {
        var request = createRequest(
                OperationTypeEnumModel.DEPOSIT,
                "user",
                null,
                null,
                "90000.00",
                CurrencyEnumModel.USD,
                "110000.00",
                CurrencyEnumModel.RUB
        );

        var result = blockerService.check(request);

        assertFalse(result.allowed());
        assertEquals(AMOUNT_LIMIT_MESSAGE, result.reason());
    }

    // endregion

    // region Base currency validation

    /**
     * <summary>
     * Проверяет отклонение операции, если базовая валюта
     * отличается от RUB.
     * </summary>
     */
    @Test
    void checkShouldRejectOperationWhenBaseCurrencyIsNotRub() {
        var request = createRequest(
                OperationTypeEnumModel.DEPOSIT,
                "user",
                null,
                null,
                "50000.00",
                CurrencyEnumModel.USD,
                "46000.00",
                CurrencyEnumModel.USD
        );

        var exception = assertThrows(
                InvalidOperationRequestException.class,
                () -> blockerService.check(request)
        );

        assertEquals(BASE_CURRENCY_MESSAGE, exception.getMessage());
    }

    // endregion

    // region Participant validation

    /**
     * <summary>
     * Проверяет, что операция пополнения без логина
     * отклоняется с исключением InvalidOperationRequestException.
     * </summary>
     */
    @Test
    void checkShouldRejectDepositWithoutLogin() {
        var request = createRequest(
                OperationTypeEnumModel.DEPOSIT,
                null,
                null,
                null,
                "50000.00",
                CurrencyEnumModel.RUB,
                "50000.00",
                CurrencyEnumModel.RUB
        );

        var exception = assertThrows(
                InvalidOperationRequestException.class,
                () -> blockerService.check(request)
        );

        assertEquals(
                "login is required for cash operation",
                exception.getMessage()
        );
    }

    /**
     * <summary>
     * Проверяет, что операция снятия без логина
     * отклоняется с исключением InvalidOperationRequestException.
     * </summary>
     */
    @Test
    void checkShouldRejectWithdrawWithoutLogin() {
        var request = createRequest(
                OperationTypeEnumModel.WITHDRAW,
                null,
                null,
                null,
                "50000.00",
                CurrencyEnumModel.RUB,
                "50000.00",
                CurrencyEnumModel.RUB
        );

        var exception = assertThrows(
                InvalidOperationRequestException.class,
                () -> blockerService.check(request)
        );

        assertEquals(
                "login is required for cash operation",
                exception.getMessage()
        );
    }

    /**
     * <summary>
     * Проверяет, что операция перевода без отправителя
     * отклоняется с исключением InvalidOperationRequestException.
     * </summary>
     */
    @Test
    void checkShouldRejectTransferWithoutSender() {
        var request = createRequest(
                OperationTypeEnumModel.TRANSFER,
                null,
                null,
                "recipient",
                "50000.00",
                CurrencyEnumModel.RUB,
                "50000.00",
                CurrencyEnumModel.RUB
        );

        var exception = assertThrows(
                InvalidOperationRequestException.class,
                () -> blockerService.check(request)
        );

        assertEquals(
                "sender is required for transfer",
                exception.getMessage()
        );
    }

    /**
     * <summary>
     * Проверяет, что операция перевода без получателя
     * отклоняется с исключением InvalidOperationRequestException.
     * </summary>
     */
    @Test
    void checkShouldRejectTransferWithoutRecipient() {
        var request = createRequest(
                OperationTypeEnumModel.TRANSFER,
                null,
                "sender",
                null,
                "50000.00",
                CurrencyEnumModel.RUB,
                "50000.00",
                CurrencyEnumModel.RUB
        );

        var exception = assertThrows(
                InvalidOperationRequestException.class,
                () -> blockerService.check(request)
        );

        assertEquals(
                "recipient is required for transfer",
                exception.getMessage()
        );
    }

    /**
     * <summary>
     * Проверяет, что пустой логин отклоняется для операции с наличными.
     * </summary>
     */
    @Test
    void checkShouldRejectCashOperationWithBlankLogin() {
        var request = createRequest(
                OperationTypeEnumModel.DEPOSIT,
                "   ",
                null,
                null,
                "50000.00",
                CurrencyEnumModel.RUB,
                "50000.00",
                CurrencyEnumModel.RUB
        );

        var exception = assertThrows(
                InvalidOperationRequestException.class,
                () -> blockerService.check(request)
        );

        assertEquals(
                "login is required for cash operation",
                exception.getMessage()
        );
    }

    /**
     * <summary>
     * Проверяет, что пустой отправитель отклоняется для операции перевода.
     * </summary>
     */
    @Test
    void checkShouldRejectTransferWithBlankSender() {
        var request = createRequest(
                OperationTypeEnumModel.TRANSFER,
                null,
                "   ",
                "recipient",
                "50000.00",
                CurrencyEnumModel.RUB,
                "50000.00",
                CurrencyEnumModel.RUB
        );

        var exception = assertThrows(
                InvalidOperationRequestException.class,
                () -> blockerService.check(request)
        );

        assertEquals(
                "sender is required for transfer",
                exception.getMessage()
        );
    }

    /**
     * <summary>
     * Проверяет, что пустой получатель отклоняется для операции перевода.
     * </summary>
     */
    @Test
    void checkShouldRejectTransferWithBlankRecipient() {
        var request = createRequest(
                OperationTypeEnumModel.TRANSFER,
                null,
                "sender",
                "   ",
                "50000.00",
                CurrencyEnumModel.RUB,
                "50000.00",
                CurrencyEnumModel.RUB
        );

        var exception = assertThrows(
                InvalidOperationRequestException.class,
                () -> blockerService.check(request)
        );

        assertEquals(
                "recipient is required for transfer",
                exception.getMessage()
        );
    }

    // endregion

    // region Helpers

    /**
     * <summary>
     * Создает запрос на проверку банковской операции
     * с указанными параметрами.
     * </summary>
     * @param operationType Тип банковской операции.
     * @param login Логин пользователя.
     * @param sender Отправитель денежных средств.
     * @param recipient Получатель денежных средств.
     * @param amount Сумма операции в исходной валюте.
     * @param currency Валюта исходной операции.
     * @param normalizedAmount Сумма операции, нормализованная в базовую валюту.
     * @param baseCurrency Базовая валюта нормализованной суммы.
     * @return Модель запроса на проверку операции.
     */
    private OperationCheckRequestViewModel createRequest(
            OperationTypeEnumModel operationType,
            String login,
            String sender,
            String recipient,
            String amount,
            CurrencyEnumModel currency,
            String normalizedAmount,
            CurrencyEnumModel baseCurrency) {

        return new OperationCheckRequestViewModel(
                "operation-id",
                operationType,
                login,
                sender,
                recipient,
                new BigDecimal(amount),
                currency,
                new BigDecimal(normalizedAmount),
                baseCurrency
        );
    }

    /**
     * <summary>
     * Проверяет успешный результат проверки операции.
     * </summary>
     * @param result Результат проверки банковской операции.
     */
    private void assertAllowed(OperationCheckResponseViewModel result) {
        assertTrue(result.allowed());
        assertNull(result.reason());
    }

    // endregion
}