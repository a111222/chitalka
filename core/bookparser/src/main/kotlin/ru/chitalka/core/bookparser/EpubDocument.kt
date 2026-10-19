package ru.chitalka.core.bookparser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import ru.chitalka.core.model.BookFormat
import ru.chitalka.core.model.Chapter
import ru.chitalka.core.model.TocEntry
import java.io.File
import java.net.URLDecoder
import java.util.zip.ZipFile

/**
 * EPUB — это ZIP с XHTML-файлами. Порядок чтения задаёт spine в файле .opf:
 * одна запись spine = одна глава. Оглавление берём из nav (EPUB 3) или toc.ncx (EPUB 2).
 */
internal class EpubDocument private constructor(
    private val zip: ZipFile,
    override val metadata: BookMetadata,
    override val toc: List<TocEntry>,
    /** Пути глав внутри ZIP в порядке чтения. */
    private val spine: List<String>,
    private val coverPath: String?,
) : BookDocument {

    override val chapterCount: Int get() = spine.size

    override fun chapter(index: Int): Chapter {
        val path = spine.getOrNull(index) ?: throw BookParseException("Нет главы $index")
        val doc = zip.readHtml(path)
        val body = doc.selectFirst("body") ?: return Chapter(index, null, emptyList())
        val blocks = HtmlBlocks.parse(body) { src -> resolvePath(path, src) }
        val title = toc.firstOrNull { it.chapterIndex == index }?.title
        return Chapter(index, title, blocks)
    }

    override fun image(path: String): ByteArray? = zip.readBytes(path)

    override fun cover(): ByteArray? = coverPath?.let(zip::readBytes)

    override fun close() = zip.close()

    companion object {
        fun open(file: File): EpubDocument {
            val zip = ZipFile(file)
            try {
                return parse(zip)
            } catch (e: Exception) {
                zip.close()
                throw e
            }
        }

        private fun parse(zip: ZipFile): EpubDocument {
            val container = zip.readXml("META-INF/container.xml")
                ?: throw BookParseException("В EPUB нет META-INF/container.xml")
            val opfPath = container.selectFirst("rootfile")?.attr("full-path")?.ifBlank { null }
                ?: throw BookParseException("В EPUB не указан файл .opf")
            val opf = zip.readXml(opfPath) ?: throw BookParseException("В EPUB нет файла $opfPath")

            val manifest = opf.select("manifest > item").associate { item ->
                item.attr("id") to ManifestItem(
                    path = resolvePath(opfPath, item.attr("href")),
                    mediaType = item.attr("media-type"),
                    properties = item.attr("properties").split(' ').toSet(),
                )
            }
            val spine = opf.select("spine > itemref")
                .mapNotNull { manifest[it.attr("idref")]?.path }
                .filter { zip.getEntry(it) != null }
                .distinct()
            if (spine.isEmpty()) throw BookParseException("В EPUB нет ни одной главы")

            val metadata = readMetadata(opf)
            val navPath = manifest.values.firstOrNull { "nav" in it.properties }?.path
            val ncxPath = opf.selectFirst("spine")?.attr("toc")?.let { manifest[it]?.path }
                ?: manifest.values.firstOrNull { it.mediaType == "application/x-dtbncx+xml" }?.path
            val toc = (navPath?.let { readNavToc(zip, it, spine) }.orEmpty())
                .ifEmpty { ncxPath?.let { readNcxToc(zip, it, spine) }.orEmpty() }

            return EpubDocument(zip, metadata, toc, spine, findCover(opf, manifest))
        }

        private fun readMetadata(opf: Document): BookMetadata {
            fun dc(name: String): String? = opf.select("metadata > *").firstOrNull { it.tagName().endsWith(name) }
                ?.text()?.cleanText()

            fun meta(name: String): String? = opf.selectFirst("metadata > meta[name='$name']")?.attr("content")?.cleanText()

            val authors = opf.select("metadata > *").filter { it.tagName().endsWith("creator") }
                .mapNotNull { it.text().cleanText() }
            return BookMetadata(
                title = dc("title"),
                author = authors.takeIf { it.isNotEmpty() }?.joinToString(", "),
                series = meta("calibre:series"),
                seriesIndex = meta("calibre:series_index")?.toDoubleOrNull()?.toInt(),
                language = dc("language"),
                format = BookFormat.EPUB,
                description = opf.select("metadata > *").firstOrNull { it.tagName().endsWith("description") }
                    ?.text()?.let { htmlParagraphs(it) },
                genres = opf.select("metadata > *").filter { it.tagName().endsWith("subject") }
                    .mapNotNull { it.text().cleanText() }.distinct(),
            )
        }

        /** dc:description часто содержит экранированный HTML: «&lt;p&gt;…&lt;/p&gt;». */
        private fun htmlParagraphs(raw: String): String? {
            val body = Jsoup.parseBodyFragment(raw).body()
            val paragraphs = body.select("p").mapNotNull { it.text().cleanText() }
            return paragraphs.takeIf { it.isNotEmpty() }?.joinToString("\n\n") ?: body.text().cleanText()
        }

        private fun findCover(opf: Document, manifest: Map<String, ManifestItem>): String? {
            manifest.values.firstOrNull { "cover-image" in it.properties }?.let { return it.path }
            opf.selectFirst("metadata > meta[name=cover]")?.attr("content")?.let { id ->
                manifest[id]?.takeIf { it.mediaType.startsWith("image/") }?.let { return it.path }
            }
            return manifest.entries.firstOrNull { (id, item) ->
                item.mediaType.startsWith("image/") && id.contains("cover", ignoreCase = true)
            }?.value?.path
        }

        /** Оглавление EPUB 3: <nav epub:type="toc"> со вложенными <ol>. */
        private fun readNavToc(zip: ZipFile, navPath: String, spine: List<String>): List<TocEntry> {
            val doc = zip.readHtml(navPath)
            val nav = doc.select("nav").firstOrNull { it.attr("epub:type") == "toc" } ?: doc.selectFirst("nav")
            val root = nav?.selectFirst("ol") ?: return emptyList()
            val result = mutableListOf<TocEntry>()
            fun walk(ol: Element, level: Int) {
                for (li in ol.children().filter { it.normalName() == "li" }) {
                    val link = li.children().firstOrNull { it.normalName() == "a" || it.normalName() == "span" }
                    val title = link?.text()?.cleanText()
                    val chapter = link?.attr("href")?.ifBlank { null }?.let { spineIndex(spine, navPath, it) }
                    if (title != null && chapter != null) result += TocEntry(title, level, chapter)
                    li.children().firstOrNull { it.normalName() == "ol" }?.let { walk(it, level + 1) }
                }
            }
            walk(root, 0)
            return result
        }

        /** Оглавление EPUB 2: toc.ncx с вложенными navPoint. */
        private fun readNcxToc(zip: ZipFile, ncxPath: String, spine: List<String>): List<TocEntry> {
            val ncx = zip.readXml(ncxPath) ?: return emptyList()
            val navMap = ncx.selectFirst("navMap") ?: return emptyList()
            val result = mutableListOf<TocEntry>()
            fun walk(parent: Element, level: Int) {
                for (point in parent.children().filter { it.normalName() == "navpoint" }) {
                    val title = point.selectFirst("navLabel > text")?.text()?.cleanText()
                    val chapter = point.selectFirst("content")?.attr("src")?.let { spineIndex(spine, ncxPath, it) }
                    if (title != null && chapter != null) result += TocEntry(title, level, chapter)
                    walk(point, level + 1)
                }
            }
            walk(navMap, 0)
            return result
        }

        private fun spineIndex(spine: List<String>, basePath: String, href: String): Int? =
            spine.indexOf(resolvePath(basePath, href)).takeIf { it >= 0 }
    }

    private class ManifestItem(val path: String, val mediaType: String, val properties: Set<String>)
}

/**
 * Путь [href] относительно файла [basePath] внутри ZIP: «OEBPS/text/ch1.xhtml» + «../img/a.jpg»
 * = «OEBPS/img/a.jpg». Якорь (#…) отбрасывается, %20 и подобное раскодируется.
 */
internal fun resolvePath(basePath: String, href: String): String {
    val clean = decode(href.substringBefore('#'))
    if (clean.isEmpty()) return basePath
    val parts = if (clean.startsWith("/")) {
        mutableListOf()
    } else {
        basePath.split('/').dropLast(1).toMutableList()
    }
    for (segment in clean.split('/')) {
        when (segment) {
            "", "." -> Unit
            ".." -> parts.removeLastOrNull()
            else -> parts += segment
        }
    }
    return parts.joinToString("/")
}

private fun decode(value: String): String = try {
    URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
} catch (_: IllegalArgumentException) {
    value
}

private fun ZipFile.readBytes(path: String): ByteArray? =
    getEntry(path)?.let { entry -> getInputStream(entry).use { it.readBytes() } }

private fun ZipFile.readXml(path: String): Document? {
    val entry = getEntry(path) ?: return null
    return getInputStream(entry).use { Jsoup.parse(it, null, "", Parser.xmlParser()) }
}

private fun ZipFile.readHtml(path: String): Document {
    val entry = getEntry(path) ?: throw BookParseException("В EPUB нет файла $path")
    // XHTML разбираем как XML: самозакрытые <a id="x"/> в HTML-режиме «съели» бы весь текст после себя
    return getInputStream(entry).use { Jsoup.parse(it, null, "", Parser.xmlParser()) }
}
