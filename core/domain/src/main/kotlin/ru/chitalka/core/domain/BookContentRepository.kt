package ru.chitalka.core.domain

import ru.chitalka.core.model.BookContent
import ru.chitalka.core.model.Chapter

/** Текст книги. Главы отдаются по одной: открыть «Войну и мир» не значит загрузить её целиком. */
interface BookContentRepository {
    /** Оглавление и число глав. Бросает исключение, если файл книги не читается. */
    suspend fun open(bookId: Long): BookContent

    suspend fun chapter(bookId: Long, index: Int): Chapter

    /** Картинка из книги по пути из [ru.chitalka.core.model.Block.Image]. */
    suspend fun image(bookId: Long, path: String): ByteArray?
}
