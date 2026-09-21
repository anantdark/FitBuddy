package com.anant.fitbuddy.bridge

import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Wire format shared with FreeScale (`anant-scale-bridge` v1).
 *
 * FitBuddy imports and exports only weight, body-fat percentage, BMR, and muscle mass.
 * Other scale fields and opaque payloads are deliberately ignored.
 * Doubles are rounded to 2 decimal places on ingest/export.
 */
object ScaleBridgeJson {
    const val FORMAT = "anant-scale-bridge"
    const val VERSION = 1

    fun decodeMeasurement(json: String): BodyMeasurement =
        decodeObject(JSONObject(json))

    fun encodeArray(measurements: List<BodyMeasurement>): String {
        val arr = JSONArray()
        measurements.forEach { arr.put(encodeObject(it)) }
        return arr.toString()
    }

    fun decodeObject(o: JSONObject): BodyMeasurement {
        val format = o.optString("format")
        if (format.isNotEmpty() && format != FORMAT) {
            throw IllegalArgumentException("Not a scale bridge payload (format=$format)")
        }
        val version = o.optInt("version", VERSION)
        if (version < 1 || version > VERSION) {
            throw IllegalArgumentException("Unsupported bridge version $version")
        }
        val timestamp = o.getLong("timestamp")
        val dateString = o.optString("dateString").ifBlank { DateUtils.format(timestamp) }
        return BodyMeasurement(
            timestamp = timestamp,
            dateString = dateString,
            weightKg = round2(o.getDouble("weightKg")),
            bodyFatPct = optDouble(o, "bodyFatPct"),
            bmr = optInt(o, "bmr"),
            muscleMassKg = optDouble(o, "muscleMassKg"),
        )
    }

    fun encodeObject(m: BodyMeasurement): JSONObject {
        val o = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("timestamp", m.timestamp)
            .put("dateString", m.dateString)
            .put("weightKg", round2(m.weightKg))
        putOptional(o, "bodyFatPct", m.bodyFatPct)
        m.bmr?.let { o.put("bmr", it) }
        putOptional(o, "muscleMassKg", m.muscleMassKg)
        return o
    }

    private fun putOptional(o: JSONObject, key: String, value: Double?) {
        if (value != null) o.put(key, round2(value))
    }

    private fun optDouble(o: JSONObject, key: String): Double? {
        if (!o.has(key) || o.isNull(key)) return null
        return round2(o.getDouble(key))
    }

    private fun optInt(o: JSONObject, key: String): Int? {
        if (!o.has(key) || o.isNull(key)) return null
        return o.getInt(key)
    }

    fun round2(value: Double): Double =
        String.format(Locale.US, "%.2f", value).toDouble()
}
