# FitBuddy — Agent Context

AI-powered health tracker (Android) optimised for Indian diets & lifestyles. Log meals and
workouts via **photo or loose text**; an LLM estimates calories/macros. Also has dashboards,
progress charts, editable meal review, and reusable food presets.

> Package: `com.anant.fitbuddy` · App name: **FitBuddy** (formerly "CalorieSamrat").
> The app was fully renamed — do not reintroduce the old name in code or UI.

## Tech stack
- **Kotlin** + **Jetpack Compose** (Material 3 / Material You dynamic color).
- **MVVM + Clean-ish architecture**: UI → ViewModel → Repository → (Room + Remote AI).
- **Room** for local persistence. **DataStore (Preferences)** for settings.
- **Retrofit + Moshi** (codegen adapters, `@JsonClass(generateAdapter = true)`) + OkHttp.
- **Manual DI** via `FitBuddyApp` (service locator) + `NetworkModule` (no Hilt/Dagger).
- Coroutines + Flow/StateFlow throughout.
- Custom Canvas charts (no external chart lib).

## Build / run
- **AGP 9.3.0**, **Kotlin 2.2.10**, **compileSdk 36.1**, **minSdk 29**, **targetSdk 36**.
  - Do NOT apply the `org.jetbrains.kotlin.android` plugin — AGP 9+ has built-in Kotlin support.
  - Do NOT add `navigation3` / `material3.adaptive` deps — they pull `lifecycle:2.11` which needs
    compileSdk ≥ 37 (not installed) and previously broke the build.
- JDK: OpenJDK 21 (`JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`).
- Config cache is ON.
- **Device testing (smoke / local features / debugging):** always install and use the **debug** app
  (`com.anant.fitbuddy.debug`, launcher label **FitBuddy Dev**). Do **not** uninstall, overwrite, or
  test against the release app on the phone (`com.anant.fitbuddy`) — that install is the user's real
  app (different signing key); agents must never `adb uninstall` it or sideload a local release over
  it. See `.cursor/rules/debug-build-for-testing.mdc`.
- Commands (run from repo root):
  - Compile check: `./gradlew :app:compileDebugKotlin`
  - Debug APK (required for on-device agent work):
    `./gradlew :app:assembleDebug && adb install -r --user 0 app/build/outputs/apk/debug/app-debug.apk`
    then launch `com.anant.fitbuddy.debug/com.anant.fitbuddy.MainActivity`
  - Optimized release build: `./gradlew :app:assembleRelease` (R8 + resource shrink). Local
    `keystore.properties` must use a **local/dev** keystore (`fitbuddy-local.jks`), not the CI
    release key — see DISTRIBUTION.md. CI signs releases via GitHub `RELEASE_*` secrets only.
  - Install release over adb **only when the user explicitly asks** (personal profile only —
    never work profile / user 10):
    `adb install -r --user 0 app/build/outputs/apk/release/FitBuddy-*.apk`
  - `installDebug`/`installRelease` also pass `--user 0` via `android.installation.installOptions`.
    Wireless adb: prefer direct `adb install -r --user 0 <apk>` if Gradle's adb push hits
    EOF/broken pipe.

## Module / file map (`app/src/main/java/com/anant/fitbuddy/`)
- `FitBuddyApp.kt` — Application/service locator; builds `SettingsRepository` + `FitnessRepository`.
- `MainActivity.kt` — sets Compose content; reads `dynamicColor` before theming.
- `data/database/`
  - `Entities.kt` — `UserProfile`, `FoodLog`, `ExerciseLog`, `FoodPreset`.
  - `Daos.kt` — DAOs + result rows (`FoodDailySummary`, `ExerciseDailySummary`, `FoodTotals`).
  - `AppDatabase.kt` — Room DB **version 2**, `fallbackToDestructiveMigration(dropAllTables=true)`
    (schema changes wipe local data — fine for dev, add real migrations before shipping).
- `data/model/` — Moshi API models (`FitnessTrackerModels.kt`: `FitnessTrackerResponse`,
  `FoodAnalysis`, `Ingredient`, `Macros`, `ExerciseAnalysis`), plus domain models `FoodDraft`/
  `IngredientDraft` (editable meal; macros stored as per-100g rates for live rescaling) and `ModelOption`.
- `data/remote/`
  - `AiApi.kt` — Retrofit: `chatCompletion` (@Url + nullable Authorization), `listModels`
    (OpenRouter), `listGeminiModels` (@Url with `?key=`).
  - `NetworkModule.kt` — Moshi (codegen + reflective fallback), OkHttp, Retrofit (placeholder base URL; calls use @Url).
  - `RemoteAiDataSource.kt` — prompt assembly, image attach, JSON parse, `fetchFreeVisionModels`
    (OpenRouter, free+vision), `fetchGeminiVisionModels` (Gemini free Flash, intelligence-sorted).
  - `dto/` — `ChatDtos.kt`, `ModelsDtos.kt` (OpenRouter `ModelDto` + Gemini `GeminiModelDto`).
- `data/repository/`
  - `FitnessRepository.kt` — single `analyze()` entry point; routes by response `status`
    (SUCCESS→FoodReady draft, EXERCISE_LOGGED→save, CLARIFICATION_REQUIRED→ask); offline
    `simulateAIService` fallback; presets CRUD; `getFoodTotalsToday` (single consolidated query).
  - `AnalysisOutcome.kt` — sealed: `FoodReady(draft)`, `ExerciseSaved`, `NeedsClarification`, `Error`.
- `data/settings/`
  - `AppSettings.kt` — `AiProvider { OPENROUTER, GEMINI, OLLAMA }`; Ollama Local/Cloud
    (`ollamaUseCloud` + `ollamaApiKey`); derives `model`/`chatUrl`/`authHeader`/`isConfigured`.
  - `SettingsRepository.kt` — DataStore-backed; first-run defaults seed from `BuildConfig`
    (which reads `local.properties`).
- `ui/viewmodel/MainViewModel.kt` — all screen state; `settings`, `isAiOnline`, dashboard,
  analytics, presets, analysis flow, provider-aware `loadFreeVisionModels(provider, apiKey, force)`.
- `ui/screens/` — `MainScreen` (scaffold, tabs, input sheet, dialogs), `DashboardScreen`,
  `AnalyticsScreen`, `ProfileScreen`, `SettingsScreen`, `FoodReviewScreen` (`FoodReviewDialog`),
  `PresetPickerSheet`.
- `ui/components/CustomCharts.kt` — `CalorieRing`, line/stacked-bar charts.
- `util/` — `DateUtils` (yyyy-MM-dd), `ImageUtils` (bitmap→scaled JPEG bytes).

## AI providers (all via OpenAI-compatible chat/completions)
- **OpenRouter**: `https://openrouter.ai/api/v1/...`; Bearer key; model dropdown = free + vision.
- **Gemini**: `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`; Bearer key;
  model list via `.../v1beta/models?key=...`; dropdown = vision-capable Gemini models (heuristic,
  since the list API exposes no modality flag). Model ids strip the `models/` prefix.
- **Ollama**: Local or Cloud (Settings toggle). Local = user-supplied base URL, no auth,
  `GET {url}/v1/models` for dropdowns, cleartext LAN HTTP (`usesCleartextTraffic=true`).
  Cloud = `https://ollama.com` + Bearer API key from ollama.com/settings/keys; same
  `/v1/models` + `/v1/chat/completions`. Vision dropdown uses a name heuristic (llava, etc.).
- Config is **runtime** via Settings screen (DataStore), not compile-time. `BuildConfig`
  (`OPENROUTER_API_KEY`, `AI_MODEL` from `local.properties`) only seeds first-run defaults.
- Multiple API keys per provider (Settings chips). **Auto failover** (default on): same model →
  next key → other models on the **same** preferred platform only. Never switches platforms;
  if all keys/models fail, the error is surfaced and the user must change platform in Settings.
  Auto off: selected model only (no model/platform change); still rotates API keys on failure,
  Auto off: selected model only (no model/platform change); still rotates API keys on failure,
  then surfaces the error. Rate-limited models are skipped until the **next UTC midnight**
  (persisted); then newer requests try the **preferred dropdown model** first again. Green
  “active” lines show the last successful model without changing the dropdown. **Show paid
  models** (off by default) lists paid OpenRouter/Gemini models too and disables Refresh
  reachability probes. Gemini uses free Flash intelligence ranking (3.5 Flash first; Pro above
  Flash when paid is on); OpenRouter/Ollama prefer Gemma by generation then size (Gemma 4 31b →
  26b → Gemma 3…). Pills note model switches within the platform.
- If no provider is configured at all, text logs use the offline simulator (photos require a key).

## Key flows
- **Food logging**: analyze → `FoodReady(FoodDraft)` → `FoodReviewDialog` (edit dish name,
  ingredient weights recalc macros live, add/remove ingredients) → Save to log, or bookmark icon
  → save as **preset**.
- **Presets**: `FoodPreset` entity; add via review-dialog bookmark; quick-log via input sheet
  "Add from presets" → `PresetPickerSheet` (tap to log today, delete icon to remove).

## Performance notes (already applied)
- Release build: R8 minify + `shrinkResources` + `proguard-rules.pro` keeps (Moshi/Retrofit/Room);
  ART startup profile auto-compiled.
- Image decode/scale/compress runs on `Dispatchers.Default` (off main thread).
- Today's food totals = **one** Room query (`FoodTotals`) instead of 4 separate SUM flows.
- Dashboard combined-log list memoized with `remember`; UI state models marked `@Immutable`.
- For accurate perf testing, run the **release** build (debug Compose is inherently slow).

## Conventions / gotchas
- Keep comments minimal and intent-focused; no narration comments.
- All serialized DTOs use Moshi codegen (`@JsonClass(generateAdapter = true)`); if adding
  reflective classes, add proguard keeps.
- Bumping the Room schema wipes local data (destructive migration) — acceptable in dev only.
- Don't reintroduce removed deps (navigation3 / adaptive) or the kotlin.android plugin.
- When committing: do **NOT** add yourself (or any AI / Claude / Cursor / Composer)
  as a `Co-authored-by` trailer, and do not add "Generated with …" footers to commit
  messages. You will NEVER mention yourself as coauthor in commits.

## Commit messages
Commit messages feed directly into the F-Droid changelog (auto-generated by the release
workflow from commits since the last `v*-fdroid` tag). Write them for end users.

### Format
```
<type>: <user-visible summary>
```

### Types (changelog-visible — these appear in the generated changelog)
- `feat:` — new feature or capability
- `fix:` — bug fix
- `perf:` — performance improvement
- `docs:` — documentation visible to users (README, in-app help)

### Types (filtered out — these are skipped by the changelog generator)
- `chore:` — maintenance, deps, config
- `ci:` — CI/CD workflow changes
- `build:` — build system changes
- `refactor:` — code restructuring with no user-visible change
- `style:` — formatting, lint
- `test:` — adding/updating tests

### Rules
- Subject line: imperative mood, lowercase after type, ≤72 chars.
- Write what changed for the user, not what you did in the code.
  - ✅ `feat: auto-rotate API keys on rate limit`
  - ❌ `feat: add RetryInterceptor to OkHttp client`
- One logical change per commit. If a commit does multiple user-visible things, split it.
- If a commit touches both user-visible and internal stuff, use the user-visible type
  and describe the user-facing change in the subject.

## F-Droid release process
- Triggered via **Actions → F-Droid Release → Run workflow** in the GitHub UI.
- No version input needed — it's fully automatic.
- `versionCode` is auto-incremented from the current value in `build.gradle.kts` (previous + 1).
- `versionName` is derived from `versionCode` using the same formula as the GitHub release:
  `3.<2 + code/100>.<code % 100>` (e.g. code 60→3.2.60, code 100→3.3.0).
- The workflow automatically:
  1. Reads the current `versionCode` from the `create("fdroid")` block and increments by 1.
  2. Computes `versionName` from the new code.
  3. Updates `versionCode` and `versionName` in `app/build.gradle.kts`.
  4. Commits the change to `main` with `[skip ci]` (won't trigger other workflows).
  5. Creates and pushes tag `v<version>-fdroid` (e.g. `v3.2.60-fdroid`).
  6. Builds, signs, and publishes a prerelease GitHub Release with the APK.
- **Critical constraints:**
  - `versionName` must exactly match the tag without `v` prefix and `-fdroid` suffix.
    The workflow ensures this.
  - `versionCode` must be strictly greater than the previous release. Auto-increment
    guarantees this.
  - The F-Droid `Binaries` URL pattern is
    `https://github.com/anantdark/FitBuddy/releases/download/v%v-fdroid/FitBuddy-%v.apk`
    where `%v` = `versionName`. Tag, versionName, and APK filename must all agree.
- After the release, update the fdroiddata metadata on GitLab:
  - Repo: `/private/tmp/fdroid-submit/fdroiddata` (fork of `fdroid/fdroiddata`).
  - Branch: `com.anant.fitbuddy`.
  - File: `metadata/com.anant.fitbuddy.yml`.
  - Update `Builds[0].versionName`, `Builds[0].versionCode`, `Builds[0].commit` (set to
    the tag name, e.g. `v3.2.5-fdroid`), plus `CurrentVersion` and `CurrentVersionCode`.
  - Commit and push: `git push origin com.anant.fitbuddy`.
  - The existing MR at https://gitlab.com/fdroid/fdroiddata/-/merge_requests/43406
    picks up the push automatically and re-runs the pipeline.

## Fastlane metadata (F-Droid store listing)
F-Droid reads store metadata from `fastlane/metadata/android/en-US/` at the tagged commit.
Agents **must** keep this up to date before tagging a release.

### Structure
```
fastlane/metadata/android/en-US/
├── title.txt                  # App name ("FitBuddy")
├── short_description.txt      # ≤80 chars, one-liner for search results
├── full_description.txt       # Full store listing (features, privacy, providers)
├── changelogs/<versionCode>.txt  # Per-release changelog (≤500 chars recommended)
└── images/
    ├── icon.png
    └── phoneScreenshots/      # Numbered 1.png … N.png
```

### Rules for agents
- **Every feature commit / PR**: if the change is user-visible, update `full_description.txt`
  and/or `short_description.txt` to reflect the new capability. Keep descriptions accurate
  and current — don't leave stale feature lists.
- **Every F-Droid release**: create `changelogs/<new_versionCode>.txt` summarising what changed
  since the last release. Use bullet points, ≤500 chars. The versionCode must match the
  `versionCode` that will be written by the workflow (current value + 1).
- **Screenshots**: update `images/phoneScreenshots/` when UI changes significantly. Number
  sequentially (1.png, 2.png, …). Don't remove screenshots without replacing them.
- **title.txt**: only change if the app is renamed.
- Metadata must be committed **before** the release tag is created, since F-Droid checks out
  the repo at the exact tag commit.

## GitHub release process
- Every push to `main` auto-triggers the **Release** workflow (`.github/workflows/release.yml`).
- Version: `3.<2 + run_number/100>.<run_number % 100>`, `versionCode` = raw `run_number`.
  Minor auto-increments every 100 builds (run 99→3.2.99, run 100→3.3.0, run 200→3.4.0).
- Merged PRs create a push to `main`, so merging = releasing.
