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
- Commands (run from repo root):
  - Compile check: `./gradlew :app:compileDebugKotlin`
  - Optimized build: `./gradlew :app:assembleRelease` (R8 + resource shrink enabled)
  - Install optimized build over adb (personal profile only — never work profile / user 10):
    `./gradlew :app:assembleRelease && adb install -r --user 0 app/build/outputs/apk/release/FitBuddy-*.apk`
    (release is signed with the debug key for dev convenience — replace before publishing).
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
  then surfaces the error. Rate-limited models are skipped until the **next UTC midnight**
  (persisted); then newer requests try the highest models again. Gemini uses free Flash
  intelligence ranking; OpenRouter/Ollama prefer Gemma. Pills note model switches within the
  platform.
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
