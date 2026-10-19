package ru.chitalka.feature.bookmarks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.chitalka.core.designsystem.component.pressScale
import ru.chitalka.core.designsystem.component.riseIn
import ru.chitalka.core.designsystem.cover.BookCover
import ru.chitalka.core.designsystem.cover.CoverArt
import ru.chitalka.core.designsystem.cover.CoverSize
import ru.chitalka.core.designsystem.icon.ChitalkaIcons
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.designsystem.typography.ChitalkaFonts
import ru.chitalka.core.model.DemoBook
import ru.chitalka.core.model.ReadingPosition
import java.time.LocalDate

/** Сколько первых карточек появляются волной: дальше по списку — сразу. */
private const val STAGGERED_ITEMS = 8

@Composable
internal fun BookmarksDestination(
    onOpenPlace: (bookId: Long, position: ReadingPosition) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BookmarksScreen(state = state, onEvent = viewModel::onEvent, onOpenPlace = onOpenPlace)
}

/**
 * Вкладка «Закладки» из макета v3: переключатель «Цитаты · Закладки» и карточки с обложкой.
 * Тап открывает книгу на этом месте, смахивание влево удаляет (с «Вернуть» внизу).
 */
@Composable
internal fun BookmarksScreen(
    state: BookmarksUiState,
    onEvent: (BookmarksEvent) -> Unit,
    onOpenPlace: (bookId: Long, position: ReadingPosition) -> Unit,
) {
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics
    val snackbar = remember { SnackbarHostState() }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val today = remember { LocalDate.now() }

    LaunchedEffect(state.undo) {
        val removed = state.undo ?: return@LaunchedEffect
        snackbar.currentSnackbarData?.dismiss()
        val result = snackbar.showSnackbar(removed.message, actionLabel = "Вернуть", withDismissAction = false)
        onEvent(if (result == SnackbarResult.ActionPerformed) BookmarksEvent.Undo else BookmarksEvent.UndoDismissed)
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = top + 20.dp, bottom = bottom + 130.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp),
        ) {
            item(key = "header") {
                Text(
                    "Закладки и цитаты",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.ink,
                    modifier = Modifier.riseIn(distance = 18.dp),
                )
            }
            item(key = "tabs") {
                MarksTabs(
                    selected = state.tab,
                    counts = mapOf(MarksTab.Quotes to state.quotes.size, MarksTab.Bookmarks to state.bookmarks.size),
                    onSelect = { tab ->
                        if (tab != state.tab) {
                            haptics.toggle(true)
                            onEvent(BookmarksEvent.SelectTab(tab))
                        }
                    },
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 4.dp)
                        .riseIn(60, distance = 18.dp),
                )
            }
            when {
                state.isLoading -> Unit
                state.tab == MarksTab.Quotes && state.quotes.isEmpty() -> item(key = "empty-quotes") {
                    EmptyMarks(
                        icon = ChitalkaIcons.Quote,
                        title = "Пока ни одной цитаты",
                        text = "В читалке нажмите на слово и подержите, а потом проведите пальцем: фраза закрасится маркером и появится здесь.",
                    )
                }
                state.tab == MarksTab.Bookmarks && state.bookmarks.isEmpty() -> item(key = "empty-bookmarks") {
                    EmptyMarks(
                        icon = ChitalkaIcons.Bookmark,
                        title = "Закладок пока нет",
                        text = "Нажмите на ленточку вверху страницы в читалке, чтобы вернуться к этому месту в один тап.",
                    )
                }
                state.tab == MarksTab.Quotes -> itemsIndexed(state.quotes, key = { _, q -> "q${q.item.id}" }) { index, quote ->
                    val highlight = quote.item
                    SwipeToDelete(
                        onDelete = {
                            haptics.confirm()
                            onEvent(BookmarksEvent.DeleteQuote(highlight))
                        },
                        modifier = Modifier
                            .animateItem()
                            .riseIn(if (index < STAGGERED_ITEMS) 120 + index * MotionTokens.RISE_STAGGER_MS else 0),
                    ) {
                        MarkCard(
                            bookId = highlight.bookId,
                            bookTitle = quote.bookTitle,
                            bookAuthor = quote.bookAuthor,
                            coverPath = quote.coverPath,
                            text = "«${highlight.text}»",
                            textStyle = QuoteStyle,
                            snippet = null,
                            meta = "${quote.bookTitle} · ${percentText(highlight.percent)} · ${relativeDay(highlight.createdAt, today)}",
                            dot = Color(highlight.color.argb),
                            description = "Цитата из «${quote.bookTitle}»",
                            onClick = { onOpenPlace(highlight.bookId, highlight.position) },
                        )
                    }
                }
                else -> itemsIndexed(state.bookmarks, key = { _, b -> "b${b.item.id}" }) { index, mark ->
                    val bookmark = mark.item
                    SwipeToDelete(
                        onDelete = {
                            haptics.confirm()
                            onEvent(BookmarksEvent.DeleteBookmark(bookmark))
                        },
                        modifier = Modifier
                            .animateItem()
                            .riseIn(if (index < STAGGERED_ITEMS) 120 + index * MotionTokens.RISE_STAGGER_MS else 0),
                    ) {
                        MarkCard(
                            bookId = bookmark.bookId,
                            bookTitle = mark.bookTitle,
                            bookAuthor = mark.bookAuthor,
                            coverPath = mark.coverPath,
                            text = bookmark.label,
                            textStyle = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            snippet = bookmark.snippet.takeIf { it.isNotBlank() },
                            meta = "${mark.bookTitle} · ${percentText(bookmark.percent)} · ${relativeDay(bookmark.createdAt, today)}",
                            dot = colors.gradientStart,
                            description = "Закладка в «${mark.bookTitle}»",
                            onClick = { onOpenPlace(bookmark.bookId, bookmark.position) },
                        )
                    }
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        )
    }
}

private val QuoteStyle = TextStyle(fontFamily = ChitalkaFonts.Playfair, fontSize = 15.sp, lineHeight = 22.sp)

/**
 * «Цитаты · Закладки»: две половины на тёмной подложке, выбранная залита фирменным градиентом.
 * Заливка не перескакивает, а переезжает под выбранную половину.
 */
@Composable
private fun MarksTabs(
    selected: MarksTab,
    counts: Map<MarksTab, Int>,
    onSelect: (MarksTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReaderTheme.colors
    val position by animateFloatAsState(
        selected.ordinal.toFloat(),
        tween(MotionTokens.CAROUSEL_MS, easing = MotionTokens.Emphasized),
        label = "tabs",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(colors.surface)
            .padding(4.dp)
            .drawBehind {
                val tabs = MarksTab.entries.size
                val width = size.width / tabs
                drawRoundRect(
                    colors.accentBrush,
                    topLeft = Offset(width * position, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
            },
    ) {
        Row(Modifier.fillMaxSize()) {
            MarksTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val count = counts[tab] ?: 0
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .semantics { this.selected = isSelected },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tab.label, style = MaterialTheme.typography.labelLarge, color = colors.ink)
                        if (count > 0) {
                            Text(
                                "  $count",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) colors.ink.copy(alpha = 0.75f) else colors.inkSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Карточка цитаты или закладки: обложка книги, текст и строка «книга · процент · когда» с цветной точкой. */
@Composable
private fun MarkCard(
    bookId: Long,
    bookTitle: String,
    bookAuthor: String?,
    coverPath: String?,
    text: String,
    textStyle: TextStyle,
    snippet: String?,
    meta: String,
    dot: Color,
    description: String,
    onClick: () -> Unit,
) {
    val colors = ReaderTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val art = remember(bookId) { if (bookId == DemoBook.ID) CoverArt.styles[0] else null }
    Row(
        Modifier
            .pressScale(interaction)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BookCover(
            title = bookTitle,
            author = bookAuthor,
            coverPath = coverPath,
            size = CoverSize.Tiny,
            artOverride = art,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(4.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .size(width = 44.dp, height = 64.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, style = textStyle, color = colors.ink, maxLines = 6, overflow = TextOverflow.Ellipsis)
            if (snippet != null) {
                Text(snippet, style = MaterialTheme.typography.bodySmall, color = colors.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dot),
                )
                Text(meta, style = MaterialTheme.typography.labelMedium, color = colors.inkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Смахнуть карточку влево — удалить. Под карточкой проступает розовая подложка с корзиной. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = ReaderTheme.colors
    val currentOnDelete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) currentOnDelete()
            value == SwipeToDismissBoxValue.EndToStart
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            val reveal = if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) 1f else 0f
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .drawBehind { drawRect(colors.accentBrush, alpha = 0.85f * reveal) }
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(ChitalkaIcons.Delete, contentDescription = null, tint = Color.White.copy(alpha = reveal), modifier = Modifier.size(22.dp))
            }
        },
    ) {
        content()
    }
}

@Composable
private fun EmptyMarks(icon: ImageVector, title: String, text: String) {
    val colors = ReaderTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, start = 18.dp, end = 18.dp)
            .riseIn(120),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(30.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.inkSecondary, textAlign = TextAlign.Center)
    }
}
