package com.anant.fitbuddy.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.anant.fitbuddy.ui.theme.appShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.anant.fitbuddy.data.donors.DonorBadgeRules
import com.anant.fitbuddy.data.donors.DonorCardStyle
import com.anant.fitbuddy.data.donors.DonorEntry
import com.anant.fitbuddy.data.donors.DonorLore
import com.anant.fitbuddy.data.donors.SupporterBadge
import com.anant.fitbuddy.util.SystemToast
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class CardPalette(
    val frame: List<Color>,
    val shellTop: Color,
    val shellBottom: Color,
    val inner: Color,
    val ink: Color,
    val accent: Color,
    val badgeLabel: String,
    val subtitle: String,
)

private fun paletteFor(style: DonorCardStyle): CardPalette = when (style) {
    // Vintage copper / parchment — clearly warm-brown, not gold.
    DonorCardStyle.OG -> CardPalette(
        frame = listOf(Color(0xFF8D6E63), Color(0xFFFFCC80), Color(0xFF5D4037), Color(0xFFBCAAA4)),
        shellTop = Color(0xFF6D4C41),
        shellBottom = Color(0xFF3E2723),
        inner = Color(0xFFFFF3E0),
        ink = Color(0xFF3E2723),
        accent = Color(0xFFFFAB40),
        badgeLabel = "OG SUPPORTER",
        subtitle = "FitBuddy · Origin circle",
    )
    // Bright emerald / mint — distinct from OG brown and Legendary purple.
    DonorCardStyle.GENEROUS -> CardPalette(
        frame = listOf(Color(0xFF00C853), Color(0xFFB9F6CA), Color(0xFF00BFA5), Color(0xFF69F0AE)),
        shellTop = Color(0xFF1B5E20),
        shellBottom = Color(0xFF0A2F12),
        inner = Color(0xFFE8F5E9),
        ink = Color(0xFF1B5E20),
        accent = Color(0xFF00E676),
        badgeLabel = "GENEROUS",
        subtitle = "FitBuddy · Warm boost",
    )
    // Electric violet / cyan foil on deep indigo.
    DonorCardStyle.LEGENDARY -> CardPalette(
        frame = listOf(Color(0xFF7C4DFF), Color(0xFF18FFFF), Color(0xFFE040FB), Color(0xFF536DFE)),
        shellTop = Color(0xFF311B92),
        shellBottom = Color(0xFF12005E),
        inner = Color(0xFF1A237E),
        ink = Color(0xFFE8EAF6),
        accent = Color(0xFF18FFFF),
        badgeLabel = "LEGENDARY",
        subtitle = "FitBuddy · Rare aura",
    )
    // Obsidian + molten gold / crimson — top of the ladder.
    DonorCardStyle.GODLIKE -> CardPalette(
        frame = listOf(
            Color(0xFFFFD700),
            Color(0xFFFF1744),
            Color(0xFFFFEA00),
            Color(0xFFFF6D00),
            Color(0xFFFFD700),
        ),
        shellTop = Color(0xFF4A0000),
        shellBottom = Color(0xFF0A0000),
        inner = Color(0xFF1A0500),
        ink = Color(0xFFFFF8E1),
        accent = Color(0xFFFFD700),
        badgeLabel = "GODLIKE",
        subtitle = "FitBuddy · Mythic seal",
    )
    DonorCardStyle.STANDARD -> CardPalette(
        frame = listOf(Color(0xFF78909C), Color(0xFFECEFF1), Color(0xFF546E7A)),
        shellTop = Color(0xFF455A64),
        shellBottom = Color(0xFF263238),
        inner = Color(0xFFCFD8DC),
        ink = Color(0xFF263238),
        accent = Color(0xFF90A4AE),
        badgeLabel = "SUPPORTER",
        subtitle = "FitBuddy · Community",
    )
}

internal enum class DonorProfileKind { GITHUB, INSTAGRAM, WEBSITE }

internal fun donorProfileKind(url: String): DonorProfileKind {
    val host = runCatching { Uri.parse(url).host?.lowercase().orEmpty() }.getOrDefault("")
    return when {
        host == "github.com" || host.endsWith(".github.com") || host == "www.github.com" ->
            DonorProfileKind.GITHUB
        host.contains("instagram.com") -> DonorProfileKind.INSTAGRAM
        else -> DonorProfileKind.WEBSITE
    }
}

internal fun donorProfileHandle(url: String, kind: DonorProfileKind): String {
    val path = runCatching { Uri.parse(url).path?.trim('/').orEmpty() }.getOrDefault("")
    val first = path.substringBefore('/').ifBlank { path }
    return when (kind) {
        DonorProfileKind.GITHUB -> if (first.isNotBlank()) "@$first" else "GitHub"
        DonorProfileKind.INSTAGRAM -> if (first.isNotBlank()) "@$first" else "Instagram"
        DonorProfileKind.WEBSITE -> runCatching { Uri.parse(url).host ?: "Website" }
            .getOrDefault("Website")
    }
}

/**
 * Full-screen trading-card reveal: holographic sheen, tap to flip, profile panel.
 */
@Composable
fun DonorTradingCardDialog(
    donor: DonorEntry,
    onDismiss: () -> Unit,
) {
    val name = donor.displayName ?: return
    val style = remember(donor) { DonorBadgeRules.cardStyle(donor) }
    val badges = remember(donor) { DonorBadgeRules.badges(donor) }
    val palette = remember(style) { paletteFor(style) }
    val lore = remember(donor, style) { DonorLore.forDonor(donor, style) }
    val supporterNumber = remember(donor.rosterIndex) {
        if (donor.rosterIndex >= 0) DonorLore.supporterNumber(donor.rosterIndex) else null
    }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val enterScale = remember { Animatable(0.72f) }
    val enterAlpha = remember { Animatable(0f) }
    val flip = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }
    var showProfilePanel by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 420f),
        label = "cardPress",
    )

    LaunchedEffect(Unit) {
        launch {
            enterAlpha.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        }
        enterScale.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = 220f))
    }

    val shimmer = rememberInfiniteTransition(label = "cardShimmer")
    val shimmerShift by shimmer.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (style) {
                    DonorCardStyle.GODLIKE -> 1800
                    DonorCardStyle.LEGENDARY -> 2200
                    else -> 2800
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerShift",
    )

    val showingBack = abs(((flip.value % 360f) + 360f) % 360f - 180f) < 90f
    val cameraDistancePx = with(density) { 24.dp.toPx() } * 8f

    Dialog(
        onDismissRequest = {
            if (showProfilePanel) showProfilePanel = false else onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f * enterAlpha.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (showProfilePanel) showProfilePanel = false else onDismiss()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            ConfettiOverlay(
                modifier = Modifier.fillMaxSize(),
                durationMillis = 3_600,
                grand = style == DonorCardStyle.GODLIKE || style == DonorCardStyle.LEGENDARY,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .graphicsLayer { alpha = enterAlpha.value }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Text(
                    text = when (style) {
                        DonorCardStyle.OG -> "OG SUPPORTER CARD"
                        DonorCardStyle.GENEROUS -> "GENEROUS CARD"
                        DonorCardStyle.LEGENDARY -> "LEGENDARY CARD"
                        DonorCardStyle.GODLIKE -> "GODLIKE CARD"
                        DonorCardStyle.STANDARD -> "SUPPORTER CARD"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = palette.accent,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(2.5f / 3.5f)
                        .graphicsLayer {
                            scaleX = enterScale.value * pressScale
                            scaleY = enterScale.value * pressScale
                            rotationY = flip.value
                            cameraDistance = cameraDistancePx
                            transformOrigin = TransformOrigin.Center
                            shadowElevation = 36f
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    pressed = true
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        pressed = false
                                    }
                                },
                                onTap = {
                                    if (flip.isRunning) return@detectTapGestures
                                    scope.launch {
                                        val target = if (flip.value < 90f) 180f else 0f
                                        flip.animateTo(
                                            target,
                                            spring(dampingRatio = 0.38f, stiffness = 200f),
                                        )
                                    }
                                },
                            )
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = if (showingBack) 180f else 0f
                                cameraDistance = cameraDistancePx
                            },
                    ) {
                        if (showingBack) {
                            DonorCardBack(
                                name = name,
                                lore = lore,
                                badges = badges,
                                palette = palette,
                                linkUrl = donor.profileLink,
                                onOpenProfile = {
                                    if (donor.profileLink != null) showProfilePanel = true
                                },
                            )
                        } else {
                            DonorCardFront(
                                donor = donor,
                                name = name,
                                badges = badges,
                                palette = palette,
                                supporterNumber = supporterNumber,
                                sheenX = 0.35f,
                                sheenY = 0.28f,
                                shimmerShift = shimmerShift,
                                intenseSheen = style == DonorCardStyle.GODLIKE ||
                                    style == DonorCardStyle.LEGENDARY,
                            )
                        }
                    }
                }

                Text(
                    text = "Tap card to flip",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color.White)
                }
            }

            if (showProfilePanel) {
                donor.profileLink?.let { url ->
                    DonorProfileDestinationPanel(
                        url = url,
                        name = name,
                        onDismiss = { showProfilePanel = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun DonorCardFront(
    donor: DonorEntry,
    name: String,
    badges: List<SupporterBadge>,
    palette: CardPalette,
    supporterNumber: Int?,
    sheenX: Float,
    sheenY: Float,
    shimmerShift: Float,
    intenseSheen: Boolean,
) {
    val photo = donor.photoUrl?.trim().orEmpty()
    var showLetter by remember(photo) { mutableStateOf(photo.isEmpty()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(appShape(18.dp))
            .background(Brush.verticalGradient(listOf(palette.shellTop, palette.shellBottom)))
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(palette.frame),
                shape = appShape(18.dp),
            )
            .drawWithContent {
                drawContent()
                val peak = if (intenseSheen) 0.45f else 0.28f
                val shimmerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = peak),
                        palette.accent.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                    start = Offset(size.width * (shimmerShift - 0.35f), 0f),
                    end = Offset(size.width * (shimmerShift + 0.15f), size.height),
                )
                drawRect(brush = shimmerBrush)
                val touchSheen = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (intenseSheen) 0.34f else 0.22f),
                        palette.accent.copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * sheenX, size.height * sheenY),
                    radius = size.minDimension * 0.55f,
                )
                drawRect(brush = touchSheen)
            }
            .padding(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(appShape(12.dp))
                .background(palette.inner)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(appShape(8.dp))
                    .background(Brush.horizontalGradient(palette.frame))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = palette.ink,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = buildString {
                            if (supporterNumber != null) append("No. $supporterNumber  ·  ")
                            append(palette.badgeLabel)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.ink,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(appShape(10.dp))
                    .background(palette.ink.copy(alpha = 0.08f))
                    .border(1.dp, palette.accent.copy(alpha = 0.45f), appShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (!showLetter && photo.isNotEmpty()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onState = { state ->
                            if (state is AsyncImagePainter.State.Error) {
                                showLetter = true
                            }
                        },
                    )
                }
                if (showLetter) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(palette.accent, palette.shellTop),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = name.take(1).uppercase(),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = palette.ink,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    color = palette.ink,
                    maxLines = 2,
                )
                Text(
                    text = palette.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accent,
                )
            }

            if (badges.isNotEmpty()) {
                Text(
                    text = "RANK CRESTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = palette.ink.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                DonorMedalRow(
                    badges = badges,
                    labelColor = palette.ink,
                    compact = true,
                )
            }
            DonorInsightRow(donor = donor, ink = palette.ink)
        }
    }
}

/** Supporter facts only — never amounts or tier math. */
private data class DonorInsight(
    val label: String,
    val value: String,
)

private fun donorInsights(donor: DonorEntry): List<DonorInsight> {
    val dates = donor.donations.map { it.normalizedAt }.filter { it.isNotEmpty() }.sorted()
    val since = dates.firstOrNull()?.let { formatDonorMonthYear(it) } ?: "—"
    val tips = donor.donations.size.coerceAtLeast(0)
    val tipsLabel = when (tips) {
        0 -> "—"
        1 -> "1×"
        else -> "${tips}×"
    }
    val home = when (val link = donor.profileLink) {
        null -> "Private"
        else -> when (donorProfileKind(link)) {
            DonorProfileKind.GITHUB -> "GitHub"
            DonorProfileKind.INSTAGRAM -> "Instagram"
            DonorProfileKind.WEBSITE -> "Web"
        }
    }
    return listOf(
        DonorInsight("Since", since),
        DonorInsight("Tips", tipsLabel),
        DonorInsight("Home", home),
    )
}

/** `yyyy-MM-dd` → `Jan '26` (no amounts). */
private fun formatDonorMonthYear(isoDate: String): String {
    val parts = isoDate.trim().split('-')
    if (parts.size < 2) return "—"
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return "—"
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )
    val mon = months.getOrNull(month - 1) ?: return "—"
    val yy = year.takeLast(2)
    return "$mon '$yy"
}

@Composable
private fun DonorInsightRow(
    donor: DonorEntry,
    ink: Color,
) {
    val insights = remember(donor) { donorInsights(donor) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(appShape(8.dp))
            .background(ink.copy(alpha = 0.06f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        insights.forEach { insight ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = insight.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = insight.value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DonorCardBack(
    name: String,
    lore: String,
    badges: List<SupporterBadge>,
    palette: CardPalette,
    linkUrl: String?,
    onOpenProfile: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(appShape(18.dp))
            .background(Brush.verticalGradient(listOf(palette.shellTop, palette.shellBottom)))
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(palette.frame),
                shape = appShape(18.dp),
            )
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = "OFFICIAL LORE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = palette.accent,
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = lore,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (badges.isNotEmpty()) {
                    DonorMedalRow(
                        badges = badges,
                        labelColor = Color.White,
                        compact = false,
                    )
                }
                if (linkUrl != null) {
                    Surface(
                        shape = appShape(12.dp),
                        color = palette.accent.copy(alpha = 0.18f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenProfile),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Open profile",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Text(
                    text = "FitBuddy Supporter Series",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

/**
 * Anime crest kit — Twemoji cores (CC-BY 4.0) + Compose aura frames.
 * Vibes: shonen rank seals / magical crest UI.
 */
private fun animeEmblemUrl(badge: SupporterBadge): String {
    // Larger 72px Twemoji via jsDelivr
    val code = when (badge) {
        is SupporterBadge.Og -> "2694" // crossed swords — founding warrior
        is SupporterBadge.Generous -> "1f338" // cherry blossom — kind aura
        is SupporterBadge.Legendary -> "1f409" // dragon — rare power
        is SupporterBadge.Godlike -> "1f525" // fire — divine blaze (paired with crown ring)
        is SupporterBadge.Custom -> "2728" // sparkles
    }
    return "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/$code.png"
}

private data class AnimeCrestTheme(
    val kanji: String,
    val rankEn: String,
    val aura: List<Color>,
    val core: List<Color>,
    val spark: Color,
    val stars: Int,
)

private fun animeTheme(badge: SupporterBadge): AnimeCrestTheme = when (badge) {
    is SupporterBadge.Og -> AnimeCrestTheme(
        kanji = "始",
        rankEn = "ORIGIN",
        aura = listOf(Color(0xFFFF8A65), Color(0xFFFFCC80), Color(0xFF6D4C41)),
        core = listOf(Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFF3E2723)),
        spark = Color(0xFFFFAB40),
        stars = 2,
    )
    is SupporterBadge.Generous -> AnimeCrestTheme(
        kanji = "慈",
        rankEn = "BLESS",
        aura = listOf(Color(0xFF69F0AE), Color(0xFFB9F6CA), Color(0xFFFce4ec), Color(0xFF00C853)),
        core = listOf(Color(0xFFFFF0F5), Color(0xFF66BB6A), Color(0xFF1B5E20)),
        spark = Color(0xFFFF80AB),
        stars = 3,
    )
    is SupporterBadge.Legendary -> AnimeCrestTheme(
        kanji = "龍",
        rankEn = "MYTHIC",
        aura = listOf(Color(0xFF18FFFF), Color(0xFF7C4DFF), Color(0xFFE040FB), Color(0xFF304FFE)),
        core = listOf(Color(0xFFE8EAF6), Color(0xFF651FFF), Color(0xFF1A237E)),
        spark = Color(0xFF18FFFF),
        stars = 4,
    )
    is SupporterBadge.Godlike -> AnimeCrestTheme(
        kanji = "神",
        rankEn = "DIVINE",
        aura = listOf(Color(0xFFFFD700), Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFFFF6D00)),
        core = listOf(Color(0xFFFFFDE7), Color(0xFFFF6F00), Color(0xFFB71C1C)),
        spark = Color(0xFFFFEA00),
        stars = 5,
    )
    is SupporterBadge.Custom -> AnimeCrestTheme(
        kanji = "印",
        rankEn = "CREST",
        aura = listOf(Color(0xFF90A4AE), Color(0xFFCFD8DC), Color(0xFF546E7A)),
        core = listOf(Color(0xFFECEFF1), Color(0xFF607D8B), Color(0xFF263238)),
        spark = Color(0xFFB0BEC5),
        stars = 1,
    )
}

@Composable
internal fun DonorMedalRow(
    badges: List<SupporterBadge>,
    labelColor: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    if (badges.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top,
    ) {
        badges.forEachIndexed { index, badge ->
            if (index > 0) Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
            AnimeRankCrest(
                badge = badge,
                labelColor = labelColor,
                sizeDp = if (compact) 58f else 72f,
            )
        }
    }
}

@Composable
private fun AnimeRankCrest(
    badge: SupporterBadge,
    labelColor: Color,
    sizeDp: Float,
) {
    val theme = remember(badge) { animeTheme(badge) }
    val emblemUrl = remember(badge) { animeEmblemUrl(badge) }
    val motion = rememberInfiniteTransition(label = "animeCrest-${badge.id}")
    val spin by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (badge) {
                    is SupporterBadge.Godlike -> 2800
                    is SupporterBadge.Legendary -> 3600
                    else -> 5200
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crestSpin",
    )
    val counterSpin by motion.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "crestCounter",
    )
    val breath by motion.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "crestBreath",
    )
    val sparkPhase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sparkPhase",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = (sizeDp + 20).dp),
    ) {
        Box(
            modifier = Modifier
                .size((sizeDp * 1.15f).dp)
                .graphicsLayer {
                    scaleX = breath
                    scaleY = breath
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f

                // Aura bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.spark.copy(alpha = 0.55f),
                            theme.aura.first().copy(alpha = 0.25f),
                            Color.Transparent,
                        ),
                        center = c,
                        radius = r,
                    ),
                    radius = r,
                    center = c,
                )

                // Outer rotating dashed energy ring
                drawCircle(
                    brush = Brush.sweepGradient(theme.aura + theme.aura.first(), c),
                    radius = r * 0.92f,
                    center = c,
                    style = Stroke(width = r * 0.045f),
                )
                // Spinning tick marks (anime UI hash marks)
                val ticks = 16
                for (i in 0 until ticks) {
                    val a = Math.toRadians((spin + i * (360.0 / ticks)))
                    val inner = r * 0.84f
                    val outer = r * 0.96f
                    drawLine(
                        color = theme.spark.copy(alpha = if (i % 2 == 0) 0.9f else 0.35f),
                        start = Offset(
                            c.x + cos(a).toFloat() * inner,
                            c.y + sin(a).toFloat() * inner,
                        ),
                        end = Offset(
                            c.x + cos(a).toFloat() * outer,
                            c.y + sin(a).toFloat() * outer,
                        ),
                        strokeWidth = r * 0.02f,
                    )
                }

                // Counter-rotating inner hex crest
                val hex = Path().apply {
                    val hr = r * 0.72f
                    for (i in 0..5) {
                        val a = Math.toRadians(counterSpin + 30.0 + i * 60.0)
                        val x = c.x + cos(a).toFloat() * hr
                        val y = c.y + sin(a).toFloat() * hr
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(
                    path = hex,
                    brush = Brush.linearGradient(theme.aura),
                    style = Stroke(width = r * 0.055f),
                )
                drawPath(
                    path = hex,
                    brush = Brush.radialGradient(
                        colors = theme.core,
                        center = Offset(c.x - r * 0.15f, c.y - r * 0.2f),
                        radius = r * 0.85f,
                    ),
                )

                // Diamond core plate
                val diamond = Path().apply {
                    val d = r * 0.48f
                    moveTo(c.x, c.y - d)
                    lineTo(c.x + d * 0.85f, c.y)
                    lineTo(c.x, c.y + d)
                    lineTo(c.x - d * 0.85f, c.y)
                    close()
                }
                drawPath(
                    path = diamond,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.25f)),
                    ),
                )
                drawPath(
                    path = diamond,
                    color = theme.spark.copy(alpha = 0.85f),
                    style = Stroke(width = r * 0.03f),
                )

                // Orbiting sparkles (anime glitter)
                val sparkles = 6
                for (i in 0 until sparkles) {
                    val orbit = r * (0.78f + (i % 2) * 0.08f)
                    val a = Math.toRadians(
                        spin * (if (i % 2 == 0) 1.0 else -1.2) + i * (360.0 / sparkles) +
                            sparkPhase * 40.0,
                    )
                    val sx = c.x + cos(a).toFloat() * orbit
                    val sy = c.y + sin(a).toFloat() * orbit
                    val sr = r * (0.04f + (i % 3) * 0.012f)
                    // 4-point star
                    val star = Path().apply {
                        moveTo(sx, sy - sr * 1.6f)
                        lineTo(sx + sr * 0.35f, sy - sr * 0.35f)
                        lineTo(sx + sr * 1.6f, sy)
                        lineTo(sx + sr * 0.35f, sy + sr * 0.35f)
                        lineTo(sx, sy + sr * 1.6f)
                        lineTo(sx - sr * 0.35f, sy + sr * 0.35f)
                        lineTo(sx - sr * 1.6f, sy)
                        lineTo(sx - sr * 0.35f, sy - sr * 0.35f)
                        close()
                    }
                    drawPath(star, color = theme.spark.copy(alpha = 0.95f))
                    drawCircle(Color.White.copy(alpha = 0.8f), radius = sr * 0.35f, center = Offset(sx, sy))
                }

                // Speed-line burst for divine / dragon ranks
                if (badge is SupporterBadge.Godlike || badge is SupporterBadge.Legendary) {
                    for (i in 0 until 10) {
                        val a = Math.toRadians(i * 36.0 + sparkPhase * 20.0)
                        drawLine(
                            color = theme.spark.copy(alpha = 0.22f),
                            start = Offset(
                                c.x + cos(a).toFloat() * r * 0.55f,
                                c.y + sin(a).toFloat() * r * 0.55f,
                            ),
                            end = Offset(
                                c.x + cos(a).toFloat() * r * 1.05f,
                                c.y + sin(a).toFloat() * r * 1.05f,
                            ),
                            strokeWidth = r * 0.018f,
                        )
                    }
                }
            }

            // Emblem + kanji stack
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = emblemUrl,
                    contentDescription = badge.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size((sizeDp * 0.36f).dp),
                )
            }

            // Kanji watermark top-right of crest
            Text(
                text = theme.kanji,
                color = theme.spark.copy(alpha = 0.9f),
                fontWeight = FontWeight.Black,
                fontSize = (sizeDp * 0.16f).sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 4.dp)
                    .shadow(2.dp),
            )
        }

        // Rank chip
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .clip(appShape(6.dp))
                .background(Brush.horizontalGradient(theme.aura))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = theme.rankEn,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
            )
        }

        // Star rarity
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            repeat(theme.stars) {
                Text("★", color = theme.spark, fontSize = 9.sp)
            }
        }

        Text(
            text = badge.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = labelColor.copy(alpha = 0.92f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 1.dp)
                .widthIn(max = (sizeDp + 12).dp),
            fontSize = 10.sp,
            lineHeight = 11.sp,
        )
    }
}

@Composable
private fun DonorProfileDestinationPanel(
    url: String,
    name: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val kind = remember(url) { donorProfileKind(url) }
    val handle = remember(url, kind) { donorProfileHandle(url, kind) }
    val (title, subtitle, panelBrush, cta) = when (kind) {
        DonorProfileKind.GITHUB -> Quad(
            "GitHub",
            "Explore $name's repos and stars",
            Brush.verticalGradient(listOf(Color(0xFF24292F), Color(0xFF0D1117))),
            "Open on GitHub",
        )
        DonorProfileKind.INSTAGRAM -> Quad(
            "Instagram",
            "See what $name is sharing",
            Brush.verticalGradient(listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))),
            "Open on Instagram",
        )
        DonorProfileKind.WEBSITE -> Quad(
            "Personal site",
            "Visit $name's corner of the web",
            Brush.verticalGradient(listOf(Color(0xFF1B4332), Color(0xFF081C15))),
            "Open website",
        )
    }

    // Same footprint as the trading card: 28.dp gutters, 88% width, 2.5:3.5 ratio.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = appShape(18.dp),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth(0.88f)
                .aspectRatio(2.5f / 3.5f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(panelBrush)
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = when (kind) {
                            DonorProfileKind.WEBSITE -> Icons.Filled.Language
                            else -> Icons.AutoMirrored.Filled.OpenInNew
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        shape = appShape(14.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val uri = runCatching { Uri.parse(url) }.getOrNull()
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
                            },
                    ) {
                        Text(
                            text = cta,
                            modifier = Modifier
                                .padding(vertical = 14.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Back to card", color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
