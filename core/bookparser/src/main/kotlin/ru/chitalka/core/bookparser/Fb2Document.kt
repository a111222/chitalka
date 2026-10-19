package ru.chitalka.core.bookparser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import ru.chitalka.core.model.Block
import ru.chitalka.core.model.BookFormat
import ru.chitalka.core.model.Chapter
import ru.chitalka.core.model.TocEntry
import java.io.File
import java.nio.charset.Charset
import java.util.Base64
import java.util.zip.ZipFile

/**
 * FB2 — один XML-файл: сведения в <description>, текст в <body> из вложенных <section>,
 * картинки в <binary> (base64). Главой считаем «листовой» раздел — без вложенных разделов:
 * так часть «Книга первая» не превращается в одну гигантскую главу.
 * Файл .fb2.zip распаковываем в память.
 */
internal class Fb2Document private constructor(
    override val metadata: BookMetadata,
    override val toc: List<TocEntry>,
    private val chapters: List<Chapter>,
    private val binaries: Map<String, String>,
    private val coverId: String?,
) : BookDocument {

    override val chapterCount: Int get() = chapters.size

    override fun chapter(index: Int): Chapter =
        chapters.getOrNull(index) ?: throw BookParseException("Нет главы $index")

    override fun image(path: String): ByteArray? = binaries[path]?.let { data ->
        runCatching { Base64.getMimeDecoder().decode(data) }.getOrNull()
    }

    override fun cover(): ByteArray? = coverId?.let(::image)

    override fun close() = Unit

    companion object {
        /** Длинную главу без разделов режем на части: пагинация огромного текста тормозит. */
        internal const val MAX_BLOCKS_PER_CHAPTER = 300

        fun open(file: File): Fb2Document = parse(readBytes(file))

        fun parse(bytes: ByteArray): Fb2Document {
            val doc = Jsoup.parse(String(bytes, detectCharset(bytes)), "", Parser.xmlParser())
            val root = doc.children().firstOrNull { it.normalName() == "fictionbook" }
                ?: throw BookParseException("Это не FB2: нет корневого тега FictionBook")

            val titleInfo = root.child("description")?.child("title-info")
            val metadata = BookMetadata(
                title = titleInfo?.child("book-title")?.text()?.cleanText(),
                author = titleInfo?.children()?.filter { it.normalName() == "author" }
                    ?.mapNotNull(::authorName)?.takeIf { it.isNotEmpty() }?.joinToString(", "),
                series = titleInfo?.child("sequence")?.attr("name")?.cleanText(),
                seriesIndex = titleInfo?.child("sequence")?.attr("number")?.toIntOrNull(),
                language = titleInfo?.child("lang")?.text()?.cleanText(),
                format = BookFormat.FB2,
            )
            val coverId = titleInfo?.child("coverpage")?.child("image")?.href()

            val body = root.children().firstOrNull { it.normalName() == "body" && it.attr("name") !in NOTE_BODIES }
                ?: throw BookParseException("В FB2 нет текста книги")
            val builder = ChapterBuilder()
            builder.container(body, depth = 0)
            builder.finish()
            if (builder.chapters.isEmpty()) throw BookParseException("В FB2 нет текста книги")

            val binaries = root.children().filter { it.normalName() == "binary" }
                .associate { it.attr("id") to it.text() }
            return Fb2Document(metadata, builder.toc, builder.chapters, binaries, coverId)
        }

        private val NOTE_BODIES = setOf("notes", "footnotes", "comments")

        private fun readBytes(file: File): ByteArray {
            val head = file.inputStream().use { input -> ByteArray(4).also { input.read(it) } }
            val isZip = head[0] == 0x50.toByte() && head[1] == 0x4B.toByte()
            if (!isZip) return file.readBytes()
            return ZipFile(file).use { zip ->
                val entry = zip.entries().asSequence().firstOrNull { it.name.endsWith(".fb2", ignoreCase = true) }
                    ?: throw BookParseException("В архиве нет файла .fb2")
                zip.getInputStream(entry).use { it.readBytes() }
            }
        }

        /** Кодировку FB2 пишут в прологе: <?xml version="1.0" encoding="windows-1251"?>. */
        internal fun detectCharset(bytes: ByteArray): Charset {
            if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
                return Charsets.UTF_8
            }
            val prolog = String(bytes, 0, minOf(bytes.size, 200), Charsets.ISO_8859_1)
            val name = Regex("""encoding\s*=\s*["']([\w.:-]+)["']""").find(prolog)?.groupValues?.get(1)
            return name?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
        }

        private fun authorName(author: Element): String? {
            val parts = listOf("first-name", "middle-name", "last-name")
                .mapNotNull { author.child(it)?.text()?.cleanText() }
            return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
                ?: author.child("nickname")?.text()?.cleanText()
        }
    }

    private class ChapterBuilder {
        val chapters = mutableListOf<Chapter>()
        val toc = mutableListOf<TocEntry>()

        /** Текст родительского раздела (его заголовок, эпиграф) — попадёт в начало следующей главы. */
        private val pending = mutableListOf<Block>()
        private var pendingTitle: String? = null

        /** Собственный текст [element] идёт в pending, вложенные разделы — на уровень [sectionDepth]. */
        fun container(element: Element, depth: Int, sectionDepth: Int = depth) {
            for (child in element.children()) {
                if (child.normalName() == "section") section(child, sectionDepth) else pending += blocks(child, depth)
            }
        }

        private fun section(section: Element, depth: Int) {
            val title = section.child("title")?.let(::titleText)
            if (title != null) toc += TocEntry(title.replace('\n', ' '), depth, chapters.size)
            if (pendingTitle == null) pendingTitle = title
            if (section.children().any { it.normalName() == "section" }) {
                container(section, depth, sectionDepth = depth + 1)
            } else {
                for (child in section.children()) pending += blocks(child, depth)
                emit()
            }
        }

        fun finish() {
            if (pending.isNotEmpty()) emit()
        }

        private fun emit() {
            if (pending.isEmpty()) return
            pending.chunked(MAX_BLOCKS_PER_CHAPTER).forEachIndexed { part, blocks ->
                chapters += Chapter(chapters.size, pendingTitle.takeIf { part == 0 }?.replace('\n', ' '), blocks)
            }
            pending.clear()
            pendingTitle = null
        }

        private fun blocks(e: Element, depth: Int): List<Block> = when (e.normalName()) {
            "title" -> listOfNotNull(titleText(e)?.let { Block.Heading(it, depth + 1) })
            "subtitle" -> listOfNotNull(e.text().cleanText()?.let { Block.Heading(it, depth + 2) })
            "p" -> listOfNotNull(e.text().cleanText()?.let { Block.Paragraph(it) })
            "epigraph", "cite" -> e.children().flatMap { child ->
                when (child.normalName()) {
                    "poem" -> blocks(child, depth)
                    else -> listOfNotNull(child.text().cleanText()?.let { Block.Quote(it) })
                }
            }
            "poem" -> e.children().flatMap { child ->
                when (child.normalName()) {
                    "title" -> blocks(child, depth + 1)
                    "stanza" -> listOfNotNull(stanzaText(child)?.let { Block.Quote(it) })
                    else -> listOfNotNull(child.text().cleanText()?.let { Block.Quote(it) })
                }
            }
            "image" -> listOfNotNull(e.href()?.let { Block.Image(it, e.attr("alt").ifBlank { null }) })
            "table" -> e.select("tr").mapNotNull { row -> row.text().cleanText()?.let { Block.Paragraph(it) } }
            "annotation" -> e.children().flatMap { blocks(it, depth) }
            else -> emptyList() // empty-line и служебные теги
        }

        private fun stanzaText(stanza: Element): String? =
            stanza.children().filter { it.normalName() == "v" }.mapNotNull { it.text().cleanText() }
                .takeIf { it.isNotEmpty() }?.joinToString("\n")
    }
}

/** Заголовок FB2 — несколько <p>: «Глава 1» и «Название» остаются на отдельных строках. */
private fun titleText(title: Element): String? =
    title.children().mapNotNull { it.text().cleanText() }.takeIf { it.isNotEmpty() }?.joinToString("\n")
        ?: title.text().cleanText()

private fun Element.child(name: String): Element? = children().firstOrNull { it.normalName() == name }

/** Ссылка на картинку: l:href="#cover.jpg" (префикс пространства имён бывает любым). */
private fun Element.href(): String? =
    attributes().firstOrNull { it.key.endsWith("href", ignoreCase = true) }?.value?.removePrefix("#")?.ifBlank { null }
