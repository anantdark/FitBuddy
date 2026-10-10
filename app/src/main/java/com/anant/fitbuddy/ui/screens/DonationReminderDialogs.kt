package com.anant.fitbuddy.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.anant.fitbuddy.ui.theme.appControlShape
import com.anant.fitbuddy.ui.theme.appShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.anant.fitbuddy.data.donors.DonorEntry
import com.anant.fitbuddy.reminders.DonationReminderCopy
import com.anant.fitbuddy.ui.components.ConfettiOverlay
import com.anant.fitbuddy.ui.components.DonorTradingCardDialog
import com.anant.fitbuddy.util.SystemToast
import kotlinx.coroutines.delay

@Composable
internal fun DonationReminderDialog(
    onSoftDismiss: () -> Unit,
    onAlreadyPaid: () -> Unit,
    onPay: () -> Unit,
    onDismissRequest: () -> Unit = onSoftDismiss,
) {
    val labels = remember { DonationReminderCopy.randomLabels() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Support FitBuddy?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "FitBuddy is free and open source. Optional tips keep the chai " +
                        "(and the features) flowing. No guilt, just an occasional nudge.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = onPay,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(labels.pay)
                }
                FilledTonalButton(
                    onClick = onAlreadyPaid,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(labels.paid)
                }
                OutlinedButton(
                    onClick = onSoftDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(labels.soft)
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

@Composable
internal fun AlreadyPaidDonorPrompt(
    supportId: String,
    developerEmail: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Get on the donor list") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Email me your name and Support ID so I can add you. " +
                        "Reminders stop once your ID is on the list.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Support ID",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    supportId.ifBlank { "(not generated yet)" },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val body = buildString {
                        appendLine("Hi,")
                        appendLine()
                        appendLine("I'd like to be added to the FitBuddy donor list.")
                        appendLine()
                        appendLine("Name: ")
                        appendLine("Support ID: $supportId")
                        appendLine()
                        appendLine("Thanks!")
                    }
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(developerEmail))
                        putExtra(Intent.EXTRA_SUBJECT, "FitBuddy donor list")
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            SystemToast.show(context, "No email app found")
                        }
                }
            ) {
                Text("Email developer")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (supportId.isNotBlank()) {
                        val clipboard = context.getSystemService(
                            android.content.ClipboardManager::class.java
                        )
                        clipboard?.setPrimaryClip(
                            android.content.ClipData.newPlainText("FitBuddy Support ID", supportId)
                        )
                        SystemToast.show(context, "Support ID copied")
                    }
                    onDismiss()
                }
            ) {
                Text("Copy ID & close")
            }
        },
    )
}

private const val THANK_YOU_LOCK_SECONDS = 5

@Composable
internal fun NewDonorsThankYouDialog(
    donors: List<DonorEntry>,
    onDismiss: () -> Unit,
) {
    SupportersListDialog(
        donors = donors,
        onDismiss = onDismiss,
        titleSingular = "Thanks to our newest supporter",
        titlePlural = "Thanks to our newest supporters",
        bodySingular = "This person helped keep FitBuddy free and open. " +
            "Take a moment to celebrate them.",
        bodyPlural = "These folks helped keep FitBuddy free and open. " +
            "Take a moment to celebrate them.",
        lockSeconds = THANK_YOU_LOCK_SECONDS,
        confetti = true,
        confirmLabel = "Cheers",
    )
}

@Composable
internal fun DonorsGalleryDialog(
    donors: List<DonorEntry>,
    onDismiss: () -> Unit,
) {
    if (donors.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Supporters") },
            text = {
                Text("No public supporters yet. Be the first — tip from the heart button.")
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            },
        )
        return
    }
    SupportersListDialog(
        donors = donors,
        onDismiss = onDismiss,
        titleSingular = "FitBuddy supporter",
        titlePlural = "FitBuddy supporters",
        bodySingular = "Tap a row to open their supporter card.",
        bodyPlural = "Tap a row to open a supporter card. Profiles live on the flip side.",
        lockSeconds = 0,
        confetti = false,
        confirmLabel = "Close",
    )
}

@Composable
private fun SupportersListDialog(
    donors: List<DonorEntry>,
    onDismiss: () -> Unit,
    titleSingular: String,
    titlePlural: String,
    bodySingular: String,
    bodyPlural: String,
    lockSeconds: Int,
    confetti: Boolean,
    confirmLabel: String,
) {
    if (donors.isEmpty()) return
    val plural = donors.size != 1
    val maxBodyHeight = (LocalConfiguration.current.screenHeightDp * 0.48f)
        .dp
        .coerceIn(240.dp, 420.dp)

    var secondsLeft by remember { mutableIntStateOf(lockSeconds) }
    val canDismiss = lockSeconds <= 0 || secondsLeft <= 0
    LaunchedEffect(lockSeconds) {
        if (lockSeconds <= 0) return@LaunchedEffect
        secondsLeft = lockSeconds
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        }
    }

    val heartScale by animateFloatAsState(
        targetValue = if (canDismiss) 1.08f else 1f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "thankYouHeart",
    )

    var expandedDonor by remember { mutableStateOf<DonorEntry?>(null) }

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (confetti) {
                ConfettiOverlay(
                    modifier = Modifier.fillMaxSize(),
                    durationMillis = 4_200,
                    grand = true,
                )
            }
            Surface(
                shape = appShape(28.dp),
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surface,
                                    )
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 20.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .scale(heartScale),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = if (plural) titlePlural else titleSingular,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = if (plural) bodyPlural else bodySingular,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxBodyHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        donors.forEach { donor ->
                            DonorRow(
                                donor = donor,
                                onOpenCard = { expandedDonor = donor },
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                    ) {
                        Button(
                            onClick = onDismiss,
                            enabled = canDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = appShape(14.dp),
                        ) {
                            Text(
                                text = if (canDismiss || lockSeconds <= 0) {
                                    confirmLabel
                                } else {
                                    "$confirmLabel ($secondsLeft)"
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    expandedDonor?.let { donor ->
        DonorTradingCardDialog(
            donor = donor,
            onDismiss = { expandedDonor = null },
        )
    }
}

@Composable
internal fun DonorRow(
    donor: DonorEntry,
    onOpenCard: () -> Unit,
) {
    val name = donor.displayName ?: return
    val photo = donor.photoUrl?.trim().orEmpty()
    var showLetterAvatar by remember(photo) { mutableStateOf(photo.isEmpty()) }

    Surface(
        shape = appShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpenCard),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val avatarShape = appControlShape(CircleShape)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(avatarShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!showLetterAvatar && photo.isNotEmpty()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = "Photo of $name",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(avatarShape),
                        onState = { state ->
                            if (state is AsyncImagePainter.State.Error) {
                                showLetterAvatar = true
                            }
                        },
                    )
                }
                if (showLetterAvatar) {
                    Surface(
                        shape = avatarShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Tap for supporter card",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
internal fun DemoDonorsPreview(donors: List<DonorEntry>) {
    var expandedDonor by remember { mutableStateOf<DonorEntry?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Demo donor data",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Preview of named donors (and hash-only rows). Thank-you skips hash-only. " +
                "Tap a row for the trading card.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        donors.forEach { donor ->
            if (donor.hasDisplayInfo) {
                DonorRow(
                    donor = donor,
                    onOpenCard = { expandedDonor = donor },
                )
            } else {
                Surface(
                    shape = appShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Hash-only · hidden from thank-you (${donor.hash.take(12)}…)",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    expandedDonor?.let { donor ->
        DonorTradingCardDialog(
            donor = donor,
            onDismiss = { expandedDonor = null },
        )
    }
}
