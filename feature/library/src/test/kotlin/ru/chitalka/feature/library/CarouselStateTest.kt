package ru.chitalka.feature.library

import org.junit.Assert.assertEquals
import org.junit.Test

class CarouselStateTest {
    @Test
    fun ringOfSixWrapsAround() {
        val state = CarouselState(0f)
        assertEquals(0, state.currentOf(6))
        // Последняя книга стоит слева от первой, а не в пяти шагах справа
        assertEquals(-1f, state.offsetOf(5, 6), 0.001f)
        state.position = -1f
        assertEquals(5, state.currentOf(6))
        state.position = 6.4f
        assertEquals(0, state.currentOf(6))
    }

    @Test
    fun twoBooksDoNotWrap() {
        val state = CarouselState(0f)
        assertEquals(1f, state.offsetOf(1, 2), 0.001f)
        state.position = 5f
        assertEquals(1, state.currentOf(2))
    }
}
