package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.remote.dto.GithubReleaseDto
import com.anant.fitbuddy.util.FdroidUpdateLauncher
import okhttp3.OkHttpClient
import okhttp3.Request

/** Where the update should be obtained from. */
enum class UpdateChannel {
    /** GitHub Releases APK (github flavor / sideload). */
    GITHUB,

    /** F-Droid client / website (fdroid flavor). */
    FDROID,
}

/** Result of comparing a remote channel against the running build. */
sealed interface UpdateCheckResult {
    data class Available(
        val versionName: String,
        val versionCode: Int,
        val downloadUrl: String,
        val releaseNotes: String,
        val htmlUrl: String,
        val channel: UpdateChannel = UpdateChannel.GITHUB,
    ) : UpdateCheckResult

    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

/**
 * Update check for both distribution flavors:
 * - **github**: latest CI sideload release (`v*-buildN`) on GitHub Releases
 * - **fdroid**: latest suggested package on f-droid.org (never points at GitHub APKs)
 */
class UpdateChecker(
    private val githubApi: GithubApi,
    private val fdroidApi: FdroidApi,
    private val okHttpClient: OkHttpClient,
    private val isFdroidBuild: Boolean = BuildConfig.IS_FDROID,
    private val fdroidPackageName: String = FdroidUpdateLauncher.PACKAGE_ID,
) {

    private val buildNumberRegex = Regex("build(\\d+)$")

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateCheckResult =
        if (isFdroidBuild) {
            checkFdroid(currentVersionCode)
        } else {
            checkGithub(currentVersionCode)
        }

    private suspend fun checkGithub(currentVersionCode: Int): UpdateCheckResult {
        return try {
            val release = githubApi.listReleases().firstOrNull(::isCiSideloadRelease)
                ?: return UpdateCheckResult.Error("No GitHub CI release with an APK found")

            val remoteVersionCode = buildNumberRegex.find(release.tagName)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
                ?: return UpdateCheckResult.Error("Could not read version from latest release")

            if (remoteVersionCode <= currentVersionCode) {
                return UpdateCheckResult.UpToDate
            }

            val apkAsset = release.assets.firstOrNull { it.name == "FitBuddy-latest.apk" }
                ?: release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return UpdateCheckResult.Error("Latest release has no APK attached")

            UpdateCheckResult.Available(
                versionName = release.tagName
                    .removePrefix("v")
                    .substringBefore("-build")
                    .ifBlank { release.name },
                versionCode = remoteVersionCode,
                downloadUrl = apkAsset.downloadUrl,
                releaseNotes = release.body.orEmpty(),
                htmlUrl = release.htmlUrl,
                channel = UpdateChannel.GITHUB,
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Update check failed")
        }
    }

    private suspend fun checkFdroid(currentVersionCode: Int): UpdateCheckResult {
        return try {
            val pkg = fdroidApi.getPackage(fdroidPackageName)
            val suggested = pkg.suggestedVersionCode
            if (suggested <= 0) {
                return UpdateCheckResult.Error("F-Droid did not report a suggested version")
            }
            if (suggested <= currentVersionCode) {
                return UpdateCheckResult.UpToDate
            }
            val version = pkg.packages.firstOrNull { it.versionCode == suggested }
                ?: pkg.packages.maxByOrNull { it.versionCode }
                ?: return UpdateCheckResult.Error("F-Droid package list is empty")
            val versionName = version.versionName.ifBlank { suggested.toString() }
            val versionCode = version.versionCode.takeIf { it > 0 } ?: suggested
            val notes = fetchFdroidChangelog(versionName, versionCode)
            UpdateCheckResult.Available(
                versionName = versionName,
                versionCode = versionCode,
                downloadUrl = FdroidUpdateLauncher.WEB_URL,
                releaseNotes = notes,
                htmlUrl = FdroidUpdateLauncher.WEB_URL,
                channel = UpdateChannel.FDROID,
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "F-Droid update check failed")
        }
    }

    /**
     * Best-effort What's new from the tagged F-Droid release commit's fastlane changelog.
     * Empty when the tag/file is missing (F-Droid may lag GitHub tags).
     */
    private fun fetchFdroidChangelog(versionName: String, versionCode: Int): String {
        val tag = "v$versionName-fdroid"
        val url =
            "https://raw.githubusercontent.com/anantdark/FitBuddy/$tag/" +
                "fastlane/metadata/android/en-US/changelogs/$versionCode.txt"
        return runCatching {
            val request = Request.Builder().url(url).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching ""
                response.body?.string().orEmpty().trim()
            }
        }.getOrDefault("")
    }

    /** CI sideload releases only — not drafts, not F-Droid clean tags. */
    private fun isCiSideloadRelease(release: GithubReleaseDto): Boolean {
        if (release.draft) return false
        if (!buildNumberRegex.containsMatchIn(release.tagName)) return false
        return release.assets.any { it.name.endsWith(".apk", ignoreCase = true) }
    }
}
