package ru.yandex.practicum.bank.transfer.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.bank.transfer.interfaces.TransferService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Тесты конфигурации RestClientConfiguration.
 * Проверяют корректность инициализации и внедрения бина RestClient.Builder с поддержкой клиентской балансировки нагрузки.
 * </summary>
 **/
@SpringBootTest
public class RestClientConfigurationTest {

    // region Fields

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что бин RestClient.Builder успешно создается и регистрируется в контексте Spring.
     * </summary>
     **/
    @Test
    public void shouldRegisterRestClientBuilderBean() {
        assertThat(restClientBuilder).isNotNull();

        var bean = applicationContext.getBean(RestClient.Builder.class);

        assertThat(bean).isSameAs(restClientBuilder);
    }

    /**
     * <summary>
     * Проверяет, что метод фабрики бина restClientBuilder помечен аннотацией @LoadBalanced.
     * </summary>
     **/
    @Test
    public void shouldHaveLoadBalancedAnnotationOnBeanMethod() throws NoSuchMethodException {
        var method = RestClientConfiguration.class.getDeclaredMethod("restClientBuilder");

        assertThat(method.isAnnotationPresent(LoadBalanced.class)).isTrue();
    }

    /**
     * <summary>
     * Проверяет, что с помощью внедренного RestClient.Builder можно успешно скомпоновать экземпляр RestClient.
     * </summary>
     **/
    @Test
    public void shouldSuccessfullyBuildRestClientInstance() {
        var restClient = restClientBuilder.build();

        assertThat(restClient).isNotNull();
    }

    // endregion
}