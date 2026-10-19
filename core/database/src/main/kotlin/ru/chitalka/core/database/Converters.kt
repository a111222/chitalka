package ru.chitalka.core.database

import androidx.room.TypeConverter
import java.time.Instant

/** Room не умеет хранить Instant напрямую — храним миллисекунды. */
internal class Converters {
    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
