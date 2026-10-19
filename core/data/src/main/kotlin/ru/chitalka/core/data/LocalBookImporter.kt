package ru.chitalka.core.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.chitalka.core.bookparser.BookFormatDetector
import ru.chitalka.core.bookparser.BookParseException
import ru.chitalka.core.bookparser.BookParsers
import ru.chitalka.core.database.dao.BookDao
import ru.chitalka.core.database.entity.BookEntity
import ru.chitalka.core.database.entity.TocEntryEntity
import ru.chitalka.core.domain.BookImporter
import ru.chitalka.core.domain.ImportResult
import ru.chitalka.core.model.BookFormat
import java.io.File
import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Импорт по шагам: копируем файл к себе, считаем SHA-256 (дубли не берём),
 * определяем формат по содержимому, читаем сведения и оглавление, сохраняем обложку и запись в базе.
 */
internal class LocalBookImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val storage: BookStorage,
) : BookImporter {

    override suspend fun import(uris: List<String>): List<ImportResult> = withContext(Dispatchers.IO) {
        uris.map { importOne(Uri.parse(it)) }
    }

    private suspend fun importOne(uri: Uri): ImportResult {
        val fileName = displayName(uri)
        val tmp = File(storage.booksDir, "import-${UUID.randomUUID()}.tmp")
        return try {
            val hash = copyWithHash(uri, tmp)
            bookDao.findIdByHash(hash)?.let { return ImportResult.Duplicate(it) }

            val format = detectFormat(tmp)
                ?: return ImportResult.Failed(fileName, "Это не EPUB и не FB2")
            if (format == BookFormat.PDF) return ImportResult.Failed(fileName, "PDF пока не поддерживается")

            // Сначала читаем книгу и только потом кладём в шкаф: битый файл не оставит мусора
            val (metadata, toc, cover) = BookParsers.open(tmp, format).use { doc ->
                Triple(doc.metadata, doc.toc, doc.cover())
            }
            val target = File(storage.booksDir, "$hash.${format.extension}")
            if (!tmp.renameTo(target)) tmp.copyTo(target, overwrite = true)
            val coverFile = cover?.let { bytes -> File(storage.coversDir, "$hash.img").apply { writeBytes(bytes) } }
            val title = metadata.title ?: fileName.substringBeforeLast('.').ifBlank { "Без названия" }

            val id = bookDao.insertWithToc(
                BookEntity(
                    title = title,
                    author = metadata.author,
                    series = metadata.series,
                    seriesIndex = metadata.seriesIndex,
                    language = metadata.language,
                    format = format,
                    filePath = target.path,
                    coverPath = coverFile?.path,
                    fileHash = hash,
                    sizeBytes = target.length(),
                    addedAt = Instant.now(),
                    lastOpenedAt = null,
                ),
                toc.map { TocEntryEntity(bookId = 0, title = it.title, level = it.level, chapterIndex = it.chapterIndex, anchor = null) },
            )
            // -1: ту же книгу только что импортировали параллельно
            if (id == -1L) ImportResult.Duplicate(bookDao.findIdByHash(hash) ?: -1L) else ImportResult.Imported(id, title)
        } catch (e: BookParseException) {
            ImportResult.Failed(fileName, e.message ?: "Файл повреждён")
        } catch (e: IOException) {
            ImportResult.Failed(fileName, "Не удалось прочитать файл")
        } catch (e: SecurityException) {
            ImportResult.Failed(fileName, "Нет доступа к файлу")
        } finally {
            tmp.delete()
        }
    }

    private fun copyWithHash(uri: Uri, target: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Файл не открывается")
        DigestInputStream(input, digest).use { source -> target.outputStream().use { source.copyTo(it) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun detectFormat(file: File): BookFormat? {
        val header = file.inputStream().use { input -> ByteArray(1024).let { it.copyOf(maxOf(0, input.read(it))) } }
        val zipEntries = if (header.size >= 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()) {
            runCatching { ZipFile(file).use { zip -> zip.entries().asSequence().map { it.name }.toList() } }
                .getOrDefault(emptyList())
        } else {
            emptyList()
        }
        return BookFormatDetector.detect(header, zipEntries)
    }

    private fun displayName(uri: Uri): String {
        val fromProvider = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return fromProvider ?: uri.lastPathSegment?.substringAfterLast('/') ?: "файл"
    }
}

private val BookFormat.extension: String
    get() = when (this) {
        BookFormat.EPUB -> "epub"
        BookFormat.FB2 -> "fb2"
        BookFormat.PDF -> "pdf"
    }
