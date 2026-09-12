package com.anant.fitbuddy.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.R
import com.anant.fitbuddy.ui.components.IconButton
import com.anant.fitbuddy.ui.components.TextButton
import com.anant.fitbuddy.util.SystemToast

private const val RAZORPAY_DONATION_URL = "https://rzp.io/rzp/fitbuddy"
private const val GITHUB_SPONSORS_URL = "https://github.com/sponsors/anantdark"
private const val UPI_PAYMENT_URI =
    "upi://pay?cu=INR&mc=5817&mode=19&pa=anantdark969817.rzp@rxairtel&" +
        "tn=Payment%20To%20Anantdark&tr=TaiXaWZZm9gukCqrv2"
private const val DEVELOPER_EMAIL = "fitbuddy31@proton.me"
private const val UPI_QR_ASPECT_RATIO = 674f / 1644f
private const val DONATION_BODY_HEIGHT_FRACTION = 0.5f
private val DONATION_DIALOG_MAX_WIDTH = 560.dp
private val DONATION_BODY_MIN_HEIGHT = 220.dp
private val DONATION_BODY_MAX_HEIGHT = 420.dp
private val DONATION_HEART_COLORS = listOf(
    Color(0xFFE91E63),
    Color(0xFFFF5722),
    Color(0xFF9C27B0),
    Color(0xFF1976D2),
    Color(0xFF00897B),
    Color(0xFFFFA000),
)

private enum class DonationStep { REGION, INDIA, INTERNATIONAL, UPI }

internal fun initialDonationHeartColorIndex(): Int =
    (System.currentTimeMillis() % DONATION_HEART_COLORS.size).toInt()

internal fun nextDonationHeartColorIndex(current: Int): Int =
    (current + 1) % DONATION_HEART_COLORS.size

internal fun donationHeartColor(index: Int): Color =
    DONATION_HEART_COLORS[index.mod(DONATION_HEART_COLORS.size)]

@Composable
internal fun MainTopBarActions(
    donationColor: Color,
    onDonate: () -> Unit,
    onSettings: () -> Unit,
) {
    IconButton(onClick = onDonate) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Support FitBuddy",
            tint = donationColor,
        )
    }
    IconButton(onClick = onSettings) {
        Icon(Icons.Filled.Settings, contentDescription = "Settings")
    }
}

@Composable
internal fun DonationDialog(
    heartColor: Color,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableStateOf(DonationStep.REGION) }
    var showQr by rememberSaveable { mutableStateOf(false) }
    val title = when (step) {
        DonationStep.REGION -> "Support FitBuddy development"
        DonationStep.INDIA -> "Donate from India"
        DonationStep.INTERNATIONAL -> "Donate internationally"
        DonationStep.UPI -> "Pay with UPI"
    }
    val icon = when (step) {
        DonationStep.REGION, DonationStep.INTERNATIONAL -> Icons.Filled.Favorite
        DonationStep.INDIA, DonationStep.UPI -> Icons.Filled.AccountBalanceWallet
    }
    val navigateBack = {
        step = when (step) {
            DonationStep.REGION -> DonationStep.REGION
            DonationStep.INDIA, DonationStep.INTERNATIONAL -> DonationStep.REGION
            DonationStep.UPI -> DonationStep.INDIA
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (step == DonationStep.REGION) onDismiss() else navigateBack()
        },
        modifier = donationDialogModifier(),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (step == DonationStep.REGION) {
                    heartColor
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        },
        title = { Text(title) },
        text = {
            DonationDialogContent {
                when (step) {
                    DonationStep.REGION -> DonationRegionContent(
                        context = context,
                        onIndia = { step = DonationStep.INDIA },
                        onInternational = { step = DonationStep.INTERNATIONAL },
                    )
                    DonationStep.INDIA -> IndiaDonationContent(
                        context = context,
                        onUpi = {
                            showQr = false
                            step = DonationStep.UPI
                        },
                    )
                    DonationStep.INTERNATIONAL -> InternationalDonationContent(context)
                    DonationStep.UPI -> UpiDonationContent(
                        context = context,
                        showQr = showQr,
                        onShowQr = { showQr = true },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (step == DonationStep.REGION) {
                TextButton(onClick = onDismiss) { Text("Close") }
            } else {
                DonationNavigationActions(
                    onBack = navigateBack,
                    onClose = onDismiss,
                )
            }
        },
    )
}

@Composable
private fun DonationRegionContent(
    context: Context,
    onIndia: () -> Unit,
    onInternational: () -> Unit,
) {
    DonationInfoCard(
        icon = Icons.Filled.Favorite,
        text = "FitBuddy is free and open source. Your optional contribution supports " +
            "continued development, maintenance, and new features.",
    )
    DonationSectionTitle("Choose your region")
    OutlinedButton(
        onClick = onIndia,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("India")
    }
    OutlinedButton(
        onClick = onInternational,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.Public, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("International")
    }
    HorizontalDivider()
    DonationSectionTitle("Contact developer")
    Text(
        DEVELOPER_EMAIL,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
    )
    OutlinedButton(
        onClick = { copyDeveloperEmail(context) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.ContentCopy, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Copy email")
    }
}

@Composable
private fun IndiaDonationContent(
    context: Context,
    onUpi: () -> Unit,
) {
    DonationInfoCard(
        icon = Icons.Filled.AccountBalanceWallet,
        text = "Choose UPI for a quick payment or use Razorpay for a debit or credit " +
            "card contribution.",
    )
    DonationSectionTitle("Payment method")
    Button(
        onClick = onUpi,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Pay with UPI")
    }
    OutlinedButton(
        onClick = { openRazorpayPaymentPage(context) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.CreditCard, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Pay by card with Razorpay")
    }
    DonationPrivacyNote(
        "Razorpay and payment apps open only when you choose them. FitBuddy does not " +
            "receive or store your payment details.",
    )
}

@Composable
private fun InternationalDonationContent(context: Context) {
    DonationInfoCard(
        icon = Icons.Filled.Public,
        text = "Support FitBuddy development securely through GitHub Sponsors.",
    )
    DonationSectionTitle("International payment")
    Button(
        onClick = { openGitHubSponsorsPage(context) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Sponsor on GitHub")
    }
    DonationPrivacyNote(
        "GitHub Sponsors opens only when you choose it. FitBuddy does not receive or " +
            "store your payment details.",
    )
}

@Composable
private fun UpiDonationContent(
    context: Context,
    showQr: Boolean,
    onShowQr: () -> Unit,
) {
    DonationInfoCard(
        icon = Icons.Filled.AccountBalanceWallet,
        text = "Open a UPI app on this device or scan the QR from another device.",
    )
    Button(
        onClick = { openUpiApp(context) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Pay with UPI app")
    }
    OutlinedButton(
        onClick = onShowQr,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.QrCode2, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Pay with UPI QR")
    }

    if (showQr) {
        HorizontalDivider()
        DonationSectionTitle("Scan from another device")
        Image(
            painter = painterResource(R.drawable.donation_upi_qr),
            contentDescription = "UPI QR code for supporting FitBuddy development",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(UPI_QR_ASPECT_RATIO)
                .clip(RoundedCornerShape(12.dp)),
        )
        Text(
            "Before paying, verify that the recipient is Anantdark.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DonationDialogContent(content: @Composable ColumnScope.() -> Unit) {
    val height = (LocalConfiguration.current.screenHeightDp * DONATION_BODY_HEIGHT_FRACTION)
        .dp
        .coerceIn(DONATION_BODY_MIN_HEIGHT, DONATION_BODY_MAX_HEIGHT)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun DonationInfoCard(
    icon: ImageVector,
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DonationSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun DonationPrivacyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DonationNavigationActions(
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        TextButton(onClick = onClose) { Text("Close") }
    }
}

private fun donationDialogModifier(): Modifier = Modifier
    .fillMaxWidth()
    .widthIn(max = DONATION_DIALOG_MAX_WIDTH)

private fun copyDeveloperEmail(context: Context) {
    copyToClipboard(
        context = context,
        label = "FitBuddy developer email",
        text = DEVELOPER_EMAIL,
        confirmation = "Email copied",
    )
}

private fun copyToClipboard(
    context: Context,
    label: String,
    text: String,
    confirmation: String,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    if (clipboard == null) {
        SystemToast.show(context, "Couldn't access the clipboard")
        return
    }
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    SystemToast.show(context, confirmation)
}

private fun openUpiApp(context: Context) {
    launchExternalIntent(
        context = context,
        intent = Intent(Intent.ACTION_VIEW, Uri.parse(UPI_PAYMENT_URI)),
        errorMessage = "No UPI app found",
    )
}

private fun openRazorpayPaymentPage(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RAZORPAY_DONATION_URL)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    launchExternalIntent(context, intent, "Couldn't open the payment page")
}

private fun openGitHubSponsorsPage(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_SPONSORS_URL)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    launchExternalIntent(context, intent, "Couldn't open GitHub Sponsors")
}

private fun launchExternalIntent(
    context: Context,
    intent: Intent,
    errorMessage: String,
) {
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { SystemToast.show(context, errorMessage) }
}
