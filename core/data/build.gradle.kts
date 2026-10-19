plugins {
    alias(libs.plugins.chitalka.android.library)
    alias(libs.plugins.chitalka.hilt)
}

android {
    namespace = "ru.chitalka.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
}
