package ru.chitalka.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingPositionTest {
    @Test
    fun startIsBeginningOfFirstChapter() {
        assertEquals(0, ReadingPosition.Start.chapterIndex)
        assertEquals(0, ReadingPosition.Start.charOffset)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeOffsetIsRejected() {
        ReadingPosition(chapterIndex = 1, charOffset = -5)
    }

    @Test
    fun defaultSettingsMatchDesign() {
        val settings = ReaderSettings()
        assertEquals(ReaderThemeMode.LightOak, settings.theme)
        assertEquals(ReaderFont.Literata, settings.font)
        assertEquals(1.65f, settings.lineSpacing.multiplier, 0.001f)
    }
}
