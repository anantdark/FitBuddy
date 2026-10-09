package com.anant.fitbuddy.data.donors

import com.anant.fitbuddy.data.remote.NetworkModule
import com.anant.fitbuddy.data.settings.SettingsRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches the public donors list from GitHub and caches the last successful payload.
 * Matching uses [SupportIdHasher]; thank-you diffs use last-seen donation date.
 */
class DonorListRepository(
    private val settingsRepository: SettingsRepository,
    private val client: OkHttpClient = NetworkModule.okHttpClient(),
    private val moshi: Moshi = NetworkModule.moshi,
) {
    private val mutex = Mutex()
    @Volatile
    private var memoryCache: DonorsFile? = null

    private val adapter by lazy { moshi.adapter(DonorsFile::class.java) }

    suspend fun cachedOrEmpty(): DonorsFile =
        memoryCache
            ?: settingsRepository.cachedDonorsJson()?.let { parse(it) }
            ?: DonorsFile()

    /**
     * Fetches remote donors when possible; falls back to cache on failure.
     * Returns the list used for this sync.
     */
    suspend fun refresh(forceNetwork: Boolean = true): DonorsFile = mutex.withLock {
        if (!forceNetwork) {
            val cached = cachedOrEmpty()
            if (cached.donors.isNotEmpty() || memoryCache != null) return cached
        }
        val remote = withContext(Dispatchers.IO) { fetchRemote() }
        if (remote != null) {
            memoryCache = remote
            settingsRepository.setCachedDonorsJson(adapter.toJson(remote))
            remote
        } else {
            cachedOrEmpty()
        }
    }

    fun containsSupportId(file: DonorsFile, supportId: String): Boolean {
        val hash = SupportIdHasher.hash(supportId)
        if (hash.isEmpty()) return false
        return file.donors.any { it.normalizedHash == hash }
    }

    /**
     * Named donors with at least one donation strictly after [lastSeenDonationDate].
     * Preserves [DonorsFile.donors] array order.
     */
    fun newDisplayableDonors(
        file: DonorsFile,
        lastSeenDonationDate: String?,
    ): List<DonorEntry> {
        val floor = DonorDates.normalizeOrNull(lastSeenDonationDate)
        return file.donors.filter { entry ->
            entry.normalizedHash.isNotEmpty() &&
                entry.hasDisplayInfo &&
                DonorBadgeRules.hasDonationAfter(entry, floor)
        }
    }

    fun allDisplayableDonors(file: DonorsFile): List<DonorEntry> =
        file.donors.filter { it.hasDisplayInfo && it.normalizedHash.isNotEmpty() }

    private fun fetchRemote(): DonorsFile? {
        return runCatching {
            val request = Request.Builder()
                .url(DONORS_URL)
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body.string()
                if (body.isBlank()) return null
                parse(body)
            }
        }.getOrNull()
    }

    private fun parse(json: String): DonorsFile? =
        runCatching { adapter.fromJson(json)?.withRosterIndices() }.getOrNull()

    companion object {
        const val DONORS_URL =
            "https://raw.githubusercontent.com/anantdark/FitBuddy/main/config/donors.json"
    }
}
