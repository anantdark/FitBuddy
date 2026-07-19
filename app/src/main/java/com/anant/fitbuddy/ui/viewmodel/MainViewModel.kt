package com.anant.fitbuddy.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.ExerciseDailySummary
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.FoodDailySummary
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.data.database.MealPreset
import com.anant.fitbuddy.data.database.SavedFood
import com.anant.fitbuddy.data.database.UserProfile
import com.anant.fitbuddy.data.model.CommonExercise
import com.anant.fitbuddy.data.model.ExerciseDraft
import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.data.model.buildExercisePickerList
import com.anant.fitbuddy.data.model.FoodDraft
import com.anant.fitbuddy.data.model.FoodEntryDraft
import com.anant.fitbuddy.data.model.MealDraft
import com.anant.fitbuddy.data.model.toSingleFoodMeal
import com.anant.fitbuddy.data.model.IngredientDraft
import com.anant.fitbuddy.data.model.ModelOption
import com.anant.fitbuddy.data.model.ProgressChatTurn
import com.anant.fitbuddy.data.model.ProgressInsightResponse
import com.anant.fitbuddy.data.model.ScannedProduct
import com.anant.fitbuddy.data.model.TargetPlanResponse
import com.anant.fitbuddy.data.model.WorkoutDraft
import com.anant.fitbuddy.crash.CrashReporter
import com.anant.fitbuddy.crash.HeartbeatInfo
import com.anant.fitbuddy.data.remote.UpdateChecker
import com.anant.fitbuddy.data.remote.UpdateCheckResult
import com.anant.fitbuddy.data.repository.AnalysisOutcome
import com.anant.fitbuddy.data.repository.FitnessRepository
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.data.settings.SettingsRepository
import com.anant.fitbuddy.util.DateUtils
import com.anant.fitbuddy.util.ProgressMetricsCompressor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

/** Snapshot of everything the Dashboard needs, with derived net-balance/macros logic. */
@Immutable
data class DashboardUiState(
    val profile: UserProfile? = null,
    val consumedCalories: Int = 0,
    val burnedCalories: Int = 0,
    val consumedProtein: Int = 0,
    val consumedCarbs: Int = 0,
    val consumedFats: Int = 0
) {
    val targetCalories: Int get() = profile?.dailyTargetCalories ?: DEFAULT_TARGET_CALORIES
    val targetProtein: Int get() = profile?.targetProteinG ?: DEFAULT_TARGET_PROTEIN
    val targetCarbs: Int get() = profile?.targetCarbsG ?: DEFAULT_TARGET_CARBS
    val targetFats: Int get() = profile?.targetFatsG ?: DEFAULT_TARGET_FATS

    /** Net balance (Step 3): today's consumed calories minus today's burned calories. */
    val netCalories: Int get() = consumedCalories - burnedCalories
    val remainingCalories: Int get() = targetCalories - netCalories

    val remainingProtein: Int get() = (targetProtein - consumedProtein).coerceAtLeast(0)
    val remainingCarbs: Int get() = (targetCarbs - consumedCarbs).coerceAtLeast(0)
    val remainingFats: Int get() = (targetFats - consumedFats).coerceAtLeast(0)

    /** 0f..1f fill of the calorie ring (net progress toward the daily target). */
    val calorieProgress: Float
        get() = if (targetCalories <= 0) 0f else (netCalories.toFloat() / targetCalories).coerceIn(0f, 1f)

    val isOverTarget: Boolean get() = netCalories > targetCalories

    companion object {
        const val DEFAULT_TARGET_CALORIES = 2000
        const val DEFAULT_TARGET_PROTEIN = 120
        const val DEFAULT_TARGET_CARBS = 250
        const val DEFAULT_TARGET_FATS = 60
    }
}

/** State of the free/vision model catalog used by the Settings dropdown. */
@Immutable
data class ModelsUiState(
    val isLoading: Boolean = false,
    val options: List<ModelOption> = emptyList(),
    val error: String? = null,
    val loaded: Boolean = false
)

/** Transient state for the AI analysis flow (loading, clarification prompt, review draft, snackbar). */
@Immutable
data class AnalysisUiState(
    val isLoading: Boolean = false,
    val clarificationMessage: String? = null,
    val foodDraft: FoodDraft? = null,
    val mealDraft: MealDraft? = null,
    val isReanalyzing: Boolean = false,
    val errorDialogTitle: String? = null,
    val errorDialogMessage: String? = null,
    val userMessage: String? = null,
    // Shown inside the full-screen review dialog (its own snackbar), since the app-level snackbar
    // is hidden behind it.
    val reviewMessage: String? = null,
    // Non-null when the review dialog is editing an existing log row (so Save updates in place).
    val editingFoodLogId: Int? = null,
    /** Developer: last raw AI JSON when "Show raw AI JSON" is on. */
    val rawAiJson: String? = null
)

/** State of the AI target-recommendation flow shown in Profile. */
@Immutable
data class TargetPlanUiState(
    val isLoading: Boolean = false,
    val plan: TargetPlanResponse? = null,
    val error: String? = null
)

/** One message in the progress-coach follow-up chat. */
@Immutable
data class ProgressChatMessage(
    val role: ProgressChatRole,
    val text: String
)

enum class ProgressChatRole { USER, ASSISTANT }

/** State of the AI progress-insight card shown at the bottom of Progress. */
@Immutable
data class ProgressInsightUiState(
    val isLoading: Boolean = false,
    val isChatLoading: Boolean = false,
    val summary: String? = null,
    val recommendations: List<String> = emptyList(),
    val bodyScore: Int? = null,
    /** Full progress metrics JSON for follow-up chat (comprehensive data). */
    val metricsContextFull: String? = null,
    val chatMessages: List<ProgressChatMessage> = emptyList(),
    val error: String? = null
)

/** State of saving a workout session + estimating its calories burned. */
@Immutable
data class WorkoutLogUiState(
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)

/** A previously logged workout loaded for viewing/editing (see [MainViewModel.openWorkoutDetails]). */
@Immutable
data class WorkoutEditUiState(
    val exerciseLogId: Int,
    val sessionId: Int,
    val draft: WorkoutDraft,
    /** When true, Save inserts a new today entry instead of updating the source row. */
    val isClone: Boolean = false
)

/** Per-day food/exercise snapshot for the swipeable week dashboard. */
@Immutable
data class DayLogSnapshot(
    val foodLogs: List<FoodLog> = emptyList(),
    val exerciseLogs: List<ExerciseLog> = emptyList(),
    val consumedCalories: Int = 0,
    val burnedCalories: Int = 0,
    val consumedProtein: Int = 0,
    val consumedCarbs: Int = 0,
    val consumedFats: Int = 0
)

/** State of the manual "Check for Updates" flow in Settings. */
@Immutable
data class UpdateUiState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    /** 0f–1f when known; null while checking size / indeterminate download. */
    val downloadProgress: Float? = null,
    val updateInfo: UpdateCheckResult.Available? = null,
    val statusMessage: String? = null,
    val statusIsError: Boolean = false
)

class MainViewModel(
    private val repository: FitnessRepository,
    private val settingsRepository: SettingsRepository,
    private val updateChecker: UpdateChecker
) : ViewModel() {

    // --- Selected calendar day (rolling week dashboard) ------------------------------------

    private val _realToday = MutableStateFlow(DateUtils.today())
    val realToday: StateFlow<String> = _realToday.asStateFlow()

    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    /** Last day of the week strip shown in Week history (defaults to [realToday]). */
    private val _weekEndDate = MutableStateFlow(DateUtils.today())

    /** Calendar month (`yyyy-MM`) shown on the Progress Monthly charts. */
    private val _analyticsMonthYm = MutableStateFlow(DateUtils.yearMonth())
    val analyticsMonthYm: StateFlow<String> = _analyticsMonthYm.asStateFlow()

    val weekDates: StateFlow<List<String>> = _weekEndDate
        .map { DateUtils.rollingWeekDates(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DateUtils.rollingWeekDates()
        )

    /** Snap dashboard back to the real local today (cold start, resume, tab re-show). */
    fun refreshToToday() {
        val now = DateUtils.today()
        _realToday.value = now
        _weekEndDate.value = now
        _selectedDate.value = now
        _analyticsMonthYm.value = DateUtils.yearMonth(now)
    }

    fun selectDate(date: String) {
        if (date in weekDates.value) {
            _selectedDate.value = date
        }
    }

    /**
     * Shift the Week history strip by [deltaWeeks] (−1 = older, +1 = newer).
     * Never past [realToday]; keeps the same weekday index when possible.
     */
    fun shiftHistoryWeek(deltaWeeks: Int) {
        if (deltaWeeks == 0) return
        val today = _realToday.value
        val currentEnd = _weekEndDate.value
        val proposed = DateUtils.addDays(currentEnd, deltaWeeks * DateUtils.ROLLING_WEEK_DAYS)
        val newEnd = if (proposed > today) today else proposed
        if (newEnd == currentEnd) return

        val oldDates = DateUtils.rollingWeekDates(currentEnd)
        val newDates = DateUtils.rollingWeekDates(newEnd)
        val idx = oldDates.indexOf(_selectedDate.value).let { if (it < 0) oldDates.lastIndex else it }
            .coerceIn(0, newDates.lastIndex)
        _weekEndDate.value = newEnd
        _selectedDate.value = newDates[idx]
    }

    fun shiftAnalyticsMonth(deltaMonths: Int) {
        if (deltaMonths == 0) return
        val currentYm = DateUtils.yearMonth(_realToday.value)
        val proposed = DateUtils.addMonths(_analyticsMonthYm.value, deltaMonths)
        _analyticsMonthYm.value = if (proposed > currentYm) currentYm else proposed
    }

    /** Wall-clock time relocated onto the active (selected) calendar day. */
    fun activeDayTimestamp(): Long = DateUtils.timestampOnDate(_selectedDate.value)

    // --- Update check -------------------------------------------------------------------------

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    /** Manual or automatic check; [silent] skips status text for up-to-date / network errors. */
    fun checkForUpdates(currentVersionCode: Int, silent: Boolean = false) {
        if (_updateState.value.isChecking || _updateState.value.isDownloading) return
        viewModelScope.launch {
            _updateState.update {
                it.copy(
                    isChecking = true,
                    statusMessage = if (silent) it.statusMessage else null,
                    statusIsError = if (silent) it.statusIsError else false
                )
            }
            when (val result = updateChecker.checkForUpdate(currentVersionCode)) {
                is UpdateCheckResult.Available -> _updateState.update {
                    it.copy(isChecking = false, updateInfo = result, statusMessage = null, statusIsError = false)
                }
                UpdateCheckResult.UpToDate -> _updateState.update {
                    it.copy(
                        isChecking = false,
                        updateInfo = null,
                        statusMessage = if (silent) null else "You're on the latest version",
                        statusIsError = false
                    )
                }
                is UpdateCheckResult.Error -> _updateState.update {
                    it.copy(
                        isChecking = false,
                        updateInfo = null,
                        statusMessage = if (silent) null else result.message,
                        statusIsError = !silent
                    )
                }
            }
        }
    }

    fun dismissUpdatePrompt() {
        _updateState.update { it.copy(updateInfo = null, statusMessage = null, statusIsError = false) }
    }

    fun beginUpdateDownload() {
        _updateState.update {
            it.copy(
                isDownloading = true,
                downloadProgress = null,
                updateInfo = null,
                statusMessage = null,
                statusIsError = false
            )
        }
    }

    fun updateDownloadProgress(progress: Float) {
        _updateState.update {
            it.copy(
                downloadProgress = progress.takeIf { p -> p >= 0f }
            )
        }
    }

    fun finishUpdateDownload() {
        _updateState.update {
            it.copy(isDownloading = false, downloadProgress = null)
        }
    }

    fun failUpdateDownload(message: String) {
        _updateState.update {
            it.copy(
                isDownloading = false,
                downloadProgress = null,
                statusMessage = message,
                statusIsError = true
            )
        }
    }

    // --- Settings ---------------------------------------------------------------------------

    private val _hasSettingsSnapshot = MutableStateFlow(false)
    val hasSettingsSnapshot: StateFlow<Boolean> = _hasSettingsSnapshot.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .onEach { _hasSettingsSnapshot.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Reactive: true when the preferred provider is configured for live AI calls. */
    val isAiOnline: StateFlow<Boolean> = settingsRepository.settings
        .map { it.isConfigured }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.save(newSettings)
            _analysisState.update { it.copy(userMessage = "Settings saved") }
        }
    }

    fun unlockDeveloperMode() {
        viewModelScope.launch {
            val current = settings.value
            if (!current.developerModeUnlocked) {
                settingsRepository.save(current.copy(developerModeUnlocked = true))
            }
        }
    }

    fun clearModelCooldowns() {
        viewModelScope.launch {
            settingsRepository.clearModelCooldowns()
            _analysisState.update { it.copy(userMessage = "Model cooldowns cleared") }
        }
    }

    fun setCrashReportingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.save(current.copy(crashReportingEnabled = enabled))
            CrashReporter.setReportingEnabled(enabled)
            if (enabled) {
                val today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
                if (settingsRepository.lastHeartbeatUtcDay() != today) {
                    val info = HeartbeatInfo(aiProvider = settings.value.provider.name)
                    if (CrashReporter.sendDailyHeartbeat(info)) {
                        settingsRepository.markHeartbeatSent(today)
                    }
                }
            }
            _analysisState.update {
                it.copy(
                    userMessage = if (enabled) "Crash reports enabled" else "Crash reports disabled"
                )
            }
        }
    }

    fun markEasterEggDiscovered() {
        viewModelScope.launch {
            val current = settings.value
            if (!current.easterEggDiscovered) {
                settingsRepository.save(current.copy(easterEggDiscovered = true))
            }
        }
    }

    fun resetEasterEggData() {
        viewModelScope.launch {
            settingsRepository.save(settings.value.copy(easterEggDiscovered = false))
        }
    }

    // Catalog of vision-capable models (provider-specific) for the Settings dropdown.
    private val _models = MutableStateFlow(ModelsUiState())
    val models: StateFlow<ModelsUiState> = _models.asStateFlow()

    // Catalog of free/text models (provider-specific) for the text-query model dropdown.
    private val _textModels = MutableStateFlow(ModelsUiState())
    val textModels: StateFlow<ModelsUiState> = _textModels.asStateFlow()

    // Identifies the last successful load so switching provider/key triggers a refresh.
    private var lastModelsCacheKey: String? = null
    private var lastTextModelsCacheKey: String? = null

    /**
     * Loads the vision-capable model list for [provider] using [apiKey] (and [baseUrl] for Ollama).
     * Results are cached per provider+key+url; a change invalidates the cache. No-op re-entry
     * while already loading.
     */
    fun loadFreeVisionModels(
        provider: AiProvider,
        apiKey: String,
        force: Boolean = false,
        baseUrl: String = ""
    ) {
        val cacheKey = "${provider.name}:$apiKey:$baseUrl"
        if (_models.value.isLoading) return
        if (!force && _models.value.loaded && lastModelsCacheKey == cacheKey) return
        _models.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                when (provider) {
                    AiProvider.OPENROUTER -> repository.fetchFreeVisionModels(apiKey)
                    AiProvider.GEMINI -> repository.fetchGeminiVisionModels(apiKey)
                    AiProvider.OLLAMA -> {
                        val url = baseUrl.trim().trimEnd('/')
                        when {
                            url.isBlank() -> emptyList()
                            url == AppSettings.OLLAMA_CLOUD_BASE_URL && apiKey.isBlank() -> emptyList()
                            else -> repository.fetchOllamaVisionModels(url, apiKey)
                        }
                    }
                }
            }
                .onSuccess { list ->
                    lastModelsCacheKey = cacheKey
                    _models.update {
                        it.copy(isLoading = false, options = list, loaded = true, error = null)
                    }
                }
                .onFailure { e ->
                    _models.update {
                        it.copy(isLoading = false, error = e.message ?: "Could not load models")
                    }
                }
        }
    }

    /**
     * Loads the free (text) model list for [provider] used by the text-query model dropdown.
     * Cached per provider+key+url like [loadFreeVisionModels].
     */
    fun loadFreeTextModels(
        provider: AiProvider,
        apiKey: String,
        force: Boolean = false,
        baseUrl: String = ""
    ) {
        val cacheKey = "${provider.name}:$apiKey:$baseUrl"
        if (_textModels.value.isLoading) return
        if (!force && _textModels.value.loaded && lastTextModelsCacheKey == cacheKey) return
        _textModels.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                when (provider) {
                    AiProvider.OPENROUTER -> repository.fetchFreeModels(apiKey)
                    AiProvider.GEMINI -> repository.fetchGeminiTextModels(apiKey)
                    AiProvider.OLLAMA -> {
                        val url = baseUrl.trim().trimEnd('/')
                        when {
                            url.isBlank() -> emptyList()
                            url == AppSettings.OLLAMA_CLOUD_BASE_URL && apiKey.isBlank() -> emptyList()
                            else -> repository.fetchOllamaTextModels(url, apiKey)
                        }
                    }
                }
            }
                .onSuccess { list ->
                    lastTextModelsCacheKey = cacheKey
                    _textModels.update {
                        it.copy(isLoading = false, options = list, loaded = true, error = null)
                    }
                }
                .onFailure { e ->
                    _textModels.update {
                        it.copy(isLoading = false, error = e.message ?: "Could not load models")
                    }
                }
        }
    }

    // --- Dashboard --------------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    val weekSnapshots: StateFlow<Map<String, DayLogSnapshot>> =
        combine(weekDates, _realToday) { dates, today ->
            // Always keep today loaded so the home dashboard stays correct while browsing
            // older weeks in the Week history overlay.
            if (today.isEmpty() || today in dates) dates else dates + today
        }
            .flatMapLatest { dates ->
                if (dates.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    combine(dates.map { date -> daySnapshotFlow(date) }) { pairs ->
                        pairs.associate { it }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private fun daySnapshotFlow(date: String) =
        combine(
            repository.getFoodLogsForDate(date),
            repository.getExerciseLogsForDate(date),
            repository.getFoodTotalsToday(date),
            repository.getExerciseBurnToday(date)
        ) { food, exercise, totals, burned ->
            date to DayLogSnapshot(
                foodLogs = food,
                exerciseLogs = exercise,
                consumedCalories = totals.totalCalories,
                burnedCalories = burned ?: 0,
                consumedProtein = totals.totalProtein,
                consumedCarbs = totals.totalCarbs,
                consumedFats = totals.totalFats
            )
        }

    val dashboardState: StateFlow<DashboardUiState> =
        combine(repository.activeProfile, _selectedDate, weekSnapshots) { profile, date, snaps ->
            val snap = snaps[date] ?: DayLogSnapshot()
            DashboardUiState(
                profile = profile,
                consumedCalories = snap.consumedCalories,
                burnedCalories = snap.burnedCalories,
                consumedProtein = snap.consumedProtein,
                consumedCarbs = snap.consumedCarbs,
                consumedFats = snap.consumedFats
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** null while the profile hasn't loaded yet; true when first-time setup is required. */
    val needsOnboarding: StateFlow<Boolean?> =
        repository.activeProfile
            .map { profile -> profile == null || !profile.hasBasicsConfigured() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _onboardingSaving = MutableStateFlow(false)
    val onboardingSaving: StateFlow<Boolean> = _onboardingSaving.asStateFlow()

    private val _onboardingValidating = MutableStateFlow(false)
    val onboardingValidating: StateFlow<Boolean> = _onboardingValidating.asStateFlow()

    /** When true, dashboard launch should run a one-shot AI calorie/macro target design. */
    private var pendingInitialTargetDesign = false

    /** Logs for the currently selected dashboard day. */
    val foodLogsForSelectedDay: StateFlow<List<FoodLog>> =
        combine(_selectedDate, weekSnapshots) { date, snaps ->
            snaps[date]?.foodLogs.orEmpty()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exerciseLogsForSelectedDay: StateFlow<List<ExerciseLog>> =
        combine(_selectedDate, weekSnapshots) { date, snaps ->
            snaps[date]?.exerciseLogs.orEmpty()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** @deprecated Prefer [foodLogsForSelectedDay]; kept for any remaining call sites. */
    val foodLogsToday: StateFlow<List<FoodLog>> = foodLogsForSelectedDay

    /** @deprecated Prefer [exerciseLogsForSelectedDay]; kept for any remaining call sites. */
    val exerciseLogsToday: StateFlow<List<ExerciseLog>> = exerciseLogsForSelectedDay

    // --- Saved foods & meal presets ---------------------------------------------------------

    val savedFoods: StateFlow<List<SavedFood>> =
        repository.savedFoods
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mealPresets: StateFlow<List<MealPreset>> =
        repository.mealPresets
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun logMealPreset(preset: MealPreset) {
        viewModelScope.launch {
            repository.logMealPreset(preset, activeDayTimestamp())
            _analysisState.update {
                it.copy(userMessage = "Logged ${preset.name} · ${preset.calories} kcal")
            }
        }
    }

    fun deleteMealPreset(preset: MealPreset) {
        viewModelScope.launch {
            repository.deleteMealPreset(preset)
            _analysisState.update { it.copy(userMessage = "Deleted meal preset ${preset.name}") }
        }
    }

    fun deleteSavedFood(food: SavedFood) {
        viewModelScope.launch {
            repository.deleteSavedFood(food)
            _analysisState.update { it.copy(userMessage = "Deleted saved food ${food.name}") }
        }
    }

    private val _barcodeLookupLoading = MutableStateFlow(false)
    val barcodeLookupLoading: StateFlow<Boolean> = _barcodeLookupLoading.asStateFlow()

    fun lookupBarcode(barcode: String, onSuccess: (ScannedProduct) -> Unit) {
        if (_barcodeLookupLoading.value) return
        _barcodeLookupLoading.value = true
        CrashReporter.breadcrumb("barcode", "lookup")
        viewModelScope.launch {
            runCatching { repository.lookupProductByBarcode(barcode) }
                .onSuccess(onSuccess)
                .onFailure { e ->
                    val code = barcode.filter { it.isDigit() }.ifBlank { barcode.trim() }
                    _analysisState.update {
                        it.copy(
                            errorDialogTitle = "Product not found",
                            errorDialogMessage = e.message
                                ?: "No Open Food Facts match for $code"
                        )
                    }
                }
            _barcodeLookupLoading.value = false
        }
    }

    fun saveScannedProductAsSavedFood(product: ScannedProduct) {
        viewModelScope.launch {
            runCatching { repository.saveScannedProductAsSavedFood(product) }
                .onSuccess {
                    _analysisState.update {
                        it.copy(userMessage = "Saved \"${product.name}\" to your food library")
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = e.message ?: "Couldn't save food")
                    }
                }
        }
    }

    fun openMealReview(draft: MealDraft) {
        _analysisState.update { it.copy(mealDraft = draft, foodDraft = null) }
    }

    fun dismissMealDraft() {
        _analysisState.update { it.copy(mealDraft = null, editingFoodLogId = null) }
    }

    fun dismissFoodDraft() {
        _analysisState.update { it.copy(foodDraft = null) }
    }

    // --- Exercise presets (workout picker) --------------------------------------------------

    val exercisePickerExercises: StateFlow<List<CommonExercise>> =
        repository.exercisePresets
            .map { presets ->
                buildExercisePickerList(presets.map { it.name to it.equipment })
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildExercisePickerList(emptyList()))

    private val _customExerciseClassifying = MutableStateFlow(false)
    val customExerciseClassifying: StateFlow<Boolean> = _customExerciseClassifying.asStateFlow()

    private val _workoutInferring = MutableStateFlow(false)
    val workoutInferring: StateFlow<Boolean> = _workoutInferring.asStateFlow()

    /** Normalises a custom exercise via AI/offline rules and saves it for future picker use. */
    fun classifyCustomExercise(rawName: String, onResolved: (name: String, equipment: String) -> Unit) {
        if (_customExerciseClassifying.value || _workoutInferring.value) return
        _customExerciseClassifying.value = true
        viewModelScope.launch {
            runCatching { repository.classifyCustomExercise(rawName) }
                .onSuccess { exercise -> onResolved(exercise.name, exercise.equipment) }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = e.message ?: "Couldn't recognise that exercise")
                    }
                }
            _customExerciseClassifying.value = false
        }
    }

    /** Parses natural-language text into fully-filled exercise rows for the session builder. */
    fun inferExercisesFromDescription(
        description: String,
        onResolved: (List<ExerciseDraft>) -> Unit
    ) {
        if (_workoutInferring.value || _customExerciseClassifying.value) return
        _workoutInferring.value = true
        viewModelScope.launch {
            runCatching { repository.parseWorkoutDescription(description) }
                .onSuccess { drafts ->
                    onResolved(drafts)
                    _analysisState.update {
                        it.copy(userMessage = "Added ${drafts.size} exercise(s) from your description")
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(
                            errorDialogTitle = "Couldn't infer exercises",
                            errorDialogMessage = e.message ?: "AI request failed"
                        )
                    }
                }
            _workoutInferring.value = false
        }
    }

    // --- Analytics --------------------------------------------------------------------------

    val weeklyFood: StateFlow<List<FoodDailySummary>> =
        repository.getWeeklyFoodSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyFood: StateFlow<List<FoodDailySummary>> =
        _analyticsMonthYm
            .flatMapLatest { ym ->
                val (start, end) = DateUtils.monthBounds(ym)
                repository.getFoodSummariesBetween(start, end)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyExercise: StateFlow<List<ExerciseDailySummary>> =
        repository.getWeeklyExerciseSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyExercise: StateFlow<List<ExerciseDailySummary>> =
        _analyticsMonthYm
            .flatMapLatest { ym ->
                val (start, end) = DateUtils.monthBounds(ym)
                repository.getExerciseSummariesBetween(start, end)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Body measurements ------------------------------------------------------------------

    val bodyMeasurements: StateFlow<List<BodyMeasurement>> =
        repository.bodyMeasurements
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestMeasurement: StateFlow<BodyMeasurement?> =
        repository.latestMeasurement
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Saves a new body reading (timestamped now) and mirrors weight onto the profile. */
    fun addMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            val ts = System.currentTimeMillis()
            repository.addMeasurement(
                measurement.copy(timestamp = ts, dateString = DateUtils.format(ts))
            )
            _analysisState.update { it.copy(userMessage = "Reading saved") }
        }
    }

    fun deleteMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.deleteMeasurement(measurement)
            _analysisState.update { it.copy(userMessage = "Reading removed") }
        }
    }

    // --- Workout sessions -------------------------------------------------------------------

    private val _workoutLog = MutableStateFlow(WorkoutLogUiState())
    val workoutLogState: StateFlow<WorkoutLogUiState> = _workoutLog.asStateFlow()

    private val _editingWorkout = MutableStateFlow<WorkoutEditUiState?>(null)
    /** Non-null while the "view / edit exercises" sheet for a logged workout is open. */
    val editingWorkout: StateFlow<WorkoutEditUiState?> = _editingWorkout.asStateFlow()

    /** Resolves the user's current weight, treating an unset/zero profile weight as "not set". */
    private fun currentWeightKg(): Double =
        dashboardState.value.profile?.weightKg?.takeIf { it > 0 }
            ?: latestMeasurement.value?.weightKg
            ?: 0.0

    /** Saves a workout session and estimates calories burned (AI, or an offline fallback). */
    fun logWorkoutSession(draft: WorkoutDraft) {
        if (draft.exercises.isEmpty()) return
        _workoutLog.update { WorkoutLogUiState(isSaving = true) }
        viewModelScope.launch {
            val weightKg = currentWeightKg()
            runCatching {
                repository.logWorkoutSession(
                    draft,
                    weightKg,
                    buildWorkoutContext(draft, weightKg),
                    timestamp = activeDayTimestamp()
                )
            }
                .onSuccess { result ->
                    _workoutLog.update { WorkoutLogUiState(savedSuccessfully = true) }
                    _analysisState.update {
                        it.copy(userMessage = "Logged ${draft.name} · -${result.caloriesBurned} kcal")
                    }
                }
                .onFailure { e ->
                    _workoutLog.update {
                        it.copy(isSaving = false, error = e.message ?: "Couldn't log this workout")
                    }
                }
        }
    }

    /** Call after the dialog has dismissed itself following [WorkoutLogUiState.savedSuccessfully]. */
    fun consumeWorkoutSaved() = _workoutLog.update { WorkoutLogUiState() }

    /**
     * Loads a logged exercise entry's underlying workout (sets/reps/weight per exercise), if it
     * was logged via the structured workout dialog, so it can be viewed/edited. Entries logged
     * via photo/text/preset have no such breakdown; the user is told so instead.
     */
    fun openWorkoutDetails(log: ExerciseLog) {
        viewModelScope.launch {
            val details = repository.getWorkoutDetails(log.id)
            if (details == null) {
                _analysisState.update {
                    it.copy(userMessage = "No exercise breakdown saved for this entry")
                }
                return@launch
            }
            _editingWorkout.value = WorkoutEditUiState(
                exerciseLogId = log.id,
                sessionId = details.session.id,
                draft = WorkoutDraft(
                    name = details.session.name,
                    durationMinutes = details.session.durationMinutes,
                    exercises = details.exercises.map {
                        ExerciseDraft(
                            name = it.name,
                            sets = it.sets,
                            reps = it.reps,
                            weightKg = it.weightKg,
                            equipment = it.equipment,
                            durationMinutes = it.durationMinutes,
                            distanceKm = it.distanceKm
                        )
                    }
                )
            )
        }
    }

    fun dismissWorkoutDetails() {
        _editingWorkout.value = null
        _workoutLog.update { WorkoutLogUiState() }
    }

    /**
     * Saves the workout currently open in [editingWorkout]. Clone mode inserts a new today
     * session; otherwise updates the existing session in place.
     */
    fun saveEditingWorkout(draft: WorkoutDraft) {
        val editing = _editingWorkout.value ?: return
        if (editing.isClone) {
            logClonedWorkout(draft)
        } else {
            updateWorkoutSession(draft)
        }
    }

    /** Inserts a cloned workout onto real today (ignores the source session ids). */
    private fun logClonedWorkout(draft: WorkoutDraft) {
        if (draft.exercises.isEmpty()) return
        _workoutLog.update { WorkoutLogUiState(isSaving = true) }
        viewModelScope.launch {
            val weightKg = currentWeightKg()
            val todayTs = DateUtils.timestampOnDate(_realToday.value)
            runCatching {
                repository.logWorkoutSession(
                    draft,
                    weightKg,
                    buildWorkoutContext(draft, weightKg),
                    timestamp = todayTs
                )
            }
                .onSuccess { result ->
                    _workoutLog.update { WorkoutLogUiState(savedSuccessfully = true) }
                    _editingWorkout.value = null
                    _analysisState.update {
                        it.copy(userMessage = "Cloned ${draft.name} · -${result.caloriesBurned} kcal")
                    }
                }
                .onFailure { e ->
                    _workoutLog.update {
                        it.copy(isSaving = false, error = e.message ?: "Couldn't clone this workout")
                    }
                }
        }
    }

    /** Saves edits to the workout currently open in [editingWorkout] and re-estimates calories. */
    fun updateWorkoutSession(draft: WorkoutDraft) {
        val editing = _editingWorkout.value ?: return
        if (draft.exercises.isEmpty()) return
        _workoutLog.update { WorkoutLogUiState(isSaving = true) }
        viewModelScope.launch {
            val weightKg = currentWeightKg()
            runCatching {
                repository.updateWorkoutSession(
                    sessionId = editing.sessionId,
                    exerciseLogId = editing.exerciseLogId,
                    draft = draft,
                    weightKg = weightKg,
                    contextJson = buildWorkoutContext(draft, weightKg)
                )
            }
                .onSuccess { result ->
                    _workoutLog.update { WorkoutLogUiState(savedSuccessfully = true) }
                    _editingWorkout.value = null
                    _analysisState.update {
                        it.copy(userMessage = "Updated ${draft.name} · -${result.caloriesBurned} kcal")
                    }
                }
                .onFailure { e ->
                    _workoutLog.update {
                        it.copy(isSaving = false, error = e.message ?: "Couldn't update this workout")
                    }
                }
        }
    }

    private fun buildWorkoutContext(draft: WorkoutDraft, weightKg: Double): String {
        val profile = dashboardState.value.profile
        val exercisesJson = JSONArray().apply {
            draft.exercises.forEach { ex ->
                put(JSONObject().apply {
                    put("name", ex.name)
                    put("equipment", ex.equipment)
                    put("sets", ex.sets)
                    put("reps", ex.reps)
                    put("weight_kg", ex.weightKg ?: JSONObject.NULL)
                    put("duration_minutes", ex.durationMinutes ?: JSONObject.NULL)
                    put("distance_km", ex.distanceKm ?: JSONObject.NULL)
                })
            }
        }
        return JSONObject().apply {
            put("age", profile?.age ?: JSONObject.NULL)
            put("sex", profile?.sex ?: JSONObject.NULL)
            put("weight_kg", if (weightKg > 0) weightKg else JSONObject.NULL)
            put("activity_level", profile?.activityLevel ?: JSONObject.NULL)
            put("session_name", draft.name)
            put("user_provided_duration_minutes", WorkoutDraft.estimateDurationMinutes(draft.exercises))
            put("exercises", exercisesJson)
        }.toString()
    }

    // --- AI target design -------------------------------------------------------------------

    private val _targetPlan = MutableStateFlow(TargetPlanUiState())
    val targetPlanState: StateFlow<TargetPlanUiState> = _targetPlan.asStateFlow()

    /**
     * Requests an AI-recommended goal + calorie/macro targets. Takes the Profile screen's
     * currently-displayed basics directly (rather than the persisted [dashboardState] profile)
     * so a not-yet-saved edit is still used to personalise the recommendation.
     */
    fun requestTargetPlan(
        age: Int,
        heightCm: Double,
        weightKg: Double,
        sex: String?,
        activityLevel: String,
        goal: String
    ) {
        if (_targetPlan.value.isLoading) return
        _targetPlan.update { it.copy(isLoading = true, error = null, plan = null) }
        viewModelScope.launch {
            val context = buildTargetContext(age, heightCm, weightKg, sex, activityLevel, goal)
            runCatching { repository.designTargets(context) }
                .onSuccess { plan -> _targetPlan.update { it.copy(isLoading = false, plan = plan) } }
                .onFailure { e ->
                    _targetPlan.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't generate targets")
                    }
                }
        }
    }

    /**
     * Persists the AI-proposed goal + targets to the profile. [age]/[heightCm]/[weightKg]/[sex]/
     * [activityLevel] are always taken from the Profile screen's current fields (never defaulted
     * to zero/null) so applying a plan can never wipe out basics that haven't been saved yet.
     */
    fun applyTargetPlan(
        plan: TargetPlanResponse,
        age: Int,
        heightCm: Double,
        weightKg: Double,
        sex: String?,
        activityLevel: String
    ) {
        viewModelScope.launch {
            repository.saveProfile(
                UserProfile(
                    id = 1,
                    age = age,
                    weightKg = weightKg,
                    heightCm = heightCm,
                    dailyTargetCalories = plan.dailyTargetCalories,
                    targetProteinG = plan.targetProteinG,
                    targetCarbsG = plan.targetCarbsG,
                    targetFatsG = plan.targetFatsG,
                    lastUpdatedTimestamp = System.currentTimeMillis(),
                    sex = sex,
                    goal = plan.recommendedGoal,
                    activityLevel = activityLevel,
                    goalRationale = plan.rationale
                )
            )
            _targetPlan.update { TargetPlanUiState() }
            _analysisState.update { it.copy(userMessage = "AI targets applied") }
        }
    }

    fun dismissTargetPlan() = _targetPlan.update { TargetPlanUiState() }

    // --- AI progress insight ----------------------------------------------------------------

    private val _progressInsight = MutableStateFlow(ProgressInsightUiState())
    val progressInsightState: StateFlow<ProgressInsightUiState> = _progressInsight.asStateFlow()

    fun requestProgressInsight() {
        if (_progressInsight.value.isLoading) return
        _progressInsight.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val contextJson = buildProgressContext()
            val compressed = ProgressMetricsCompressor.compress(contextJson)
            runCatching { repository.summarizeProgress(compressed) }
                .onSuccess { r ->
                    val seedMessage = formatProgressInsightForChat(r)
                    _progressInsight.update {
                        ProgressInsightUiState(
                            summary = r.summary,
                            recommendations = r.recommendations,
                            bodyScore = r.bodyScore,
                            metricsContextFull = contextJson,
                            chatMessages = listOf(
                                ProgressChatMessage(ProgressChatRole.ASSISTANT, seedMessage)
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _progressInsight.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't generate insight")
                    }
                }
        }
    }

    fun sendProgressChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _progressInsight.value.isChatLoading) return
        val fullContext = _progressInsight.value.metricsContextFull ?: return

        val userMessage = ProgressChatMessage(ProgressChatRole.USER, trimmed)
        _progressInsight.update {
            it.copy(
                chatMessages = it.chatMessages + userMessage,
                isChatLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val history = _progressInsight.value.chatMessages.map { msg ->
                ProgressChatTurn(
                    role = if (msg.role == ProgressChatRole.USER) "user" else "assistant",
                    content = msg.text
                )
            }
            runCatching { repository.chatProgressInsight(fullContext, history) }
                .onSuccess { reply ->
                    _progressInsight.update {
                        it.copy(
                            isChatLoading = false,
                            chatMessages = it.chatMessages +
                                ProgressChatMessage(ProgressChatRole.ASSISTANT, reply)
                        )
                    }
                }
                .onFailure { e ->
                    _progressInsight.update {
                        it.copy(
                            isChatLoading = false,
                            error = e.message ?: "Couldn't get a reply"
                        )
                    }
                }
        }
    }

    /** Resets follow-up chat to the initial insight message only. */
    fun clearProgressChat() {
        _progressInsight.update { state ->
            val seed = state.chatMessages.firstOrNull()
            state.copy(
                chatMessages = if (seed != null) listOf(seed) else emptyList(),
                isChatLoading = false,
                error = null
            )
        }
    }

    private fun formatProgressInsightForChat(response: ProgressInsightResponse): String = buildString {
        append(response.summary)
        response.bodyScore?.let { append("\n\nBody score: $it/100") }
        if (response.recommendations.isNotEmpty()) {
            append("\n\nRecommendations:")
            response.recommendations.forEach { append("\n• $it") }
        }
    }

    // --- Backup (export / import) -----------------------------------------------------------

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            runCatching { repository.exportData(uri) }
                .onSuccess { count ->
                    _analysisState.update { it.copy(userMessage = "Exported $count records") }
                }
                .onFailure { e ->
                    _analysisState.update { it.copy(userMessage = "Export failed: ${e.message}") }
                }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            runCatching { repository.importData(uri) }
                .onSuccess { count ->
                    _analysisState.update { it.copy(userMessage = "Imported $count records") }
                }
                .onFailure { e ->
                    _analysisState.update { it.copy(userMessage = "Import failed: ${e.message}") }
                }
        }
    }

    // --- AI analysis ------------------------------------------------------------------------

    private val _analysisState = MutableStateFlow(AnalysisUiState())
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    // Retained so a clarification answer can be merged with the original prompt/photo.
    private var pendingText: String = ""
    private var pendingImage: ByteArray? = null

    fun analyzeText(text: String) = submit(text, null)

    fun analyzeImage(imageBytes: ByteArray, note: String = "") = submit(note, imageBytes)

    fun submitClarification(answer: String) {
        val merged = listOf(pendingText, answer)
            .filter { it.isNotBlank() }
            .joinToString(". ")
        submit(merged, pendingImage, resetPending = false)
    }

    private fun submit(text: String, image: ByteArray?, resetPending: Boolean = true) {
        if (resetPending) {
            pendingText = text
            pendingImage = image
        }
        CrashReporter.breadcrumb(
            "ai",
            if (image != null) "analyze_photo" else "analyze_text"
        )
        _analysisState.update { it.copy(isLoading = true, clarificationMessage = null) }
        viewModelScope.launch {
            val outcome = repository.analyze(
                text,
                image,
                buildUserStateContext(),
                customTimestamp = activeDayTimestamp()
            )
            handleOutcome(outcome)
        }
    }

    private fun handleOutcome(outcome: AnalysisOutcome) {
        val rawJson = maybeRawAiJson()
        when (outcome) {
            is AnalysisOutcome.FoodReady -> {
                clearPending()
                _analysisState.update {
                    it.copy(
                        isLoading = false,
                        clarificationMessage = null,
                        foodDraft = outcome.draft,
                        mealDraft = null,
                        userMessage = outcome.failoverNote,
                        rawAiJson = rawJson
                    )
                }
            }

            is AnalysisOutcome.ExerciseSaved -> {
                clearPending()
                _analysisState.update {
                    it.copy(
                        isLoading = false,
                        clarificationMessage = null,
                        userMessage = outcome.failoverNote
                            ?: "Logged ${outcome.activityName} · -${outcome.caloriesBurned} kcal",
                        rawAiJson = rawJson
                    )
                }
            }

            is AnalysisOutcome.NeedsClarification -> _analysisState.update {
                it.copy(
                    isLoading = false,
                    clarificationMessage = outcome.message,
                    userMessage = outcome.failoverNote,
                    rawAiJson = rawJson
                )
            }

            is AnalysisOutcome.NotIdentified -> {
                clearPending()
                _analysisState.update {
                    it.copy(
                        isLoading = false,
                        errorDialogTitle = "Item not identified",
                        errorDialogMessage = outcome.message,
                        userMessage = outcome.failoverNote,
                        rawAiJson = rawJson
                    )
                }
            }

            is AnalysisOutcome.Error -> {
                clearPending()
                _analysisState.update {
                    it.copy(
                        isLoading = false,
                        errorDialogTitle = "Couldn't reach AI",
                        errorDialogMessage = outcome.message,
                        rawAiJson = rawJson
                    )
                }
            }
        }
    }

    private fun maybeRawAiJson(): String? {
        val s = settings.value
        if (!s.developerModeUnlocked || !s.showRawAiJson) return null
        return repository.lastRawAiJson()?.takeIf { it.isNotBlank() }
    }

    fun dismissErrorDialog() {
        _analysisState.update { it.copy(errorDialogTitle = null, errorDialogMessage = null) }
    }

    fun dismissClarification() {
        clearPending()
        _analysisState.update { it.copy(clarificationMessage = null, isLoading = false) }
    }

    /** User confirmed a single food from AI review — log as a one-food meal. */
    fun confirmFood(draft: FoodDraft) {
        confirmMeal(draft.toSingleFoodMeal())
    }

    /** User confirmed a meal (one or more foods) from the review/builder flow. */
    fun confirmMeal(draft: MealDraft) {
        val editingId = _analysisState.value.editingFoodLogId
        viewModelScope.launch {
            repository.saveMealDraft(draft, editingId ?: 0)
            _analysisState.update {
                it.copy(
                    foodDraft = null,
                    mealDraft = null,
                    editingFoodLogId = null,
                    userMessage = if (editingId != null) {
                        "Updated ${draft.name} · ${draft.totalCalories} kcal"
                    } else {
                        "Logged ${draft.name} · ${draft.totalCalories} kcal"
                    }
                )
            }
        }
    }

    /**
     * Opens the meal builder/review flow for an already-logged meal, restoring each food and its
     * ingredients from the stored hierarchy.
     */
    fun editFoodLog(log: FoodLog) {
        viewModelScope.launch {
            val draft = repository.mealLogToMealDraft(log)
            _analysisState.update {
                it.copy(
                    mealDraft = draft,
                    foodDraft = null,
                    editingFoodLogId = log.id,
                    reviewMessage = null
                )
            }
        }
    }

    /** Removes a food entry from the log. */
    fun deleteFoodLog(log: FoodLog) {
        viewModelScope.launch {
            repository.deleteFood(log)
            _analysisState.update { it.copy(userMessage = "Removed ${log.dishName}") }
        }
    }

    /** Removes an exercise entry from the log. */
    fun deleteExerciseLog(log: ExerciseLog) {
        viewModelScope.launch {
            repository.deleteExercise(log)
            _analysisState.update { it.copy(userMessage = "Removed ${log.activityName}") }
        }
    }

    private val _cloneMealRequest = MutableStateFlow<MealDraft?>(null)
    /** One-shot: open meal review with a clone-to-today draft. */
    val cloneMealRequest: StateFlow<MealDraft?> = _cloneMealRequest.asStateFlow()

    fun consumeCloneMealRequest() {
        _cloneMealRequest.value = null
    }

    /**
     * Opens meal review prefilled from [log], stamped for real today. Save inserts a new row
     * ([editingFoodLogId] stays null).
     */
    fun cloneFoodLogToToday(log: FoodLog) {
        viewModelScope.launch {
            val draft = repository.mealLogToMealDraft(log).copy(
                timestamp = DateUtils.timestampOnDate(_realToday.value)
            )
            _analysisState.update {
                it.copy(
                    mealDraft = draft,
                    foodDraft = null,
                    editingFoodLogId = null,
                    reviewMessage = null
                )
            }
            _cloneMealRequest.value = draft
        }
    }

    /**
     * Opens the workout editor prefilled from [log] in clone mode. Save inserts a new today
     * session. Simple AI exercise logs (no set breakdown) get a cardio draft so the user can
     * still confirm or tweak before saving.
     */
    fun cloneWorkoutToToday(log: ExerciseLog) {
        viewModelScope.launch {
            val details = repository.getWorkoutDetails(log.id)
            val draft = if (details != null) {
                WorkoutDraft(
                    name = details.session.name,
                    durationMinutes = details.session.durationMinutes,
                    exercises = details.exercises.map {
                        ExerciseDraft(
                            name = it.name,
                            sets = it.sets,
                            reps = it.reps,
                            weightKg = it.weightKg,
                            equipment = it.equipment,
                            durationMinutes = it.durationMinutes,
                            distanceKm = it.distanceKm
                        )
                    }
                )
            } else {
                WorkoutDraft(
                    name = log.activityName,
                    durationMinutes = log.durationMinutes.coerceAtLeast(5),
                    exercises = listOf(
                        ExerciseDraft(
                            name = log.activityName,
                            sets = 1,
                            reps = 1,
                            equipment = Equipment.CARDIO,
                            durationMinutes = log.durationMinutes.coerceAtLeast(1)
                        )
                    )
                )
            }
            _editingWorkout.value = WorkoutEditUiState(
                exerciseLogId = 0,
                sessionId = 0,
                draft = draft,
                isClone = true
            )
        }
    }

    /**
     * Saves the (possibly edited) meal from the review sheet as a reusable preset. Confirmation is
     * shown by the review dialog's own snackbar (the app-level one is hidden behind that dialog).
     */
    fun saveDraftAsSavedFood(draft: FoodDraft) {
        viewModelScope.launch {
            repository.saveDraftAsSavedFood(draft)
        }
    }

    fun saveMealDraftAsPreset(draft: MealDraft) {
        viewModelScope.launch {
            repository.saveMealDraftAsPreset(draft)
        }
    }

    fun openFoodEditor(draft: FoodDraft) {
        _analysisState.update { it.copy(foodDraft = draft, reviewMessage = null) }
    }

    /** Updates a food row inside the in-progress meal review (after editing its ingredients). */
    fun updateMealDraftFood(index: Int, food: com.anant.fitbuddy.data.model.FoodEntryDraft) {
        _analysisState.update { state ->
            val meal = state.mealDraft ?: return@update state
            if (index !in meal.foods.indices) return@update state
            val updated = meal.foods.toMutableList().also { it[index] = food }
            state.copy(mealDraft = meal.copy(foods = updated))
        }
    }

    /**
     * User corrected the dish name/description in the review sheet (e.g. "whipped cream" →
     * "pineapple cake"). Re-run analysis on the corrected text so the AI recomputes the
     * ingredients + macros, and swap in the new draft. Non-food/failed results keep the current
     * draft open and just surface a message.
     */
    fun reanalyzeFood(correctedText: String) {
        val text = correctedText.trim()
        if (text.isBlank()) return
        _analysisState.update { it.copy(isReanalyzing = true, reviewMessage = null) }
        viewModelScope.launch {
            // forceEstimate: the user is renaming an item mid-review, so always recompute a
            // best-effort result instead of bouncing back a clarification question.
            val outcome = repository.analyze(text, null, buildUserStateContext(), forceEstimate = true)
            applyReanalyzeOutcome(outcome)
        }
    }

    /**
     * Re-runs analysis with a fixed portion template so the AI estimates a standard home serving
     * when the user knows the dish but not grams.
     */
    fun askForPortion(dishName: String) {
        val name = dishName.trim()
        if (name.isBlank()) return
        val prompt = "$name. Estimate a standard single home serving using typical North Indian " +
            "portion sizes (roti counts, katori volumes, rice bowls). Break into named " +
            "ingredients with weights in grams and consistent macros. Include cooking fat " +
            "(ghee/oil) as its own ingredient when the dish is tadka, fried, or buttery."
        _analysisState.update { it.copy(isReanalyzing = true, reviewMessage = null) }
        viewModelScope.launch {
            val outcome = repository.analyze(
                prompt,
                null,
                buildUserStateContext(),
                forceEstimate = true
            )
            applyReanalyzeOutcome(outcome)
        }
    }

    private fun applyReanalyzeOutcome(outcome: AnalysisOutcome) {
        val rawJson = maybeRawAiJson()
        _analysisState.update { state ->
            when (outcome) {
                is AnalysisOutcome.FoodReady ->
                    state.copy(
                        isReanalyzing = false,
                        foodDraft = outcome.draft,
                        reviewMessage = outcome.failoverNote,
                        rawAiJson = rawJson
                    )

                is AnalysisOutcome.NeedsClarification ->
                    state.copy(
                        isReanalyzing = false,
                        reviewMessage = outcome.message,
                        rawAiJson = rawJson
                    )

                is AnalysisOutcome.NotIdentified ->
                    state.copy(
                        isReanalyzing = false,
                        reviewMessage = outcome.message,
                        rawAiJson = rawJson
                    )

                is AnalysisOutcome.Error ->
                    state.copy(
                        isReanalyzing = false,
                        reviewMessage = outcome.message,
                        rawAiJson = rawJson
                    )

                is AnalysisOutcome.ExerciseSaved ->
                    state.copy(
                        isReanalyzing = false,
                        reviewMessage = "That looks like an activity, not a food item.",
                        rawAiJson = rawJson
                    )
            }
        }
    }

    /** Call after a snackbar/toast has shown [AnalysisUiState.userMessage]. */
    fun consumeUserMessage() = _analysisState.update { it.copy(userMessage = null) }

    /** Call after the review dialog's snackbar has shown [AnalysisUiState.reviewMessage]. */
    fun consumeReviewMessage() = _analysisState.update { it.copy(reviewMessage = null) }

    fun consumeRawAiJson() = _analysisState.update { it.copy(rawAiJson = null) }

    private fun clearPending() {
        pendingText = ""
        pendingImage = null
    }

    // --- Profile ----------------------------------------------------------------------------

    /**
     * Verifies onboarding AI credentials by listing models from the chosen provider.
     * Calls [onResult] with success/failure on the main thread when done.
     */
    fun validateOnboardingAi(settings: AppSettings, onResult: (Boolean, String?) -> Unit) {
        if (_onboardingValidating.value) return
        _onboardingValidating.value = true
        viewModelScope.launch {
            val result = runCatching { probeAiCredentials(settings) }
            _onboardingValidating.value = false
            result
                .onSuccess { onResult(true, null) }
                .onFailure { e ->
                    onResult(false, e.message ?: "Couldn't validate that API key")
                }
        }
    }

    /**
     * First-run setup: persists AI settings (required), saves profile with placeholder targets,
     * seeds an initial body reading, then flags a one-shot target design for dashboard launch.
     */
    fun completeOnboarding(
        age: Int,
        weightKg: Double,
        heightCm: Double,
        sex: String?,
        goal: String,
        activityLevel: String,
        aiSettings: AppSettings
    ) {
        if (_onboardingSaving.value) return
        _onboardingSaving.value = true
        viewModelScope.launch {
            runCatching {
                settingsRepository.save(aiSettings)
                repository.saveProfile(
                    UserProfile(
                        id = 1,
                        age = age,
                        weightKg = weightKg,
                        heightCm = heightCm,
                        dailyTargetCalories = DashboardUiState.DEFAULT_TARGET_CALORIES,
                        targetProteinG = DashboardUiState.DEFAULT_TARGET_PROTEIN,
                        targetCarbsG = DashboardUiState.DEFAULT_TARGET_CARBS,
                        targetFatsG = DashboardUiState.DEFAULT_TARGET_FATS,
                        lastUpdatedTimestamp = System.currentTimeMillis(),
                        sex = sex,
                        goal = goal,
                        activityLevel = activityLevel
                    )
                )
                val ts = System.currentTimeMillis()
                repository.addMeasurement(
                    BodyMeasurement(
                        timestamp = ts,
                        dateString = DateUtils.format(ts),
                        weightKg = weightKg
                    )
                )
                pendingInitialTargetDesign = true
            }.onFailure { e ->
                _analysisState.update {
                    it.copy(userMessage = e.message ?: "Couldn't save your profile")
                }
            }
            _onboardingSaving.value = false
        }
    }

    /**
     * Called when the main UI becomes active after onboarding. Designs calorie/macro targets
     * once via AI and applies them to the profile.
     */
    fun onDashboardLaunched() {
        if (!pendingInitialTargetDesign) return
        pendingInitialTargetDesign = false
        viewModelScope.launch {
            val profile = repository.activeProfile
                .mapNotNull { p -> p?.takeIf { it.hasBasicsConfigured() } }
                .first()
            _analysisState.update { it.copy(userMessage = "Calculating your calorie targets…") }
            val context = buildTargetContext(
                age = profile.age,
                heightCm = profile.heightCm,
                weightKg = profile.weightKg,
                sex = profile.sex,
                activityLevel = profile.activityLevel,
                goal = profile.goal
            )
            runCatching { repository.designTargets(context) }
                .onSuccess { plan ->
                    repository.saveProfile(
                        profile.copy(
                            dailyTargetCalories = plan.dailyTargetCalories,
                            targetProteinG = plan.targetProteinG,
                            targetCarbsG = plan.targetCarbsG,
                            targetFatsG = plan.targetFatsG,
                            goal = plan.recommendedGoal.ifBlank { profile.goal },
                            goalRationale = plan.rationale,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )
                    )
                    _analysisState.update {
                        it.copy(userMessage = "Your personalized targets are ready")
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(
                            userMessage = e.message
                                ?: "Using default targets — refine anytime in Body"
                        )
                    }
                }
        }
    }

    /** Hits the provider's model-list endpoint to confirm credentials work. */
    private suspend fun probeAiCredentials(settings: AppSettings) {
        when (settings.provider) {
            AiProvider.OPENROUTER -> {
                val key = settings.activeKey(AiProvider.OPENROUTER)
                check(key.isNotBlank()) { "Enter an OpenRouter API key" }
                repository.fetchFreeVisionModels(key)
            }
            AiProvider.GEMINI -> {
                val key = settings.activeKey(AiProvider.GEMINI)
                check(key.isNotBlank()) { "Enter a Gemini API key" }
                repository.fetchGeminiVisionModels(key)
            }
            AiProvider.OLLAMA -> {
                val base = settings.ollamaEffectiveBaseUrl
                check(base.isNotBlank()) { "Enter your Ollama server URL" }
                if (settings.ollamaUseCloud) {
                    val key = settings.activeKey(AiProvider.OLLAMA)
                    check(key.isNotBlank()) { "Enter an Ollama Cloud API key" }
                    repository.fetchOllamaVisionModels(base, key)
                } else {
                    repository.fetchOllamaVisionModels(base)
                }
            }
        }
    }

    fun saveProfile(
        age: Int,
        weightKg: Double,
        heightCm: Double,
        dailyTargetCalories: Int,
        targetProteinG: Int,
        targetCarbsG: Int,
        targetFatsG: Int,
        sex: String? = null,
        goal: String = "RECOMP",
        activityLevel: String = "MODERATE"
    ) {
        viewModelScope.launch {
            repository.saveProfile(
                UserProfile(
                    id = 1,
                    age = age,
                    weightKg = weightKg,
                    heightCm = heightCm,
                    dailyTargetCalories = dailyTargetCalories,
                    targetProteinG = targetProteinG,
                    targetCarbsG = targetCarbsG,
                    targetFatsG = targetFatsG,
                    lastUpdatedTimestamp = System.currentTimeMillis(),
                    sex = sex,
                    goal = goal,
                    activityLevel = activityLevel,
                    // Preserve the latest AI rationale across manual edits.
                    goalRationale = dashboardState.value.profile?.goalRationale
                )
            )
            _analysisState.update { it.copy(userMessage = "Body saved · dashboard recalibrated") }
        }
    }

    /**
     * The "User State Context" block (Step 3) sent to the AI so estimates can be personalised
     * (e.g. body weight strongly affects calories burned during exercise).
     */
    private fun buildUserStateContext(): String {
        val state = dashboardState.value
        val profile = state.profile
        val latest = latestMeasurement.value
        return JSONObject().apply {
            put("date", _selectedDate.value)
            put("age", profile?.age ?: JSONObject.NULL)
            put("sex", profile?.sex ?: JSONObject.NULL)
            put("goal", profile?.goal ?: JSONObject.NULL)
            put("activity_level", profile?.activityLevel ?: JSONObject.NULL)
            put("weight_kg", profile?.weightKg ?: JSONObject.NULL)
            put("height_cm", profile?.heightCm ?: JSONObject.NULL)
            put("daily_target_calories", state.targetCalories)
            put("target_protein_g", state.targetProtein)
            put("consumed_calories_today", state.consumedCalories)
            put("burned_calories_today", state.burnedCalories)
            put("net_calories_today", state.netCalories)
            put("remaining_calories_today", state.remainingCalories)
            put("latest_measurement", latest?.let(::measurementJson) ?: JSONObject.NULL)
        }.toString()
    }

    /**
     * Profile basics (as currently shown on screen, not necessarily saved yet) + latest body
     * composition + recent trends, for AI target design.
     */
    private fun buildTargetContext(
        age: Int,
        heightCm: Double,
        weightKg: Double,
        sex: String?,
        activityLevel: String,
        goal: String
    ): String {
        val measurements = bodyMeasurements.value
        val latest = latestMeasurement.value
        val food = weeklyFood.value
        val exercise = weeklyExercise.value

        val avgIntake = food.takeIf { it.isNotEmpty() }?.map { it.totalCalories }?.average()
        val avgBurned = exercise.takeIf { it.isNotEmpty() }?.map { it.totalBurned }?.average()
        // Oldest-to-newest weight change across the stored readings.
        val weightChange = measurements.takeIf { it.size >= 2 }?.let {
            it.first().weightKg - it.last().weightKg
        }

        return JSONObject().apply {
            put("age", if (age > 0) age else JSONObject.NULL)
            put("sex", sex ?: JSONObject.NULL)
            put("height_cm", if (heightCm > 0) heightCm else JSONObject.NULL)
            put("current_weight_kg", if (weightKg > 0) weightKg else JSONObject.NULL)
            put("activity_level", activityLevel)
            put("stated_goal", goal.ifBlank { "AUTO" })
            put("latest_measurement", latest?.let(::measurementJson) ?: JSONObject.NULL)
            put("weight_change_kg_recent", weightChange ?: JSONObject.NULL)
            put("avg_daily_calories_in_recent", avgIntake ?: JSONObject.NULL)
            put("avg_daily_calories_burned_recent", avgBurned ?: JSONObject.NULL)
            put("current_target_calories", dashboardState.value.targetCalories)
        }.toString()
    }

    /**
     * Body-composition series + nutrition/exercise trends for the AI progress insight.
     * Past ~30 days are day-level detail; older history is rolled into concise calendar-month
     * averages so the prompt stays token-efficient.
     */
    private suspend fun buildProgressContext(): String {
        val profile = dashboardState.value.profile
        val today = _realToday.value.ifBlank { DateUtils.today() }
        // Inclusive 30-day window ending today.
        val detailCutoff = DateUtils.addDays(today, -(PROGRESS_DETAIL_DAYS - 1))

        val allFood = repository.getAllFoodDailySummaries()
        val allExercise = repository.getAllExerciseDailySummaries()
        val allMeasurements = repository.getAllBodyMeasurementsOnce()

        val burnedByDate = allExercise.associate { it.dateString to it.totalBurned }

        val recentFood = allFood.filter { it.dateString >= detailCutoff }
        val olderFood = allFood.filter { it.dateString < detailCutoff }
        val recentExercise = allExercise.filter { it.dateString >= detailCutoff }
        val olderExercise = allExercise.filter { it.dateString < detailCutoff }

        val recentMeasurements = allMeasurements.filter { it.dateString >= detailCutoff }
        val olderMeasurements = allMeasurements.filter { it.dateString < detailCutoff }

        val measurementSeries = JSONArray().apply {
            recentMeasurements.asReversed().forEach { put(measurementJson(it)) }
        }
        val priorBodyMonths = buildPriorMonthBodySeries(olderMeasurements)
        val dailyFoodSeries = buildNutritionSeries(recentFood, burnedByDate)
        val priorFoodMonths = buildPriorMonthNutritionSeries(olderFood, burnedByDate)
        val dailyExerciseSeries = buildExerciseSeries(recentExercise)
        val priorExerciseMonths = buildPriorMonthExerciseSeries(olderExercise)

        val avgNet = recentFood.takeIf { it.isNotEmpty() }
            ?.map { it.totalCalories - (burnedByDate[it.dateString] ?: 0) }
            ?.average()
        val eatBackRatio = recentExercise.sumOf { it.totalBurned }.takeIf { it > 0 }?.let { totalBurned ->
            val avgIntakeOnExerciseDays = recentFood
                .filter { (burnedByDate[it.dateString] ?: 0) > 0 }
                .sumOf { it.totalCalories }
            avgIntakeOnExerciseDays.toDouble() / totalBurned
        }

        return JSONObject().apply {
            put("age", profile?.age?.takeIf { it > 0 } ?: JSONObject.NULL)
            put("sex", profile?.sex?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            put("height_cm", profile?.heightCm?.takeIf { it > 0 } ?: JSONObject.NULL)
            put("weight_kg", profile?.weightKg?.takeIf { it > 0 } ?: JSONObject.NULL)
            put("activity_level", profile?.activityLevel ?: JSONObject.NULL)
            put("goal", profile?.goal ?: JSONObject.NULL)
            put("target_calories_rest_day_baseline", dashboardState.value.targetCalories)
            put("target_protein_g", dashboardState.value.targetProtein)
            put("target_carbs_g", dashboardState.value.targetCarbs)
            put("target_fats_g", dashboardState.value.targetFats)
            put(
                "calorie_model_note",
                "target_calories_rest_day_baseline is compared against NET calories " +
                    "(calories eaten minus calories burned via exercise) each day, so exercise " +
                    "calories are automatically credited back to that day's eating allowance."
            )
            put(
                "metrics_granularity_note",
                "nutrition_daily / exercise_daily / body_measurements cover the past " +
                    "$PROGRESS_DETAIL_DAYS days in detail. nutrition_prior_months / " +
                    "exercise_prior_months / body_prior_months summarise older history as " +
                    "calendar-month averages (omit if empty)."
            )
            put("avg_daily_net_calories_recent", avgNet ?: JSONObject.NULL)
            put("avg_exercise_calorie_eat_back_ratio", eatBackRatio ?: JSONObject.NULL)
            put("body_measurements", measurementSeries)
            put("body_prior_months", priorBodyMonths)
            put("nutrition_daily", dailyFoodSeries)
            put("nutrition_prior_months", priorFoodMonths)
            put("exercise_daily", dailyExerciseSeries)
            put("exercise_prior_months", priorExerciseMonths)
        }.toString()
    }

    private fun buildNutritionSeries(
        food: List<FoodDailySummary>,
        burnedByDate: Map<String, Int>
    ): JSONArray = JSONArray().apply {
        food.asReversed().forEach { s ->
            val burned = burnedByDate[s.dateString] ?: 0
            put(JSONObject().apply {
                put("date", s.dateString)
                put("calories", s.totalCalories)
                put("calories_burned", burned)
                put("net_calories", s.totalCalories - burned)
                put("protein_g", s.totalProtein)
                put("carbs_g", s.totalCarbs)
                put("fats_g", s.totalFats)
            })
        }
    }

    private fun buildExerciseSeries(exercise: List<ExerciseDailySummary>): JSONArray =
        JSONArray().apply {
            exercise.asReversed().forEach { s ->
                put(JSONObject().apply {
                    put("date", s.dateString)
                    put("calories_burned", s.totalBurned)
                })
            }
        }

    /** Calendar-month averages for food days older than the detailed window (oldest → newest). */
    private fun buildPriorMonthNutritionSeries(
        food: List<FoodDailySummary>,
        burnedByDate: Map<String, Int>
    ): JSONArray {
        if (food.isEmpty()) return JSONArray()
        val byMonth = food.groupBy { it.dateString.take(7) }.toSortedMap()
        return JSONArray().apply {
            byMonth.forEach { (month, days) ->
                val n = days.size
                val burned = days.map { burnedByDate[it.dateString] ?: 0 }
                put(JSONObject().apply {
                    put("month", month)
                    put("days_logged", n)
                    put("avg_calories", days.map { it.totalCalories }.average().roundToInt())
                    put("avg_protein_g", days.map { it.totalProtein }.average().roundToInt())
                    put("avg_carbs_g", days.map { it.totalCarbs }.average().roundToInt())
                    put("avg_fats_g", days.map { it.totalFats }.average().roundToInt())
                    put("avg_calories_burned", burned.average().roundToInt())
                    put(
                        "avg_net_calories",
                        days.map { it.totalCalories - (burnedByDate[it.dateString] ?: 0) }
                            .average()
                            .roundToInt()
                    )
                    put("exercise_days", burned.count { it > 0 })
                })
            }
        }
    }

    /** Calendar-month exercise totals for days older than the detailed window (oldest → newest). */
    private fun buildPriorMonthExerciseSeries(
        exercise: List<ExerciseDailySummary>
    ): JSONArray {
        if (exercise.isEmpty()) return JSONArray()
        val byMonth = exercise.groupBy { it.dateString.take(7) }.toSortedMap()
        return JSONArray().apply {
            byMonth.forEach { (month, days) ->
                put(JSONObject().apply {
                    put("month", month)
                    put("days_trained", days.size)
                    put("avg_burn_kcal", days.map { it.totalBurned }.average().roundToInt())
                    put("total_burn_kcal", days.sumOf { it.totalBurned })
                })
            }
        }
    }

    /** One concise row per calendar month of older body readings (oldest → newest). */
    private fun buildPriorMonthBodySeries(measurements: List<BodyMeasurement>): JSONArray {
        if (measurements.isEmpty()) return JSONArray()
        val byMonth = measurements.groupBy { it.dateString.take(7) }.toSortedMap()
        return JSONArray().apply {
            byMonth.forEach { (month, readings) ->
                // readings arrive newest-first from Room; pick chronological ends within the month.
                val oldest = readings.last()
                val newest = readings.first()
                put(JSONObject().apply {
                    put("month", month)
                    put("readings", readings.size)
                    put("avg_weight_kg", readings.map { it.weightKg }.average())
                    put("start_weight_kg", oldest.weightKg)
                    put("end_weight_kg", newest.weightKg)
                    newest.bodyFatPct?.let { put("end_body_fat_pct", it) }
                    newest.muscleMassKg?.let { put("end_muscle_mass_kg", it) }
                    newest.visceralFat?.let { put("end_visceral_fat", it) }
                    newest.bmr?.let { put("end_bmr", it) }
                    newest.bmi?.let { put("end_bmi", it) }
                })
            }
        }
    }

    /** Serializes a body reading to JSON, omitting null smart-scale fields. */
    private fun measurementJson(m: BodyMeasurement): JSONObject = JSONObject().apply {
        put("date", m.dateString)
        put("weight_kg", m.weightKg)
        m.bmi?.let { put("bmi", it) }
        m.bodyFatPct?.let { put("body_fat_pct", it) }
        m.muscleRatePct?.let { put("muscle_rate_pct", it) }
        m.bodyWaterPct?.let { put("body_water_pct", it) }
        m.boneMassKg?.let { put("bone_mass_kg", it) }
        m.bmr?.let { put("bmr", it) }
        m.metabolicAge?.let { put("metabolic_age", it) }
        m.visceralFat?.let { put("visceral_fat", it) }
        m.subcutaneousFatPct?.let { put("subcutaneous_fat_pct", it) }
        m.proteinMassKg?.let { put("protein_mass_kg", it) }
        m.muscleMassKg?.let { put("muscle_mass_kg", it) }
        m.fatFreeMassKg?.let { put("fat_free_mass_kg", it) }
        m.skeletalMuscleMassKg?.let { put("skeletal_muscle_mass_kg", it) }
        m.waterWeightKg?.let { put("water_weight_kg", it) }
        m.fatMassKg?.let { put("fat_mass_kg", it) }
    }

    companion object {
        /** Calendar days of day-level nutrition/exercise/body detail for AI insights. */
        private const val PROGRESS_DETAIL_DAYS = 30
    }
}

/** Manual ViewModel factory (no DI framework); wires the shared repositories in. */
class MainViewModelFactory(
    private val repository: FitnessRepository,
    private val settingsRepository: SettingsRepository,
    private val updateChecker: UpdateChecker
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MainViewModel(repository, settingsRepository, updateChecker) as T
    }
}
