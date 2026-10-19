plugins {
    alias(libs.plugins.chitalka.android.compose)
}

android {
    namespace = "ru.chitalka.core.designsystem"
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
}
