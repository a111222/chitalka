package ru.chitalka.feature.library

/** Место корешка в шкафу: номер полки и отступ от её левого края, dp. */
internal data class SpineSlot(val shelf: Int, val x: Float)

/**
 * Расставляет корешки по полкам слева направо, как книги в настоящем шкафу:
 * не влезла — переезжает на следующую полку. Ширина полки берётся настоящая,
 * поэтому на планшете на полке больше книг, а сами книги не растягиваются.
 */
internal fun arrangeSpines(widths: List<Float>, shelfWidth: Float, gap: Float): List<SpineSlot> {
    var shelf = 0
    var x = 0f
    return widths.map { width ->
        if (x > 0f && x + width > shelfWidth) {
            shelf++
            x = 0f
        }
        SpineSlot(shelf, x).also { x += width + gap }
    }
}

/** Пустой шкаф всё равно показывает три полки: так он выглядит мебелью, а не списком. */
internal const val MIN_SHELVES = 3

internal fun shelfCountFor(slots: List<SpineSlot>): Int =
    maxOf(MIN_SHELVES, (slots.maxOfOrNull { it.shelf } ?: -1) + 1)
