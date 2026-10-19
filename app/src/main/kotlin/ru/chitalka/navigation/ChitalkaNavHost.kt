package ru.chitalka.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import ru.chitalka.core.designsystem.motion.LocalSharedTransitionScope
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.feature.bookmarks.navigation.bookmarksScreen
import ru.chitalka.feature.library.LibraryFilter
import ru.chitalka.feature.library.navigation.HomeRoute
import ru.chitalka.feature.library.navigation.bookDetailScreen
import ru.chitalka.feature.library.navigation.homeScreen
import ru.chitalka.feature.library.navigation.libraryScreen
import ru.chitalka.feature.reader.navigation.navigateToReader
import ru.chitalka.feature.reader.navigation.readerScreen
import ru.chitalka.feature.settings.navigation.SettingsRoute
import ru.chitalka.feature.settings.navigation.settingsScreen
import ru.chitalka.feature.stats.navigation.statsScreen

/**
 * Все переходы между экранами собраны здесь, в app: фичи друг о друге не знают.
 * SharedTransitionLayout вокруг навигации позволяет обложке перелетать между экранами.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ChitalkaNavHost(
    navController: NavHostController,
    onOpenShelf: (LibraryFilter) -> Unit,
    onSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SharedTransitionLayout(modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            ChitalkaGraph(navController, onOpenShelf, onSearch, onOpenBookmarks)
        }
    }
}

@Composable
private fun ChitalkaGraph(
    navController: NavHostController,
    onOpenShelf: (LibraryFilter) -> Unit,
    onSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        // Вкладки сменяются мягким проявлением; свои анимации экраны добавляют сами
        enterTransition = { fadeIn(tween(MotionTokens.NAV_MS)) },
        exitTransition = { fadeOut(tween(MotionTokens.NAV_MS)) },
        popEnterTransition = { fadeIn(tween(MotionTokens.NAV_MS)) },
        popExitTransition = { fadeOut(tween(MotionTokens.NAV_MS)) },
    ) {
        homeScreen(
            onOpenBook = { navController.navigate(it) },
            onOpenShelf = onOpenShelf,
            onSearch = onSearch,
            onOpenSettings = { navController.navigate(SettingsRoute) },
        )
        libraryScreen(onOpenBook = { navController.navigate(it) })
        bookDetailScreen(
            onBack = { navController.popBackStack() },
            onRead = navController::navigateToReader,
            onOpenBookmarks = onOpenBookmarks,
            onOpenSettings = { navController.navigate(SettingsRoute) },
        )
        bookmarksScreen(onOpenPlace = { bookId, position -> navController.navigateToReader(bookId, position) })
        statsScreen()
        readerScreen(onBack = { navController.popBackStack() })
        settingsScreen(onBack = { navController.popBackStack() })
    }
}
