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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
private const val UPI_ID = "anantdark969817.rzp@rxairtel"
private const val DEVELOPER_EMAIL = "fitbuddy31@proton.me"
private const val UPI_QR_ASPECT_RATIO = 674f / 1644f
private val DONATION_HEART_COLORS = listOf(
    Color(0xFFE91E63),
    Color(0xFFFF5722),
    Color(0xFF9C27B0),
    Color(0xFF1976D2),
    Color(0xFF00897B),
    Color(0xFFFFA000),
)

private enum class UpiPaymentOption { QR, ID }

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
    val maxBodyHeight = (LocalConfiguration.current.screenHeightDp * 0.58f).dp
    var showUpiDialog by rememberSaveable { mutableStateOf(false) }

    if (showUpiDialog) {
        UpiPaymentDialog(onBack = { showUpiDialog = false })
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = heartColor,
            )
        },
        title = { Text("Support FitBuddy development") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxBodyHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "FitBuddy is free and open source. Your optional contribution helps " +
                        "support continued development, maintenance, and new features.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Choose a payment method",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Button(
                    onClick = { showUpiDialog = true },
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
                    Text("Pay with card")
                }

                HorizontalDivider()
                Text(
                    "Contact developer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
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

                Text(
                    "Payment apps and Razorpay open only when you choose them. FitBuddy does " +
                        "not receive or store your payment details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun UpiPaymentDialog(onBack: () -> Unit) {
    val context = LocalContext.current
    val maxBodyHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    var selectedOption by rememberSaveable { mutableStateOf<UpiPaymentOption?>(null) }

    AlertDialog(
        onDismissRequest = onBack,
        icon = {
            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null)
        },
        title = { Text("Pay with UPI") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxBodyHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Choose how you want to complete your UPI payment.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedButton(
                    onClick = { selectedOption = UpiPaymentOption.QR },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Pay with UPI QR")
                }

                OutlinedButton(
                    onClick = { selectedOption = UpiPaymentOption.ID },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.AlternateEmail, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Pay with UPI ID")
                }

                Button(
                    onClick = { openUpiApp(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Pay with UPI app")
                }

                when (selectedOption) {
                    UpiPaymentOption.QR -> {
                        HorizontalDivider()
                        Text(
                            "Scan from another device",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Image(
                            painter = painterResource(R.drawable.donation_upi_qr),
                            contentDescription =
                                "UPI QR code for supporting FitBuddy development",
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
                    UpiPaymentOption.ID -> {
                        HorizontalDivider()
                        Text(
                            "UPI ID",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            UPI_ID,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        OutlinedButton(
                            onClick = { copyUpiId(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Copy UPI ID")
                        }
                    }
                    null -> Unit
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onBack) { Text("Back") }
        },
    )
}

private fun copyUpiId(context: Context) {
    copyToClipboard(
        context = context,
        label = "FitBuddy UPI ID",
        text = UPI_ID,
        confirmation = "UPI ID copied",
    )
}

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
    val uri = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", UPI_ID)
        .appendQueryParameter("pn", "Anantdark")
        .appendQueryParameter("tn", "Support FitBuddy development")
        .appendQueryParameter("cu", "INR")
        .build()
    launchExternalIntent(
        context = context,
        intent = Intent(Intent.ACTION_VIEW, uri),
        errorMessage = "No UPI app found",
    )
}

private fun openRazorpayPaymentPage(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RAZORPAY_DONATION_URL)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    launchExternalIntent(context, intent, "Couldn't open the payment page")
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
