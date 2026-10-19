package ru.chitalka.core.designsystem.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
 * Шрифты для чтения. Шрифт интерфейса и шрифт чтения — разные токены.
 * Literata, PT Serif и Lora придут вместе с новой читалкой; пока здесь системные засечки.
 */
object ReaderFonts {
    fun familyFor(font: ReaderFont): FontFamily = when (font) {
        ReaderFont.Literata,
        ReaderFont.PtSerif,
        ReaderFont.Lora,
        ReaderFont.Cormorant,
        -> FontFamily.Serif
        ReaderFont.Manrope -> ChitalkaFonts.Rubik
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
