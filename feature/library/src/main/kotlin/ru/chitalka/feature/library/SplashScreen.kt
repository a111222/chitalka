package ru.chitalka.feature.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ru.chitalka.core.designsystem.component.GradientIconButton
import ru.chitalka.core.designsystem.component.riseIn
import ru.chitalka.core.designsystem.cover.BookCover
import ru.chitalka.core.designsystem.cover.CoverArt
import ru.chitalka.core.designsystem.cover.CoverSize
import ru.chitalka.core.designsystem.icon.ChitalkaIcons
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.designsystem.typography.ChitalkaFonts
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val WALL_COLUMNS = 4
private const val WALL_COVERS = 28

/**
 * Заставка из видео: наклонённая стена обложек медленно плывёт. По кнопке обложки
 * разлетаются в стороны, в центре разворачивается корешок книги, надпись рассыпается
 * искрами — и открывается главная.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics
    val animate = ReaderTheme.animationsEnabled
    var leaving by remember { mutableStateOf(false) }
    // Время ухода в миллисекундах: от него считаются все эффекты разом
    val clock = remember { Animatable(0f) }
    LaunchedEffect(leaving) {
        if (!leaving) return@LaunchedEffect
        if (animate) {
            clock.animateTo(MotionTokens.SPLASH_TOTAL_MS.toFloat(), tween(MotionTokens.SPLASH_TOTAL_MS, easing = LinearEasing))
        }
        onFinished()
    }
    val drift by rememberInfiniteTransition(label = "wallDrift").animateFloat(
        initialValue = 0f,
        targetValue = if (animate) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(MotionTokens.WALL_DRIFT_MS, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val t = { if (leaving) clock.value else -1f }

    BoxWithConstraints(modifier.fillMaxSize().background(colors.background)) {
        CoverWall(drift = { drift }, time = t)
        // Низ экрана уходит в фон, чтобы текст читался поверх обложек
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(430.dp)
                .background(
                    Brush.verticalGradient(
                        0f to colors.background.copy(alpha = 0f),
                        0.38f to colors.background.copy(alpha = 0.86f),
                        0.62f to colors.background,
                    ),
                ),
        )
        Spine(time = t, modifier = Modifier.align(Alignment.TopCenter).offset(y = maxHeight * 0.42f - 165.dp))

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.sparks(t)) {
                DustText(
                    "Все ваши книги\nна одной полке",
                    MaterialTheme.typography.headlineMedium,
                    colors.ink,
                    t,
                    Modifier.riseIn(150),
                )
            }
            Spacer(Modifier.height(20.dp))
            DustText(
                "EPUB и FB2 · читается без интернета",
                MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                colors.inkSecondary,
                t,
                Modifier.riseIn(230),
            )
            Spacer(Modifier.height(24.dp))
            GradientIconButton(
                icon = ChitalkaIcons.ArrowForward,
                contentDescription = "Открыть библиотеку",
                onClick = {
                    if (!leaving) {
                        haptics.confirm()
                        leaving = true
                    }
                },
                size = 56.dp,
                shape = CircleShape,
                modifier = Modifier
                    .riseIn(310)
                    .graphicsLayer {
                        val p = dustProgress(t())
                        alpha = 1f - p
                        scaleX = 1f - 0.3f * p
                        scaleY = 1f - 0.3f * p
                    },
            )
        }
    }
}

/** Доля рассыпания надписи 0…1. */
private fun dustProgress(time: Float): Float =
    if (time < 0f) 0f else (time / MotionTokens.SPLASH_DUST_MS).coerceIn(0f, 1f)

/** Надпись, которая при уходе расплывается, разреживается и тает (chDust). */
@Composable
private fun DustText(text: String, style: TextStyle, color: Color, time: () -> Float, modifier: Modifier = Modifier) {
    val p = dustProgress(time())
    Text(
        text = text,
        style = style.copy(letterSpacing = (0.35f * p).em, textAlign = TextAlign.Center),
        color = color,
        modifier = modifier
            .blur((6 * p).dp)
            .graphicsLayer { alpha = 1f - p },
    )
}

/** Наклонённая стена из 28 обложек, 4 колонки (как в макете: 640×1100, поворот −16°). */
@Composable
private fun CoverWall(drift: () -> Float, time: () -> Float) {
    Box(
        Modifier
            .offset(x = (-120).dp, y = (-150).dp)
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .requiredSize(640.dp, 1100.dp)
            .graphicsLayer { rotationZ = -16f },
    ) {
        Column(
            Modifier.graphicsLayer { translationY = -120.dp.toPx() * drift() },
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (row in 0 until WALL_COVERS / WALL_COLUMNS) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (column in 0 until WALL_COLUMNS) {
                        val k = row * WALL_COLUMNS + column
                        val book = DecorativeBooks[(k * 5) % DecorativeBooks.size]
                        BookCover(
                            title = book.title,
                            author = book.author,
                            coverPath = null,
                            size = CoverSize.Wall,
                            artOverride = CoverArt.styles[book.style],
                            modifier = Modifier
                                .flyAway(k, time)
                                .width(149.dp)
                                .height(196.dp)
                                .shadow(16.dp, RoundedCornerShape(6.dp), ambientColor = Color.Black, spotColor = Color.Black),
                        )
                    }
                }
            }
        }
    }
}

/** Обложка k улетает по «золотому углу», чтобы разлёт выглядел случайным, но был одинаковым. */
private fun Modifier.flyAway(k: Int, time: () -> Float): Modifier = graphicsLayer {
    val now = time()
    if (now < 0f) return@graphicsLayer
    val delay = (k % 7) * MotionTokens.SPLASH_FLY_STAGGER_MS
    val local = ((now - delay) / MotionTokens.SPLASH_FLY_MS).coerceIn(0f, 1f)
    val move = MotionTokens.Standard.transform(local)
    val angle = (k * 137.5f) % 360f
    val rad = angle * PI.toFloat() / 180f
    translationX = cos(rad) * 520.dp.toPx() * move
    translationY = sin(rad) * 620.dp.toPx() * move
    rotationZ = (angle % 60f - 30f) * move
    alpha = 1f - ((now - delay) / 700f).coerceIn(0f, 1f)
}

/** Бирюзовый корешок: выезжает из глубины, разворачивается к нам и уходит поворотом (chSpine). */
@Composable
private fun Spine(time: () -> Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .graphicsLayer {
                val now = time()
                if (now < 0f) {
                    alpha = 0f
                    return@graphicsLayer
                }
                val f = MotionTokens.Emphasized.transform((now / MotionTokens.SPLASH_SPINE_MS).coerceIn(0f, 1f))
                cameraDistance = 12f * density
                when {
                    f < 0.35f -> {
                        val a = f / 0.35f
                        alpha = a
                        rotationY = -95f + 95f * a
                        scaleX = 0.7f + 0.3f * a
                        scaleY = scaleX
                    }
                    f < 0.75f -> alpha = 1f
                    else -> {
                        val a = (f - 0.75f) / 0.25f
                        alpha = 1f - a
                        rotationY = 80f * a
                        scaleX = 1f + 0.05f * a
                        scaleY = scaleX
                    }
                }
            }
            .size(46.dp, 330.dp)
            .shadow(30.dp, RoundedCornerShape(3.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .background(
                Brush.horizontalGradient(
                    0f to Color(0xFF0E7C73),
                    0.4f to Color(0xFF22C3B0),
                    1f to Color(0xFF12998B),
                ),
                RoundedCornerShape(3.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "САД ЗА РЕКОЙ",
            style = TextStyle(
                fontFamily = ChitalkaFonts.Playfair,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                letterSpacing = 0.06.em,
            ),
            color = Color(0xFFF4FFFC),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .rotate(90f),
        )
    }
}

/** Искры: 44 точки разлетаются из надписи, пока она рассыпается (chBurst). */
private fun Modifier.sparks(time: () -> Float): Modifier = drawWithContent {
    drawContent()
    val now = time()
    if (now < 0f) return@drawWithContent
    val origin = center
    for (k in 0 until 44) {
        val local = ((now - (k % 9) * 25f) / MotionTokens.SPLASH_BURST_MS).coerceIn(0f, 1f)
        if (local <= 0f || local >= 1f) continue
        val e = MotionTokens.Emphasized.transform(local)
        val angle = k * 2.4f
        val r = (60 + (k * 37) % 120).dp.toPx()
        val start = Offset(((k * 23) % 220 - 110).dp.toPx(), ((k * 11) % 50 - 25).dp.toPx())
        val delta = Offset(cos(angle) * r, sin(angle) * r - 40.dp.toPx())
        val radius = (1 + k % 3).dp.toPx() / 2f * (1f - 0.8f * e)
        drawCircle(Color.White.copy(alpha = 1f - e), radius = radius, center = origin + start + delta * e)
    }
}

/** Классика в общественном достоянии для стены обложек; style — номер стиля из макета. */
internal class DecorativeBook(val title: String, val author: String, val style: Int)

internal val DecorativeBooks = listOf(
    DecorativeBook("Сад за рекой", "Вера Лесная", 0),
    DecorativeBook("Анна Каренина", "Л. Толстой", 1),
    DecorativeBook("Мёртвые души", "Н. Гоголь", 2),
    DecorativeBook("Белые ночи", "Ф. Достоевский", 3),
    DecorativeBook("Евгений Онегин", "А. Пушкин", 4),
    DecorativeBook("Отцы и дети", "И. Тургенев", 5),
    DecorativeBook("Обломов", "И. Гончаров", 6),
    DecorativeBook("Вишнёвый сад", "А. Чехов", 7),
    DecorativeBook("Герой нашего времени", "М. Лермонтов", 8),
    DecorativeBook("Шинель", "Н. Гоголь", 9),
    DecorativeBook("Пиковая дама", "А. Пушкин", 10),
    DecorativeBook("Идиот", "Ф. Достоевский", 11),
)
