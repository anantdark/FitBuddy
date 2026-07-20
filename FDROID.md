# F-Droid branch

This branch (`fdroid`) is the FOSS build FitBuddy submits to [F-Droid](https://f-droid.org/).
GitHub Releases and Play stay on `main` / `develop`.

## What differs from `develop`

| Area | F-Droid branch |
|------|----------------|
| Barcode scan | ZXing (`com.google.zxing:core`) — no ML Kit |
| Play Services | Removed (unused `play-services-location`) |
| In-app APK updates | Disabled (`BuildConfig.FDROID`); no `REQUEST_INSTALL_PACKAGES` |
| Crash reports | Opt-in (default off) |
| Version | Literal `versionCode` / `versionName` in `app/build.gradle.kts` |
| Store metadata | `fastlane/metadata/android/en-US/` |

Optional network features (AI providers, Sentry, MongoDB Atlas backup) remain available and are documented as AntiFeatures in the fdroiddata recipe.

## Do not reintroduce on this branch

- `com.google.android.gms:*` / `com.google.mlkit:*`
- GitHub APK sideload updater (`ApkInstaller` / update prompts) for end users
- Crash reporting enabled by default
- CI-only `-PappVersionCode` / `-PappVersionName` as the sole version source

## Release a new F-Droid version

1. On `develop`, finish the feature work and merge as usual.
2. Update this branch:
   ```bash
   git checkout fdroid
   git merge develop   # or: git rebase develop
   ```
   Resolve conflicts carefully for barcode deps, update gating, Sentry defaults, and version literals.
3. Bump in `app/build.gradle.kts`:
   - `versionCode` (must increase)
   - `versionName` (e.g. `3.1.1`)
   - matching `releaseApkVersionName` if present
4. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (max 500 characters).
5. Commit, then tag a **clean** version (no `-buildN`):
   ```bash
   git tag v3.1.1
   git push origin fdroid
   git push origin v3.1.1
   ```
6. After inclusion, F-Droid `checkupdates` picks clean `vX.Y.Z` tags (CI tags like `v*-build*` are ignored via metadata).

## Screenshots

Add at least two PNGs under:

`fastlane/metadata/android/en-US/images/phoneScreenshots/`

named `1.png`, `2.png`, … before or during the [fdroiddata](https://gitlab.com/fdroid/fdroiddata) merge request.

## Signing

F-Droid signs the published APK with **their** key. Users who installed from GitHub or Play cannot update in place to the F-Droid build (different signature). Mention this in listing text if needed.

## Draft fdroiddata metadata

See [`metadata/com.anant.fitbuddy.yml`](metadata/com.anant.fitbuddy.yml) — copy into a fork of fdroiddata and open an MR after this branch and a clean tag are on GitHub.

Quick start: https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/
