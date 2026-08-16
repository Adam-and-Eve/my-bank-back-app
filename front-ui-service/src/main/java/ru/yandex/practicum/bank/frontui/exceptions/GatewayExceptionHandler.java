package ru.yandex.practicum.bank.frontui.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.bank.frontui.exceptions.GatewayClientException;
import ru.yandex.practicum.bank.frontui.viewmodels.ApiErrorResponseViewModel;

import java.io.IOException;

/**
 * <summary>
 * Компонент обработки HTTP-ошибок при ответах от API Gateway.
 * Разбирает тело ошибки в {@link ApiErrorResponseViewModel} и формирует {@link GatewayClientException}.
 * </summary>
 **/
@Component
public class GatewayExceptionHandler {

    // region Fields

    private final ObjectMapper objectMapper;

    // endregion

    // region Constructors

    @Autowired
    public GatewayExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // endregion

    // region Exceptions

    /**
     * <summary>
     * Обрабатывает ошибочный ответ от API Gateway.
     * </summary>
     * @param response HTTP-ответ с ошибкой.
     * @throws IOException При ошибках чтения тела ответа.
     * @throws GatewayClientException Всегда выбрасывается с описанием причины ошибки.
     **/
    public void handleError(ClientHttpResponse response) throws IOException {
        var body = response.getBody().readAllBytes();

        var message = extractMessage(body);

        if (message != null) {
            throw new GatewayClientException(message);
        }

        throw new GatewayClientException("Gateway request failed: " + response.getStatusCode());
    }

    private String extractMessage(byte[] body) {
        if (body.length == 0) {
            return null;
        }

        try {
            ApiErrorResponseViewModel error = objectMapper.readValue(body, ApiErrorResponseViewModel.class);

            if (error.message() != null && !error.message().isBlank()) {
                return error.message();
            }
        } catch (IOException ignored) {
            // Фолбэк на статус ответа, если шлюз вернул тело ошибки не в формате ApiErrorResponseViewModel
        }

        return null;
    }

    // endregion
}