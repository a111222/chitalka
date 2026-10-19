package ru.chitalka.core.bookparser

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.chitalka.core.model.Block
import ru.chitalka.core.model.BookFormat
import ru.chitalka.core.model.TocEntry
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubDocumentTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val coverBytes = byteArrayOf(9, 8, 7)

    private fun epub(withNav: Boolean): File {
        val file = tmp.newFile("book.epub")
        ZipOutputStream(file.outputStream()).use { zip ->
            fun put(name: String, text: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
            put("mimetype", "application/epub+zip")
            put(
                "META-INF/container.xml",
                """<container><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>""",
            )
            put(
                "OEBPS/content.opf",
                """
                <package xmlns="http://www.idpf.org/2007/opf" xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <metadata>
                    <dc:title>Мастер и Маргарита</dc:title>
                    <dc:creator>Михаил Булгаков</dc:creator>
                    <dc:language>ru</dc:language>
                    <meta name="cover" content="cover-img"/>
                    <meta name="calibre:series" content="Романы"/>
                    <meta name="calibre:series_index" content="2.0"/>
                  </metadata>
                  <manifest>
                    <item id="cover-img" href="images/cover.jpg" media-type="image/jpeg"/>
                    ${if (withNav) """<item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>""" else ""}
                    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                    <item id="c1" href="text/chapter%201.xhtml" media-type="application/xhtml+xml"/>
                    <item id="c2" href="text/c2.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine toc="ncx"><itemref idref="c1"/><itemref idref="c2"/></spine>
                </package>
                """.trimIndent(),
            )
            zip.putNextEntry(ZipEntry("OEBPS/images/cover.jpg"))
            zip.write(coverBytes)
            zip.closeEntry()
            put(
                "OEBPS/nav.xhtml",
                """
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body>
                <nav epub:type="toc"><ol>
                  <li><a href="text/chapter%201.xhtml">Часть первая</a>
                    <ol><li><a href="text/c2.xhtml#start">Глава 2</a></li></ol></li>
                </ol></nav></body></html>
                """.trimIndent(),
            )
            put(
                "OEBPS/toc.ncx",
                """
                <ncx><navMap>
                  <navPoint id="p1"><navLabel><text>Из NCX</text></navLabel><content src="text/c2.xhtml"/></navPoint>
                </navMap></ncx>
                """.trimIndent(),
            )
            put(
                "OEBPS/text/chapter 1.xhtml",
                """
                <?xml version="1.0" encoding="utf-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml"><head><title>Глава</title><style>p{}</style></head>
                <body>
                  <h1>Никогда не разговаривайте с неизвестными</h1>
                  <p>Однажды весною, в час <i>небывало</i> жаркого заката,<br/>в Москве.</p>
                  <div><a id="anchor"/>Текст прямо в div</div>
                  <blockquote><p>Цитата</p></blockquote>
                  <p><img src="../images/cover.jpg" alt="Обложка"/></p>
                </body></html>
                """.trimIndent(),
            )
            put("OEBPS/text/c2.xhtml", """<html><body><p>Вторая&#160;глава</p></body></html>""")
        }
        return file
    }

    @Test
    fun readsMetadataAndCover() {
        EpubDocument.open(epub(withNav = true)).use { doc ->
            assertEquals(
                BookMetadata("Мастер и Маргарита", "Михаил Булгаков", "Романы", 2, "ru", BookFormat.EPUB),
                doc.metadata,
            )
            assertEquals(2, doc.chapterCount)
            assertArrayEquals(coverBytes, doc.cover())
        }
    }

    @Test
    fun navTocWinsOverNcx() {
        EpubDocument.open(epub(withNav = true)).use { doc ->
            assertEquals(listOf(TocEntry("Часть первая", 0, 0), TocEntry("Глава 2", 1, 1)), doc.toc)
        }
    }

    @Test
    fun ncxTocIsUsedWithoutNav() {
        EpubDocument.open(epub(withNav = false)).use { doc ->
            assertEquals(listOf(TocEntry("Из NCX", 0, 1)), doc.toc)
        }
    }

    @Test
    fun chapterHtmlBecomesBlocks() {
        EpubDocument.open(epub(withNav = true)).use { doc ->
            val chapter = doc.chapter(0)
            assertEquals(
                listOf(
                    Block.Heading("Никогда не разговаривайте с неизвестными", 1),
                    Block.Paragraph("Однажды весною, в час небывало жаркого заката, в Москве."),
                    Block.Paragraph("Текст прямо в div"),
                    Block.Quote("Цитата"),
                    Block.Image("OEBPS/images/cover.jpg", "Обложка"),
                ),
                chapter.blocks,
            )
            assertArrayEquals(coverBytes, doc.image("OEBPS/images/cover.jpg"))
            val second = doc.chapter(1).blocks.single() as Block.Paragraph
            assertEquals("Вторая глава", second.text.replace('\u00A0', ' '))
            assertFalse(doc.chapter(0).blocks.any { it is Block.Paragraph && it.text.contains("p{}") })
        }
    }

    @Test
    fun resolvesRelativePaths() {
        assertEquals("OEBPS/img/a.jpg", resolvePath("OEBPS/text/ch1.xhtml", "../img/a.jpg"))
        assertEquals("OEBPS/text/ch 2.xhtml", resolvePath("OEBPS/content.opf", "text/ch%202.xhtml#x"))
        assertEquals("a.xhtml", resolvePath("content.opf", "./a.xhtml"))
    }
}
