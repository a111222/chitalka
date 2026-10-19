package ru.chitalka.feature.library

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.chitalka.core.designsystem.component.GradientIconButton
import ru.chitalka.core.designsystem.component.GradientProgress
import ru.chitalka.core.designsystem.component.HeaderIconButton
import ru.chitalka.core.designsystem.component.pressScale
import ru.chitalka.core.designsystem.component.riseIn
import ru.chitalka.core.designsystem.cover.BookCover
import ru.chitalka.core.designsystem.cover.CoverArt
import ru.chitalka.core.designsystem.cover.CoverSize
import ru.chitalka.core.designsystem.icon.ChitalkaIcons
import ru.chitalka.core.designsystem.theme.MotionTokens
import ru.chitalka.core.designsystem.theme.ReaderTheme
import ru.chitalka.core.designsystem.typography.ChitalkaFonts
import ru.chitalka.core.model.Book
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt

/** id демонстрационной книги «Сад за рекой»: её можно открыть, пока библиотека пуста. */
const val SAMPLE_BOOK_ID = 0L

/** Книга в карусели: настоящая из библиотеки или демонстрационная. */
internal data class CarouselBook(
    val id: Long,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val art: CoverArt? = null,
)

private fun Book.toCarousel() = CarouselBook(id, title, author, coverPath)

private val SampleCarouselBook = CarouselBook(SAMPLE_BOOK_ID, "Сад за рекой", "Демо-книга", null, CoverArt.styles[0])

@Composable
internal fun HomeDestination(
    onOpenBook: (Long) -> Unit,
    onOpenShelf: (LibraryFilter) -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberBookPicker { uris -> viewModel.onEvent(LibraryEvent.ImportBooks(uris)) }
    HomeScreen(
        state = state,
        onOpenBook = onOpenBook,
        onOpenShelf = onOpenShelf,
        onSearch = onSearch,
        onOpenSettings = onOpenSettings,
        onAddBooks = picker,
        onMessageShown = { viewModel.onEvent(LibraryEvent.MessageShown) },
    )
}

/**
 * Главная из видео: приветствие, 3D-карусель на размытом фоне текущей обложки,
 * полки с веером обложек, ряды «Недавно добавлены» и «Читаю сейчас», авторы на полке.
 */
@Composable
internal fun HomeScreen(
    state: LibraryUiState,
    onOpenBook: (Long) -> Unit,
    onOpenShelf: (LibraryFilter) -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddBooks: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val colors = ReaderTheme.colors
    val books = state.books
    val carouselBooks = remember(books) {
        HomeSections.carousel(books).map { it.toCarousel() }.ifEmpty { listOf(SampleCarouselBook) }
    }
    val carousel = rememberCarouselState()
    val current by remember(carouselBooks) {
        derivedStateOf { carouselBooks[carousel.currentOf(carouselBooks.size)] }
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth()) {
                HeroBackdrop(current, Modifier.matchParentSize())
                Column {
                    Header(onSearch = onSearch, onOpenSettings = onOpenSettings, modifier = Modifier.statusBarsPadding())
                    Spacer(Modifier.height(12.dp))
                    CoverCarousel(
                        books = carouselBooks,
                        state = carousel,
                        onOpen = { onOpenBook(it.id) },
                        modifier = Modifier.riseIn(150, distance = 0.dp),
                        background = { GiantTitle(current.title) },
                    )
                    Spacer(Modifier.height(10.dp))
                    Crossfade(current, animationSpec = tween(400), label = "heroTitle") { book ->
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                book.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                            Text(book.author.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.inkSoft, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Dots(count = carouselBooks.size, current = carouselBooks.indexOf(current))
                    Spacer(Modifier.height(18.dp))
                }
            }

            Column(
                Modifier
                    .padding(start = 18.dp, end = 18.dp, top = 6.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!state.isLoading && books.isEmpty()) {
                    EmptyLibraryCard(isImporting = state.isImporting, onAddBooks = onAddBooks, modifier = Modifier.riseIn(250))
                } else if (books.isNotEmpty()) {
                    HomeSectionsContent(books, onOpenBook, onOpenShelf)
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

@Composable
private fun HomeSectionsContent(books: List<Book>, onOpenBook: (Long) -> Unit, onOpenShelf: (LibraryFilter) -> Unit) {
    val shelves = remember(books) { HomeSections.shelves(books) }
    val recent = remember(books) { HomeSections.recentlyAdded(books) }
    val reading = remember(books) { HomeSections.readingNow(books) }
    val authors = remember(books) { HomeSections.authors(books) }

    SectionTitle("Мои полки", Modifier.riseIn(250))
    shelves.forEachIndexed { k, shelf ->
        ShelfCard(shelf, onClick = { onOpenShelf(shelf.filter) }, modifier = Modifier.riseIn(320 + k * MotionTokens.RISE_STAGGER_MS))
    }
    SectionTitle("Недавно добавлены", Modifier.padding(top = 10.dp))
    BookRow(recent, onOpenBook, showProgress = false)
    if (reading.isNotEmpty()) {
        SectionTitle("Читаю сейчас", Modifier.padding(top = 10.dp))
        BookRow(reading, onOpenBook, showProgress = true)
    }
    if (authors.isNotEmpty()) AuthorsCard(authors, Modifier.padding(top = 10.dp))
}

@Composable
private fun Header(onSearch: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Row(
        modifier
            .riseIn()
            .fillMaxWidth()
            .padding(start = 22.dp, end = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("${greeting()},", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = colors.inkSoft)
            Text("что почитаем?", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
        }
        HeaderIconButton(ChitalkaIcons.Search, contentDescription = "Поиск по библиотеке", onClick = onSearch)
        HeaderIconButton(ChitalkaIcons.Settings, contentDescription = "Оформление", onClick = onOpenSettings)
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Доброе утро"
    in 12..17 -> "Добрый день"
    in 18..22 -> "Добрый вечер"
    else -> "Доброй ночи"
}

/**
 * Приглушённый фон за каруселью: увеличенная и сильно размытая текущая обложка.
 * При перелистывании фон плавно перетекает в обложку новой книги.
 */
@Composable
private fun HeroBackdrop(book: CarouselBook, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Box(modifier.clipToBounds()) {
        Crossfade(book, animationSpec = tween(MotionTokens.BACKDROP_MS), label = "backdrop", modifier = Modifier.fillMaxSize()) { shown ->
            val art = remember(shown) { shown.art ?: CoverArt.forBook(shown.title, shown.author) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BookCover(
                    title = shown.title,
                    author = shown.author,
                    coverPath = shown.coverPath,
                    size = CoverSize.Bare,
                    artOverride = art,
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.35f
                            scaleY = 1.35f
                            alpha = 0.55f
                        }
                        .blur(42.dp, BlurredEdgeTreatment.Unbounded),
                )
            } else {
                // Android 8–11 не умеет размывать слой: рисуем мягкое пятно главного цвета обложки
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                Brush.radialGradient(
                                    listOf(art.glow.copy(alpha = 0.55f), art.glow.copy(alpha = 0.18f), Color.Transparent),
                                    center = center.copy(y = size.height * 0.4f),
                                    radius = size.maxDimension * 0.75f,
                                ),
                            )
                        },
                )
            }
        }
        // Затемнение: сверху чуть-чуть, книзу фон полностью уходит в цвет экрана
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to colors.background.copy(alpha = 0.35f),
                        0.55f to colors.background.copy(alpha = 0.2f),
                        1f to colors.background,
                    ),
                ),
        )
    }
}

/** Огромное бледное название за каруселью (Playfair 66 sp, 13% белого). */
@Composable
private fun GiantTitle(title: String) {
    Crossfade(title, animationSpec = tween(400), label = "giantTitle", modifier = Modifier.fillMaxWidth()) { text ->
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Text(
                text,
                style = TextStyle(fontFamily = ChitalkaFonts.Playfair, fontWeight = FontWeight.SemiBold, fontSize = 66.sp),
                color = Color.White.copy(alpha = 0.13f),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .offset(y = 150.dp)
                    .wrapContentSize(unbounded = true)
                    .blur(1.5.dp, BlurredEdgeTreatment.Unbounded),
            )
        }
    }
}

/** Точки под каруселью: текущая вытягивается в чёрточку. */
@Composable
private fun Dots(count: Int, current: Int) {
    val colors = ReaderTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 18.dp else 4.dp, tween(400, easing = MotionTokens.Emphasized), label = "dot")
            val alpha by animateFloatAsState(if (selected) 1f else 0.35f, tween(400), label = "dotAlpha")
            Box(
                Modifier
                    .size(width, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.ink.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = ReaderTheme.colors.ink, modifier = modifier.padding(top = 4.dp))
}

/** Карточка полки: название и счётчик слева, веер из трёх обложек справа. */
@Composable
private fun ShelfCard(shelf: Shelf, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(interaction)
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
    ) {
        Column(Modifier.padding(start = 20.dp, top = 26.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(shelf.name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text(HomeSections.booksCount(shelf.count), style = MaterialTheme.typography.bodySmall, color = colors.inkSecondary)
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
                .size(170.dp, 100.dp),
        ) {
            for (j in 0 until 3) {
                val book = shelf.fan.getOrNull(j)
                val fanModifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = -(j * 46).dp)
                    .zIndex(3f - j)
                    .graphicsLayer {
                        rotationZ = -4f * j
                        transformOrigin = TransformOrigin(1f, 1f)
                    }
                    .size(58.dp, (88 - j * 10).dp)
                val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                if (book != null) {
                    BookCover(
                        title = book.title,
                        author = book.author,
                        coverPath = book.coverPath,
                        size = CoverSize.Tiny,
                        shape = shape,
                        modifier = fanModifier.shadow(10.dp, shape, ambientColor = Color.Black, spotColor = Color.Black),
                    )
                } else {
                    // Пустая полка: контуры будущих книг
                    Box(fanModifier.clip(shape).background(colors.track.copy(alpha = 0.55f - 0.12f * j)))
                }
            }
        }
    }
}

/** Ряд из четырёх обложек: «Недавно добавлены» или «Читаю сейчас» (тогда с прогрессом). */
@Composable
private fun BookRow(books: List<Book>, onOpenBook: (Long) -> Unit, showProgress: Boolean) {
    val colors = ReaderTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in 0 until HomeSections.ROW_SIZE) {
            val book = books.getOrNull(i)
            if (book == null) {
                Spacer(Modifier.weight(1f))
                continue
            }
            val interaction = remember(book.id) { MutableInteractionSource() }
            Column(
                Modifier
                    .weight(1f)
                    .pressScale(interaction)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button) { onOpenBook(book.id) },
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                BookCover(
                    title = book.title,
                    author = book.author,
                    coverPath = book.coverPath,
                    size = CoverSize.Small,
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                        .shadow(12.dp, RoundedCornerShape(5.dp), ambientColor = Color.Black, spotColor = Color.Black),
                )
                if (showProgress) {
                    GradientProgress(book.progressPercent)
                    Text(
                        "Прочитано ${(book.progressPercent * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkSecondary,
                    )
                } else {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.labelMedium.copy(lineHeight = 14.sp),
                        color = colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        book.author.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.offset(y = (-4).dp),
                    )
                }
            }
        }
    }
}

private val AuthorPalette = listOf(Color(0xFF4B3B8F), Color(0xFFB5486A), Color(0xFF1F8A7A), Color(0xFF6A4BC4))

/** «Авторы на полке»: блоки с монограммами, самый читаемый автор — в центре и выше всех. */
@Composable
private fun AuthorsCard(authors: List<AuthorOnShelf>, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val order = remember(authors.size) { HomeSections.pyramidOrder(authors.size) }
    val centre = order.indexOf(0)
    Box(
        modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface),
    ) {
        Text(
            "Авторы на полке",
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
        )
        // Блоки заходят друг на друга на 8 dp: сдвигаем ряд, чтобы он остался по центру
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (4 * (order.size - 1)).dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            order.forEachIndexed { position, rank ->
                val author = authors[rank]
                val height = when (abs(position - centre)) {
                    0 -> 118.dp
                    1 -> 104.dp
                    else -> 92.dp
                }
                val isTop = rank == 0
                val shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                Column(
                    Modifier
                        .zIndex(if (isTop) 5f else 3f - abs(position - centre))
                        .offset(x = if (position == 0) 0.dp else (-8 * position).dp)
                        .width(58.dp)
                        .height(height)
                        .shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(shape)
                        .background(
                            if (isTop) {
                                Brush.verticalGradient(listOf(Color(0xFFF4F1EA), Color(0xFFCFCBDF)))
                            } else {
                                SolidColor(AuthorPalette[(rank - 1) % AuthorPalette.size])
                            },
                        )
                        .padding(bottom = 10.dp, start = 2.dp, end = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    val ink = if (isTop) Color(0xFF2A2340) else Color.White
                    Text(
                        author.monogram.toString(),
                        style = TextStyle(fontFamily = ChitalkaFonts.Playfair, fontWeight = FontWeight.Bold, fontSize = 22.sp),
                        color = ink,
                    )
                    Text(
                        author.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = ink.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Пока библиотека пуста: приглашение добавить книги вместо полок и рядов. */
@Composable
internal fun EmptyLibraryCard(isImporting: Boolean, onAddBooks: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Полки пока пустые", style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text(
                if (isImporting) "Добавляю книги…" else "Добавьте EPUB или FB2 с телефона: книги появятся в карусели и на полках. А пока можно открыть демо-книгу.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary,
            )
        }
        GradientIconButton(ChitalkaIcons.Plus, contentDescription = "Добавить книги с телефона", onClick = onAddBooks)
    }
}
