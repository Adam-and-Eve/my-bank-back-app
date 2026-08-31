package ru.yandex.practicum.bank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.yandex.practicum.bank.shared.configurations.NotificationProducerConfiguration;

@EnableRetry
@EnableScheduling
@Import(NotificationProducerConfiguration.class)
@ConfigurationPropertiesScan
@SpringBootApplication
public class AccountServiceApplication {

    // region Methods

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    // endregion
}