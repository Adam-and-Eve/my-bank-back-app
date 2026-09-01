package ru.yandex.practicum.bank.transfer.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * <summary>
 * Фильтр HTTP-запросов для сбора метрик неудачных переводов денежных средств.
 * Перехватывает запросы к эндпоинту переводов, кэширует их тело и инкрементирует
 * метрику-счетчик в случае возникновения исключений или возврата ошибочного HTTP-статуса (не 2xx).
 * </summary>
 **/
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TransferFailureMetricsFilter extends OncePerRequestFilter {

    // region Fields

    static final String METRIC_NAME = "my.bank.transfer.failures";
    private static final String TRANSFER_PATH = "/api/transfer";
    private static final String UNKNOWN_RECIPIENT = "unknown";
    private static final int REQUEST_CACHE_LIMIT = 16_384;

    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    // endregion

    // region Constructors

    /**
     * <summary>
     * Инициализирует фильтр с необходимыми зависимостями для работы с метриками и десериализации JSON.
     * </summary>
     * @param meterRegistry Реестр метрик Micrometer для регистрации счетчиков ошибок.
     * @param objectMapper Маппер для чтения тела запроса и извлечения логина получателя.
     **/
    public TransferFailureMetricsFilter(MeterRegistry meterRegistry, ObjectMapper objectMapper) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    // endregion

    // region Methods

    /**
     * <summary>
     * Основной метод фильтрации. Пропускает запросы, не относящиеся к переводам.
     * Для целевых запросов кэширует тело и оборачивает выполнение цепочки фильтров в try-catch
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
        if (!isTransfer(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        var cachedRequest = new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);

        try {
            filterChain.doFilter(cachedRequest, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            recordFailure(cachedRequest);

            throw exception;
        }

        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            recordFailure(cachedRequest);
        }
    }

    /**
     * <summary>
     * Проверяет, является ли текущий HTTP-запрос целевым запросом на перевод средств (POST /api/transfer).
     * </summary>
     * @param request HTTP-запрос для проверки.
     * @return true, если это POST-запрос к эндпоинту переводов, иначе false.
     **/
    private boolean isTransfer(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && TRANSFER_PATH.equals(request.getRequestURI());
    }

    /**
     * <summary>
     * Инкрементирует метрику неудачного перевода.
     * Извлекает логин отправителя из контекста безопасности (JWT), а логин получателя
     * из закэшированного тела запроса для добавления в качестве тегов метрики.
     * </summary>
     * @param request HTTP-запрос с закэшированным телом для извлечения данных.
     **/
    private void recordFailure(ContentCachingRequestWrapper request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            return;
        }

        var senderLogin = jwtAuthentication.getToken().getClaimAsString("preferred_username");

        if (senderLogin == null || senderLogin.isBlank()) {
            return;
        }

        meterRegistry.counter(
                METRIC_NAME,
                "sender_login", senderLogin,
                "recipient_login", recipientLogin(request)
        ).increment();
    }

    /**
     * <summary>
     * Безопасно извлекает логин получателя из JSON-тела закэшированного запроса.
     * В случае отсутствия тела, ошибки парсинга или пустого значения возвращает fallback-строку.
     * </summary>
     * @param request HTTP-запрос с закэшированным телом.
     * @return Строка с логином получателя или константа UNKNOWN_RECIPIENT при невозможности извлечения.
     **/
    private String recipientLogin(ContentCachingRequestWrapper request) {
        var content = request.getContentAsByteArray();

        if (content.length == 0) {
            return UNKNOWN_RECIPIENT;
        }

        try {
            var recipient = objectMapper.readTree(content).path("recipientLogin").asText();

            return recipient == null || recipient.isBlank() ? UNKNOWN_RECIPIENT : recipient;
        } catch (IOException exception) {
            return UNKNOWN_RECIPIENT;
        }
    }

    // endregion
}