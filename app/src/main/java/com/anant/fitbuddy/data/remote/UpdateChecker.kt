package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.remote.dto.GithubReleaseDto

/** Result of comparing the latest GitHub release against the running build. */
sealed interface UpdateCheckResult {
    data class Available(
        val versionName: String,
        val versionCode: Int,
        val downloadUrl: String,
        val releaseNotes: String,
        val htmlUrl: String
    ) : UpdateCheckResult

    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

/**
 * Sideload-friendly update check: the app isn't Play Store distributed, so we query the repo's
 * GitHub Releases for the newest **CI** build (`v*-buildN`) instead of relying on a store
 * update prompt. F-Droid tags (`vX.Y.Z`) are ignored — those binaries are for the store only.
 */
class UpdateChecker(private val api: GithubApi) {

    // release.yml tags releases "v<versionName>-build<run_number>"; run_number is also the
    // versionCode, so this regex ties the two together without a separate version field.
    private val buildNumberRegex = Regex("build(\\d+)$")

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateCheckResult {
        return try {
            val release = api.listReleases().firstOrNull(::isCiSideloadRelease)
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
                htmlUrl = release.htmlUrl
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Update check failed")
        }
    }

    /** CI sideload releases only — not drafts, not F-Droid clean tags. */
    private fun isCiSideloadRelease(release: GithubReleaseDto): Boolean {
        if (release.draft) return false
        if (!buildNumberRegex.containsMatchIn(release.tagName)) return false
        return release.assets.any { it.name.endsWith(".apk", ignoreCase = true) }
    }
}
