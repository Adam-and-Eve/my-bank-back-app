package ru.yandex.practicum.bank.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class NotificationServiceApplicationTest {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
        assertThat(environment.getProperty("spring.main.web-application-type"))
                .isEqualTo("servlet");
        assertThat(environment.getProperty("spring.kafka.bootstrap-servers"))
                .isEqualTo("localhost:9092");
        assertThat(environment.getProperty("spring.kafka.consumer.group-id"))
                .isEqualTo("bank-notification");
        assertThat(environment.getProperty("spring.kafka.consumer.enable-auto-commit"))
                .isEqualTo("false");
        assertThat(environment.getProperty("spring.kafka.consumer.auto-offset-reset"))
                .isEqualTo("earliest");
        assertThat(environment.getProperty("spring.kafka.listener.ack-mode"))
                .isEqualTo("record");
        assertThat(environment.getProperty("bank.kafka.notification-topic"))
                .isEqualTo("bank.notification");
        assertThat(environment.getProperty("bank.kafka.notification-dlt-topic"))
                .isEqualTo("bank.notification.dlt");
        assertThat(environment.getProperty("bank.kafka.notification-partitions"))
                .isEqualTo("3");
        assertThat(environment.getProperty("bank.kafka.notification-dlt-partitions"))
                .isEqualTo("3");
        assertThat(environment.getProperty("bank.kafka.notification-replication-factor"))
                .isEqualTo("1");
        assertThat(environment.getProperty("bank.kafka.notification-dlt-retention-ms"))
                .isEqualTo("604800000");
    }
}