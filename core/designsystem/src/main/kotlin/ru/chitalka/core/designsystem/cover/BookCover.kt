package ru.chitalka.core.designsystem.cover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ru.chitalka.core.designsystem.typography.ChitalkaFonts
import java.io.File

/** Насколько крупно подписана обложка: от карусели до крошечных обложек в веере полки. */
enum class CoverSize(
    internal val padding: PaddingValues,
    internal val authorSp: Float?,
    internal val titleSp: Float,
    internal val longTitleSp: Float,
    internal val titleOnly: Boolean,
) {
    /** Карусель на главной. */
    Hero(PaddingValues(horizontal = 14.dp, vertical = 16.dp), 8f, 21f, 17f, false),

    /** Стена обложек на заставке. */
    Wall(PaddingValues(horizontal = 12.dp, vertical = 14.dp), 8f, 16f, 13f, false),

    /** Сетка библиотеки. */
    Grid(PaddingValues(horizontal = 9.dp, vertical = 12.dp), 7f, 13f, 13f, false),

    /** Ряды «Недавно добавлены» и «Читаю сейчас». */
    Small(PaddingValues(horizontal = 7.dp, vertical = 9.dp), null, 10f, 10f, true),

    /** Веер обложек на карточке полки. */
    Tiny(PaddingValues(horizontal = 6.dp, vertical = 8.dp), null, 9f, 9f, true),

    /** Без подписей: размытый фон за каруселью. */
    Bare(PaddingValues(0.dp), null, 0f, 0f, true),
}

/**
 * Обложка книги. Если в файле книги была картинка — показываем её,
 * иначе рисуем обложку в стиле макета: цветной фон, автор сверху, название снизу.
 */
@Composable
fun BookCover(
    title: String,
    author: String?,
    coverPath: String?,
    size: CoverSize,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    /** Свой стиль вместо выбранного по названию: для декоративных обложек заставки. */
    artOverride: CoverArt? = null,
) {
    val art = artOverride ?: remember(title, author) { CoverArt.forBook(title, author) }
    Box(
        modifier
            .clip(shape)
            .drawBehind { art.draw(this) },
    ) {
        if (coverPath != null) {
            AsyncImage(
                model = remember(coverPath) { File(coverPath) },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (size != CoverSize.Bare) {
            CoverLettering(title, author, art, size)
        }
    }
}

@Composable
private fun CoverLettering(title: String, author: String?, art: CoverArt, size: CoverSize) {
    val titleSp = if (title.length > 14) size.longTitleSp else size.titleSp
    Column(
        Modifier
            .fillMaxSize()
            .padding(size.padding),
        verticalArrangement = if (size.titleOnly) Arrangement.Bottom else Arrangement.SpaceBetween,
    ) {
        if (!size.titleOnly && size.authorSp != null) {
            Text(
                text = author.orEmpty().uppercase(),
                style = TextStyle(
                    fontFamily = ChitalkaFonts.Rubik,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = size.authorSp.sp,
                    letterSpacing = 0.16.em,
                ),
                color = art.ink.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = title,
            style = TextStyle(
                fontFamily = if (art.serifTitle) ChitalkaFonts.Playfair else ChitalkaFonts.Rubik,
                fontWeight = FontWeight.Bold,
                fontSize = titleSp.sp,
                lineHeight = (titleSp * 1.04f).sp,
            ),
            color = art.ink,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
