plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.cloud.contract)
}

contracts {
    baseClassForTests.set("ru.yandex.practicum.bank.cash.contract.CashContractBase")
    baseClassMappings {
        baseClassMapping(
            ".*messaging.*",
            "ru.yandex.practicum.bank.cash.contract.CashNotificationMessagingContractBase",
        )
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.aop)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.kafka)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.micrometer.tracing.bridge.brave)
    implementation(libs.zipkin.reporter.brave)
    runtimeOnly(libs.micrometer.registry.prometheus)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.cloud.starter.contract.verifier)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.kafka.test)
    implementation(libs.spring.integration.core)
}