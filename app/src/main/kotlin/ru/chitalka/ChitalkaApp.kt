package ru.chitalka

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ru.chitalka.core.designsystem.component.FloatingNavBar
import ru.chitalka.core.designsystem.component.NavItem
import ru.chitalka.core.designsystem.component.glass
import ru.chitalka.core.designsystem.component.glassSource
import ru.chitalka.core.designsystem.component.rememberGlassState
import ru.chitalka.core.designsystem.icon.ChitalkaIcons
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.feature.library.SplashScreen
import ru.chitalka.feature.library.navigation.HomeRoute
import ru.chitalka.feature.library.navigation.LibraryRoute
import ru.chitalka.feature.reader.navigation.navigateToReader
import ru.chitalka.navigation.BookmarksRoute
import ru.chitalka.navigation.ChitalkaNavHost
import ru.chitalka.navigation.StatsRoute
import kotlin.reflect.KClass

/** Четыре вкладки нижней панели из видео. */
private enum class TopLevelDestination(val item: NavItem, val route: Any, val routeClass: KClass<*>) {
    Home(NavItem(ChitalkaIcons.Home, "Главная"), HomeRoute, HomeRoute::class),
    Library(NavItem(ChitalkaIcons.Library, "Библиотека"), LibraryRoute(), LibraryRoute::class),
    Bookmarks(NavItem(ChitalkaIcons.Bookmark, "Закладки"), BookmarksRoute, BookmarksRoute::class),
    Stats(NavItem(ChitalkaIcons.Stats, "Статистика"), StatsRoute, StatsRoute::class),
}

private fun NavDestination?.topLevelIndex(): Int =
    TopLevelDestination.entries.indexOfFirst { tab -> this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true }

/** Переход на вкладку как в любом приложении с вкладками: не копим экраны в стеке. */
private fun NavHostController.navigateToTab(route: Any, restore: Boolean = true) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = restore
}

/**
 * Каркас приложения: заставка при запуске, затем экраны и плавающая стеклянная панель
 * поверх них. В читалке и настройках панель уезжает вниз.
 */
@Composable
fun ChitalkaApp(showSplash: Boolean = false, openBookRequests: Flow<Long> = emptyFlow()) {
    val navController = rememberNavController()
    var splashDone by rememberSaveable { mutableStateOf(!showSplash) }
    // Книга из другого приложения: заставку пропускаем, а переходим, когда граф навигации уже создан
    var pendingBook by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(openBookRequests) {
        openBookRequests.collect { bookId ->
            splashDone = true
            pendingBook = bookId
        }
    }

    if (!splashDone) {
        SplashScreen(onFinished = { splashDone = true })
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val selected = backStackEntry?.destination.topLevelIndex()
    val glassState = rememberGlassState()
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics

    Box(Modifier.fillMaxSize().background(colors.background)) {
        ChitalkaNavHost(
            navController = navController,
            onOpenShelf = { filter -> navController.navigateToTab(LibraryRoute(filter.name), restore = false) },
            onSearch = { navController.navigateToTab(LibraryRoute()) },
            modifier = Modifier.glassSource(glassState),
        )
        LaunchedEffect(pendingBook) {
            pendingBook?.let { bookId ->
                pendingBook = null
                navController.navigateToReader(bookId)
            }
        }
        AnimatedVisibility(
            visible = selected >= 0,
            enter = fadeIn(tween(MotionTokens.RISE_MS)) +
                slideInVertically(tween(MotionTokens.RISE_MS, delayMillis = 250, easing = MotionTokens.Emphasized)) { it },
            exit = fadeOut(tween(MotionTokens.NAV_MS)) + slideOutVertically(tween(MotionTokens.NAV_MS)) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 22.dp)
                .widthIn(max = 480.dp),
        ) {
            FloatingNavBar(
                items = TopLevelDestination.entries.map { it.item },
                selectedIndex = selected,
                onSelect = { index ->
                    if (index != selected) {
                        haptics.toggle(true)
                        navController.navigateToTab(TopLevelDestination.entries[index].route)
                    }
                },
                glass = Modifier.glass(glassState, tint = colors.glass, background = colors.background),
            )
        }
    }
}
