package ru.chitalka.core.model

/**
 * Позиция в книге: «глава + смещение в символах», а не номер страницы.
 * Номер страницы меняется от шрифта и размера экрана, смещение в тексте — никогда.
 */
data class ReadingPosition(
    val chapterIndex: Int,
    val charOffset: Int,
) {
    init {
        require(chapterIndex >= 0) { "chapterIndex не может быть отрицательным" }
        require(charOffset >= 0) { "charOffset не может быть отрицательным" }
    }

    companion object {
        val Start = ReadingPosition(chapterIndex = 0, charOffset = 0)
    }
}
