package ru.chitalka.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Матовое стекло. Экран под панелью помечается [glassSource], сама панель — [glass]:
 * она рисует размытую копию того, что под ней, и поверх полупрозрачный тон.
 * Android 12+ размывает через RenderEffect, Android 8–11 — через RenderScript.
 */
@Composable
fun rememberGlassState(): HazeState = rememberHazeState(blurEnabled = true)

fun Modifier.glassSource(state: HazeState): Modifier = hazeSource(state)

fun Modifier.glass(
    state: HazeState,
    tint: Color,
    background: Color,
    blurRadius: Dp = 16.dp,
): Modifier = hazeEffect(state) {
    backgroundColor = background
    tints = listOf(HazeTint(tint))
    this.blurRadius = blurRadius
    noiseFactor = 0f
    blurEnabled = true
}
