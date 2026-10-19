package ru.chitalka.core.testing

import ru.chitalka.core.domain.BookContentRepository
import ru.chitalka.core.model.Block
import ru.chitalka.core.model.BookContent
import ru.chitalka.core.model.Chapter

/** Книга в памяти: [chapterCount] глав по одному абзацу «Глава N». */
class FakeBookContentRepository(
    private val chapterCount: Int = 3,
    private val title: String = "Тестовая книга",
) : BookContentRepository {
    val requestedChapters = mutableListOf<Int>()

    override suspend fun open(bookId: Long) = BookContent(
        title = title,
        author = "Автор",
        language = "ru",
        chapterCount = chapterCount,
        toc = emptyList(),
    )

    override suspend fun chapter(bookId: Long, index: Int): Chapter {
        requestedChapters += index
        return Chapter(index, "Глава ${index + 1}", listOf(Block.Paragraph("Глава ${index + 1}")))
    }

    override suspend fun image(bookId: Long, path: String): ByteArray? = null
}
