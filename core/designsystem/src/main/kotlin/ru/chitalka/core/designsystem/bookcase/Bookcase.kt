package ru.chitalka.core.designsystem.bookcase

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderColors
import ru.chitalka.core.designsystem.theme.ReaderTheme

/**
 * Шкаф: стенки из дерева и полки с тёплой подсветкой. Всё рисуется на Canvas векторно,
 * поэтому масштабируется под любую ширину без «мыла».
 *
 * Это упрощённая версия для каркаса. Резной карниз, текстура дерева (WebP) и AGSL-шейдеры
 * добавятся на этапах «Дизайн-система» и «Премиум-эффекты».
 *
 * @param lightsOn подсветка полок; включается и гаснет плавно, как настоящая лампа
 * @param shelfContent что стоит на полке с номером index (обычно ряд корешков)
 */
@Composable
fun Bookcase(
    shelfCount: Int,
    modifier: Modifier = Modifier,
    lightsOn: Boolean = true,
    shelfHeight: Dp = 148.dp,
    shelfContent: @Composable (index: Int) -> Unit,
) {
    val colors = ReaderTheme.colors
    val glow by animateFloatAsState(
        targetValue = if (lightsOn) 1f else 0f,
        animationSpec = if (colors.isEInk) tween(0) else tween(MotionTokens.SHELF_GLOW_MS, easing = MotionTokens.Emphasized),
        label = "shelfGlow",
    )
    Column(
        modifier = modifier
            .drawBehind { drawCaseFrame(colors) }
            .padding(horizontal = SideWall, vertical = SideWall),
    ) {
        repeat(shelfCount) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(shelfHeight)
                    .drawBehind { drawShelf(colors, glow) },
                contentAlignment = Alignment.BottomStart,
            ) {
                Box(Modifier.padding(start = 12.dp, end = 12.dp, bottom = PlankThickness)) {
                    shelfContent(index)
                }
            }
        }
    }
}

/** Ряд корешков на полке, прижатых к доске. */
@Composable
fun ShelfRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) { content() }
}

private val SideWall = 14.dp
private val PlankThickness = 14.dp

private fun DrawScope.drawCaseFrame(colors: ReaderColors) {
    val wall = SideWall.toPx()
    // Задняя стенка
    drawRect(colors.wall)
    if (colors.isEInk) {
        // Гравюра: только контуры
        val stroke = Stroke(width = 2.dp.toPx())
        drawRect(colors.line, style = stroke)
        drawRect(colors.line, topLeft = Offset(wall, wall), size = Size(size.width - 2 * wall, size.height - 2 * wall), style = stroke)
        return
    }
    val woodBrush = Brush.horizontalGradient(listOf(colors.woodShadow, colors.wood, colors.woodShadow))
    // Боковые стенки, крышка и цоколь
    drawRect(woodBrush, size = Size(wall, size.height))
    drawRect(woodBrush, topLeft = Offset(size.width - wall, 0f), size = Size(wall, size.height))
    drawRect(Brush.verticalGradient(listOf(colors.wood, colors.woodShadow)), size = Size(size.width, wall))
    drawRect(
        Brush.verticalGradient(listOf(colors.wood, colors.woodShadow)),
        topLeft = Offset(0f, size.height - wall),
        size = Size(size.width, wall),
    )
}

private fun DrawScope.drawShelf(colors: ReaderColors, glow: Float) {
    val plank = PlankThickness.toPx()
    val plankTop = size.height - plank
    if (colors.isEInk) {
        drawLine(colors.line, Offset(0f, plankTop), Offset(size.width, plankTop), strokeWidth = 2.dp.toPx())
        drawLine(colors.line, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
        return
    }
    // Тёплый конус света от светодиода под полкой выше: сверху вниз
    if (glow > 0f) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to colors.shelfGlow.copy(alpha = 0.55f * glow),
                0.6f to colors.shelfGlow.copy(alpha = 0.12f * glow),
                1f to colors.shelfGlow.copy(alpha = 0f),
            ),
            size = Size(size.width, plankTop),
        )
    }
    // Доска полки с бликом на переднем крае
    drawRect(
        brush = Brush.verticalGradient(listOf(colors.wood, colors.woodShadow)),
        topLeft = Offset(0f, plankTop),
        size = Size(size.width, plank),
    )
    drawLine(
        color = colors.shelfGlow.copy(alpha = 0.6f * glow),
        start = Offset(0f, plankTop + 1f),
        end = Offset(size.width, plankTop + 1f),
        strokeWidth = 1.dp.toPx(),
    )
}

/** Корешок книги без обложки: рисуем его в стиле шкафа. */
@Composable
fun BookSpinePlaceholder(
    modifier: Modifier = Modifier,
) {
    val colors = ReaderTheme.colors
    Canvas(modifier) {
        if (colors.isEInk) {
            drawRect(colors.line, style = Stroke(width = 1.5.dp.toPx()))
        } else {
            drawRect(Brush.horizontalGradient(listOf(colors.woodShadow, colors.accent, colors.woodShadow)))
            val band = 3.dp.toPx()
            drawRect(colors.shelfGlow.copy(alpha = 0.5f), topLeft = Offset(0f, size.height * 0.18f), size = Size(size.width, band))
            drawRect(colors.shelfGlow.copy(alpha = 0.5f), topLeft = Offset(0f, size.height * 0.78f), size = Size(size.width, band))
        }
    }
}
