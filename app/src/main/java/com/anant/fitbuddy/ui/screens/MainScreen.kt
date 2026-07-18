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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.anant.fitbuddy.ui.viewmodel.MainViewModel
import com.anant.fitbuddy.util.ApkInstaller
import com.anant.fitbuddy.util.ImageUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TODAY("Today", Icons.Filled.Today),
    PROGRESS("Progress", Icons.Filled.Insights),
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
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val foodLogs by viewModel.foodLogsToday.collectAsStateWithLifecycle()
    val exerciseLogs by viewModel.exerciseLogsToday.collectAsStateWithLifecycle()

    val weeklyFood by viewModel.weeklyFood.collectAsStateWithLifecycle()
    val monthlyFood by viewModel.monthlyFood.collectAsStateWithLifecycle()
    val weeklyExercise by viewModel.weeklyExercise.collectAsStateWithLifecycle()
    val monthlyExercise by viewModel.monthlyExercise.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasSettingsSnapshot by viewModel.hasSettingsSnapshot.collectAsStateWithLifecycle()
    val modelsState by viewModel.models.collectAsStateWithLifecycle()
    val textModelsState by viewModel.textModels.collectAsStateWithLifecycle()
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

    var selectedTab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    // Tabs are composed once on first visit and then kept alive (just hidden) so switching back
    // and forth is instant and doesn't lose scroll/animation/unsaved-edit state or re-run
    // expensive first-composition work (e.g. the Progress tab's charts) every time.
    var visitedTabs by remember { mutableStateOf(setOf(selectedTab)) }
    LaunchedEffect(selectedTab) { visitedTabs = visitedTabs + selectedTab }
    var showSettings by remember { mutableStateOf(false) }
    var showProgressChat by remember { mutableStateOf(false) }
    var showLogHub by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showMealPresetSheet by remember { mutableStateOf(false) }
    var showSavedFoodSheet by remember { mutableStateOf(false) }
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
    var lastOpenedEditMealId by remember { mutableStateOf<Int?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var startupCreditShown by rememberSaveable { mutableStateOf(false) }
    var easterEggTriggeredThisSession by rememberSaveable { mutableStateOf(false) }
    var livePillMessage by remember { mutableStateOf<String?>(null) }
    var showAnantEasterEgg by remember { mutableStateOf(false) }

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
                    "Camera permission is needed to photograph meals."
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
    ) { uri -> uri?.let(viewModel::exportData) }

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

    LaunchedEffect(settings.easterEggDiscovered, hasSettingsSnapshot, easterEggTriggeredThisSession) {
        if (!hasSettingsSnapshot) return@LaunchedEffect
        if (settings.easterEggDiscovered || easterEggTriggeredThisSession) {
            startupCreditShown = true
            snackbarHostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        if (startupCreditShown) return@LaunchedEffect
        startupCreditShown = true
        snackbarHostState.showFitBuddyPill(
            message = "Created by Anant",
            displayMillis = FitBuddyPillConfig.STARTUP_DISPLAY_MS
        )
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
                modelsState = modelsState,
                textModelsState = textModelsState,
                onLoadModels = { provider, apiKey, force, baseUrl ->
                    viewModel.loadFreeVisionModels(provider, apiKey, force, baseUrl)
                },
                onLoadTextModels = { provider, apiKey, force, baseUrl ->
                    viewModel.loadFreeTextModels(provider, apiKey, force, baseUrl)
                },
                onSave = viewModel::saveSettings,
                onDynamicColorChange = { enabled ->
                    viewModel.saveSettings(settings.copy(dynamicColor = enabled))
                },
                onExport = { exportLauncher.launch("fitness-backup.json") },
                onImport = { importLauncher.launch(arrayOf("application/json")) },
                onEasterEggTriggered = {
                    easterEggTriggeredThisSession = true
                    startupCreditShown = true
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
                    startupCreditShown = false
                    easterEggTriggeredThisSession = false
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
                onSupportIdCopied = {
                    scope.launch {
                        snackbarHostState.showFitBuddyPill("Support ID copied")
                    }
                },
                onDeveloperUnlockHint = { remaining ->
                    livePillMessage = "$remaining taps to go"
                },
                onDeveloperUnlockHintDismiss = { livePillMessage = null },
                onDeveloperUnlocked = {
                    livePillMessage = null
                    scope.launch {
                        snackbarHostState.showFitBuddyPill("Developer settings unlocked")
                    }
                },
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
        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (selectedTab == Tab.TODAY) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = LocalDate.now().format(
                                        DateTimeFormatter.ofPattern("EEEE, MMM d")
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(selectedTab.label)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            snackbarHost = { },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == Tab.TODAY) {
                    ExtendedFloatingActionButton(
                        onClick = { showLogHub = true },
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
                    Box(modifier = if (isSelected) Modifier.fillMaxSize() else Modifier.size(0.dp)) {
                        when (tab) {
                            Tab.TODAY -> DashboardScreen(
                                state = dashboardState,
                                foodLogs = foodLogs,
                                exerciseLogs = exerciseLogs,
                                isAnalyzing = analysisState.isLoading,
                                onEditFood = viewModel::editFoodLog,
                                onDeleteFood = viewModel::deleteFoodLog,
                                onViewExercise = viewModel::openWorkoutDetails,
                                onDeleteExercise = viewModel::deleteExerciseLog
                            )

                            Tab.PROGRESS -> AnalyticsScreen(
                                weeklyFood = weeklyFood,
                                monthlyFood = monthlyFood,
                                weeklyExercise = weeklyExercise,
                                monthlyExercise = monthlyExercise,
                                measurements = measurements,
                                targetCalories = dashboardState.targetCalories,
                                progressInsightState = progressInsightState,
                                isAiConfigured = isAiOnline,
                                onRequestInsight = viewModel::requestProgressInsight,
                                onOpenChat = { showProgressChat = true }
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

            FitBuddySnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxSize()
            )
            FitBuddyLivePill(
                message = if (showAnantEasterEgg) null else livePillMessage,
                modifier = Modifier.fillMaxSize()
            )
        }
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
            mode = SavedFoodSheetMode.PICK_FOR_MEAL,
            onPick = { food ->
                mealItems.add(food.toFoodEntry())
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
                val ts = mealBuilderInitial?.timestamp ?: draft.timestamp
                val snapshot = draft.copy(timestamp = ts).snapshot()
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
            onPickPreset = { showSavedFoodSheet = true },
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
            onSave = viewModel::updateWorkoutSession,
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
        MealReviewDialog(
            draft = draft,
            isEditing = analysisState.editingFoodLogId != null,
            onConfirm = { confirmed ->
                viewModel.confirmMeal(confirmed)
                mealReviewDraft = null
            },
            onSaveAsPreset = { mealDraft ->
                viewModel.saveMealDraftAsPreset(mealDraft)
            },
            onEditFood = { index, food ->
                foodEditorTarget = FoodEditorTarget.MEAL_REVIEW
                mealFoodEditIndex = index
                viewModel.openFoodEditor(food.toFoodDraft())
            },
            onDismiss = {
                mealReviewDraft = null
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
            onDismiss = {
                mealFoodEditIndex = null
                viewModel.dismissFoodDraft()
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
                    "Importing this backup will delete your current profile, readings, and logs, " +
                        "then replace them with the file's contents. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importData(uri)
                    pendingImportUri = null
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
            }
        )
    }

    UpdatePromptDialogs(
        updateState = updateState,
        onDismissUpdatePrompt = viewModel::dismissUpdatePrompt,
        onConfirmUpdate = { downloadUrl ->
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
