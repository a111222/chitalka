plugins {
    alias(libs.plugins.chitalka.android.library)
}

android {
    namespace = "ru.chitalka.core.bookparser"
}

dependencies {
    api(projects.core.model)
    implementation(libs.jsoup)
}
