package ru.chitalka.core.designsystem.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.chitalka.core.model.ReaderFont

/**
 * Шрифты для чтения. Шрифт интерфейса и шрифт чтения — разные токены.
 *
 * Пока здесь системные засечки и гротеск. На этапе «Дизайн-система» файлы Literata,
 * PT Serif, Lora, Cormorant Garamond и Manrope (OFL, с кириллицей) лягут в res/font,
 * и меняется только эта функция — экраны трогать не придётся.
 */
object ReaderFonts {
    fun familyFor(font: ReaderFont): FontFamily = when (font) {
        ReaderFont.Literata,
        ReaderFont.PtSerif,
        ReaderFont.Lora,
        ReaderFont.Cormorant,
        -> FontFamily.Serif
        ReaderFont.Manrope -> FontFamily.SansSerif
    }
}

/** Типографика интерфейса: книжные засечки в заголовках, гротеск в мелких подписях. */
internal val ChitalkaTypography: Typography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
        headlineLarge = headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
        headlineSmall = headlineSmall.copy(fontFamily = FontFamily.Serif),
        titleLarge = titleLarge.copy(fontFamily = FontFamily.Serif),
        titleMedium = titleMedium.copy(fontFamily = FontFamily.Serif, fontSize = 17.sp),
    )
}
