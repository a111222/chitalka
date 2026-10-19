package ru.chitalka.feature.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.model.ReadingPosition
import kotlin.math.abs

/** Что видно сейчас: глава, разрезанная на страницы, и первая видимая страница (в развороте — левая). */
internal data class Spread(val paged: PagedChapter, val first: Int) {
    val position: ReadingPosition get() = ReadingPosition(paged.chapterIndex, paged.pages[first].startOffset)
}

/**
 * Перелистывание вперёд с [from] на [to]: лист [from] (в развороте — его правая страница) сгибается
 * и переворачивается. Назад листаем тем же движением наоборот: [from] — предыдущая страница,
 * её лист возвращается и ложится поверх [to].
 * [cornerY] — за какую высоту правого края взялись: низ, верх или середина (тогда сгиб вертикальный).
 */
internal data class PageTurn(
    val from: Spread,
    val to: Spread,
    val forward: Boolean,
    val cornerY: Float,
    /** Сгиб вертикальный: тянули за середину края, лист не перекашивается. */
    val straight: Boolean = false,
    val committed: Boolean = false,
) {
    /** Какая страница окажется на экране, когда лист ляжет. */
    val expected: Spread get() = if (forward) to else from
}

/**
 * Листание как в Apple Books: страницу берут за уголок, она изгибается за пальцем и переворачивается,
 * показывая оборот. Тап по краю — лист сам переворачивается от нижнего уголка.
 * Отпустили после четверти хода (или резко смахнули) — страница переворачивается, иначе ложится обратно.
 */
@Stable
internal class PageTurner(private val scope: CoroutineScope) {
    var turn by mutableStateOf<PageTurn?>(null)
        private set

    /** Где сейчас уголок листа (в координатах области страниц). */
    val finger = Animatable(Offset.Zero, Offset.VectorConverter)

    // Обновляются на каждой композиции экрана
    var visible: Spread? = null
    var area: Size = Size.Zero
    var spineX: Float = 0f
    var pageWidth: Float = 0f
    var animate: Boolean = true
    var onEvent: (ReaderEvent) -> Unit = {}
    var onTurned: () -> Unit = {}

    /** Соседний лист: следующая/предыдущая страница или разворот, в том числе в соседней главе. null — его ещё нет. */
    var neighbourOf: (Spread, Boolean) -> Spread? = { _, _ -> null }

    /** Куда перейти, если соседний лист ещё не разложен (соседняя глава грузится): без анимации. */
    var fallbackTarget: (Spread, Boolean) -> ReadingPosition? = { _, _ -> null }

    private var dragStart = Offset.Zero
    private var dragAt = Offset.Zero
    private var dragOwned = false

    /** Сгиб для рисования: читается в фазе рисования, поэтому кадры идут без перекомпозиции. */
    fun fold(): Fold? {
        val t = turn ?: return null
        val corner = Offset(spineX + pageWidth, t.cornerY)
        val f = finger.value
        return Fold(spineX, pageWidth, area.height, corner, if (t.straight) Offset(f.x, t.cornerY) else f)
    }

    private fun corner(cornerY: Float) = Offset(spineX + pageWidth, cornerY)

    /** Положение уголка, когда лист перевёрнут целиком: зеркально через корешок. */
    private fun turned(cornerY: Float) = Offset(spineX - pageWidth, cornerY)

    /** Тап по краю страницы: лист переворачивается от нижнего уголка по дуге. */
    fun tap(forward: Boolean) {
        if (turn != null) return
        val spread = visible ?: return
        val neighbour = neighbourOf(spread, forward)
        if (neighbour == null) {
            fallbackTarget(spread, forward)?.let {
                onTurned()
                onEvent(ReaderEvent.GoTo(it))
            }
            return
        }
        onTurned()
        if (!animate || area.height <= 0f) {
            onEvent(ReaderEvent.GoTo(neighbour.position))
            return
        }
        val cornerY = area.height * 0.985f
        val start = corner(cornerY)
        val end = turned(cornerY)
        // Уголок сначала приподнимается к середине страницы, потом уходит за корешок
        val lift = Offset(spineX + pageWidth * 0.42f, area.height * 0.80f)
        scope.launch {
            if (forward) {
                turn = PageTurn(from = spread, to = neighbour, forward = true, cornerY = cornerY)
                finger.snapTo(start)
                finger.animateTo(end, arc(start, lift, end, liftAt = 0.38f))
            } else {
                turn = PageTurn(from = neighbour, to = spread, forward = false, cornerY = cornerY)
                finger.snapTo(end)
                finger.animateTo(start, arc(end, lift, start, liftAt = 0.62f))
            }
            commit()
        }
    }

    fun dragStart(at: Offset) {
        dragStart = at
        dragAt = at
        dragOwned = false
    }

    fun drag(at: Offset) {
        dragAt = at
        // Пока доигрывает перелистывание тапом, свайп его не перехватывает
        if (!animate || area.height <= 0f || (turn != null && !dragOwned)) return
        val moved = at - dragStart
        if (turn == null) {
            if (abs(moved.x) < 12f) return
            val spread = visible ?: return
            val forward = moved.x < 0
            val neighbour = neighbourOf(spread, forward) ?: return
            val third = area.height / 3f
            val (cornerY, straight) = when {
                dragStart.y < third -> 0f to false
                dragStart.y > third * 2 -> area.height to false
                else -> dragStart.y to true
            }
            turn = if (forward) {
                PageTurn(from = spread, to = neighbour, forward = true, cornerY = cornerY, straight = straight)
            } else {
                PageTurn(from = neighbour, to = spread, forward = false, cornerY = cornerY, straight = straight)
            }
            dragOwned = true
        }
        val active = turn?.takeIf { dragOwned } ?: return
        // Вперёд уголок идёт за пальцем; назад лист появляется из-за корешка и проходит вдвое больший путь
        val target = if (active.forward) {
            corner(active.cornerY) + moved
        } else {
            turned(active.cornerY) + Offset(moved.x * 2f, moved.y)
        }
        scope.launch { finger.snapTo(target) }
    }

    fun dragEnd(velocityX: Float) {
        val active = turn?.takeIf { dragOwned }
        dragOwned = false
        if (active == null) {
            // Лист за пальцем не шёл (анимации выключены или соседняя глава ещё грузится): листаем по направлению
            val dx = dragAt.x - dragStart.x
            if (turn == null && abs(dx) > area.width * 0.1f) tap(dx < 0)
            return
        }
        val travel = 2f * pageWidth
        val turnedShare = ((spineX + pageWidth - finger.value.x) / travel).coerceIn(0f, 1f)
        val corner = corner(active.cornerY)
        val turned = turned(active.cornerY)
        scope.launch {
            val settle = tween<Offset>(MotionTokens.PAGE_SETTLE_MS, easing = MotionTokens.PageSettle)
            if (active.forward) {
                if (turnedShare >= MotionTokens.PAGE_TURN_THRESHOLD || velocityX < -800f) {
                    finger.animateTo(turned, settle)
                    commit()
                } else {
                    finger.animateTo(corner, settle)
                    turn = null
                }
            } else {
                if (turnedShare <= 1f - MotionTokens.PAGE_TURN_THRESHOLD || velocityX > 800f) {
                    finger.animateTo(corner, settle)
                    commit()
                } else {
                    finger.animateTo(turned, settle)
                    turn = null
                }
            }
        }
    }

    /** Лист лёг: переходим на новую страницу и ждём, пока экран её покажет (см. [onShown]). */
    private fun commit() {
        val active = turn ?: return
        onEvent(ReaderEvent.GoTo(active.expected.position))
        turn = active.copy(committed = true)
    }

    /** Экран показал страницу: если это та, на которую листали, лист больше не нужен. */
    fun onShown(shown: Spread?) {
        val current = turn ?: return
        if (current.committed && current.expected == shown) turn = null
    }

    /** Страхуемся: если экран так и не показал ожидаемую страницу (например, поменялся шрифт), убираем лист. */
    fun forceSettle() {
        if (turn?.committed == true) turn = null
    }

    private fun arc(start: Offset, lift: Offset, end: Offset, liftAt: Float): AnimationSpec<Offset> = keyframes {
        durationMillis = MotionTokens.PAGE_CURL_MS
        start at 0 using LinearOutSlowInEasing
        lift at (MotionTokens.PAGE_CURL_MS * liftAt).toInt() using LinearOutSlowInEasing
        end at MotionTokens.PAGE_CURL_MS
    }
}

/** Куда листать без анимации, если соседний лист ещё не разложен: граница соседней главы. null — дальше некуда. */
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

/**
 * Соседний лист: в этой главе или, на её границе, первая/последняя страница соседней главы
 * из [chapters] (уже разложенные главы по номеру). В развороте первая страница всегда чётная.
 */
internal fun neighbourSpread(spread: Spread, forward: Boolean, step: Int, chapters: Map<Int, PagedChapter>): Spread? {
    val pages = spread.paged.pages
    val chapter = spread.paged.chapterIndex
    return if (forward) {
        if (spread.first + step <= pages.lastIndex) {
            spread.copy(first = spread.first + step)
        } else {
            chapters[chapter + 1]?.let { Spread(it, 0) }
        }
    } else {
        if (spread.first > 0) {
            spread.copy(first = (spread.first - step).coerceAtLeast(0))
        } else {
            chapters[chapter - 1]?.let { prev ->
                val last = prev.pages.lastIndex
                Spread(prev, if (step == 2) last - last % 2 else last)
            }
        }
    }
}
