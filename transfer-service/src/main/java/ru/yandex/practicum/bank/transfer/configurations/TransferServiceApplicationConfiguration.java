package ru.yandex.practicum.bank.transfer.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import ru.yandex.practicum.bank.shared.providers.ServiceTokenProvider;

@Configuration
public class TransferServiceApplicationConfiguration {

    // region Beans

    @Bean
    public ServiceTokenProvider serviceTokenProvider(OAuth2AuthorizedClientManager manager) {
        return new ServiceTokenProvider(manager, "transfer-service");
    }

    // endregion
}