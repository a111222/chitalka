package ru.chitalka.feature.reader.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.chitalka.core.model.ReadingPosition
import ru.chitalka.feature.reader.ReaderRoute as ReaderScreenRoute

/** Читалка. [chapterIndex] и [charOffset] — открыть на этом месте (из закладок); -1 — где остановились. */
@Serializable
data class ReaderRoute(val bookId: Long, val chapterIndex: Int = -1, val charOffset: Int = 0)

fun NavController.navigateToReader(bookId: Long) = navigate(ReaderRoute(bookId))

fun NavController.navigateToReader(bookId: Long, position: ReadingPosition) =
    navigate(ReaderRoute(bookId, position.chapterIndex, position.charOffset))

fun NavGraphBuilder.readerScreen(onBack: () -> Unit) {
    composable<ReaderRoute> {
        ReaderScreenRoute(onBack = onBack)
    }
}
