package ru.chitalka.core.designsystem.bookcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ru.chitalka.core.designsystem.theme.ReaderColors
import ru.chitalka.core.designsystem.theme.ReaderTheme

/**
 * Как выглядит корешок: толщина, высота и цвет ткани. Выбирается по id книги,
 * поэтому у каждой книги свой корешок, и он не меняется между запусками.
 */
@Immutable
data class SpineStyle(
    val width: Dp,
    /** Высота книги как доля шага полки. */
    val heightFraction: Float,
    val cloth: Color,
    /** Номер оттенка серого для E-Ink. */
    val inkShade: Int,
)

/** Стабильный стиль корешка для книги: одинаковый id — одинаковый корешок. */
fun spineStyleFor(seed: Long): SpineStyle {
    val hash = mix(seed)
    fun pick(shift: Int, size: Int): Int = ((hash ushr shift).toInt() and Int.MAX_VALUE) % size
    return SpineStyle(
        width = SpineWidths[pick(0, SpineWidths.size)].dp,
        heightFraction = SpineHeights[pick(12, SpineHeights.size)],
        cloth = SpineCloths[pick(24, SpineCloths.size)],
        inkShade = pick(40, EInkSpineFills.size),
    )
}

/**
 * Корешок книги на полке: переплётная ткань с объёмом, два золотых бинта сверху,
 * один снизу и название, вытисненное снизу вверх, как на русских книгах.
 *
 * @param pressed подсветить корешок, пока палец на нём
 * @param glowing тёплый ореол вокруг книги, которую читают сейчас
 */
@Composable
fun BookSpine(
    title: String,
    style: SpineStyle,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    glowing: Boolean = false,
) {
    val colors = ReaderTheme.colors
    val fill = clothFor(style, colors)
    val foil = foilFor(fill, colors)
    Box(
        modifier = modifier
            .drawBehind {
                if (colors.isEInk) return@drawBehind
                if (glowing) {
                    val center = Offset(size.width / 2, size.height * 0.35f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(colors.lampGlow, Color.Transparent),
                            center = center,
                            radius = size.height * 0.6f,
                        ),
                        radius = size.height * 0.6f,
                        center = center,
                    )
                }
                // Мягкая тень вправо на соседнюю книгу
                val sh = colors.shadowStrength * 0.9f
                drawRect(Color.Black.copy(alpha = sh * 0.6f), Offset(2.dp.toPx(), 0f), size)
                drawRect(Color.Black.copy(alpha = sh * 0.4f), Offset(3.5.dp.toPx(), 1.dp.toPx()), size)
            }
            .clip(SpineShape)
            .drawBehind {
                drawRect(fill)
                if (colors.isEInk) {
                    drawRect(colors.line, style = Stroke(1.5.dp.toPx()))
                } else {
                    // Объём: корешок скруглён, края уходят в тень
                    drawRect(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.38f),
                            0.28f to Color.White.copy(alpha = 0.14f),
                            0.55f to Color.White.copy(alpha = 0.04f),
                            1f to Color.Black.copy(alpha = 0.42f),
                        ),
                    )
                }
                val inset = 3.dp.toPx()
                val bandWidth = size.width - 2 * inset
                val topBand = size.height * 0.08f
                drawRect(foil.copy(alpha = 0.75f), Offset(inset, topBand), Size(bandWidth, 2.dp.toPx()))
                drawRect(foil.copy(alpha = 0.6f), Offset(inset, topBand + 5.dp.toPx()), Size(bandWidth, 1.dp.toPx()))
                drawRect(
                    foil.copy(alpha = 0.75f),
                    Offset(inset, size.height * 0.93f - 2.dp.toPx()),
                    Size(bandWidth, 2.dp.toPx()),
                )
                if (pressed) drawRect(Color.White.copy(alpha = 0.16f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxHeight(0.66f), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                modifier = Modifier.verticalText(),
                color = foil,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.03.em,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Обложка для книги без картинки: та же ткань, что и на корешке,
 * автор капителью, название и золотая черта, как в макете.
 */
@Composable
fun BookCoverPlaceholder(
    title: String,
    author: String?,
    style: SpineStyle,
    modifier: Modifier = Modifier,
) {
    val colors = ReaderTheme.colors
    val fill = clothFor(style, colors)
    val foil = foilFor(fill, colors)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .drawBehind {
                drawRect(fill)
                if (colors.isEInk) {
                    drawRect(colors.line, style = Stroke(1.5.dp.toPx()))
                } else {
                    drawRect(Color.Black.copy(alpha = 0.25f), size = Size(6.dp.toPx(), size.height))
                    drawRect(Color.Black.copy(alpha = 0.1f), style = Stroke(10.dp.toPx()))
                }
            }
            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = author.orEmpty().uppercase(),
            color = foil.copy(alpha = 0.85f),
            fontSize = 7.sp,
            letterSpacing = 0.16.em,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = title,
            color = foil,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 13.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(
            Modifier
                .size(width = 18.dp, height = 1.dp)
                .drawBehind { drawRect(foil) },
        )
    }
}

private val SpineShape = RoundedCornerShape(topStart = 2.dp, topEnd = 3.dp, bottomEnd = 3.dp, bottomStart = 2.dp)

private fun clothFor(style: SpineStyle, colors: ReaderColors): Color =
    if (colors.isEInk) EInkSpineFills[style.inkShade] else style.cloth

/** На светлой ткани тиснение тёмное, иначе его не прочесть. */
private fun foilFor(fill: Color, colors: ReaderColors): Color = when {
    colors.isEInk -> if (fill.luminance() > 0.3f) colors.ink else colors.paper
    fill.luminance() > 0.35f -> DarkFoil
    else -> colors.spineFoil
}

/** SplitMix64: соседние id дают непохожие корешки. */
private fun mix(seed: Long): Long {
    var z = seed + -0x61c8864680b583ebL
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
    return z xor (z ushr 31)
}

private val DarkFoil = Color(0xFF3A2A1A)

/** Толщины корешков из макета, dp. */
private val SpineWidths = intArrayOf(34, 28, 38, 26, 31, 36, 27, 33, 29, 40, 25, 32, 30, 35, 27, 31)

/** Высоты книг как доля шага полки. */
private val SpineHeights = floatArrayOf(
    0.82f, 0.74f, 0.84f, 0.70f, 0.77f, 0.80f, 0.72f, 0.79f,
    0.75f, 0.86f, 0.69f, 0.78f, 0.81f, 0.73f, 0.76f, 0.83f,
)

/** Переплётные ткани: бордо, индиго, бутылочное стекло, охра, графит и пара светлых льняных. */
private val SpineCloths = listOf(
    Color(0xFF6B2737), Color(0xFF23324A), Color(0xFF2E2A26), Color(0xFF7A3B22), Color(0xFF2F4A3A),
    Color(0xFF4A1C22), Color(0xFF8A6A2E), Color(0xFF3C4048), Color(0xFF1F4E5F), Color(0xFFC9B78F),
    Color(0xFF2E2F5A), Color(0xFF4E5230), Color(0xFF6E7B62), Color(0xFF5A4030), Color(0xFF4B3555),
    Color(0xFF7D2E3A), Color(0xFFD8CBB0), Color(0xFF5B6B70), Color(0xFF6B4A22), Color(0xFF34503F),
    Color(0xFF3A2233), Color(0xFF8C5A2B), Color(0xFF26384A), Color(0xFF4F3A2A), Color(0xFF2D4A52),
    Color(0xFF3B4660), Color(0xFF5A2E4A), Color(0xFF3E5A45),
)

/** В E-Ink корешки чередуют чёрный, белый и серые. */
private val EInkSpineFills = listOf(
    Color(0xFF1A1A1A), Color(0xFFF5F5F2), Color(0xFF5A5A5A), Color(0xFFF5F5F2), Color(0xFF2E2E2E), Color(0xFF9A9A9A),
)
