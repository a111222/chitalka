package ru.chitalka.feature.library

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import ru.chitalka.core.designsystem.cover.BookCover
import ru.chitalka.core.designsystem.cover.CoverSize
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

/** Шаг между обложками карусели и порог свайпа из макета. */
private val Step = 108.dp
private val SwipeThreshold = 40.dp

/**
 * Состояние карусели: дробная позиция (0 — первая книга в центре, 1.5 — между второй и третьей).
 * Дробная, чтобы обложки ехали за пальцем, а не прыгали.
 */
@Stable
internal class CarouselState(initial: Float) {
    var position by mutableFloatStateOf(initial)

    /** Индекс книги в центре карусели из [count] книг. */
    fun currentOf(count: Int): Int = indexOf(position.roundToInt(), count)

    companion object {
        val Saver: Saver<CarouselState, Float> = Saver(save = { it.position.roundToInt().toFloat() }, restore = { CarouselState(it) })

        /** При трёх и больше книгах карусель закольцована, как в видео. */
        fun wraps(count: Int): Boolean = count >= 3

        fun indexOf(slot: Int, count: Int): Int {
            if (count == 0) return 0
            return if (wraps(count)) ((slot % count) + count) % count else slot.coerceIn(0, count - 1)
        }
    }

    /** Смещение i-й книги от центра в шагах, с учётом кольца: от −n/2 до n/2. */
    fun offsetOf(index: Int, count: Int): Float {
        val raw = index - position
        if (!wraps(count)) return raw
        return raw - count * floor((raw + count / 2f) / count)
    }
}

@Composable
internal fun rememberCarouselState(): CarouselState = rememberSaveable(saver = CarouselState.Saver) { CarouselState(0f) }

/**
 * Карусель обложек из видео: центральная крупно, соседние меньше и прячутся за ней. Свайп листает, тап по соседней подвозит её в центр,
 * тап по центральной открывает книгу.
 */
@Composable
internal fun CoverCarousel(
    books: List<CarouselBook>,
    state: CarouselState,
    onOpen: (CarouselBook) -> Unit,
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val haptics = ReaderTheme.haptics
    val animate = ReaderTheme.animationsEnabled
    val stepPx = with(LocalDensity.current) { Step.toPx() }
    val thresholdPx = with(LocalDensity.current) { SwipeThreshold.toPx() }
    val count = books.size
    val wraps = CarouselState.wraps(count)
    var dragStart by remember { mutableFloatStateOf(0f) }

    fun settleTo(target: Float) {
        scope.launch {
            val clamped = if (wraps) target else target.coerceIn(0f, (count - 1).coerceAtLeast(0).toFloat())
            val before = state.currentOf(count)
            if (animate) {
                animate(state.position, clamped, animationSpec = tween(MotionTokens.CAROUSEL_MS, easing = MotionTokens.Emphasized)) { value, _ ->
                    state.position = value
                }
            } else {
                state.position = clamped
            }
            if (state.currentOf(count) != before) haptics.shelfSnap()
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(250.dp)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    val next = state.position - delta / stepPx
                    state.position = if (wraps) next else next.coerceIn(-0.3f, count - 0.7f)
                },
                onDragStarted = { dragStart = state.position },
                onDragStopped = { velocity ->
                    val moved = state.position - dragStart
                    val base = dragStart.roundToInt().toFloat()
                    val target = when {
                        abs(moved) >= 0.5f -> state.position.roundToInt().toFloat()
                        abs(moved * stepPx) > thresholdPx || abs(velocity) > 800f ->
                            base + if (moved != 0f) sign(moved) else -sign(velocity)
                        else -> base
                    }
                    settleTo(target)
                },
            ),
    ) {
        background()
        books.forEachIndexed { index, book ->
            val offset = state.offsetOf(index, count)
            val distance = abs(offset)
            if (distance > 2.6f) return@forEachIndexed
            key(book.id) {
                CarouselCover(book, offset, stepPx) {
                    if (distance < 0.5f) {
                        haptics.bookPicked()
                        onOpen(book)
                    } else {
                        settleTo(state.position + offset)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CarouselCover(book: CarouselBook, offset: Float, stepPx: Float, onTap: () -> Unit) {
    val distance = abs(offset)
    val near = min(distance, 1f)
    val interaction = remember { MutableInteractionSource() }
    BookCover(
        title = book.title,
        author = book.author,
        coverPath = book.coverPath,
        size = CoverSize.Hero,
        artOverride = book.art,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .zIndex(10f - distance)
            .graphicsLayer {
                // Как в видео: соседние обложки меньше и прячутся за центральной, без поворота
                translationX = offset * stepPx
                translationY = near * 12.dp.toPx()
                val scale = when {
                    distance <= 1f -> 1f - 0.16f * distance
                    distance <= 2f -> 0.84f - 0.14f * (distance - 1f)
                    else -> 0.7f
                }
                scaleX = scale
                scaleY = scale
                alpha = when {
                    distance <= 1f -> 1f
                    distance <= 2f -> 1f - 0.3f * (distance - 1f)
                    else -> (0.7f * (2.6f - distance) / 0.6f).coerceAtLeast(0f)
                }
                shadowElevation = (26f - 14f * near) * density
                shape = RoundedCornerShape(8.dp)
                clip = false
                ambientShadowColor = Color.Black
                spotShadowColor = Color.Black
            }
            .size(150.dp, 222.dp)
            .clip(RoundedCornerShape(8.dp))
            .bookSpine()
            .semantics { contentDescription = if (distance < 0.5f) "Открыть «${book.title}»" else "Показать «${book.title}»" }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onTap),
    )
}

/** Корешок твёрдой обложки: светлая полоска и тень у левого края, чтобы обложка выглядела книгой. */
internal fun Modifier.bookSpine(): Modifier = drawWithContent {
    drawContent()
    val spine = 7.dp.toPx()
    drawRect(
        Brush.horizontalGradient(
            0f to Color.White.copy(alpha = 0.28f),
            0.6f to Color.White.copy(alpha = 0.08f),
            1f to Color.Black.copy(alpha = 0.3f),
            endX = spine,
        ),
        size = Size(spine, size.height),
    )
    drawRect(Color.Black.copy(alpha = 0.18f), topLeft = Offset(spine, 0f), size = Size(1.dp.toPx(), size.height))
}
