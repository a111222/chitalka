plugins {
    alias(libs.plugins.chitalka.android.feature)
}

android {
    namespace = "ru.chitalka.feature.library"
}

dependencies {
    // Системное окно выбора файлов (Storage Access Framework): разрешение на память не нужно
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
}
