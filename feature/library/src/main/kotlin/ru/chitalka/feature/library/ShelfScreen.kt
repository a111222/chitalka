package ru.chitalka.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import ru.chitalka.core.designsystem.adaptive.LocalWindowWidth
import ru.chitalka.core.designsystem.bookcase.BookSpinePlaceholder
import ru.chitalka.core.designsystem.bookcase.Bookcase
import ru.chitalka.core.designsystem.bookcase.ShelfRow
import ru.chitalka.core.designsystem.bookcase.verticalText
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.model.Book
import java.io.File
import kotlin.math.roundToInt

/** id демонстрационной книги «Сад за рекой»: её можно открыть, пока шкаф пуст. */
const val SAMPLE_BOOK_ID = 0L

private const val BOOKS_PER_SHELF = 7

@Composable
internal fun ShelfRoute(
    onOpenBook: (Long) -> Unit,
    viewModel: ShelfViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Системное окно выбора файлов. Фильтр «*/*»: у FB2 нет общепринятого MIME-типа,
    // формат всё равно определяется по содержимому файла
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.onEvent(ShelfEvent.ImportBooks(uris.map { it.toString() }))
    }
    ShelfScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenBook = onOpenBook,
        onAddBooks = { picker.launch(arrayOf("*/*")) },
    )
}

/** Экран только рисует состояние и отправляет события: никакой логики внутри. */
@Composable
fun ShelfScreen(
    state: ShelfUiState,
    onEvent: (ShelfEvent) -> Unit,
    onOpenBook: (Long) -> Unit,
    onAddBooks: () -> Unit,
) {
    val colors = ReaderTheme.colors
    val haptics = ReaderTheme.haptics
    val isPhone = LocalWindowWidth.current.isPhone
    val openBook: (Long) -> Unit = { id ->
        haptics.bookPicked()
        onOpenBook(id)
    }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    val askDelete: (Book) -> Unit = { book ->
        haptics.confirm()
        bookToDelete = book
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(ShelfEvent.MessageShown)
        }
    }

    Box(Modifier.fillMaxSize()) {
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
                    Header(state.isImporting, onAddBooks)
                    ContinueCard(state.continueReading, onOpen = openBook, onAddBooks = onAddBooks)
                    SortChips(state, onEvent)
                    ShelfBookcase(state.books, onOpen = openBook, onLongPress = askDelete)
                }
            } else {
                Row(
                    Modifier
                        .safeDrawingPadding()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Header(state.isImporting, onAddBooks)
                        ContinueCard(state.continueReading, onOpen = openBook, onAddBooks = onAddBooks, large = true)
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SortChips(state, onEvent)
                        ShelfBookcase(state.books, onOpen = openBook, onLongPress = askDelete)
                    }
                }
            }
        }
        SnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding(),
        )
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Убрать книгу из шкафа?") },
            text = { Text("«${book.title}» и прогресс чтения будут удалены с телефона.") },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(ShelfEvent.DeleteBook(book))
                    bookToDelete = null
                }) { Text("Убрать") }
            },
            dismissButton = { TextButton(onClick = { bookToDelete = null }) { Text("Оставить") } },
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            textContentColor = colors.inkSecondary,
        )
    }
}

@Composable
private fun Header(isImporting: Boolean, onAddBooks: () -> Unit) {
    val colors = ReaderTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Читалка", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Ваш книжный шкаф",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
            )
        }
        if (isImporting) {
            CircularProgressIndicator(Modifier.size(24.dp), color = colors.accent, strokeWidth = 2.dp)
        } else {
            FilledTonalButton(
                onClick = onAddBooks,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.paper, contentColor = colors.accent),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Добавить")
            }
        }
    }
}

@Composable
private fun ContinueCard(book: Book?, onOpen: (Long) -> Unit, onAddBooks: () -> Unit, large: Boolean = false) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BookCover(book, Modifier.size(width = if (large) 96.dp else 72.dp, height = if (large) 144.dp else 108.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        book.author?.let { Text(it, color = colors.inkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        LinearProgressIndicator(
                            progress = { book.progressPercent },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.accent,
                            trackColor = colors.wall,
                        )
                        Text(
                            "Прочитано ${(book.progressPercent * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.inkSecondary,
                        )
                    }
                }
                Button(onClick = { onOpen(book.id) }) { Text("Читать") }
            } else {
                Text(
                    "Шкаф пока пуст. Добавьте книги EPUB или FB2 с телефона, а пока можно открыть пример.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (large) Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddBooks) { Text("Добавить книги") }
                    OutlinedButton(onClick = { onOpen(SAMPLE_BOOK_ID) }) { Text("Пример") }
                }
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

/** Обложка книги, а если её нет — корешок-заглушка с названием. */
@Composable
private fun BookCover(book: Book, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    val coverPath = book.coverPath
    if (coverPath != null) {
        AsyncImage(
            model = File(coverPath),
            contentDescription = "Обложка «${book.title}»",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    } else {
        Box(modifier.clip(shape), contentAlignment = Alignment.Center) {
            BookSpinePlaceholder(Modifier.matchParentSize())
        }
    }
}

@Composable
private fun ShelfBookcase(books: List<Book>, onOpen: (Long) -> Unit, onLongPress: (Book) -> Unit) {
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
                shelf != null -> shelf.forEach { book ->
                    Spine(book.title, onClick = { onOpen(book.id) }, onLongClick = { onLongPress(book) })
                }
                index == 0 && books.isEmpty() -> Spine("Сад за рекой", onClick = { onOpen(SAMPLE_BOOK_ID) })
            }
        }
    }
}

@Composable
private fun Spine(title: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 116.dp)
            .combinedClickable(
                onClickLabel = "Открыть книгу «$title»",
                onLongClickLabel = "Убрать из шкафа",
                onLongClick = onLongClick,
                onClick = onClick,
            ),
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
        ShelfScreen(state = ShelfUiState(isLoading = false), onEvent = {}, onOpenBook = {}, onAddBooks = {})
    }
}
