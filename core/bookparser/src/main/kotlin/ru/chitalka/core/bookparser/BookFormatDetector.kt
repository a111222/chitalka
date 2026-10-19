package ru.chitalka.core.bookparser

import ru.chitalka.core.model.BookFormat

/**
 * Определяет формат по первым байтам файла, а не по расширению:
 * у файлов из мессенджеров расширение часто теряется.
 */
object BookFormatDetector {
    private val ZIP = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // «PK»
    private val PDF = "%PDF".toByteArray(Charsets.US_ASCII)

    /**
     * @param header первые ~1 КБ файла
     * @param zipEntryNames имена файлов внутри ZIP, если это ZIP
     */
    fun detect(header: ByteArray, zipEntryNames: List<String> = emptyList()): BookFormat? = when {
        header.startsWith(PDF) -> BookFormat.PDF
        header.startsWith(ZIP) -> when {
            zipEntryNames.any { it == "mimetype" || it.endsWith(".opf") } -> BookFormat.EPUB
            zipEntryNames.any { it.endsWith(".fb2", ignoreCase = true) } -> BookFormat.FB2 // .fb2.zip
            else -> null
        }
        header.looksLikeFb2() -> BookFormat.FB2
        else -> null
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private fun ByteArray.looksLikeFb2(): Boolean =
        String(this, Charsets.ISO_8859_1).contains("<FictionBook")
}
