package ru.chitalka.feature.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.chitalka.core.designsystem.bookcase.spineStyleFor

class ShelfLayoutTest {
    @Test
    fun spinesMoveToNextShelfWhenFull() {
        val slots = arrangeSpines(listOf(30f, 30f, 30f, 30f), shelfWidth = 70f, gap = 2f)
        assertEquals(
            listOf(SpineSlot(0, 0f), SpineSlot(0, 32f), SpineSlot(1, 0f), SpineSlot(1, 32f)),
            slots,
        )
        assertEquals(MIN_SHELVES, shelfCountFor(slots))
    }

    @Test
    fun widerShelfHoldsMoreBooksInsteadOfStretchingThem() {
        val widths = List(20) { 32f }
        val phone = arrangeSpines(widths, shelfWidth = 310f, gap = 2f)
        val tablet = arrangeSpines(widths, shelfWidth = 660f, gap = 2f)
        assertEquals(9, phone.count { it.shelf == 0 })
        assertEquals(19, tablet.count { it.shelf == 0 })
        assertEquals(3, shelfCountFor(phone))
    }

    @Test
    fun emptyCaseStillHasThreeShelves() {
        assertEquals(3, shelfCountFor(emptyList()))
        assertEquals(5, shelfCountFor(listOf(SpineSlot(4, 0f))))
    }

    @Test
    fun spineStyleIsStableForTheSameBook() {
        assertEquals(spineStyleFor(42), spineStyleFor(42))
        // Соседние id не должны давать одинаковые корешки подряд
        val styles = (1L..10L).map(::spineStyleFor)
        assertTrue(styles.distinct().size > 5)
    }
}
