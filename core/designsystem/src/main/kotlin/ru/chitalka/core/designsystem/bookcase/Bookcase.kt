package ru.chitalka.core.designsystem.bookcase

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderColors
import ru.chitalka.core.designsystem.theme.ReaderTheme

/**
 * Размеры резного шкафа из макета v2. Книги расставляет фича, а шкаф
 * подсказывает, где у какой полки «пол»: [shelfFloor].
 */
object BookcaseMetrics {
    /** Карниз: профиль, зубчики, бусины, фриз и резной гребень. */
    val Crown: Dp = 52.dp

    /** Цоколь внизу. */
    val Plinth: Dp = 26.dp

    /** Толщина полки. */
    val Plank: Dp = 14.dp

    /** Ширина боковых пилястр. */
    val Frame: Dp = 14.dp

    /** Отступ первой книги от пилястры. */
    val ShelfInset: Dp = 10.dp

    /** Расстояние от полки до полки. */
    val ShelfPitch: Dp = 168.dp

    /** Шире шкаф не становится: на планшете он стоит по центру, а не тянется на весь экран. */
    val MaxWidth: Dp = 720.dp

    /** Зазор между корешками. */
    val SpineGap: Dp = 2.dp

    fun height(shelfCount: Int, pitch: Dp = ShelfPitch): Dp = Crown + pitch * shelfCount + Plinth

    /** Где стоят книги полки [index]: координата верха доски от верха шкафа. */
    fun shelfFloor(index: Int, pitch: Dp = ShelfPitch): Dp = Crown + pitch * (index + 1) - Plank

    /** Сколько места для книг на одной полке. */
    fun shelfWidth(caseWidth: Dp): Dp = caseWidth - (Frame + ShelfInset) * 2

    /** Левый край первой книги. */
    val ShelfStart: Dp get() = Frame + ShelfInset
}

/**
 * Резной шкаф из макета v2: карниз с зубчиками и бусинами, гребень с резьбой,
 * пилястры с каннелюрами, задняя стенка из досок, полки и цоколь. Под каждой полкой
 * светодиод бросает тёплый конус света на книги ниже.
 *
 * Всё рисуется векторно на Canvas, поэтому шкаф чёткий на любой плотности экрана.
 * Слои: стенка и свет → содержимое (книги) → полки, пилястры и карниз поверх.
 * В E-Ink шкаф становится гравюрой: только контуры, без теней и свечения.
 *
 * @param lightsOn подсветка полок; разгорается и гаснет плавно, как настоящая лампа
 * @param content книги; ставьте их через Modifier.offset по [BookcaseMetrics.shelfFloor]
 */
@Composable
fun CarvedBookcase(
    shelfCount: Int,
    modifier: Modifier = Modifier,
    lightsOn: Boolean = true,
    shelfPitch: Dp = BookcaseMetrics.ShelfPitch,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = ReaderTheme.colors
    val glow by animateFloatAsState(
        targetValue = if (lightsOn && !colors.isEInk) 1f else 0f,
        animationSpec = if (colors.isEInk) snap() else tween(MotionTokens.SHELF_GLOW_MS, easing = MotionTokens.Emphasized),
        label = "shelfGlow",
    )
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .height(BookcaseMetrics.height(shelfCount, shelfPitch))
            .then(
                if (colors.isEInk) Modifier
                else Modifier.shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black),
            )
            .clip(shape)
            .drawWithContent {
                val pitch = shelfPitch.toPx()
                drawCaseBody(colors)
                drawBackWall(colors)
                repeat(shelfCount) { drawShelfLight(colors, it, pitch, glow) }
                drawContent()
                repeat(shelfCount) { drawPlank(colors, it, shelfCount, pitch, glow) }
                drawPilasters(colors)
                drawCrown(colors)
                drawPlinth(colors)
            },
        content = content,
    )
}

private fun DrawScope.drawCaseBody(colors: ReaderColors) {
    if (colors.isEInk) {
        drawRect(colors.wood)
        drawRect(colors.line, style = Stroke(2.dp.toPx()))
        return
    }
    drawRect(
        Brush.verticalGradient(
            0f to colors.woodHighlight,
            0.12f to colors.wood,
            1f to colors.woodShadow,
        ),
    )
}

/** Задняя стенка: вертикальные доски и тень от карниза и пилястр. */
private fun DrawScope.drawBackWall(colors: ReaderColors) {
    val frame = BookcaseMetrics.Frame.toPx()
    val top = BookcaseMetrics.Crown.toPx()
    val bottom = size.height - BookcaseMetrics.Plinth.toPx()
    val left = frame
    val right = size.width - frame
    val wallSize = Size(right - left, bottom - top)
    val sh = colors.shadowStrength
    drawRect(
        brush = Brush.verticalGradient(listOf(colors.wallTop, colors.wall), startY = top, endY = bottom),
        topLeft = Offset(left, top),
        size = wallSize,
    )
    // Стыки досок через каждые 26 dp
    val boardStep = 26.dp.toPx()
    val seam = Color.Black.copy(alpha = if (colors.isEInk) 0.12f else 0.10f)
    var x = left
    while (x < right) {
        drawLine(seam, Offset(x, top), Offset(x, bottom), strokeWidth = 1.dp.toPx())
        x += boardStep
    }
    if (colors.isEInk) {
        drawRect(colors.line, topLeft = Offset(left, top), size = wallSize, style = Stroke(1.dp.toPx()))
        return
    }
    // Внутренние тени: стенка утоплена вглубь шкафа
    val shadowDepth = 18.dp.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = sh + 0.1f), Color.Transparent),
            startY = top,
            endY = top + shadowDepth,
        ),
        topLeft = Offset(left, top),
        size = Size(wallSize.width, shadowDepth),
    )
    val sideDepth = 16.dp.toPx()
    val side = Color.Black.copy(alpha = sh * 0.6f)
    drawRect(
        brush = Brush.horizontalGradient(listOf(side, Color.Transparent), startX = left, endX = left + sideDepth),
        topLeft = Offset(left, top),
        size = Size(sideDepth, wallSize.height),
    )
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color.Transparent, side), startX = right - sideDepth, endX = right),
        topLeft = Offset(right - sideDepth, top),
        size = Size(sideDepth, wallSize.height),
    )
}

/** Конус света от светодиода: эллипс 72 % × 95 % отсека с центром у верхнего края. */
private fun DrawScope.drawShelfLight(colors: ReaderColors, index: Int, pitch: Float, glow: Float) {
    if (glow <= 0f) return
    val frame = BookcaseMetrics.Frame.toPx()
    val top = BookcaseMetrics.Crown.toPx() + index * pitch
    val height = pitch - BookcaseMetrics.Plank.toPx()
    val left = frame
    val right = size.width - frame
    val width = right - left
    val cx = left + width / 2
    val rx = width * 0.72f
    val ry = height * 0.95f
    clipRect(left, top, right, top + height) {
        withTransform({ scale(1f, ry / rx, pivot = Offset(cx, top)) }) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to colors.lampGlow.copy(alpha = colors.lampGlow.alpha * glow),
                    0.72f to Color.Transparent,
                    center = Offset(cx, top),
                    radius = rx,
                ),
                radius = rx,
                center = Offset(cx, top),
            )
        }
    }
}

/** Полка с бликом на кромке и тенью вниз; под ней — полоска светодиода. */
private fun DrawScope.drawPlank(colors: ReaderColors, index: Int, shelfCount: Int, pitch: Float, glow: Float) {
    val plank = BookcaseMetrics.Plank.toPx()
    val top = BookcaseMetrics.Crown.toPx() + pitch * (index + 1) - plank
    val left = 10.dp.toPx()
    val width = size.width - 2 * left
    if (colors.isEInk) {
        drawRect(colors.wood, topLeft = Offset(left, top), size = Size(width, plank))
        val stroke = 2.dp.toPx()
        drawLine(colors.line, Offset(left, top + stroke / 2), Offset(left + width, top + stroke / 2), stroke)
        drawLine(colors.line, Offset(left, top + plank - stroke / 2), Offset(left + width, top + plank - stroke / 2), stroke)
        return
    }
    val shadowDepth = 12.dp.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = colors.shadowStrength), Color.Transparent),
            startY = top + plank,
            endY = top + plank + shadowDepth,
        ),
        topLeft = Offset(left, top + plank),
        size = Size(width, shadowDepth),
    )
    val edge = 3.dp.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            0f to colors.woodHighlight,
            edge / plank to colors.plank,
            1f to colors.woodShadow,
            startY = top,
            endY = top + plank,
        ),
        topLeft = Offset(left, top),
        size = Size(width, plank),
    )
    // Светодиод крепится к нижней стороне доски и светит на полку ниже
    if (glow > 0f && index + 1 < shelfCount) drawLed(colors, top + plank, glow)
    // Самый верхний светодиод — под карнизом
    if (glow > 0f && index == 0) drawLed(colors, BookcaseMetrics.Crown.toPx(), glow)
}

private fun DrawScope.drawLed(colors: ReaderColors, y: Float, glow: Float) {
    val inset = (BookcaseMetrics.Frame + 10.dp).toPx()
    val halo = 10.dp.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            listOf(colors.shelfGlow.copy(alpha = 0.55f * glow), Color.Transparent),
            startY = y,
            endY = y + halo,
        ),
        topLeft = Offset(inset - halo, y),
        size = Size(size.width - 2 * inset + 2 * halo, halo),
    )
    drawLine(
        color = colors.shelfGlow.copy(alpha = glow),
        start = Offset(inset, y + 1.dp.toPx()),
        end = Offset(size.width - inset, y + 1.dp.toPx()),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

/** Пилястры с каннелюрами: желобок, блик, плоская грань — шаг 7 dp. */
private fun DrawScope.drawPilasters(colors: ReaderColors) {
    val frame = BookcaseMetrics.Frame.toPx()
    listOf(0f, size.width - frame).forEach { left ->
        if (colors.isEInk) {
            var x = left
            while (x < left + frame) {
                drawLine(colors.line, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                x += 4.dp.toPx()
            }
            drawRect(colors.line, Offset(left, 0f), Size(frame, size.height), style = Stroke(2.dp.toPx()))
            return@forEach
        }
        drawRect(colors.wood, Offset(left, 0f), Size(frame, size.height))
        val step = 7.dp.toPx()
        var x = left
        while (x < left + frame) {
            drawRect(colors.woodShadow, Offset(x, 0f), Size(2.dp.toPx(), size.height))
            drawRect(colors.woodHighlight, Offset(x + 2.dp.toPx(), 0f), Size(1.dp.toPx(), size.height))
            x += step
        }
        drawRect(Color.Black.copy(alpha = 0.4f), Offset(left, 0f), Size(frame, size.height), style = Stroke(1.dp.toPx()))
    }
}

/** Карниз: профиль, зубчики, бусины, фриз и резной гребень по центру. */
private fun DrawScope.drawCrown(colors: ReaderColors) {
    val frame = BookcaseMetrics.Frame.toPx()
    val innerWidth = size.width - 2 * frame
    val eInk = colors.isEInk
    // Профиль карниза
    val corniceHeight = 11.dp.toPx()
    if (eInk) {
        drawRect(colors.wood, size = Size(size.width, corniceHeight))
        drawLine(colors.line, Offset(0f, corniceHeight), Offset(size.width, corniceHeight), 2.dp.toPx())
    } else {
        drawRect(Brush.verticalGradient(listOf(colors.woodHighlight, colors.wood), endY = corniceHeight), size = Size(size.width, corniceHeight))
        drawRect(Color.Black.copy(alpha = 0.35f), Offset(0f, corniceHeight), Size(size.width, 2.dp.toPx()))
    }
    // Зубчики: 6 dp выступ, 4 dp зазор
    val dentilTop = 15.dp.toPx()
    val dentilHeight = 8.dp.toPx()
    val dentil = 6.dp.toPx()
    val dentilStep = 10.dp.toPx()
    if (!eInk) drawRect(colors.woodShadow, Offset(frame, dentilTop), Size(innerWidth, dentilHeight))
    var x = frame
    while (x < frame + innerWidth) {
        val w = minOf(dentil, frame + innerWidth - x)
        drawRect(if (eInk) colors.line else colors.woodHighlight, Offset(x, dentilTop), Size(w, dentilHeight))
        x += dentilStep
    }
    if (!eInk) drawRect(Color.Black.copy(alpha = 0.4f), Offset(frame, dentilTop + dentilHeight), Size(innerWidth, 2.dp.toPx()))
    // Бусины
    val beadY = 31.dp.toPx()
    val beadStep = 8.dp.toPx()
    val beadRadius = if (eInk) 1.4.dp.toPx() else 1.6.dp.toPx()
    x = frame + beadStep / 2
    while (x < frame + innerWidth) {
        drawCircle(if (eInk) colors.line else colors.woodHighlight, beadRadius, Offset(x, beadY))
        x += beadStep
    }
    // Фриз
    val friezeTop = 40.dp.toPx()
    val friezeHeight = 12.dp.toPx()
    if (eInk) {
        drawLine(colors.line, Offset(frame, friezeTop), Offset(frame + innerWidth, friezeTop), 1.dp.toPx())
        drawLine(colors.line, Offset(frame, friezeTop + friezeHeight), Offset(frame + innerWidth, friezeTop + friezeHeight), 1.dp.toPx())
    } else {
        drawRect(
            Brush.verticalGradient(
                0f to colors.woodShadow,
                0.4f to colors.wood,
                1f to colors.woodShadow,
                startY = friezeTop,
                endY = friezeTop + friezeHeight,
            ),
            Offset(frame, friezeTop),
            Size(innerWidth, friezeHeight),
        )
    }
    drawCrest(colors)
}

/** Гребень 160 × 52 dp: веер-пальметта с завитками. Резьба — светлая линия поверх тени со сдвигом. */
private fun DrawScope.drawCrest(colors: ReaderColors) {
    val unit = 1.dp.toPx()
    translate(left = size.width / 2 - 80 * unit) {
        withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
            drawPath(CrestShield, colors.wood)
            drawPath(CrestShield, if (colors.isEInk) colors.line else colors.carveShadow, style = Stroke(1.2f))
            if (!colors.isEInk) {
                translate(0.8f, 1.4f) {
                    drawPath(CrestCarving, colors.carveShadow, style = carveStroke(2.6f))
                }
            }
            drawPath(CrestCarving, colors.carve, style = carveStroke(1.6f))
        }
    }
}

private fun carveStroke(width: Float) = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawPlinth(colors: ReaderColors) {
    val height = BookcaseMetrics.Plinth.toPx()
    val top = size.height - height
    if (colors.isEInk) {
        drawRect(colors.wood, Offset(0f, top), Size(size.width, height))
        drawLine(colors.line, Offset(0f, top), Offset(size.width, top), 2.dp.toPx())
        return
    }
    drawRect(
        Brush.verticalGradient(
            0f to colors.woodHighlight,
            3.dp.toPx() / height to colors.wood,
            1f to colors.woodShadow,
            startY = top,
            endY = size.height,
        ),
        Offset(0f, top),
        Size(size.width, height),
    )
    drawRect(Color.Black.copy(alpha = 0.3f), Offset(0f, top), Size(size.width, 3.dp.toPx()))
}

private fun svgPath(data: String): Path = PathParser().parsePathString(data).toPath()

/** Щит гребня в координатах 160 × 52. */
private val CrestShield: Path by lazy { svgPath("M36 52 C38 30 58 10 80 7 C102 10 122 30 124 52 Z") }

/** Линии резьбы: лепесток, веер прожилок, два завитка и два усика. */
private val CrestCarving: Path by lazy {
    svgPath(
        "M80 47 C70 39 68 25 80 13 C92 25 90 39 80 47 Z " +
            "M80 45 L80 17 M80 45 L73 21 M80 45 L87 21 M80 45 L69 29 M80 45 L91 29 " +
            "M66 46 C54 46 50 36 40 36 C31 36 28 43 33 47 C37 50 42 46 38 42 " +
            "M94 46 C106 46 110 36 120 36 C129 36 132 43 127 47 C123 50 118 46 122 42 " +
            "M60 41 C54 32 46 28 38 29 M100 41 C106 32 114 28 122 29",
    )
}
