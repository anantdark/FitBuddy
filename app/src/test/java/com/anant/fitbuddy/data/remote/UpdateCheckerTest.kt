package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.remote.dto.FdroidPackageDto
import com.anant.fitbuddy.data.remote.dto.FdroidPackageVersionDto
import com.anant.fitbuddy.data.remote.dto.GithubAssetDto
import com.anant.fitbuddy.data.remote.dto.GithubReleaseDto
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `fdroid build reports update when suggested version is newer`() = runTest {
        val checker = UpdateChecker(
            githubApi = FakeGithubApi(),
            fdroidApi = FakeFdroidApi(
                FdroidPackageDto(
                    packageName = "com.anant.fitbuddy",
                    suggestedVersionCode = 80,
                    packages = listOf(
                        FdroidPackageVersionDto("3.2.80", 80),
                        FdroidPackageVersionDto("3.2.79", 79),
                    )
                )
            ),
            okHttpClient = OkHttpClient(),
            isFdroidBuild = true,
        )
        val result = checker.checkForUpdate(currentVersionCode = 76)
        assertTrue(result is UpdateCheckResult.Available)
        val available = result as UpdateCheckResult.Available
        assertEquals(80, available.versionCode)
        assertEquals("3.2.80", available.versionName)
        assertEquals(UpdateChannel.FDROID, available.channel)
        assertTrue(available.htmlUrl.contains("f-droid.org"))
    }

    @Test
    fun `fdroid build is up to date when suggested is current`() = runTest {
        val checker = UpdateChecker(
            githubApi = FakeGithubApi(),
            fdroidApi = FakeFdroidApi(
                FdroidPackageDto(
                    suggestedVersionCode = 76,
                    packages = listOf(FdroidPackageVersionDto("3.2.76", 76))
                )
            ),
            okHttpClient = OkHttpClient(),
            isFdroidBuild = true,
        )
        assertEquals(UpdateCheckResult.UpToDate, checker.checkForUpdate(76))
    }

    @Test
    fun `github build never uses fdroid channel`() = runTest {
        val checker = UpdateChecker(
            githubApi = FakeGithubApi(
                listOf(
                    GithubReleaseDto(
                        tagName = "v3.3.24-build124",
                        name = "FitBuddy 3.3.24",
                        body = "- feat: something",
                        htmlUrl = "https://github.com/anantdark/FitBuddy/releases/tag/v3.3.24-build124",
                        draft = false,
                        assets = listOf(
                            GithubAssetDto(
                                name = "FitBuddy-latest.apk",
                                downloadUrl = "https://example.com/FitBuddy-latest.apk"
                            )
                        )
                    )
                )
            ),
            fdroidApi = FakeFdroidApi(
                FdroidPackageDto(suggestedVersionCode = 999, packages = emptyList())
            ),
            okHttpClient = OkHttpClient(),
            isFdroidBuild = false,
        )
        val result = checker.checkForUpdate(100)
        assertTrue(result is UpdateCheckResult.Available)
        assertEquals(UpdateChannel.GITHUB, (result as UpdateCheckResult.Available).channel)
        assertEquals(124, result.versionCode)
    }

    private class FakeFdroidApi(
        private val dto: FdroidPackageDto
    ) : FdroidApi {
        override suspend fun getPackage(packageName: String): FdroidPackageDto = dto
    }

    private class FakeGithubApi(
        private val releases: List<GithubReleaseDto> = emptyList()
    ) : GithubApi {
        override suspend fun listReleases(perPage: Int): List<GithubReleaseDto> = releases
    }
}
