# Publishing FitBuddy

Package: `com.anant.fitbuddy`  
Display name: **FitBuddy**

## 1. Create a release keystore

```bash
keytool -genkeypair -v \
  -keystore fitbuddy-release.jks \
  -alias fitbuddy \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype JKS
```

Keep the `.jks` file and passwords safe. Add `fitbuddy-release.jks` to `.gitignore` (already listed).

## 2. Configure signing

```bash
cp keystore.properties.example keystore.properties
# Edit keystore.properties with your paths and passwords
```

Place `fitbuddy-release.jks` in the project root (or update `storeFile` in `keystore.properties`).

Release builds automatically use the release keystore when `keystore.properties` exists; otherwise they fall back to the debug key for local testing.

## 3. Build a release APK / AAB

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# Optimized APK (side-loading / testing)
./gradlew :app:assembleRelease

# Play Store bundle (recommended for Play Console)
./gradlew :app:bundleRelease
```

Outputs:
- APK: `app/build/outputs/apk/release/FitBuddy-<versionName>.apk` (e.g. `FitBuddy-2.1.0-dev.apk`)
- AAB: `app/build/outputs/bundle/release/app-release.aab`

## 4. Play Console checklist

- [ ] Upload `app-release.aab`
- [ ] App icon: adaptive icon in `res/mipmap-anydpi/` + store listing PNG at `assets/fitbuddy_icon.png`
- [ ] Privacy policy URL (required if collecting health/fitness data)
- [ ] Content rating questionnaire
- [ ] Target API level matches `targetSdk` in `app/build.gradle.kts`
- [ ] Test on release build (debug Compose is slower; release uses R8 + resource shrink)

## 5. Version bumps

Edit `versionCode` and `versionName` in `app/build.gradle.kts` before each store upload.
