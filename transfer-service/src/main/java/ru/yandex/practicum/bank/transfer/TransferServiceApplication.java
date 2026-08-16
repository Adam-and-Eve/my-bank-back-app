package ru.yandex.practicum.bank.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "ru.yandex.practicum.bank.transfer",
        "ru.yandex.practicum.bank.shared"
})
public class TransferServiceApplication {

    // region Methods

    public static void main(String[] args) {
        SpringApplication.run(TransferServiceApplication.class, args);
    }

    // endregion
}