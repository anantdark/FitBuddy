# F-Droid branch

This branch (`fdroid`) is the FOSS build FitBuddy submits to [F-Droid](https://f-droid.org/).
GitHub Releases and Play stay on `main` / `develop`.

## What differs from `develop`

| Area | F-Droid branch |
|------|----------------|
| Barcode scan | ZXing (`com.google.zxing:core`) — no ML Kit |
| Play Services | Removed |
| In-app APK updates | **Removed** (no `UpdateChecker` / `ApkInstaller`) |
| Crash reporting | **Removed** (no Sentry) |
| Cloud backup | **Removed** (no MongoDB Atlas) |
| Local JSON backup | Kept (export / import) |
| Version | Literal `versionCode` / `versionName` in `app/build.gradle.kts` |
| Store metadata | `fastlane/metadata/android/en-US/` |

Optional AI providers (OpenRouter, Gemini, Ollama) remain available at runtime and are
documented as a NonFreeNet AntiFeature when the user enables them.

## Do not reintroduce on this branch

- `com.google.android.gms:*` / `com.google.mlkit:*`
- Sentry / `io.sentry:*`
- MongoDB driver / Atlas cloud backup
- GitHub APK sideload updater (`ApkInstaller` / update prompts)
- CI-only `-PappVersionCode` / `-PappVersionName` as the sole version source

## Release a new F-Droid version

1. On `develop`, finish the feature work and merge as usual.
2. Update this branch:
   ```bash
   git checkout fdroid
   git merge develop   # or: git rebase develop
   ```
   Resolve conflicts carefully for barcode deps, removed network SDKs, and version literals.
3. Bump in `app/build.gradle.kts`:
   - `versionCode` (must increase)
   - `versionName` (e.g. `3.1.2`)
   - matching `releaseApkVersionName` if present
4. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (max 500 characters).
5. Commit, then tag a **clean** version (no `-buildN`):
   ```bash
   git tag v3.1.2
   git push origin fdroid
   git push origin v3.1.2
   ```
6. Run **Actions → F-Droid Release** with that tag. The workflow attaches
   `FitBuddy-<version>.apk` and marks the GitHub release as **prerelease** with
   `make_latest: false` so it never becomes `/releases/latest`.
7. After inclusion, F-Droid `checkupdates` picks clean `vX.Y.Z` tags.

## Signing

F-Droid signs the published APK with **their** key. Users who installed from GitHub or Play cannot update in place to the F-Droid build (different signature).

## Draft fdroiddata metadata

See [`metadata/com.anant.fitbuddy.yml`](metadata/com.anant.fitbuddy.yml).
