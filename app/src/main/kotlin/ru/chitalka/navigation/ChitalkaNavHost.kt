package ru.chitalka.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import ru.chitalka.feature.library.navigation.LibraryRoute
import ru.chitalka.feature.library.navigation.libraryScreen
import ru.chitalka.feature.reader.navigation.navigateToReader
import ru.chitalka.feature.reader.navigation.readerScreen
import ru.chitalka.feature.settings.navigation.settingsScreen

/** Все переходы между экранами собраны здесь, в app: фичи друг о друге не знают. */
@Composable
fun ChitalkaNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = LibraryRoute) {
        libraryScreen(onOpenBook = navController::navigateToReader)
        readerScreen(onBack = { navController.popBackStack() })
        settingsScreen()
    }
}
