import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.cloud.contract) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

group = "ru.yandex.practicum.my-bank.back-app"
version = "0.0.1-SNAPSHOT"

val springBootVersion = libs.versions.spring.boot.get()
val springCloudVersion = libs.versions.spring.cloud.get()
val springBootStarterTest = libs.spring.boot.starter.test

subprojects {
    pluginManager.apply("java")
    pluginManager.apply("org.springframework.boot")
    pluginManager.apply("io.spring.dependency-management")

    group = rootProject.group
    version = rootProject.version

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    extensions.configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        }
    }

    dependencies {
        "testImplementation"(springBootStarterTest)
    }
}