package ru.chitalka.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme

/**
 * Появление снизу вверх с проявлением (chRise в макете): блок поднимается на [distance]
 * за 600 мс. Задержка [delayMillis] даёт «волну», когда блоков несколько.
 */
fun Modifier.riseIn(delayMillis: Int = 0, distance: Dp = 26.dp): Modifier = composed {
    val enabled = ReaderTheme.animationsEnabled
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (enabled) progress.animateTo(1f, tween(MotionTokens.RISE_MS, delayMillis, MotionTokens.Emphasized))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * distance.toPx()
    }
}

/**
 * Обложки в рядах главной, как в видео: когда ряд доезжает до экрана при прокрутке,
 * обложки по очереди разворачиваются к нам из профиля (поворот вокруг вертикальной оси).
 */
fun Modifier.flipInOnReveal(delayMillis: Int = 0): Modifier = composed {
    val enabled = ReaderTheme.animationsEnabled
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    var revealed by remember { mutableStateOf(!enabled) }
    LaunchedEffect(revealed) {
        if (revealed && progress.value < 1f) {
            progress.animateTo(1f, tween(MotionTokens.RISE_MS, delayMillis, MotionTokens.Emphasized))
        }
    }
    this
        .onGloballyPositioned { coordinates ->
            if (!revealed) {
                val top = coordinates.positionInRoot().y
                val screen = coordinates.findRootCoordinates().size.height
                if (top < screen * 0.92f) revealed = true
            }
        }
        .graphicsLayer {
            val p = progress.value
            rotationY = 75f * (1f - p)
            alpha = (p * 1.6f).coerceAtMost(1f)
            cameraDistance = 10f * density
        }
}

/** Лёгкое «вдавливание» при нажатии, как .ch-btn:active в макете. */
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(160, easing = MotionTokens.Emphasized), label = "press")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Кнопка с фирменным градиентом и розовым свечением: «+» в библиотеке, стрелка на заставке. */
@Composable
fun GradientIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    val colors = ReaderTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(interaction)
            .size(size)
            .shadow(14.dp, shape, ambientColor = colors.gradientStart, spotColor = colors.gradientStart)
            .clip(shape)
            .background(colors.accentDiagonal)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/** Прозрачная круглая кнопка-иконка 44 dp в шапках экранов. */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ReaderTheme.colors.ink,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(interaction)
            .size(44.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(21.dp))
    }
}

/** Полоска прогресса чтения: тёмная дорожка, заполненная фирменным градиентом. */
@Composable
fun GradientProgress(progress: Float, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .drawBehind {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(colors.track, cornerRadius = radius)
                val filled = size.width * progress.coerceIn(0f, 1f)
                if (filled > 0f) {
                    drawRoundRect(colors.accentBrush, size = Size(filled, size.height), cornerRadius = radius)
                }
            },
    )
}

/** Чип фильтра: выбранный залит градиентом, остальные — цветом карточки. */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReaderTheme.colors
    val selection by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "pill")
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(colors.surface)
            .drawBehind { drawRect(colors.accentBrush, alpha = selection) }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.ink)
    }
}

/** Пункт нижней панели. */
class NavItem(val icon: ImageVector, val label: String)

/**
 * Плавающая нижняя панель из макета: матовое стекло, четыре иконки, у активной — розовый цвет
 * и светящаяся точка. Точка не исчезает и появляется, а переезжает к выбранной иконке.
 *
 * [glass] размывает то, что под панелью (см. [ru.chitalka.core.designsystem.component.glass]).
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glass: Modifier = Modifier,
) {
    val colors = ReaderTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val dotIndex by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = tween(MotionTokens.CAROUSEL_MS, easing = MotionTokens.Emphasized),
        label = "navDot",
    )
    val dotAlpha by animateFloatAsState(if (selectedIndex >= 0) 1f else 0f, tween(MotionTokens.NAV_MS), label = "navDotAlpha")
    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .then(glass)
            .drawBehind {
                // Точка-индикатор со свечением: центр i-й ячейки — (2i + 1) / 2n ширины
                val cx = size.width * (2 * dotIndex + 1) / (2 * items.size)
                val cy = size.height - 8.dp.toPx()
                drawCircle(colors.gradientStart.copy(alpha = 0.35f * dotAlpha), radius = 9.dp.toPx(), center = Offset(cx, cy))
                drawCircle(colors.gradientStart.copy(alpha = dotAlpha), radius = 3.dp.toPx(), center = Offset(cx, cy))
            },
    ) {
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val tint by animateColorAsState(
                    if (selected) colors.accent else Color(0xFF8F92A8),
                    tween(MotionTokens.NAV_MS),
                    label = "navTint",
                )
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressScale(interaction)
                        .clickable(interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(23.dp))
                }
            }
        }
    }
}
