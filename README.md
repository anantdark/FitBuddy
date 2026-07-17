# FitBuddy

AI-powered health tracker for Android, tuned for Indian diets and daily routines. Log meals and workouts with a photo or loose text; an LLM estimates calories and macros. Includes dashboards, progress charts, editable meal review, and reusable food presets.

[![CI](https://github.com/anantdark/FitBuddy/actions/workflows/ci.yml/badge.svg)](https://github.com/anantdark/FitBuddy/actions/workflows/ci.yml)
[![Release](https://github.com/anantdark/FitBuddy/actions/workflows/release.yml/badge.svg)](https://github.com/anantdark/FitBuddy/actions/workflows/release.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

## Features

- **Smart logging** — photo or text input; AI parses food and exercise
- **Meal review** — edit dish name, tweak ingredient weights with live macro recalc
- **Presets** — bookmark meals for one-tap logging
- **Dashboard** — daily calorie ring, food/exercise logs, macro breakdown
- **Analytics** — custom Canvas charts for trends over time
- **Offline fallback** — works without AI config via built-in simulator
- **Material You** — dynamic color theming on Android 12+

## Download

Pre-built APKs and AABs are attached to [GitHub Releases](https://github.com/anantdark/FitBuddy/releases). Every push to `main` (including merged PRs) publishes a new build tagged `v{versionName}-build{N}`.

## AI providers

Configure at runtime in **Settings** (stored locally via DataStore):

| Provider | Notes |
|----------|-------|
| **OpenRouter** | Free vision models via model dropdown |
| **Gemini** | Google AI Studio key; OpenAI-compatible endpoint |
| **Ollama** | Local models (e.g. `llava`); HTTP cleartext enabled for LAN |

First-run defaults can be seeded from `local.properties` (see below). Without a key, the app uses the offline simulator.

## Build from source

### Requirements

- JDK 21
- Android SDK with API 36.1 (compile) / API 29+ (min)
- macOS/Linux/WSL (Gradle wrapper included)

### Setup

```bash
git clone https://github.com/anantdark/FitBuddy.git
cd FitBuddy

# SDK path + optional AI defaults (never commit this file)
cp local.properties.example local.properties
# Edit local.properties — set sdk.dir and optional API keys
```

### Compile & test

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home  # macOS Homebrew

./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

### Install release build (dev)

Release builds use R8 minify + resource shrink. Without `keystore.properties`, release is signed with the debug key for local sideloading:

```bash
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

For Play Store signing, see [DISTRIBUTION.md](DISTRIBUTION.md).

## Project structure

```
app/src/main/java/com/anant/fitbuddy/
├── FitBuddyApp.kt          # Application / manual DI
├── data/
│   ├── database/           # Room entities & DAOs
│   ├── model/              # API & domain models
│   ├── remote/             # Retrofit, AI prompts, DTOs
│   ├── repository/         # Business logic
│   └── settings/           # DataStore preferences
├── ui/
│   ├── screens/            # Compose screens
│   ├── viewmodel/          # MainViewModel
│   └── components/         # Charts, snackbar, etc.
└── util/                   # DateUtils, ImageUtils
```

Architecture: **MVVM** — UI → ViewModel → Repository → Room + Remote AI.

## CI / CD

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| [CI](.github/workflows/ci.yml) | Push & PR to `main` | Compile debug/release, run unit tests |
| [Release](.github/workflows/release.yml) | Push to `main` | Build signed APK + AAB, publish GitHub Release |

Merged PRs trigger release automatically (merge creates a push to `main`).

### Optional: signed release builds in CI

Add these [repository secrets](https://github.com/anantdark/FitBuddy/settings/secrets/actions) to sign releases with your upload key instead of the debug key:

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded `.jks` file (`base64 -i fitbuddy-release.jks \| pbcopy`) |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias (default: `fitbuddy`) |
| `RELEASE_KEY_PASSWORD` | Key password |

Without these secrets, CI release builds fall back to debug signing (fine for sideloading, not for Play Store).

## Contributing

1. Fork and create a feature branch
2. Run `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
3. Open a PR against `main` — CI must pass

Bug reports and feature requests: [Issues](https://github.com/anantdark/FitBuddy/issues).

## License

GPL-3.0 — see [LICENSE](LICENSE).

## Related docs

- [DISTRIBUTION.md](DISTRIBUTION.md) — Play Store publishing & keystore setup
