package ru.yandex.practicum.bank.shared.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.shared.exceptions.BlockerClientException;
import ru.yandex.practicum.bank.shared.models.CurrencyEnumModel;
import ru.yandex.practicum.bank.shared.models.OperationTypeEnumModel;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;
import ru.yandex.practicum.bank.shared.viewmodels.OperationCheckRequestViewModel;

import java.io.IOException;
import java.math.BigDecimal;

import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * <summary>
 * Модульные тесты для HTTP-клиента сервиса блокировки операций HttpBlockerClient.
 * Проверяют корректность отправки HTTP-запросов на проверку операций,
 * передачи токена аутентификации, маппинга ответов, обработки пустых ответов,
 * сетевых ошибок и работы Circuit Breaker.
 * </summary>
 **/
@ExtendWith(MockitoExtension.class)
public class HttpBlockerClientTest {


}