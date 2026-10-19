package ru.chitalka.feature.bookmarks.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.chitalka.core.model.ReadingPosition
import ru.chitalka.feature.bookmarks.BookmarksDestination

/** Вкладка «Закладки»: цитаты и закладки из всех книг. */
@Serializable
data object BookmarksRoute

fun NavGraphBuilder.bookmarksScreen(onOpenPlace: (bookId: Long, position: ReadingPosition) -> Unit) {
    composable<BookmarksRoute> {
        BookmarksDestination(onOpenPlace = onOpenPlace)
    }
}
