package ru.chitalka.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.chitalka.feature.settings.SettingsRoute as SettingsScreenRoute

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen(onBack: () -> Unit) {
    composable<SettingsRoute> {
        SettingsScreenRoute(onBack = onBack)
    }
}
