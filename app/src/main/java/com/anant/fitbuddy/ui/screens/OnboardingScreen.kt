package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.R
import androidx.compose.ui.res.stringResource

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import com.anant.fitbuddy.ui.theme.appShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.crash.CrashReporter
import com.anant.fitbuddy.data.model.ActivityLevels
import com.anant.fitbuddy.util.SystemToast
import com.anant.fitbuddy.data.model.OpenAiCatalog
import com.anant.fitbuddy.data.region.AppRegion
import com.anant.fitbuddy.data.region.RegionDetector
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.components.Button
import com.anant.fitbuddy.ui.components.ConfettiOverlay
import com.anant.fitbuddy.ui.components.CraftedWithLoveCredit
import com.anant.fitbuddy.ui.components.OpenRouterConnectSection
import com.anant.fitbuddy.ui.components.OutlinedButton
import com.anant.fitbuddy.ui.components.TextButton
import com.anant.fitbuddy.ui.util.dismissKeyboardOnTap
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

private val GOAL_OPTION_RES = listOf(
    "AUTO" to R.string.onboarding_choose_automatically,
    "LOSE_WEIGHT" to R.string.onboarding_lose_weight,
    "GAIN_MUSCLE" to R.string.onboarding_gain_muscle,
    "RECOMP" to R.string.onboarding_body_recomposition
)
private val SEX_OPTION_RES = listOf(
    "" to R.string.onboarding_prefer_not_say,
    "MALE" to R.string.onboarding_male,
    "FEMALE" to R.string.onboarding_female
)

/** Per-provider setup on the FitBuddy onboarding site (API key walkthroughs + videos). */
private val AI_SETUP_DOC_RES: Map<AiProvider, Pair<Int, String>> = mapOf(
    AiProvider.OPENROUTER to (
        R.string.settings_how_to_create_an_openrouter_api to
            "https://anantdark.github.io/FitBuddy/onboarding.html#openrouter"
    ),
    AiProvider.GEMINI to (
        R.string.onboarding_how_create_gemini_api_key to
            "https://anantdark.github.io/FitBuddy/onboarding.html#gemini"
    ),
    AiProvider.OLLAMA to (
        R.string.onboarding_ollama_install_setup_guide to
            "https://anantdark.github.io/FitBuddy/onboarding.html#ollama"
    ),
    AiProvider.CUSTOM to (
        R.string.onboarding_openai_api_keys_compatible_apis to
            "https://anantdark.github.io/FitBuddy/onboarding.html#openai"
    )
)

private val OLLAMA_CLOUD_KEYS_URL =
    "https://anantdark.github.io/FitBuddy/onboarding.html#ollama-cloud"

/** First-run / API-key handholding page on the FitBuddy gh-pages site. */
private const val ONBOARDING_GUIDE_URL =
    "https://anantdark.github.io/FitBuddy/onboarding.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    isSaving: Boolean,
    isValidating: Boolean,
    isRestoring: Boolean = false,
    aiOnly: Boolean = false,
    /** Developer throwaway walkthrough — must not persist crash-reporting toggles. */
    previewMode: Boolean = false,
    /**
     * Build default for crash reporting. When this is false (F-Droid / debug), an opt-in
     * page is inserted before the region step.
     */
    initialCrashReportingEnabled: Boolean = AppSettings().crashReportingEnabled,
    /** Anonymous support id, forwarded to the region-request consent dialog. */
    supportId: String = "",
    /** One custom-region Sentry request allowed per install. */
    regionRequestAlreadySent: Boolean = false,
    onRegionRequestSent: () -> Unit = {},
    cloudRestoreAvailable: Boolean = false,
    openRouterOAuthBusy: Boolean = false,
    openRouterOAuthKey: String = "",
    userMessage: String? = null,
    onUserMessageConsumed: () -> Unit = {},
    onConnectOpenRouter: (android.content.Context) -> Unit = {},
    onDisconnectOpenRouter: () -> Unit = {},
    onStartGuest: () -> Unit = {},
    onRestoreCloud: (
        supportId: String,
        passwordProvider: suspend () -> CharArray?,
        onResult: (Boolean, String?) -> Unit
    ) -> Unit = { _, _, _ -> },
    onRestoreLocal: (
        uri: Uri,
        passwordProvider: suspend () -> CharArray?,
        onResult: (Boolean, String?) -> Unit
    ) -> Unit = { _, _, _ -> },
    onValidateAi: (AppSettings, (Boolean, String?) -> Unit) -> Unit,
    onComplete: (
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
        cloudBackupEnabled: Boolean
    ) -> Unit,
    onCompleteAiOnly: (AppSettings) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // -1 = path picker; 0..N = AI / profile / lifestyle [/ cloud] [/ crash] / region (or AI-only).
    var step by remember(aiOnly) { mutableIntStateOf(if (aiOnly) 0 else -1) }
    // Cloud page: F-Droid release only (GitHub builds skip this; enable later in Settings).
    val showCloudStep =
        !aiOnly && !BuildConfig.DEBUG && BuildConfig.IS_FDROID && cloudRestoreAvailable
    // Crash page only when the build defaults reporting off (F-Droid / debug).
    val showCrashStep = !initialCrashReportingEnabled
    var cloudBackupChoice by remember { mutableStateOf(false) }
    var crashReportingChoice by remember(initialCrashReportingEnabled) {
        mutableStateOf(initialCrashReportingEnabled)
    }
    val stepCloud = 3
    val stepCrash = if (showCloudStep) 4 else 3
    val stepRegion = 3 + (if (showCloudStep) 1 else 0) + (if (showCrashStep) 1 else 0)
    var cloudSupportId by remember { mutableStateOf("") }
    var cloudError by remember { mutableStateOf<String?>(null) }
    // Password prompt for restoring a custom-password-protected cloud backup.
    var cloudRestorePasswordPrompt by remember { mutableStateOf(false) }
    var cloudRestorePassword by remember { mutableStateOf("") }
    var cloudRestorePasswordContinuation by remember {
        mutableStateOf<CancellableContinuation<CharArray?>?>(null)
    }
    // Password prompt for restoring a password-protected local backup file.
    var localRestorePasswordPrompt by remember { mutableStateOf(false) }
    var localRestorePassword by remember { mutableStateOf("") }
    var localRestorePasswordAttempts by remember { mutableStateOf(0) }
    var localRestoreError by remember { mutableStateOf<String?>(null) }
    var localRestorePasswordContinuation by remember {
        mutableStateOf<CancellableContinuation<CharArray?>?>(null)
    }
    val localImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            localRestoreError = null
            localRestorePasswordAttempts = 0
            onRestoreLocal(
                uri,
                {
                    // Runs on a background dispatcher (BackupManager.importFrom); publish the
                    // prompt + capture the continuation on the main thread so the dialog shows.
                    withContext(Dispatchers.Main) {
                        suspendCancellableCoroutine { cont ->
                            localRestorePasswordContinuation = cont
                            localRestorePassword = ""
                            localRestorePasswordPrompt = true
                        }
                    }
                }
            ) { ok, error ->
                if (!ok) localRestoreError = error
            }
        }
    }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("RECOMP") }
    var activity by remember { mutableStateOf("MODERATE") }
    var aiProvider by remember { mutableStateOf(AiProvider.OPENROUTER) }

    val context = LocalContext.current
    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            SystemToast.show(context, message, long = true)
            onUserMessageConsumed()
        }
    }
    LaunchedEffect(aiOnly) {
        if (aiOnly) step = 0
    }
    var apiKeys by remember { mutableStateOf(emptyList<String>()) }
    var ollamaUrl by remember { mutableStateOf(AppSettings.DEFAULT_OLLAMA_URL) }
    var ollamaUseCloud by remember { mutableStateOf(false) }
    var ollamaKeys by remember { mutableStateOf(emptyList<String>()) }
    var customUrl by remember { mutableStateOf(AppSettings.DEFAULT_CUSTOM_BASE_URL) }
    var customKeys by remember { mutableStateOf(emptyList<String>()) }
    var aiValidated by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    /** After Continue on AI / profile steps, empty required fields also turn red. */
    var aiAttempted by remember { mutableStateOf(false) }
    var profileAttempted by remember { mutableStateOf(false) }

    val openRouterOAuthConnected = openRouterOAuthKey.isNotBlank()
    // Debug builds skip the name fields so local testing can move through setup faster.
    val skipNameInOnboarding = BuildConfig.DEBUG
    val firstNameError = onboardingRequiredTextError(
        value = firstName,
        attempted = profileAttempted && !skipNameInOnboarding,
        blankMessage = stringResource(R.string.onboarding_enter_first_name),
    )
    val ageError = onboardingIntRangeError(
        value = age,
        range = 18..120,
        attempted = profileAttempted,
        label = stringResource(R.string.onboarding_age),
        context = context,
    )
    val heightError = onboardingDoubleRangeError(
        value = height,
        min = 100.0,
        max = 250.0,
        attempted = profileAttempted,
        label = stringResource(R.string.onboarding_height),
        unit = "cm",
        context = context,
    )
    val weightError = onboardingDoubleRangeError(
        value = weight,
        min = 25.0,
        max = 400.0,
        attempted = profileAttempted,
        label = stringResource(R.string.onboarding_weight),
        unit = "kg",
        context = context,
    )
    // Completeness for Continue (independent of whether red hints are visible yet).
    // Last name is optional.
    val stepOneValid = (skipNameInOnboarding || firstName.trim().isNotEmpty()) &&
        (age.toIntOrNull() ?: 0) in 18..120 &&
        (height.toDoubleOrNull() ?: 0.0) in 100.0..250.0 &&
        (weight.toDoubleOrNull() ?: 0.0) in 25.0..400.0
    val aiConfigValid = when (aiProvider) {
        AiProvider.OPENROUTER -> apiKeys.isNotEmpty() || openRouterOAuthConnected
        AiProvider.GEMINI -> apiKeys.isNotEmpty()
        AiProvider.OLLAMA -> if (ollamaUseCloud) {
            ollamaKeys.isNotEmpty()
        } else {
            ollamaUrl.isNotBlank()
        }
        AiProvider.OPENAI -> customUrl.isNotBlank() && customKeys.isNotEmpty()
        AiProvider.CUSTOM -> {
            val url = customUrl.trim()
            when {
                url.isBlank() -> false
                AppSettings.isOfficialOpenAiBaseUrl(url) -> customKeys.isNotEmpty()
                else -> true
            }
        }
    }
    val showAiFieldErrors = aiAttempted || aiError != null
    val aiKeysMissing = when (aiProvider) {
        AiProvider.OPENROUTER -> apiKeys.isEmpty() && !openRouterOAuthConnected
        AiProvider.GEMINI -> apiKeys.isEmpty()
        AiProvider.OLLAMA -> ollamaUseCloud && ollamaKeys.isEmpty()
        AiProvider.OPENAI -> customKeys.isEmpty()
        AiProvider.CUSTOM -> AppSettings.isOfficialOpenAiBaseUrl(customUrl) && customKeys.isEmpty()
    }
    val aiKeysError = if (showAiFieldErrors && aiKeysMissing) {
        when (aiProvider) {
            AiProvider.OPENROUTER -> stringResource(R.string.onboarding_add_api_key_continue_openrouter_sign)
            AiProvider.OLLAMA -> stringResource(R.string.onboarding_add_ollama_cloud_api_key)
            AiProvider.CUSTOM, AiProvider.OPENAI -> stringResource(R.string.onboarding_add_api_key)
            else -> stringResource(R.string.onboarding_add_api_key)
        }
    } else {
        null
    }
    val ollamaUrlError = when {
        aiProvider != AiProvider.OLLAMA || ollamaUseCloud -> null
        ollamaUrl.isBlank() -> if (showAiFieldErrors) stringResource(R.string.onboarding_enter_ollama_server_url) else null
        else -> null
    }
    val customUrlError = when {
        aiProvider != AiProvider.CUSTOM && aiProvider != AiProvider.OPENAI -> null
        customUrl.isBlank() -> if (showAiFieldErrors) stringResource(R.string.onboarding_enter_base_url) else null
        else -> null
    }

    fun buildAiSettings(): AppSettings {
        val orKeys = if (aiProvider == AiProvider.OPENROUTER) apiKeys else emptyList()
        val gemKeys = if (aiProvider == AiProvider.GEMINI) apiKeys else emptyList()
        val olKeys = if (aiProvider == AiProvider.OLLAMA && ollamaUseCloud) ollamaKeys else emptyList()
        val cuKeys = if (aiProvider == AiProvider.CUSTOM || aiProvider == AiProvider.OPENAI) {
            customKeys
        } else {
            emptyList()
        }
        // OpenAI-compatible (including official OpenAI): seed URL + a sensible default model.
        val usingCustom = aiProvider == AiProvider.CUSTOM || aiProvider == AiProvider.OPENAI
        val usingOfficialOpenAi = usingCustom && AppSettings.isOfficialOpenAiBaseUrl(customUrl)
        return AppSettings(
            provider = if (usingCustom) AiProvider.CUSTOM else aiProvider,
            openRouterApiKeys = orKeys,
            openRouterApiKey = orKeys.firstOrNull().orEmpty(),
            openRouterOAuthKey = if (aiProvider == AiProvider.OPENROUTER) openRouterOAuthKey else "",
            geminiApiKeys = gemKeys,
            geminiApiKey = gemKeys.firstOrNull().orEmpty(),
            ollamaBaseUrl = if (aiProvider == AiProvider.OLLAMA && !ollamaUseCloud) {
                ollamaUrl.trim()
            } else {
                AppSettings.DEFAULT_OLLAMA_URL
            },
            ollamaUseCloud = aiProvider == AiProvider.OLLAMA && ollamaUseCloud,
            ollamaApiKeys = olKeys,
            ollamaApiKey = olKeys.firstOrNull().orEmpty(),
            customBaseUrl = if (usingCustom) {
                customUrl.trim().ifBlank { AppSettings.DEFAULT_CUSTOM_BASE_URL }
            } else {
                AppSettings.DEFAULT_CUSTOM_BASE_URL
            },
            customModel = if (usingCustom) {
                AppSettings.DEFAULT_CUSTOM_MODEL
            } else {
                ""
            },
            customTextModel = if (usingCustom && usingOfficialOpenAi) {
                OpenAiCatalog.TEXT_MODELS.first().id
            } else {
                ""
            },
            customApiKeys = cuKeys,
            customApiKey = cuKeys.firstOrNull().orEmpty(),
            showPaidModelsByProvider = if (usingOfficialOpenAi) {
                mapOf(AiProvider.CUSTOM to true)
            } else {
                emptyMap()
            },
            aiAutoFailoverByProvider = emptyMap() // all default to true
        )
    }

    fun invalidateAi() {
        aiValidated = false
        aiError = null
        // Keep aiAttempted so corrected fields clear red while missing ones stay marked.
    }

    LaunchedEffect(openRouterOAuthKey) {
        if (openRouterOAuthKey.isNotBlank()) {
            aiValidated = false
            aiError = null
        }
    }

    fun finishOnboarding(
        region: AppRegion,
        crashReportingEnabled: Boolean,
        cloudBackupEnabled: Boolean
    ) {
        onComplete(
            firstName.trim(),
            lastName.trim(),
            age.toIntOrNull() ?: 0,
            weight.toDoubleOrNull() ?: 0.0,
            height.toDoubleOrNull() ?: 0.0,
            sex.ifBlank { null },
            goal,
            activity,
            buildAiSettings(),
            region,
            crashReportingEnabled,
            cloudBackupEnabled
        )
    }

    fun stepBeforeRegion(): Int = when {
        // Cloud-on skips the crash page — go back to cloud (or lifestyle).
        cloudBackupChoice && showCloudStep -> stepCloud
        showCrashStep && !cloudBackupChoice -> stepCrash
        showCloudStep -> stepCloud
        else -> 2
    }

    fun stepBeforeCrash(): Int = if (showCloudStep) stepCloud else 2

    fun finishAiOnly() {
        if (aiValidated) {
            onCompleteAiOnly(buildAiSettings())
            return
        }
        aiError = null
        onValidateAi(buildAiSettings()) { ok, error ->
            if (ok) {
                aiValidated = true
                aiError = null
                onCompleteAiOnly(buildAiSettings())
            } else {
                aiValidated = false
                aiError = error ?: context.getString(R.string.onboarding_couldn_t_validate_api_key)
            }
        }
    }

    fun advanceFromAiStep() {
        if (aiOnly) {
            finishAiOnly()
            return
        }
        if (aiValidated) {
            step = 1
            return
        }
        aiError = null
        onValidateAi(buildAiSettings()) { ok, error ->
            if (ok) {
                aiValidated = true
                aiError = null
                step = 1
            } else {
                aiValidated = false
                aiError = error ?: context.getString(R.string.onboarding_couldn_t_validate_api_key)
            }
        }
    }

    val busy = isSaving || isValidating || isRestoring
    val onPathPicker = step < 0 && !aiOnly
    val totalSteps = when {
        aiOnly -> 1
        else -> 4 + (if (showCloudStep) 1 else 0) + (if (showCrashStep) 1 else 0)
    }
    val progressStep = if (onPathPicker) 0 else (if (aiOnly) 0 else step)

    val showPathCredit = onPathPicker && step == -1
    var confettiKey by remember { mutableIntStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(confettiKey) {
        if (confettiKey == 0) return@LaunchedEffect
        showConfetti = true
        delay(4_000L)
        showConfetti = false
    }

    if (!aiOnly && showCloudStep && step == stepCloud) {
        CloudBackupOptInScreen(
            initialEnabled = cloudBackupChoice,
            onContinue = { enabled ->
                cloudBackupChoice = enabled
                if (enabled) {
                    // Cloud backup requires crash reporting — skip the crash opt-in page.
                    crashReportingChoice = true
                    if (!previewMode) {
                        CrashReporter.setReportingEnabled(true)
                    }
                    step = stepRegion
                } else {
                    step = if (showCrashStep) stepCrash else stepRegion
                }
            },
            onBack = { step = 2 },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    // Only when cloud backup was left off (cloud-on path skips this page).
    if (!aiOnly && showCrashStep && !cloudBackupChoice && step == stepCrash) {
        CrashReportingOptInScreen(
            initialEnabled = crashReportingChoice,
            onContinue = { enabled ->
                crashReportingChoice = enabled
                // Apply immediately so region-request send works before onboarding finishes.
                // Skip in developer preview — must not change the real reporting preference.
                if (!previewMode) {
                    CrashReporter.setReportingEnabled(enabled)
                }
                step = stepRegion
            },
            onBack = { step = stepBeforeCrash() },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    // Region is the final onboarding step for the full path.
    if (!aiOnly && step == stepRegion) {
        val context = LocalContext.current
        val detectedRegion = remember { RegionDetector.detectFromDevice(context) }
        RegionSelectionScreen(
            defaultRegion = detectedRegion,
            crashReportingEnabled = crashReportingChoice,
            supportId = supportId,
            isSaving = isSaving,
            regionRequestAlreadySent = regionRequestAlreadySent,
            onFinished = { region ->
                finishOnboarding(region, crashReportingChoice, cloudBackupChoice)
            },
            onBack = { step = stepBeforeRegion() },
            requestDisabledHint = if (showCrashStep) {
                stringResource(R.string.onboarding_enable_crash_reporting_previous_page_request_cus)
            } else {
                context.getString(R.string.onboarding_enable_send_crash_reports_settings_request_custo)
            },
            onRequestRegionSent = onRegionRequestSent,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize() 
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 24.dp)
                .dismissKeyboardOnTap()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (showPathCredit) Modifier else Modifier.weight(1f))
                    .verticalScroll(rememberScrollState())
            ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (aiOnly) context.getString(R.string.onboarding_reconnect_ai) else stringResource(R.string.onboarding_welcome_to_fitbuddy),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    aiOnly ->
                        stringResource(R.string.onboarding_your_data_was_restored_but_ai)
                    onPathPicker ->
                        stringResource(R.string.onboarding_start_fresh_restore_previous_backup_cloud_local)
                    else ->
                        stringResource(R.string.onboarding_connect_ai_provider_first_then_tell_us)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            if (!onPathPicker) {
                LinearProgressIndicator(
                    progress = { (progressStep + 1) / totalSteps.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (aiOnly) {
                        context.getString(R.string.onboarding_ai_setup)
                    } else {
                        context.getString(R.string.onboarding_step, (step + 1).toString(), totalSteps.toString())
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when {
                        onPathPicker && step == -2 -> {
                            // Cloud Support ID form (nested under path picker).
                            Text(
                                text = stringResource(R.string.onboarding_restore_from_cloud),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.onboarding_enter_the_support_id_from_the),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = cloudSupportId,
                                onValueChange = {
                                    cloudSupportId = it
                                    cloudError = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.donate_reminder_support_id)) },
                                singleLine = true,
                                enabled = !busy,
                                isError = cloudError != null,
                                supportingText = cloudError?.let { message ->
                                    {
                                        Text(
                                            text = message,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                },
                            )
                        }

                        onPathPicker -> {
                            val uriHandler = LocalUriHandler.current
                            Text(
                                text = stringResource(R.string.onboarding_how_do_you_want_to_start),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = {
                                    onStartGuest()
                                    step = 0
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.onboarding_start_fresh))
                            }
                            if (cloudRestoreAvailable) {
                                OutlinedButton(
                                    onClick = {
                                        cloudSupportId = ""
                                        cloudError = null
                                        step = -2
                                    },
                                    enabled = !busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.onboarding_restore_from_cloud_backup))
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    localRestoreError = null
                                    localImportLauncher.launch(
                                        arrayOf("application/json", "*/*")
                                    )
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isRestoring) stringResource(R.string.onboarding_restoring) else stringResource(R.string.onboarding_restore_from_local_backup)
                                )
                            }
                            localRestoreError?.let { err ->
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OnboardingGuideCard(
                                enabled = !busy,
                                onClick = { uriHandler.openUri(ONBOARDING_GUIDE_URL) }
                            )
                        }

                        step == 0 -> {
                            Text(
                                text = stringResource(R.string.onboarding_connect_an_ai),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.onboarding_fitbuddy_needs_an_llm_to_read),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val providerOptions = listOf(
                                AiProvider.OPENROUTER to context.getString(R.string.settings_openrouter),
                                AiProvider.GEMINI to context.getString(R.string.settings_gemini),
                                AiProvider.OLLAMA to context.getString(R.string.settings_ollama),
                                AiProvider.CUSTOM to context.getString(R.string.settings_openai_compatible)
                            )
                            ProviderSelectorGrid(
                                options = providerOptions,
                                selected = aiProvider,
                                onSelect = {
                                    if (aiProvider != it) {
                                        aiProvider = it
                                        invalidateAi()
                                    }
                                }
                            )

                            when (aiProvider) {
                                AiProvider.OPENROUTER -> {
                                    val context = LocalContext.current
                                    OpenRouterConnectSection(
                                        oauthConnected = openRouterOAuthConnected,
                                        oauthBusy = openRouterOAuthBusy,
                                        hasManualKeys = apiKeys.isNotEmpty(),
                                        onConnect = onConnectOpenRouter,
                                        onDisconnect = {
                                            onDisconnectOpenRouter()
                                            invalidateAi()
                                        },
                                        context = context
                                    ) {
                                        ApiKeyChipEditor(
                                            label = "API keys",
                                            keys = apiKeys,
                                            onKeysChange = {
                                                apiKeys = it
                                                invalidateAi()
                                            },
                                            isError = aiKeysError != null || aiError != null,
                                            errorMessage = aiKeysError,
                                        )
                                    }
                                }

                                AiProvider.GEMINI -> {
                                    AI_SETUP_DOC_RES[aiProvider]?.let { (labelRes, url) ->
                                        OnboardingDocsLink(label = stringResource(labelRes), url = url)
                                    }
                                    ApiKeyChipEditor(
                                        label = "API keys",
                                        keys = apiKeys,
                                        onKeysChange = {
                                            apiKeys = it
                                            invalidateAi()
                                        },
                                        isError = aiKeysError != null || aiError != null,
                                        errorMessage = aiKeysError,
                                    )
                                }

                                AiProvider.OLLAMA -> {
                                    AI_SETUP_DOC_RES[aiProvider]?.let { (labelRes, url) ->
                                        OnboardingDocsLink(label = stringResource(labelRes), url = url)
                                    }
                                    val modeOptions = listOf(false to context.getString(R.string.onboarding_local), true to context.getString(R.string.settings_cloud))
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        modeOptions.forEachIndexed { index, (useCloud, label) ->
                                            SegmentedButton(
                                                selected = ollamaUseCloud == useCloud,
                                                onClick = {
                                                    if (ollamaUseCloud != useCloud) {
                                                        ollamaUseCloud = useCloud
                                                        invalidateAi()
                                                    }
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(
                                                    index,
                                                    modeOptions.size
                                                )
                                            ) { Text(label) }
                                        }
                                    }
                                    if (ollamaUseCloud) {
                                        OnboardingDocsLink(
                                            label = "Create an Ollama Cloud API key",
                                            url = OLLAMA_CLOUD_KEYS_URL
                                        )
                                        ApiKeyChipEditor(
                                            label = "API keys",
                                            keys = ollamaKeys,
                                            onKeysChange = {
                                                ollamaKeys = it
                                                invalidateAi()
                                            },
                                            isError = aiKeysError != null || aiError != null,
                                            errorMessage = aiKeysError,
                                        )
                                    } else {
                                        OnboardingTextField(
                                            label = "Server URL",
                                            value = ollamaUrl,
                                            keyboardType = KeyboardType.Uri,
                                            isError = ollamaUrlError != null || aiError != null,
                                            errorMessage = ollamaUrlError,
                                        ) {
                                            ollamaUrl = it
                                            invalidateAi()
                                        }
                                    }
                                }

                                AiProvider.CUSTOM, AiProvider.OPENAI -> {
                                    Text(
                                        text = stringResource(R.string.onboarding_use_openai_or_any_compatible_server),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    AI_SETUP_DOC_RES[AiProvider.CUSTOM]?.let { (labelRes, url) ->
                                        OnboardingDocsLink(label = stringResource(labelRes), url = url)
                                    }
                                    OnboardingTextField(
                                        label = "Base URL",
                                        value = customUrl,
                                        keyboardType = KeyboardType.Uri,
                                        isError = customUrlError != null || aiError != null,
                                        errorMessage = customUrlError,
                                    ) {
                                        customUrl = it
                                        invalidateAi()
                                    }
                                    ApiKeyChipEditor(
                                        label = if (AppSettings.isOfficialOpenAiBaseUrl(customUrl)) {
                                            context.getString(R.string.onboarding_api_keys)
                                        } else {
                                            context.getString(R.string.onboarding_api_keys_optional)
                                        },
                                        keys = customKeys,
                                        onKeysChange = {
                                            customKeys = it
                                            invalidateAi()
                                        },
                                        isError = aiKeysError != null || aiError != null,
                                        errorMessage = aiKeysError,
                                    )
                                }
                            }

                            aiError?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Text(
                                text = stringResource(R.string.onboarding_your_credentials_stay_on_device_and),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        step == 1 -> {
                            Text(
                                text = stringResource(R.string.onboarding_about_you),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!skipNameInOnboarding) {
                                OnboardingTextField(
                                    label = "First name",
                                    value = firstName,
                                    isError = firstNameError != null,
                                    errorMessage = firstNameError,
                                ) { firstName = it }
                                OnboardingTextField(
                                    label = "Last name (optional)",
                                    value = lastName,
                                ) { lastName = it }
                            }
                            OnboardingNumberField(
                                label = "Age",
                                value = age,
                                isError = ageError != null,
                                errorMessage = ageError,
                            ) { age = it }
                            OnboardingNumberField(
                                label = "Height (cm)",
                                value = height,
                                decimal = true,
                                isError = heightError != null,
                                errorMessage = heightError,
                            ) { height = it }
                            OnboardingNumberField(
                                label = "Current weight (kg)",
                                value = weight,
                                decimal = true,
                                isError = weightError != null,
                                errorMessage = weightError,
                            ) { weight = it }
                            val sexOptions = SEX_OPTION_RES.map { it.first to stringResource(it.second) }
                            OnboardingDropdown(stringResource(R.string.settings_sex), sex, sexOptions) { sex = it }
                        }

                        else -> {
                            Text(
                                text = stringResource(R.string.onboarding_your_lifestyle),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.onboarding_we_ll_calculate_adult_calorie_and),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val activityOptions = remember(context) {
                                ActivityLevels.optionRes.map { (value, res) ->
                                    value to context.getString(res)
                                }
                            }
                            val activityDescriptions = remember(context) {
                                ActivityLevels.descriptions(context)
                            }
                            OnboardingDropdown(
                                label = stringResource(R.string.onboarding_typical_overall_activity),
                                selectedValue = activity,
                                options = activityOptions,
                                descriptions = activityDescriptions
                            ) { activity = it }
                            Text(
                                text = activityDescriptions[activity].orEmpty() +
                                    stringResource(R.string.onboarding_include_workouts_daily_movement_physical_work),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val goalOptions = GOAL_OPTION_RES.map { it.first to stringResource(it.second) }
                            OnboardingDropdown(stringResource(R.string.onboarding_goal), goal, goalOptions) { goal = it }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            when {
                step == -2 -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { step = -1 },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.backup_back))
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = cloudSupportId.isNotBlank() && !busy,
                            onClick = {
                                cloudError = null
                                onRestoreCloud(
                                    cloudSupportId.trim(),
                                    {
                                        // Prompt for the custom password when the Support-ID
                                        // auto-attempt fails, then await the user's entry.
                                        cloudRestorePassword = ""
                                        cloudRestorePasswordPrompt = true
                                        suspendCancellableCoroutine { cont ->
                                            cloudRestorePasswordContinuation = cont
                                        }
                                    }
                                ) { ok, error ->
                                    if (!ok) {
                                        cloudError = error ?: context.getString(R.string.onboarding_restore_failed)
                                    }
                                    // On success, needsOnboarding / aiOnly gates update via VM.
                                }
                            }
                        ) {
                            Text(if (isRestoring) stringResource(R.string.onboarding_restoring) else stringResource(R.string.onboarding_restore))
                        }
                    }
                }

                !onPathPicker -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (!aiOnly) {
                            OutlinedButton(
                                onClick = { step = if (step == 0) -1 else step - 1 },
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.backup_back))
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            onClick = {
                                when (step) {
                                    0 -> {
                                        if (!aiConfigValid) {
                                            aiAttempted = true
                                        } else {
                                            advanceFromAiStep()
                                        }
                                    }
                                    1 -> {
                                        if (!stepOneValid) {
                                            profileAttempted = true
                                        } else {
                                            step = 2
                                        }
                                    }
                                    else -> step = when {
                                        showCloudStep -> stepCloud
                                        showCrashStep -> stepCrash
                                        else -> stepRegion
                                    }
                                }
                            }
                        ) {
                            Text(
                                when {
                                    isValidating -> stringResource(R.string.onboarding_validating)
                                    isSaving -> stringResource(R.string.onboarding_setting_up)
                                    aiOnly -> stringResource(R.string.onboarding_save_continue)
                                    else -> stringResource(R.string.backup_continue)
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            } // scrollable content

            if (showPathCredit) {
                CraftedWithLoveCredit(
                    onHeartDoubleTap = { confettiKey += 1 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 40.dp)
                )
            } else {
                Spacer(Modifier.height(32.dp))
            }
        }
    }

        if (showConfetti) {
            key(confettiKey) {
                ConfettiOverlay(
                    modifier = Modifier.fillMaxSize(),
                    durationMillis = 4_000,
                    grand = true
                )
            }
        }

        if (cloudRestorePasswordPrompt) {
            fun resume(result: CharArray?) {
                cloudRestorePasswordContinuation?.resumeWith(Result.success(result))
                cloudRestorePasswordContinuation = null
                cloudRestorePasswordPrompt = false
                cloudRestorePassword = ""
            }
            AlertDialog(
                onDismissRequest = { resume(null) },
                title = { Text(stringResource(R.string.main_password_required)) },
                text = {
                    Column {
                        Text(stringResource(R.string.onboarding_this_cloud_backup_is_protected_by),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.size(12.dp))
                        OutlinedTextField(
                            value = cloudRestorePassword,
                            onValueChange = { cloudRestorePassword = it },
                            label = { Text(stringResource(R.string.main_password)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { resume(cloudRestorePassword.toCharArray()) },
                        enabled = cloudRestorePassword.isNotEmpty()
                    ) { Text(stringResource(R.string.main_unlock)) }
                },
                dismissButton = {
                    TextButton(onClick = { resume(null) }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

        if (localRestorePasswordPrompt) {
            fun resume(result: CharArray?) {
                localRestorePasswordContinuation?.resumeWith(Result.success(result))
                localRestorePasswordContinuation = null
                localRestorePasswordPrompt = false
                localRestorePassword = ""
            }
            AlertDialog(
                onDismissRequest = { resume(null) },
                title = { Text(stringResource(R.string.main_password_required)) },
                text = {
                    Column {
                        Text(stringResource(R.string.onboarding_this_backup_is_password_protected_enter),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (localRestorePasswordAttempts > 0) {
                            Spacer(Modifier.size(4.dp))
                            Text(stringResource(R.string.onboarding_incorrect_password_attempt_5, (localRestorePasswordAttempts + 1).toString()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        OutlinedTextField(
                            value = localRestorePassword,
                            onValueChange = { localRestorePassword = it },
                            label = { Text(stringResource(R.string.main_password)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            localRestorePasswordAttempts++
                            resume(localRestorePassword.toCharArray())
                        },
                        enabled = localRestorePassword.isNotEmpty()
                    ) { Text(stringResource(R.string.main_unlock)) }
                },
                dismissButton = {
                    TextButton(onClick = { resume(null) }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }
    }
}

@Composable
private fun OnboardingNumberField(
    label: String,
    value: String,
    decimal: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || (decimal && it == '.') }
            onValueChange(filtered)
        },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = errorMessage?.let { message ->
            {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OnboardingTextField(
    label: String,
    value: String,
    isSecret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = errorMessage?.let { message ->
            {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun onboardingRequiredTextError(
    value: String,
    attempted: Boolean,
    blankMessage: String,
): String? {
    val trimmed = value.trim()
    return when {
        trimmed.isEmpty() -> if (attempted) blankMessage else null
        else -> null
    }
}

private fun onboardingIntRangeError(
    value: String,
    range: IntRange,
    attempted: Boolean,
    label: String,
    context: android.content.Context,
): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        return if (attempted) context.getString(R.string.onboarding_required, label, range.first, range.last) else null
    }
    val parsed = trimmed.toIntOrNull()
        ?: return context.getString(R.string.onboarding_enter_whole_number)
    return if (parsed in range) {
        null
    } else {
        context.getString(R.string.onboarding_must_between, range.first, range.last)
    }
}

private fun onboardingDoubleRangeError(
    value: String,
    min: Double,
    max: Double,
    attempted: Boolean,
    label: String,
    unit: String,
    context: android.content.Context,
): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        return if (attempted) {
            context.getString(R.string.onboarding_required_2, label, min.toInt(), max.toInt(), unit)
        } else {
            null
        }
    }
    val parsed = trimmed.toDoubleOrNull()
        ?: return context.getString(R.string.onboarding_enter_valid_number)
    return if (parsed in min..max) {
        null
    } else {
        context.getString(R.string.onboarding_must_between_2, min.toInt(), max.toInt(), unit)
    }
}

/** Soft gradient help card — visually separate from Start Fresh / Restore actions. */
@Composable
private fun OnboardingGuideCard(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = appShape(18.dp)
    val wash = remember(scheme.primaryContainer, scheme.secondaryContainer, scheme.tertiaryContainer) {
        Brush.linearGradient(
            colors = listOf(
                scheme.primaryContainer.copy(alpha = 0.95f),
                scheme.secondaryContainer.copy(alpha = 0.9f),
                scheme.tertiaryContainer.copy(alpha = 0.75f)
            )
        )
    }
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.22f)),
        colors = CardDefaults.cardColors(
            containerColor = scheme.surface.copy(alpha = 0f),
            contentColor = scheme.onPrimaryContainer,
            disabledContainerColor = scheme.surface.copy(alpha = 0f),
            disabledContentColor = scheme.onPrimaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(wash)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(appShape(14.dp))
                        .background(scheme.surface.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = scheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_need_a_hand),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary.copy(alpha = 0.9f),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.onboarding_onboarding_guide),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.onboarding_walk_through_first_run_setup_in),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    modifier = Modifier
                        .clip(appShape(999.dp))
                        .background(scheme.primary.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_open),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = scheme.primary
                    )
                }
            }
        }
    }
}

/** Tappable link to the provider's key-creation/setup docs, opened in the browser. */
@Composable
private fun OnboardingDocsLink(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingDropdown(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    descriptions: Map<String, String> = emptyMap(),
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == selectedValue }?.second
        ?: options.firstOrNull()?.second.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text)
                            descriptions[value]?.let { description ->
                                Text(
                                    description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
