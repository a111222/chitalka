package ru.chitalka.core.model

/** Настройки чтения. Хранятся в DataStore, а не в базе. */
data class ReaderSettings(
    val theme: ReaderThemeMode = ReaderThemeMode.LightOak,
    val font: ReaderFont = ReaderFont.Literata,
    val fontSizeSp: Int = DEFAULT_FONT_SIZE,
    val lineSpacing: LineSpacing = LineSpacing.Normal,
    val hapticsEnabled: Boolean = true,
    /** Подсветка полок в шкафу: включается кнопкой-лампой на экране библиотеки. */
    val shelfLightsOn: Boolean = true,
) {
    companion object {
        const val MIN_FONT_SIZE = 14
        const val MAX_FONT_SIZE = 28
        const val DEFAULT_FONT_SIZE = 18
    }
}

/** Три темы из макета v2. */
enum class ReaderThemeMode { LightOak, DarkWalnut, EInk }

/** Пять встроенных шрифтов из макета v2 (все с кириллицей, лицензия OFL). */
enum class ReaderFont(val displayName: String) {
    Literata("Literata"),
    PtSerif("PT Serif"),
    Lora("Lora"),
    Cormorant("Cormorant Garamond"),
    Manrope("Manrope"),
}

/** Межстрочный интервал: 1.45 / 1.65 / 1.85 из макета. */
enum class LineSpacing(val multiplier: Float) {
    Compact(1.45f),
    Normal(1.65f),
    Relaxed(1.85f),
}
