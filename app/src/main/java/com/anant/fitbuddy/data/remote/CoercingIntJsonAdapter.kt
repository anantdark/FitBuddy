package com.anant.fitbuddy.data.remote

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type
import kotlin.math.roundToInt

/**
 * Accepts JSON numbers with a fractional part for [Int] fields (e.g. `83.2` → `83`).
 * Needed for hand-authored / external backups and AI payloads that emit decimal macros.
 */
object CoercingIntJsonAdapter : JsonAdapter<Int>() {
    val FACTORY = JsonAdapter.Factory { type: Type, annotations: Set<Annotation>, _: Moshi ->
        if (annotations.isNotEmpty()) return@Factory null
        val raw = Types.getRawType(type)
        if (raw != Int::class.javaPrimitiveType && raw != Int::class.javaObjectType) {
            return@Factory null
        }
        // nullSafe so nullable Int fields (e.g. MealFood.presetId) accept JSON null.
        CoercingIntJsonAdapter.nullSafe()
    }

    override fun fromJson(reader: JsonReader): Int = reader.nextDouble().roundToInt()

    override fun toJson(writer: JsonWriter, value: Int?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value)
        }
    }
}
