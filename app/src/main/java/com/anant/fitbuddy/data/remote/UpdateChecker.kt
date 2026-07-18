package com.anant.fitbuddy.data.remote

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
 * latest GitHub release directly instead of relying on a store update prompt.
 */
class UpdateChecker(private val api: GithubApi) {

    // release.yml tags releases "v<versionName>-build<run_number>"; run_number is also the
    // versionCode, so this regex ties the two together without a separate version field.
    private val buildNumberRegex = Regex("build(\\d+)$")

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateCheckResult {
        return try {
            val release = api.getLatestRelease()
            val remoteVersionCode = buildNumberRegex.find(release.tagName)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
                ?: return UpdateCheckResult.Error("Could not read version from latest release")

            if (remoteVersionCode <= currentVersionCode) {
                return UpdateCheckResult.UpToDate
            }

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return UpdateCheckResult.Error("Latest release has no APK attached")

            UpdateCheckResult.Available(
                versionName = release.name,
                versionCode = remoteVersionCode,
                downloadUrl = apkAsset.downloadUrl,
                releaseNotes = release.body.orEmpty(),
                htmlUrl = release.htmlUrl
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Update check failed")
        }
    }
}
