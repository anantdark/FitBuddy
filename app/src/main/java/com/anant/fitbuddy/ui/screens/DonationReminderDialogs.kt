package com.anant.fitbuddy.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.anant.fitbuddy.data.donors.DonorEntry
import com.anant.fitbuddy.reminders.DonationReminderCopy
import com.anant.fitbuddy.util.SystemToast

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
                        "(and the features) flowing. No guilt, just a weekly nudge.",
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

@Composable
internal fun NewDonorsThankYouDialog(
    donors: List<DonorEntry>,
    onDismiss: () -> Unit,
) {
    if (donors.isEmpty()) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thanks to our newest supporters") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "These folks helped keep FitBuddy free and open. You're awesome.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                donors.forEach { donor ->
                    DonorRow(donor)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cheers") }
        },
    )
}

@Composable
internal fun DonorRow(donor: DonorEntry) {
    val name = donor.displayName ?: return
    val context = LocalContext.current
    val link = donor.profileLink
    val photo = donor.photoUrl?.trim().orEmpty()
    var showLetterAvatar by remember(photo) { mutableStateOf(photo.isEmpty()) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (link != null) {
                    Modifier.clickable {
                        val uri = runCatching { Uri.parse(link) }.getOrNull()
                        if (uri == null || uri.scheme.isNullOrBlank()) {
                            SystemToast.show(context, "Couldn't open link")
                            return@clickable
                        }
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            addCategory(Intent.CATEGORY_BROWSABLE)
                        }
                        runCatching { context.startActivity(intent) }
                            .onFailure {
                                SystemToast.show(context, "Couldn't open link")
                            }
                    }
                } else {
                    Modifier
                }
            ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (!showLetterAvatar && photo.isNotEmpty()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        onState = { state ->
                            if (state is AsyncImagePainter.State.Error) {
                                showLetterAvatar = true
                            }
                        },
                    )
                }
                if (showLetterAvatar) {
                    Surface(
                        shape = CircleShape,
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (link != null) TextDecoration.Underline else TextDecoration.None,
                modifier = Modifier.weight(1f),
            )
            if (link != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open profile",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
internal fun DemoDonorsPreview(donors: List<DonorEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Demo donor data",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Preview of how named donors (and hash-only rows) appear. Thank-you skips hash-only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        donors.forEach { donor ->
            if (donor.hasDisplayInfo) {
                DonorRow(donor)
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
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
}
