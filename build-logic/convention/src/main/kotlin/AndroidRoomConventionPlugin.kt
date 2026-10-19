import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import ru.chitalka.buildlogic.lib
import ru.chitalka.buildlogic.libs

/** Room + KSP. Схема базы сохраняется в schemas/, чтобы писать миграции. */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("androidx.room")
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }
        dependencies {
            add("implementation", libs.lib("room-runtime"))
            add("implementation", libs.lib("room-ktx"))
            add("ksp", libs.lib("room-compiler"))
        }
    }
}
