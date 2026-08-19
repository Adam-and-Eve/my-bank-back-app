package ru.yandex.practicum.bank.blocker.controllers;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.bank.blocker.interfaces.BlockerService;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckResponseViewModel;

/**
 * <summary>
 * REST-контроллер для проверки банковских операций на соответствие
 * ограничениям сервиса блокировки.
 * </summary>
 */
@RestController
@RequestMapping("/api/blocker")
public class BlockerController {

    // region Fields

    /**
     * <summary>
     * Сервис для проверки банковских операций.
     * </summary>
     */
    private final BlockerService blockerService;

    // endregion

    // region Constructors

    public BlockerController(BlockerService blockerService) {
        this.blockerService = blockerService;
    }

    // endregion

    // region Actions

    /**
     * <summary>
     * Проверяет банковскую операцию на соответствие ограничениям блокировки.
     * </summary>
     * @param request Запрос с данными банковской операции.
     * @return Результат проверки операции с признаком разрешения
     *         и причиной отказа при наличии.
     */
    @PostMapping("/check")
    public OperationCheckResponseViewModel check(
            @Valid
            @RequestBody
            OperationCheckRequestViewModel request) {

        return blockerService.check(request);
    }

    // endregion
}