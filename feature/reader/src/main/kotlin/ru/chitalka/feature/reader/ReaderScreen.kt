package ru.chitalka.feature.reader

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.chitalka.core.designsystem.adaptive.LocalWindowWidth
import ru.chitalka.core.designsystem.adaptive.ReadingColumnMaxWidth
import ru.chitalka.core.designsystem.adaptive.WindowWidth
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.designsystem.typography.ReaderFonts
import ru.chitalka.core.model.ReaderSettings
import ru.chitalka.core.model.ReadingPosition
import ru.chitalka.core.model.TocEntry
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ReaderRoute(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReaderScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

private val PagePaddingH = 24.dp
private val PagePaddingV = 8.dp
private val SpreadPadding = 32.dp
private val SpreadGap = 48.dp

/** Что видно сейчас: глава, разрезанная на страницы, и первая видимая страница. */
private data class Spread(val paged: PagedChapter, val first: Int)

/**
 * Экран чтения. Тап по правой трети или свайп влево — вперёд, левая треть или свайп вправо — назад.
 * На планшете вместо одной страницы — разворот из двух. Страницы считаются здесь, а не во ViewModel:
 * их число зависит от шрифта, кегля и размера экрана.
 * 3D-листание с изгибом листа (PageCurl на AGSL) придёт на этапе «Премиум-эффекты».
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
    var footer by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .safeDrawingPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад к шкафу", tint = colors.inkSecondary)
            }
            Text(
                state.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showToc = true }, enabled = state.chapterCount > 0) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Оглавление", tint = colors.inkSecondary)
            }
        }

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val pageWidth = if (isSpread) {
                (maxWidth - SpreadPadding * 2 - SpreadGap) / 2
            } else {
                min(maxWidth, ReadingColumnMaxWidth)
            }
            val density = LocalDensity.current
            val textWidthPx = with(density) { (pageWidth - PagePaddingH * 2).roundToPx() }
            val textHeightPx = with(density) { (maxHeight - PagePaddingV * 2).roundToPx() }
            val paged = rememberPagedChapter(state.chapter, state.settings, state.language, textWidthPx, textHeightPx)

            // Страницы готовы, только если разрезана именно та глава, где стоит читатель
            val visible = paged?.takeIf { it.chapterIndex == state.position.chapterIndex }?.let {
                val page = it.pageFor(state.position.charOffset)
                Spread(it, if (isSpread) page - page % 2 else page)
            }
            // Пока грузится соседняя глава, держим на экране последнюю показанную страницу
            val lastShown = remember { mutableStateOf<Spread?>(null) }
            val shown = visible ?: lastShown.value
            SideEffect { if (visible != null) lastShown.value = visible }

            LaunchedEffect(visible) {
                val spread = visible ?: return@LaunchedEffect
                val pages = spread.paged.pages
                val chapterProgress = (spread.first + step).coerceAtMost(pages.size).toFloat() / pages.size
                val percent = (spread.paged.chapterIndex + chapterProgress) / state.chapterCount.coerceAtLeast(1)
                footer = "Глава ${spread.paged.chapterIndex + 1} из ${state.chapterCount}  ·  ${(percent * 100).roundToInt()}%"
                onEvent(ReaderEvent.PageShown(ReadingPosition(spread.paged.chapterIndex, pages[spread.first].startOffset), percent))
            }

            val turn by rememberUpdatedState { forward: Boolean ->
                val spread = visible
                if (spread != null) {
                    val target = targetPosition(spread, forward, step, state.chapterCount)
                    if (target != null) {
                        haptics.pageTurn()
                        onEvent(ReaderEvent.GoTo(target))
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            when {
                                offset.x > size.width * 2 / 3 -> turn(true)
                                offset.x < size.width / 3 -> turn(false)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        var dragged = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragged = 0f },
                            onHorizontalDrag = { _, dx -> dragged += dx },
                            onDragEnd = { if (abs(dragged) > 48.dp.toPx()) turn(dragged < 0) },
                        )
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                if (state.error != null) {
                    Message(state.error)
                } else if (shown == null) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = colors.accent)
                } else {
                    AnimatedContent(
                        targetState = shown,
                        transitionSpec = {
                            if (!animate) {
                                // E-Ink: мгновенная смена страниц, без промежуточных кадров
                                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            } else {
                                val forward = targetState.paged.chapterIndex > initialState.paged.chapterIndex ||
                                    (targetState.paged.chapterIndex == initialState.paged.chapterIndex && targetState.first > initialState.first)
                                val dir = if (forward) 1 else -1
                                val spec = tween<IntOffset>(MotionTokens.PAGE_SETTLE_MS, easing = MotionTokens.PageSettle)
                                slideInHorizontally(spec) { it * dir } togetherWith slideOutHorizontally(spec) { -it * dir / 3 }
                            }
                        },
                        label = "page",
                    ) { spread ->
                        if (isSpread) {
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .background(colors.paper)
                                    .padding(horizontal = SpreadPadding),
                                horizontalArrangement = Arrangement.spacedBy(SpreadGap),
                            ) {
                                PageView(spread.paged, spread.first, state.chapter, Modifier.weight(1f))
                                PageView(spread.paged, spread.first + 1, state.chapter, Modifier.weight(1f))
                            }
                        } else {
                            Box(Modifier.fillMaxSize().background(colors.paper), contentAlignment = Alignment.TopCenter) {
                                PageView(spread.paged, spread.first, state.chapter, Modifier.width(pageWidth))
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = footer,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
    }

    if (showToc) {
        ModalBottomSheet(onDismissRequest = { showToc = false }, containerColor = colors.paper, contentColor = colors.ink) {
            TableOfContents(state) { entry ->
                showToc = false
                onEvent(ReaderEvent.GoTo(ReadingPosition(entry.chapterIndex, 0)))
            }
        }
    }
}

/** Куда листать: соседняя страница этой главы или граница соседней главы. null — дальше некуда. */
private fun targetPosition(spread: Spread, forward: Boolean, step: Int, chapterCount: Int): ReadingPosition? {
    val chapter = spread.paged.chapterIndex
    val pages = spread.paged.pages
    return if (forward) {
        when {
            spread.first + step <= pages.lastIndex -> ReadingPosition(chapter, pages[spread.first + step].startOffset)
            chapter + 1 < chapterCount -> ReadingPosition(chapter + 1, 0)
            else -> null
        }
    } else {
        when {
            spread.first > 0 -> ReadingPosition(chapter, pages[(spread.first - step).coerceAtLeast(0)].startOffset)
            chapter > 0 -> ReadingPosition(chapter - 1, END_OF_CHAPTER)
            else -> null
        }
    }
}

/**
 * Разрезает загруженную главу на страницы в фоне: длинная глава раскладывается заметное время,
 * а интерфейс в это время не должен замирать. Пока режется новая — возвращается прежний результат.
 */
@Composable
private fun rememberPagedChapter(
    loaded: LoadedChapter?,
    settings: ReaderSettings,
    language: String?,
    width: Int,
    height: Int,
): PagedChapter? {
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
    val paged by produceState<PagedChapter?>(null, loaded, style, width, height) {
        if (loaded == null || width <= 0 || height <= 0) return@produceState
        value = withContext(Dispatchers.Default) {
            paginate(measurer, loaded.chapter.index, chapterText(loaded.chapter), style, width, height)
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
