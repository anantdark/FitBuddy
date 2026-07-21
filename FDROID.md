# F-Droid branch

This branch (`fdroid`) is the build FitBuddy submits to [F-Droid](https://f-droid.org/).
GitHub Releases and Play stay on `main` / `develop`.

## What differs from `develop`

As of the `distribution` flavor split, this branch shares the same app source as
`develop`/`main` — it is not a stripped-down fork anymore. The only things unique to this
branch are:

| Area | F-Droid branch |
|------|----------------|
| Build flavor | Built with `assembleFdroidRelease` (the `fdroid` product flavor), not `github` |
| Version | Fixed `versionCode` / `versionName` for the `fdroid` flavor in `app/build.gradle.kts` (F-Droid builds from source with no CI `-P` overrides) |
| Store metadata | `fastlane/metadata/android/en-US/` and `metadata/com.anant.fitbuddy.yml` |
| Sync | Regularly merges `develop` in directly — no manual re-stripping needed |

`BuildConfig.IS_FDROID` (set per flavor) drives the only runtime differences:

- Settings' Updates card shows "install from GitHub releases" instead of the auto-updater,
  since F-Droid owns updates for this build.
- Crash reporting and auto-update-check default to **off** (still user-toggleable).

Sentry, MongoDB Atlas cloud backup, and Google Play Services (via CameraX/location) are
compiled into this build same as the GitHub build — they're optional/opt-in and mostly
default-off, but F-Droid's scanner will flag them regardless (see AntiFeatures below).
That's an accepted tradeoff for keeping one codebase instead of two divergent branches.

## Release a new F-Droid version

1. On `develop`, finish the feature work, merge as usual, push.
2. Update this branch:
   ```bash
   git checkout fdroid
   git merge develop
   ```
   Should be a clean merge — there's no more code divergence to resolve, just the
   F-Droid-only packaging paths (`fastlane/`, `metadata/`, this file, the fdroid-release
   workflow), which `develop` never touches.
3. Bump the `fdroid` flavor's `versionCode` / `versionName` in `app/build.gradle.kts`
   (`versionCode` must strictly increase).
4. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (max 500 characters).
5. Commit, then tag a **clean** version (no `-buildN`):
   ```bash
   git tag v3.1.2
   git push origin fdroid
   git push origin v3.1.2
   ```
6. Run **Actions → F-Droid Release** with that tag. The workflow attaches
   `FitBuddy-<version>.apk` (built via `assembleFdroidRelease`) and marks the GitHub
   release as **prerelease** with `make_latest: false` so it never becomes
   `/releases/latest`.
7. After inclusion, F-Droid `checkupdates` picks clean `vX.Y.Z` tags.

## Signing

F-Droid signs the published APK with **their** key. Users who installed from GitHub or Play
cannot update in place to the F-Droid build (different signature).

## Draft fdroiddata metadata

See [`metadata/com.anant.fitbuddy.yml`](metadata/com.anant.fitbuddy.yml). Its `Builds` entry
must set `gradle: - fdroid` so `fdroidserver` builds the `fdroid` flavor
(`assembleFdroidRelease`), not the unflavored default.
