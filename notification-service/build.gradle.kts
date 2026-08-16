plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.cloud.contract)
}

contracts {
    baseClassForTests.set("ru.yandex.practicum.bank.notification.contract.NotificationContractBase")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    implementation(libs.spring.cloud.starter.config)
    implementation(libs.spring.cloud.starter.netflix.eureka.client)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.cloud.starter.contract.verifier)
    testImplementation(libs.spring.security.test)
}