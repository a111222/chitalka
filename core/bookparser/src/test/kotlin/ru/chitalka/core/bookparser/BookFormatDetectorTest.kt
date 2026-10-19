package ru.chitalka.core.bookparser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.chitalka.core.model.BookFormat

class BookFormatDetectorTest {
    private val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0, 0)

    @Test
    fun epubIsZipWithMimetype() {
        assertEquals(BookFormat.EPUB, BookFormatDetector.detect(zipHeader, listOf("mimetype", "OEBPS/content.opf")))
    }

    @Test
    fun zippedFb2IsFb2() {
        assertEquals(BookFormat.FB2, BookFormatDetector.detect(zipHeader, listOf("book.FB2")))
    }

    @Test
    fun plainFb2IsDetectedByRootTag() {
        val xml = """<?xml version="1.0" encoding="utf-8"?><FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0">"""
        assertEquals(BookFormat.FB2, BookFormatDetector.detect(xml.toByteArray()))
    }

    @Test
    fun pdfIsDetected() {
        assertEquals(BookFormat.PDF, BookFormatDetector.detect("%PDF-1.7".toByteArray()))
    }

    @Test
    fun unknownFileGivesNull() {
        assertNull(BookFormatDetector.detect("hello".toByteArray()))
        assertNull(BookFormatDetector.detect(zipHeader, listOf("photo.jpg")))
    }
}
