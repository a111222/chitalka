package ru.chitalka.core.bookparser

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.chitalka.core.model.Block
import ru.chitalka.core.model.TocEntry
import java.util.Base64

class Fb2DocumentTest {
    private val cover = byteArrayOf(1, 2, 3, 4)

    private val fb2 = """
        <?xml version="1.0" encoding="windows-1251"?>
        <FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0" xmlns:l="http://www.w3.org/1999/xlink">
          <description>
            <title-info>
              <author><first-name>Лев</first-name><last-name>Толстой</last-name></author>
              <book-title>Война и мир</book-title>
              <lang>ru</lang>
              <sequence name="Эпопея" number="1"/>
              <coverpage><image l:href="#cover.jpg"/></coverpage>
            </title-info>
          </description>
          <body>
            <title><p>Война и мир</p></title>
            <section>
              <title><p>Том первый</p></title>
              <epigraph><p>Эпиграф</p><text-author>Автор</text-author></epigraph>
              <section>
                <title><p>Глава I</p><p>Салон</p></title>
                <p>— Eh bien, mon prince.</p>
                <empty-line/>
                <p>Так говорила <emphasis>Анна Павловна</emphasis>.</p>
              </section>
              <section>
                <title><p>Глава II</p></title>
                <poem><stanza><v>Строка один</v><v>Строка два</v></stanza></poem>
                <image l:href="#pic.png"/>
              </section>
            </section>
          </body>
          <body name="notes"><section id="n1"><p>Сноска</p></section></body>
          <binary id="cover.jpg" content-type="image/jpeg">${Base64.getEncoder().encodeToString(cover)}</binary>
        </FictionBook>
    """.trimIndent().toByteArray(charset("windows-1251"))

    private val doc = Fb2Document.parse(fb2)

    @Test
    fun readsMetadataInDeclaredEncoding() {
        val meta = doc.metadata
        assertEquals("Война и мир", meta.title)
        assertEquals("Лев Толстой", meta.author)
        assertEquals("Эпопея", meta.series)
        assertEquals(1, meta.seriesIndex)
        assertEquals("ru", meta.language)
    }

    @Test
    fun leafSectionsBecomeChaptersAndNotesAreSkipped() {
        assertEquals(2, doc.chapterCount)
        val first = doc.chapter(0).blocks
        // Заголовок книги, части и эпиграф части попадают в начало первой главы
        assertEquals(Block.Heading("Война и мир", 1), first[0])
        assertEquals(Block.Heading("Том первый", 1), first[1])
        assertEquals(Block.Quote("Эпиграф"), first[2])
        assertEquals(Block.Heading("Глава I\nСалон", 2), first[4])
        assertEquals(Block.Paragraph("Так говорила Анна Павловна."), first.last())
        assertTrue(doc.chapter(1).blocks.none { it is Block.Paragraph && it.text == "Сноска" })
    }

    @Test
    fun tocPointsToChapters() {
        assertEquals(
            listOf(
                TocEntry("Том первый", 0, 0),
                TocEntry("Глава I Салон", 1, 0),
                TocEntry("Глава II", 1, 1),
            ),
            doc.toc,
        )
    }

    @Test
    fun poemsAndImagesAreKept() {
        val blocks = doc.chapter(1).blocks
        assertTrue(Block.Quote("Строка один\nСтрока два") in blocks)
        assertEquals(Block.Image("pic.png"), blocks.last())
        assertNull(doc.image("pic.png"))
    }

    @Test
    fun coverIsDecodedFromBinary() {
        assertArrayEquals(cover, doc.cover())
    }

    @Test
    fun bodyWithoutSectionsIsSplitIntoParts() {
        val paragraphs = (1..Fb2Document.MAX_BLOCKS_PER_CHAPTER + 10).joinToString("") { "<p>Абзац $it</p>" }
        val xml = """<?xml version="1.0" encoding="utf-8"?><FictionBook><body>$paragraphs</body></FictionBook>"""
        val plain = Fb2Document.parse(xml.toByteArray())
        assertEquals(2, plain.chapterCount)
        assertEquals(10, plain.chapter(1).blocks.size)
        assertNull(plain.metadata.title)
    }
}
