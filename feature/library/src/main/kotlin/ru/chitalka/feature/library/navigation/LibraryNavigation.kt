package ru.chitalka.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.chitalka.feature.library.HomeDestination
import ru.chitalka.feature.library.LibraryFilter
import ru.chitalka.feature.library.LibraryDestination

/** Главная: карусель, полки, ряды книг. Маршруты — обычные классы: опечатку поймает компилятор. */
@Serializable
data object HomeRoute

/** Вкладка «Библиотека». [filter] — имя [LibraryFilter], с которым её открыли с полки на главной. */
@Serializable
data class LibraryRoute(val filter: String? = null)

fun NavGraphBuilder.homeScreen(
    onOpenBook: (bookId: Long) -> Unit,
    onOpenShelf: (LibraryFilter) -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<HomeRoute> {
        HomeDestination(
            onOpenBook = onOpenBook,
            onOpenShelf = onOpenShelf,
            onSearch = onSearch,
            onOpenSettings = onOpenSettings,
        )
    }
}

fun NavGraphBuilder.libraryScreen(onOpenBook: (bookId: Long) -> Unit) {
    composable<LibraryRoute> { entry ->
        val filter = entry.toRoute<LibraryRoute>().filter
        LibraryDestination(
            initialFilter = LibraryFilter.entries.firstOrNull { it.name == filter },
            onOpenBook = onOpenBook,
        )
    }
}
