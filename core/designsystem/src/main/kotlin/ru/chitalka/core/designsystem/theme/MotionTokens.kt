package ru.chitalka.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/** Длительности и кривые анимаций из спецификации макета v2 («Движение и отклик»). */
object MotionTokens {
    /** Основная кривая Material 3 Expressive: (.2, 0, 0, 1). */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Морфинг телефон ⇄ планшет: 650 мс с небольшой волной задержек. */
    const val MORPH_MS = 650
    const val MORPH_STAGGER_MS = 40

    /** Листание: лист доводится за 560 мс; отпустили после 25% хода — страница переворачивается. */
    const val PAGE_SETTLE_MS = 560
    val PageSettle: Easing = CubicBezierEasing(0.3f, 0.6f, 0.2f, 1f)
    const val PAGE_TURN_THRESHOLD = 0.25f

    /** Открытие книги: подъём с полки, затем обложка распахивается. */
    const val BOOK_LIFT_MS = 320
    const val COVER_OPEN_MS = 950
    /** Книги расставляются по полкам волной: каждая следующая на 9 мс позже. */
    const val BOOK_STAGGER_MS = 9
    /** Новая книга опускается на полку сверху; при первом показе шкафа — волной слева направо. */
    const val BOOK_LAND_MS = 520
    const val BOOK_LAND_STAGGER_MS = 28
    val CoverOpen: Easing = CubicBezierEasing(0.45f, 0.05f, 0.2f, 1f)

    /** Закладка-ленточка падает с пружинящим перелётом. */
    const val BOOKMARK_MS = 500
    val BookmarkOvershoot: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    /** Подсветка полок разгорается как настоящая лампа. */
    const val SHELF_GLOW_MS = 700

    /** Верхняя и нижняя панели читалки. */
    const val PANELS_MS = 300

    /** Шторка оформления. */
    const val SHEET_MS = 450
    val Sheet: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** E-Ink вместо анимации на долю секунды «обновляет» экран. */
    const val EINK_REFRESH_MS = 300
}
