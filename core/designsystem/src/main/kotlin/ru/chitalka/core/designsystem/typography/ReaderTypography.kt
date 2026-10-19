package ru.chitalka.core.designsystem.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.chitalka.core.designsystem.R
import ru.chitalka.core.model.ReaderFont

/** Шрифты интерфейса из макета v3. Лежат в APK (лицензия OFL, см. core/designsystem/OFL.txt). */
object ChitalkaFonts {
    /** Rubik: весь интерфейс — кнопки, подписи, заголовки разделов. */
    val Rubik = FontFamily(
        Font(R.font.rubik_regular, FontWeight.Normal),
        Font(R.font.rubik_medium, FontWeight.Medium),
        Font(R.font.rubik_semibold, FontWeight.SemiBold),
        Font(R.font.rubik_bold, FontWeight.Bold),
    )

    /** Playfair Display: названия книг на обложках, крупные цитаты, гигантское название за каруселью. */
    val Playfair = FontFamily(
        Font(R.font.playfair_medium, FontWeight.Medium),
        Font(R.font.playfair_semibold, FontWeight.SemiBold),
        Font(R.font.playfair_bold, FontWeight.Bold),
    )
}

/**
 * Шрифты для чтения: все пять лежат в APK (OFL), у каждого есть кириллица, курсив для цитат
 * и полужирный для заголовков. Шрифт интерфейса и шрифт чтения — разные токены.
 */
object ReaderFonts {
    private val Literata = FontFamily(
        Font(R.font.literata_regular, FontWeight.Normal),
        Font(R.font.literata_regular_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.literata_semibold, FontWeight.SemiBold),
    )
    private val PtSerif = FontFamily(
        Font(R.font.ptserif_regular, FontWeight.Normal),
        Font(R.font.ptserif_regular_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.ptserif_bold, FontWeight.Bold),
    )
    private val Lora = FontFamily(
        Font(R.font.lora_regular, FontWeight.Normal),
        Font(R.font.lora_regular_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.lora_semibold, FontWeight.SemiBold),
    )
    private val Cormorant = FontFamily(
        Font(R.font.cormorant_medium, FontWeight.Normal),
        Font(R.font.cormorant_medium_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.cormorant_bold, FontWeight.Bold),
    )
    private val Manrope = FontFamily(
        Font(R.font.manrope_regular, FontWeight.Normal),
        Font(R.font.manrope_semibold, FontWeight.SemiBold),
    )

    fun familyFor(font: ReaderFont): FontFamily = when (font) {
        ReaderFont.Literata -> Literata
        ReaderFont.PtSerif -> PtSerif
        ReaderFont.Lora -> Lora
        ReaderFont.Cormorant -> Cormorant
        ReaderFont.Manrope -> Manrope
    }
}

private fun rubik(size: Int, weight: FontWeight, lineHeight: Float = 1.3f) = TextStyle(
    fontFamily = ChitalkaFonts.Rubik,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
)

/** Типографика интерфейса: Rubik везде, размеры как в макете (24 / 19 / 17 / 15 / 13 / 12 / 11). */
internal val ChitalkaTypography: Typography = Typography(
    displayLarge = TextStyle(fontFamily = ChitalkaFonts.Playfair, fontWeight = FontWeight.SemiBold, fontSize = 66.sp, lineHeight = 70.sp),
    displayMedium = TextStyle(fontFamily = ChitalkaFonts.Playfair, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 46.sp),
    displaySmall = TextStyle(fontFamily = ChitalkaFonts.Playfair, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineLarge = rubik(24, FontWeight.SemiBold),
    headlineMedium = rubik(22, FontWeight.Medium, 1.35f),
    headlineSmall = rubik(19, FontWeight.SemiBold),
    titleLarge = rubik(17, FontWeight.SemiBold),
    titleMedium = rubik(15, FontWeight.SemiBold),
    titleSmall = rubik(13, FontWeight.Medium),
    bodyLarge = rubik(15, FontWeight.Normal, 1.5f),
    bodyMedium = rubik(14, FontWeight.Normal, 1.45f),
    bodySmall = rubik(12, FontWeight.Normal),
    labelLarge = rubik(13, FontWeight.Medium),
    labelMedium = rubik(11, FontWeight.Medium),
    labelSmall = rubik(10, FontWeight.Normal),
)
