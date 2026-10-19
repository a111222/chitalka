package ru.chitalka.feature.reader

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.random.Random

/** Фактура бумаги: мелкое зерно, крупные «облака» волокон. Считается один раз на процесс. */
internal class PaperGrain private constructor(val grain: ImageBitmap, val mottle: ImageBitmap) {
    companion object {
        val instance: PaperGrain by lazy { PaperGrain(noise(GRAIN_SIZE, seed = 7), noise(MOTTLE_SIZE, seed = 11)) }

        private const val GRAIN_SIZE = 160
        private const val MOTTLE_SIZE = 24

        /** Серый шум: каждый пиксель — белый или чёрный со случайной прозрачностью. */
        private fun noise(size: Int, seed: Int): ImageBitmap {
            val random = Random(seed)
            val pixels = IntArray(size * size) {
                val v = random.nextFloat()
                val alpha = (kotlin.math.abs(v - 0.5f) * 2f * 255).toInt().coerceIn(0, 255)
                val rgb = if (v > 0.5f) 0xFFFFFF else 0x000000
                (alpha shl 24) or rgb
            }
            return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
    }
}

/** Сколько видно обреза книги: слева — прочитанные страницы, справа — оставшиеся. Доли от 0 до 1. */
internal data class PageEdges(val left: Float, val right: Float) {
    companion object {
        val None = PageEdges(0f, 0f)
    }
}

/**
 * Бумага страницы: цвет [paper], зерно и волокна, лёгкая тень к краям листа и обрез книги —
 * тонкие полоски торцов страниц по бокам, толще с той стороны, где страниц больше.
 */
@Composable
internal fun Modifier.paper(paper: Color, edges: PageEdges): Modifier {
    val grain = remember { PaperGrain.instance }
    val grainBrush = remember(grain) { ShaderBrush(ImageShader(grain.grain, TileMode.Repeated, TileMode.Repeated)) }
    return drawBehind {
        drawRect(paper)
        // Волокна: крупный шум, растянутый с размытием, даёт неровный тон, как у настоящей бумаги
        drawImage(
            grain.mottle,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(grain.mottle.width, grain.mottle.height),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            alpha = 0.12f,
            filterQuality = FilterQuality.High,
        )
        drawRect(grainBrush, alpha = 0.09f)
        // Чуть темнее к верху и низу листа: страница не идеально плоская
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.16f),
                0.12f to Color.Transparent,
                0.88f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.18f),
            ),
        )
        if (edges.left > 0f) drawEdges(paper, edges.left, rightSide = false)
        if (edges.right > 0f) drawEdges(paper, edges.right, rightSide = true)
    }
}

/** Обрез: стопка тонких торцов страниц, каждый следующий чуть темнее, у листа — мягкая тень. */
private fun DrawScope.drawEdges(paper: Color, share: Float, rightSide: Boolean) {
    val strip = 1.4.dp.toPx()
    val count = ceil(3 + share * 8).toInt()
    val width = strip * count
    val light = lerp(paper, Color.White, 0.18f)
    val dark = lerp(paper, Color.Black, 0.35f)
    for (i in 0 until count) {
        // i = 0 — самый внешний торец
        val x = if (rightSide) size.width - strip * (i + 1) else strip * i
        val t = i.toFloat() / count
        drawRect(
            color = if (i % 2 == 0) lerp(dark, light, t) else lerp(dark, paper, t),
            topLeft = Offset(x, strip * (count - i) * 0.6f),
            size = Size(strip, size.height - strip * (count - i) * 1.2f),
        )
    }
    // Тень, которую стопка бросает на лист
    val edge = if (rightSide) size.width - width else width
    val fall = 14.dp.toPx()
    drawRect(
        Brush.horizontalGradient(
            0f to Color.Black.copy(alpha = 0.38f),
            1f to Color.Transparent,
            startX = edge,
            endX = if (rightSide) edge - fall else edge + fall,
        ),
        topLeft = Offset(if (rightSide) edge - fall else edge, 0f),
        size = Size(fall, size.height),
    )
}
