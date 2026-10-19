package ru.chitalka.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.chitalka.core.designsystem.component.FilterPill
import ru.chitalka.core.designsystem.component.GradientIconButton
import ru.chitalka.core.designsystem.component.GradientProgress
import ru.chitalka.core.designsystem.component.HeaderIconButton
import ru.chitalka.core.designsystem.component.pressScale
import ru.chitalka.core.designsystem.component.riseIn
import ru.chitalka.core.designsystem.cover.BookCover
import ru.chitalka.core.designsystem.cover.CoverSize
import ru.chitalka.core.designsystem.icon.ChitalkaIcons
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.model.Book
import kotlin.math.roundToInt

/** Сколько первых обложек сетки появляются волной; остальные — сразу, чтобы прокрутка не дёргалась. */
private const val STAGGERED_ITEMS = 12

@Composable
internal fun LibraryDestination(
    initialFilter: LibraryFilter?,
    onOpenBook: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) { viewModel.applyInitialFilter(initialFilter) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberBookPicker { uris -> viewModel.onEvent(LibraryEvent.ImportBooks(uris)) }
    LibraryScreen(state = state, onEvent = viewModel::onEvent, onOpenBook = onOpenBook, onAddBooks = picker)
}

/** Вкладка «Библиотека»: поиск, фильтры и сетка всех книг с прогрессом. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryScreen(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onOpenBook: (Long) -> Unit,
    onAddBooks: () -> Unit,
) {
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            onEvent(LibraryEvent.MessageShown)
        }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = top + 20.dp, bottom = bottom + 130.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LibraryHeader(state, onAddBooks, Modifier.riseIn(distance = 18.dp))
                    SearchField(
                        query = state.query,
                        onQueryChange = { onEvent(LibraryEvent.ChangeQuery(it)) },
                        modifier = Modifier.riseIn(60, distance = 18.dp),
                    )
                    Row(
                        Modifier
                            .riseIn(120, distance = 18.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LibraryFilter.entries.forEach { filter ->
                            FilterPill(
                                label = filter.label,
                                selected = state.filter == filter,
                                onClick = {
                                    haptics.toggle(true)
                                    onEvent(LibraryEvent.ChangeFilter(filter))
                                },
                            )
                        }
                    }
                }
            }
            if (!state.isLoading && state.shown.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    if (state.books.isEmpty()) {
                        EmptyLibraryCard(isImporting = state.isImporting, onAddBooks = onAddBooks, modifier = Modifier.riseIn(160))
                    } else {
                        Text(
                            if (state.query.isNotBlank()) "Ничего не нашлось" else "На этой полке пока пусто",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkSecondary,
                            modifier = Modifier.padding(top = 24.dp).riseIn(160),
                        )
                    }
                }
            }
            itemsIndexed(state.shown, key = { _, book -> book.id }) { index, book ->
                LibraryTile(
                    book = book,
                    onClick = {
                        haptics.bookPicked()
                        onOpenBook(book.id)
                    },
                    onLongClick = {
                        haptics.confirm()
                        bookToDelete = book
                    },
                    modifier = Modifier
                        .animateItem()
                        .then(if (index < STAGGERED_ITEMS) Modifier.riseIn(160 + index * 45) else Modifier),
                )
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

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            containerColor = colors.surfaceHigh,
            titleContentColor = colors.ink,
            textContentColor = colors.inkSoft,
            title = { Text("Убрать «${book.title}»?") },
            text = { Text("Файл книги, обложка и прогресс чтения удалятся с телефона.") },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(LibraryEvent.DeleteBook(book))
                    bookToDelete = null
                }) { Text("Убрать", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("Оставить", color = colors.ink) }
            },
        )
    }
}

@Composable
private fun LibraryHeader(state: LibraryUiState, onAddBooks: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Библиотека", style = MaterialTheme.typography.headlineLarge, color = colors.ink)
            Text(
                "${HomeSections.booksCount(state.books.size)} · всё доступно без интернета",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary,
            )
        }
        if (state.isImporting) {
            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), color = colors.accent, strokeWidth = 2.dp)
            }
        } else {
            GradientIconButton(ChitalkaIcons.Plus, contentDescription = "Добавить книги с телефона", onClick = onAddBooks)
        }
    }
}

/** Поле поиска: ищет по названию, автору и серии (полнотекстовый индекс). */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(ChitalkaIcons.Search, contentDescription = null, tint = colors.inkSecondary, modifier = Modifier.size(19.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Название, автор или серия", style = MaterialTheme.typography.bodyMedium, color = colors.inkSecondary, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            HeaderIconButton(ChitalkaIcons.Close, contentDescription = "Очистить поиск", onClick = { onQueryChange("") }, tint = colors.inkSecondary)
        }
    }
}

/** Плитка книги в сетке: обложка, название, полоска прогресса и статус. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryTile(book: Book, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val percent = (book.progressPercent * 100).roundToInt()
    val status = when (book.readingState) {
        ReadingState.NotStarted -> "Не начата"
        ReadingState.Finished -> "Прочитана"
        ReadingState.Reading -> "Прочитано $percent%"
    }
    Column(
        modifier
            .pressScale(interaction)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = "Читать",
                onLongClickLabel = "Убрать из библиотеки",
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        BookCover(
            title = book.title,
            author = book.author,
            coverPath = book.coverPath,
            size = CoverSize.Grid,
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .shadow(14.dp, RoundedCornerShape(6.dp), ambientColor = Color.Black, spotColor = Color.Black),
        )
        Text(
            book.title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        GradientProgress(book.progressPercent, Modifier.offset(y = (-2).dp))
        Text(status, style = MaterialTheme.typography.labelSmall, color = colors.inkSecondary, modifier = Modifier.offset(y = (-3).dp))
    }
}
