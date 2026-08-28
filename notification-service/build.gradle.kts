plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.cloud.contract)
}

contracts {
    baseClassForTests.set("ru.yandex.practicum.bank.notification.contract.NotificationMessagingContractBase")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.json)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    implementation(libs.spring.kafka)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.cloud.starter.contract.verifier)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.spring.integration.core)
}