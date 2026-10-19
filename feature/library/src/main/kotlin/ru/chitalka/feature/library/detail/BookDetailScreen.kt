package ru.chitalka.feature.library.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.chitalka.core.designsystem.component.HeaderIconButton
import ru.chitalka.core.designsystem.component.pressScale
import ru.chitalka.core.designsystem.component.riseIn
import ru.chitalka.core.designsystem.component.sparkleBurst
import ru.chitalka.core.designsystem.cover.BookCover
import ru.chitalka.core.designsystem.cover.CoverArt
import ru.chitalka.core.designsystem.cover.CoverSize
import ru.chitalka.core.designsystem.icon.ChitalkaIcons
import ru.chitalka.core.designsystem.motion.navEnterExit
import ru.chitalka.core.designsystem.motion.sharedBook
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.designsystem.typography.ChitalkaFonts
import ru.chitalka.feature.library.SAMPLE_BOOK_ID
import ru.chitalka.feature.library.bookSpine
import kotlin.math.roundToInt
import kotlin.math.sin

/** Размер книги совпадает с центральной обложкой карусели: при перелёте она только едет, не растягиваясь. */
private val BookWidth = 150.dp
private val BookHeight = 222.dp

/** Сколько раскрытой книги прячется за левым краем экрана, как в видео. */
private val HiddenEdge = 34.dp

/** Обложка раскрывается до 180°: ложится слева за край экрана, остаётся видна только страница. */
private const val OPEN_ANGLE = 180f

private val PagePaper = Color(0xFFF4F0E8)
private val PageEdge = Color(0xFFD9D2C4)
private val PageInk = Color(0xFF2B2833)
private val InsideCover = Color(0xFFE4DDCF)

/** То, что карточка знает сразу из перехода, ещё до загрузки: этого хватает, чтобы обложка перелетела без пустого кадра. */
internal data class BookPreview(val id: Long, val title: String, val author: String?, val coverPath: String?)

@Composable
internal fun BookDetailRoute(
    preview: BookPreview,
    onBack: () -> Unit,
    onRead: (Long) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isGone) { if (state.isGone) onBack() }
    BookDetailScreen(
        preview = preview,
        state = state,
        onBack = onBack,
        onRead = onRead,
        onOpenBookmarks = onOpenBookmarks,
        onOpenSettings = onOpenSettings,
    )
}

/**
 * Карточка «О книге» из видео: обложка прилетает из карусели, уезжает влево и раскрывается —
 * остаётся белая страница, наполовину за краем экрана. Справа название, автор и сведения,
 * ниже аннотация, внизу кнопка чтения. Назад — книга сначала закрывается, потом улетает обратно.
 */
@Composable
internal fun BookDetailScreen(
    preview: BookPreview,
    state: BookDetailUiState,
    onBack: () -> Unit,
    onRead: (Long) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = ReaderTheme.colors
    val animate = ReaderTheme.animationsEnabled
    val scope = rememberCoroutineScope()
    val detail = state.book
    val title = detail?.title ?: preview.title
    val author = detail?.author ?: preview.author

    val openAngle = remember { Animatable(if (animate) 0f else OPEN_ANGLE) }
    LaunchedEffect(Unit) {
        if (animate && openAngle.value < OPEN_ANGLE) {
            // Сначала обложка долетает на место, потом раскрывается
            delay(MotionTokens.BOOK_FLIGHT_MS - 80L)
            openAngle.animateTo(OPEN_ANGLE, tween(MotionTokens.BOOK_OPEN_MS, easing = MotionTokens.BookOpen))
        }
    }
    var leaving by remember { mutableStateOf(false) }
    fun close() {
        if (leaving) return
        leaving = true
        scope.launch {
            if (animate) openAngle.animateTo(0f, tween(MotionTokens.BOOK_CLOSE_MS, easing = MotionTokens.Emphasized))
            onBack()
        }
    }
    BackHandler(enabled = !leaving) { close() }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth()) {
                // Подложка верхней карточки: в видео она проявляется, пока книга летит
                Box(
                    Modifier
                        .matchParentSize()
                        .navEnterExit(
                            enter = fadeIn(tween(MotionTokens.RISE_MS)),
                            exit = fadeOut(tween(MotionTokens.NAV_MS)),
                        )
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(colors.surface)
                        .drawBehind {
                            // Большой тусклый круг за названием, как в видео
                            drawCircle(colors.surfaceHigh.copy(alpha = 0.55f), radius = size.width * 0.42f, center = Offset(size.width * 0.72f, size.height * 0.36f))
                        },
                )
                Column(Modifier.statusBarsPadding()) {
                    TopBar(
                        onBack = ::close,
                        onOpenBookmarks = onOpenBookmarks,
                        onOpenSettings = onOpenSettings,
                    )
                    Row(Modifier.padding(top = 6.dp, bottom = 28.dp)) {
                        Box(Modifier.width(BookWidth - HiddenEdge).height(BookHeight)) {
                            OpeningBook(
                                preview = preview,
                                angle = { openAngle.value },
                                modifier = Modifier
                                    .wrapContentWidth(Alignment.End, unbounded = true)
                                    .size(BookWidth, BookHeight),
                            )
                        }
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f).padding(end = 20.dp, top = 8.dp)) {
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 24.sp),
                                color = colors.ink,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.riseIn(MotionTokens.BOOK_FLIGHT_MS / 2),
                            )
                            if (!author.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    author,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.author,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.riseIn(MotionTokens.BOOK_FLIGHT_MS / 2 + MotionTokens.RISE_STAGGER_MS),
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            StatsGrid(detail)
                        }
                    }
                }
            }

            About(state, Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp).navigationBarsPadding().padding(bottom = 132.dp))
        }

        ReadButton(
            progress = detail?.progress ?: 0f,
            onClick = { onRead(preview.id) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onOpenBookmarks: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 10.dp, top = 6.dp)
            .riseIn(120, distance = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIconButton(ChitalkaIcons.Back, contentDescription = "Назад", onClick = onBack)
        Spacer(Modifier.weight(1f))
        HeaderIconButton(ChitalkaIcons.Bookmark, contentDescription = "Закладки и цитаты", onClick = onOpenBookmarks)
        HeaderIconButton(ChitalkaIcons.Settings, contentDescription = "Оформление", onClick = onOpenSettings)
    }
}

/**
 * Книга, которая раскрывается: снизу стопка страниц с титульным листом, сверху обложка
 * на петле у левого края. Пока обложка повёрнута меньше чем на 90°, видна её лицевая сторона,
 * дальше — изнанка. На страницу падает тень от поднятой обложки.
 */
@Composable
private fun OpeningBook(preview: BookPreview, angle: () -> Float, modifier: Modifier = Modifier) {
    val art = remember(preview.id) { if (preview.id == SAMPLE_BOOK_ID) CoverArt.styles[0] else null }
    val showsInside by remember { derivedStateOf { angle() > 90f } }
    val opened by remember { derivedStateOf { angle() >= OPEN_ANGLE - 1f } }
    Box(
        modifier
            .sharedBook(preview.id)
            .semantics { contentDescription = "Книга «${preview.title}»" },
    ) {
        TitlePage(
            title = preview.title,
            author = preview.author,
            coverShadow = { (sin(Math.toRadians(angle().toDouble())).toFloat()).coerceAtLeast(0f) * (1f - angle() / OPEN_ANGLE) },
            modifier = Modifier.matchParentSize(),
        )
        if (!opened) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = -angle()
                        cameraDistance = 16f * density
                    },
            ) {
                if (showsInside) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(InsideCover)
                            .drawWithContent {
                                drawContent()
                                drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent)))
                            },
                    )
                } else {
                    BookCover(
                        title = preview.title,
                        author = preview.author,
                        coverPath = preview.coverPath,
                        size = CoverSize.Hero,
                        artOverride = art,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .matchParentSize()
                            .shadow(18.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black, spotColor = Color.Black)
                            .clip(RoundedCornerShape(8.dp))
                            .bookSpine(),
                    )
                }
            }
        }
    }
}

/** Титульный лист и обрез стопки страниц справа и снизу. */
@Composable
private fun TitlePage(title: String, author: String?, coverShadow: () -> Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp)
    Box(
        modifier
            .drawBehind {
                // Обрез: несколько листов чуть выглядывают из-под верхнего
                for (k in 3 downTo 1) {
                    val shift = k * 1.6f * density
                    drawRoundRect(
                        color = if (k % 2 == 0) PageEdge else PagePaper.copy(alpha = 0.9f),
                        topLeft = Offset(shift, shift),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                }
            }
            .shadow(16.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(PagePaper)
            .drawWithContent {
                drawContent()
                // Сгиб у корешка
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent), endX = 22.dp.toPx()),
                    size = Size(22.dp.toPx(), size.height),
                )
                // Тень от поднятой обложки
                val shade = coverShadow()
                if (shade > 0f) drawRect(Color.Black.copy(alpha = 0.32f * shade))
            }
            .padding(start = HiddenEdge + 6.dp, end = 10.dp, top = 26.dp, bottom = 14.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = TextStyle(fontFamily = ChitalkaFonts.Playfair, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 15.sp),
                color = PageInk,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (!author.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    author,
                    style = TextStyle(fontFamily = ChitalkaFonts.Playfair, fontSize = 8.sp, lineHeight = 10.sp),
                    color = PageInk.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(18.dp, 1.dp).background(PageInk.copy(alpha = 0.35f)))
        }
        Text(
            "Читалка",
            style = TextStyle(fontSize = 6.sp, letterSpacing = 1.sp),
            color = PageInk.copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Сведения о книге двумя столбцами с розовой чертой между ними, как «Rating / Genre | Listening / Pages» в видео. */
@Composable
private fun StatsGrid(detail: BookDetail?) {
    val colors = ReaderTheme.colors
    val progress = detail?.progress ?: 0f
    val read = when {
        detail == null -> "…"
        progress <= 0f -> "Не начата"
        progress >= 0.99f -> "Прочитана"
        else -> "${(progress * 100).roundToInt()}%"
    }
    val chapters = detail?.chapterCount?.toString() ?: "…"
    val genre = detail?.genre ?: if (detail?.chapterCount != null) "Не указан" else "…"
    val format = detail?.format ?: "…"
    val base = MotionTokens.BOOK_FLIGHT_MS / 2 + 2 * MotionTokens.RISE_STAGGER_MS
    Row(Modifier.height(IntrinsicHeightFallback)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Stat("Прочитано", read, Modifier.riseIn(base), star = true)
            Stat("Жанр", genre, Modifier.riseIn(base + 2 * MotionTokens.RISE_STAGGER_MS))
        }
        Box(
            Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .width(1.5.dp)
                .height(IntrinsicHeightFallback - 8.dp)
                .riseIn(base + MotionTokens.RISE_STAGGER_MS, distance = 0.dp)
                .background(Brush.verticalGradient(listOf(colors.gradientStart, colors.gradientEnd))),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Stat("Глав", chapters, Modifier.riseIn(base + MotionTokens.RISE_STAGGER_MS))
            Stat("Формат", format, Modifier.riseIn(base + 3 * MotionTokens.RISE_STAGGER_MS))
        }
    }
}

/** Высота сетки сведений: две строки «подпись + значение» с отступом. */
private val IntrinsicHeightFallback = 92.dp

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, star: Boolean = false) {
    val colors = ReaderTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.inkSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (star) {
                Text("★", style = MaterialTheme.typography.labelLarge, color = colors.star)
                Spacer(Modifier.width(4.dp))
            }
            Text(value, style = MaterialTheme.typography.labelLarge, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Аннотация поднимается снизу, когда книга уже раскрылась. Пока файл читается — серые строки-заглушки. */
@Composable
private fun About(state: BookDetailUiState, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val detail = state.book
    val delayMs = MotionTokens.BOOK_FLIGHT_MS + 120
    Column(
        modifier.navEnterExit(
            enter = slideInVertically(tween(MotionTokens.RISE_MS, delayMs, MotionTokens.Emphasized)) { it / 3 } + fadeIn(tween(MotionTokens.RISE_MS, delayMs)),
            exit = slideOutVertically(tween(MotionTokens.NAV_MS)) { it / 3 } + fadeOut(tween(MotionTokens.NAV_MS)),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (detail?.descriptionIsExcerpt == true) "Начало книги" else "О книге",
            style = TextStyle(fontFamily = ChitalkaFonts.Playfair, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
            color = colors.ink,
        )
        val description = detail?.description
        when {
            description != null -> description.split("\n\n").forEach { paragraph ->
                Text(paragraph, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp), color = colors.inkSecondary)
            }
            detail != null && detail.chapterCount != null ->
                Text("В файле книги нет аннотации.", style = MaterialTheme.typography.bodyMedium, color = colors.inkSecondary)
            else -> repeat(4) { line ->
                Box(
                    Modifier
                        .fillMaxWidth(if (line == 3) 0.6f else 1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(colors.track.copy(alpha = 0.6f)),
                )
            }
        }
    }
}

/**
 * Кнопка чтения внизу, как «Continue Reading» в видео: градиентная, с искрами при нажатии.
 * Над ней экран мягко уходит в фон, чтобы текст не обрывался о край кнопки.
 */
@Composable
private fun ReadButton(progress: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics
    val animate = ReaderTheme.animationsEnabled
    val scope = rememberCoroutineScope()
    val burst = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val label = when {
        progress <= 0f -> "Начать чтение"
        progress >= 0.99f -> "Перечитать"
        else -> "Продолжить чтение · ${(progress * 100).roundToInt()}%"
    }
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background.copy(alpha = 0.92f), colors.background)))
            .navigationBarsPadding()
            .padding(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 18.dp)
            .riseIn(MotionTokens.BOOK_FLIGHT_MS + 200),
    ) {
        Row(
            Modifier
                .pressScale(interaction)
                .fillMaxWidth()
                .height(56.dp)
                .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = colors.gradientStart, spotColor = colors.gradientStart)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.accentBrush)
                .sparkleBurst({ burst.value }, radius = 0.9f)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                    if (pressed) return@clickable
                    pressed = true
                    haptics.confirm()
                    scope.launch {
                        if (animate) {
                            launch { burst.animateTo(1f, tween(MotionTokens.SPARKLE_MS)) }
                            delay(MotionTokens.SPARKLE_LEAD_MS.toLong())
                        }
                        onClick()
                        delay(MotionTokens.NAV_MS.toLong())
                        burst.snapTo(0f)
                        pressed = false
                    }
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.width(10.dp))
            Icon(ChitalkaIcons.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp).offset(y = 0.5.dp))
        }
    }
}
