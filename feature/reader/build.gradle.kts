plugins {
    alias(libs.plugins.chitalka.android.feature)
}

android {
    namespace = "ru.chitalka.feature.reader"
}

dependencies {
    // Системная кнопка «назад» закрывает шторку оформления
    implementation(libs.androidx.activity.compose)
}
