package ru.chitalka.feature.reader

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.chitalka.core.designsystem.component.HeaderIconButton
import ru.chitalka.core.designsystem.component.pressScale
import ru.chitalka.core.designsystem.icon.ChitalkaIcons
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.model.HighlightColor

/** Цвет маркера на странице: полупрозрачный, чтобы буквы под ним читались. */
internal fun HighlightColor.markColor(): Color = Color(argb).copy(alpha = 0.42f)

internal fun HighlightColor.label(): String = when (this) {
    HighlightColor.Teal -> "Бирюзовый"
    HighlightColor.Pink -> "Розовый"
    HighlightColor.Amber -> "Янтарный"
    HighlightColor.Violet -> "Фиолетовый"
}

/**
 * Панель над выделенной фразой из матового стекла: четыре цвета маркера, «Копировать»,
 * «Поделиться» и «Убрать». Выбранный цвет обведён кольцом, кольцо переезжает к новому.
 */
@Composable
internal fun HighlightBar(
    color: HighlightColor,
    onColor: (HighlightColor) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    glass: Modifier,
    modifier: Modifier = Modifier,
) {
    val colors = ReaderTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .height(56.dp)
            .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .then(glass)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HighlightColor.entries.forEach { option ->
            ColorDot(option, selected = option == color, onClick = { onColor(option) })
        }
        Box(
            Modifier
                .padding(horizontal = 6.dp)
                .width(1.dp)
                .height(24.dp)
                .background(colors.track),
        )
        HeaderIconButton(ChitalkaIcons.Copy, contentDescription = "Копировать", onClick = onCopy)
        HeaderIconButton(ChitalkaIcons.Share, contentDescription = "Поделиться цитатой", onClick = onShare)
        HeaderIconButton(ChitalkaIcons.Delete, contentDescription = "Убрать выделение", onClick = onDelete, tint = colors.inkSecondary)
    }
}

@Composable
private fun ColorDot(color: HighlightColor, selected: Boolean, onClick: () -> Unit) {
    val ring by animateFloatAsState(if (selected) 1f else 0f, tween(MotionTokens.NAV_MS, easing = MotionTokens.Emphasized), label = "ring")
    val interaction = remember { MutableInteractionSource() }
    val ink = ReaderTheme.colors.ink
    Box(
        Modifier
            .pressScale(interaction)
            .size(40.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = "Маркер: ${color.label()}"
                this.selected = selected
            }
            .drawBehind {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(Color(color.argb), radius = 9.dp.toPx() - ring * 1.5.dp.toPx(), center = center)
                if (ring > 0f) {
                    drawCircle(
                        ink.copy(alpha = ring),
                        radius = 13.dp.toPx(),
                        center = center,
                        style = Stroke(1.6.dp.toPx()),
                    )
                }
            },
    )
}

/**
 * Ленточка-закладка у верхнего края страницы. Когда закладку ставят, лента «выпадает» сверху,
 * когда снимают — уезжает обратно.
 */
@Composable
internal fun BookmarkRibbon(visible: Boolean, modifier: Modifier = Modifier, length: Dp = 34.dp) {
    val colors = ReaderTheme.colors
    val animate = ReaderTheme.animationsEnabled
    val drop by animateDpAsState(
        targetValue = if (visible) length else 0.dp,
        animationSpec = if (animate) tween(MotionTokens.RISE_MS, easing = MotionTokens.Emphasized) else tween(0),
        label = "ribbon",
    )
    Box(
        modifier
            .width(14.dp)
            .height(length)
            .drawBehind {
                val h = drop.toPx()
                if (h <= 0f) return@drawBehind
                val w = size.width
                val notch = w * 0.55f
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(w / 2, (h - notch).coerceAtLeast(0f))
                    lineTo(0f, h)
                    close()
                }
                drawPath(path, Brush.verticalGradient(listOf(colors.gradientEnd, colors.gradientStart), endY = h))
                drawRoundRect(Color.Black.copy(alpha = 0.18f), size = Size(w, 2.dp.toPx()), cornerRadius = CornerRadius.Zero)
            },
    )
}

/** Подсказка над панелью: «Скопировано». */
@Composable
internal fun HintPill(text: String, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceHigh)
            .border(1.dp, colors.track, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = colors.ink)
    }
}
