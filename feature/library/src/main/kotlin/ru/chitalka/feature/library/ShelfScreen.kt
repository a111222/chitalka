package ru.chitalka.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.chitalka.core.designsystem.adaptive.LocalWindowWidth
import ru.chitalka.core.designsystem.bookcase.BookSpinePlaceholder
import ru.chitalka.core.designsystem.bookcase.Bookcase
import ru.chitalka.core.designsystem.bookcase.ShelfRow
import ru.chitalka.core.designsystem.bookcase.verticalText
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.model.Book

/** id демонстрационной книги «Сад за рекой», пока импорт не готов. */
const val SAMPLE_BOOK_ID = 0L

private const val BOOKS_PER_SHELF = 7

@Composable
internal fun ShelfRoute(
    onOpenBook: (Long) -> Unit,
    viewModel: ShelfViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShelfScreen(state = state, onEvent = viewModel::onEvent, onOpenBook = onOpenBook)
}

/** Экран только рисует состояние и отправляет события: никакой логики внутри. */
@Composable
fun ShelfScreen(
    state: ShelfUiState,
    onEvent: (ShelfEvent) -> Unit,
    onOpenBook: (Long) -> Unit,
) {
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics
    val isPhone = LocalWindowWidth.current.isPhone
    val openBook: (Long) -> Unit = { id ->
        haptics.bookPicked()
        onOpenBook(id)
    }

    Surface(color = colors.background, contentColor = colors.ink, modifier = Modifier.fillMaxSize()) {
        // Телефон: карточка «Продолжить» строкой над шкафом. Планшет: она вырастает в боковую панель.
        if (isPhone) {
            Column(
                Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Header()
                ContinueCard(state.continueReading, onOpen = openBook)
                SortChips(state, onEvent)
                ShelfBookcase(state.books, onOpen = openBook)
            }
        } else {
            Row(
                Modifier
                    .safeDrawingPadding()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Header()
                    ContinueCard(state.continueReading, onOpen = openBook, large = true)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SortChips(state, onEvent)
                    ShelfBookcase(state.books, onOpen = openBook)
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column {
        Text("Читалка", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Ваш книжный шкаф",
            style = MaterialTheme.typography.bodyMedium,
            color = ReaderTheme.colors.inkSecondary,
        )
    }
}

@Composable
private fun ContinueCard(book: Book?, onOpen: (Long) -> Unit, large: Boolean = false) {
    val colors = ReaderTheme.colors
    Surface(
        color = colors.paper,
        contentColor = colors.ink,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Продолжить чтение", style = MaterialTheme.typography.labelLarge, color = colors.inkSecondary)
            if (book != null) {
                Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                book.author?.let { Text(it, color = colors.inkSecondary) }
                LinearProgressIndicator(
                    progress = { book.progressPercent },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accent,
                    trackColor = colors.wall,
                )
                Button(onClick = { onOpen(book.id) }) { Text("Читать") }
            } else {
                Text(
                    "Шкаф пока пуст. Импорт EPUB и FB2 появится на следующем этапе, а пока можно открыть пример.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (large) Spacer(Modifier.height(8.dp))
                Button(onClick = { onOpen(SAMPLE_BOOK_ID) }) { Text("Открыть «Сад за рекой»") }
            }
        }
    }
}

@Composable
private fun SortChips(state: ShelfUiState, onEvent: (ShelfEvent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShelfSort.entries.forEach { sort ->
            FilterChip(
                selected = state.sort == sort,
                onClick = { onEvent(ShelfEvent.ChangeSort(sort)) },
                label = { Text(sort.label) },
            )
        }
    }
}

@Composable
private fun ShelfBookcase(books: List<Book>, onOpen: (Long) -> Unit) {
    val shelves = books.chunked(BOOKS_PER_SHELF)
    Bookcase(
        shelfCount = maxOf(3, shelves.size),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp)),
    ) { index ->
        ShelfRow {
            val shelf = shelves.getOrNull(index)
            when {
                shelf != null -> shelf.forEach { book -> Spine(book.title, onClick = { onOpen(book.id) }) }
                index == 0 && books.isEmpty() -> Spine("Сад за рекой", onClick = { onOpen(SAMPLE_BOOK_ID) })
            }
        }
    }
}

@Composable
private fun Spine(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 116.dp)
            .clickable(onClickLabel = "Открыть книгу «$title»", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BookSpinePlaceholder(Modifier.matchParentSize())
        Text(
            text = title,
            modifier = Modifier.verticalText(),
            color = if (ReaderTheme.colors.isEInk) ReaderTheme.colors.ink else ReaderTheme.colors.paper,
            fontFamily = FontFamily.Serif,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun ShelfScreenPreview() {
    ReaderTheme {
        ShelfScreen(state = ShelfUiState(isLoading = false), onEvent = {}, onOpenBook = {})
    }
}
