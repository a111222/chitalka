package ru.chitalka.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import ru.chitalka.core.designsystem.adaptive.LocalWindowWidth
import ru.chitalka.core.designsystem.bookcase.BookCoverPlaceholder
import ru.chitalka.core.designsystem.bookcase.BookSpine
import ru.chitalka.core.designsystem.bookcase.BookcaseMetrics
import ru.chitalka.core.designsystem.bookcase.CarvedBookcase
import ru.chitalka.core.designsystem.bookcase.SpineStyle
import ru.chitalka.core.designsystem.bookcase.spineStyleFor
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.model.Book
import java.io.File
import kotlin.math.roundToInt

/** id демонстрационной книги «Сад за рекой»: её можно открыть, пока шкаф пуст. */
const val SAMPLE_BOOK_ID = 0L

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
    val toggleLights: () -> Unit = {
        haptics.toggle(!state.lightsOn)
        onEvent(ShelfEvent.ToggleLights)
    }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    val askDelete: (Book) -> Unit = { book ->
        haptics.confirm()
        bookToDelete = book
    }
    val bookcase: @Composable () -> Unit = {
        ShelfBookcase(
            books = state.books,
            isLoading = state.isLoading,
            currentBookId = state.continueReading?.id,
            lightsOn = state.lightsOn,
            onOpen = onOpenBook,
            onLongPress = askDelete,
        )
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
                    Header(state, onAddBooks, toggleLights)
                    ContinueCard(state.continueReading, onOpen = openBook, onAddBooks = onAddBooks)
                    SortChips(state, onEvent)
                    bookcase()
                }
            } else {
                Row(
                    Modifier
                        .safeDrawingPadding()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Header(state, onAddBooks, toggleLights)
                        ContinueCard(state.continueReading, onOpen = openBook, onAddBooks = onAddBooks, large = true)
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SortChips(state, onEvent)
                        bookcase()
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
private fun Header(state: ShelfUiState, onAddBooks: () -> Unit, onToggleLights: () -> Unit) {
    val colors = ReaderTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Моя полка", style = MaterialTheme.typography.headlineLarge)
            Text(
                if (state.books.isEmpty()) "Всё доступно офлайн" else "${booksCountLabel(state.books.size)} · всё доступно офлайн",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary,
            )
        }
        if (!colors.isEInk) {
            IconToggleButton(checked = state.lightsOn, onCheckedChange = { onToggleLights() }) {
                Icon(
                    LampIcon,
                    contentDescription = "Подсветка полок",
                    tint = if (state.lightsOn) colors.accent else colors.inkSecondary,
                )
            }
        }
        if (state.isImporting) {
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
        BookCoverPlaceholder(book.title, book.author, spineStyleFor(book.id), modifier)
    }
}

/** Что стоит на полке: книга из базы или демонстрационная, пока шкаф пуст. */
private data class ShelfEntry(val id: Long, val title: String, val author: String?, val book: Book?)

/**
 * Шкаф с книгами. Корешки расставляются по настоящей ширине полки: на планшете
 * помещается больше книг, а сами книги не растягиваются. При первом показе книги
 * опускаются на полки волной, при смене сортировки переезжают на новые места,
 * а книга, которую открывают, сначала приподнимается с полки.
 */
@Composable
private fun ShelfBookcase(
    books: List<Book>,
    isLoading: Boolean,
    currentBookId: Long?,
    lightsOn: Boolean,
    onOpen: (Long) -> Unit,
    onLongPress: (Book) -> Unit,
) {
    val animate = ReaderTheme.animationsEnabled
    val haptics = ReaderTheme.haptics
    val scope = rememberCoroutineScope()
    var pickedId by remember { mutableStateOf<Long?>(null) }
    // Волна «книги встают на полки» играет один раз; при возврате из читалки шкаф уже расставлен
    var introPlayed by rememberSaveable { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { settled = true }
    LaunchedEffect(books.isNotEmpty()) {
        if (books.isNotEmpty()) {
            delay(MotionTokens.BOOK_LAND_MS + MAX_LANDING_WAVE * MotionTokens.BOOK_LAND_STAGGER_MS.toLong())
            introPlayed = true
        }
    }

    val entries = when {
        books.isNotEmpty() -> books.map { ShelfEntry(it.id, it.title, it.author, it) }
        isLoading -> emptyList()
        else -> listOf(ShelfEntry(SAMPLE_BOOK_ID, "Сад за рекой", "Демо", null))
    }
    val pick: (Long) -> Unit = { id ->
        if (pickedId == null) {
            haptics.bookPicked()
            if (animate) {
                pickedId = id
                scope.launch {
                    delay(MotionTokens.BOOK_LIFT_MS + 110L)
                    onOpen(id)
                    delay(MotionTokens.BOOK_LIFT_MS.toLong())
                    pickedId = null
                }
            } else {
                onOpen(id)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        val caseWidth = minOf(maxWidth, BookcaseMetrics.MaxWidth)
        val styles = entries.map { spineStyleFor(it.id) }
        val slots = arrangeSpines(
            widths = styles.map { it.width.value },
            shelfWidth = BookcaseMetrics.shelfWidth(caseWidth).value,
            gap = BookcaseMetrics.SpineGap.value,
        )
        CarvedBookcase(
            shelfCount = shelfCountFor(slots),
            lightsOn = lightsOn,
            modifier = Modifier.width(caseWidth),
        ) {
            entries.forEachIndexed { index, entry ->
                key(entry.id) {
                    ShelfSpine(
                        entry = entry,
                        style = styles[index],
                        slot = slots[index],
                        index = index,
                        isCurrent = entry.id == currentBookId,
                        isPicked = entry.id == pickedId,
                        glowing = entry.id == currentBookId && lightsOn,
                        lands = !introPlayed || settled,
                        wave = !introPlayed,
                        onClick = { pick(entry.id) },
                        onLongClick = entry.book?.let { book -> { onLongPress(book) } },
                    )
                }
            }
        }
    }
}

/** Сколько первых книг участвуют в волне; остальные встают сразу за ними. */
private const val MAX_LANDING_WAVE = 20

@Composable
private fun ShelfSpine(
    entry: ShelfEntry,
    style: SpineStyle,
    slot: SpineSlot,
    index: Int,
    isCurrent: Boolean,
    isPicked: Boolean,
    glowing: Boolean,
    lands: Boolean,
    wave: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val animate = ReaderTheme.animationsEnabled
    val height = BookcaseMetrics.ShelfPitch * style.heightFraction
    // Переезд на новое место при смене сортировки или ширины окна: волной, по 9 мс на книгу
    val moveSpec: AnimationSpec<Dp> = if (animate) {
        tween(MotionTokens.MORPH_MS, delayMillis = index * MotionTokens.BOOK_STAGGER_MS, easing = MotionTokens.Emphasized)
    } else {
        snap()
    }
    val x by animateDpAsState(BookcaseMetrics.ShelfStart + slot.x.dp, moveSpec, label = "spineX")
    val y by animateDpAsState(BookcaseMetrics.shelfFloor(slot.shelf) - height, moveSpec, label = "spineY")
    // Открываемая книга поднимается на 26 dp, читаемая сейчас чуть выдвинута вперёд
    val lift by animateDpAsState(
        targetValue = when {
            isPicked -> (-26).dp
            isCurrent -> (-5).dp
            else -> 0.dp
        },
        animationSpec = if (animate) tween(MotionTokens.BOOK_LIFT_MS, easing = MotionTokens.Emphasized) else snap(),
        label = "spineLift",
    )
    val landing = remember { Animatable(if (animate && lands) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (landing.value < 1f) {
            if (wave) delay(minOf(index, MAX_LANDING_WAVE) * MotionTokens.BOOK_LAND_STAGGER_MS.toLong())
            landing.animateTo(1f, tween(MotionTokens.BOOK_LAND_MS, easing = MotionTokens.Emphasized))
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    BookSpine(
        title = entry.title,
        style = style,
        pressed = pressed,
        glowing = glowing,
        modifier = Modifier
            .offset {
                val drop = LandingDrop * (1f - landing.value)
                IntOffset(x.roundToPx(), (y + lift - drop).roundToPx())
            }
            .graphicsLayer { alpha = landing.value }
            .size(width = style.width, height = height)
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(entry.title, entry.author).joinToString(", ")
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = "Открыть книгу",
                onLongClickLabel = onLongClick?.let { "Убрать из шкафа" },
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    )
}

/** С какой высоты книга опускается на полку. */
private val LandingDrop = 28.dp

/** Лампа из макета: кнопка подсветки полок. */
private val LampIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Lamp", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = addPathNodes(
                "M9 18h6M10 21h4M12 3a6 6 0 0 0-3.6 10.8c.7.5 1.1 1.3 1.1 2.2h5c0-.9.4-1.7 1.1-2.2A6 6 0 0 0 12 3z",
            ),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}

@Preview
@Composable
private fun ShelfScreenPreview() {
    ReaderTheme {
        ShelfScreen(state = ShelfUiState(isLoading = false), onEvent = {}, onOpenBook = {}, onAddBooks = {})
    }
}
