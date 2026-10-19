package ru.chitalka.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.chitalka.feature.library.ShelfRoute

/** Маршрут экрана шкафа. Обычный класс: опечатку в маршруте поймает компилятор. */
@Serializable
data object LibraryRoute

fun NavGraphBuilder.libraryScreen(onOpenBook: (bookId: Long) -> Unit) {
    composable<LibraryRoute> {
        ShelfRoute(onOpenBook = onOpenBook)
    }
}
