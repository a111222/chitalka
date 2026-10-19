package ru.chitalka.core.domain

/**
 * Импорт книг с телефона. Файл копируется в папку приложения, поэтому книга остаётся
 * в шкафу, даже если оригинал удалят из «Загрузок».
 */
interface BookImporter {
    /** @param uris адреса файлов (content://…), которые выбрал пользователь */
    suspend fun import(uris: List<String>): List<ImportResult>
}

sealed interface ImportResult {
    data class Imported(val bookId: Long, val title: String) : ImportResult

    /** Такая книга уже стоит в шкафу (совпал хеш файла). */
    data class Duplicate(val bookId: Long) : ImportResult

    data class Failed(val fileName: String, val reason: String) : ImportResult
}
