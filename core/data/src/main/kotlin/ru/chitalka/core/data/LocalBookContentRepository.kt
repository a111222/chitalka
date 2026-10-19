package ru.chitalka.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.chitalka.core.bookparser.BookDocument
import ru.chitalka.core.bookparser.BookParseException
import ru.chitalka.core.bookparser.BookParsers
import ru.chitalka.core.database.dao.BookDao
import ru.chitalka.core.domain.BookContentRepository
import ru.chitalka.core.model.BookContent
import ru.chitalka.core.model.Chapter
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Держит открытой одну книгу — ту, что сейчас читают. Листание по главам не открывает файл заново.
 * Mutex: парсер не потокобезопасен, а главы могут попросить одновременно.
 */
@Singleton
internal class LocalBookContentRepository @Inject constructor(
    private val bookDao: BookDao,
) : BookContentRepository {
    private val mutex = Mutex()
    private var openedId: Long? = null
    private var opened: BookDocument? = null

    override suspend fun open(bookId: Long): BookContent = withDocument(bookId) { doc ->
        val book = bookDao.getById(bookId) ?: throw BookParseException("Книга удалена")
        BookContent(
            title = book.title,
            author = book.author,
            language = book.language,
            chapterCount = doc.chapterCount,
            toc = doc.toc,
        )
    }

    override suspend fun chapter(bookId: Long, index: Int): Chapter = withDocument(bookId) { it.chapter(index) }

    override suspend fun image(bookId: Long, path: String): ByteArray? = withDocument(bookId) { it.image(path) }

    private suspend fun <T> withDocument(bookId: Long, block: suspend (BookDocument) -> T): T = mutex.withLock {
        withContext(Dispatchers.IO) {
            val doc = opened?.takeIf { openedId == bookId } ?: run {
                opened?.close()
                opened = null
                val book = bookDao.getById(bookId) ?: throw BookParseException("Книга удалена")
                BookParsers.open(File(book.filePath), book.format).also {
                    opened = it
                    openedId = bookId
                }
            }
            block(doc)
        }
    }

    /** Закрывает книгу, если она открыта: перед удалением файла. */
    suspend fun release(bookId: Long) = mutex.withLock {
        if (openedId == bookId) {
            opened?.close()
            opened = null
            openedId = null
        }
    }
}
