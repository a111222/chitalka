package ru.chitalka.feature.stats.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.chitalka.feature.stats.StatsDestination

/** Вкладка «Статистика». */
@Serializable
data object StatsRoute

fun NavGraphBuilder.statsScreen() {
    composable<StatsRoute> { StatsDestination() }
}
