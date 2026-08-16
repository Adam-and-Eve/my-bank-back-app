package ru.yandex.practicum.bank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class AccountServiceApplication {

    // region Methods

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    // endregion
}