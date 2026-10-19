plugins {
    alias(libs.plugins.chitalka.android.application)
    alias(libs.plugins.chitalka.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ru.chitalka"

    defaultConfig {
        applicationId = "ru.chitalka"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Сжатие кода включим перед бетой в Google Play (этап 6)
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(projects.feature.library)
    implementation(projects.feature.reader)
    implementation(projects.feature.settings)
    implementation(projects.feature.bookmarks)
    implementation(projects.feature.stats)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsCore)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
