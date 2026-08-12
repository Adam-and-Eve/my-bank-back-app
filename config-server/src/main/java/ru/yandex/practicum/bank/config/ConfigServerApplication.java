package ru.yandex.practicum.bank.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

    // region Methods

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }

    // endregion
}