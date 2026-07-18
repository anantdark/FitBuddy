package com.anant.fitbuddy.data.settings

import java.time.Instant
import java.time.ZoneOffset

/**
 * Persisted "don't try this model until …" records for Auto failover.
 * Survives app restarts; expired entries are ignored and pruned on read/write.
 */
data class ModelCooldown(
    val provider: AiProvider,
    val modelId: String,
    /** Epoch millis (UTC) — skip this model until this instant. */
    val untilEpochMs: Long
) {
    fun storageKey(): String = keyOf(provider, modelId)

    companion object {
        fun keyOf(provider: AiProvider, modelId: String): String =
            "${provider.name}|$modelId"
    }
}

object ModelCooldownPolicy {
    /** True when [error] looks like a rate/quota limit (worth cooling the model). */
    fun isRateLimitError(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is retrofit2.HttpException && current.code() == 429) return true
            val msg = current.message?.lowercase().orEmpty()
            if ("429" in msg || "rate limit" in msg || "rate-limited" in msg || "rate limited" in msg ||
                "quota" in msg
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    /**
     * Cooldown ends at the next UTC midnight after [nowEpochMs].
     * Newer requests then try the highest (non-cooled) models again.
     */
    fun cooldownUntilEpochMs(nowEpochMs: Long = System.currentTimeMillis()): Long {
        val utcDate = Instant.ofEpochMilli(nowEpochMs).atZone(ZoneOffset.UTC).toLocalDate()
        return utcDate.plusDays(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }
}

/** Encode/decode cooldown map for DataStore (one line: PROVIDER|modelId=untilEpochMs). */
fun encodeModelCooldowns(cooldowns: Map<String, Long>): String =
    cooldowns.entries
        .sortedBy { it.key }
        .joinToString("\n") { "${it.key}=${it.value}" }

fun decodeModelCooldowns(raw: String?, nowEpochMs: Long = System.currentTimeMillis()): Map<String, Long> {
    if (raw.isNullOrBlank()) return emptyMap()
    return raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && '=' in it }
        .mapNotNull { line ->
            val eq = line.lastIndexOf('=')
            if (eq <= 0) return@mapNotNull null
            val key = line.substring(0, eq)
            val until = line.substring(eq + 1).toLongOrNull() ?: return@mapNotNull null
            if (until <= nowEpochMs) null else key to until
        }
        .toMap()
}
