package ru.chitalka.feature.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.chitalka.core.designsystem.adaptive.LocalWindowWidth
import ru.chitalka.core.designsystem.adaptive.ReadingColumnMaxWidth
import ru.chitalka.core.designsystem.adaptive.WindowWidth
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.designsystem.typography.ReaderFonts

@Composable
internal fun ReaderRoute(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReaderScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/**
 * Экран чтения. Правая треть — вперёд, левая — назад (как в макете).
 * На планшете вместо одной страницы — разворот из двух.
 * 3D-листание с изгибом листа (PageCurl на AGSL) придёт на этапе «Премиум-эффекты».
 */
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onEvent: (ReaderEvent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics
    val animate = ReaderTheme.animationsEnabled
    val isSpread = LocalWindowWidth.current == WindowWidth.Expanded
    // В развороте листаем сразу по две страницы
    val step = if (isSpread) 2 else 1
    val firstVisible = if (isSpread) state.currentPage - state.currentPage % 2 else state.currentPage

    val turn: (forward: Boolean) -> Unit = { forward ->
        val canTurn = if (forward) firstVisible + step <= state.pages.lastIndex else firstVisible > 0
        if (canTurn) {
            haptics.pageTurn()
            repeat(step) { onEvent(if (forward) ReaderEvent.NextPage else ReaderEvent.PreviousPage) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .safeDrawingPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад к шкафу", tint = colors.inkSecondary)
            }
            Text(
                state.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(firstVisible, step, state.pages.size) {
                    detectTapGestures { offset ->
                        when {
                            offset.x > size.width * 2 / 3 -> turn(true)
                            offset.x < size.width / 3 -> turn(false)
                        }
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedContent(
                targetState = firstVisible,
                transitionSpec = {
                    if (!animate) {
                        // E-Ink: мгновенная смена страниц, без промежуточных кадров
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        val dir = if (targetState > initialState) 1 else -1
                        val spec = tween<IntOffset>(MotionTokens.PAGE_SETTLE_MS, easing = MotionTokens.PageSettle)
                        slideInHorizontally(spec) { it * dir } togetherWith slideOutHorizontally(spec) { -it * dir / 3 }
                    }
                },
                label = "page",
            ) { first ->
                if (isSpread) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                        Page(state.pages.getOrNull(first), state, Modifier.weight(1f))
                        Page(state.pages.getOrNull(first + 1), state, Modifier.weight(1f))
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Page(state.pages.getOrNull(first), state, Modifier.widthIn(max = ReadingColumnMaxWidth))
                    }
                }
            }
        }

        Text(
            text = if (state.pageCount > 0) "${state.currentPage + 1} из ${state.pageCount}" else "",
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun Page(text: String?, state: ReaderUiState, modifier: Modifier = Modifier) {
    val settings = state.settings
    Box(modifier.fillMaxHeight().padding(horizontal = 24.dp, vertical = 8.dp)) {
        if (text != null) {
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = ReaderFonts.familyFor(settings.font),
                    fontSize = settings.fontSizeSp.sp,
                    lineHeight = settings.lineSpacing.multiplier.em,
                    textAlign = TextAlign.Justify,
                    color = ReaderTheme.colors.ink,
                ),
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReaderScreenPreview() {
    ReaderTheme {
        ReaderScreen(
            state = ReaderUiState(title = SampleBook.TITLE, pages = SampleBook.pages, isLoading = false),
            onEvent = {},
            onBack = {},
        )
    }
}

