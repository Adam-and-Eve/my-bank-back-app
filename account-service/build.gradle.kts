plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.cloud.contract)
}

contracts {
    baseClassForTests.set("ru.yandex.practicum.bank.account.contract.AccountContractBase")
    baseClassMappings {
        baseClassMapping(
            ".*messaging.*",
            "ru.yandex.practicum.bank.account.contract.AccountNotificationMessagingContractBase"
        )
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.aop)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.retry)
    implementation(libs.spring.kafka)

    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.datasource.micrometer.spring.boot)
    runtimeOnly(libs.micrometer.registry.prometheus)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.micrometer.tracing.bridge.brave)
    implementation(libs.zipkin.reporter.brave)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.cloud.starter.contract.verifier)
    testImplementation(libs.spring.integration.core)
    testImplementation(libs.spring.security.test)
    testRuntimeOnly(libs.h2)
    testImplementation(libs.spring.kafka.test)
}