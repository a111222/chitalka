plugins {
    alias(libs.plugins.chitalka.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
}
