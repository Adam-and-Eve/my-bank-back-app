package ru.yandex.practicum.bank.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import ru.yandex.practicum.bank.shared.clients.HttpBlockerClient;
import ru.yandex.practicum.bank.shared.configurations.OAuth2ClientConfiguration;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "ru.yandex.practicum.bank.notification",
                "ru.yandex.practicum.bank.shared"
        },
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "ru\\.yandex\\.practicum\\.bank\\.shared\\.clients\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = OAuth2ClientConfiguration.class
                )
        }
)
public class NotificationServiceApplication {

    // region Methods

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    // endregion
}