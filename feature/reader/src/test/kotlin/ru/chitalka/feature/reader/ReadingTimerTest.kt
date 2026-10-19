package ru.chitalka.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingTimerTest {
    private var clock = 0L
    private val timer = ReadingTimer(idleLimitMs = 1_000) { clock }

    @Test
    fun countsOnlyWhileStarted() {
        clock = 500
        assertEquals(0L, timer.tick())
        timer.start()
        clock = 800
        assertEquals(300L, timer.tick())
        clock = 900
        assertEquals(100L, timer.stop())
        clock = 5_000
        assertEquals(0L, timer.tick())
    }

    @Test
    fun longPauseIsCappedAndClockGoingBackIsIgnored() {
        timer.start()
        clock = 60_000
        assertEquals(1_000L, timer.tick())
        clock = 10_000
        assertEquals(0L, timer.tick())
    }
}
