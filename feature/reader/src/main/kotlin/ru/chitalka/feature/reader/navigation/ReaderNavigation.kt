package ru.chitalka.feature.reader.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.chitalka.feature.reader.ReaderRoute as ReaderScreenRoute

@Serializable
data class ReaderRoute(val bookId: Long)

fun NavController.navigateToReader(bookId: Long) = navigate(ReaderRoute(bookId))

fun NavGraphBuilder.readerScreen(onBack: () -> Unit) {
    composable<ReaderRoute> {
        ReaderScreenRoute(onBack = onBack)
    }
}
