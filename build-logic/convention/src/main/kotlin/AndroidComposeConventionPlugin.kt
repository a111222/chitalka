import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import ru.chitalka.buildlogic.configureAndroidCompose

/** Android-библиотека с Compose (например, core:designsystem). */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("chitalka.android.library")
        extensions.configure<LibraryExtension> {
            configureAndroidCompose(this)
        }
    }
}
