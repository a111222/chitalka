package ru.chitalka.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/** Длительности и кривые анимаций из макета v3. */
object MotionTokens {
    /** Основная кривая: быстрый старт и долгое мягкое торможение (.2, 0, 0, 1). */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Разлёт обложек на заставке (.4, 0, .2, 1). */
    val Standard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** Заставка: обложки разлетаются, корешок разворачивается, текст рассыпается. */
    const val SPLASH_FLY_MS = 900
    const val SPLASH_FLY_STAGGER_MS = 35
    const val SPLASH_SPINE_MS = 1250
    const val SPLASH_BURST_MS = 1100
    const val SPLASH_DUST_MS = 700
    const val SPLASH_TOTAL_MS = 1300

    /** Стена обложек медленно плывёт вверх и обратно. */
    const val WALL_DRIFT_MS = 18_000

    /** Карусель: обложка доезжает до центра. */
    const val CAROUSEL_MS = 560

    /** Фон за каруселью перетекает в цвета новой книги. */
    const val BACKDROP_MS = 600

    /** Появление блоков на экране снизу вверх с волной задержек. */
    const val RISE_MS = 600
    const val RISE_STAGGER_MS = 70

    /** Подсветка вкладки в нижней панели. */
    const val NAV_MS = 300

    /** Листание: лист доводится за 560 мс; отпустили после 25% хода — страница переворачивается. */
    const val PAGE_SETTLE_MS = 560
    val PageSettle: Easing = CubicBezierEasing(0.3f, 0.6f, 0.2f, 1f)
    const val PAGE_TURN_THRESHOLD = 0.25f

    /** Верхняя и нижняя панели читалки. */
    const val PANELS_MS = 300

    /** Шторка оформления. */
    const val SHEET_MS = 450
    val Sheet: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}
