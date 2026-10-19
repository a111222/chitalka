package ru.chitalka.feature.library

import ru.chitalka.core.model.Book

/**
 * Разделы главной строятся из библиотеки самого человека: в видео это витрина магазина,
 * у нас — его собственные полки. Чистые функции, поэтому их легко проверить тестами.
 */
internal object HomeSections {
    const val CAROUSEL_SIZE = 6
    const val ROW_SIZE = 4
    const val AUTHORS_SIZE = 5

    /** Карусель: сначала то, что читали недавно, потом новые книги. */
    fun carousel(books: List<Book>): List<Book> = books.take(CAROUSEL_SIZE)

    /** Полки-подборки: «Читаю», «Хочу прочитать», «Прочитано» и «Все книги». */
    fun shelves(books: List<Book>): List<Shelf> = listOf(
        LibraryFilter.Reading to "Читаю",
        LibraryFilter.WantToRead to "Хочу прочитать",
        LibraryFilter.Finished to "Прочитано",
        LibraryFilter.All to "Все книги",
    ).map { (filter, name) ->
        val onShelf = books.filter(filter::matches)
        Shelf(filter = filter, name = name, count = onShelf.size, fan = onShelf.take(3))
    }

    fun recentlyAdded(books: List<Book>): List<Book> = books.sortedByDescending { it.addedAt }.take(ROW_SIZE)

    fun readingNow(books: List<Book>): List<Book> =
        books.filter { it.readingState == ReadingState.Reading }.take(ROW_SIZE)

    /** Авторы, у которых больше всего книг на полке; самый частый — первым. У каждого — его последняя книга для обложки. */
    fun authors(books: List<Book>): List<AuthorOnShelf> =
        books.filter { !it.author.isNullOrBlank() }
            .groupBy { surnameOf(it.author!!) }
            .filterKeys { it.isNotBlank() }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, List<Book>>> { it.value.size }.thenBy { it.key })
            .take(AUTHORS_SIZE)
            .map { (name, own) ->
                AuthorOnShelf(name = name, monogram = name.first().uppercaseChar(), books = own.size, cover = own.maxBy { it.addedAt })
            }

    /**
     * Фамилия для подписи: «Лев Толстой» → «Толстой», «Толстой Лев Николаевич» → «Толстой»
     * (в FB2 автор часто записан фамилией вперёд), «Л. Толстой» → «Толстой».
     */
    fun surnameOf(author: String): String {
        val words = author.split(' ', ',').map { it.trim() }.filter { it.isNotEmpty() }
        if (words.isEmpty()) return ""
        if (words.size >= 3 && words.last().lowercase().let { it.endsWith("вич") || it.endsWith("вна") || it.endsWith("ична") }) {
            return words.first()
        }
        return words.last()
    }

    /** «1 книга», «3 книги», «12 книг». */
    fun booksCount(count: Int): String {
        val word = when {
            count % 100 in 11..14 -> "книг"
            count % 10 == 1 -> "книга"
            count % 10 in 2..4 -> "книги"
            else -> "книг"
        }
        return "$count $word"
    }

    /**
     * Порядок блоков авторов «пирамидой»: самый частый в центре, дальше поочерёдно слева и справа.
     * Возвращает индексы в исходном списке для позиций слева направо.
     */
    fun pyramidOrder(size: Int): List<Int> {
        val order = ArrayDeque<Int>()
        for (rank in 0 until size) if (rank % 2 == 1) order.addFirst(rank) else order.addLast(rank)
        return order.toList()
    }
}

internal data class Shelf(val filter: LibraryFilter, val name: String, val count: Int, val fan: List<Book>)

internal data class AuthorOnShelf(val name: String, val monogram: Char, val books: Int, val cover: Book)
