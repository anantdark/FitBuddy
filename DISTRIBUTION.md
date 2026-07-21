# Publishing FitBuddy

Package: `com.anant.fitbuddy`  
Display name: **FitBuddy**

## 1. Create a local/dev keystore (machine testing)

Use a **separate** keystore for local `assembleRelease` / sideloading. Do **not** put the
CI/Play release keystore in `keystore.properties` on your laptop.

```bash
keytool -genkeypair -v \
  -keystore fitbuddy-local.jks \
  -alias fitbuddy-local \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype JKS
```

```bash
cp keystore.properties.example keystore.properties
# Point storeFile at fitbuddy-local.jks and fill in the local passwords
```

`*.jks` and `keystore.properties` are gitignored.

## 2. Create the Play/CI release keystore (once)

Generate once, store only in a password manager + GitHub Actions secrets — not in the repo
and not as your everyday local `keystore.properties`:

```bash
keytool -genkeypair -v \
  -keystore fitbuddy-release.jks \
  -alias fitbuddy \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype JKS
```

## 3. GitHub Actions (required for Releases)

Add these repository secrets so CI always signs with the **release** key (otherwise each runner
uses a different debug keystore and in-app updates fail between releases):

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | `base64 -i fitbuddy-release.jks \| pbcopy` |
| `RELEASE_STORE_PASSWORD` | Release store password |
| `RELEASE_KEY_ALIAS` | Release alias (default `fitbuddy`) |
| `RELEASE_KEY_PASSWORD` | Release key password |

Sentry DSN and the cloud-backup API key are not CI secrets — both are committed obfuscated
(XOR + Base64) directly in `app/build.gradle.kts`, so no env vars are needed to build. To
rotate one, XOR the new plaintext against its mask seed (`sentryDsnMaskSeed` /
`backupApiKeyMaskSeed` in that file), Base64-encode, and paste the result in as the new
`...BlobEscaped` literal. Rotating the API key in Vercel also instantly invalidates any
previously leaked copy, since the proxy checks against its live env var.

The app never holds Atlas credentials — those live only in the fitbuddy-cloud-backup Vercel
project's env vars. `CLOUD_BACKUP_BASE_URL` and `MONGO_DB_NAME` are non-secret defaults in
`app/build.gradle.kts` / `local.properties.example`. Override via env if needed.

Cloud backup uses a build-baked API key (never pasted in Settings) to call the HTTPS proxy.
Guest installs opt in via **Enable cloud backup**; restores are onboarding-only (or Developer
tools). Auto-upload runs on app startup with a 12-hour debounce unless the user taps **Upload now**.

The release workflow writes a temporary `keystore.properties` on the runner from these secrets.
Local `keystore.properties` is never used by CI.

After switching to a new release keystore, uninstall older installs once and install the first
APK signed with that key.

## 4. Build a release APK / AAB

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# Optimized APK (side-loading / testing) — signed with local keystore.properties if present
./gradlew :app:assembleRelease

# Play Store bundle (recommended for Play Console) — use CI or a machine with the release key
./gradlew :app:bundleRelease
```

Outputs:
- APK: `app/build/outputs/apk/release/FitBuddy-<versionName>.apk` (e.g. `FitBuddy-3.0.0-dev.apk`)
- AAB: `app/build/outputs/bundle/release/app-release.aab`

## 5. Play Console checklist

- [ ] Upload `app-release.aab`
- [ ] App icon: adaptive icon in `res/mipmap-anydpi/` + store listing PNG at `assets/fitbuddy_icon.png`
- [ ] Privacy policy URL (required if collecting health/fitness data)
- [ ] Content rating questionnaire
- [ ] Target API level matches `targetSdk` in `app/build.gradle.kts`
- [ ] Test on release build (debug Compose is slower; release uses R8 + resource shrink)

## 6. Version bumps

Edit `versionCode` and `versionName` in `app/build.gradle.kts` before each store upload.
Local/dev builds keep the fallback versions; CI sets them via `-PappVersionCode` / `-PappVersionName`.

## 7. GitHub “Latest” vs F-Droid

CI releases on `main` (`v*-buildN`, with `FitBuddy-latest.apk`) own GitHub **Latest** —
the website and in-app updater use those. F-Droid publishes clean `vX.Y.Z` tags as
**prerelease** with `make_latest: false` (see `fdroid` branch workflow); F-Droid installs
via the tag `Binaries` URL only, never `/releases/latest`.
