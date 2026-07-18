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
import com.anant.fitbuddy.data.remote.UpdateChecker
import com.anant.fitbuddy.data.remote.UpdateCheckResult
import com.anant.fitbuddy.data.repository.AnalysisOutcome
import com.anant.fitbuddy.data.repository.FitnessRepository
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.data.settings.SettingsRepository
import com.anant.fitbuddy.util.DateUtils
import com.anant.fitbuddy.util.ProgressMetricsCompressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val editingFoodLogId: Int? = null
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
    val draft: WorkoutDraft
)

/** State of the manual "Check for Updates" flow in Settings. */
@Immutable
data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateInfo: UpdateCheckResult.Available? = null,
    val statusMessage: String? = null,
    val statusIsError: Boolean = false
)

class MainViewModel(
    private val repository: FitnessRepository,
    private val settingsRepository: SettingsRepository,
    private val updateChecker: UpdateChecker
) : ViewModel() {

    private val today: String = DateUtils.today()

    // --- Update check -------------------------------------------------------------------------

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    /** Manual check triggered from Settings; app is sideloaded, so there's no store-driven prompt. */
    fun checkForUpdates(currentVersionCode: Int) {
        if (_updateState.value.isChecking) return
        viewModelScope.launch {
            _updateState.update { it.copy(isChecking = true, statusMessage = null, statusIsError = false) }
            when (val result = updateChecker.checkForUpdate(currentVersionCode)) {
                is UpdateCheckResult.Available -> _updateState.update {
                    it.copy(isChecking = false, updateInfo = result, statusMessage = null, statusIsError = false)
                }
                UpdateCheckResult.UpToDate -> _updateState.update {
                    it.copy(isChecking = false, updateInfo = null, statusMessage = "You're on the latest version", statusIsError = false)
                }
                is UpdateCheckResult.Error -> _updateState.update {
                    it.copy(isChecking = false, updateInfo = null, statusMessage = result.message, statusIsError = true)
                }
            }
        }
    }

    fun dismissUpdatePrompt() {
        _updateState.update { it.copy(updateInfo = null, statusMessage = null, statusIsError = false) }
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

    private val totalsFlow = combine(
        repository.getFoodTotalsToday(today),
        repository.getExerciseBurnToday(today)
    ) { food, burned ->
        Totals(
            calories = food.totalCalories,
            burned = burned ?: 0,
            protein = food.totalProtein,
            carbs = food.totalCarbs,
            fats = food.totalFats
        )
    }

    val dashboardState: StateFlow<DashboardUiState> =
        combine(repository.activeProfile, totalsFlow) { profile, totals ->
            DashboardUiState(
                profile = profile,
                consumedCalories = totals.calories,
                burnedCalories = totals.burned,
                consumedProtein = totals.protein,
                consumedCarbs = totals.carbs,
                consumedFats = totals.fats
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** null while the profile hasn't loaded yet; true when first-time setup is required. */
    val needsOnboarding: StateFlow<Boolean?> =
        repository.activeProfile
            .map { profile -> profile == null || !profile.hasBasicsConfigured() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _onboardingSaving = MutableStateFlow(false)
    val onboardingSaving: StateFlow<Boolean> = _onboardingSaving.asStateFlow()

    val foodLogsToday: StateFlow<List<FoodLog>> =
        repository.getFoodLogsForDate(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exerciseLogsToday: StateFlow<List<ExerciseLog>> =
        repository.getExerciseLogsForDate(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Saved foods & meal presets ---------------------------------------------------------

    val savedFoods: StateFlow<List<SavedFood>> =
        repository.savedFoods
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mealPresets: StateFlow<List<MealPreset>> =
        repository.mealPresets
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun logMealPreset(preset: MealPreset) {
        viewModelScope.launch {
            repository.logMealPreset(preset)
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
        viewModelScope.launch {
            runCatching { repository.lookupProductByBarcode(barcode) }
                .onSuccess(onSuccess)
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(
                            errorDialogTitle = "Product not found",
                            errorDialogMessage = e.message ?: "Couldn't look up this barcode"
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

    val monthlyFood: StateFlow<List<FoodDailySummary>> =
        repository.getMonthlyFoodSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyExercise: StateFlow<List<ExerciseDailySummary>> =
        repository.getWeeklyExerciseSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthlyExercise: StateFlow<List<ExerciseDailySummary>> =
        repository.getMonthlyExerciseSummaries()
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
            runCatching { repository.logWorkoutSession(draft, weightKg, buildWorkoutContext(draft, weightKg)) }
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
        _analysisState.update { it.copy(isLoading = true, clarificationMessage = null) }
        viewModelScope.launch {
            val outcome = repository.analyze(text, image, buildUserStateContext())
            handleOutcome(outcome)
        }
    }

    private fun handleOutcome(outcome: AnalysisOutcome) {
        when (outcome) {
            is AnalysisOutcome.FoodReady -> {
                clearPending()
                _analysisState.update {
                    it.copy(
                        isLoading = false,
                        clarificationMessage = null,
                        foodDraft = outcome.draft,
                        mealDraft = null,
                        userMessage = outcome.failoverNote
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
                            ?: "Logged ${outcome.activityName} · -${outcome.caloriesBurned} kcal"
                    )
                }
            }

            is AnalysisOutcome.NeedsClarification -> _analysisState.update {
                it.copy(
                    isLoading = false,
                    clarificationMessage = outcome.message,
                    userMessage = outcome.failoverNote
                )
            }

            is AnalysisOutcome.NotIdentified -> {
                clearPending()
                _analysisState.update {
                    it.copy(
                        isLoading = false,
                        errorDialogTitle = "Item not identified",
                        errorDialogMessage = outcome.message,
                        userMessage = outcome.failoverNote
                    )
                }
            }

            is AnalysisOutcome.Error -> {
                clearPending()
                _analysisState.update {
                    it.copy(
                        isLoading = false,
                        errorDialogTitle = "Couldn't reach AI",
                        errorDialogMessage = outcome.message
                    )
                }
            }
        }
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
            _analysisState.update { state ->
                when (outcome) {
                    is AnalysisOutcome.FoodReady ->
                        state.copy(
                            isReanalyzing = false,
                            foodDraft = outcome.draft,
                            reviewMessage = outcome.failoverNote
                        )

                    // Non-success outcomes keep the current draft; report via the dialog's own
                    // snackbar (the app-level one is hidden behind the full-screen review dialog).
                    is AnalysisOutcome.NeedsClarification ->
                        state.copy(isReanalyzing = false, reviewMessage = outcome.message)

                    is AnalysisOutcome.NotIdentified ->
                        state.copy(isReanalyzing = false, reviewMessage = outcome.message)

                    is AnalysisOutcome.Error ->
                        state.copy(isReanalyzing = false, reviewMessage = outcome.message)

                    is AnalysisOutcome.ExerciseSaved ->
                        state.copy(
                            isReanalyzing = false,
                            reviewMessage = "That looks like an activity, not a food item."
                        )
                }
            }
        }
    }

    /** Call after a snackbar/toast has shown [AnalysisUiState.userMessage]. */
    fun consumeUserMessage() = _analysisState.update { it.copy(userMessage = null) }

    /** Call after the review dialog's snackbar has shown [AnalysisUiState.reviewMessage]. */
    fun consumeReviewMessage() = _analysisState.update { it.copy(reviewMessage = null) }

    private fun clearPending() {
        pendingText = ""
        pendingImage = null
    }

    // --- Profile ----------------------------------------------------------------------------

    /**
     * First-run setup: saves profile with default targets, seeds an initial body reading, and
     * (when the user didn't skip the AI step) persists their chosen provider/API key.
     */
    fun completeOnboarding(
        age: Int,
        weightKg: Double,
        heightCm: Double,
        sex: String?,
        goal: String,
        activityLevel: String,
        aiSettings: AppSettings?
    ) {
        if (_onboardingSaving.value) return
        _onboardingSaving.value = true
        viewModelScope.launch {
            runCatching {
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
                aiSettings?.let { settingsRepository.save(it) }
            }.onFailure { e ->
                _analysisState.update {
                    it.copy(userMessage = e.message ?: "Couldn't save your profile")
                }
            }
            _onboardingSaving.value = false
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
        return JSONObject().apply {
            put("date", today)
            put("age", profile?.age ?: JSONObject.NULL)
            put("sex", profile?.sex ?: JSONObject.NULL)
            put("goal", profile?.goal ?: JSONObject.NULL)
            put("weight_kg", profile?.weightKg ?: JSONObject.NULL)
            put("height_cm", profile?.heightCm ?: JSONObject.NULL)
            put("daily_target_calories", state.targetCalories)
            put("consumed_calories_today", state.consumedCalories)
            put("burned_calories_today", state.burnedCalories)
            put("net_calories_today", state.netCalories)
            put("remaining_calories_today", state.remainingCalories)
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

    /** Body-composition series + nutrition/exercise trends, for the AI progress insight. */
    private fun buildProgressContext(): String {
        val profile = dashboardState.value.profile
        val measurements = bodyMeasurements.value.take(30)
        val weeklyFoodData = weeklyFood.value
        val monthlyFoodData = monthlyFood.value
        val weeklyExerciseData = weeklyExercise.value
        val monthlyExerciseData = monthlyExercise.value
        val monthlyBurnedByDate = monthlyExerciseData.associate { it.dateString to it.totalBurned }
        val weeklyBurnedByDate = weeklyExerciseData.associate { it.dateString to it.totalBurned }

        val measurementSeries = JSONArray().apply {
            measurements.reversed().forEach { put(measurementJson(it)) }
        }
        val weeklyFoodSeries = buildNutritionSeries(weeklyFoodData, weeklyBurnedByDate)
        val monthlyFoodSeries = buildNutritionSeries(monthlyFoodData, monthlyBurnedByDate)
        val weeklyExerciseSeries = buildExerciseSeries(weeklyExerciseData)
        val monthlyExerciseSeries = buildExerciseSeries(monthlyExerciseData)

        val avgNet = monthlyFoodData.takeIf { it.isNotEmpty() }
            ?.map { it.totalCalories - (monthlyBurnedByDate[it.dateString] ?: 0) }
            ?.average()
        val eatBackRatio = monthlyExerciseData.sumOf { it.totalBurned }.takeIf { it > 0 }?.let { totalBurned ->
            val avgIntakeOnExerciseDays = monthlyFoodData
                .filter { (monthlyBurnedByDate[it.dateString] ?: 0) > 0 }
                .sumOf { it.totalCalories }
            avgIntakeOnExerciseDays.toDouble() / totalBurned
        }

        return JSONObject().apply {
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
            put("avg_daily_net_calories_recent", avgNet ?: JSONObject.NULL)
            put("avg_exercise_calorie_eat_back_ratio", eatBackRatio ?: JSONObject.NULL)
            put("body_measurements", measurementSeries)
            put("nutrition_weekly", weeklyFoodSeries)
            put("nutrition_daily", monthlyFoodSeries)
            put("exercise_weekly", weeklyExerciseSeries)
            put("exercise_daily", monthlyExerciseSeries)
        }.toString()
    }

    private fun buildNutritionSeries(
        food: List<FoodDailySummary>,
        burnedByDate: Map<String, Int>
    ): JSONArray = JSONArray().apply {
        food.reversed().forEach { s ->
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
            exercise.reversed().forEach { s ->
                put(JSONObject().apply {
                    put("date", s.dateString)
                    put("calories_burned", s.totalBurned)
                })
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

    private data class Totals(
        val calories: Int,
        val burned: Int,
        val protein: Int,
        val carbs: Int,
        val fats: Int
    )
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
