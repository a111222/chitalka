package ru.chitalka

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.feature.library.navigation.LibraryRoute
import ru.chitalka.feature.reader.navigation.ReaderRoute
import ru.chitalka.feature.reader.navigation.navigateToReader
import ru.chitalka.feature.settings.navigation.SettingsRoute
import ru.chitalka.navigation.ChitalkaNavHost
import kotlin.reflect.KClass

private enum class TopLevelDestination(val label: String, val icon: ImageVector, val route: Any, val routeClass: KClass<*>) {
    Library("Шкаф", Icons.Filled.Home, LibraryRoute, LibraryRoute::class),
    Settings("Оформление", Icons.Filled.Settings, SettingsRoute, SettingsRoute::class),
}

/**
 * Каркас приложения. NavigationSuiteScaffold сам выбирает вид навигации по ширине окна:
 * на телефоне — нижняя панель, на планшете — боковая колонка. В режиме чтения навигация скрыта.
 */
@Composable
fun ChitalkaApp(openBookRequests: Flow<Long> = emptyFlow()) {
    val navController = rememberNavController()
    LaunchedEffect(openBookRequests) {
        openBookRequests.collect { bookId -> navController.navigateToReader(bookId) }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isReading = destination?.hasRoute(ReaderRoute::class) == true

    val colors = ReaderTheme.colors
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.paper,
            indicatorColor = colors.accent,
            unselectedIconColor = colors.inkSecondary,
            unselectedTextColor = colors.inkSecondary,
            selectedTextColor = colors.ink,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = colors.paper,
            indicatorColor = colors.accent,
            unselectedIconColor = colors.inkSecondary,
            unselectedTextColor = colors.inkSecondary,
            selectedTextColor = colors.ink,
        ),
    )

    NavigationSuiteScaffold(
        layoutType = if (isReading) {
            NavigationSuiteType.None
        } else {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
        },
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = colors.paper,
            navigationRailContainerColor = colors.paper,
        ),
        containerColor = colors.background,
        contentColor = colors.ink,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { item ->
                val selected = destination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
                item(
                    selected = selected,
                    onClick = {
                        navController.navigate(item.route) {
                            // Как в любом приложении с вкладками: не копим экраны в стеке
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.label) },
                    colors = itemColors,
                )
            }
        },
    ) {
        ChitalkaNavHost(navController)
    }
}
