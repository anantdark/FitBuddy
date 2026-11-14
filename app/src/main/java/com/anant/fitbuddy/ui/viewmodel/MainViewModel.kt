package com.anant.fitbuddy.ui.viewmodel

import com.anant.fitbuddy.util.roundMacro
import android.app.Application
import androidx.annotation.StringRes
import com.anant.fitbuddy.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.MainActivity
import com.anant.fitbuddy.data.backup.BackupErrorMessages
import com.anant.fitbuddy.data.backup.BackupImportResult
import com.anant.fitbuddy.data.healthconnect.forSync
import com.anant.fitbuddy.data.healthconnect.sanitized
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.ExerciseDailySummary
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.FoodDailySummary
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.data.database.MealPreset
import com.anant.fitbuddy.data.database.SavedFood
import com.anant.fitbuddy.data.database.UserProfile
import com.anant.fitbuddy.data.database.WaterDrinkKind
import com.anant.fitbuddy.data.database.WaterLog
import com.anant.fitbuddy.data.database.filledForDateRange
import com.anant.fitbuddy.data.model.ActivityLevelRecommendation
import com.anant.fitbuddy.data.model.ActivityLevelRecommender
import com.anant.fitbuddy.data.model.BodyTrendAnalyzer
import com.anant.fitbuddy.data.model.BodyTrendEvidence
import com.anant.fitbuddy.data.model.BodyTrendReading
import com.anant.fitbuddy.data.model.CatalogExercise
import com.anant.fitbuddy.data.model.GifSuggestion
import com.anant.fitbuddy.data.model.COMMON_EXERCISES_SEED
import com.anant.fitbuddy.data.model.ExerciseDraft
import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.data.model.FoodDraft
import com.anant.fitbuddy.data.model.QueuedPhotoLog
import com.anant.fitbuddy.data.model.FoodEntryDraft
import com.anant.fitbuddy.data.model.HealthTargetCalculator
import com.anant.fitbuddy.data.model.HealthTargetInput
import com.anant.fitbuddy.data.model.MealDraft
import com.anant.fitbuddy.data.model.asSingleFoodDraftOrNull
import com.anant.fitbuddy.data.model.snapshot
import com.anant.fitbuddy.data.model.toFoodDraft
import com.anant.fitbuddy.data.model.toFoodEntry
import com.anant.fitbuddy.data.model.toSingleFoodMeal
import com.anant.fitbuddy.data.model.ModelOption
import com.anant.fitbuddy.data.model.NutritionTargetHistory
import com.anant.fitbuddy.data.model.NutritionTrendEvidence
import com.anant.fitbuddy.data.model.ProgressChatTurn
import com.anant.fitbuddy.data.model.ProgressInsightResponse
import com.anant.fitbuddy.data.model.ScannedProduct
import com.anant.fitbuddy.data.model.TargetPlanOptions
import com.anant.fitbuddy.data.model.TargetPlanPersonalizer
import com.anant.fitbuddy.data.model.TargetPlanResponse
import com.anant.fitbuddy.data.model.WorkoutDraft
import com.anant.fitbuddy.crash.CrashReporter
import com.anant.fitbuddy.util.BackupShare
import com.anant.fitbuddy.util.DiagnosticLogger
import com.anant.fitbuddy.crash.HeartbeatInfo
import com.anant.fitbuddy.crash.HeartbeatKind
import com.anant.fitbuddy.data.donors.DemoDonors
import com.anant.fitbuddy.data.donors.DonorBadgeRules
import com.anant.fitbuddy.data.donors.DonorEntry
import com.anant.fitbuddy.data.donors.DonorListRepository
import com.anant.fitbuddy.data.remote.OpenFoodFactsProductUnavailableException
import com.anant.fitbuddy.data.remote.UpdateChannel
import com.anant.fitbuddy.data.remote.UpdateChecker
import com.anant.fitbuddy.util.FdroidUpdateLauncher
import com.anant.fitbuddy.data.remote.UpdateCheckResult
import com.anant.fitbuddy.data.remote.oauth.OpenRouterOAuth
import com.anant.fitbuddy.data.remote.oauth.OpenRouterOAuthCallbackServer
import com.anant.fitbuddy.data.remote.oauth.OpenRouterPkce
import com.anant.fitbuddy.data.region.AppRegion
import com.anant.fitbuddy.data.repository.AnalysisOutcome
import com.anant.fitbuddy.data.repository.FitnessRepository
import com.anant.fitbuddy.data.model.ProgressChatSession
import com.anant.fitbuddy.data.model.ProgressChatSessionMessage
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.data.settings.FailoverLadders
import com.anant.fitbuddy.data.settings.PhotoQueueStore
import com.anant.fitbuddy.data.settings.ProgressChatSessionStore
import com.anant.fitbuddy.data.settings.SettingsRepository
import com.anant.fitbuddy.data.remote.dto.ModelCatalogModality
import com.anant.fitbuddy.reminders.DonationReminderScheduler
import com.anant.fitbuddy.util.DateUtils
import com.anant.fitbuddy.util.ProgressMetricsCompressor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject


/** Snapshot of everything the Dashboard needs, with derived net-balance/macros logic. */
@Immutable
data class DashboardUiState(
    val profile: UserProfile? = null,
    /** Targets in force for the selected day (from history); falls back to profile/defaults. */
    val targetCalories: Int = DEFAULT_TARGET_CALORIES,
    val targetProtein: Int = DEFAULT_TARGET_PROTEIN,
    val targetCarbs: Int = DEFAULT_TARGET_CARBS,
    val targetFats: Int = DEFAULT_TARGET_FATS,
    val consumedCalories: Int = 0,
    val burnedCalories: Int = 0,
    val consumedProtein: Double = 0.0,
    val consumedCarbs: Double = 0.0,
    val consumedFats: Double = 0.0
) {
    /** Exercise burn is shown separately; the TDEE-based target already includes average activity. */
    val netCalories: Int get() = consumedCalories - burnedCalories
    val remainingCalories: Int get() = targetCalories - consumedCalories

    val remainingProtein: Double get() = (targetProtein - consumedProtein).coerceAtLeast(0.0).roundMacro()
    val remainingCarbs: Double get() = (targetCarbs - consumedCarbs).coerceAtLeast(0.0).roundMacro()
    val remainingFats: Double get() = (targetFats - consumedFats).coerceAtLeast(0.0).roundMacro()

    /** 0f..1f fill of the calorie ring (food intake progress toward the daily target). */
    val calorieProgress: Float
        get() = if (targetCalories <= 0) 0f else (consumedCalories.toFloat() / targetCalories).coerceIn(0f, 1f)

    val isOverTarget: Boolean get() = consumedCalories > targetCalories

    companion object {
        const val DEFAULT_TARGET_CALORIES = 2000
        const val DEFAULT_TARGET_PROTEIN = 120
        const val DEFAULT_TARGET_CARBS = 250
        const val DEFAULT_TARGET_FATS = 60

        fun targetsFrom(profile: UserProfile?, date: String): QuadrupleTargets {
            val period = profile?.targetsForDate(date)
            return QuadrupleTargets(
                calories = period?.kcal ?: DEFAULT_TARGET_CALORIES,
                protein = period?.proteinG ?: DEFAULT_TARGET_PROTEIN,
                carbs = period?.carbsG ?: DEFAULT_TARGET_CARBS,
                fats = period?.fatsG ?: DEFAULT_TARGET_FATS
            )
        }
    }
}

/** Small holder so [DashboardUiState.targetsFrom] stays allocation-light. */
data class QuadrupleTargets(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int
)


/** Transient state for the AI analysis flow (loading, clarification prompt, review draft, snackbar). */
@Immutable
data class AnalysisUiState(
    val isLoading: Boolean = false,
    /** Model ID currently being used for analysis (updates dynamically on auto-failover). */
    val analyzingModel: String? = null,
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
    /**
     * When editing a food inside an open [mealDraft], the index being edited.
     * Null when food review is standalone or for meal-builder (builder keeps its own index).
     */
    val editingMealFoodIndex: Int? = null,
    /** Developer: last raw AI JSON when "Show raw AI JSON" is on. */
    val rawAiJson: String? = null
)

const val MAX_PHOTO_LOG_IMAGES = 5

/** Photos and note being composed in the photo-log dialog before sending or queueing. */
@Immutable
data class PhotoDraftUiState(
    val visible: Boolean = false,
    val photos: List<ByteArray> = emptyList(),
    val note: String = "",
    /** Set when editing an existing queue item instead of composing a new log. */
    val editingQueueId: String? = null
)

/** State of the free/vision model catalog used by the Settings dropdown. */
@Immutable
data class ModelsUiState(
    val isLoading: Boolean = false,
    val options: List<ModelOption> = emptyList(),
    val error: String? = null,
    val loaded: Boolean = false
)


/** State of the science-based target calculation shown in Body. */
@Immutable
data class TargetPlanUiState(
    val isLoading: Boolean = false,
    val plan: TargetPlanResponse? = null,
    val error: String? = null,
    val appliedRevision: Long = 0L
)

/** One message in the progress-coach follow-up chat. */
@Immutable
data class ProgressChatMessage(
    val role: ProgressChatRole,
    val text: String
)

enum class ProgressChatRole { USER, ASSISTANT }

/** State of the AI progress-insight card shown at the top of Progress. */
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
    val error: String? = null,
    /** True when a kept chat exists on disk and is still within the UTC-day window. */
    val hasKeptChat: Boolean = false,
    /** Shows the insight summary popup after a fresh generate (not when resuming a kept chat). */
    val showInsightDialog: Boolean = false,
    /** Epoch ms of the latest chat message; kept sessions expire after that UTC day. */
    val lastMessageAtEpochMs: Long = 0L
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
    val consumedProtein: Double = 0.0,
    val consumedCarbs: Double = 0.0,
    val consumedFats: Double = 0.0
)

/** State of the manual "Check for Updates" flow in Settings. */
@Immutable
data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateInfo: UpdateCheckResult.Available? = null,
    val statusMessage: String? = null,
    val statusIsError: Boolean = false,
    /** True after a successful pre-update export (cloud or local). */
    val backupCompleted: Boolean = false,
    val isExportingBackup: Boolean = false,
    /** Waiting for the SAF create-document picker (local export path). */
    val isAwaitingBackupFilePick: Boolean = false,
    /** APK download URL to open in the browser after [backupCompleted]. */
    val pendingDownloadUrlAfterBackup: String? = null,
    /** Inline status under the update dialog backup actions. */
    val backupStatusMessage: String? = null,
    val backupStatusIsError: Boolean = false
)

private data class TargetPlanningContext(
    val options: TargetPlanOptions,
    val bodyTrend: BodyTrendEvidence,
    val nutritionTrend: NutritionTrendEvidence
)

class MainViewModel(
    private val app: Application,
    private val repository: FitnessRepository,
    private val settingsRepository: SettingsRepository,
    private val updateChecker: UpdateChecker
) : ViewModel() {

    private fun str(@StringRes id: Int, vararg args: Any): String =
        if (args.isEmpty()) app.getString(id) else app.getString(id, *args)

    // --- Selected calendar day (rolling week dashboard) ------------------------------------

    /** Returns the logical "today" string for the current day-change-hour setting. */
    private fun todayWithOffset(): String =
        DateUtils.today(settings.value.dayChangeHour)

    private val _realToday = MutableStateFlow(DateUtils.today())
    val realToday: StateFlow<String> = _realToday.asStateFlow()

    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    /** Last day of the week strip shown in Week history (defaults to [realToday]). */
    private val _weekEndDate = MutableStateFlow(DateUtils.today())

    /** End date for the rolling 30-day analytics window. */
    private val _monthlyEndDate = MutableStateFlow(DateUtils.today())
    val monthlyEndDate: StateFlow<String> = _monthlyEndDate.asStateFlow()

    val weekDates: StateFlow<List<String>> = _weekEndDate
        .map { DateUtils.rollingWeekDates(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DateUtils.rollingWeekDates()
        )

    /**
     * Set before launching an external camera or gallery activity so that the
     * ON_RESUME triggered on return does not reset the user's past-day selection.
     * Cleared automatically once [activeDayTimestamp] consumes it.
     */
    private var _suppressNextResumeRefresh = false

    /** Call this immediately before launching the camera or gallery picker. */
    fun notifyExternalMediaLaunch() {
        _suppressNextResumeRefresh = true
    }

    /** Snap dashboard back to the real local today (cold start, resume, tab re-show). */
    fun refreshToToday() {
        if (_suppressNextResumeRefresh) return
        val now = todayWithOffset()
        _realToday.value = now
        _weekEndDate.value = now
        _selectedDate.value = now
        _monthlyEndDate.value = now
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

    fun shiftAnalyticsMonth(delta: Int) {
        if (delta == 0) return
        val today = _realToday.value
        val proposed = DateUtils.addDays(_monthlyEndDate.value, delta * 30)
        _monthlyEndDate.value = if (proposed > today) today else proposed
    }

    /** Wall-clock time relocated onto the active (selected) calendar day. */
    fun activeDayTimestamp(): Long {
        _suppressNextResumeRefresh = false
        return DateUtils.timestampOnDate(_selectedDate.value)
    }

    // --- Update check -------------------------------------------------------------------------

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    /** Manual or automatic check; [silent] skips status text for up-to-date / network errors. */
    fun checkForUpdates(currentVersionCode: Int, silent: Boolean = false) {
        // github → GitHub Releases; fdroid → f-droid.org (never mixes channels).
        if (_updateState.value.isChecking) return
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
                    it.copy(
                        isChecking = false,
                        updateInfo = result,
                        statusMessage = null,
                        statusIsError = false,
                        backupCompleted = false,
                        isExportingBackup = false,
                        isAwaitingBackupFilePick = false,
                        pendingDownloadUrlAfterBackup = null,
                        backupStatusMessage = null,
                        backupStatusIsError = false
                    )
                }
                UpdateCheckResult.UpToDate -> _updateState.update {
                    it.copy(
                        isChecking = false,
                        updateInfo = null,
                        statusMessage = if (silent) null else str(R.string.vm_re_latest_version),
                        statusIsError = false,
                        backupCompleted = false,
                        isExportingBackup = false,
                        isAwaitingBackupFilePick = false,
                        pendingDownloadUrlAfterBackup = null,
                        backupStatusMessage = null,
                        backupStatusIsError = false
                    )
                }
                is UpdateCheckResult.Error -> _updateState.update {
                    it.copy(
                        isChecking = false,
                        updateInfo = null,
                        statusMessage = if (silent) null else result.message,
                        statusIsError = !silent,
                        backupCompleted = false,
                        isExportingBackup = false,
                        isAwaitingBackupFilePick = false,
                        pendingDownloadUrlAfterBackup = null,
                        backupStatusMessage = null,
                        backupStatusIsError = false
                    )
                }
            }
        }
    }

    fun dismissUpdatePrompt() {
        // Don't dismiss while SAF picker is open or backup is running.
        val s = _updateState.value
        if (s.isAwaitingBackupFilePick || s.isExportingBackup) return
        _updateState.update {
            it.copy(
                updateInfo = null,
                statusMessage = null,
                statusIsError = false,
                backupCompleted = false,
                isExportingBackup = false,
                isAwaitingBackupFilePick = false,
                pendingDownloadUrlAfterBackup = null,
                backupStatusMessage = null,
                backupStatusIsError = false
            )
        }
    }

    /**
     * Called after opening the APK asset download URL in the system browser.
     * Clears the update prompt; the browser handles the download.
     */
    fun onUpdateDownloadOpened() {
        _updateState.update {
            it.copy(
                updateInfo = null,
                statusMessage = str(R.string.vm_download_started_browser_install_apk_when_finish),
                statusIsError = false,
                backupCompleted = false,
                isExportingBackup = false,
                isAwaitingBackupFilePick = false,
                pendingDownloadUrlAfterBackup = null,
                backupStatusMessage = null,
                backupStatusIsError = false
            )
        }
    }

    fun failOpenUpdateDownload(message: String) {
        _updateState.update {
            it.copy(
                statusMessage = message,
                statusIsError = true,
                pendingDownloadUrlAfterBackup = null,
                backupCompleted = false
            )
        }
    }

    /**
     * Start Export backup & update: remember [downloadUrl], then begin cloud upload or share-sheet
     * export immediately.
     * @return always false (no SAF picker); kept for call-site compatibility.
     */
    fun beginExportBackupAndUpdate(context: Context, downloadUrl: String): Boolean {
        if (_updateState.value.isExportingBackup) return false
        if (_updateState.value.backupCompleted) return false
        _updateState.update {
            it.copy(
                pendingDownloadUrlAfterBackup = downloadUrl,
                backupStatusMessage = null,
                backupStatusIsError = false
            )
        }
        exportBackupForUpdate(context)
        return false
    }

    /**
     * Pre-update backup: cloud upload when enabled, otherwise write + share a local JSON file.
     * On success sets [UpdateUiState.backupCompleted] so the APK URL can open in the browser.
     */
    fun exportBackupForUpdate(context: Context? = null) {
        if (_updateState.value.isExportingBackup) return
        if (_updateState.value.backupCompleted) return
        val useCloud = settings.value.cloudBackupEnabled
        if (!useCloud && context == null) {
            _updateState.update {
                it.copy(
                    isAwaitingBackupFilePick = false,
                    pendingDownloadUrlAfterBackup = null,
                    backupStatusMessage = str(R.string.vm_could_not_open_share_sheet),
                    backupStatusIsError = true
                )
            }
            return
        }
        viewModelScope.launch {
            _updateState.update {
                it.copy(
                    isExportingBackup = true,
                    isAwaitingBackupFilePick = false,
                    backupStatusMessage = null,
                    backupStatusIsError = false
                )
            }
            val result = if (useCloud) {
                runCatching { repository.uploadMongoBackup(force = true).recordCount }
            } else {
                runCatching {
                    val (file, count) = repository.exportDataToShareFile(password = null)
                    BackupShare.shareJsonFile(
                        context!!.applicationContext,
                        file,
                        chooserTitle = str(R.string.vm_share_fitbuddy_backup),
                    )
                    count
                }
            }
            result
                .onSuccess { count ->
                    val fresh = settingsRepository.settings.first().hasFreshSuccessfulBackup()
                    if (!fresh) {
                        _updateState.update {
                            it.copy(
                                isExportingBackup = false,
                                backupCompleted = false,
                                pendingDownloadUrlAfterBackup = null,
                                backupStatusMessage = str(R.string.vm_backup_timestamp_missing_too_old_update_cancelle),
                                backupStatusIsError = true
                            )
                        }
                        return@onSuccess
                    }
                    val message = if (useCloud) {
                        str(R.string.vm_uploaded_records_cloud_backup, count)
                    } else {
                        str(R.string.vm_shared_records, count)
                    }
                    _updateState.update {
                        it.copy(
                            isExportingBackup = false,
                            backupCompleted = true,
                            backupStatusMessage = message,
                            backupStatusIsError = false
                        )
                    }
                }
                .onFailure { e ->
                    val message = if (useCloud) {
                        str(R.string.vm_cloud_upload_failed, BackupErrorMessages.encryptionFailed(e.message))
                    } else {
                        str(R.string.vm_export_failed, e.message ?: "")
                    }
                    _updateState.update {
                        it.copy(
                            isExportingBackup = false,
                            backupCompleted = false,
                            pendingDownloadUrlAfterBackup = null,
                            backupStatusMessage = message,
                            backupStatusIsError = true
                        )
                    }
                }
        }
    }

    /** Developer: open the update prompt with fake release info (no network). */
    fun showTestUpdatePrompt() {
        _updateState.update {
            it.copy(
                isChecking = false,
                updateInfo = UpdateCheckResult.Available(
                    versionName = "99.0.0-test",
                    versionCode = BuildConfig.VERSION_CODE + 999,
                    downloadUrl = "https://example.invalid/fitbuddy-test-update.apk",
                    releaseNotes = str(R.string.vm_test_update_prompt_backup_before_update_ui),
                    htmlUrl = "",
                    channel = if (BuildConfig.IS_FDROID) {
                        UpdateChannel.FDROID
                    } else {
                        UpdateChannel.GITHUB
                    },
                ),
                statusMessage = null,
                statusIsError = false,
                backupCompleted = false,
                isExportingBackup = false,
                isAwaitingBackupFilePick = false,
                pendingDownloadUrlAfterBackup = null,
                backupStatusMessage = null,
                backupStatusIsError = false
            )
        }
    }

    /** F-Droid channel: open F-Droid client(s) / website, then dismiss the prompt. */
    fun openFdroidUpdate(context: Context) {
        val destination = runCatching { FdroidUpdateLauncher.open(context) }
            .getOrElse { e ->
                failOpenUpdateDownload(
                    e.message ?: str(R.string.vm_couldn_t_open_f_droid_visit_f)
                )
                return
            }
        val status = when (destination) {
            "app picker" -> str(R.string.vm_choose_app_update)
            "website" -> str(R.string.vm_opened_update_website)
            else -> str(R.string.vm_opened_update, destination)
        }
        _updateState.update {
            it.copy(
                updateInfo = null,
                statusMessage = status,
                statusIsError = false,
                backupCompleted = false,
                isExportingBackup = false,
                isAwaitingBackupFilePick = false,
                pendingDownloadUrlAfterBackup = null,
                backupStatusMessage = null,
                backupStatusIsError = false
            )
        }
    }

    // --- Donation reminder / thank-you ------------------------------------------------------

    private val donorListRepository = DonorListRepository(settingsRepository)

    private val _donationReminderVisible = MutableStateFlow(false)
    val donationReminderVisible: StateFlow<Boolean> = _donationReminderVisible.asStateFlow()

    private val _alreadyPaidPromptVisible = MutableStateFlow(false)
    val alreadyPaidPromptVisible: StateFlow<Boolean> = _alreadyPaidPromptVisible.asStateFlow()

    private val _openDonationDialogRequested = MutableStateFlow(false)
    val openDonationDialogRequested: StateFlow<Boolean> = _openDonationDialogRequested.asStateFlow()

    private val _newDonorsThankYou = MutableStateFlow<List<DonorEntry>>(emptyList())
    val newDonorsThankYou: StateFlow<List<DonorEntry>> = _newDonorsThankYou.asStateFlow()

    /** Null = gallery closed; empty list = open with no named donors. */
    private val _donorGallery = MutableStateFlow<List<DonorEntry>?>(null)
    val donorGallery: StateFlow<List<DonorEntry>?> = _donorGallery.asStateFlow()

    private val _pendingEveningDonateReminder = MutableStateFlow(false)

    /** Themes unlock only (OG or money badge). Other donor perks use roster membership. */
    private val _isSupporter = MutableStateFlow(false)
    val isSupporter: StateFlow<Boolean> = _isSupporter.asStateFlow()

    private suspend fun refreshSupporterStatus(supportId: String? = null) {
        val id = supportId ?: settingsRepository.settings.first().supportId
        val file = donorListRepository.cachedOrEmpty()
        _isSupporter.value = donorListRepository.unlocksThemes(file, id)
    }

    fun consumeOpenDonationDialogRequest() {
        _openDonationDialogRequested.value = false
    }

    fun onDonateReminderNotificationOpened() {
        val s = settings.value
        val now = System.currentTimeMillis()
        if (DonationReminderScheduler.canShowEveningDialog(s, now)) {
            _donationReminderVisible.value = true
        } else {
            _pendingEveningDonateReminder.value = true
        }
    }

    fun dismissDonationReminderSoft() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val current = settings.value
            settingsRepository.save(
                current.copy(
                    donationLastNudgeAt = now,
                    donationLastDialogAt = now,
                )
            )
            _donationReminderVisible.value = false
            _pendingEveningDonateReminder.value = false
        }
    }

    fun onDonationReminderAlreadyPaid() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val current = settings.value
            settingsRepository.save(
                current.copy(
                    donationLastNudgeAt = now,
                    donationLastDialogAt = now,
                )
            )
            _donationReminderVisible.value = false
            _alreadyPaidPromptVisible.value = true
            _pendingEveningDonateReminder.value = false
        }
    }

    fun dismissAlreadyPaidPrompt() {
        _alreadyPaidPromptVisible.value = false
    }

    fun onDonationReminderPay() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val current = settings.value
            settingsRepository.save(
                current.copy(
                    donationLastNudgeAt = now,
                    donationLastDialogAt = now,
                )
            )
            _donationReminderVisible.value = false
            _pendingEveningDonateReminder.value = false
            _openDonationDialogRequested.value = true
        }
    }

    fun dismissNewDonorsThankYou() {
        val shown = _newDonorsThankYou.value
        _newDonorsThankYou.value = emptyList()
        if (shown.isEmpty()) return
        // Developer demo donors must not pollute last-seen.
        if (shown.any { it.hash.startsWith("demo-hash") }) return
        viewModelScope.launch {
            val file = donorListRepository.cachedOrEmpty()
            DonorBadgeRules.latestDonationDate(file)?.let { latest ->
                settingsRepository.advanceLastSeenDonorDonationDate(latest)
            }
        }
    }

    fun dismissDonorGallery() {
        _donorGallery.value = null
    }

    fun openDonorGallery() {
        viewModelScope.launch {
            val file = runCatching { donorListRepository.refresh(forceNetwork = true) }
                .getOrElse { donorListRepository.cachedOrEmpty() }
            _donorGallery.value = donorListRepository.allDisplayableDonors(file)
        }
    }

    /** Developer: gallery with hard-coded demo supporters (does not touch last-seen). */
    fun showTestDonorGallery() {
        _donorGallery.value = DemoDonors.forGallery
    }

    private val _lastSeenDonorDonationDate = MutableStateFlow<String?>(null)
    val lastSeenDonorDonationDate: StateFlow<String?> = _lastSeenDonorDonationDate.asStateFlow()

    fun refreshLastSeenDonorDonationDate() {
        viewModelScope.launch {
            _lastSeenDonorDonationDate.value = settingsRepository.lastSeenDonorDonationDate()
        }
    }

    /** Developer: clear so every donation after empty floor shows in thank-you. */
    fun clearLastSeenDonorDonationDate() {
        viewModelScope.launch {
            settingsRepository.clearLastSeenDonorDonationDate()
            _lastSeenDonorDonationDate.value = null
            showTransientMessage(str(R.string.vm_donor_last_seen_cleared))
        }
    }

    /** Developer: mark everything currently in the roster as already seen. */
    fun setLastSeenDonorDonationDateToLatest() {
        viewModelScope.launch {
            val file = runCatching { donorListRepository.refresh(forceNetwork = true) }
                .getOrElse { donorListRepository.cachedOrEmpty() }
            val latest = DonorBadgeRules.latestDonationDate(file)
                ?: DonorBadgeRules.latestDonationDate(
                    com.anant.fitbuddy.data.donors.DonorsFile(donors = DemoDonors.all),
                )
            if (latest == null) {
                showTransientMessage(str(R.string.vm_no_donation_dates_found))
                return@launch
            }
            settingsRepository.setLastSeenDonorDonationDate(latest)
            _lastSeenDonorDonationDate.value = latest
            showTransientMessage(str(R.string.vm_donor_last_seen_set, latest))
        }
    }

    /** Developer: set an explicit ISO date (`yyyy-MM-dd`). */
    fun setLastSeenDonorDonationDate(isoDate: String) {
        viewModelScope.launch {
            val normalized = isoDate.trim()
            if (normalized.isEmpty()) {
                settingsRepository.clearLastSeenDonorDonationDate()
                _lastSeenDonorDonationDate.value = null
                showTransientMessage(str(R.string.vm_donor_last_seen_cleared))
                return@launch
            }
            settingsRepository.setLastSeenDonorDonationDate(normalized)
            _lastSeenDonorDonationDate.value = normalized
            showTransientMessage(str(R.string.vm_donor_last_seen_set, normalized))
        }
    }

    fun showTestDonationReminderDialog() {
        _donationReminderVisible.value = true
    }

    fun showTestNewDonorsThankYou() {
        _newDonorsThankYou.value = DemoDonors.forThankYou
    }

    fun showTestDonationDialog() {
        _openDonationDialogRequested.value = true
    }

    fun resetDonationReminderTimers() {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.save(
                current.copy(
                    donationLastNudgeAt = DonationReminderScheduler.seedFirstRunNudgeAt(),
                    donationLastNotifAt = 0L,
                    donationLastDialogAt = 0L,
                    donationReminderEnabled = true,
                )
            )
            showTransientMessage(str(R.string.vm_donate_reminder_timers_reset))
        }
    }

    /**
     * Fetches donors, toggles personal reminder after upgrades, and queues public thank-you
     * for named donors with donations after the last-seen donation date.
     */
    fun syncDonors() {
        viewModelScope.launch {
            runCatching {
                // Always read DataStore — never settings.value (StateFlow defaults to empty
                // AppSettings and a full save() would wipe AI keys on upgrade).
                var current = settingsRepository.settings.first()
                if (current.donationLastNudgeAt <= 0L) {
                    settingsRepository.setDonationLastNudgeAt(
                        DonationReminderScheduler.seedFirstRunNudgeAt()
                    )
                    current = settingsRepository.settings.first()
                }

                val version = BuildConfig.VERSION_CODE
                val lastReminderVersion = settingsRepository.lastDonationReminderVersionCode()
                if (lastReminderVersion != null && version > lastReminderVersion) {
                    settingsRepository.setDonationReminderEnabled(true)
                    current = settingsRepository.settings.first()
                }
                if (lastReminderVersion == null || version != lastReminderVersion) {
                    settingsRepository.setLastDonationReminderVersionCode(version)
                }

                val file = donorListRepository.refresh(forceNetwork = true)
                val isDonor = donorListRepository.containsSupportId(file, current.supportId)
                _isSupporter.value = donorListRepository.unlocksThemes(file, current.supportId)
                // No Settings toggle — always on for everyone except known donors.
                if (isDonor && current.donationReminderEnabled) {
                    settingsRepository.setDonationReminderEnabled(false)
                    current = settingsRepository.settings.first()
                } else if (!isDonor && !current.donationReminderEnabled) {
                    settingsRepository.setDonationReminderEnabled(true)
                    current = settingsRepository.settings.first()
                }

                runCatching { repository.ensureSignupDateFromLogs() }
                current = settingsRepository.settings.first()
                val lastSeenDate = settingsRepository.lastSeenDonorDonationDate()
                val signupDate = current.signupDate.ifBlank { null }
                // First thank-you waits a few days after install; don't advance last-seen
                // early or newcomers would be skipped when the delay finally elapses.
                val pastInstallDelay = lastSeenDate != null ||
                    DonorListRepository.isPastThankYouInstallDelay(signupDate)
                if (pastInstallDelay) {
                    val newcomers = donorListRepository.newDisplayableDonors(
                        file = file,
                        lastSeenDonationDate = lastSeenDate,
                        signupDate = signupDate,
                    )
                    if (newcomers.isNotEmpty()) {
                        _newDonorsThankYou.value = newcomers
                    } else {
                        DonorBadgeRules.latestDonationDate(file)?.let { latest ->
                            settingsRepository.advanceLastSeenDonorDonationDate(latest)
                        }
                    }
                }

                maybeShowEveningDonateReminder()
            }
        }
    }

    fun maybeShowEveningDonateReminder() {
        val s = settings.value
        if (_pendingEveningDonateReminder.value) {
            val now = System.currentTimeMillis()
            val hour = java.util.Calendar.getInstance().apply { timeInMillis = now }
                .get(java.util.Calendar.HOUR_OF_DAY)
            val gapOk = s.donationLastNotifAt <= 0L ||
                now - s.donationLastNotifAt >= DonationReminderScheduler.MIN_GAP_MS
            if (hour >= DonationReminderScheduler.DIALOG_HOUR && gapOk) {
                _donationReminderVisible.value = true
                _pendingEveningDonateReminder.value = false
                return
            }
        }
        if (DonationReminderScheduler.canShowEveningDialog(s)) {
            _donationReminderVisible.value = true
        }
    }

    // --- Settings ---------------------------------------------------------------------------

    private val _hasSettingsSnapshot = MutableStateFlow(false)
    val hasSettingsSnapshot: StateFlow<Boolean> = _hasSettingsSnapshot.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .onEach { _hasSettingsSnapshot.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    init {
        // Re-snap "today" whenever the day-change hour is changed in Settings.
        settingsRepository.settings
            .map { it.dayChangeHour }
            .distinctUntilChanged()
            .onEach { refreshToToday() }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            // Wait for first settings emission so Support ID / nudge seed see real prefs.
            settingsRepository.settings.first()
            refreshSupporterStatus()
            syncDonors()
        }

        // Quiet foreground pull when HC + auto-sync are on (hourly debounce inside sync).
        settingsRepository.settings
            .map { it.healthConnectEnabled && it.healthConnectAutoSync }
            .distinctUntilChanged()
            .onEach { auto ->
                if (auto) {
                    // syncNow reconciles HC permissions before the data pull (and before debounce).
                    runCatching { repository.syncHealthConnect(force = false) }
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val profile = repository.activeProfile.mapNotNull { it }.first()
            val legacyCalculatedTargets = profile.goalRationale
                ?.startsWith("Evidence-based v") == true &&
                profile.goalRationale.startsWith("Evidence-based v3:").not()
            if (!legacyCalculatedTargets || !profile.hasBasicsConfigured()) return@launch

            val plan = runCatching {
                HealthTargetCalculator.calculate(
                    HealthTargetInput(
                        age = profile.age,
                        heightCm = profile.heightCm,
                        weightKg = profile.weightKg,
                        sex = profile.sex,
                        activityLevel = profile.activityLevel,
                        statedGoal = profile.goal,
                        currentCalories = 0,
                        currentProteinG = 0,
                        currentCarbsG = 0,
                        currentFatsG = 0,
                        currentTargetsCalculated = false
                    )
                )
            }.getOrNull() ?: return@launch

            repository.saveProfileRecordingTargets(
                profile = profile.copy(
                    dailyTargetCalories = plan.dailyTargetCalories,
                    targetProteinG = plan.targetProteinG,
                    targetCarbsG = plan.targetCarbsG,
                    targetFatsG = plan.targetFatsG,
                    goal = plan.recommendedGoal,
                    goalRationale = plan.rationale,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                ),
                effectiveFromDate = DateUtils.today(),
                recordNutritionTargets = true
            )
        }
    }

    /** Reactive: true when the preferred provider is configured for live AI calls. */
    val isAiOnline: StateFlow<Boolean> = settingsRepository.settings
        .map { it.isConfigured }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun saveSettings(newSettings: AppSettings, announce: Boolean = false) {
        viewModelScope.launch {
            settingsRepository.save(newSettings)
            if (announce) {
                _analysisState.update { it.copy(userMessage = str(R.string.vm_settings_saved)) }
            }
        }
    }

    fun setDeveloperModeUnlocked(unlocked: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            if (current.developerModeUnlocked != unlocked) {
                settingsRepository.save(current.copy(developerModeUnlocked = unlocked))
            }
        }
    }

    fun clearModelCooldowns() {
        viewModelScope.launch {
            settingsRepository.clearModelCooldowns()
            _analysisState.update { it.copy(userMessage = str(R.string.vm_model_cooldowns_cleared)) }
        }
    }

    /** Developer: reset OpenRouter / Gemini / Ollama preferred models to built-in defaults. */
    fun applyBuiltInModelDefaults() {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.save(
                current.copy(
                    openRouterModel = AppSettings.DEFAULT_OPENROUTER_MODEL,
                    openRouterTextModel = AppSettings.DEFAULT_OPENROUTER_TEXT_MODEL,
                    geminiModel = AppSettings.DEFAULT_GEMINI_MODEL,
                    geminiTextModel = AppSettings.DEFAULT_GEMINI_TEXT_MODEL,
                    ollamaModel = AppSettings.DEFAULT_OLLAMA_MODEL,
                    ollamaTextModel = AppSettings.DEFAULT_OLLAMA_TEXT_MODEL
                )
            )
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_built_model_defaults_applied))
            }
        }
    }

    /** Settings region picker: updates the diet/region pack only (no other fields touched). */
    fun setRegion(region: AppRegion) {
        viewModelScope.launch {
            val current = settings.value
            if (current.region != region.name) {
                settingsRepository.save(current.copy(region = region.name))
            }
        }
    }

    /** Records a successful custom-region Sentry send (one allowed per install / supportId). */
    fun markRegionRequestSent() {
        if (_forceFullOnboarding.value) return
        viewModelScope.launch {
            val current = settings.value
            if (current.regionRequestSentAt > 0L) return@launch
            settingsRepository.save(
                current.copy(regionRequestSentAt = System.currentTimeMillis())
            )
        }
    }

    /**
     * Developer: show the full onboarding UI as an in-memory preview.
     * Does not clear region, crash reporting, profile, AI keys, or logs. Completing the flow
     * (or killing the process) returns to the dashboard with data unchanged — the flag is not
     * persisted, so a cold start always lands on home.
     */
    fun restartOnboardingForTesting() {
        _forceAiSetup.value = false
        _forceFullOnboarding.value = true
        _analysisState.update {
            it.copy(userMessage = str(R.string.vm_onboarding_preview_no_data_saved))
        }
    }

    private fun finishOnboardingPreview(message: String = str(R.string.vm_onboarding_preview_closed_no_data_changed)) {
        _forceFullOnboarding.value = false
        _forceAiSetup.value = false
        _analysisState.update { it.copy(userMessage = message) }
    }

    /**
     * Finishes the standalone region-selection page. Optionally updates crash reporting when
     * the preceding opt-in page (F-Droid / debug) collected a choice.
     */
    fun completeRegionSelection(
        region: AppRegion,
        crashReportingEnabled: Boolean? = null,
        cloudBackupEnabled: Boolean? = null
    ) {
        if (_regionSelectionSaving.value) return
        _regionSelectionSaving.value = true
        viewModelScope.launch {
            val current = settings.value
            val cloudOn = (cloudBackupEnabled ?: current.cloudBackupEnabled) && !BuildConfig.DEBUG
            val reporting = (crashReportingEnabled ?: current.crashReportingEnabled) || cloudOn
            if (cloudOn) {
                settingsRepository.ensureSupportId()
                // Onboarding/region path: Support ID default — no custom password prompt.
                settingsRepository.setCloudBackupPasswordBlob(null)
            }
            settingsRepository.save(
                current.copy(
                    region = region.name,
                    crashReportingEnabled = reporting,
                    cloudBackupEnabled = cloudOn,
                    cloudAutoUploadEnabled = if (cloudOn) true else current.cloudAutoUploadEnabled,
                    cloudBackupPasswordSet = if (cloudOn) false else current.cloudBackupPasswordSet
                )
            )
            CrashReporter.setReportingEnabled(reporting)
            _regionSelectionSaving.value = false
        }
    }

    /**
     * Toggles crash reporting. Turning it off while cloud backup is on also disables cloud
     * backup (caller should warn the user first). Turning crash reports on never enables cloud.
     */
    fun setCrashReportingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            val disableCloud = !enabled && current.cloudBackupEnabled
            settingsRepository.save(
                current.copy(
                    crashReportingEnabled = enabled,
                    cloudBackupEnabled = if (disableCloud) false else current.cloudBackupEnabled,
                    cloudAutoUploadEnabled = if (disableCloud) false else current.cloudAutoUploadEnabled
                )
            )
            CrashReporter.setReportingEnabled(enabled)
            if (enabled) {
                val today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
                if (settingsRepository.lastHeartbeatUtcDay() != today) {
                    sendHeartbeatInternal()
                }
            }
            _analysisState.update {
                it.copy(
                    userMessage = when {
                        enabled -> str(R.string.vm_crash_reports_enabled)
                        disableCloud -> str(R.string.vm_crash_reports_cloud_backup_disabled)
                        else -> str(R.string.vm_crash_reports_disabled)
                    }
                )
            }
        }
    }

    private val _healthConnectSyncBusy = MutableStateFlow(false)
    val healthConnectSyncBusy: StateFlow<Boolean> = _healthConnectSyncBusy.asStateFlow()

    fun setHealthConnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(healthConnectEnabled = enabled) }
            if (enabled) {
                val auto = settingsRepository.settings.first().healthConnectAutoSync
                if (auto) syncHealthConnect(announce = true)
            } else {
                _analysisState.update {
                    it.copy(userMessage = str(R.string.vm_health_connect_disabled))
                }
            }
        }
    }

    fun setHealthConnectAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(healthConnectAutoSync = enabled) }
            if (enabled && settingsRepository.settings.first().healthConnectEnabled) {
                syncHealthConnect(announce = false)
            }
        }
    }

    fun updateHealthConnectMetrics(metrics: com.anant.fitbuddy.data.healthconnect.HealthConnectMetricPrefs) {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(
                    healthConnectMetrics = metrics.forSync(it.waterTrackingEnabled)
                )
            }
        }
    }

    fun syncHealthConnect(announce: Boolean = true) {
        viewModelScope.launch {
            if (_healthConnectSyncBusy.value) return@launch
            _healthConnectSyncBusy.value = true
            val result = runCatching { repository.syncHealthConnect(force = true) }
                .getOrElse {
                    com.anant.fitbuddy.data.healthconnect.HealthConnectSyncResult(
                        error = it.message ?: str(R.string.vm_health_connect_sync_failed)
                    )
                }
            _healthConnectSyncBusy.value = false
            val revoked = result.error ==
                com.anant.fitbuddy.data.healthconnect.HealthConnectSync.ERROR_PERMISSIONS_REVOKED
            if (revoked) {
                settingsRepository.update {
                    it.copy(healthConnectEnabled = false)
                }
            }
            if (!announce) return@launch
            _analysisState.update {
                it.copy(
                    userMessage = when {
                        revoked -> str(R.string.vm_health_connect_permissions_revoked)
                        !result.error.isNullOrBlank() -> result.error
                        else -> str(R.string.vm_health_connect_synced)
                    }
                )
            }
        }
    }

    fun setDiagnosticLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.save(current.copy(diagnosticLoggingEnabled = enabled))
            DiagnosticLogger.setEnabled(enabled, current, restartSession = enabled)
            _analysisState.update {
                it.copy(
                    userMessage = if (enabled) {
                        str(R.string.vm_diagnostic_logging_reproduce_issue_then_stop_exp)
                    } else {
                        str(R.string.vm_diagnostic_logging_off)
                    }
                )
            }
        }
    }

    /** Stops recording (keeps the buffer) and opens the share sheet for the log file. */
    fun stopDiagnosticLoggingAndExport(context: Context): Boolean {
        DiagnosticLogger.setEnabled(false, restartSession = false)
        viewModelScope.launch {
            val current = settings.value
            if (current.diagnosticLoggingEnabled) {
                settingsRepository.save(current.copy(diagnosticLoggingEnabled = false))
            }
            _analysisState.update { it.copy(userMessage = str(R.string.vm_diagnostic_logging_stopped)) }
        }
        val file = DiagnosticLogger.exportToShareFile(context) ?: return false
        return runCatching {
            BackupShare.shareTextFile(context, file, chooserTitle = str(R.string.vm_share_diagnostic_log))
            true
        }.getOrDefault(false)
    }


    /**
     * Easter egg: Settings “crafted with ♥” double-tap. Sends a Sentry heartbeat when
     * crash reporting is on; ignores the once-per-day gate so the tap pulses.
     */
    fun sendHeartbeatFromLoveTap() {
        viewModelScope.launch {
            sendHeartbeatInternal(force = true)
        }
    }

    private suspend fun sendHeartbeatInternal(force: Boolean = false) {
        val s = settings.value
        val recordCount = runCatching { repository.getTotalRecordCount() }.getOrDefault(0)
        val info = HeartbeatInfo(
            aiProvider = s.provider.name,
            recordCount = recordCount,
            isDeveloper = s.developerModeUnlocked,
            signupDate = s.signupDate
        )
        val today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
        val kind = if (force) HeartbeatKind.CONFETTI else HeartbeatKind.DAILY
        val sent = withContext(Dispatchers.IO) {
            CrashReporter.sendHeartbeat(info, kind)
        }
        if (sent) {
            settingsRepository.markHeartbeatSent(today)
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
            settingsRepository.update { it.copy(easterEggDiscovered = false) }
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
     * Results are cached per provider+key+url+paid; a change invalidates the cache. No-op re-entry
     * while already loading. When [force] is true (Refresh button) and paid models are off, keeps
     * only models whose chat endpoint returns HTTP 200 or 429. Paid-models mode never probes.
     */
    fun loadFreeVisionModels(
        provider: AiProvider,
        apiKey: String,
        force: Boolean = false,
        baseUrl: String = "",
        includePaid: Boolean = settings.value.showPaidFor(provider)
    ) {
        val cacheKey = "${provider.name}:$apiKey:$baseUrl:$includePaid"
        if (_models.value.isLoading) return
        if (!force && _models.value.loaded && lastModelsCacheKey == cacheKey) return
        _models.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val catalog = when (provider) {
                    AiProvider.OPENROUTER -> repository.fetchFreeVisionModels(apiKey, includePaid)
                    AiProvider.GEMINI -> repository.fetchGeminiVisionModels(apiKey, includePaid)
                    AiProvider.OLLAMA -> {
                        val url = baseUrl.trim().trimEnd('/')
                        when {
                            url.isBlank() -> emptyList()
                            url == AppSettings.OLLAMA_CLOUD_BASE_URL && apiKey.isBlank() -> emptyList()
                            else -> repository.fetchOllamaVisionModels(url, apiKey)
                        }
                    }
                    AiProvider.CUSTOM -> {
                        val url = baseUrl.trim().trimEnd('/')
                        if (url.isBlank()) emptyList()
                        else repository.fetchOllamaVisionModels(
                            url, apiKey, ladderProvider = AiProvider.CUSTOM
                        )
                    }
                    AiProvider.OPENAI -> repository.fetchOpenAiVisionModels(apiKey)
                }
                // Skip reachability probes when paid models are listed (never ping paid endpoints).
                if (force && !includePaid) {
                    filterReachableIfPossible(catalog, provider, apiKey, baseUrl)
                } else {
                    catalog
                }
            }
                .onSuccess { list ->
                    lastModelsCacheKey = cacheKey
                    _models.update {
                        it.copy(isLoading = false, options = list, loaded = true, error = null)
                    }
                    reconcilePreferredModelAfterCatalog(provider, list, forPhoto = true)
                }
                .onFailure { e ->
                    _models.update {
                        it.copy(isLoading = false, error = e.message ?: str(R.string.vm_could_not_load_models))
                    }
                }
        }
    }

    /**
     * Loads the free (text) model list for [provider] used by the text-query model dropdown.
     * Cached per provider+key+url+paid like [loadFreeVisionModels]. Refresh probes only when
     * paid models are off.
     */
    fun loadFreeTextModels(
        provider: AiProvider,
        apiKey: String,
        force: Boolean = false,
        baseUrl: String = "",
        includePaid: Boolean = settings.value.showPaidFor(provider)
    ) {
        val cacheKey = "${provider.name}:$apiKey:$baseUrl:$includePaid"
        if (_textModels.value.isLoading) return
        if (!force && _textModels.value.loaded && lastTextModelsCacheKey == cacheKey) return
        _textModels.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val catalog = when (provider) {
                    AiProvider.OPENROUTER -> repository.fetchFreeModels(apiKey, includePaid)
                    AiProvider.GEMINI -> repository.fetchGeminiTextModels(apiKey, includePaid)
                    AiProvider.OLLAMA -> {
                        val url = baseUrl.trim().trimEnd('/')
                        when {
                            url.isBlank() -> emptyList()
                            url == AppSettings.OLLAMA_CLOUD_BASE_URL && apiKey.isBlank() -> emptyList()
                            else -> repository.fetchOllamaTextModels(url, apiKey)
                        }
                    }
                    AiProvider.CUSTOM -> {
                        val url = baseUrl.trim().trimEnd('/')
                        if (url.isBlank()) emptyList()
                        else repository.fetchOllamaTextModels(
                            url, apiKey, ladderProvider = AiProvider.CUSTOM
                        )
                    }
                    AiProvider.OPENAI -> repository.fetchOpenAiTextModels(apiKey)
                }
                if (force && !includePaid) {
                    filterReachableIfPossible(catalog, provider, apiKey, baseUrl)
                } else {
                    catalog
                }
            }
                .onSuccess { list ->
                    lastTextModelsCacheKey = cacheKey
                    _textModels.update {
                        it.copy(isLoading = false, options = list, loaded = true, error = null)
                    }
                    reconcilePreferredModelAfterCatalog(provider, list, forPhoto = false)
                }
                .onFailure { e ->
                    _textModels.update {
                        it.copy(isLoading = false, error = e.message ?: str(R.string.vm_could_not_load_models))
                    }
                }
        }
    }

    /**
     * If the preferred photo/text model for [provider] is missing from a freshly loaded
     * catalog, switch to the next [FailoverLadders] pick and show a short pill.
     * OpenAI is left alone (curated catalog). No-op when the list is empty.
     */
    private suspend fun reconcilePreferredModelAfterCatalog(
        provider: AiProvider,
        options: List<ModelOption>,
        forPhoto: Boolean
    ) {
        if (provider == AiProvider.OPENAI || options.isEmpty()) return
        val current = settings.value
        val selected = when (provider) {
            AiProvider.OPENROUTER -> if (forPhoto) {
                current.openRouterModel
            } else {
                current.openRouterTextModel.ifBlank { current.openRouterModel }
            }
            AiProvider.GEMINI -> if (forPhoto) {
                current.geminiModel
            } else {
                current.geminiTextModel.ifBlank { current.geminiModel }
            }
            AiProvider.OLLAMA -> if (forPhoto) {
                current.ollamaModel
            } else {
                current.ollamaTextModel.ifBlank { current.ollamaModel }
            }
            AiProvider.CUSTOM -> if (forPhoto) {
                current.customModel
            } else {
                current.customTextModel.ifBlank { current.customModel }
            }
            AiProvider.OPENAI -> return
        }
        if (selected.isBlank() || options.any { it.id == selected }) return
        val modality = if (forPhoto) ModelCatalogModality.PHOTO else ModelCatalogModality.TEXT
        val next = FailoverLadders.nextBest(
            provider,
            modality,
            options.map { it.id },
            selected
        ) ?: return
        val updated = when (provider) {
            AiProvider.OPENROUTER -> if (forPhoto) {
                current.copy(openRouterModel = next)
            } else if (current.openRouterTextModel.isNotBlank()) {
                current.copy(openRouterTextModel = next)
            } else {
                current.copy(openRouterModel = next)
            }
            AiProvider.GEMINI -> if (forPhoto) {
                current.copy(geminiModel = next)
            } else if (current.geminiTextModel.isNotBlank()) {
                current.copy(geminiTextModel = next)
            } else {
                current.copy(geminiModel = next)
            }
            AiProvider.OLLAMA -> if (forPhoto) {
                current.copy(ollamaModel = next)
            } else if (current.ollamaTextModel.isNotBlank()) {
                current.copy(ollamaTextModel = next)
            } else {
                current.copy(ollamaModel = next)
            }
            AiProvider.CUSTOM -> if (forPhoto) {
                current.copy(customModel = next)
            } else if (current.customTextModel.isNotBlank()) {
                current.copy(customTextModel = next)
            } else {
                current.copy(customModel = next)
            }
            AiProvider.OPENAI -> return
        }
        settingsRepository.save(updated)
        val label = if (forPhoto) str(R.string.log_hub_photo) else str(R.string.vm_text)
        _analysisState.update {
            it.copy(userMessage = str(R.string.vm_model_unavailable_switched, label, next))
        }
    }

    /**
     * Probes chat for each catalog entry when credentials (or local Ollama) allow it.
     * Without a key, returns [catalog] unchanged — probes would all 401.
     */
    private suspend fun filterReachableIfPossible(
        catalog: List<ModelOption>,
        provider: AiProvider,
        apiKey: String,
        baseUrl: String
    ): List<ModelOption> {
        // OpenAI has no free tier — always shown as paid, so callers never reach here for it
        // (force && !includePaid is never true), but never probe it regardless: billed calls.
        if (provider == AiProvider.OPENAI) return catalog
        val trimmedUrl = baseUrl.trim().trimEnd('/')
        val keylessLocal = (
            provider == AiProvider.OLLAMA &&
                trimmedUrl != AppSettings.OLLAMA_CLOUD_BASE_URL
            ) || (provider == AiProvider.CUSTOM && trimmedUrl.isNotBlank())
        if (apiKey.isBlank() && !keylessLocal) return catalog
        val chatUrl = when (provider) {
            AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AiProvider.GEMINI ->
                "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
            AiProvider.OLLAMA -> baseUrl.trim().trimEnd('/') + "/v1/chat/completions"
            AiProvider.CUSTOM -> baseUrl.trim().trimEnd('/') + "/v1/chat/completions"
            AiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
        }
        val auth = apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        return repository.filterReachableModels(catalog, chatUrl, auth)
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
            val targets = DashboardUiState.targetsFrom(profile, date)
            DashboardUiState(
                profile = profile,
                targetCalories = targets.calories,
                targetProtein = targets.protein,
                targetCarbs = targets.carbs,
                targetFats = targets.fats,
                consumedCalories = snap.consumedCalories,
                burnedCalories = snap.burnedCalories,
                consumedProtein = snap.consumedProtein,
                consumedCarbs = snap.consumedCarbs,
                consumedFats = snap.consumedFats
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** Today's water logs (empty when water tracking is off). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayWaterLogs: StateFlow<List<WaterLog>> =
        combine(settings, _realToday) { appSettings, today ->
            appSettings.waterTrackingEnabled to today
        }.flatMapLatest { (enabled, today) ->
            if (!enabled) flowOf(emptyList())
            else repository.getWaterLogsForDate(today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Today's total water ml (0 when tracking is off). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayWaterMl: StateFlow<Int> =
        combine(settings, _realToday) { appSettings, today ->
            appSettings.waterTrackingEnabled to today
        }.flatMapLatest { (enabled, today) ->
            if (!enabled) flowOf(0)
            else repository.getWaterTotalMlForDate(today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * null while profile hasn't loaded; true when onboarding (full or AI-only) is required.
     * Combines profile basics with AI setup / post-restore force-AI flag.
     */
    private val _forceAiSetup = MutableStateFlow(false)

    /** Developer: re-show the full onboarding path (path picker → … → region) without wiping logs. */
    private val _forceFullOnboarding = MutableStateFlow(false)

    /** True while Developer → Restart onboarding is showing a throwaway walkthrough. */
    val onboardingPreviewMode: StateFlow<Boolean> = _forceFullOnboarding.asStateFlow()

    val needsOnboarding: StateFlow<Boolean?> =
        combine(
            repository.activeProfile,
            settings,
            _forceAiSetup,
            _forceFullOnboarding,
            hasSettingsSnapshot,
        ) { profile, appSettings, forceAi, forceFull, hasSnapshot ->
            // Wait for real DataStore prefs — settings StateFlow starts as empty AppSettings.
            if (!hasSnapshot) return@combine null
            when {
                forceFull -> true
                // Room may emit null before first read settles; treat missing/incomplete as onboard.
                profile == null || !profile.hasBasicsConfigured() -> true
                forceAi || !appSettings.isConfigured -> true
                else -> false
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** When true, onboarding shows only the AI credentials step (profile already restored). */
    val onboardingAiOnly: StateFlow<Boolean> =
        combine(
            repository.activeProfile,
            settings,
            _forceAiSetup,
            _forceFullOnboarding,
            hasSettingsSnapshot,
        ) { profile, appSettings, forceAi, forceFull, hasSnapshot ->
            if (!hasSnapshot || forceFull) return@combine false
            val basicsOk = profile != null && profile.hasBasicsConfigured()
            basicsOk && (forceAi || !appSettings.isConfigured)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * True once onboarding is fully done (profile + AI configured) but no region has been
     * chosen yet — gates a standalone [com.anant.fitbuddy.ui.screens.RegionSelectionScreen]
     * page before the dashboard. False while [needsOnboarding] is still loading (null) or true.
     */
    val needsRegionSelection: StateFlow<Boolean> =
        combine(needsOnboarding, settings) { needsOnboard, appSettings ->
            needsOnboard == false && appSettings.region.isBlank()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _regionSelectionSaving = MutableStateFlow(false)
    val regionSelectionSaving: StateFlow<Boolean> = _regionSelectionSaving.asStateFlow()

    private val _onboardingSaving = MutableStateFlow(false)
    val onboardingSaving: StateFlow<Boolean> = _onboardingSaving.asStateFlow()

    private val _onboardingValidating = MutableStateFlow(false)
    val onboardingValidating: StateFlow<Boolean> = _onboardingValidating.asStateFlow()

    private val _onboardingRestoring = MutableStateFlow(false)
    val onboardingRestoring: StateFlow<Boolean> = _onboardingRestoring.asStateFlow()

    private val _openRouterOAuthBusy = MutableStateFlow(false)
    val openRouterOAuthBusy: StateFlow<Boolean> = _openRouterOAuthBusy.asStateFlow()

    private var pendingOpenRouterCodeVerifier: String? = null
    private var openRouterOAuthCallbackServer: OpenRouterOAuthCallbackServer? = null

    /** When true, dashboard launch should run a one-shot AI calorie/macro target design. */
    private var pendingInitialTargetDesign = false

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
                it.copy(userMessage = str(R.string.vm_logged_kcal, preset.name, preset.calories))
            }
        }
    }

    fun logSavedFood(food: SavedFood) {
        viewModelScope.launch {
            repository.logSavedFood(food, activeDayTimestamp())
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_logged_kcal, food.name, food.calories))
            }
        }
    }

    fun deleteMealPreset(preset: MealPreset) {
        viewModelScope.launch {
            repository.deleteMealPreset(preset)
            _analysisState.update { it.copy(userMessage = str(R.string.vm_deleted_meal_preset, preset.name)) }
        }
    }

    fun deleteSavedFood(food: SavedFood) {
        viewModelScope.launch {
            repository.deleteSavedFood(food)
            _analysisState.update { it.copy(userMessage = str(R.string.vm_deleted_saved_food, food.name)) }
        }
    }

    fun updateSavedFood(food: SavedFood) {
        viewModelScope.launch {
            runCatching { repository.updateSavedFood(food) }
                .onSuccess {
                    _analysisState.update { it.copy(userMessage = str(R.string.main_updated, food.name)) }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = e.message ?: str(R.string.vm_couldn_t_update_food))
                    }
                }
        }
    }

    fun updateMealPreset(existing: MealPreset, draft: MealDraft) {
        viewModelScope.launch {
            runCatching { repository.updateMealPreset(existing, draft) }
                .onSuccess {
                    _analysisState.update { it.copy(userMessage = str(R.string.main_updated, draft.name)) }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = e.message ?: str(R.string.vm_couldn_t_update_meal_preset))
                    }
                }
        }
    }

    /** Marks a saved food as just used (meal builder pick) so it sorts to the top. */
    fun touchSavedFood(food: SavedFood) {
        viewModelScope.launch {
            repository.touchSavedFood(food)
        }
    }

    private val _barcodeLookupLoading = MutableStateFlow(false)
    val barcodeLookupLoading: StateFlow<Boolean> = _barcodeLookupLoading.asStateFlow()

    fun lookupBarcode(
        barcode: String,
        onSuccess: (ScannedProduct) -> Unit,
        onProductUnavailable: (String) -> Unit
    ) {
        if (_barcodeLookupLoading.value) return
        _barcodeLookupLoading.value = true
        CrashReporter.breadcrumb("barcode", "lookup")
        viewModelScope.launch {
            try {
                onSuccess(repository.lookupProductByBarcode(barcode))
            } catch (e: OpenFoodFactsProductUnavailableException) {
                onProductUnavailable(e.barcode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _analysisState.update {
                    it.copy(
                        errorDialogTitle = str(R.string.vm_couldn_t_look_up_product),
                        errorDialogMessage = e.message ?: str(R.string.vm_check_connection_try_again)
                    )
                }
            } finally {
                _barcodeLookupLoading.value = false
            }
        }
    }

    fun saveScannedProductAsSavedFood(product: ScannedProduct) {
        viewModelScope.launch {
            runCatching { repository.saveScannedProductAsSavedFood(product) }
                .onSuccess {
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_saved_food_library, product.name))
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = e.message ?: str(R.string.vm_couldn_t_save_food))
                    }
                }
        }
    }

    /** Logs a barcode product to the active day without saving it to the library. */
    fun logScannedProduct(product: ScannedProduct) {
        viewModelScope.launch {
            runCatching {
                repository.logScannedProduct(product, activeDayTimestamp())
            }
                .onSuccess {
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_logged_kcal, product.name, product.calories))
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = e.message ?: str(R.string.vm_couldn_t_log_food))
                    }
                }
        }
    }

    fun openMealReview(draft: MealDraft) {
        _analysisState.update {
            it.copy(
                mealDraft = draft.snapshot(),
                foodDraft = null,
                editingMealFoodIndex = null
            )
        }
    }

    fun dismissMealDraft() {
        _analysisState.update {
            it.copy(
                mealDraft = null,
                foodDraft = null,
                editingFoodLogId = null,
                editingMealFoodIndex = null
            )
        }
    }

    fun dismissFoodDraft() {
        _analysisState.update {
            it.copy(
                foodDraft = null,
                editingMealFoodIndex = null,
                // Clear edit id only when we were editing a single-food log (no meal draft open).
                editingFoodLogId = if (it.mealDraft == null) null else it.editingFoodLogId
            )
        }
    }

    /** Opens Food review nested inside meal review for the food at [index]. */
    fun editMealDraftFood(index: Int) {
        _analysisState.update { state ->
            val meal = state.mealDraft ?: return@update state
            val food = meal.foods.getOrNull(index) ?: return@update state
            state.copy(
                foodDraft = food.toFoodDraft(timestamp = meal.timestamp, iconTopic = meal.iconTopic),
                editingMealFoodIndex = index,
                reviewMessage = null
            )
        }
    }

    fun setMealDraftName(name: String) {
        _analysisState.update { state ->
            val meal = state.mealDraft ?: return@update state
            state.copy(mealDraft = meal.copy(name = name))
        }
    }

    fun setMealDraftTimestamp(timestamp: Long) {
        _analysisState.update { state ->
            val meal = state.mealDraft ?: return@update state
            state.copy(mealDraft = meal.copy(timestamp = timestamp))
        }
    }

    fun setMealDraftFoodQuantity(index: Int, qty: Double) {
        _analysisState.update { state ->
            val meal = state.mealDraft ?: return@update state
            if (index !in meal.foods.indices) return@update state
            val updated = meal.foods[index].withReviewQuantity(qty)
            state.copy(
                mealDraft = meal.copy(
                    foods = meal.foods.toMutableList().also { it[index] = updated }
                )
            )
        }
    }

    /** Appends a food to the open meal review (e.g. Add food). */
    fun addMealDraftFood(food: FoodEntryDraft) {
        _analysisState.update { state ->
            val meal = state.mealDraft ?: return@update state
            state.copy(mealDraft = meal.copy(foods = meal.foods + food.snapshot()))
        }
    }

    // --- Exercise presets (workout picker) --------------------------------------------------

    val exercisePickerExercises: StateFlow<List<CatalogExercise>> =
        repository.exercisePickerExercises
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), COMMON_EXERCISES_SEED)

    val exerciseUsages: StateFlow<List<com.anant.fitbuddy.data.database.ExerciseUsage>> =
        repository.exerciseUsages
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exerciseCatalogBodyParts: StateFlow<List<String>> =
        repository.exerciseCatalogBodyParts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exerciseCatalogEquipments: StateFlow<List<String>> =
        repository.exerciseCatalogEquipments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exerciseCatalogLoading: StateFlow<Boolean> =
        repository.exerciseCatalogLoading
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Long-pressed workout filter chips (front of row); empty = default order only. */
    val pinnedWorkoutBodyPartFilters: StateFlow<List<String>> =
        settingsRepository.pinnedWorkoutBodyPartFilters
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pinnedWorkoutEquipmentFilters: StateFlow<List<String>> =
        settingsRepository.pinnedWorkoutEquipmentFilters
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun promoteWorkoutBodyPartFilter(label: String) {
        viewModelScope.launch {
            settingsRepository.promoteWorkoutBodyPartFilter(label)
        }
    }

    fun promoteWorkoutEquipmentFilter(label: String) {
        viewModelScope.launch {
            settingsRepository.promoteWorkoutEquipmentFilter(label)
        }
    }

    private val _customExerciseClassifying = MutableStateFlow(false)
    val customExerciseClassifying: StateFlow<Boolean> = _customExerciseClassifying.asStateFlow()

    private val _workoutInferring = MutableStateFlow(false)
    val workoutInferring: StateFlow<Boolean> = _workoutInferring.asStateFlow()

    /** Records a catalog/custom pick for Recent / Frequent ranking. */
    fun recordExercisePick(name: String, exerciseId: String? = null) {
        viewModelScope.launch {
            runCatching { repository.recordExerciseUsage(name, exerciseId) }
        }
    }

    /** Favourites or unfavourites an exercise in the workout picker. */
    fun setExerciseFavorite(name: String, exerciseId: String?, favorite: Boolean) {
        viewModelScope.launch {
            runCatching { repository.setExerciseFavorite(name, exerciseId, favorite) }
        }
    }

    private val _gifSuggestion = MutableStateFlow<GifSuggestion?>(null)
    val gifSuggestion: StateFlow<GifSuggestion?> = _gifSuggestion.asStateFlow()

    /** Normalises a custom exercise via AI/offline rules and saves it for future picker use. */
    fun classifyCustomExercise(rawName: String, onResolved: (CatalogExercise) -> Unit) {
        if (_customExerciseClassifying.value || _workoutInferring.value) return
        _customExerciseClassifying.value = true
        viewModelScope.launch {
            runCatching { repository.classifyCustomExercise(rawName) }
                .onSuccess { exercise ->
                    onResolved(exercise)
                    // Only brand-new customs (no gif yet, not a known catalog match) need a suggestion.
                    if (exercise.exerciseId == null && exercise.gifUrl == null) {
                        launch {
                            runCatching { repository.suggestGifForCustomExercise(exercise.name) }
                                .getOrNull()
                                ?.let { _gifSuggestion.value = it }
                        }
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = e.message ?: str(R.string.vm_couldn_t_recognise_exercise))
                    }
                }
            _customExerciseClassifying.value = false
        }
    }

    /** User accepted the suggested GIF for their custom exercise — persist it on the preset. */
    fun acceptGifSuggestion() {
        val suggestion = _gifSuggestion.value ?: return
        _gifSuggestion.value = null
        viewModelScope.launch {
            runCatching { repository.acceptGifSuggestion(suggestion) }
        }
    }

    /** User denied the suggested GIF — discard it, nothing persisted. */
    fun dismissGifSuggestion() {
        _gifSuggestion.value = null
    }

    /** Removes a user-added custom exercise from the picker (ExerciseDB catalog entries are a no-op). */
    fun deleteCustomExercise(name: String) {
        viewModelScope.launch {
            runCatching { repository.deleteCustomExercise(name) }
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
                        it.copy(userMessage = str(R.string.vm_added_exercise_s_description, drafts.size))
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(
                            errorDialogTitle = str(R.string.vm_couldn_t_infer_exercises),
                            errorDialogMessage = e.message ?: str(R.string.vm_ai_request_failed)
                        )
                    }
                }
            _workoutInferring.value = false
        }
    }

    private val _workoutNaming = MutableStateFlow(false)
    /** True while an AI workout-name suggestion is in flight. */
    val workoutNaming: StateFlow<Boolean> = _workoutNaming.asStateFlow()

    /** Asks AI to suggest a short session name based on the exercises currently in the draft. */
    fun suggestWorkoutName(exerciseNames: List<String>, onResolved: (String) -> Unit) {
        if (_workoutNaming.value || _workoutInferring.value) return
        _workoutNaming.value = true
        viewModelScope.launch {
            runCatching { repository.suggestWorkoutName(exerciseNames) }
                .onSuccess { name -> onResolved(name) }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = e.message ?: str(R.string.vm_couldn_t_suggest_name))
                    }
                }
            _workoutNaming.value = false
        }
    }

    // --- Analytics --------------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyFood: StateFlow<List<FoodDailySummary>> =
        _realToday
            .flatMapLatest { endDate ->
                val dates = DateUtils.rollingWeekDates(endDate)
                val start = dates.first()
                val end = dates.last()
                repository.getFoodSummariesBetween(start, end)
                    .map { it.filledForDateRange(start, end) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyFood: StateFlow<List<FoodDailySummary>> =
        _monthlyEndDate
            .flatMapLatest { endDate ->
                val (start, end) = DateUtils.rolling30DayBounds(endDate)
                repository.getFoodSummariesBetween(start, end)
                    .map { it.filledForDateRange(start, end) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyExercise: StateFlow<List<ExerciseDailySummary>> =
        _realToday
            .flatMapLatest { endDate ->
                val dates = DateUtils.rollingWeekDates(endDate)
                repository.getExerciseSummariesBetween(dates.first(), dates.last())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyExercise: StateFlow<List<ExerciseDailySummary>> =
        _monthlyEndDate
            .flatMapLatest { endDate ->
                val (start, end) = DateUtils.rolling30DayBounds(endDate)
                repository.getExerciseSummariesBetween(start, end)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sixMonthExercise: StateFlow<List<ExerciseDailySummary>> =
        _realToday
            .flatMapLatest { endDate ->
                val (start, end) = DateUtils.rollingSixMonthBounds(endDate)
                repository.getExerciseSummariesBetween(start, end)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Body measurements ------------------------------------------------------------------

    val bodyMeasurements: StateFlow<List<BodyMeasurement>> =
        repository.bodyMeasurements
            .map { readings -> readings.map(BodyMeasurement::supportedMetricsOnly) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestMeasurement: StateFlow<BodyMeasurement?> =
        repository.latestMeasurement
            .map { it?.supportedMetricsOnly() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Saves a new body reading. Keeps [BodyMeasurement.timestamp] when already set (e.g. FreeScale pull). */
    fun addMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            val ts = if (measurement.timestamp > 0L) {
                measurement.timestamp
            } else {
                System.currentTimeMillis()
            }
            val dateString = measurement.dateString.ifBlank { DateUtils.format(ts) }
            repository.upsertMeasurementByTimestamp(
                measurement.copy(timestamp = ts, dateString = dateString)
            )
            _analysisState.update { it.copy(userMessage = str(R.string.vm_reading_saved)) }
        }
    }

    fun deleteMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.deleteMeasurement(measurement)
            _analysisState.update { it.copy(userMessage = str(R.string.vm_reading_removed)) }
        }
    }

    // --- Workout sessions -------------------------------------------------------------------

    private val _workoutLog = MutableStateFlow(WorkoutLogUiState())
    val workoutLogState: StateFlow<WorkoutLogUiState> = _workoutLog.asStateFlow()

    private val _editingWorkout = MutableStateFlow<WorkoutEditUiState?>(null)
    /** Non-null while the "view / edit exercises" sheet for a logged workout is open. */
    val editingWorkout: StateFlow<WorkoutEditUiState?> = _editingWorkout.asStateFlow()

    /** Resolves the user's current weight from the newest reading, then the profile fallback. */
    private fun currentWeightKg(): Double =
        latestMeasurement.value?.weightKg?.takeIf { it > 0 }
            ?: dashboardState.value.profile?.weightKg?.takeIf { it > 0 }
            ?: 0.0

    private fun catalogGifUrl(exerciseName: String): String? =
        exercisePickerExercises.value
            .firstOrNull { it.name.equals(exerciseName, ignoreCase = true) }
            ?.gifUrl

    /** Saves a workout session and estimates calories burned with a local MET formula. */
    fun logWorkoutSession(draft: WorkoutDraft) {
        if (draft.exercises.isEmpty()) return
        _workoutLog.update { WorkoutLogUiState(isSaving = true) }
        viewModelScope.launch {
            val weightKg = currentWeightKg()
            runCatching {
                repository.logWorkoutSession(
                    draft,
                    weightKg,
                    timestamp = activeDayTimestamp()
                )
            }
                .onSuccess { result ->
                    val name = result.sessionName ?: draft.name
                    _workoutLog.update { WorkoutLogUiState(savedSuccessfully = true) }
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_logged_kcal_4, name, result.caloriesBurned))
                    }
                }
                .onFailure { e ->
                    _workoutLog.update {
                        it.copy(isSaving = false, error = e.message ?: str(R.string.vm_couldn_t_log_workout))
                    }
                }
        }
    }

    /** Call after the dialog has dismissed itself following [WorkoutLogUiState.savedSuccessfully]. */
    fun consumeWorkoutSaved() = _workoutLog.update { WorkoutLogUiState() }

    /**
     * Loads a logged exercise entry's underlying workout (sets/reps/weight per exercise), if it
     * was logged via the structured workout dialog, so it can be viewed/edited. Entries logged
     * via photo/text/preset get a default cardio draft so the user can still edit or restructure.
     */
    fun openWorkoutDetails(log: ExerciseLog) {
        viewModelScope.launch {
            // Clear any leftover save flag from a prior log/edit — the edit dialog shares
            // workoutLogState and would auto-dismiss on first open if savedSuccessfully is stale.
            _workoutLog.value = WorkoutLogUiState()
            val details = repository.getWorkoutDetails(log.id)
            if (details != null) {
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
                                distanceKm = it.distanceKm,
                                durationSeconds = it.durationSeconds,
                                gifUrl = catalogGifUrl(it.name)
                            )
                        }
                    )
                )
            } else {
                // Simple AI-logged exercise with no structured breakdown — open editor with a
                // cardio draft so the user can edit name/duration or add exercises.
                _editingWorkout.value = WorkoutEditUiState(
                    exerciseLogId = log.id,
                    sessionId = 0,
                    draft = WorkoutDraft(
                        name = log.activityName,
                        durationMinutes = log.durationMinutes.coerceAtLeast(5),
                        exercises = listOf(
                            ExerciseDraft(
                                name = log.activityName,
                                sets = 1,
                                reps = 1,
                                equipment = Equipment.CARDIO,
                                durationMinutes = log.durationMinutes.coerceAtLeast(1),
                                gifUrl = catalogGifUrl(log.activityName)
                            )
                        )
                    )
                )
            }
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
                    timestamp = todayTs
                )
            }
                .onSuccess { result ->
                    // Edit/clone closes by clearing editingWorkout (not via savedSuccessfully
                    // LaunchedEffect). Leaving that flag set would auto-dismiss the next open.
                    val name = result.sessionName ?: draft.name
                    _workoutLog.value = WorkoutLogUiState()
                    _editingWorkout.value = null
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_cloned_kcal, name, result.caloriesBurned))
                    }
                }
                .onFailure { e ->
                    _workoutLog.update {
                        it.copy(isSaving = false, error = e.message ?: str(R.string.vm_couldn_t_clone_workout))
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
                if (editing.sessionId == 0) {
                    // Simple AI-logged exercise being upgraded to a structured workout.
                    repository.upgradeExerciseLogToWorkout(
                        exerciseLogId = editing.exerciseLogId,
                        draft = draft,
                        weightKg = weightKg
                    )
                } else {
                    repository.updateWorkoutSession(
                        sessionId = editing.sessionId,
                        exerciseLogId = editing.exerciseLogId,
                        draft = draft,
                        weightKg = weightKg
                    )
                }
            }
                .onSuccess { result ->
                    // Edit/clone closes by clearing editingWorkout (not via savedSuccessfully
                    // LaunchedEffect). Leaving that flag set would auto-dismiss the next open.
                    val name = result.sessionName ?: draft.name
                    _workoutLog.value = WorkoutLogUiState()
                    _editingWorkout.value = null
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_updated_kcal, name, result.caloriesBurned))
                    }
                }
                .onFailure { e ->
                    _workoutLog.update {
                        it.copy(isSaving = false, error = e.message ?: str(R.string.vm_couldn_t_update_workout))
                    }
                }
        }
    }

    // --- Health target calculation -----------------------------------------------------------

    private val _targetPlan = MutableStateFlow(TargetPlanUiState())
    val targetPlanState: StateFlow<TargetPlanUiState> = _targetPlan.asStateFlow()
    private var manualTargetsOverride = false

    /**
     * Calculates a goal + calorie/macro targets from the Body screen's currently displayed basics.
     * An optional AI explanation uses the same unsaved values but cannot change the calculation.
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
            runCatching {
                val activityRecommendation = recentActivityRecommendation()
                val planning = buildTargetPlanningContext(
                    age = age,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    sex = sex,
                    activityLevel = activityLevel,
                    goal = goal,
                    activityRecommendation = activityRecommendation,
                    forceRecalculation = activityRecommendation?.level != null &&
                        activityRecommendation.level != activityLevel
                )
                val context = buildTargetContext(
                    age = age,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    sex = sex,
                    activityLevel = activityLevel,
                    goal = goal,
                    planning = planning
                )
                repository.designTargets(context, planning.options)
            }
                .onSuccess { plan ->
                    _targetPlan.update { it.copy(isLoading = false, plan = plan) }
                }
                .onFailure { e ->
                    _targetPlan.update {
                        it.copy(isLoading = false, error = e.message ?: str(R.string.vm_couldn_t_calculate_targets))
                    }
                }
        }
    }

    /**
     * Persists the selected local plan. Declining a workout-based activity suggestion rebuilds the
     * safe candidates against the retained activity before preserving the prior candidate choice.
     */
    fun applyTargetPlan(
        plan: TargetPlanResponse,
        age: Int,
        heightCm: Double,
        weightKg: Double,
        sex: String?,
        activityLevel: String,
        acceptActivityRecommendation: Boolean
    ) {
        viewModelScope.launch {
            val recommendedActivityDiffers = plan.recommendedActivityLevel?.let {
                !it.equals(activityLevel, ignoreCase = true)
            } == true
            val appliedPlan = if (!acceptActivityRecommendation && recommendedActivityDiffers) {
                val rebuilt = runCatching {
                    buildTargetPlanningContext(
                        age = age,
                        heightCm = heightCm,
                        weightKg = weightKg,
                        sex = sex,
                        activityLevel = activityLevel,
                        goal = plan.recommendedGoal.ifBlank { "RECOMP" },
                        forceRecalculation = true
                    )
                }.getOrNull()
                rebuilt?.options?.planFor(plan.personalizationCandidateId)
                    ?: rebuilt?.options?.defaultPlan
                    ?: HealthTargetCalculator.calculate(
                        targetInput(
                            age = age,
                            heightCm = heightCm,
                            weightKg = weightKg,
                            sex = sex,
                            activityLevel = activityLevel,
                            goal = plan.recommendedGoal.ifBlank { "RECOMP" },
                            forceRecalculation = true
                        )
                    )
            } else {
                plan
            }
            val existing = dashboardState.value.profile
            val applyNutritionTargets = appliedPlan.targetsChanged
            repository.saveProfileRecordingTargets(
                profile = UserProfile(
                    id = 1,
                    age = age,
                    weightKg = weightKg,
                    heightCm = heightCm,
                    dailyTargetCalories = if (applyNutritionTargets) {
                        appliedPlan.dailyTargetCalories
                    } else {
                        existing?.dailyTargetCalories ?: appliedPlan.dailyTargetCalories
                    },
                    targetProteinG = if (applyNutritionTargets) {
                        appliedPlan.targetProteinG
                    } else {
                        existing?.targetProteinG ?: appliedPlan.targetProteinG
                    },
                    targetCarbsG = if (applyNutritionTargets) {
                        appliedPlan.targetCarbsG
                    } else {
                        existing?.targetCarbsG ?: appliedPlan.targetCarbsG
                    },
                    targetFatsG = if (applyNutritionTargets) {
                        appliedPlan.targetFatsG
                    } else {
                        existing?.targetFatsG ?: appliedPlan.targetFatsG
                    },
                    lastUpdatedTimestamp = System.currentTimeMillis(),
                    sex = sex,
                    goal = appliedPlan.recommendedGoal.ifBlank { existing?.goal ?: "RECOMP" },
                    activityLevel = appliedPlan.recommendedActivityLevel ?: activityLevel,
                    goalRationale = appliedPlan.rationale,
                    nutritionTargetHistory = existing?.nutritionTargetHistory.orEmpty()
                ),
                effectiveFromDate = DateUtils.today(),
                recordNutritionTargets = applyNutritionTargets
            )
            manualTargetsOverride = false
            _targetPlan.update {
                TargetPlanUiState(appliedRevision = it.appliedRevision + 1)
            }
            _analysisState.update { it.copy(userMessage = str(R.string.vm_targets_applied)) }
        }
    }

    fun dismissTargetPlan() = _targetPlan.update {
        TargetPlanUiState(appliedRevision = it.appliedRevision)
    }

    // --- AI progress insight ----------------------------------------------------------------

    private val progressChatSessionStore = ProgressChatSessionStore(app)
    private val _progressInsight = MutableStateFlow(ProgressInsightUiState())
    val progressInsightState: StateFlow<ProgressInsightUiState> = _progressInsight.asStateFlow()

    init {
        // After store init — earlier class init runs before this property exists.
        refreshKeptProgressChatFlag()
    }

    /** Drops Continue chat once the kept session has crossed UTC midnight since last message. */
    fun refreshKeptProgressChatFlag() {
        viewModelScope.launch {
            val kept = progressChatSessionStore.loadActive() != null
            _progressInsight.update { state ->
                if (state.hasKeptChat == kept) state else state.copy(hasKeptChat = kept)
            }
        }
    }

    fun requestProgressInsight() {
        if (_progressInsight.value.isLoading) return
        val kept = _progressInsight.value.hasKeptChat
        _progressInsight.update {
            it.copy(isLoading = true, error = null, showInsightDialog = false)
        }
        viewModelScope.launch {
            val contextJson = buildProgressContext()
            val compressed = ProgressMetricsCompressor.compress(contextJson)
            runCatching { repository.summarizeProgress(compressed) }
                .onSuccess { r ->
                    val seedMessage = formatProgressInsightForChat(r)
                    val now = System.currentTimeMillis()
                    _progressInsight.update {
                        ProgressInsightUiState(
                            summary = r.summary,
                            recommendations = r.recommendations,
                            bodyScore = r.bodyScore,
                            metricsContextFull = contextJson,
                            chatMessages = listOf(
                                ProgressChatMessage(ProgressChatRole.ASSISTANT, seedMessage)
                            ),
                            hasKeptChat = kept,
                            showInsightDialog = true,
                            lastMessageAtEpochMs = now
                        )
                    }
                }
                .onFailure { e ->
                    _progressInsight.update {
                        it.copy(
                            isLoading = false,
                            error = friendlyThrowableMessage(e, str(R.string.vm_couldn_t_generate_insight)),
                            hasKeptChat = kept
                        )
                    }
                }
        }
    }

    /**
     * Opens chat for a freshly generated insight. Replaces any previously kept session
     * (Continue disappears until this chat is kept again).
     */
    fun openProgressChatFromInsight() {
        _progressInsight.update {
            it.copy(hasKeptChat = false, showInsightDialog = false, error = null)
        }
        viewModelScope.launch { progressChatSessionStore.clear() }
    }

    /** Restores a kept chat; [onReady] is true when the chat screen can open. */
    fun continueKeptProgressChat(onReady: (Boolean) -> Unit) {
        viewModelScope.launch {
            val session = progressChatSessionStore.loadActive()
            if (session == null) {
                _progressInsight.update { it.copy(hasKeptChat = false, showInsightDialog = false) }
                onReady(false)
                return@launch
            }
            val messages = session.messages.mapNotNull { msg ->
                val role = when (msg.role.lowercase()) {
                    "user" -> ProgressChatRole.USER
                    "assistant" -> ProgressChatRole.ASSISTANT
                    else -> return@mapNotNull null
                }
                ProgressChatMessage(role, msg.text)
            }
            if (messages.isEmpty() || session.metricsContextFull.isBlank()) {
                progressChatSessionStore.clear()
                _progressInsight.update { it.copy(hasKeptChat = false, showInsightDialog = false) }
                onReady(false)
                return@launch
            }
            _progressInsight.value = ProgressInsightUiState(
                summary = session.summary,
                recommendations = session.recommendations,
                bodyScore = session.bodyScore,
                metricsContextFull = session.metricsContextFull,
                chatMessages = messages,
                hasKeptChat = true,
                showInsightDialog = false,
                lastMessageAtEpochMs = session.lastMessageAtEpochMs
            )
            onReady(true)
        }
    }

    fun sendProgressChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _progressInsight.value.isChatLoading) return
        val fullContext = _progressInsight.value.metricsContextFull ?: return

        val now = System.currentTimeMillis()
        val userMessage = ProgressChatMessage(ProgressChatRole.USER, trimmed)
        _progressInsight.update {
            it.copy(
                chatMessages = it.chatMessages + userMessage,
                isChatLoading = true,
                error = null,
                lastMessageAtEpochMs = now
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
                    val replyAt = System.currentTimeMillis()
                    _progressInsight.update {
                        it.copy(
                            isChatLoading = false,
                            chatMessages = it.chatMessages +
                                ProgressChatMessage(ProgressChatRole.ASSISTANT, reply),
                            lastMessageAtEpochMs = replyAt
                        )
                    }
                }
                .onFailure { e ->
                    _progressInsight.update {
                        it.copy(
                            isChatLoading = false,
                            error = friendlyThrowableMessage(e, str(R.string.vm_couldn_t_get_reply))
                        )
                    }
                }
        }
    }

    /** Persists the current chat on back so Continue works until the next UTC day. */
    fun keepProgressChat() {
        viewModelScope.launch {
            val state = _progressInsight.value
            val context = state.metricsContextFull
            if (context.isNullOrBlank() || state.chatMessages.isEmpty()) {
                _progressInsight.value = ProgressInsightUiState(
                    hasKeptChat = progressChatSessionStore.loadActive() != null
                )
                return@launch
            }
            val lastAt = state.lastMessageAtEpochMs.takeIf { it > 0L }
                ?: System.currentTimeMillis()
            val session = ProgressChatSession(
                messages = state.chatMessages.map {
                    ProgressChatSessionMessage(
                        role = if (it.role == ProgressChatRole.USER) "user" else "assistant",
                        text = it.text
                    )
                },
                metricsContextFull = context,
                summary = state.summary,
                recommendations = state.recommendations,
                bodyScore = state.bodyScore,
                lastMessageAtEpochMs = lastAt
            )
            progressChatSessionStore.save(session)
            _progressInsight.value = ProgressInsightUiState(hasKeptChat = true)
        }
    }

    /**
     * Dismisses the insight popup without ending a kept chat.
     * Fresh (unsaved) generate state is discarded.
     */
    fun dismissProgressInsight() {
        val kept = _progressInsight.value.hasKeptChat
        _progressInsight.value = ProgressInsightUiState(hasKeptChat = kept)
        if (kept) refreshKeptProgressChatFlag()
    }

    private fun formatProgressInsightForChat(response: ProgressInsightResponse): String = buildString {
        append(response.summary)
        response.bodyScore?.let { append(str(R.string.vm_body_score_100, it)) }
        if (response.recommendations.isNotEmpty()) {
            append(str(R.string.vm_recommendations))
            response.recommendations.forEach { append("\n• $it") }
        }
    }

    /** Prefer [Throwable.message]; fall back to class name so blank Errors aren't a dead-end UI string. */
    private fun friendlyThrowableMessage(e: Throwable, fallback: String): String {
        val primary = e.message?.takeIf { it.isNotBlank() }
            ?: e.cause?.message?.takeIf { it.isNotBlank() }
        if (primary != null) return primary
        val name = e::class.simpleName?.takeIf { it.isNotBlank() }
        return if (name != null) "$fallback ($name)" else fallback
    }

    // --- Backup (export / import) -----------------------------------------------------------

    /**
     * Exports a local backup via the system share sheet. When [password] is non-null, the backup
     * is encrypted with that password (must be 8–128 chars; validation is done in the UI layer).
     * The password CharArray is zeroed after use.
     */
    fun exportData(context: Context, password: CharArray? = null) {
        viewModelScope.launch {
            try {
                val (file, count) = repository.exportDataToShareFile(password)
                BackupShare.shareJsonFile(
                    context.applicationContext,
                    file,
                    chooserTitle = str(R.string.vm_share_fitbuddy_backup),
                )
                _analysisState.update { it.copy(userMessage = str(R.string.vm_sharing_records, count)) }
            } catch (e: Exception) {
                val msg = if (password != null && password.isNotEmpty()) {
                    str(R.string.vm_export_failed, BackupErrorMessages.encryptionFailed(e.message))
                } else {
                    str(R.string.vm_export_failed, e.message ?: "")
                }
                _analysisState.update { it.copy(userMessage = msg) }
            } finally {
                password?.fill('\u0000')
            }
        }
    }

    /**
     * Imports a local backup with password support. For encrypted files, shows a password dialog
     * (via [passwordProvider]) up to 5 times. Cancel/empty aborts leaving data unchanged.
     */
    fun importData(uri: Uri, passwordProvider: suspend () -> CharArray? = { null }) {
        viewModelScope.launch {
            var attempts = 0
            while (attempts < 5) {
                val result = try {
                    repository.importData(uri, passwordProvider)
                } catch (e: Exception) {
                    _analysisState.update { it.copy(userMessage = str(R.string.vm_import_failed, e.message ?: "")) }
                    return@launch
                }
                when (result) {
                    is BackupImportResult.Success -> {
                        _analysisState.update {
                            it.copy(userMessage = str(R.string.vm_imported_records, result.recordCount))
                        }
                        return@launch
                    }
                    BackupImportResult.WrongPassword -> {
                        attempts++
                        if (attempts >= 5) {
                            _analysisState.update {
                                it.copy(userMessage = BackupErrorMessages.IMPORT_TOO_MANY_ATTEMPTS)
                            }
                            return@launch
                        }
                        // Loop continues — passwordProvider will be called again next iteration
                    }
                    BackupImportResult.PasswordRequired -> {
                        _analysisState.update {
                            it.copy(userMessage = BackupErrorMessages.IMPORT_ABORTED)
                        }
                        return@launch
                    }
                    BackupImportResult.Corrupt -> {
                        _analysisState.update {
                            it.copy(userMessage = str(R.string.vm_import_failed, BackupErrorMessages.BACKUP_CORRUPT))
                        }
                        return@launch
                    }
                    BackupImportResult.Unrecognized -> {
                        _analysisState.update {
                            it.copy(userMessage = str(R.string.vm_import_failed, BackupErrorMessages.NOT_VALID_BACKUP))
                        }
                        return@launch
                    }
                }
            }
        }
    }

    private val _mongoBackupBusy = MutableStateFlow(false)
    val mongoBackupBusy: StateFlow<Boolean> = _mongoBackupBusy.asStateFlow()

    fun uploadMongoBackup() {
        if (BuildConfig.DEBUG) {
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_cloud_backup_uploads_disabled_debug_builds))
            }
            return
        }
        viewModelScope.launch {
            _mongoBackupBusy.value = true
            runCatching { repository.uploadMongoBackup(force = true) }
                .onSuccess { result ->
                    _analysisState.update {
                        it.copy(
                            userMessage = when {
                                result.skippedNoActivity ->
                                    str(R.string.vm_log_meal_workout_before_uploading_cloud_backup)
                                result.skipped -> str(R.string.vm_cloud_backup_already_up_date)
                                else -> str(R.string.vm_uploaded_records_cloud_backup, result.recordCount)
                            }
                        )
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_cloud_upload_failed, BackupErrorMessages.encryptionFailed(e.message)))
                    }
                }
            _mongoBackupBusy.value = false
        }
    }

    /** Developer tools: freeze current tip log rows and append a new cloud tip chunk. */
    fun forceNewCloudBackupChunk() {
        if (BuildConfig.DEBUG) {
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_cloud_backup_uploads_disabled_debug_builds))
            }
            return
        }
        viewModelScope.launch {
            _mongoBackupBusy.value = true
            runCatching { repository.uploadMongoBackup(force = true, forceNewChunk = true) }
                .onSuccess { result ->
                    _analysisState.update {
                        it.copy(
                            userMessage = when {
                                result.skippedNoActivity ->
                                    str(R.string.vm_log_meal_workout_before_uploading_cloud_backup)
                                else -> {
                                    val tip = result.newTipChunkId?.let { str(R.string.vm_tip, it) }.orEmpty()
                                    str(R.string.vm_created_new_cloud_chunk, tip)
                                }
                            }
                        )
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(
                            userMessage = str(R.string.vm_create_chunk_failed, e.message ?: e.javaClass.simpleName)
                        )
                    }
                }
            _mongoBackupBusy.value = false
        }
    }

    /**
     * Settings → Developer tools restore. Blocked in debug builds; use onboarding
     * "Restore from cloud backup" with a Support ID instead ([restoreOnboardingFromCloud]).
     */
    fun downloadMongoBackup(
        supportId: String,
        passwordProvider: suspend () -> CharArray? = { null }
    ) {
        if (BuildConfig.DEBUG) {
            _analysisState.update {
                it.copy(
                    userMessage = str(R.string.vm_debug_builds_restore_cloud_only_during_onboardin)
                )
            }
            return
        }
        viewModelScope.launch {
            _mongoBackupBusy.value = true
            val enteredId = supportId.trim()
            runCatching {
                val count = repository.downloadMongoBackup(enteredId, passwordProvider)
                val restored = settingsRepository.settings.first()
                val reusedId = restored.supportId.trim().ifBlank { enteredId }
                val cloudOn = !BuildConfig.DEBUG
                settingsRepository.save(
                    restored.copy(
                        supportId = reusedId,
                        // Debug builds never opt into cloud upload after restore.
                        cloudBackupEnabled = cloudOn,
                        crashReportingEnabled = if (cloudOn) true else restored.crashReportingEnabled
                    )
                )
                CrashReporter.setSupportId(reusedId)
                if (cloudOn) CrashReporter.setReportingEnabled(true)
                runCatching { repository.ensureSignupDateFromLogs() }
                count
            }
                .onSuccess { count ->
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_restored_records_cloud_backup, count))
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = BackupErrorMessages.cloudRestoreFailed(e.message))
                    }
                }
            _mongoBackupBusy.value = false
        }
    }

    fun changeCloudPassword(newPassword: CharArray?) {
        if (BuildConfig.DEBUG) {
            newPassword?.fill('\u0000')
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_cloud_backup_uploads_disabled_debug_builds))
            }
            return
        }
        viewModelScope.launch {
            _mongoBackupBusy.value = true
            val isBlank = newPassword == null || newPassword.isEmpty() ||
                newPassword.all { it.isWhitespace() }
            runCatching { repository.changeCloudPassword(newPassword) }
                .onSuccess {
                    val msg = if (isBlank) {
                        str(R.string.vm_cloud_backup_reset_default_support_id_protection)
                    } else {
                        str(R.string.vm_cloud_backup_password_updated)
                    }
                    _analysisState.update { it.copy(userMessage = msg) }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(
                            userMessage = str(R.string.vm_password_change_failed, BackupErrorMessages.encryptionFailed(e.message))
                        )
                    }
                }
            _mongoBackupBusy.value = false
        }
    }

    fun prepareCloudBackupEnable() {
        viewModelScope.launch {
            settingsRepository.ensureSupportId()
        }
    }

    fun setCloudBackupEnabled(enabled: Boolean) {
        if (BuildConfig.DEBUG && enabled) {
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_cloud_backup_disabled_debug_builds))
            }
            return
        }
        viewModelScope.launch {
            if (enabled) {
                settingsRepository.ensureSupportId()
            }
            val current = settings.value
            val next = current.copy(
                cloudBackupEnabled = enabled,
                // Enabling cloud also turns crash reports on (and they stay on if cloud is later off).
                crashReportingEnabled = if (enabled) true else current.crashReportingEnabled,
                cloudAutoUploadEnabled = if (enabled) {
                    // Default auto-upload on when first enabling.
                    true
                } else {
                    false
                }
            )
            settingsRepository.save(next)
            if (enabled) {
                CrashReporter.setReportingEnabled(true)
            }
        }
    }

    /**
     * Enables cloud backup and performs the first upload using [password] (Requirement 12).
     * A blank/null password falls back to the Support ID (Requirement 12.4); length validation
     * (8–128 for non-blank) happens in the enable dialog before this is called (Requirement 12.5).
     * A non-blank password is stored encrypted (Android Keystore, device-local) so subsequent
     * manual/auto uploads keep using it; a blank password uses the Support-ID default. On upload
     * failure the toggle stays enabled and a retry runs on the next auto-upload.
     */
    fun enableCloudBackup(password: CharArray?) {
        if (BuildConfig.DEBUG) {
            password?.fill('\u0000')
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_cloud_backup_disabled_debug_builds))
            }
            return
        }
        viewModelScope.launch {
            _mongoBackupBusy.value = true
            val isBlank = password == null || password.isEmpty() || password.all { it.isWhitespace() }
            runCatching {
                settingsRepository.ensureSupportId()
                val current = settingsRepository.settings.first()
                settingsRepository.save(
                    current.copy(
                        cloudBackupEnabled = true,
                        cloudAutoUploadEnabled = true,
                        crashReportingEnabled = true
                    )
                )
                CrashReporter.setReportingEnabled(true)
                // First upload seals with the chosen password (blank → Support ID).
                repository.changeCloudPassword(password)
            }
                .onSuccess {
                    _analysisState.update {
                        it.copy(
                            userMessage = if (isBlank) {
                                str(R.string.vm_cloud_backup_enabled_default_support_id_protecti)
                            } else {
                                str(R.string.vm_cloud_backup_enabled_encrypted)
                            }
                        )
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_cloud_backup_enable_failed, e.message ?: ""))
                    }
                }
            _mongoBackupBusy.value = false
        }
    }

    fun setCloudAutoUploadEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(cloudAutoUploadEnabled = enabled) }
        }
    }

    /** Guest path: ensure a Support ID exists before the AI/profile steps. */
    fun startGuestOnboarding() {
        if (_forceFullOnboarding.value) return // preview — do not mint / touch Support ID
        viewModelScope.launch {
            settingsRepository.ensureSupportId()
        }
    }

    /**
     * Onboarding cloud restore (pull-only). Reuses the Support ID from the backup payload
     * (or the ID entered to fetch it), then probes AI credentials. Release builds opt into
     * cloud backup afterward; debug builds keep uploads off.
     */
    fun restoreOnboardingFromCloud(
        supportId: String,
        passwordProvider: suspend () -> CharArray? = { null },
        onResult: (Boolean, String?) -> Unit
    ) {
        if (_forceFullOnboarding.value) {
            onResult(false, str(R.string.vm_preview_mode_restore_disabled))
            return
        }
        if (_onboardingRestoring.value) return
        _onboardingRestoring.value = true
        viewModelScope.launch {
            val enteredId = supportId.trim()
            val result = runCatching {
                repository.downloadMongoBackup(enteredId, passwordProvider)
                val restored = settingsRepository.settings.first()
                // Prefer Support ID from restored settings; fall back to the ID used to fetch.
                val reusedId = restored.supportId.trim().ifBlank { enteredId }
                val cloudOn = !BuildConfig.DEBUG
                settingsRepository.save(
                    restored.copy(
                        supportId = reusedId,
                        // Debug builds never opt into cloud upload after restore.
                        cloudBackupEnabled = cloudOn,
                        cloudAutoUploadEnabled = cloudOn,
                        crashReportingEnabled = if (cloudOn) true else restored.crashReportingEnabled
                    )
                )
                CrashReporter.setSupportId(reusedId)
                if (cloudOn) CrashReporter.setReportingEnabled(true)
                runCatching { repository.ensureSignupDateFromLogs() }
                val after = settingsRepository.settings.first()
                applyRestoredAiCredentialGate(after)
            }
            _onboardingRestoring.value = false
            result
                .onSuccess { onResult(true, null) }
                .onFailure { e ->
                    onResult(false, e.message ?: str(R.string.vm_couldn_t_restore_cloud))
                }
        }
    }

    /**
     * Onboarding local file restore. Keeps Support ID from backup when present. For encrypted
     * backups the [passwordProvider] is invoked to obtain the password (the UI shows a prompt);
     * a wrong password retries up to 5 times total before failing.
     */
    fun restoreOnboardingFromLocal(
        uri: Uri,
        passwordProvider: suspend () -> CharArray? = { null },
        onResult: (Boolean, String?) -> Unit
    ) {
        if (_forceFullOnboarding.value) {
            onResult(false, str(R.string.vm_preview_mode_restore_disabled))
            return
        }
        if (_onboardingRestoring.value) return
        _onboardingRestoring.value = true
        viewModelScope.launch {
            val result = runCatching {
                var attempts = 1
                var importResult = repository.importData(uri, passwordProvider)
                while (importResult is BackupImportResult.WrongPassword && attempts < 5) {
                    attempts++
                    importResult = repository.importData(uri, passwordProvider)
                }
                if (importResult !is BackupImportResult.Success) {
                    error(
                        when (importResult) {
                            BackupImportResult.WrongPassword ->
                                BackupErrorMessages.IMPORT_TOO_MANY_ATTEMPTS
                            BackupImportResult.PasswordRequired ->
                                BackupErrorMessages.IMPORT_ABORTED
                            BackupImportResult.Corrupt -> BackupErrorMessages.BACKUP_CORRUPT
                            BackupImportResult.Unrecognized -> BackupErrorMessages.NOT_VALID_BACKUP
                            is BackupImportResult.Success -> "" // unreachable
                        }
                    )
                }
                val after = settingsRepository.settings.first()
                if (after.supportId.isNotBlank()) {
                    CrashReporter.setSupportId(after.supportId)
                }
                runCatching { repository.ensureSignupDateFromLogs() }
                applyRestoredAiCredentialGate(after)
            }
            _onboardingRestoring.value = false
            result
                .onSuccess { onResult(true, null) }
                .onFailure { e ->
                    onResult(false, e.message ?: str(R.string.vm_couldn_t_restore_file))
                }
        }
    }

    /** Developer: mint a new Support ID for crash reporting and future cloud uploads. */
    fun regenerateSupportId() {
        viewModelScope.launch {
            val id = settingsRepository.regenerateSupportId()
            // Signup date was cleared with the ID; re-mint from earliest food/exercise log.
            repository.ensureSignupDateFromLogs()
            CrashReporter.setSupportId(id)
            refreshSupporterStatus(id)
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_new_support_id_generated_copy_backup_settings))
            }
        }
    }

    /** AI-only onboarding finish: persist keys and clear the force-AI flag. */
    fun completeAiSetupOnly(aiSettings: AppSettings) {
        if (_forceFullOnboarding.value) {
            finishOnboardingPreview()
            return
        }
        if (_onboardingSaving.value) return
        _onboardingSaving.value = true
        viewModelScope.launch {
            runCatching {
                val current = settingsRepository.settings.first()
                val merged = current.copy(
                    provider = aiSettings.provider,
                    openRouterApiKeys = aiSettings.openRouterApiKeys,
                    openRouterApiKey = aiSettings.openRouterApiKey,
                    openRouterOAuthKey = aiSettings.openRouterOAuthKey.ifBlank {
                        current.openRouterOAuthKey
                    },
                    openRouterModel = aiSettings.openRouterModel.ifBlank {
                        current.openRouterModel
                    },
                    openRouterTextModel = aiSettings.openRouterTextModel,
                    geminiApiKeys = aiSettings.geminiApiKeys,
                    geminiApiKey = aiSettings.geminiApiKey,
                    geminiModel = aiSettings.geminiModel.ifBlank { current.geminiModel },
                    geminiTextModel = aiSettings.geminiTextModel,
                    ollamaBaseUrl = aiSettings.ollamaBaseUrl,
                    ollamaModel = aiSettings.ollamaModel.ifBlank { current.ollamaModel },
                    ollamaTextModel = aiSettings.ollamaTextModel,
                    ollamaUseCloud = aiSettings.ollamaUseCloud,
                    ollamaApiKeys = aiSettings.ollamaApiKeys,
                    ollamaApiKey = aiSettings.ollamaApiKey,
                    openAiApiKeys = aiSettings.openAiApiKeys,
                    openAiApiKey = aiSettings.openAiApiKey,
                    openAiModel = aiSettings.openAiModel.ifBlank { current.openAiModel },
                    openAiTextModel = aiSettings.openAiTextModel,
                    customBaseUrl = aiSettings.customBaseUrl,
                    customModel = aiSettings.customModel,
                    customTextModel = aiSettings.customTextModel,
                    customApiKeys = aiSettings.customApiKeys,
                    customApiKey = aiSettings.customApiKey,
                    aiAutoFailoverByProvider = aiSettings.aiAutoFailoverByProvider,
                    showPaidModelsByProvider = aiSettings.showPaidModelsByProvider
                )
                settingsRepository.save(merged)
                _forceAiSetup.value = false
            }.onFailure { e ->
                _analysisState.update {
                    it.copy(userMessage = e.message ?: str(R.string.vm_couldn_t_save_ai_settings))
                }
            }
            _onboardingSaving.value = false
        }
    }

    // --- AI analysis ------------------------------------------------------------------------

    private val _analysisState = MutableStateFlow(AnalysisUiState())
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    // Retained so a clarification answer can be merged with the original prompt/photo.
    private var pendingText: String = ""
    private var pendingImages: List<ByteArray> = emptyList()

    fun analyzeText(text: String) {
        runningQueueId = null
        submit(text, emptyList())
    }

    // --- Photo log dialog + queue -----------------------------------------------------------

    private val photoQueueStore = PhotoQueueStore(app)
    val photoQueue: StateFlow<List<QueuedPhotoLog>> = photoQueueStore.items
    private val _photoDraft = MutableStateFlow(PhotoDraftUiState())
    val photoDraft: StateFlow<PhotoDraftUiState> = _photoDraft.asStateFlow()

    // Queue item being analysed; removed once it produces a result so failures stay queued.
    private var runningQueueId: String? = null

    init {
        viewModelScope.launch { photoQueueStore.load() }
    }

    fun addPhotosToDraft(photos: List<ByteArray>) = _photoDraft.update {
        it.copy(visible = true, photos = (it.photos + photos).take(MAX_PHOTO_LOG_IMAGES))
    }

    fun removePhotoFromDraft(index: Int) = _photoDraft.update {
        it.copy(photos = it.photos.filterIndexed { i, _ -> i != index })
    }

    fun setPhotoDraftNote(note: String) = _photoDraft.update { it.copy(note = note) }

    fun dismissPhotoDraft() {
        _photoDraft.value = PhotoDraftUiState()
    }

    fun sendPhotoDraft() {
        val draft = _photoDraft.value
        if (draft.photos.isEmpty()) return
        dismissPhotoDraft()
        runningQueueId = draft.editingQueueId
        submit(draft.note.trim(), draft.photos)
    }

    fun queuePhotoDraft() {
        val draft = _photoDraft.value
        if (draft.photos.isEmpty()) return
        dismissPhotoDraft()
        viewModelScope.launch {
            val editId = draft.editingQueueId
            if (editId != null) {
                photoQueueStore.update(editId, draft.photos, draft.note)
            } else {
                photoQueueStore.add(draft.photos, draft.note)
            }
            _analysisState.update {
                it.copy(
                    userMessage = str(
                        if (editId != null) R.string.photo_queue_updated else R.string.photo_queue_saved
                    )
                )
            }
        }
    }

    fun editQueuedPhoto(id: String) {
        val item = photoQueue.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val photos = photoQueueStore.photos(id)
            if (photos.isEmpty()) {
                photoQueueStore.remove(id)
                _analysisState.update { it.copy(userMessage = str(R.string.photo_queue_missing)) }
                return@launch
            }
            _photoDraft.value = PhotoDraftUiState(
                visible = true,
                photos = photos,
                note = item.note,
                editingQueueId = id
            )
        }
    }

    fun runQueuedPhoto(id: String) {
        val item = photoQueue.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val photos = photoQueueStore.photos(id)
            if (photos.isEmpty()) {
                photoQueueStore.remove(id)
                _analysisState.update { it.copy(userMessage = str(R.string.photo_queue_missing)) }
                return@launch
            }
            runningQueueId = id
            submit(item.note, photos)
        }
    }

    suspend fun queuedThumbnail(id: String): ByteArray? = photoQueueStore.photos(id).firstOrNull()

    fun deleteQueuedPhoto(id: String) {
        viewModelScope.launch { photoQueueStore.remove(id) }
    }

    private fun finishQueuedRun() {
        val id = runningQueueId ?: return
        runningQueueId = null
        viewModelScope.launch { photoQueueStore.remove(id) }
    }

    fun submitClarification(answer: String) {
        val merged = listOf(pendingText, answer)
            .filter { it.isNotBlank() }
            .joinToString(". ")
        submit(merged, pendingImages, resetPending = false)
    }

    private fun submit(text: String, images: List<ByteArray>, resetPending: Boolean = true) {
        if (resetPending) {
            pendingText = text
            pendingImages = images
        }
        CrashReporter.breadcrumb(
            "ai",
            if (pendingImages.isNotEmpty()) "analyze_photo" else "analyze_text"
        )
        val initialModel = settings.value.modelFor(pendingImages.isNotEmpty()).takeIf { it.isNotBlank() }
        _analysisState.update {
            it.copy(
                isLoading = true,
                clarificationMessage = null,
                analyzingModel = initialModel,
            )
        }
        viewModelScope.launch {
            val outcome = repository.analyze(
                text,
                pendingImages,
                buildUserStateContext(),
                customTimestamp = activeDayTimestamp(),
                onModelActive = { modelId ->
                    _analysisState.update { it.copy(analyzingModel = modelId) }
                }
            )
            handleOutcome(outcome)
        }
    }

    private fun handleOutcome(outcome: AnalysisOutcome) {
        val rawJson = maybeRawAiJson()
        // Capture before clearPending() wipes the photo buffer.
        when (outcome) {
            is AnalysisOutcome.FoodReady -> {
                clearPending()
                finishQueuedRun()
                _analysisState.update {
                    it.copy(
                        isLoading = false,
                        analyzingModel = null,
                        clarificationMessage = null,
                        foodDraft = outcome.draft,
                        mealDraft = null,
                        editingFoodLogId = null,
                        editingMealFoodIndex = null,
                        userMessage = outcome.failoverNote,
                        rawAiJson = rawJson
                    )
                }
            }

            is AnalysisOutcome.MealReady -> {
                clearPending()
                finishQueuedRun()
                _analysisState.update {
                    it.copy(
                        isLoading = false,
                        analyzingModel = null,
                        clarificationMessage = null,
                        foodDraft = null,
                        mealDraft = outcome.draft.snapshot(),
                        editingFoodLogId = null,
                        editingMealFoodIndex = null,
                        userMessage = outcome.failoverNote,
                        rawAiJson = rawJson
                    )
                }
            }

            is AnalysisOutcome.ExerciseSaved -> {
                clearPending()
                finishQueuedRun()
                _analysisState.update {
                    it.copy(
                        isLoading = false,
                        analyzingModel = null,
                        clarificationMessage = null,
                        userMessage = outcome.failoverNote
                            ?: str(R.string.vm_logged_kcal_4, outcome.activityName, outcome.caloriesBurned),
                        rawAiJson = rawJson
                    )
                }
            }

            is AnalysisOutcome.NeedsClarification -> _analysisState.update {
                it.copy(
                    isLoading = false,
                    analyzingModel = null,
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
                        analyzingModel = null,
                        errorDialogTitle = str(R.string.vm_item_not_identified),
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
                        analyzingModel = null,
                        errorDialogTitle = str(R.string.vm_couldn_t_reach_ai),
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
                    editingMealFoodIndex = null,
                    userMessage = if (editingId != null) {
                        str(R.string.vm_updated_kcal_2, draft.name, draft.totalCalories)
                    } else {
                        str(R.string.vm_logged_kcal, draft.name, draft.totalCalories)
                    }
                )
            }
        }
    }

    /** Opens Food review for a one-food log; Meal review when two or more foods. */
    fun editFoodLog(log: FoodLog) {
        viewModelScope.launch {
            val draft = repository.mealLogToMealDraft(log)
            val foodDraft = draft.asSingleFoodDraftOrNull()
            if (foodDraft != null) {
                _analysisState.update {
                    it.copy(
                        foodDraft = foodDraft,
                        mealDraft = null,
                        editingFoodLogId = log.id,
                        editingMealFoodIndex = null,
                        reviewMessage = null
                    )
                }
            } else {
                _analysisState.update {
                    it.copy(
                        mealDraft = draft.snapshot(),
                        foodDraft = null,
                        editingFoodLogId = log.id,
                        editingMealFoodIndex = null,
                        reviewMessage = null
                    )
                }
            }
        }
    }

    /** Removes a food entry from the log. */
    fun deleteFoodLog(log: FoodLog) {
        viewModelScope.launch {
            repository.deleteFood(log)
            _analysisState.update { it.copy(userMessage = str(R.string.vm_removed, log.dishName)) }
        }
    }

    /** Removes an exercise entry from the log (blocked for Health Connect imports). */
    fun deleteExerciseLog(log: ExerciseLog) {
        viewModelScope.launch {
            if (log.isFromHealthConnect()) {
                _analysisState.update {
                    it.copy(userMessage = str(R.string.vm_health_connect_imported_not_deletable))
                }
                return@launch
            }
            val removed = repository.deleteExercise(log)
            _analysisState.update {
                it.copy(
                    userMessage = if (removed) {
                        str(R.string.vm_removed, log.activityName)
                    } else {
                        str(R.string.vm_health_connect_imported_not_deletable)
                    }
                )
            }
        }
    }

    /** Quick-add a drink toward today's water goal (uses the chip's saved amount). */
    fun addWaterIntake(kind: WaterDrinkKind) {
        viewModelScope.launch {
            val amount = settings.value.quickAddMl(kind)
            repository.addWaterLog(
                amountMl = amount,
                drinkKind = kind,
                dateString = _realToday.value,
            )
        }
    }

    /** Persist a long-press change to a drink chip's quick-add milliliters. */
    fun setWaterQuickAddMl(kind: WaterDrinkKind, amountMl: Int) {
        viewModelScope.launch {
            settingsRepository.update { it.withQuickAddMl(kind, amountMl) }
        }
    }

    fun deleteWaterLog(log: WaterLog) {
        viewModelScope.launch {
            repository.deleteWaterLog(log)
        }
    }

    /** Edit a logged drink bucket's total milliliters for that day. */
    fun updateWaterLogAmount(log: WaterLog, amountMl: Int) {
        viewModelScope.launch {
            repository.updateWaterLogAmount(log, amountMl)
        }
    }

    /**
     * Opens food or meal review prefilled from [log], stamped for real today. Save inserts a new
     * row ([editingFoodLogId] stays null).
     */
    fun cloneFoodLogToToday(log: FoodLog) {
        viewModelScope.launch {
            val todayTs = DateUtils.timestampOnDate(_realToday.value)
            val draft = repository.mealLogToMealDraft(log).copy(timestamp = todayTs)
            val foodDraft = draft.asSingleFoodDraftOrNull()
            if (foodDraft != null) {
                _analysisState.update {
                    it.copy(
                        foodDraft = foodDraft,
                        mealDraft = null,
                        editingFoodLogId = null,
                        editingMealFoodIndex = null,
                        reviewMessage = null
                    )
                }
            } else {
                _analysisState.update {
                    it.copy(
                        mealDraft = draft.snapshot(),
                        foodDraft = null,
                        editingFoodLogId = null,
                        editingMealFoodIndex = null,
                        reviewMessage = null
                    )
                }
            }
        }
    }

    /**
     * Opens the workout editor prefilled from [log] in clone mode. Save inserts a new today
     * session. Simple AI exercise logs (no set breakdown) get a cardio draft so the user can
     * still confirm or tweak before saving.
     */
    fun cloneWorkoutToToday(log: ExerciseLog) {
        viewModelScope.launch {
            _workoutLog.value = WorkoutLogUiState()
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
                            distanceKm = it.distanceKm,
                            durationSeconds = it.durationSeconds,
                            gifUrl = catalogGifUrl(it.name)
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
                            durationMinutes = log.durationMinutes.coerceAtLeast(1),
                            gifUrl = catalogGifUrl(log.activityName)
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

    fun openFoodEditor(draft: FoodDraft, mealFoodIndex: Int? = null) {
        _analysisState.update {
            it.copy(
                foodDraft = draft,
                editingMealFoodIndex = mealFoodIndex,
                reviewMessage = null
            )
        }
    }

    /** Updates a food row inside the in-progress meal review (after editing its ingredients). */
    fun updateMealDraftFood(index: Int, food: FoodEntryDraft) {
        _analysisState.update { state ->
            val meal = state.mealDraft ?: return@update state
            if (index !in meal.foods.indices) return@update state
            val updated = meal.foods.toMutableList().also { it[index] = food.snapshot() }
            state.copy(mealDraft = meal.copy(foods = updated))
        }
    }

    /** Removes a food row from the in-progress meal review draft. */
    fun removeMealDraftFood(index: Int) {
        _analysisState.update { state ->
            val meal = state.mealDraft ?: return@update state
            if (index !in meal.foods.indices) return@update state
            val updated = meal.foods.toMutableList().also { it.removeAt(index) }
            state.copy(
                mealDraft = meal.copy(foods = updated),
                editingMealFoodIndex = if (state.editingMealFoodIndex == index) null
                else state.editingMealFoodIndex
            )
        }
    }

    /** Applies nested Food review confirm back into [mealDraft] and closes the food sheet. */
    fun applyMealFoodEdit(confirmed: FoodDraft) {
        _analysisState.update { state ->
            val meal = state.mealDraft ?: return@update state.copy(
                foodDraft = null,
                editingMealFoodIndex = null
            )
            val index = state.editingMealFoodIndex
            if (index == null || index !in meal.foods.indices) {
                return@update state.copy(foodDraft = null, editingMealFoodIndex = null)
            }
            val entry = confirmed.toFoodEntry().snapshot()
            val foods = meal.foods.toMutableList().also { it[index] = entry }
            state.copy(
                mealDraft = meal.copy(foods = foods),
                foodDraft = null,
                editingMealFoodIndex = null
            )
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
            val outcome = repository.analyze(text, emptyList(), buildUserStateContext(), forceEstimate = true)
            applyReanalyzeOutcome(outcome)
        }
    }

    /** Re-runs analysis for the open meal review (name + current food names as context). */
    fun reanalyzeMeal(correctedText: String) {
        val meal = _analysisState.value.mealDraft ?: return
        val name = correctedText.trim().ifBlank { meal.name }.trim()
        if (name.isBlank()) return
        val prompt = buildString {
            append(name)
            if (meal.foods.isNotEmpty()) {
                append(": ")
                append(meal.foods.joinToString(", ") { it.name })
            }
        }
        _analysisState.update { it.copy(isReanalyzing = true, reviewMessage = null) }
        viewModelScope.launch {
            val outcome = repository.analyze(
                prompt,
                emptyList(),
                buildUserStateContext(),
                forceEstimate = true
            )
            applyReanalyzeOutcome(outcome)
        }
    }

    private fun applyReanalyzeOutcome(outcome: AnalysisOutcome) {
        val rawJson = maybeRawAiJson()
        _analysisState.update { state ->
            // Nested food edit keeps foodDraft while mealDraft is also set.
            val foodReviewOpen = state.foodDraft != null
            when (outcome) {
                is AnalysisOutcome.FoodReady -> {
                    val stamped = outcome.draft.copy(
                        timestamp = state.foodDraft?.timestamp
                            ?: state.mealDraft?.timestamp
                            ?: outcome.draft.timestamp,
                        quantity = state.foodDraft?.quantity ?: outcome.draft.quantity,
                        iconTopic = state.foodDraft?.iconTopic
                            ?: state.mealDraft?.iconTopic
                            ?: outcome.draft.iconTopic
                    )
                    if (foodReviewOpen) {
                        state.copy(
                            isReanalyzing = false,
                            foodDraft = stamped,
                            reviewMessage = outcome.failoverNote,
                            rawAiJson = rawJson
                        )
                    } else {
                        // Stay on meal review even when AI returns one food.
                        state.copy(
                            isReanalyzing = false,
                            mealDraft = stamped.toSingleFoodMeal().snapshot(),
                            foodDraft = null,
                            editingMealFoodIndex = null,
                            reviewMessage = outcome.failoverNote,
                            rawAiJson = rawJson
                        )
                    }
                }

                is AnalysisOutcome.MealReady -> {
                    val preservedTs = state.foodDraft?.timestamp
                        ?: state.mealDraft?.timestamp
                        ?: outcome.draft.timestamp
                    val preservedIcon = state.foodDraft?.iconTopic
                        ?: state.mealDraft?.iconTopic
                        ?: outcome.draft.iconTopic
                    val meal = outcome.draft.copy(
                        timestamp = preservedTs,
                        iconTopic = preservedIcon
                    ).snapshot()
                    val single = meal.asSingleFoodDraftOrNull()
                    if (foodReviewOpen) {
                        if (single != null) {
                            val priorQty = state.foodDraft?.quantity
                            state.copy(
                                isReanalyzing = false,
                                foodDraft = single.copy(
                                    quantity = priorQty ?: single.quantity
                                ),
                                reviewMessage = outcome.failoverNote,
                                rawAiJson = rawJson
                            )
                        } else {
                            // Multi-food result while editing a food → open meal review.
                            state.copy(
                                isReanalyzing = false,
                                mealDraft = meal,
                                foodDraft = null,
                                editingMealFoodIndex = null,
                                reviewMessage = outcome.failoverNote,
                                rawAiJson = rawJson
                            )
                        }
                    } else {
                        state.copy(
                            isReanalyzing = false,
                            mealDraft = meal,
                            foodDraft = null,
                            editingMealFoodIndex = null,
                            reviewMessage = outcome.failoverNote,
                            rawAiJson = rawJson
                        )
                    }
                }

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
                        reviewMessage = str(R.string.vm_looks_like_activity_not_food_item),
                        rawAiJson = rawJson
                    )
            }
        }
    }

    /** Surfaces a short pill message on [MainScreen] (permissions, one-shot status, etc.). */
    fun showTransientMessage(message: String) {
        _analysisState.update { it.copy(userMessage = message) }
    }

    /** Turns off the daily reminder when notification permission is unavailable. */
    fun disableDailyLogReminder() {
        viewModelScope.launch {
            val current = settings.value
            if (current.dailyLogReminderEnabled) {
                settingsRepository.save(current.copy(dailyLogReminderEnabled = false))
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
        pendingImages = emptyList()
    }

    // --- Profile ----------------------------------------------------------------------------

    /** Opens OpenRouter OAuth PKCE via loopback callback + Custom Tab / browser. */
    fun startOpenRouterOAuth(context: Context) {
        if (_forceFullOnboarding.value) {
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_preview_mode_openrouter_sign_disabled))
            }
            return
        }
        if (_openRouterOAuthBusy.value) return
        val appContext = context.applicationContext
        val verifier = OpenRouterPkce.generateVerifier()
        pendingOpenRouterCodeVerifier = verifier
        val challenge = OpenRouterPkce.challengeS256(verifier)
        viewModelScope.launch {
            settingsRepository.setPendingOpenRouterPkceVerifier(verifier)
            openRouterOAuthCallbackServer?.stop()
            val server = OpenRouterOAuthCallbackServer(
                onCode = { code ->
                    bringFitBuddyToForeground(appContext)
                    handleOpenRouterOAuthCode(code)
                },
                onFailure = { message ->
                    _analysisState.update { it.copy(userMessage = message) }
                }
            )
            openRouterOAuthCallbackServer = server
            val port = runCatching { server.start(viewModelScope) }.getOrElse {
                _analysisState.update {
                    it.copy(userMessage = str(R.string.vm_couldn_t_start_openrouter_sign_listener))
                }
                return@launch
            }
            val callbackUrl = OpenRouterOAuth.loopbackCallbackUrl(port)
            OpenRouterOAuth.launchAuth(context, challenge, callbackUrl)
        }
    }

    private fun bringFitBuddyToForeground(appContext: Context) {
        runCatching {
            val am = appContext.getSystemService(android.app.ActivityManager::class.java)
            am?.appTasks?.firstOrNull()?.moveToFront()
                ?: appContext.startActivity(
                    Intent(appContext, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                )
        }
    }

    /** Handles `fitbuddy://openrouter/callback?code=…` from a custom-scheme redirect. */
    fun handleOpenRouterOAuthCallback(uri: Uri) {
        if (!OpenRouterOAuth.isCallback(uri)) return
        val code = OpenRouterOAuth.codeFromCallback(uri)
        if (code.isNullOrBlank()) {
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_openrouter_sign_cancelled_incomplete))
            }
            return
        }
        handleOpenRouterOAuthCode(code)
    }

    private fun handleOpenRouterOAuthCode(code: String) {
        if (_openRouterOAuthBusy.value) return
        _openRouterOAuthBusy.value = true
        openRouterOAuthCallbackServer?.stop()
        openRouterOAuthCallbackServer = null
        viewModelScope.launch {
            val verifier = pendingOpenRouterCodeVerifier
                ?: settingsRepository.takePendingOpenRouterPkceVerifier()
            pendingOpenRouterCodeVerifier = null
            if (verifier.isNullOrBlank()) {
                _analysisState.update {
                    it.copy(userMessage = str(R.string.vm_openrouter_sign_expired_try_connect_again))
                }
                _openRouterOAuthBusy.value = false
                return@launch
            }
            runCatching {
                val key = OpenRouterOAuth.exchangeCode(code, verifier)
                settingsRepository.setOpenRouterOAuthKey(key)
                key
            }
                .onSuccess {
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.settings_connected_with_openrouter))
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(userMessage = e.message ?: str(R.string.vm_couldn_t_connect_openrouter))
                    }
                }
            _openRouterOAuthBusy.value = false
        }
    }

    fun disconnectOpenRouterOAuth() {
        if (_forceFullOnboarding.value) {
            _analysisState.update {
                it.copy(userMessage = str(R.string.vm_preview_mode_openrouter_disconnect_disabled))
            }
            return
        }
        viewModelScope.launch {
            pendingOpenRouterCodeVerifier = null
            openRouterOAuthCallbackServer?.stop()
            openRouterOAuthCallbackServer = null
            settingsRepository.takePendingOpenRouterPkceVerifier()
            settingsRepository.clearOpenRouterOAuthKey()
            _analysisState.update { it.copy(userMessage = str(R.string.vm_openrouter_disconnected)) }
        }
    }

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
                    onResult(false, e.message ?: str(R.string.onboarding_couldn_t_validate_api_key))
                }
        }
    }

    /**
     * First-run setup: persists AI settings (required), saves profile with placeholder targets,
     * seeds an initial body reading, then flags a one-shot target design for dashboard launch.
     */
    fun completeOnboarding(
        firstName: String,
        lastName: String,
        age: Int,
        weightKg: Double,
        heightCm: Double,
        sex: String?,
        goal: String,
        activityLevel: String,
        aiSettings: AppSettings,
        region: AppRegion,
        crashReportingEnabled: Boolean,
        cloudBackupEnabled: Boolean = false
    ) {
        if (_forceFullOnboarding.value) {
            finishOnboardingPreview()
            return
        }
        if (_onboardingSaving.value) return
        _onboardingSaving.value = true
        viewModelScope.launch {
            runCatching {
                require(age in 18..120) { "FitBuddy's automated health targets are for adults 18+" }
                require(heightCm in 100.0..250.0) { "Enter a valid height between 100 and 250 cm" }
                require(weightKg in 25.0..400.0) { "Enter a valid weight between 25 and 400 kg" }
                val current = settings.value
                val cloudOn = cloudBackupEnabled && !BuildConfig.DEBUG
                // Cloud backup implies crash reporting.
                val crashOn = crashReportingEnabled || cloudOn
                if (cloudOn) {
                    settingsRepository.ensureSupportId()
                    // Onboarding: Support ID default — change password later in Settings.
                    settingsRepository.setCloudBackupPasswordBlob(null)
                }
                settingsRepository.save(
                    current.copy(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        region = region.name,
                        crashReportingEnabled = crashOn,
                        cloudBackupEnabled = cloudOn,
                        cloudAutoUploadEnabled = if (cloudOn) true else current.cloudAutoUploadEnabled,
                        cloudBackupPasswordSet = if (cloudOn) false else current.cloudBackupPasswordSet,
                        provider = aiSettings.provider,
                        openRouterApiKeys = aiSettings.openRouterApiKeys,
                        openRouterApiKey = aiSettings.openRouterApiKey,
                        openRouterOAuthKey = aiSettings.openRouterOAuthKey.ifBlank {
                            current.openRouterOAuthKey
                        },
                        openRouterModel = aiSettings.openRouterModel.ifBlank {
                            current.openRouterModel
                        },
                        openRouterTextModel = aiSettings.openRouterTextModel,
                        geminiApiKeys = aiSettings.geminiApiKeys,
                        geminiApiKey = aiSettings.geminiApiKey,
                        geminiModel = aiSettings.geminiModel.ifBlank { current.geminiModel },
                        geminiTextModel = aiSettings.geminiTextModel,
                        ollamaBaseUrl = aiSettings.ollamaBaseUrl,
                        ollamaModel = aiSettings.ollamaModel.ifBlank { current.ollamaModel },
                        ollamaTextModel = aiSettings.ollamaTextModel,
                        ollamaUseCloud = aiSettings.ollamaUseCloud,
                        ollamaApiKeys = aiSettings.ollamaApiKeys,
                        ollamaApiKey = aiSettings.ollamaApiKey,
                        openAiApiKeys = aiSettings.openAiApiKeys,
                        openAiApiKey = aiSettings.openAiApiKey,
                        openAiModel = aiSettings.openAiModel.ifBlank { current.openAiModel },
                        openAiTextModel = aiSettings.openAiTextModel,
                        customBaseUrl = aiSettings.customBaseUrl,
                        customModel = aiSettings.customModel,
                        customTextModel = aiSettings.customTextModel,
                        customApiKeys = aiSettings.customApiKeys,
                        customApiKey = aiSettings.customApiKey,
                        aiAutoFailoverByProvider = aiSettings.aiAutoFailoverByProvider,
                        showPaidModelsByProvider = aiSettings.showPaidModelsByProvider
                    )
                )
                repository.saveProfileRecordingTargets(
                    profile = UserProfile(
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
                    ),
                    effectiveFromDate = DateUtils.today(),
                    recordNutritionTargets = false
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
                CrashReporter.setReportingEnabled(crashOn)
                _forceAiSetup.value = false
                _forceFullOnboarding.value = false
            }.onFailure { e ->
                _analysisState.update {
                    it.copy(userMessage = e.message ?: str(R.string.vm_couldn_t_save_profile))
                }
            }
            _onboardingSaving.value = false
        }
    }

    /** Persist display name only (DataStore). Used by the existing-user name prompt. */
    fun saveUserName(firstName: String, lastName: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.save(
                current.copy(
                    firstName = firstName.trim(),
                    lastName = lastName.trim()
                )
            )
        }
    }

    /**
     * Settings Profile card: name (DataStore) + permanent body attrs on [UserProfile]
     * (age/height/sex), preserving weight, goals, and targets.
     */
    fun savePermanentProfile(
        firstName: String,
        lastName: String,
        age: Int,
        heightCm: Double,
        sex: String?
    ) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.save(
                current.copy(
                    firstName = firstName.trim(),
                    lastName = lastName.trim()
                )
            )
            val existing = dashboardState.value.profile
            repository.saveProfileRecordingTargets(
                profile = UserProfile(
                    id = 1,
                    age = age,
                    weightKg = existing?.weightKg ?: 0.0,
                    heightCm = heightCm,
                    dailyTargetCalories = existing?.dailyTargetCalories
                        ?: DashboardUiState.DEFAULT_TARGET_CALORIES,
                    targetProteinG = existing?.targetProteinG
                        ?: DashboardUiState.DEFAULT_TARGET_PROTEIN,
                    targetCarbsG = existing?.targetCarbsG
                        ?: DashboardUiState.DEFAULT_TARGET_CARBS,
                    targetFatsG = existing?.targetFatsG
                        ?: DashboardUiState.DEFAULT_TARGET_FATS,
                    lastUpdatedTimestamp = System.currentTimeMillis(),
                    sex = sex,
                    goal = existing?.goal ?: "RECOMP",
                    activityLevel = existing?.activityLevel ?: "MODERATE",
                    goalRationale = existing?.goalRationale,
                    nutritionTargetHistory = existing?.nutritionTargetHistory.orEmpty()
                ),
                effectiveFromDate = DateUtils.today(),
                recordNutritionTargets = false
            )
            _analysisState.update { it.copy(userMessage = str(R.string.vm_profile_saved)) }
        }
    }

    /**
     * Called when the main UI becomes active after onboarding. Calculates calorie/macro targets
     * once on-device and optionally asks AI to personalize the explanation.
     */
    fun onDashboardLaunched() {
        if (!pendingInitialTargetDesign) return
        pendingInitialTargetDesign = false
        viewModelScope.launch {
            val profile = repository.activeProfile
                .mapNotNull { p -> p?.takeIf { it.hasBasicsConfigured() } }
                .first()
            _analysisState.update { it.copy(userMessage = str(R.string.vm_calculating_science_based_targets)) }
            runCatching {
                val planning = buildTargetPlanningContext(
                    age = profile.age,
                    heightCm = profile.heightCm,
                    weightKg = profile.weightKg,
                    sex = profile.sex,
                    activityLevel = profile.activityLevel,
                    goal = profile.goal
                )
                val context = buildTargetContext(
                    age = profile.age,
                    heightCm = profile.heightCm,
                    weightKg = profile.weightKg,
                    sex = profile.sex,
                    activityLevel = profile.activityLevel,
                    goal = profile.goal,
                    planning = planning
                )
                repository.designTargets(context, planning.options)
            }
                .onSuccess { plan ->
                    repository.saveProfileRecordingTargets(
                        profile = profile.copy(
                            dailyTargetCalories = plan.dailyTargetCalories,
                            targetProteinG = plan.targetProteinG,
                            targetCarbsG = plan.targetCarbsG,
                            targetFatsG = plan.targetFatsG,
                            goal = plan.recommendedGoal.ifBlank { profile.goal },
                            goalRationale = plan.rationale,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        ),
                        effectiveFromDate = DateUtils.today(),
                        recordNutritionTargets = true
                    )
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_science_based_targets_ready))
                    }
                }
                .onFailure { e ->
                    _analysisState.update {
                        it.copy(
                            userMessage = e.message
                                ?: str(R.string.vm_using_default_targets_refine_anytime_body)
                        )
                    }
                }
        }
    }

    /**
     * After FitBuddy cloud/file restore: force AI-only onboarding when keys are absent or
     * the provider rejects them. Transient network/host errors keep restored keys and let
     * the user continue (they can fix connectivity in Settings).
     */
    private suspend fun applyRestoredAiCredentialGate(after: AppSettings) {
        if (!after.isConfigured) {
            _forceAiSetup.value = true
            return
        }
        runCatching { probeAiCredentials(after) }
            .onSuccess { _forceAiSetup.value = false }
            .onFailure { e ->
                _forceAiSetup.value = isCredentialAuthFailure(e)
            }
    }

    /** Hits the provider's model-list endpoint to confirm credentials work. */
    private suspend fun probeAiCredentials(settings: AppSettings) {
        when (settings.provider) {
            AiProvider.OPENROUTER -> {
                val key = settings.activeKey(AiProvider.OPENROUTER)
                    .ifBlank { settings.openRouterOAuthKey }
                check(key.isNotBlank()) { "Connect OpenRouter or enter an API key" }
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
            AiProvider.CUSTOM -> {
                val base = settings.customEffectiveBaseUrl
                check(base.isNotBlank()) { "Enter your custom server URL" }
                val key = settings.activeKey(AiProvider.CUSTOM)
                repository.fetchOllamaVisionModels(
                    base, key, ladderProvider = AiProvider.CUSTOM
                )
            }
            AiProvider.OPENAI -> {
                val key = settings.activeKey(AiProvider.OPENAI)
                check(key.isNotBlank()) { "Enter an OpenAI API key" }
                repository.fetchOpenAiVisionModels(key)
            }
        }
    }

    private fun isCredentialAuthFailure(error: Throwable): Boolean {
        val message = (error.message ?: "").lowercase()
        return message.contains("authentication failed") ||
            message.contains("unauthorized") ||
            message.contains("http 401") ||
            message.contains("http 403")
    }

    fun saveProfile(
        goal: String = "RECOMP",
        activityLevel: String = "MODERATE"
    ) {
        viewModelScope.launch {
            runCatching {
                check(
                    repository.updateBodyProfile(
                        goal = goal,
                        activityLevel = activityLevel
                    )
                ) { str(R.string.vm_complete_profile_before_saving_body_data) }
            }
                .onSuccess {
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_body_saved_dashboard_recalibrated))
                    }
                }
                .onFailure { error ->
                    _analysisState.update {
                        it.copy(userMessage = error.message ?: str(R.string.vm_couldn_t_save_body_data))
                    }
                }
        }
    }

    fun saveDailyTargets(
        dailyTargetCalories: Int,
        targetProteinG: Int,
        targetCarbsG: Int,
        targetFatsG: Int
    ) {
        if (
            dailyTargetCalories <= 0 || targetProteinG <= 0 ||
            targetCarbsG < 0 || targetFatsG <= 0
        ) {
            _analysisState.update { it.copy(userMessage = str(R.string.vm_enter_valid_daily_targets)) }
            return
        }
        manualTargetsOverride = true
        viewModelScope.launch {
            runCatching {
                check(
                    repository.updateDailyTargets(
                        dailyTargetCalories = dailyTargetCalories,
                        targetProteinG = targetProteinG,
                        targetCarbsG = targetCarbsG,
                        targetFatsG = targetFatsG,
                        effectiveFromDate = DateUtils.today()
                    )
                ) { str(R.string.vm_complete_body_profile_before_saving_targets) }
                repository.activeProfile.first { profile ->
                    profile != null && profile.goalRationale == null
                }
            }
                .onSuccess {
                    manualTargetsOverride = false
                    _analysisState.update {
                        it.copy(userMessage = str(R.string.vm_manual_targets_saved_dashboard_recalibrated))
                    }
                }
                .onFailure { error ->
                    manualTargetsOverride = false
                    _analysisState.update {
                        it.copy(userMessage = error.message ?: str(R.string.vm_couldn_t_save_manual_targets))
                    }
                }
        }
    }

    /**
     * The "User State Context" block (Step 3) sent to the AI so estimates can be personalised
     * (e.g. body weight strongly affects calories burned during exercise).
     * Includes optional Health Connect hydration/sleep when those import toggles are on.
     */
    private suspend fun buildUserStateContext(): String {
        val state = dashboardState.value
        val profile = state.profile
        val latest = latestMeasurement.value
        val settingsNow = settings.value
        val wellness = if (
            settingsNow.healthConnectEnabled &&
            (settingsNow.healthConnectMetrics.importHydration ||
                settingsNow.healthConnectMetrics.importSleep)
        ) {
            repository.healthConnectWellnessContext()
        } else {
            null
        }
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
            if (wellness != null) {
                if (
                    settingsNow.healthConnectMetrics.importHydration &&
                    wellness.hydrationMlToday != null
                ) {
                    put("hydration_ml_today", wellness.hydrationMlToday)
                    put("hydration_date", wellness.hydrationDate ?: JSONObject.NULL)
                }
                if (
                    settingsNow.healthConnectMetrics.importSleep &&
                    wellness.sleepMinutesLastNight != null
                ) {
                    put("sleep_minutes_last_night", wellness.sleepMinutesLastNight)
                    put("sleep_ended_date", wellness.sleepEndedDate ?: JSONObject.NULL)
                }
            }
        }.toString()
    }

    private suspend fun recentActivityRecommendation(): ActivityLevelRecommendation? {
        val endDate = _realToday.value.ifBlank { DateUtils.today() }
        val startDate = DateUtils.addDays(endDate, -(ACTIVITY_RECOMMENDATION_DAYS - 1))
        val summaries = repository.getExerciseSummariesBetween(startDate, endDate).first()
        return ActivityLevelRecommender.recommend(
            windowDays = ACTIVITY_RECOMMENDATION_DAYS,
            workoutDays = summaries.size,
            workoutCount = summaries.sumOf { it.workoutCount },
            totalDurationMinutes = summaries.sumOf { it.totalDurationMinutes }
        )
    }

    private suspend fun recentBodyTrend(): BodyTrendEvidence {
        val today = LocalDate.parse(_realToday.value.ifBlank { DateUtils.today() })
        val readings = repository.getAllBodyMeasurementsOnce().mapNotNull { measurement ->
            runCatching {
                BodyTrendReading(
                    epochDay = LocalDate.parse(measurement.dateString).toEpochDay(),
                    weightKg = measurement.weightKg,
                    bodyFatPct = measurement.bodyFatPct,
                    muscleMassKg = measurement.muscleMassKg
                )
            }.getOrNull()
        }
        return BodyTrendAnalyzer.analyze(readings, today.toEpochDay())
    }

    private suspend fun recentNutritionTrend(): NutritionTrendEvidence {
        val today = _realToday.value.ifBlank { DateUtils.today() }
        val endDate = DateUtils.addDays(today, -1)
        val startDate = DateUtils.addDays(endDate, -(TARGET_NUTRITION_WINDOW_DAYS - 1))
        val summaries = repository.getFoodSummariesBetween(startDate, endDate).first()
        val profile = dashboardState.value.profile
        val fallback = profile?.currentTargetPeriod()
        val averageTarget = NutritionTargetHistory.averageTargetCalories(
            history = profile?.nutritionTargetHistory.orEmpty(),
            dates = summaries.map { it.dateString },
            fallback = fallback
        )
        return NutritionTrendEvidence(
            windowDays = TARGET_NUTRITION_WINDOW_DAYS,
            loggedDays = summaries.size,
            averageCalories = summaries.takeIf { it.isNotEmpty() }
                ?.map { it.totalCalories }
                ?.average()
                ?.roundToInt(),
            averageTargetCalories = averageTarget
        )
    }

    private suspend fun buildTargetPlanningContext(
        age: Int,
        heightCm: Double,
        weightKg: Double,
        sex: String?,
        activityLevel: String,
        goal: String,
        activityRecommendation: ActivityLevelRecommendation? = null,
        forceRecalculation: Boolean = false
    ): TargetPlanningContext {
        val effectiveActivityLevel = activityRecommendation?.level ?: activityLevel
        val input = targetInput(
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            sex = sex,
            activityLevel = effectiveActivityLevel,
            goal = goal,
            forceRecalculation = forceRecalculation
        )
        val basePlan = HealthTargetCalculator.calculate(input)
            .withActivityRecommendation(activityRecommendation)
        val bodyTrend = recentBodyTrend()
        val nutritionTrend = recentNutritionTrend()
        return TargetPlanningContext(
            options = TargetPlanPersonalizer.buildOptions(
                basePlan = basePlan,
                input = input,
                bodyTrend = bodyTrend,
                nutritionTrend = nutritionTrend
            ),
            bodyTrend = bodyTrend,
            nutritionTrend = nutritionTrend
        )
    }

    private fun TargetPlanResponse.withActivityRecommendation(
        recommendation: ActivityLevelRecommendation?
    ): TargetPlanResponse = copy(
        recommendedActivityLevel = recommendation?.level,
        activityWindowDays = recommendation?.windowDays,
        activityWorkoutDays = recommendation?.workoutDays,
        activityWorkoutCount = recommendation?.workoutCount,
        activityWeeklyMinutes = recommendation?.weeklyMinutes
    )

    private fun targetInput(
        age: Int,
        heightCm: Double,
        weightKg: Double,
        sex: String?,
        activityLevel: String,
        goal: String,
        forceRecalculation: Boolean
    ): HealthTargetInput {
        val current = dashboardState.value
        val savedRationale = current.profile?.goalRationale
        val currentTargetsCalculated = !manualTargetsOverride && !forceRecalculation &&
            savedRationale?.startsWith("Evidence-based v3:") == true &&
            savedRationale.contains(str(R.string.vm_trend_adjusted_locally)).not()
        return HealthTargetInput(
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            sex = sex,
            activityLevel = activityLevel,
            statedGoal = goal,
            currentCalories = if (currentTargetsCalculated) current.targetCalories else 0,
            currentProteinG = if (currentTargetsCalculated) current.targetProtein else 0,
            currentCarbsG = if (currentTargetsCalculated) current.targetCarbs else 0,
            currentFatsG = if (currentTargetsCalculated) current.targetFats else 0,
            currentTargetsCalculated = currentTargetsCalculated
        )
    }

    private fun buildTargetContext(
        age: Int,
        heightCm: Double,
        weightKg: Double,
        sex: String?,
        activityLevel: String,
        goal: String,
        planning: TargetPlanningContext
    ): String = JSONObject().apply {
        val currentRationale = dashboardState.value.profile?.goalRationale
        val currentTargetsCalculated = !manualTargetsOverride &&
            currentRationale?.startsWith("Evidence-based v3:") == true &&
            currentRationale.contains(str(R.string.vm_trend_adjusted_locally)).not()
        put("age", if (age > 0) age else JSONObject.NULL)
        put("sex", sex ?: JSONObject.NULL)
        put("height_cm", if (heightCm > 0) heightCm else JSONObject.NULL)
        put("current_weight_kg", if (weightKg > 0) weightKg else JSONObject.NULL)
        put("activity_level", activityLevel)
        put("stated_goal", goal.ifBlank { "AUTO" })
        put(
            "current_target_calories",
            if (currentTargetsCalculated) dashboardState.value.targetCalories else JSONObject.NULL
        )
        put(
            "current_target_protein_g",
            if (currentTargetsCalculated) dashboardState.value.targetProtein else JSONObject.NULL
        )
        put("body_trend", JSONObject().apply {
            put("status", planning.bodyTrend.quality.name)
            put("reason", planning.bodyTrend.reason)
            put("window_days", planning.bodyTrend.windowDays)
            put("measurement_days", planning.bodyTrend.sampleCount)
            put("span_days", planning.bodyTrend.spanDays)
            put("latest_age_days", planning.bodyTrend.latestAgeDays ?: JSONObject.NULL)
            put("weight_change_kg", planning.bodyTrend.weightChangeKg ?: JSONObject.NULL)
            put(
                "weight_change_per_week_pct",
                planning.bodyTrend.weightChangePerWeekPct ?: JSONObject.NULL
            )
            put(
                "body_fat_change_percentage_points",
                planning.bodyTrend.bodyFatChangePct ?: JSONObject.NULL
            )
            put(
                "muscle_mass_change_kg",
                planning.bodyTrend.muscleMassChangeKg ?: JSONObject.NULL
            )
        })
        put("nutrition_coverage", JSONObject().apply {
            put("window_days", planning.nutritionTrend.windowDays)
            put("days_logged", planning.nutritionTrend.loggedDays)
            put("average_calories", planning.nutritionTrend.averageCalories ?: JSONObject.NULL)
            put(
                "average_target_calories",
                planning.nutritionTrend.averageTargetCalories ?: JSONObject.NULL
            )
            put("sufficient", planning.nutritionTrend.hasSufficientCoverage)
        })
        put("target_history", targetHistoryJson(dashboardState.value.profile))
        put("default_candidate_id", planning.options.defaultCandidateId)
        put("safe_candidates", JSONArray().apply {
            planning.options.candidates.forEach { candidate ->
                put(targetCandidateJson(candidate.id, candidate.plan))
            }
        })
    }.toString()

    private fun targetCandidateJson(id: String, plan: TargetPlanResponse): JSONObject =
        JSONObject().apply {
            put("candidate_id", id)
            put("recommended_goal", plan.recommendedGoal)
            put("daily_target_calories", plan.dailyTargetCalories)
            put("target_protein_g", plan.targetProteinG)
            put("target_carbs_g", plan.targetCarbsG)
            put("target_fats_g", plan.targetFatsG)
            put("targets_changed", plan.targetsChanged)
            put("recommended_activity_level", plan.recommendedActivityLevel ?: JSONObject.NULL)
            put("activity_window_days", plan.activityWindowDays ?: JSONObject.NULL)
            put("activity_workout_days", plan.activityWorkoutDays ?: JSONObject.NULL)
            put("activity_workout_count", plan.activityWorkoutCount ?: JSONObject.NULL)
            put("activity_weekly_minutes", plan.activityWeeklyMinutes ?: JSONObject.NULL)
            put("trend_adjustment_calories", plan.bodyTrendAdjustmentCalories)
        }

    /**
     * Body-composition series + nutrition/exercise trends for the AI progress insight.
     * Past ~30 days are day-level detail; older history is rolled into concise calendar-month
     * averages so the prompt stays token-efficient.
     */
    private suspend fun buildProgressContext(): String {
        val profile = dashboardState.value.profile
        val today = _realToday.value.ifBlank { DateUtils.today() }
        // Exclude today's (incomplete) data — insight considers readings till yesterday only.
        val yesterday = DateUtils.addDays(today, -1)
        // Inclusive 30-day window ending yesterday.
        val detailCutoff = DateUtils.addDays(yesterday, -(PROGRESS_DETAIL_DAYS - 1))

        val allFood = repository.getAllFoodDailySummaries()
            .filter { it.dateString <= yesterday }
        val allExercise = repository.getAllExerciseDailySummaries()
            .filter { it.dateString <= yesterday }
        val allMeasurements = repository.getAllBodyMeasurementsOnce()
            .filter { it.dateString <= yesterday }

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
        val dailyFoodSeries = buildNutritionSeries(recentFood, burnedByDate, profile)
        val priorFoodMonths = buildPriorMonthNutritionSeries(olderFood, burnedByDate, profile)
        val dailyExerciseSeries = buildExerciseSeries(recentExercise)
        val priorExerciseMonths = buildPriorMonthExerciseSeries(olderExercise)

        val avgIntake = recentFood.takeIf { it.isNotEmpty() }
            ?.map { it.totalCalories }
            ?.average()

        val firstName = settings.value.displayFirstName
        return JSONObject().apply {
            put(
                "first_name",
                firstName.takeIf { it.isNotEmpty() } ?: JSONObject.NULL
            )
            put("age", profile?.age?.takeIf { it > 0 } ?: JSONObject.NULL)
            put("sex", profile?.sex?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            put("height_cm", profile?.heightCm?.takeIf { it > 0 } ?: JSONObject.NULL)
            put("weight_kg", profile?.weightKg?.takeIf { it > 0 } ?: JSONObject.NULL)
            put("activity_level", profile?.activityLevel ?: JSONObject.NULL)
            put("goal", profile?.goal ?: JSONObject.NULL)
            put("target_daily_calories", profile?.dailyTargetCalories
                ?: DashboardUiState.DEFAULT_TARGET_CALORIES)
            put("target_protein_g", profile?.targetProteinG
                ?: DashboardUiState.DEFAULT_TARGET_PROTEIN)
            put("target_carbs_g", profile?.targetCarbsG
                ?: DashboardUiState.DEFAULT_TARGET_CARBS)
            put("target_fats_g", profile?.targetFatsG
                ?: DashboardUiState.DEFAULT_TARGET_FATS)
            put("target_history", targetHistoryJson(profile))
            put(
                "calorie_model_note",
                "The daily target is a full-day TDEE estimate that already includes the selected " +
                    "average activity level. Compare it directly with calories eaten; logged " +
                    "exercise is tracked separately and is not credited back a second time. " +
                    "target_history / per-day target_* fields are the targets that were in force " +
                    "on each date; header target_* values are the current live targets."
            )
            put(
                "metrics_granularity_note",
                "nutrition_daily / exercise_daily / body_measurements cover the past " +
                    "$PROGRESS_DETAIL_DAYS days up to yesterday (today's incomplete data " +
                    "is excluded). nutrition_prior_months / exercise_prior_months / " +
                    "body_prior_months summarise older history as calendar-month averages " +
                    "(omit if empty)."
            )
            put("avg_daily_calories_eaten_recent", avgIntake ?: JSONObject.NULL)
            put("body_measurements", measurementSeries)
            put("body_prior_months", priorBodyMonths)
            put("nutrition_daily", dailyFoodSeries)
            put("nutrition_prior_months", priorFoodMonths)
            put("exercise_daily", dailyExerciseSeries)
            put("exercise_prior_months", priorExerciseMonths)
        }.toString()
    }

    private fun targetHistoryJson(profile: UserProfile?): JSONArray {
        val history = NutritionTargetHistory.ensureSeeded(
            history = profile?.nutritionTargetHistory.orEmpty(),
            fallbackCurrent = profile?.currentTargetPeriod()
        )
        return JSONArray().apply {
            history.forEach { period ->
                put(JSONObject().apply {
                    put("effective_from", period.from)
                    put("calories", period.kcal)
                    put("protein_g", period.proteinG)
                    put("carbs_g", period.carbsG)
                    put("fats_g", period.fatsG)
                })
            }
        }
    }

    private fun buildNutritionSeries(
        food: List<FoodDailySummary>,
        burnedByDate: Map<String, Int>,
        profile: UserProfile?
    ): JSONArray = JSONArray().apply {
        food.asReversed().forEach { s ->
            val burned = burnedByDate[s.dateString] ?: 0
            val dayTarget = profile?.targetsForDate(s.dateString)
            put(JSONObject().apply {
                put("date", s.dateString)
                put("calories", s.totalCalories)
                put("calories_burned", burned)
                put("net_calories", s.totalCalories - burned)
                put("protein_g", s.totalProtein)
                put("carbs_g", s.totalCarbs)
                put("fats_g", s.totalFats)
                put(
                    "target_calories",
                    dayTarget?.kcal ?: DashboardUiState.DEFAULT_TARGET_CALORIES
                )
                put(
                    "target_protein_g",
                    dayTarget?.proteinG ?: DashboardUiState.DEFAULT_TARGET_PROTEIN
                )
                put(
                    "target_carbs_g",
                    dayTarget?.carbsG ?: DashboardUiState.DEFAULT_TARGET_CARBS
                )
                put(
                    "target_fats_g",
                    dayTarget?.fatsG ?: DashboardUiState.DEFAULT_TARGET_FATS
                )
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
        burnedByDate: Map<String, Int>,
        profile: UserProfile?
    ): JSONArray {
        if (food.isEmpty()) return JSONArray()
        val byMonth = food.groupBy { it.dateString.take(7) }.toSortedMap()
        return JSONArray().apply {
            byMonth.forEach { (month, days) ->
                val n = days.size
                val burned = days.map { burnedByDate[it.dateString] ?: 0 }
                val dayTargets = days.map {
                    profile?.targetsForDate(it.dateString)?.kcal
                        ?: DashboardUiState.DEFAULT_TARGET_CALORIES
                }
                put(JSONObject().apply {
                    put("month", month)
                    put("days_logged", n)
                    put("avg_calories", days.map { it.totalCalories }.average().roundToInt())
                    put("avg_target_calories", dayTargets.average().roundToInt())
                    put("avg_protein_g", days.map { it.totalProtein }.average().roundMacro())
                    put("avg_carbs_g", days.map { it.totalCarbs }.average().roundMacro())
                    put("avg_fats_g", days.map { it.totalFats }.average().roundMacro())
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
                    newest.bmr?.let { put("end_bmr", it) }
                    newest.muscleMassKg?.let { put("end_muscle_mass_kg", it) }
                })
            }
        }
    }

    /** Serializes the four supported body metrics for AI context. */
    private fun measurementJson(m: BodyMeasurement): JSONObject = JSONObject().apply {
        put("date", m.dateString)
        put("weight_kg", m.weightKg)
        m.bodyFatPct?.let { put("body_fat_pct", it) }
        m.bmr?.let { put("bmr", it) }
        m.muscleMassKg?.let { put("muscle_mass_kg", it) }
    }

    companion object {
        private const val ACTIVITY_RECOMMENDATION_DAYS = 28
        private const val TARGET_NUTRITION_WINDOW_DAYS = 28
        /** Calendar days of day-level nutrition/exercise/body detail for AI insights. */
        private const val PROGRESS_DETAIL_DAYS = 30
    }
}

/** Manual ViewModel factory (no DI framework); wires the shared repositories in. */
class MainViewModelFactory(
    private val app: Application,
    private val repository: FitnessRepository,
    private val settingsRepository: SettingsRepository,
    private val updateChecker: UpdateChecker
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MainViewModel(app, repository, settingsRepository, updateChecker) as T
    }
}
