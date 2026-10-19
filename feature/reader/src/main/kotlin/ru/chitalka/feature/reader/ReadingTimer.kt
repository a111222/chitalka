package ru.chitalka.feature.reader

/**
 * Считает, сколько человек читал. Время идёт, пока читалка на экране; каждое перелистывание
 * отдаёт накопленное с прошлого раза. Если страницу не листали дольше [idleLimitMs]
 * (телефон отложили, отвлеклись), засчитывается не больше этого предела.
 */
internal class ReadingTimer(
    private val idleLimitMs: Long = IDLE_LIMIT_MS,
    private val now: () -> Long,
) {
    private var last: Long? = null

    fun start() {
        last = now()
    }

    /** Сколько миллисекунд прочитано с прошлого вызова. До [start] — ноль. */
    fun tick(): Long {
        val previous = last ?: return 0L
        val current = now()
        last = current
        return (current - previous).coerceIn(0L, idleLimitMs)
    }

    fun stop(): Long = tick().also { last = null }

    companion object {
        const val IDLE_LIMIT_MS = 5 * 60_000L
    }
}
