pluginManagement {
    // Наши convention plugins: общая настройка всех модулей в одном месте
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Chitalka"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":feature:library")
include(":feature:reader")
include(":feature:settings")
include(":feature:bookmarks")
include(":feature:stats")

include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:bookparser")
include(":core:designsystem")
include(":core:testing")
