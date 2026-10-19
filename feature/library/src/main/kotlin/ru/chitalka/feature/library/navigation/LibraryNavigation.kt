package ru.chitalka.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.chitalka.core.model.Book
import ru.chitalka.core.designsystem.motion.ProvideNavAnimatedScope
import ru.chitalka.feature.library.HomeDestination
import ru.chitalka.feature.library.LibraryDestination
import ru.chitalka.feature.library.LibraryFilter
import ru.chitalka.feature.library.detail.BookDetailRoute as BookDetailScreenRoute
import ru.chitalka.feature.library.detail.BookPreview

/** Главная: карусель, полки, ряды книг. Маршруты — обычные классы: опечатку поймает компилятор. */
@Serializable
data object HomeRoute

/** Вкладка «Библиотека». [filter] — имя [LibraryFilter], с которым её открыли с полки на главной. */
@Serializable
data class LibraryRoute(val filter: String? = null)

/**
 * Карточка «О книге». Название, автор и обложка едут вместе с маршрутом:
 * карточка рисует книгу с первого кадра, и обложка перелетает из карусели без пустого места.
 */
@Serializable
data class BookDetailRoute(
    val bookId: Long,
    val title: String,
    val author: String? = null,
    val coverPath: String? = null,
)

fun NavGraphBuilder.homeScreen(
    onOpenBook: (BookDetailRoute) -> Unit,
    onOpenShelf: (LibraryFilter) -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<HomeRoute> {
        ProvideNavAnimatedScope {
            HomeDestination(
                onOpenBook = onOpenBook,
                onOpenShelf = onOpenShelf,
                onSearch = onSearch,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

fun NavGraphBuilder.libraryScreen(onOpenBook: (BookDetailRoute) -> Unit) {
    composable<LibraryRoute> { entry ->
        val filter = entry.toRoute<LibraryRoute>().filter
        ProvideNavAnimatedScope {
            LibraryDestination(
                initialFilter = LibraryFilter.entries.firstOrNull { it.name == filter },
                onOpenBook = onOpenBook,
            )
        }
    }
}

fun NavGraphBuilder.bookDetailScreen(
    onBack: () -> Unit,
    onRead: (bookId: Long) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<BookDetailRoute> { entry ->
        val route = entry.toRoute<BookDetailRoute>()
        ProvideNavAnimatedScope {
            BookDetailScreenRoute(
                preview = BookPreview(route.bookId, route.title, route.author, route.coverPath),
                onBack = onBack,
                onRead = onRead,
                onOpenBookmarks = onOpenBookmarks,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

internal fun Book.toDetailRoute() = BookDetailRoute(id, title, author, coverPath)
