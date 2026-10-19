plugins {
    alias(libs.plugins.chitalka.android.library)
    alias(libs.plugins.chitalka.hilt)
}

android {
    namespace = "ru.chitalka.core.datastore"
}

dependencies {
    api(projects.core.model)
    api(libs.datastore.preferences)
}
