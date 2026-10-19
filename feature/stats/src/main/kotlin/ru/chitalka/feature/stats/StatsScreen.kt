package ru.chitalka.feature.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.chitalka.core.designsystem.component.FilterPill
import ru.chitalka.core.designsystem.component.pressScale
import ru.chitalka.core.designsystem.component.riseIn
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.model.ReaderSettings

@Composable
internal fun StatsDestination(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(state = state, onGoal = viewModel::setGoal)
}

/**
 * Вкладка «Статистика» из макета v3: кольцо дневной цели, столбики минут по дням недели
 * и четыре плитки — серия, книги за год, страницы за неделю, в среднем за день.
 * Тап по кольцу открывает выбор цели.
 */
@Composable
internal fun StatsScreen(state: StatsUiState, onGoal: (Int) -> Unit) {
    val colors = ReaderTheme.colors
    val stats = state.stats
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }

    Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = top + 20.dp, bottom = bottom + 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Статистика", style = MaterialTheme.typography.headlineLarge, color = colors.ink, modifier = Modifier.riseIn(distance = 18.dp))
            if (state.isLoading) return@Column
            GoalCard(stats, onGoal, Modifier.riseIn(60))
            WeekCard(stats, Modifier.riseIn(120))
            val tiles = listOf(
                Tile("Серия", "${stats.streak} ${plural(stats.streak, "день", "дня", "дней")}", "лучшая — ${stats.bestStreak}"),
                Tile(
                    "Книг за год",
                    "${stats.booksThisYear}",
                    if (stats.booksThisMonth > 0) "+${stats.booksThisMonth} за ${monthName(stats.month)}" else "дочитанные до конца",
                ),
                Tile("Страниц за неделю", "${stats.pagesThisWeek}", pagesTrend(stats.pagesThisWeek, stats.pagesLastWeek)),
                Tile("В среднем", formatMinutes(stats.averageMinutes), "в день за неделю"),
            )
            tiles.chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEachIndexed { column, tile ->
                        StatTile(tile, Modifier.weight(1f).riseIn(180 + (row * 2 + column) * 60))
                    }
                }
            }
        }
    }
}

private class Tile(val label: String, val value: String, val note: String)

/**
 * Кольцо «Цель на сегодня»: розово-фиолетовая дуга набегает от нуля до прочитанного,
 * в центре минуты, справа — сколько осталось до продолжения серии. В углу — фиолетовое свечение.
 */
@Composable
private fun GoalCard(stats: ReadingStats, onGoal: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics
    val animate = ReaderTheme.animationsEnabled
    var choosing by remember { mutableStateOf(false) }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(stats.goalProgress) {
        if (animate) {
            sweep.animateTo(stats.goalProgress, tween(900, delayMillis = 200, easing = MotionTokens.Emphasized))
        } else {
            sweep.snapTo(stats.goalProgress)
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .drawBehind {
                // Свечение в правом верхнем углу, как radial-gradient в макете
                val radius = 120.dp.toPx()
                val center = Offset(size.width + 70.dp.toPx() - radius, -70.dp.toPx() + radius)
                drawCircle(
                    Brush.radialGradient(listOf(colors.gradientEnd.copy(alpha = 0.25f), Color.Transparent), center, radius),
                    radius,
                    center,
                )
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .pressScale(interaction)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                    haptics.toggle(!choosing)
                    choosing = !choosing
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = "Цель на сегодня: ${stats.todayMinutes} из ${stats.goalMinutes} минут. Нажмите, чтобы изменить цель"
                }
                .padding(horizontal = 22.dp, vertical = 35.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    val inset = stroke / 2
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(colors.track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arc, style = Stroke(stroke))
                    if (sweep.value > 0f) {
                        drawArc(
                            Brush.linearGradient(listOf(colors.gradientStart, colors.gradientEnd), Offset.Zero, Offset(size.width, size.height)),
                            startAngle = -90f,
                            sweepAngle = 360f * sweep.value,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arc,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stats.todayMinutes}", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp), color = colors.ink)
                    Text("из ${stats.goalMinutes} мин", style = MaterialTheme.typography.labelMedium, color = colors.inkSecondary)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Цель на сегодня", style = MaterialTheme.typography.bodySmall, color = colors.inkSecondary)
                Text(goalCaption(stats), style = MaterialTheme.typography.titleLarge, color = colors.ink)
            }
        }
        AnimatedVisibility(
            visible = choosing,
            enter = expandVertically(tween(MotionTokens.PANELS_MS, easing = MotionTokens.Emphasized)) + fadeIn(),
            exit = shrinkVertically(tween(MotionTokens.PANELS_MS, easing = MotionTokens.Emphasized)) + fadeOut(),
        ) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Сколько минут в день", style = MaterialTheme.typography.labelLarge, color = colors.inkSecondary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderSettings.DAILY_GOALS.forEach { minutes ->
                        FilterPill(
                            label = "$minutes",
                            selected = minutes == stats.goalMinutes,
                            onClick = {
                                haptics.toggle(true)
                                onGoal(minutes)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * «Эта неделя»: семь столбиков минут, сегодня — фирменный градиент, остальные — приглушённый фиолетовый.
 * Пунктир — дневная цель. Столбики вырастают снизу по очереди.
 */
@Composable
private fun WeekCard(stats: ReadingStats, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val animate = ReaderTheme.animationsEnabled
    val grow = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) grow.animateTo(1f, tween(650 + 6 * 50, delayMillis = 180, easing = MotionTokens.Emphasized))
    }
    val scale = maxOf(stats.week.maxOfOrNull { it.minutes } ?: 0, stats.goalMinutes, 1)
    val description = "Минуты чтения по дням: " + stats.week.joinToString(", ") { "${it.label.lowercase()} ${it.minutes}" }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
    ) {
        Row(Modifier.padding(bottom = 14.dp), verticalAlignment = Alignment.Bottom) {
            Text("Эта неделя", style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
            Text(formatMinutes(stats.weekMinutes), style = MaterialTheme.typography.bodySmall, color = colors.inkSecondary)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .semantics { contentDescription = description },
        ) {
            val count = stats.week.size.coerceAtLeast(1)
            val gap = 10.dp.toPx()
            val barWidth = (size.width - gap * (count - 1)) / count
            val radius = CornerRadius(8.dp.toPx())
            stats.week.forEachIndexed { i, day ->
                val x = i * (barWidth + gap)
                // Каждый следующий столбик стартует чуть позже: волна слева направо
                val local = ((grow.value * (count + 2) - i) / 3f).coerceIn(0f, 1f)
                val minHeight = 6.dp.toPx()
                val full = (day.minutes.toFloat() / scale * size.height).coerceAtLeast(if (day.isFuture) 0f else minHeight)
                val h = full * local
                if (day.isFuture || h <= 0f) {
                    drawRoundRect(colors.track.copy(alpha = 0.5f), Offset(x, size.height - minHeight), Size(barWidth, minHeight), radius)
                } else {
                    val brush = if (day.isToday) {
                        Brush.verticalGradient(listOf(colors.gradientStart, colors.gradientEnd), startY = size.height - h, endY = size.height)
                    } else {
                        Brush.verticalGradient(listOf(colors.gradientEnd.copy(alpha = 0.35f), colors.gradientEnd.copy(alpha = 0.35f)))
                    }
                    drawRoundRect(brush, Offset(x, size.height - h), Size(barWidth, h), radius)
                }
            }
            val goalY = size.height - stats.goalMinutes.toFloat() / scale * size.height
            drawLine(
                colors.inkSecondary.copy(alpha = 0.45f),
                Offset(0f, goalY),
                Offset(size.width, goalY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            stats.week.forEach { day ->
                Text(
                    day.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (day.isToday) colors.accent else colors.inkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatTile(tile: Tile, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .semantics(mergeDescendants = true) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(tile.label, style = MaterialTheme.typography.bodySmall, color = colors.inkSecondary)
        Text(tile.value, style = MaterialTheme.typography.headlineLarge, color = colors.ink, maxLines = 1)
        Text(tile.note, style = MaterialTheme.typography.labelMedium, color = colors.author, maxLines = 2)
    }
}
