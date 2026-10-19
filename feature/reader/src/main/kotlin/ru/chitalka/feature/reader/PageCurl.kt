package ru.chitalka.feature.reader

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Сгиб листа, как в Apple Books: лист берут за уголок [corner] и тянут к точке [finger].
 * Линия сгиба — серединный перпендикуляр к отрезку «уголок — палец». Часть листа за сгибом
 * (со стороны уголка) переворачивается: зеркально отражается относительно линии сгиба и ложится
 * поверх оставшейся части, показывая оборот. Так ведёт себя настоящая бумага.
 *
 * Лист занимает полосу от корешка [spineX] шириной [pageWidth] (в координатах области страниц).
 * Палец ограничен, чтобы лист не «отрывался» от корешка: уголок не может уйти от корешка
 * дальше ширины листа. Геометрия по мотивам библиотеки oleksandrbalan/pagecurl (Apache 2.0).
 */
internal class Fold(
    val spineX: Float,
    val pageWidth: Float,
    val height: Float,
    val corner: Offset,
    finger: Offset,
) {
    /** Палец после ограничений. */
    val finger: Offset = constrain(finger)

    /** Середина отрезка «уголок — палец»: через неё проходит сгиб. */
    val mid: Offset = (corner + this.finger) / 2f

    /** Единичная нормаль к сгибу, смотрит в сторону уголка. */
    val normal: Offset

    /** Насколько глубоко загнут лист: расстояние от сгиба до уголка. */
    val depth: Float

    /** Часть листа, которая осталась лежать (со стороны корешка). */
    val remaining: List<Offset>

    /** Отогнутая часть листа на своём исходном месте: здесь теперь видна следующая страница. */
    val lifted: List<Offset>

    /** Оборот листа: отогнутая часть после отражения через сгиб. */
    val flap: List<Offset>

    init {
        val d = corner - this.finger
        val length = d.getDistance()
        normal = if (length > 0.5f) d / length else Offset(1f, 0f)
        depth = length / 2f
        val sheet = listOf(
            Offset(spineX, 0f),
            Offset(spineX + pageWidth, 0f),
            Offset(spineX + pageWidth, height),
            Offset(spineX, height),
        )
        if (length <= 0.5f) {
            remaining = sheet
            lifted = emptyList()
        } else {
            remaining = clipHalfPlane(sheet, keepCornerSide = false)
            lifted = clipHalfPlane(sheet, keepCornerSide = true)
        }
        flap = lifted.map(::reflect)
    }

    private fun side(p: Offset): Float = (p - mid).dot(normal)

    fun reflect(p: Offset): Offset = p - normal * (2f * side(p))

    /**
     * Матрица отражения через сгиб для рисования в локальных координатах страницы,
     * чей левый верхний угол стоит в точке [origin] области страниц.
     */
    fun reflection(origin: Offset): Matrix {
        val m = mid - origin
        val a = 1f - 2f * normal.x * normal.x
        val b = -2f * normal.x * normal.y
        val e = 1f - 2f * normal.y * normal.y
        val k = 2f * m.dot(normal)
        return Matrix(
            floatArrayOf(
                a, b, 0f, 0f,
                b, e, 0f, 0f,
                0f, 0f, 1f, 0f,
                normal.x * k, normal.y * k, 0f, 1f,
            ),
        )
    }

    /** Отражение через корешок: так оборот листа становится следующей страницей разворота. */
    fun spineMirror(origin: Offset): Matrix {
        val s = spineX - origin.x
        return Matrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                2f * s, 0f, 0f, 1f,
            ),
        )
    }

    private fun constrain(p: Offset): Offset {
        var q = p
        // Уголок не дальше ширины листа от корешка на своей высоте...
        val near = Offset(spineX, corner.y)
        q = limit(q, near, pageWidth)
        // ...и не дальше диагонали листа от противоположного угла у корешка
        val far = Offset(spineX, height - corner.y)
        q = limit(q, far, sqrt(pageWidth * pageWidth + height * height))
        return q
    }

    private fun limit(p: Offset, centre: Offset, radius: Float): Offset {
        val v = p - centre
        val dist = v.getDistance()
        return if (dist > radius && dist > 0f) centre + v * (radius / dist) else p
    }

    /** Отсекает многоугольник полуплоскостью сгиба (алгоритм Сазерленда — Ходжмена). */
    private fun clipHalfPlane(poly: List<Offset>, keepCornerSide: Boolean): List<Offset> {
        val out = ArrayList<Offset>(poly.size + 2)
        fun inside(p: Offset) = if (keepCornerSide) side(p) > 0f else side(p) <= 0f
        for (i in poly.indices) {
            val cur = poly[i]
            val prev = poly[(i + poly.size - 1) % poly.size]
            val curIn = inside(cur)
            val prevIn = inside(prev)
            if (curIn != prevIn) {
                val sp = side(prev)
                val t = sp / (sp - side(cur))
                out += prev + (cur - prev) * t
            }
            if (curIn) out += cur
        }
        return out
    }
}

private fun Offset.dot(other: Offset): Float = x * other.x + y * other.y

private fun List<Offset>.toPath(origin: Offset): Path = Path().apply {
    forEachIndexed { i, p -> if (i == 0) moveTo(p.x - origin.x, p.y - origin.y) else lineTo(p.x - origin.x, p.y - origin.y) }
    close()
}

/** Что из листа рисует этот слой. */
internal enum class CurlPart {
    /** Одностраничный режим: лежащая часть листа и его оборот с еле заметным зеркальным текстом. */
    FrontWithBack,

    /** Разворот: только лежащая часть правой страницы, оборот рисует следующая левая страница. */
    FrontOnly,

    /** Разворот: следующая левая страница видна на обороте переворачиваемого листа. */
    BackOnly,
}

/**
 * Рисует страницу как часть сгибаемого листа. [origin] — где на области страниц стоит левый верхний угол
 * этой страницы. [backPaper] — цвет оборота листа (его полупрозрачность даёт просвечивающий текст).
 * Пока [fold] возвращает null, страница рисуется как обычно (а оборот в развороте не рисуется вовсе).
 */
internal fun Modifier.pageCurl(
    fold: () -> Fold?,
    origin: Offset,
    part: CurlPart,
    backPaper: Color,
): Modifier = drawWithContent {
    val f = fold()
    if (f == null) {
        if (part != CurlPart.BackOnly) drawContent()
        return@drawWithContent
    }
    when (part) {
        CurlPart.FrontWithBack -> {
            drawFront(f, origin)
            drawBack(f, origin) {
                withTransform({ transform(f.reflection(origin)) }) { this@drawWithContent.drawContent() }
                drawRect(backPaper)
            }
        }
        CurlPart.FrontOnly -> drawFront(f, origin)
        CurlPart.BackOnly -> drawBack(f, origin) {
            withTransform({
                transform(f.reflection(origin))
                transform(f.spineMirror(origin))
            }) { this@drawWithContent.drawContent() }
            // Оборот листа чуть светлее страницы под ним, как бумага на просвет
            drawRect(Color.White.copy(alpha = 0.035f))
        }
    }
}

/** Лежащая часть листа и тень, которую поднятый лист бросает на открывшуюся страницу. */
private fun ContentDrawScope.drawFront(f: Fold, origin: Offset) {
    if (f.lifted.size >= 3) {
        val shadow = min(f.depth * 0.6f, 46f * density)
        clipPath(f.lifted.toPath(origin)) {
            drawRect(
                Brush.linearGradient(
                    0f to Color.Black.copy(alpha = 0.42f),
                    1f to Color.Transparent,
                    start = f.mid - origin,
                    end = f.mid - origin + f.normal * shadow,
                ),
            )
        }
    }
    if (f.remaining.size >= 3) {
        clipPath(f.remaining.toPath(origin)) { this@drawFront.drawContent() }
    }
}

/**
 * Оборот листа: мягкая тень вокруг него, само содержимое ([content]) и объём —
 * у сгиба бумага темнее, к краю светлеет, как на изогнутом листе.
 */
private inline fun ContentDrawScope.drawBack(f: Fold, origin: Offset, content: ContentDrawScope.() -> Unit) {
    if (f.flap.size < 3) return
    val path = f.flap.toPath(origin)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // Тень под оборотом; аппаратное ускорение рисует setShadowLayer только с Android 9
        val radius = 14f * density
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.TRANSPARENT
            setShadowLayer(radius, -f.normal.x * radius * 0.4f, -f.normal.y * radius * 0.4f, Color.Black.copy(alpha = 0.45f).toArgb())
        }
        drawIntoCanvas { it.nativeCanvas.drawPath(path.asAndroidPath(), paint) }
    }
    clipPath(path) {
        content()
        val start = f.mid - origin
        val end = start - f.normal * f.depth
        drawRect(
            Brush.linearGradient(
                0f to Color.Black.copy(alpha = 0.30f),
                0.18f to Color.Black.copy(alpha = 0.10f),
                0.55f to Color.White.copy(alpha = 0.05f),
                1f to Color.Black.copy(alpha = 0.06f),
                start = start,
                end = end,
            ),
        )
    }
}
