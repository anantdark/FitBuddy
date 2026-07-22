package com.anant.fitbuddy.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import com.anant.fitbuddy.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.anant.fitbuddy.ui.components.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateListOf
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.model.FoodEntryDraft
import com.anant.fitbuddy.data.model.IngredientDraft
import com.anant.fitbuddy.data.model.MealDraft
import com.anant.fitbuddy.data.model.ScannedProduct
import com.anant.fitbuddy.data.model.snapshot
import com.anant.fitbuddy.data.model.toFoodDraft
import com.anant.fitbuddy.data.model.toFoodEntry
import com.anant.fitbuddy.ui.components.AnantEasterEggDialog
import com.anant.fitbuddy.ui.components.FitBuddyLivePill
import com.anant.fitbuddy.ui.components.FitBuddyPillConfig
import com.anant.fitbuddy.ui.components.FitBuddySnackbarHost
import com.anant.fitbuddy.ui.components.showFitBuddyPill
import com.anant.fitbuddy.ui.util.rememberDismissKeyboard
import com.anant.fitbuddy.ui.viewmodel.MainViewModel
import com.anant.fitbuddy.util.ApkInstaller
import com.anant.fitbuddy.util.ImageUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PROGRESS("Progress", Icons.Filled.Insights),
    DASHBOARD("Dashboard", Icons.Filled.Dashboard),
    BODY("Body", Icons.Filled.MonitorWeight)
}

private enum class ScanFlow { SAVE_FOOD, ADD_TO_MEAL }

/** Where a [FoodReviewDialog] confirmation should route the edited food. */
private enum class FoodEditorTarget {
    /** AI / photo flow — log as a one-food meal. */
    LOG_MEAL,
    /** Add or replace a row in the meal builder. */
    MEAL_BUILDER,
    /** Replace a row in the meal review dialog. */
    MEAL_REVIEW
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    openLogHubRequest: Boolean = false,
    onOpenLogHubConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val dismissKeyboard = rememberDismissKeyboard()

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val weekDates by viewModel.weekDates.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val realToday by viewModel.realToday.collectAsStateWithLifecycle()
    val weekSnapshots by viewModel.weekSnapshots.collectAsStateWithLifecycle()
    val cloneMealRequest by viewModel.cloneMealRequest.collectAsStateWithLifecycle()

    val weeklyFood by viewModel.weeklyFood.collectAsStateWithLifecycle()
    val monthlyFood by viewModel.monthlyFood.collectAsStateWithLifecycle()
    val weeklyExercise by viewModel.weeklyExercise.collectAsStateWithLifecycle()
    val monthlyExercise by viewModel.monthlyExercise.collectAsStateWithLifecycle()
    val analyticsMonthYm by viewModel.analyticsMonthYm.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasSettingsSnapshot by viewModel.hasSettingsSnapshot.collectAsStateWithLifecycle()
    val modelsState by viewModel.models.collectAsStateWithLifecycle()
    val textModelsState by viewModel.textModels.collectAsStateWithLifecycle()
    val openRouterOAuthBusy by viewModel.openRouterOAuthBusy.collectAsStateWithLifecycle()
    val mongoBackupBusy by viewModel.mongoBackupBusy.collectAsStateWithLifecycle()
    val mealPresets by viewModel.mealPresets.collectAsStateWithLifecycle()
    val savedFoods by viewModel.savedFoods.collectAsStateWithLifecycle()
    val measurements by viewModel.bodyMeasurements.collectAsStateWithLifecycle()
    val latestMeasurement by viewModel.latestMeasurement.collectAsStateWithLifecycle()
    val targetPlanState by viewModel.targetPlanState.collectAsStateWithLifecycle()
    val progressInsightState by viewModel.progressInsightState.collectAsStateWithLifecycle()
    val isAiOnline by viewModel.isAiOnline.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val workoutLogState by viewModel.workoutLogState.collectAsStateWithLifecycle()
    val editingWorkout by viewModel.editingWorkout.collectAsStateWithLifecycle()
    val exercisePickerExercises by viewModel.exercisePickerExercises.collectAsStateWithLifecycle()
    val customExerciseClassifying by viewModel.customExerciseClassifying.collectAsStateWithLifecycle()
    val workoutInferring by viewModel.workoutInferring.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(Tab.DASHBOARD) }
    // Tabs are composed once on first visit and then kept alive (just hidden) so switching back
    // and forth is instant and doesn't lose scroll/animation/unsaved-edit state or re-run
    // expensive first-composition work (e.g. the Progress tab's charts) every time.
    var visitedTabs by remember { mutableStateOf(setOf(selectedTab)) }
    LaunchedEffect(selectedTab) {
        visitedTabs = visitedTabs + selectedTab
        if (selectedTab == Tab.DASHBOARD) {
            viewModel.refreshToToday()
        }
    }
    LaunchedEffect(Unit) { viewModel.onDashboardLaunched() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshToToday()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showSettings by remember { mutableStateOf(false) }
    var showProgressChat by remember { mutableStateOf(false) }
    var showWeekHistory by remember { mutableStateOf(false) }
    var showLogHub by remember { mutableStateOf(false) }
    LaunchedEffect(openLogHubRequest) {
        if (openLogHubRequest) {
            showLogHub = true
            onOpenLogHubConsumed()
        }
    }
    var showTextDialog by remember { mutableStateOf(false) }
    var showMealPresetSheet by remember { mutableStateOf(false) }
    var showSavedFoodSheet by remember { mutableStateOf(false) }
    var savedFoodSheetMode by remember { mutableStateOf(SavedFoodSheetMode.PICK_FOR_MEAL) }
    var showSavedFoodManageSheet by remember { mutableStateOf(false) }
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var showMealBuilder by remember { mutableStateOf(false) }
    var showBarcodeScan by remember { mutableStateOf(false) }
    var scanFlow by remember { mutableStateOf<ScanFlow?>(null) }
    var pendingProduct by remember { mutableStateOf<ScannedProduct?>(null) }
    val mealItems = remember { mutableStateListOf<FoodEntryDraft>() }
    var mealBuilderInitial by remember { mutableStateOf<MealDraft?>(null) }
    var foodEditorTarget by remember { mutableStateOf(FoodEditorTarget.LOG_MEAL) }
    var mealFoodEditIndex by remember { mutableStateOf<Int?>(null) }
    var mealReviewDraft by remember { mutableStateOf<MealDraft?>(null) }
    /** True when meal review came from Build a meal (save preset, do not log today). */
    var mealReviewSavesAsPreset by remember { mutableStateOf(false) }
    var lastOpenedEditMealId by remember { mutableStateOf<Int?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    // Export password dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var exportConfirmPassword by remember { mutableStateOf("") }
    // Import password dialog state (for encrypted backups)
    var importPasswordPromptUri by remember { mutableStateOf<Uri?>(null) }
    var importPassword by remember { mutableStateOf("") }
    var importPasswordError by remember { mutableStateOf<String?>(null) }
    var importPasswordAttempts by remember { mutableStateOf(0) }
    var importPasswordContinuation by remember {
        mutableStateOf<CancellableContinuation<CharArray?>?>(null)
    }
    // Cloud restore password dialog state (shown only when Support-ID decryption fails)
    var cloudRestorePasswordPrompt by remember { mutableStateOf(false) }
    var cloudRestorePassword by remember { mutableStateOf("") }
    var cloudRestorePasswordError by remember { mutableStateOf<String?>(null) }
    var cloudRestorePasswordContinuation by remember {
        mutableStateOf<CancellableContinuation<CharArray?>?>(null)
    }
    var livePillMessage by remember { mutableStateOf<String?>(null) }
    var showAnantEasterEgg by remember { mutableStateOf(false) }

    LaunchedEffect(cloneMealRequest) {
        val draft = cloneMealRequest ?: return@LaunchedEffect
        mealReviewSavesAsPreset = false
        mealReviewDraft = draft.snapshot()
        viewModel.consumeCloneMealRequest()
    }

    val editingMealId = analysisState.editingFoodLogId
    LaunchedEffect(editingMealId, analysisState.mealDraft) {
        val id = editingMealId ?: run {
            lastOpenedEditMealId = null
            return@LaunchedEffect
        }
        if (id == lastOpenedEditMealId) return@LaunchedEffect
        val draft = analysisState.mealDraft ?: return@LaunchedEffect
        lastOpenedEditMealId = id
        mealBuilderInitial = draft
        mealItems.clear()
        mealItems.addAll(draft.foods)
        showMealBuilder = true
    }

    LaunchedEffect(analysisState.mealDraft) {
        val vmDraft = analysisState.mealDraft ?: return@LaunchedEffect
        if (mealReviewDraft != null) {
            mealReviewDraft = vmDraft.snapshot()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // Decode/scale/compress off the main thread to avoid dropping frames.
            scope.launch(Dispatchers.Default) {
                val bytes = ImageUtils.bitmapToJpeg(bitmap)
                foodEditorTarget = FoodEditorTarget.LOG_MEAL
                mealFoodEditIndex = null
                viewModel.analyzeImage(bytes)
            }
        }
    }

    // TakePicturePreview starts IMAGE_CAPTURE, which requires CAMERA at runtime on
    // Android 6+. Launching without it crashes with SecurityException (permission denial).
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            scope.launch {
                snackbarHostState.showFitBuddyPill(
                    "Camera permission not allowed."
                )
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.Default) {
                foodEditorTarget = FoodEditorTarget.LOG_MEAL
                mealFoodEditIndex = null
                ImageUtils.uriToJpeg(context, uri)?.let { viewModel.analyzeImage(it) }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val pw = exportPassword
            val password = if (pw.isNotEmpty()) pw.toCharArray() else null
            viewModel.exportData(uri, password)
            exportPassword = ""
            exportConfirmPassword = ""
        }
        showExportDialog = false
    }

    val updateBackupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupForUpdate(uri)
        } else {
            viewModel.cancelBackupFilePick()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { pendingImportUri = it } }

    // Surface transient outcomes (logged / errors) as a snackbar, then clear.
    LaunchedEffect(analysisState.userMessage) {
        analysisState.userMessage?.let { message ->
            snackbarHostState.showFitBuddyPill(message)
            viewModel.consumeUserMessage()
        }
    }

    LaunchedEffect(livePillMessage) {
        val message = livePillMessage ?: return@LaunchedEffect
        delay(FitBuddyPillConfig.DISPLAY_MS)
        if (livePillMessage == message) {
            livePillMessage = null
        }
    }

    // One delayed silent update check per process when the toggle is on.
    var startupUpdateChecked by remember { mutableStateOf(false) }
    LaunchedEffect(hasSettingsSnapshot, settings.autoCheckUpdates) {
        if (!hasSettingsSnapshot || !settings.autoCheckUpdates || startupUpdateChecked) return@LaunchedEffect
        startupUpdateChecked = true
        delay(1_500)
        viewModel.checkForUpdates(BuildConfig.VERSION_CODE, silent = true)
    }

    if (showSettings) {
        BackHandler { showSettings = false }
        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            snackbarHost = { FitBuddySnackbarHost(snackbarHostState, bottomPadding = 24.dp) }
        ) { innerPadding ->
            SettingsScreen(
                settings = settings,
                profile = dashboardState.profile,
                modelsState = modelsState,
                textModelsState = textModelsState,
                onLoadModels = { provider, apiKey, force, baseUrl, includePaid ->
                    viewModel.loadFreeVisionModels(provider, apiKey, force, baseUrl, includePaid)
                },
                onLoadTextModels = { provider, apiKey, force, baseUrl, includePaid ->
                    viewModel.loadFreeTextModels(provider, apiKey, force, baseUrl, includePaid)
                },
                onSave = { viewModel.saveSettings(it, announce = true) },
                onSaveQuiet = { viewModel.saveSettings(it, announce = false) },
                onSavePermanentProfile = viewModel::savePermanentProfile,
                onConnectOpenRouter = viewModel::startOpenRouterOAuth,
                onDisconnectOpenRouter = viewModel::disconnectOpenRouterOAuth,
                openRouterOAuthBusy = openRouterOAuthBusy,
                onDynamicColorChange = { enabled ->
                    viewModel.saveSettings(settings.copy(dynamicColor = enabled))
                },
                onExport = { showExportDialog = true },
                onImport = { importLauncher.launch(arrayOf("application/json")) },
                onEasterEggTriggered = {
                    livePillMessage = null
                    snackbarHostState.currentSnackbarData?.dismiss()
                    showAnantEasterEgg = true
                    viewModel.markEasterEggDiscovered()
                },
                onAnantTapHint = { remaining ->
                    livePillMessage = "$remaining taps to go"
                },
                onAnantTapHintDismiss = { livePillMessage = null },
                onAnantTapWhenUnlocked = {
                    scope.launch {
                        snackbarHostState.showFitBuddyPill("Don't be greedy")
                    }
                },
                onResetEasterEggData = {
                    viewModel.resetEasterEggData()
                    livePillMessage = null
                    showAnantEasterEgg = false
                    snackbarHostState.currentSnackbarData?.dismiss()
                },
                updateState = updateState,
                onCheckForUpdates = { viewModel.checkForUpdates(BuildConfig.VERSION_CODE) },
                onAutoCheckUpdatesChange = { enabled ->
                    viewModel.saveSettings(settings.copy(autoCheckUpdates = enabled))
                },
                onCrashReportingChange = { enabled ->
                    viewModel.setCrashReportingEnabled(enabled)
                },
                onHeartDoubleTapHeartbeat = viewModel::sendHeartbeatFromLoveTap,
                onSupportIdCopied = {
                    scope.launch {
                        snackbarHostState.showFitBuddyPill("Support ID copied")
                    }
                },
                onDeveloperUnlockHint = { remaining ->
                    livePillMessage = "$remaining taps to go"
                },
                onDeveloperUnlockHintDismiss = { livePillMessage = null },
                onDeveloperModeToggled = { unlocked ->
                    livePillMessage = null
                    viewModel.setDeveloperModeUnlocked(unlocked)
                    scope.launch {
                        snackbarHostState.showFitBuddyPill(
                            if (unlocked) "Developer settings unlocked"
                            else "Developer settings hidden"
                        )
                    }
                },
                onClearModelCooldowns = viewModel::clearModelCooldowns,
                onShowTestUpdatePrompt = viewModel::showTestUpdatePrompt,
                onTestNotificationSent = { ok ->
                    scope.launch {
                        snackbarHostState.showFitBuddyPill(
                            if (ok) "Test notification sent" else "Couldn't send notification"
                        )
                    }
                },
                onPermissionDenied = { message ->
                    scope.launch {
                        snackbarHostState.showFitBuddyPill(message)
                    }
                },
                mongoBackupBusy = mongoBackupBusy,
                onCloudBackupEnabledChange = viewModel::setCloudBackupEnabled,
                onPrepareCloudBackupEnable = viewModel::prepareCloudBackupEnable,
                onEnableCloudBackup = viewModel::enableCloudBackup,
                onCloudAutoUploadChange = viewModel::setCloudAutoUploadEnabled,
                onMongoUpload = viewModel::uploadMongoBackup,
                onMongoDownload = { supportId ->
                    viewModel.downloadMongoBackup(supportId) {
                        // Provider runs off the main thread; publish the prompt + continuation on
                        // the main thread so the dialog recomposes and the restore doesn't hang.
                        withContext(Dispatchers.Main) {
                            suspendCancellableCoroutine { cont ->
                                cloudRestorePasswordContinuation = cont
                                cloudRestorePasswordError =
                                    "Incorrect password — enter the password you set for this backup"
                                cloudRestorePasswordPrompt = true
                            }
                        }
                    }
                },
                onRegenerateSupportId = viewModel::regenerateSupportId,
                onChangeCloudPassword = viewModel::changeCloudPassword,
                modifier = Modifier.padding(innerPadding)
            )
        }
            FitBuddyLivePill(
                message = if (showAnantEasterEgg) null else livePillMessage,
                modifier = Modifier.fillMaxSize(),
                bottomPadding = 24.dp
            )
        }
    } else if (showProgressChat) {
        ProgressCoachChatScreen(
            state = progressInsightState,
            isAiConfigured = isAiOnline,
            onBack = { showProgressChat = false },
            onClearAndBack = {
                viewModel.clearProgressChat()
                showProgressChat = false
            },
            onSendMessage = viewModel::sendProgressChatMessage
        )
    } else {
        // Keep the main tab scaffold composed while Week history is open. Replacing it in an
        // if/else disposed every visited tab (Progress charts etc.) and made Back feel lagged.
        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (selectedTab == Tab.DASHBOARD && settings.hasUserName) {
                    TopAppBar(
                        title = {
                            Text(dashboardGreeting(settings.displayFirstName))
                        },
                        actions = {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                } else {
                    CenterAlignedTopAppBar(
                        title = { Text(selectedTab.label) },
                        actions = {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            },
            snackbarHost = { },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                dismissKeyboard()
                                selectedTab = tab
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == Tab.DASHBOARD && !showWeekHistory) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            dismissKeyboard()
                            showLogHub = true
                        },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Log") }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                Tab.entries.forEach { tab ->
                    if (tab !in visitedTabs) return@forEach
                    val isSelected = tab == selectedTab
                    // Keep visited tabs composed for instant switches, but never measure them at
                    // 0×0 — Progress/Body use fixed-size charts (e.g. height(240.dp)) whose min
                    // constraints exceed a zero max and crash Compose layout.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (isSelected) 1f else 0f)
                            .graphicsLayer { alpha = if (isSelected) 1f else 0f }
                    ) {
                        when (tab) {
                            Tab.DASHBOARD -> DashboardHomeScreen(
                                realToday = realToday,
                                weekSnapshots = weekSnapshots,
                                profileState = dashboardState,
                                isAnalyzing = analysisState.isLoading,
                                onOpenWeekHistory = {
                                    viewModel.refreshToToday()
                                    showWeekHistory = true
                                },
                                onEditFood = viewModel::editFoodLog,
                                onDeleteFood = viewModel::deleteFoodLog,
                                onCloneFood = viewModel::cloneFoodLogToToday,
                                onViewExercise = viewModel::openWorkoutDetails,
                                onDeleteExercise = viewModel::deleteExerciseLog,
                                onCloneExercise = viewModel::cloneWorkoutToToday
                            )

                            Tab.PROGRESS -> AnalyticsScreen(
                                weeklyFood = weeklyFood,
                                monthlyFood = monthlyFood,
                                weeklyExercise = weeklyExercise,
                                monthlyExercise = monthlyExercise,
                                measurements = measurements,
                                targetCalories = dashboardState.targetCalories,
                                analyticsMonthYm = analyticsMonthYm,
                                realToday = realToday,
                                progressInsightState = progressInsightState,
                                isAiConfigured = isAiOnline,
                                onShiftMonth = viewModel::shiftAnalyticsMonth,
                                onRequestInsight = viewModel::requestProgressInsight,
                                onOpenChat = { showProgressChat = true },
                                onDismissInsight = viewModel::dismissProgressInsight
                            )

                            Tab.BODY -> BodyScreen(
                                profile = dashboardState.profile,
                                latestMeasurement = latestMeasurement,
                                measurements = measurements,
                                savedFoodCount = savedFoods.size,
                                targetPlanState = targetPlanState,
                                isAiConfigured = isAiOnline,
                                onSave = viewModel::saveProfile,
                                onAddMeasurement = viewModel::addMeasurement,
                                onDeleteMeasurement = viewModel::deleteMeasurement,
                                onRequestTargetPlan = viewModel::requestTargetPlan,
                                onApplyTargetPlan = viewModel::applyTargetPlan,
                                onDismissTargetPlan = viewModel::dismissTargetPlan,
                                onScanSavedFood = {
                                    scanFlow = ScanFlow.SAVE_FOOD
                                    showBarcodeScan = true
                                },
                                onManageSavedFoods = { showSavedFoodManageSheet = true }
                            )
                        }
                    }
                }
            }
        }

            if (showWeekHistory) {
                BackHandler {
                    showWeekHistory = false
                    viewModel.refreshToToday()
                }
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = { Text("Week history") },
                            navigationIcon = {
                                IconButton(onClick = {
                                    showWeekHistory = false
                                    viewModel.refreshToToday()
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { showSettings = true }) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = {
                                dismissKeyboard()
                                showLogHub = true
                            },
                            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            text = { Text("Log") }
                        )
                    }
                ) { innerPadding ->
                    WeekHistoryScreen(
                        weekDates = weekDates,
                        selectedDate = selectedDate,
                        realToday = realToday,
                        weekSnapshots = weekSnapshots,
                        profileState = dashboardState,
                        isAnalyzing = analysisState.isLoading,
                        onSelectDate = viewModel::selectDate,
                        onShiftWeek = viewModel::shiftHistoryWeek,
                        onEditFood = viewModel::editFoodLog,
                        onDeleteFood = viewModel::deleteFoodLog,
                        onCloneFood = viewModel::cloneFoodLogToToday,
                        onViewExercise = viewModel::openWorkoutDetails,
                        onDeleteExercise = viewModel::deleteExerciseLog,
                        onCloneExercise = viewModel::cloneWorkoutToToday,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            FitBuddySnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(3f)
            )
            FitBuddyLivePill(
                message = if (showAnantEasterEgg) null else livePillMessage,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(3f)
            )
        }
    }

    if (hasSettingsSnapshot && !settings.hasUserName) {
        NamePromptDialog(onSave = viewModel::saveUserName)
    }

    if (showAnantEasterEgg) {
        AnantEasterEggDialog(onDismiss = { showAnantEasterEgg = false })
    }

    if (showLogHub) {
        LogHubSheet(
            onDismiss = { showLogHub = false },
            onLogPhoto = {
                showLogHub = false
                if (cameraPermission.status.isGranted) {
                    cameraLauncher.launch(null)
                } else {
                    cameraPermission.launchPermissionRequest()
                }
            },
            onLogGallery = {
                showLogHub = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onLogText = {
                showLogHub = false
                showTextDialog = true
            },
            onBuildMeal = {
                showLogHub = false
                mealItems.clear()
                mealBuilderInitial = null
                showMealBuilder = true
            },
            onLogSavedMeal = {
                showLogHub = false
                showMealPresetSheet = true
            },
            onLogSavedFood = {
                showLogHub = false
                savedFoodSheetMode = SavedFoodSheetMode.LOG_TO_DAY
                showSavedFoodSheet = true
            },
            onLogWorkout = {
                showLogHub = false
                showWorkoutDialog = true
            }
        )
    }

    if (showMealPresetSheet) {
        MealPresetPickerSheet(
            presets = mealPresets,
            onPick = { preset ->
                viewModel.logMealPreset(preset)
                showMealPresetSheet = false
            },
            onDelete = viewModel::deleteMealPreset,
            onDismiss = { showMealPresetSheet = false }
        )
    }

    if (showSavedFoodSheet) {
        SavedFoodPickerSheet(
            foods = savedFoods,
            mode = savedFoodSheetMode,
            onPick = { food ->
                when (savedFoodSheetMode) {
                    SavedFoodSheetMode.PICK_FOR_MEAL -> mealItems.add(food.toFoodEntry())
                    SavedFoodSheetMode.LOG_TO_DAY -> viewModel.logSavedFood(food)
                    SavedFoodSheetMode.MANAGE_LIBRARY -> Unit
                }
                showSavedFoodSheet = false
            },
            onDelete = viewModel::deleteSavedFood,
            onDismiss = { showSavedFoodSheet = false }
        )
    }

    if (showSavedFoodManageSheet) {
        SavedFoodPickerSheet(
            foods = savedFoods,
            mode = SavedFoodSheetMode.MANAGE_LIBRARY,
            onDelete = viewModel::deleteSavedFood,
            onDismiss = { showSavedFoodManageSheet = false }
        )
    }

    if (showMealBuilder) {
        MealBuilderDialog(
            initialDraft = mealBuilderInitial,
            items = mealItems,
            onReview = { draft ->
                val ts = mealBuilderInitial?.timestamp ?: viewModel.activeDayTimestamp()
                val snapshot = draft.copy(timestamp = ts).snapshot()
                // New builds save as preset; editing an existing log still updates that log.
                mealReviewSavesAsPreset = mealBuilderInitial == null
                mealReviewDraft = snapshot
                showMealBuilder = false
                mealBuilderInitial = null
                mealItems.clear()
                viewModel.openMealReview(snapshot)
            },
            onCreateFood = {
                foodEditorTarget = FoodEditorTarget.MEAL_BUILDER
                mealFoodEditIndex = null
                viewModel.openFoodEditor(
                    com.anant.fitbuddy.data.model.FoodDraft(
                        dishName = "New food",
                        timestamp = System.currentTimeMillis(),
                        ingredients = listOf(
                            IngredientDraft.fromAbsolute("Ingredient", 100, 0, 0, 0, 0)
                        )
                    )
                )
            },
            onEditFood = { index ->
                foodEditorTarget = FoodEditorTarget.MEAL_BUILDER
                mealFoodEditIndex = index
                viewModel.openFoodEditor(mealItems[index].toFoodDraft())
            },
            onScanProduct = {
                scanFlow = ScanFlow.ADD_TO_MEAL
                showBarcodeScan = true
            },
            onPickPreset = {
                savedFoodSheetMode = SavedFoodSheetMode.PICK_FOR_MEAL
                showSavedFoodSheet = true
            },
            onDismiss = {
                showMealBuilder = false
                mealBuilderInitial = null
                mealItems.clear()
                lastOpenedEditMealId = null
                viewModel.dismissMealDraft()
            }
        )
    }

    if (showBarcodeScan) {
        BarcodeScanDialog(
            onBarcode = { code ->
                showBarcodeScan = false
                viewModel.lookupBarcode(code) { product -> pendingProduct = product }
            },
            onDismiss = {
                showBarcodeScan = false
                scanFlow = null
            },
            onCameraPermissionDenied = {
                scope.launch {
                    snackbarHostState.showFitBuddyPill("Camera permission not allowed.")
                }
            }
        )
    }

    pendingProduct?.let { product ->
        ScannedProductDialog(
            product = product,
            isSaving = false,
            primaryLabel = if (scanFlow == ScanFlow.ADD_TO_MEAL) "Add to meal" else "Save food",
            onPrimary = { confirmed ->
                when (scanFlow) {
                    ScanFlow.ADD_TO_MEAL -> mealItems.add(confirmed.toFoodEntry())
                    ScanFlow.SAVE_FOOD, null -> viewModel.saveScannedProductAsSavedFood(confirmed)
                }
                pendingProduct = null
                scanFlow = null
            },
            onDismiss = {
                pendingProduct = null
                scanFlow = null
            }
        )
    }

    if (showWorkoutDialog) {
        WorkoutLogDialog(
            state = workoutLogState,
            pickerExercises = exercisePickerExercises,
            isClassifyingCustom = customExerciseClassifying,
            isInferringExercises = workoutInferring,
            isAiOnline = isAiOnline,
            onClassifyCustom = viewModel::classifyCustomExercise,
            onInferExercises = viewModel::inferExercisesFromDescription,
            onSave = viewModel::logWorkoutSession,
            onDismiss = {
                showWorkoutDialog = false
                viewModel.consumeWorkoutSaved()
            }
        )
    }

    editingWorkout?.let { editing ->
        WorkoutLogDialog(
            state = workoutLogState,
            initialDraft = editing.draft,
            pickerExercises = exercisePickerExercises,
            isClassifyingCustom = customExerciseClassifying,
            isInferringExercises = workoutInferring,
            isAiOnline = isAiOnline,
            onClassifyCustom = viewModel::classifyCustomExercise,
            onInferExercises = viewModel::inferExercisesFromDescription,
            onSave = viewModel::saveEditingWorkout,
            onDismiss = viewModel::dismissWorkoutDetails
        )
    }

    if (showTextDialog) {
        TextLogDialog(
            onDismiss = { showTextDialog = false },
            onSubmit = { text ->
                showTextDialog = false
                foodEditorTarget = FoodEditorTarget.LOG_MEAL
                mealFoodEditIndex = null
                viewModel.analyzeText(text)
            }
        )
    }

    analysisState.clarificationMessage?.let { message ->
        ClarificationDialog(
            message = message,
            onDismiss = viewModel::dismissClarification,
            onSubmit = viewModel::submitClarification
        )
    }

    mealReviewDraft?.let { draft ->
        val editingMeal = analysisState.editingFoodLogId != null
        val saveAsPresetOnly = mealReviewSavesAsPreset && !editingMeal
        MealReviewDialog(
            draft = draft,
            isEditing = editingMeal,
            saveAsPresetOnly = saveAsPresetOnly,
            onConfirm = { confirmed ->
                viewModel.confirmMeal(confirmed)
                mealReviewDraft = null
                mealReviewSavesAsPreset = false
            },
            onSaveAsPreset = { mealDraft ->
                viewModel.saveMealDraftAsPreset(mealDraft)
                if (saveAsPresetOnly) {
                    livePillMessage = "Saved \"${mealDraft.name}\" as meal preset"
                    mealReviewDraft = null
                    mealReviewSavesAsPreset = false
                    viewModel.dismissMealDraft()
                }
            },
            onEditFood = { index, food ->
                foodEditorTarget = FoodEditorTarget.MEAL_REVIEW
                mealFoodEditIndex = index
                viewModel.openFoodEditor(food.toFoodDraft())
            },
            onDismiss = {
                mealReviewDraft = null
                mealReviewSavesAsPreset = false
                viewModel.dismissMealDraft()
            }
        )
    }

    analysisState.foodDraft?.let { draft ->
        FoodReviewDialog(
            draft = draft,
            isReanalyzing = analysisState.isReanalyzing,
            reviewMessage = analysisState.reviewMessage,
            onReviewMessageShown = viewModel::consumeReviewMessage,
            onConfirm = { confirmed ->
                when (foodEditorTarget) {
                    FoodEditorTarget.LOG_MEAL -> viewModel.confirmFood(confirmed)
                    FoodEditorTarget.MEAL_BUILDER -> {
                        val entry = confirmed.toFoodEntry()
                        val idx = mealFoodEditIndex
                        if (idx != null && idx in mealItems.indices) {
                            mealItems[idx] = entry
                        } else {
                            mealItems.add(entry)
                        }
                        mealFoodEditIndex = null
                        viewModel.dismissFoodDraft()
                    }
                    FoodEditorTarget.MEAL_REVIEW -> {
                        mealFoodEditIndex?.let { idx ->
                            val entry = confirmed.toFoodEntry().snapshot()
                            viewModel.updateMealDraftFood(idx, entry)
                        }
                        mealFoodEditIndex = null
                        viewModel.dismissFoodDraft()
                    }
                }
            },
            onSaveAsPreset = viewModel::saveDraftAsSavedFood,
            onReanalyze = viewModel::reanalyzeFood,
            onAskForPortion = viewModel::askForPortion,
            onDismiss = {
                mealFoodEditIndex = null
                viewModel.dismissFoodDraft()
            }
        )
    }

    analysisState.rawAiJson?.let { json ->
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = viewModel::consumeRawAiJson,
            title = { Text("Raw AI JSON") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(json, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::consumeRawAiJson) { Text("Close") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(json))
                        scope.launch {
                            snackbarHostState.showFitBuddyPill("Copied")
                        }
                    }
                ) { Text("Copy") }
            }
        )
    }

    analysisState.errorDialogMessage?.let { message ->
        AiErrorDialog(
            title = analysisState.errorDialogTitle ?: "Something went wrong",
            message = message,
            onDismiss = viewModel::dismissErrorDialog
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Replace all data?") },
            text = {
                Text(
                    "Importing this backup will replace your current profile, readings, logs, " +
                        "and custom foods/meals/exercises with the file's contents. Settings " +
                        "(including AI keys) are replaced when the backup includes them. " +
                        "This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val importUri = uri
                    pendingImportUri = null
                    importPassword = ""
                    importPasswordError = null
                    importPasswordAttempts = 0
                    viewModel.importData(importUri) {
                        // The provider runs on Dispatchers.IO (BackupManager.importFrom), so publish
                        // the prompt state and capture the continuation on the main thread —
                        // otherwise the dialog never recomposes and the import hangs forever.
                        withContext(Dispatchers.Main) {
                            suspendCancellableCoroutine { cont ->
                                importPasswordContinuation = cont
                                importPasswordError =
                                    if (importPasswordAttempts > 0) "Incorrect password" else null
                                importPasswordPromptUri = importUri
                            }
                        }
                    }
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
            }
        )
    }

    // Export dialog: optional password with confirmation (8–128, masked)
    if (showExportDialog) {
        val exportIsBlank = exportPassword.isEmpty() || exportPassword.all { it.isWhitespace() }
        val exportPasswordsMatch = exportPassword == exportConfirmPassword
        val exportLengthValid = exportIsBlank || exportPassword.length in 8..128
        val exportValidationError = when {
            !exportLengthValid && exportPassword.length < 8 ->
                "Password must be at least 8 characters"
            !exportLengthValid -> "Password must be 128 characters or fewer"
            !exportIsBlank && exportConfirmPassword.isNotEmpty() && !exportPasswordsMatch ->
                "Passwords do not match"
            else -> null
        }
        val canExport = exportValidationError == null && (exportIsBlank || exportPasswordsMatch)
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                exportPassword = ""
                exportConfirmPassword = ""
            },
            title = { Text("Export backup") },
            text = {
                Column {
                    Text(
                        "Optionally protect this backup with a password.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.size(12.dp))
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Password (optional)") },
                        singleLine = true,
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = exportValidationError != null &&
                            exportValidationError != "Passwords do not match",
                        supportingText = exportValidationError?.let { error ->
                            { Text(error, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!exportIsBlank) {
                        Spacer(Modifier.size(8.dp))
                        OutlinedTextField(
                            value = exportConfirmPassword,
                            onValueChange = { exportConfirmPassword = it },
                            label = { Text("Confirm password") },
                            singleLine = true,
                            visualTransformation =
                                androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            isError = exportConfirmPassword.isNotEmpty() && !exportPasswordsMatch,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { exportLauncher.launch("fitness-backup.json") },
                    enabled = canExport
                ) { Text("Export") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportPassword = ""
                    exportConfirmPassword = ""
                }) { Text("Cancel") }
            }
        )
    }

    // Import password dialog: shown only when the backup is encrypted (up to 5 attempts)
    if (importPasswordPromptUri != null) {
        AlertDialog(
            onDismissRequest = {
                importPasswordContinuation?.resumeWith(Result.success(null))
                importPasswordContinuation = null
                importPasswordPromptUri = null
                importPassword = ""
                importPasswordError = null
            },
            title = { Text("Password required") },
            text = {
                Column {
                    Text(
                        "This backup is encrypted. Enter the password to import it.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (importPasswordAttempts > 0 && importPasswordError != null) {
                        Spacer(Modifier.size(4.dp))
                        Text(
                            importPasswordError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (importPasswordAttempts > 0) {
                        Spacer(Modifier.size(4.dp))
                        Text(
                            "Attempt ${importPasswordAttempts + 1} of 5",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pw = importPassword.toCharArray()
                        importPassword = ""
                        importPasswordAttempts++
                        importPasswordPromptUri = null
                        importPasswordContinuation?.resumeWith(Result.success(pw))
                        importPasswordContinuation = null
                    },
                    enabled = importPassword.isNotEmpty()
                ) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = {
                    importPasswordContinuation?.resumeWith(Result.success(null))
                    importPasswordContinuation = null
                    importPasswordPromptUri = null
                    importPassword = ""
                    importPasswordError = null
                }) { Text("Cancel") }
            }
        )
    }

    // Cloud restore password dialog: shown only when Support-ID decryption fails
    if (cloudRestorePasswordPrompt) {
        AlertDialog(
            onDismissRequest = {
                cloudRestorePasswordContinuation?.resumeWith(Result.success(null))
                cloudRestorePasswordContinuation = null
                cloudRestorePasswordPrompt = false
                cloudRestorePassword = ""
                cloudRestorePasswordError = null
            },
            title = { Text("Password required") },
            text = {
                Column {
                    Text(
                        cloudRestorePasswordError ?: "Enter the password for this cloud backup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (cloudRestorePasswordError != null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.size(12.dp))
                    OutlinedTextField(
                        value = cloudRestorePassword,
                        onValueChange = { cloudRestorePassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pw = cloudRestorePassword.toCharArray()
                        cloudRestorePassword = ""
                        cloudRestorePasswordPrompt = false
                        cloudRestorePasswordError = null
                        cloudRestorePasswordContinuation?.resumeWith(Result.success(pw))
                        cloudRestorePasswordContinuation = null
                    },
                    enabled = cloudRestorePassword.isNotEmpty()
                ) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = {
                    cloudRestorePasswordContinuation?.resumeWith(Result.success(null))
                    cloudRestorePasswordContinuation = null
                    cloudRestorePasswordPrompt = false
                    cloudRestorePassword = ""
                    cloudRestorePasswordError = null
                }) { Text("Cancel") }
            }
        )
    }

    fun startUpdateDownload(downloadUrl: String) {
        viewModel.beginUpdateDownload()
        scope.launch {
            try {
                ApkInstaller.downloadAndInstall(context, downloadUrl) { progress ->
                    viewModel.updateDownloadProgress(progress)
                }
                viewModel.finishUpdateDownload()
            } catch (e: Exception) {
                viewModel.failUpdateDownload(
                    e.message?.takeIf { it.isNotBlank() } ?: "Update download failed"
                )
            }
        }
    }

    // After Export backup & update succeeds with a fresh backup timestamp, auto-install.
    LaunchedEffect(
        updateState.backupCompleted,
        updateState.pendingDownloadUrlAfterBackup,
        settings.lastSuccessfulBackupAt
    ) {
        val url = updateState.pendingDownloadUrlAfterBackup ?: return@LaunchedEffect
        if (!updateState.backupCompleted) return@LaunchedEffect
        if (!settings.hasFreshSuccessfulBackup()) return@LaunchedEffect
        startUpdateDownload(url)
    }

    UpdatePromptDialogs(
        updateState = updateState,
        cloudBackupEnabled = settings.cloudBackupEnabled,
        onDismissUpdatePrompt = viewModel::dismissUpdatePrompt,
        onExportBackupAndUpdate = { downloadUrl ->
            if (viewModel.beginExportBackupAndUpdate(downloadUrl)) {
                updateBackupExportLauncher.launch("fitness-backup.json")
            }
        },
        onSkipBackupAndUpdate = ::startUpdateDownload
    )
}

@Composable
private fun AiErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.ErrorOutline, contentDescription = null) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
private fun TextLogDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Describe it") },
        text = {
            Column {
                Text(
                    "e.g. \"2 rotis with dal tadka\" or \"aloo paratha with curd\"",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Food or activity") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Analyze") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ClarificationDialog(
    message: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var answer by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick question") },
        text = {
            Column {
                Text(message)
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Your answer") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(answer.trim()) },
                enabled = answer.isNotBlank()
            ) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun dashboardGreeting(firstName: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val period = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    return "$period, $firstName"
}

@Composable
private fun NamePromptDialog(onSave: (firstName: String, lastName: String) -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    val canSave = firstName.trim().isNotEmpty() && lastName.trim().isNotEmpty()
    AlertDialog(
        onDismissRequest = {},
        title = { Text("What’s your name?") },
        text = {
            Column {
                Text(
                    text = "We’ll use your first name in the dashboard greeting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(firstName.trim(), lastName.trim()) },
                enabled = canSave
            ) { Text("Save") }
        }
    )
}
