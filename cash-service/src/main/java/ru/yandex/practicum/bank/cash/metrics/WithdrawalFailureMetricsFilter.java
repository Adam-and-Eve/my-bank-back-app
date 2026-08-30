package ru.yandex.practicum.bank.cash.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <summary>
 * Фильтр HTTP-запросов для сбора метрик неудачных операций снятия наличных.
 * Перехватывает запросы к эндпоинту снятия средств и инкрементирует метрику-счетчик
 * в случае возникновения исключений или возврата ошибочного HTTP-статуса (не 2xx).
 * </summary>
 **/
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class WithdrawalFailureMetricsFilter extends OncePerRequestFilter {

    // region Fields

    static final String METRIC_NAME = "my.bank.cash.withdrawal.failures";
    private static final String WITHDRAWAL_PATH = "/api/cash/withdraw";

    private final MeterRegistry meterRegistry;

    // endregion

    // region Constructors

    /**
     * <summary>
     * Инициализирует фильтр с зависимостью для работы с реестром метрик.
     * </summary>
     * @param meterRegistry Реестр метрик Micrometer для регистрации счетчиков ошибок.
     **/
    public WithdrawalFailureMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Основной метод фильтрации. Пропускает запросы, не относящиеся к снятию наличных.
     * Для целевых запросов оборачивает выполнение цепочки фильтров в try-catch
     * для надежной регистрации сбоев. Также проверяет итоговый HTTP-статус ответа.
     * </summary>
     * @param request Текущий HTTP-запрос.
     * @param response Текущий HTTP-ответ.
     * @param filterChain Цепочка фильтров для дальнейшей обработки запроса.
     * @throws ServletException Если произошла ошибка внутри сервлета при обработке.
     * @throws IOException Если произошла сетевая ошибка или ошибка ввода-вывода.
     * @throws RuntimeException В случае непредвиденных ошибок времени выполнения.
     **/
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isWithdrawal(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            recordFailure();

            throw exception;
        }

        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            recordFailure();
        }
    }

    /**
     * <summary>
     * Проверяет, является ли текущий HTTP-запрос целевым запросом на снятие наличных (POST /api/cash/withdraw).
     * </summary>
     * @param request HTTP-запрос для проверки.
     * @return true, если это POST-запрос к эндпоинту снятия наличных, иначе false.
     **/
    private boolean isWithdrawal(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && WITHDRAWAL_PATH.equals(request.getRequestURI());
    }

    /**
     * <summary>
     * Инкрементирует метрику неудачного снятия наличных.
     * Извлекает логин пользователя из контекста безопасности (JWT) для добавления
     * в качестве тега метрики.
     * </summary>
     **/
    private void recordFailure() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            return;
        }

        var login = jwtAuthentication.getToken().getClaimAsString("preferred_username");

        if (login == null || login.isBlank()) {
            return;
        }

        meterRegistry.counter(METRIC_NAME, "login", login).increment();
    }

    // endregion
}