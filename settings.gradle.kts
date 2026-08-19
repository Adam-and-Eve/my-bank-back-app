pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "my-bank-back-app"

include("front-ui-service")
include("account-service")
include("cash-service")
include("transfer-service")
include("notification-service")
include("api-gateway")
include("config-server")
include("discovery-server")
include("shared")
include("exchange-service")
include("exchange-generator-service")
include("blocker-service")