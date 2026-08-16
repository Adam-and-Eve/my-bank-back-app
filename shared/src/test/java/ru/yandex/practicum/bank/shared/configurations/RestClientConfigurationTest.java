package ru.yandex.practicum.bank.shared.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <summary>
 * Тесты конфигурации RestClientConfiguration.
 * Проверяют корректность создания и регистрации бина RestClient.Builder
 * с поддержкой клиентской балансировки нагрузки.
 * </summary>
 **/
@SpringBootTest(classes = RestClientConfiguration.class)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
public class RestClientConfigurationTest {

    // region Fields

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RestClient.Builder restClientBuilder;

    // endregion

    // region Tests

    /**
     * <summary>
     * Проверяет, что бин RestClient.Builder успешно создается
     * и регистрируется в контексте Spring.
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
     * Проверяет, что фабричный метод restClientBuilder
     * помечен аннотацией @LoadBalanced.
     * </summary>
     **/
    @Test
    public void shouldHaveLoadBalancedAnnotationOnBeanMethod() throws NoSuchMethodException {
        Method method = RestClientConfiguration.class.getDeclaredMethod("restClientBuilder");

        assertThat(method.isAnnotationPresent(LoadBalanced.class)).isTrue();
    }

    /**
     * <summary>
     * Проверяет, что с помощью внедренного RestClient.Builder
     * можно успешно создать экземпляр RestClient.
     * </summary>
     **/
    @Test
    public void shouldSuccessfullyBuildRestClientInstance() {
        var restClient = restClientBuilder.build();

        assertThat(restClient).isNotNull();
    }

    // endregion
}