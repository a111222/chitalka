plugins {
    alias(libs.plugins.chitalka.android.library)
    alias(libs.plugins.chitalka.android.room)
    alias(libs.plugins.chitalka.hilt)
}

android {
    namespace = "ru.chitalka.core.database"
}

dependencies {
    api(projects.core.model)
}
