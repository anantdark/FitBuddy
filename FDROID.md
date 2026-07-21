# F-Droid distribution

FitBuddy submits to [F-Droid](https://f-droid.org/) straight from `main` — there is no
separate branch. F-Droid builds the `fdroid` product flavor; GitHub Releases build the
`github` flavor. Same source, same commit, different `assemble<Flavor>Release` task.

## What's F-Droid-specific

| Area | Detail |
|------|--------|
| Build flavor | `assembleFdroidRelease` (the `fdroid` product flavor in `app/build.gradle.kts`), not `github` |
| Version | Fixed `versionCode` / `versionName` for the `fdroid` flavor in `app/build.gradle.kts` (F-Droid builds from source with no CI `-P` overrides) |
| Store metadata | `fastlane/metadata/android/en-US/` and `metadata/com.anant.fitbuddy.yml` |
| Sync | None needed — it's the same branch as everything else |

`BuildConfig.IS_FDROID` (set per flavor) drives the only runtime differences:

- Settings' Updates card shows "install from GitHub releases" instead of the auto-updater,
  since F-Droid owns updates for that build.
- Crash reporting and auto-update-check default to **off** (still user-toggleable).

Sentry, MongoDB Atlas cloud backup, and Google Play Services (via CameraX/location) are
compiled into this build same as the GitHub build — they're optional/opt-in and mostly
default-off, but F-Droid's scanner will flag them regardless (see AntiFeatures below).
That's an accepted tradeoff for keeping one codebase.

## Release a new F-Droid version

1. On `main`, finish the work as usual (via `develop` → PR → `main`), so the GitHub
   Releases workflow has already published the `github` flavor build for this commit.
2. Bump the `fdroid` flavor's `versionCode` / `versionName` in `app/build.gradle.kts`
   (`versionCode` must strictly increase).
3. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (max 500 characters).
4. Commit to `main`, then tag a **clean** version (no `-buildN`):
   ```bash
   git tag v3.1.2
   git push origin main
   git push origin v3.1.2
   ```
5. Run **Actions → F-Droid Release** manually (`workflow_dispatch`) with that tag. The
   workflow checks out the tag, builds `assembleFdroidRelease` with no Sentry/Mongo
   secrets (matching F-Droid's own unsigned reproducible build), and attaches
   `FitBuddy-<version>.apk` as a GitHub release marked **prerelease** with
   `make_latest: false` — it never becomes `/releases/latest`.
6. Update `metadata/com.anant.fitbuddy.yml`'s `Builds:` list with a new entry
   (`versionName`, `versionCode`, `commit` = the tagged commit, `subdir: app`,
   `gradle: - fdroid`), plus `CurrentVersion` / `CurrentVersionCode`, and submit to
   [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
7. After inclusion, F-Droid `checkupdates` picks clean `vX.Y.Z` tags.

## Signing

F-Droid signs the published APK with **their** key. Users who installed from GitHub or Play
cannot update in place to the F-Droid build (different signature).

## Draft fdroiddata metadata

See [`metadata/com.anant.fitbuddy.yml`](metadata/com.anant.fitbuddy.yml). Its `Builds` entry
must set `gradle: - fdroid` so `fdroidserver` builds the `fdroid` flavor
(`assembleFdroidRelease`), not the unflavored default.
