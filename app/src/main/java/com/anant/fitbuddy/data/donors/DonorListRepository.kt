package com.anant.fitbuddy.data.donors

import com.anant.fitbuddy.data.remote.NetworkModule
import com.anant.fitbuddy.data.settings.SettingsRepository
import com.anant.fitbuddy.util.DateUtils
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

    fun containsSupportId(file: DonorsFile, supportId: String): Boolean =
        findBySupportId(file, supportId) != null

    fun findBySupportId(file: DonorsFile, supportId: String): DonorEntry? {
        val hash = SupportIdHasher.hash(supportId)
        if (hash.isEmpty()) return null
        return file.donors.firstOrNull { it.normalizedHash == hash }
    }

    /**
     * Themes only. Other roster perks (e.g. donation-reminder off) use [containsSupportId]
     * and stay available for every listed donor, including standard.
     */
    fun unlocksThemes(file: DonorsFile, supportId: String): Boolean {
        val entry = findBySupportId(file, supportId) ?: return false
        return DonorBadgeRules.unlocksThemes(entry)
    }

    /**
     * Named donors with a thank-you–eligible donation: new vs [lastSeenDonationDate], and
     * either on/after [signupDate] (unbounded) or within [THANK_YOU_MAX_AGE_MONTHS] before
     * signup (or before today when signup is unset). Gallery uses [allDisplayableDonors].
     *
     * Callers should also check [isPastThankYouInstallDelay] before the first thank-you
     * (when last-seen is still unset) so new installs are not prompted on day one.
     */
    fun newDisplayableDonors(
        file: DonorsFile,
        lastSeenDonationDate: String?,
        signupDate: String? = null,
        today: String = DateUtils.today(),
    ): List<DonorEntry> {
        val ageAnchor = DonorDates.normalizeOrNull(signupDate) ?: today
        val preSignupNotBefore = DateUtils.addCalendarMonths(ageAnchor, -THANK_YOU_MAX_AGE_MONTHS)
        return file.donors.filter { entry ->
            entry.normalizedHash.isNotEmpty() &&
                entry.hasDisplayInfo &&
                DonorBadgeRules.hasThankYouEligibleDonation(
                    donor = entry,
                    lastSeenDate = lastSeenDonationDate,
                    signupDate = signupDate,
                    preSignupNotBefore = preSignupNotBefore,
                )
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
        /** Thank-you banner hides donations older than this many months before signup. */
        const val THANK_YOU_MAX_AGE_MONTHS = 6
        /** Days after signup before the first supporters thank-you may appear. */
        const val THANK_YOU_INSTALL_DELAY_DAYS = 3

        /**
         * Fresh installs wait [THANK_YOU_INSTALL_DELAY_DAYS] after signup before the first
         * supporters thank-you. Missing/blank signup → not ready (wait until signup is minted).
         */
        fun isPastThankYouInstallDelay(
            signupDate: String?,
            today: String = DateUtils.today(),
        ): Boolean {
            val signup = DonorDates.normalizeOrNull(signupDate) ?: return false
            return today >= DateUtils.addDays(signup, THANK_YOU_INSTALL_DELAY_DAYS)
        }
    }
}
