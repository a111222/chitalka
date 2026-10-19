package ru.chitalka.core.bookparser

/**
 * Жанры FB2 записаны кодами («sf_fantasy», «prose_classic»). На карточке книги показываем
 * их по-русски; неизвестный код подписываем по его группе, а если и группа неизвестна — «Другое».
 */
internal object Genres {
    private val exact = mapOf(
        "prose_classic" to "Классика",
        "prose_rus_classic" to "Русская классика",
        "prose_su_classics" to "Советская классика",
        "prose_contemporary" to "Современная проза",
        "prose_history" to "Историческая проза",
        "prose_military" to "Военная проза",
        "prose_counter" to "Контркультура",
        "sf" to "Фантастика",
        "sf_fantasy" to "Фэнтези",
        "sf_social" to "Социальная фантастика",
        "sf_space" to "Космическая фантастика",
        "sf_horror" to "Ужасы",
        "sf_humor" to "Юмористическая фантастика",
        "sf_cyberpunk" to "Киберпанк",
        "sf_postapocalyptic" to "Постапокалипсис",
        "sf_history" to "Альтернативная история",
        "det_classic" to "Классический детектив",
        "det_police" to "Полицейский детектив",
        "det_irony" to "Иронический детектив",
        "detective" to "Детектив",
        "thriller" to "Триллер",
        "love_contemporary" to "Любовный роман",
        "love_history" to "Исторический роман о любви",
        "adv_history" to "Исторические приключения",
        "adventure" to "Приключения",
        "child_tale" to "Сказка",
        "child_prose" to "Детская проза",
        "children" to "Детское",
        "poetry" to "Поэзия",
        "dramaturgy" to "Драматургия",
        "humor" to "Юмор",
        "nonf_biography" to "Биография",
        "nonf_publicism" to "Публицистика",
        "sci_history" to "История",
        "sci_philosophy" to "Философия",
        "sci_psychology" to "Психология",
        "religion" to "Религия",
        "antique" to "Старинная литература",
        "antique_myths" to "Мифы и легенды",
        "home" to "Дом и семья",
        "comp_programming" to "Программирование",
        "reference" to "Справочник",
    )

    private val groups = listOf(
        "sf" to "Фантастика",
        "det" to "Детектив",
        "prose" to "Проза",
        "love" to "Любовный роман",
        "adv" to "Приключения",
        "child" to "Детское",
        "poetry" to "Поэзия",
        "humor" to "Юмор",
        "nonf" to "Документальное",
        "sci" to "Наука",
        "comp" to "Компьютеры",
        "religion" to "Религия",
        "antique" to "Старинная литература",
        "home" to "Дом и семья",
        "ref" to "Справочник",
    )

    fun fb2Label(code: String): String {
        val key = code.trim().lowercase()
        exact[key]?.let { return it }
        return groups.firstOrNull { (prefix, _) -> key == prefix || key.startsWith(prefix + "_") }?.second ?: "Другое"
    }
}
