package ru.yandex.practicum.bank.cash.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;

@Configuration
public class CashServiceApplicationConfiguration {

    // region Beans

    @Bean
    public ServiceTokenProvider serviceTokenProvider(OAuth2AuthorizedClientManager manager) {
        return new ServiceTokenProvider(manager, "cash-service");
    }

    // endregion
}