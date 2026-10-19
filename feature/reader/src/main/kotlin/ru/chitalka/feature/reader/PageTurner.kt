package ru.chitalka.feature.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.model.ReadingPosition
import kotlin.math.abs

/** Что видно сейчас: глава, разрезанная на страницы, и первая видимая страница. */
internal data class Spread(val paged: PagedChapter, val first: Int)

/**
 * Лист в движении. [over] лежит сверху и поворачивается на петле у левого края, [under] — под ним.
 * null вместо листа значит «то, что сейчас показано»: так листаем в соседнюю главу, которой ещё нет на экране.
 * [expected] — страница, которая должна оказаться на экране после перелистывания.
 */
internal data class PageTurn(
    val under: Spread?,
    val over: Spread?,
    val forward: Boolean,
    val expected: Spread? = null,
    val committed: Boolean = false,
)

/** Угол листа, когда он стоит ребром и уже не виден. */
internal const val EDGE_ANGLE = 90f

/**
 * 3D-листание из видео: страница поворачивается вокруг левого края, как лист книги.
 * Вперёд — текущий лист уходит, открывая следующий; назад — предыдущий лист возвращается сверху.
 * При свайпе лист идёт за пальцем; отпустили после 25% хода — страница переворачивается, иначе ложится обратно.
 */
@Stable
internal class PageTurner(private val scope: CoroutineScope) {
    var turn by mutableStateOf<PageTurn?>(null)
        private set

    /** Угол верхнего листа: 0 — лежит, [EDGE_ANGLE] — ушёл. */
    val angle = Animatable(0f)

    // Обновляются на каждой композиции экрана
    var visible: Spread? = null
    var chapterCount: Int = 0
    var step: Int = 1
    var animate: Boolean = true
    var onEvent: (ReaderEvent) -> Unit = {}
    var onTurned: () -> Unit = {}

    private var dragged = 0f
    private var dragFallback: Boolean? = null
    private var dragOwned = false

    private val spec get() = tween<Float>(MotionTokens.PAGE_SETTLE_MS, easing = MotionTokens.PageSettle)

    /** Тап по краю страницы. */
    fun tap(forward: Boolean) {
        if (turn != null) return
        val spread = visible ?: return
        val target = targetPosition(spread, forward, step, chapterCount) ?: return
        val neighbour = neighbourOf(spread, forward)
        onTurned()
        if (!animate) {
            onEvent(ReaderEvent.GoTo(target))
            return
        }
        scope.launch {
            if (forward) {
                turn = PageTurn(under = neighbour, over = spread, forward = true, expected = neighbour)
                angle.snapTo(0f)
                if (neighbour == null) onEvent(ReaderEvent.GoTo(target))
                angle.animateTo(EDGE_ANGLE, spec)
            } else {
                turn = PageTurn(under = spread, over = neighbour, forward = false, expected = neighbour)
                angle.snapTo(EDGE_ANGLE)
                if (neighbour == null) onEvent(ReaderEvent.GoTo(target))
                angle.animateTo(0f, spec)
            }
            if (neighbour != null) onEvent(ReaderEvent.GoTo(target))
            turn = turn?.copy(committed = true)
        }
    }

    fun dragStart() {
        dragged = 0f
        dragFallback = null
        dragOwned = false
    }

    fun drag(dx: Float, width: Float) {
        dragged += dx
        // Пока доигрывает перелистывание тапом, свайп его не перехватывает
        if (!animate || width <= 0f || (turn != null && !dragOwned)) return
        if (turn == null && dragFallback == null && abs(dragged) > 12f) {
            val spread = visible ?: return
            val forward = dragged < 0
            val neighbour = neighbourOf(spread, forward)
            if (neighbour == null) {
                // Соседняя страница в другой главе: листаем по отпусканию, как тапом
                dragFallback = forward
                return
            }
            turn = if (forward) {
                PageTurn(under = neighbour, over = spread, forward = true, expected = neighbour)
            } else {
                PageTurn(under = spread, over = neighbour, forward = false, expected = neighbour)
            }
            dragOwned = true
        }
        val active = turn?.takeIf { dragOwned } ?: return
        val progress = (if (active.forward) -dragged / width else dragged / width).coerceIn(0f, 1f)
        scope.launch { angle.snapTo(if (active.forward) EDGE_ANGLE * progress else EDGE_ANGLE * (1f - progress)) }
    }

    fun dragEnd(width: Float, velocity: Float = 0f) {
        val active = turn?.takeIf { dragOwned }
        dragOwned = false
        if (active == null) {
            // Лист за пальцем не шёл: соседняя глава или выключенные анимации — листаем по направлению свайпа
            val forward = dragFallback ?: (dragged < 0)
            dragFallback = null
            if (turn == null && abs(dragged) > width * 0.1f) tap(forward)
            return
        }
        val progress = if (active.forward) angle.value / EDGE_ANGLE else 1f - angle.value / EDGE_ANGLE
        val fling = if (active.forward) velocity < -800f else velocity > 800f
        val complete = progress >= MotionTokens.PAGE_TURN_THRESHOLD || fling
        val spread = visible
        scope.launch {
            if (complete && spread != null) {
                angle.animateTo(if (active.forward) EDGE_ANGLE else 0f, spec)
                targetPosition(spread, active.forward, step, chapterCount)?.let {
                    onTurned()
                    onEvent(ReaderEvent.GoTo(it))
                }
                turn = active.copy(committed = true)
            } else {
                angle.animateTo(if (active.forward) 0f else EDGE_ANGLE, spec)
                turn = null
            }
        }
    }

    /** Новая страница на экране: перелистывание закончено, верхний лист больше не нужен. */
    fun onShown(shown: Spread?) {
        val current = turn ?: return
        if (current.committed && (current.expected == null || current.expected == shown)) turn = null
    }

    /** Страхуемся: если экран так и не показал ожидаемую страницу (например, поменялся шрифт), убираем лист. */
    fun forceSettle() {
        if (turn?.committed == true) turn = null
    }

    /** Соседняя страница в этой же главе или null, если она в другой. */
    private fun neighbourOf(spread: Spread, forward: Boolean): Spread? {
        val next = if (forward) spread.first + step else spread.first - step
        return if (next >= 0 && next <= spread.paged.pages.lastIndex) spread.copy(first = next) else null
    }
}

/** Куда листать: соседняя страница этой главы или граница соседней главы. null — дальше некуда. */
internal fun targetPosition(spread: Spread, forward: Boolean, step: Int, chapterCount: Int): ReadingPosition? {
    val chapter = spread.paged.chapterIndex
    val pages = spread.paged.pages
    return if (forward) {
        when {
            spread.first + step <= pages.lastIndex -> ReadingPosition(chapter, pages[spread.first + step].startOffset)
            chapter + 1 < chapterCount -> ReadingPosition(chapter + 1, 0)
            else -> null
        }
    } else {
        when {
            spread.first > 0 -> ReadingPosition(chapter, pages[(spread.first - step).coerceAtLeast(0)].startOffset)
            chapter > 0 -> ReadingPosition(chapter - 1, END_OF_CHAPTER)
            else -> null
        }
    }
}
