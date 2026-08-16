package ru.yandex.practicum.bank.cash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "ru.yandex.practicum.bank.cash",
        "ru.yandex.practicum.bank.shared"
})
public class CashServiceApplication {

    // region Methods

    public static void main(String[] args) {
        SpringApplication.run(CashServiceApplication.class, args);
    }

    // endregion
}