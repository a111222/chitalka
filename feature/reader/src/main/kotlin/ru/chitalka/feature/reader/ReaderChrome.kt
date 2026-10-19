package ru.chitalka.feature.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.chitalka.core.designsystem.component.HeaderIconButton
import ru.chitalka.core.designsystem.component.riseIn
import ru.chitalka.core.designsystem.icon.ChitalkaIcons
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme

/** Верх читалки из видео: «назад» слева, справа закладка, оглавление и «Аа». */
@Composable
internal fun ReaderTopBar(
    onBack: () -> Unit,
    onContents: () -> Unit,
    contentsEnabled: Boolean,
    onAppearance: () -> Unit,
    bookmarked: Boolean,
    onBookmark: () -> Unit,
) {
    val colors = ReaderTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 8.dp, top = 4.dp)
            .riseIn(0, distance = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIconButton(ChitalkaIcons.Back, contentDescription = "Назад к книге", onClick = onBack)
        Spacer(Modifier.weight(1f))
        HeaderIconButton(
            if (bookmarked) ChitalkaIcons.BookmarkFilled else ChitalkaIcons.Bookmark,
            contentDescription = if (bookmarked) "Убрать закладку" else "Закладка на странице",
            onClick = onBookmark,
            tint = if (bookmarked) colors.gradientStart else colors.ink,
        )
        HeaderIconButton(
            ChitalkaIcons.Contents,
            contentDescription = "Оглавление",
            onClick = { if (contentsEnabled) onContents() },
            tint = if (contentsEnabled) colors.ink else colors.inkSecondary,
        )
        HeaderIconButton(ChitalkaIcons.Settings, contentDescription = "Оформление: шрифт, размер, оттенок", onClick = onAppearance)
    }
}

/** Низ читалки: кольцо прогресса книги с процентом и «Глава N · стр. X из Y». */
@Composable
internal fun ReaderBottomBar(percent: Float, label: String, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(percent)
        Spacer(Modifier.weight(1f))
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.inkSecondary)
    }
}

/**
 * Кольцо прогресса с фирменным градиентом. При открытии книги дуга набегает от нуля,
 * как в видео, потом плавно догоняет каждую новую страницу.
 */
@Composable
internal fun ProgressRing(percent: Float, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val animate = ReaderTheme.animationsEnabled
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(percent) {
        val target = percent.coerceIn(0f, 1f)
        if (animate) sweep.animateTo(target, tween(MotionTokens.RISE_MS, easing = MotionTokens.Emphasized)) else sweep.snapTo(target)
    }
    val label = percentLabel(percent)
    Box(
        modifier
            .size(50.dp)
            .semantics { contentDescription = "Прочитано $label" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(50.dp)) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2 + 1.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(colors.track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            if (sweep.value > 0f) {
                drawArc(
                    Brush.linearGradient(listOf(colors.gradientStart, colors.gradientEnd), start = Offset.Zero, end = Offset(size.width, size.height)),
                    startAngle = -90f,
                    sweepAngle = 360f * sweep.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
    }
}
