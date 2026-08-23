package com.anant.fitbuddy.bridge

import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Wire format shared with FreeScale (`anant-scale-bridge` v1).
 *
 * Display/body-comp fields map onto [BodyMeasurement] columns. Optional
 * [KEY_FREESCALE_PAYLOAD] is stored opaquely as [BodyMeasurement.freescalePayloadJson]
 * for FreeScale restore and is never shown in FitBuddy UI.
 * Doubles are rounded to 2 decimal places on ingest/export.
 */
object ScaleBridgeJson {
    const val FORMAT = "anant-scale-bridge"
    const val VERSION = 1
    const val KEY_FREESCALE_PAYLOAD = "freescalePayload"

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
        val payload = when {
            o.has(KEY_FREESCALE_PAYLOAD) && !o.isNull(KEY_FREESCALE_PAYLOAD) -> {
                val raw = o.get(KEY_FREESCALE_PAYLOAD)
                when (raw) {
                    is JSONObject -> raw.toString()
                    is String -> raw.takeIf { it.isNotBlank() }
                    else -> null
                }
            }
            else -> null
        }
        return BodyMeasurement(
            timestamp = timestamp,
            dateString = dateString,
            weightKg = round2(o.getDouble("weightKg")),
            bmi = optDouble(o, "bmi"),
            bodyFatPct = optDouble(o, "bodyFatPct"),
            muscleRatePct = optDouble(o, "muscleRatePct"),
            bodyWaterPct = optDouble(o, "bodyWaterPct"),
            boneMassKg = optDouble(o, "boneMassKg"),
            bmr = optInt(o, "bmr"),
            metabolicAge = optInt(o, "metabolicAge"),
            visceralFat = optDouble(o, "visceralFat"),
            subcutaneousFatPct = optDouble(o, "subcutaneousFatPct"),
            proteinMassKg = optDouble(o, "proteinMassKg"),
            muscleMassKg = optDouble(o, "muscleMassKg"),
            fatFreeMassKg = optDouble(o, "fatFreeMassKg"),
            skeletalMuscleMassKg = optDouble(o, "skeletalMuscleMassKg"),
            waterWeightKg = optDouble(o, "waterWeightKg"),
            fatMassKg = optDouble(o, "fatMassKg"),
            freescalePayloadJson = payload,
        )
    }

    fun encodeObject(m: BodyMeasurement): JSONObject {
        val o = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("timestamp", m.timestamp)
            .put("dateString", m.dateString)
            .put("weightKg", round2(m.weightKg))
        putOptional(o, "bmi", m.bmi)
        putOptional(o, "bodyFatPct", m.bodyFatPct)
        putOptional(o, "muscleRatePct", m.muscleRatePct)
        putOptional(o, "bodyWaterPct", m.bodyWaterPct)
        putOptional(o, "boneMassKg", m.boneMassKg)
        m.bmr?.let { o.put("bmr", it) }
        m.metabolicAge?.let { o.put("metabolicAge", it) }
        putOptional(o, "visceralFat", m.visceralFat)
        putOptional(o, "subcutaneousFatPct", m.subcutaneousFatPct)
        putOptional(o, "proteinMassKg", m.proteinMassKg)
        putOptional(o, "muscleMassKg", m.muscleMassKg)
        putOptional(o, "fatFreeMassKg", m.fatFreeMassKg)
        putOptional(o, "skeletalMuscleMassKg", m.skeletalMuscleMassKg)
        putOptional(o, "waterWeightKg", m.waterWeightKg)
        putOptional(o, "fatMassKg", m.fatMassKg)
        val payload = m.freescalePayloadJson
        if (!payload.isNullOrBlank()) {
            o.put(KEY_FREESCALE_PAYLOAD, JSONObject(payload))
        }
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
