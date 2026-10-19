package ru.chitalka.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryTest {
    @Test
    fun wordsBecomePrefixQueries() {
        assertEquals("толстой* война*", "  толстой   война ".toFtsQuery())
    }

    @Test
    fun specialCharactersAreDropped() {
        assertEquals("abc*", "a\"b*c".toFtsQuery())
    }

    @Test
    fun blankQueryGivesNull() {
        assertNull("   ".toFtsQuery())
    }
}
