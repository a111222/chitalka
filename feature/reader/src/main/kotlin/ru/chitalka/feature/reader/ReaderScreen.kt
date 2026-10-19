package ru.chitalka.feature.reader

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.chitalka.core.designsystem.adaptive.LocalWindowWidth
import ru.chitalka.core.designsystem.adaptive.ReadingColumnMaxWidth
import ru.chitalka.core.designsystem.adaptive.WindowWidth
import ru.chitalka.core.designsystem.component.glassSource
import ru.chitalka.core.designsystem.component.rememberGlassState
import ru.chitalka.core.designsystem.component.riseIn
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.designsystem.typography.ChitalkaFonts
import ru.chitalka.core.designsystem.typography.ReaderFonts
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReadingPosition
import ru.chitalka.core.model.TocEntry
import kotlin.math.roundToInt

@Composable
internal fun ReaderRoute(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReaderScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

private val PagePaddingH = 26.dp
private val PagePaddingV = 8.dp
private val SpreadPadding = 32.dp
private val SpreadGap = 48.dp
private val SpineShadow = 28.dp

/**
 * Читалка из видео: тёмная страница, сверху «назад», оглавление и «Аа», снизу кольцо прогресса
 * и номер страницы. Листание как в Apple Books: лист берут за уголок, он изгибается за пальцем
 * и переворачивается, показывая оборот (см. [PageTurner], [Fold]). Тап по правой трети — вперёд,
 * по левой — назад: лист сам переворачивается от нижнего уголка.
 * «Аа» открывает шторку из матового стекла: шрифт, размер, интервал, оттенок страницы.
 * На планшете — раскрытая книга из двух страниц: переворачивается правый лист, на его обороте следующая левая. Страницы считаются здесь, а не во ViewModel:
 * их число зависит от шрифта, кегля и размера экрана.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onEvent: (ReaderEvent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics
    val animate = ReaderTheme.animationsEnabled
    val isSpread = LocalWindowWidth.current == WindowWidth.Expanded
    // В развороте листаем сразу по две страницы
    val step = if (isSpread) 2 else 1
    var showToc by remember { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) }
    var pageLabel by remember { mutableStateOf("") }
    var bookPercent by remember { mutableFloatStateOf(0f) }
    val glassState = rememberGlassState()
    val scope = rememberCoroutineScope()
    val turner = remember { PageTurner(scope) }

    BackHandler(enabled = sheetOpen) { sheetOpen = false }

    Box(Modifier.fillMaxSize().background(colors.paper)) {
        Column(
            Modifier
                .fillMaxSize()
                .glassSource(glassState)
                .background(colors.paper)
                .safeDrawingPadding(),
        ) {
            ReaderTopBar(
                onBack = onBack,
                onContents = { showToc = true },
                contentsEnabled = state.chapterCount > 0,
                onAppearance = { sheetOpen = true },
            )

            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                val halfWidth = maxWidth / 2
                val pageWidth = if (isSpread) {
                    halfWidth - SpreadPadding - SpreadGap / 2
                } else {
                    min(maxWidth, ReadingColumnMaxWidth)
                }
                val screenDensity = LocalDensity.current
                val widthPx = constraints.maxWidth.toFloat()
                val heightPx = constraints.maxHeight.toFloat()
                val textWidthPx = with(screenDensity) { (pageWidth - PagePaddingH * 2).roundToPx() }
                val textHeightPx = with(screenDensity) { (maxHeight - PagePaddingV * 2).roundToPx() }
                val chapters = state.nearby.ifEmpty { listOfNotNull(state.chapter) }
                val loadedByIndex = remember(chapters) { chapters.associateBy { it.chapter.index } }
                val pagedMap = rememberPagedChapters(
                    chapters, state.position.chapterIndex, state.settings, state.language, textWidthPx, textHeightPx,
                )

                // Страницы готовы, только если разрезана именно та глава, где стоит читатель
                val visible = pagedMap[state.position.chapterIndex]?.let {
                    val page = it.pageFor(state.position.charOffset)
                    Spread(it, if (isSpread) page - page % 2 else page)
                }
                // Пока грузится соседняя глава, держим на экране последнюю показанную страницу
                val lastShown = remember { mutableStateOf<Spread?>(null) }
                val shown = visible ?: lastShown.value
                SideEffect {
                    if (visible != null) lastShown.value = visible
                    turner.visible = visible
                    turner.area = Size(widthPx, heightPx)
                    turner.spineX = if (isSpread) widthPx / 2 else 0f
                    turner.pageWidth = if (isSpread) widthPx / 2 else widthPx
                    turner.animate = animate
                    turner.onEvent = onEvent
                    turner.onTurned = haptics::pageTurn
                    turner.neighbourOf = { spread, forward -> neighbourSpread(spread, forward, step, pagedMap) }
                    turner.fallbackTarget = { spread, forward -> targetPosition(spread, forward, step, state.chapterCount) }
                }

                LaunchedEffect(visible) {
                    val spread = visible ?: return@LaunchedEffect
                    val pages = spread.paged.pages
                    val chapterProgress = (spread.first + step).coerceAtMost(pages.size).toFloat() / pages.size
                    val percent = (spread.paged.chapterIndex + chapterProgress) / state.chapterCount.coerceAtLeast(1)
                    val lastPage = (spread.first + step).coerceAtMost(pages.size)
                    pageLabel = "Глава ${spread.paged.chapterIndex + 1}  ·  стр. $lastPage из ${pages.size}"
                    bookPercent = percent
                    onEvent(ReaderEvent.PageShown(ReadingPosition(spread.paged.chapterIndex, pages[spread.first].startOffset), percent))
                }
                LaunchedEffect(visible, turner.turn) {
                    turner.onShown(visible)
                    if (turner.turn?.committed == true) {
                        delay(MotionTokens.PAGE_SETTLE_MS.toLong())
                        turner.forceSettle()
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .riseIn(80)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                when {
                                    offset.x > size.width * 2 / 3 -> turner.tap(true)
                                    offset.x < size.width / 3 -> turner.tap(false)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            val velocity = VelocityTracker()
                            detectDragGestures(
                                onDragStart = { at ->
                                    velocity.resetTracking()
                                    turner.dragStart(at)
                                },
                                onDrag = { change, _ ->
                                    velocity.addPosition(change.uptimeMillis, change.position)
                                    turner.drag(change.position)
                                },
                                onDragEnd = { turner.dragEnd(velocity.calculateVelocity().x) },
                                onDragCancel = { turner.dragEnd(0f) },
                            )
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val turn = turner.turn
                    val backPaper = colors.paper.copy(alpha = 0.88f)
                    when {
                        state.error != null -> Message(state.error)
                        shown == null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = colors.accent)
                        isSpread && turn != null -> {
                            // Слева лежит левая страница прежнего разворота, справа под листом — правая следующего.
                            // Сам лист: лицо — правая страница прежнего, оборот — левая следующего.
                            PageHalf(turn.from, 0, loadedByIndex, left = true, halfWidth, Modifier.align(Alignment.TopStart))
                            PageHalf(turn.to, 1, loadedByIndex, left = false, halfWidth, Modifier.align(Alignment.TopEnd))
                            PageHalf(
                                turn.from, 1, loadedByIndex, left = false, halfWidth,
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .pageCurl({ turner.fold() }, Offset(widthPx / 2, 0f), CurlPart.FrontOnly, backPaper),
                            )
                            PageHalf(
                                turn.to, 0, loadedByIndex, left = true, halfWidth,
                                Modifier
                                    .align(Alignment.TopStart)
                                    .pageCurl({ turner.fold() }, Offset.Zero, CurlPart.BackOnly, backPaper),
                            )
                        }
                        isSpread -> {
                            PageHalf(shown, 0, loadedByIndex, left = true, halfWidth, Modifier.align(Alignment.TopStart))
                            PageHalf(shown, 1, loadedByIndex, left = false, halfWidth, Modifier.align(Alignment.TopEnd))
                        }
                        turn != null -> {
                            SinglePage(turn.to, loadedByIndex, pageWidth)
                            SinglePage(
                                turn.from, loadedByIndex, pageWidth,
                                Modifier.pageCurl({ turner.fold() }, Offset.Zero, CurlPart.FrontWithBack, backPaper),
                            )
                        }
                        else -> SinglePage(shown, loadedByIndex, pageWidth)
                    }
                }
            }

            ReaderBottomBar(
                percent = bookPercent,
                label = pageLabel,
                modifier = Modifier.riseIn(260),
            )
        }

        // Затемнение под шторкой: тап по нему закрывает шторку
        AnimatedVisibility(
            visible = sheetOpen,
            enter = fadeIn(tween(MotionTokens.PANELS_MS)),
            exit = fadeOut(tween(MotionTokens.PANELS_MS)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x400A0A10))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { sheetOpen = false },
            )
        }
        AnimatedVisibility(
            visible = sheetOpen,
            enter = slideInVertically(tween(MotionTokens.SHEET_MS, easing = MotionTokens.Sheet)) { it } + fadeIn(tween(MotionTokens.PANELS_MS)),
            exit = slideOutVertically(tween(MotionTokens.PANELS_MS, easing = MotionTokens.Emphasized)) { it } + fadeOut(tween(MotionTokens.PANELS_MS)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            AppearanceSheet(
                settings = state.settings,
                glassState = glassState,
                onEvent = onEvent,
                onContents = {
                    sheetOpen = false
                    showToc = true
                },
            )
        }
    }

    if (showToc) {
        ModalBottomSheet(onDismissRequest = { showToc = false }, containerColor = colors.surface, contentColor = colors.ink) {
            TableOfContents(state) { entry ->
                showToc = false
                onEvent(ReaderEvent.GoTo(ReadingPosition(entry.chapterIndex, 0)))
            }
        }
    }
}

/** Страница телефона: лист во всю ширину области с непрозрачной бумагой — верхний лист закрывает нижний. */
@Composable
private fun SinglePage(spread: Spread, loaded: Map<Int, LoadedChapter>, pageWidth: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(ReaderTheme.colors.paper),
        contentAlignment = Alignment.TopCenter,
    ) {
        PageView(spread.paged, spread.first, loaded[spread.paged.chapterIndex], Modifier.width(pageWidth))
    }
}

/**
 * Половина разворота на планшете: левая ([left]) или правая страница. У корешка бумага чуть темнее,
 * как у раскрытой книги, где страницы уходят в переплёт.
 */
@Composable
private fun PageHalf(
    spread: Spread,
    page: Int,
    loaded: Map<Int, LoadedChapter>,
    left: Boolean,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = ReaderTheme.colors
    Box(
        modifier
            .width(width)
            .fillMaxHeight()
            .background(colors.paper)
            .drawWithContent {
                drawContent()
                val fall = SpineShadow.toPx()
                val edge = if (left) size.width else 0f
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.22f),
                        1f to Color.Transparent,
                        startX = edge,
                        endX = if (left) edge - fall else edge + fall,
                    ),
                )
            }
            .padding(
                start = if (left) SpreadPadding else SpreadGap / 2,
                end = if (left) SpreadGap / 2 else SpreadPadding,
            ),
    ) {
        PageView(spread.paged, spread.first + page, loaded[spread.paged.chapterIndex], Modifier.fillMaxWidth())
    }
}

/**
 * Разрезает главы на страницы в фоне: длинная глава раскладывается заметное время,
 * а интерфейс в это время не должен замирать. Сначала текущая глава, потом соседи —
 * их страницы нужны, чтобы лист можно было согнуть через границу главы.
 * Готовые раскладки хранятся, пока не поменялись шрифт, кегль или размер экрана.
 */
@Composable
private fun rememberPagedChapters(
    chapters: List<LoadedChapter>,
    current: Int,
    settings: ReaderSettings,
    language: String?,
    width: Int,
    height: Int,
): Map<Int, PagedChapter> {
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val style = remember(settings.font, settings.fontSizeSp, settings.lineSpacing, language) {
        TextStyle(
            fontFamily = ReaderFonts.familyFor(settings.font),
            fontSize = settings.fontSizeSp.sp,
            lineHeight = settings.lineSpacing.multiplier.em,
            // Переносы по словарю языка книги: ровный край без «рек» из пробелов
            hyphens = Hyphens.Auto,
            lineBreak = LineBreak.Paragraph,
            localeList = language?.let { runCatching { LocaleList(it) }.getOrNull() },
        )
    }
    val paged = remember(style, width, height) { mutableStateMapOf<Int, PagedChapter>() }
    LaunchedEffect(chapters, current, paged) {
        if (width <= 0 || height <= 0) return@LaunchedEffect
        val wanted = chapters.map { it.chapter.index }.toSet()
        (paged.keys - wanted).forEach { paged.remove(it) }
        for (loaded in chapters.sortedBy { if (it.chapter.index == current) 0 else 1 }) {
            val index = loaded.chapter.index
            if (index in paged) continue
            paged[index] = withContext(Dispatchers.Default) {
                paginate(measurer, index, chapterText(loaded.chapter, ChitalkaFonts.Playfair), style, width, height)
            }
        }
    }
    return paged
}

@Composable
private fun PageView(paged: PagedChapter, pageIndex: Int, loaded: LoadedChapter?, modifier: Modifier = Modifier) {
    val ink = ReaderTheme.colors.ink
    val page = paged.pages.getOrNull(pageIndex)
    val image = loaded?.takeIf { it.chapter.index == paged.chapterIndex }?.image
    Box(
        modifier
            .fillMaxHeight()
            .padding(horizontal = PagePaddingH, vertical = PagePaddingV),
    ) {
        when {
            page == null -> Unit
            image != null -> ChapterImage(image)
            else -> Canvas(
                Modifier
                    .fillMaxSize()
                    .clipToBounds(),
            ) {
                // Рисуем общую раскладку главы, сдвинутую так, чтобы в окне оказались строки этой страницы
                clipRect(bottom = page.bottom - page.top) {
                    translate(top = -page.top) { drawText(paged.layout, color = ink) }
                }
            }
        }
    }
}

@Composable
private fun ChapterImage(bytes: ByteArray) {
    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
    if (bitmap != null) {
        Image(bitmap, contentDescription = "Иллюстрация", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = ReaderTheme.colors.inkSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TableOfContents(state: ReaderUiState, onOpen: (TocEntry) -> Unit) {
    val colors = ReaderTheme.colors
    // В книге без оглавления показываем просто номера глав
    val entries = state.toc.ifEmpty { (0 until state.chapterCount).map { TocEntry("Глава ${it + 1}", 0, it) } }
    val current = entries.indexOfLast { it.chapterIndex <= state.position.chapterIndex }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (current - 2).coerceAtLeast(0))
    Text(
        "Оглавление",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
    LazyColumn(state = listState) {
        itemsIndexed(entries) { index, entry ->
            val isCurrent = index == current
            Text(
                entry.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) colors.accent else colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(entry) }
                    .padding(start = 24.dp + 16.dp * entry.level.coerceAtMost(4), end = 24.dp, top = 12.dp, bottom = 12.dp),
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReaderScreenPreview() {
    ReaderTheme {
        ReaderScreen(
            state = ReaderUiState(
                title = SampleBook.TITLE,
                chapterCount = 1,
                chapter = LoadedChapter(SampleBook.chapter),
                isLoading = false,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

/** Проценты для подписи кольца: 0–100 без дробей. */
internal fun percentLabel(percent: Float): String = "${(percent * 100).roundToInt().coerceIn(0, 100)}%"
