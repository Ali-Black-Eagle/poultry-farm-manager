package com.example.poultryapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.poultryapp.data.model.IranCitiesCatalog
import com.example.poultryapp.data.model.IranCity
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.SurvivalCountdownState
import com.example.poultryapp.engine.SurvivalRiskLevel
import com.example.poultryapp.engine.SurvivalThreatType
import com.example.poultryapp.engine.toPersianDigits
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max
import com.example.poultryapp.theme.FarmDimens
import com.example.poultryapp.theme.FarmGreenPrimary

// Distinct Alert Red colors
private val DangerRed = Color(0xFFDC2626)
private val DangerRedDark = Color(0xFF991B1B)
private val DangerRedContainerLight = Color(0xFFFEE2E2)
private val DangerRedContainerDark = Color(0xFF450A0A)
private val WarningAmber = Color(0xFFD97706)
private val WarningAmberContainer = Color(0xFFFEF3C7)

/**
 * Striking, scientific Red Survival Countdown Card ("تایمر مرگ تا اولین تلفات").
 * Rendered when feed or water has completely run out.
 */
@Composable
fun SurvivalTimerCard(
    survivalState: SurvivalCountdownState,
    onEmergencyChargeClick: (ChargeItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.run {
        (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5
    }

    val cardBg = if (isDark) DangerRedContainerDark else DangerRedContainerLight
    val textPrimary = if (isDark) Color(0xFFFEE2E2) else Color(0xFF7F1D1D)
    val textSecondary = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)

    // Pulsing border animation for critical survival alert
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Dynamic, live 1-second ticker that syncs with lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(survivalState.deadlineEpochMillis, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                currentMillis = System.currentTimeMillis()
                val remaining = survivalState.deadlineEpochMillis - currentMillis
                if (remaining <= 0) break
                val delayMs = 1000L - (System.currentTimeMillis() % 1000L)
                delay(if (delayMs > 0) delayMs else 1000L)
            }
        }
    }

    val liveRemainingMillis = max(0L, survivalState.deadlineEpochMillis - currentMillis)
    val isLiveExpired = liveRemainingMillis <= 0

    val liveRiskLevel = when {
        isLiveExpired -> SurvivalRiskLevel.CRITICAL
        liveRemainingMillis < 3 * 3600 * 1000L -> SurvivalRiskLevel.CRITICAL
        liveRemainingMillis < 8 * 3600 * 1000L -> SurvivalRiskLevel.HIGH
        else -> survivalState.riskLevel
    }

    val totalSeconds = liveRemainingMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    Surface(
        shape = RoundedCornerShape(FarmDimens.CardRadius),
        color = cardBg,
        border = BorderStroke(2.dp, DangerRed.copy(alpha = borderAlpha)),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmDimens.Space16)
        ) {
            // Header Row: Warning Icon + Title + Risk Level Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DangerRed,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "هشدار بقا",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(FarmDimens.Space8))
                    Text(
                        text = "تایمر بقا (زمان تا اولین تلفات)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(FarmDimens.BadgeRadius),
                    color = if (isLiveExpired) Color.Black else DangerRedDark
                ) {
                    Text(
                        text = if (isLiveExpired) "مرگ گله آغاز شده!" else liveRiskLevel.badgeFa,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmDimens.Space8))

            // Active Threat Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0x33FF5252) else Color(0x18DC2626),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (survivalState.threatType == SurvivalThreatType.FEED_DEPLETED) Icons.Default.Grain else Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = survivalState.threatType.titleFa,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = liveRiskLevel.titleFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmDimens.Space12))

            // Digital Countdown Display
            Surface(
                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                color = if (isDark) Color(0xFF1E0606) else Color.White,
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FarmDimens.Space12, vertical = FarmDimens.Space10),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLiveExpired) {
                        Text(
                            text = "۰۰ : ۰۰ : ۰۰",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            ),
                            color = DangerRed,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⚠️ زمان بقای علمی به پایان رسیده است!",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = DangerRedDark,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // 3 Segmented units: Hours, Minutes, Seconds
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // In RTL: Hours on the right, Minutes center, Seconds on the left
                            CountdownTimeUnit(
                                value = toPersianDigits("%02d".format(Locale.US, hours)),
                                unit = "ساعت",
                                isDark = isDark
                            )

                            Text(
                                text = ":",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 26.sp
                                ),
                                color = DangerRed,
                                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 14.dp)
                            )

                            CountdownTimeUnit(
                                value = toPersianDigits("%02d".format(Locale.US, minutes)),
                                unit = "دقیقه",
                                isDark = isDark
                            )

                            Text(
                                text = ":",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 26.sp
                                ),
                                color = DangerRed,
                                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 14.dp)
                            )

                            CountdownTimeUnit(
                                value = toPersianDigits("%02d".format(Locale.US, seconds)),
                                unit = "ثانیه",
                                isDark = isDark
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "ساعت : دقیقه : ثانیه تا شروع تلفات گله",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmDimens.Space10))

            // Scientific factors & temperature details
            Surface(
                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                color = DangerRed.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FarmDimens.Space10),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "دمای مبنای محاسبه:",
                            style = MaterialTheme.typography.bodySmall,
                            color = textPrimary
                        )
                        Text(
                            text = "${toPersianDigits("%.1f".format(Locale.US, survivalState.tempUsedCelsius))}°C (${survivalState.tempSourceFa})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            textAlign = TextAlign.End
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مقاومت بیولوژیک کل:",
                            style = MaterialTheme.typography.bodySmall,
                            color = textPrimary
                        )
                        Text(
                            text = "حدود ${toPersianDigits("%.1f".format(Locale.US, survivalState.totalSurvivalHours))} ساعت",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            textAlign = TextAlign.End
                        )
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = DangerRed.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Text(
                        text = survivalState.explanationFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = textPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmDimens.Space12))

            // Urgent Charge Action Button
            val chargeItemType = when (survivalState.threatType) {
                SurvivalThreatType.WATER_DEPLETED -> ChargeItemType.WATER
                SurvivalThreatType.FEED_DEPLETED -> ChargeItemType.FEED
                SurvivalThreatType.BOTH_DEPLETED -> ChargeItemType.WATER // Water is top priority
            }

            Button(
                onClick = { onEmergencyChargeClick(chargeItemType) },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (chargeItemType == ChargeItemType.WATER) Icons.Default.WaterDrop else Icons.Default.Grain,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(FarmDimens.Space8))
                Text(
                    text = if (chargeItemType == ChargeItemType.WATER) "💧 شارژ اضطراری و فوری آب" else "🌾 شارژ اضطراری دان",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CountdownTimeUnit(
    value: String,
    unit: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isDark) Color(0xFF2E0A0A) else Color(0xFFFFEEEE),
            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
            modifier = Modifier.width(68.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                ),
                color = DangerRed,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
        )
    }
}

/**
 * Warning banner displayed when no farm city has been selected yet.
 */
@Composable
fun MissingCityAlertBanner(
    onSelectCityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(FarmDimens.CardRadius),
        color = WarningAmberContainer,
        border = BorderStroke(1.5.dp, WarningAmber),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmDimens.Space14)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(FarmDimens.Space8))
                Text(
                    text = "شهر ثبت نشده جهت اندازه گیری دقیق تر مصرف آب شهر را مشخص کنید",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F)
                )
            }

            Spacer(modifier = Modifier.height(FarmDimens.Space6))

            Text(
                text = "برای محاسبه علمی اثر دمای محیط روی افزایش مصرف آب و تعیین دقیق زمان بقای طیور تا اولین تلفات، لطفاً شهر مزرعه خود را ثبت نمایید.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF92400E),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(FarmDimens.Space10))

            Button(
                onClick = onSelectCityClick,
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.AddLocationAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(FarmDimens.Space6))
                Text(
                    text = "ثبت شهر مزرعه",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Modern, responsive Ambient Weather Bar for live feed & water screen.
 * Displays city name, current effective temperature, weather source, and dedicated action buttons.
 * Guarantees zero vertical text compression and clean Persian RTL layout.
 */
@Composable
fun AmbientWeatherBar(
    cityName: String,
    temperatureCelsius: Double,
    isOnline: Boolean,
    isManual: Boolean,
    sourceDescription: String,
    onRefreshClick: (() -> Unit)? = null,
    onManualClick: () -> Unit,
    onCityClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isHot = temperatureCelsius >= 30.0
    val iconColor = if (isManual) MaterialTheme.colorScheme.tertiary else if (isHot) DangerRed else FarmGreenPrimary

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Leading Icon + Temperature Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                isManual -> Icons.Default.Tune
                                isOnline -> Icons.Default.CloudQueue
                                else -> Icons.Default.Thermostat
                            },
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    AutoResizeText(
                        text = "مزرعه: $cityName",
                        targetFontSize = 14.sp,
                        minFontSize = 10.sp,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🌡️ ${toPersianDigits(temperatureCelsius)}°C",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isHot) DangerRed else MaterialTheme.colorScheme.primary
                        )
                        AutoResizeText(
                            text = "($sourceDescription)",
                            targetFontSize = 11.sp,
                            minFontSize = 8.5.sp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Refresh button (if online)
                if (isOnline && onRefreshClick != null) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onRefreshClick,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "بروزرسانی وضعیت آب‌وهوا",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Manual temp button
                FilledTonalIconButton(
                    onClick = onManualClick,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isManual) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = "تنظیم دستی دما",
                        tint = if (isManual) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // City selection button
                FilledTonalIconButton(
                    onClick = onCityClick,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "تغییر شهر فارم",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Ambient Temperature Chip displayed in headers or screens.
 */
@Composable
fun AmbientTemperatureChip(
    cityName: String,
    temperatureCelsius: Double,
    isOnline: Boolean,
    sourceDescription: String,
    onRefreshClick: (() -> Unit)? = null,
    onManualClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(FarmDimens.ChipRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Default.CloudQueue else Icons.Default.Thermostat,
                contentDescription = null,
                tint = if (temperatureCelsius >= 30.0) DangerRed else FarmGreenPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(FarmDimens.Space6))
            Text(
                text = "دمای $cityName: ${toPersianDigits(temperatureCelsius)}°C ($sourceDescription)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (onManualClick != null) {
                Spacer(modifier = Modifier.width(FarmDimens.Space4))
                IconButton(
                    onClick = onManualClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "تنظیم دستی دما",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (onRefreshClick != null && isOnline) {
                Spacer(modifier = Modifier.width(FarmDimens.Space4))
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "بروزرسانی دما",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal dialog for manually entering ambient temperature or resetting to automatic weather.
 */
@Composable
fun ManualTemperatureDialog(
    currentManualTemp: Double?,
    cityTemp: Double?,
    cityName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    var tempInput by remember {
        mutableStateOf(currentManualTemp?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(FarmDimens.CardRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmDimens.Space16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FarmDimens.Space20)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(FarmDimens.Space8))
                        Text(
                            text = "تنظیم دستی دمای سالن",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space8))
                Text(
                    text = "می‌توانید دمای سالن را به صورت دستی بر حسب درجه سانتی‌گراد وارد کنید تا محاسبات آب، دان و بقا بر مبنای این دما تنظیم شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (cityTemp != null && cityName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(FarmDimens.Space8))
                    Text(
                        text = "دمای فعلی شهر $cityName: ${toPersianDigits(cityTemp)}°C",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space12))

                // Presets for quick selection
                Text(
                    text = "دماهای متداول مرغداری:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(FarmDimens.Space6))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        Pair("۳۳°C جوجه", "33"),
                        Pair("۲۸°C رشد", "28"),
                        Pair("۲۴°C استاندارد", "24"),
                        Pair("۲۰°C خنک", "20")
                    )
                    presets.forEach { (label, value) ->
                        FilterChip(
                            selected = tempInput == value,
                            onClick = { tempInput = value },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space12))

                OutlinedTextField(
                    value = tempInput,
                    onValueChange = { input ->
                        val cleaned = input.replace(',', '.')
                        if (cleaned.isEmpty() || cleaned == "-" || cleaned.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                            tempInput = cleaned
                        }
                    },
                    label = { Text("دمای دستی سالن (°C)") },
                    placeholder = { Text("مثال: ۲۸ یا ۳۲.۵") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(FarmDimens.Space16))

                // Action buttons: Two prominent buttons side-by-side + full-width auto button below
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(FarmDimens.RadiusMedium)
                        ) {
                            Text(
                                text = "انصراف",
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                val parsed = tempInput.toDoubleOrNull()
                                if (parsed != null) {
                                    onConfirm(parsed)
                                    onDismiss()
                                }
                            },
                            enabled = tempInput.toDoubleOrNull() != null,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(FarmDimens.RadiusMedium)
                        ) {
                            Text(
                                text = "ثبت دما",
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            onConfirm(null)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "بازگشت به حالت خودکار (دمای آنلاین)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal dialog for selecting an Iranian Province and City with search and GPS detection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionDialog(
    initialProvince: String = "",
    initialCity: String = "",
    onDismiss: () -> Unit,
    onCitySelected: (province: String, city: String) -> Unit,
    onGpsDetectClick: () -> Unit
) {
    val provinces = remember { IranCitiesCatalog.getAllProvinces() }
    var selectedProvince by remember {
        mutableStateOf(initialProvince.ifBlank { provinces.firstOrNull() ?: "تهران" })
    }

    var searchQuery by remember { mutableStateOf("") }

    val availableCities = remember(selectedProvince) {
        IranCitiesCatalog.getCitiesForProvince(selectedProvince)
    }

    val filteredCities = remember(availableCities, searchQuery) {
        if (searchQuery.isBlank()) {
            availableCities
        } else {
            val q = searchQuery.trim()
            val inProvince = availableCities.filter { it.name.contains(q, ignoreCase = true) }
            if (inProvince.isNotEmpty()) {
                inProvince
            } else {
                IranCitiesCatalog.searchCities(q)
            }
        }
    }

    var selectedCityName by remember(initialCity, availableCities) {
        mutableStateOf(initialCity.ifBlank { availableCities.firstOrNull()?.name ?: "" })
    }

    var provinceExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(FarmDimens.CardRadius),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FarmDimens.Space16)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "انتخاب استان و شهر مزرعه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Text(
                    text = "جهت استخراج داده‌های هواشناسی و دمای دقیق فصل برای پایش آب و بقای پرندگان",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(FarmDimens.Space12))

                // GPS Auto-detection button
                OutlinedButton(
                    onClick = onGpsDetectClick,
                    shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                    contentPadding = PaddingValues(horizontal = FarmDimens.Space12, vertical = FarmDimens.Space8),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(FarmDimens.Space8))
                    Text(
                        text = "📍 شناسایی خودکار شهر با GPS گوشی",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space12))

                // Province Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = provinceExpanded,
                    onExpandedChange = { provinceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProvince,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("استان") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(FarmDimens.RadiusMedium)
                    )

                    ExposedDropdownMenu(
                        expanded = provinceExpanded,
                        onDismissRequest = { provinceExpanded = false }
                    ) {
                        provinces.forEach { prov ->
                            DropdownMenuItem(
                                text = { Text(prov) },
                                onClick = {
                                    selectedProvince = prov
                                    provinceExpanded = false
                                    selectedCityName = IranCitiesCatalog.getCitiesForProvince(prov).firstOrNull()?.name ?: ""
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space8))

                // City Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجوی نام شهر (مثلاً بیضا)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(FarmDimens.Space8))

                // Cities List Header
                val isGlobalSearch = searchQuery.isNotBlank() && filteredCities.any { it.province != selectedProvince }
                Text(
                    text = if (isGlobalSearch) "نتایج جستجو در سراسر کشور (${toPersianDigits(filteredCities.size)} شهر):"
                           else "شهرهای $selectedProvince (${toPersianDigits(filteredCities.size)} شهر):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(FarmDimens.Space4))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCities, key = { "${it.province}_${it.name}" }) { city ->
                        val isSelected = city.name == selectedCityName && (city.province == selectedProvince || !isGlobalSearch)
                        Surface(
                            shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCityName = city.name
                                    selectedProvince = city.province
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = FarmDimens.Space12, vertical = FarmDimens.Space10),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCityName = city.name
                                        selectedProvince = city.province
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(FarmDimens.Space10))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6)
                                    ) {
                                        AutoResizeText(
                                            text = city.name,
                                            targetFontSize = 14.sp,
                                            minFontSize = 10.sp,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        if (city.province != selectedProvince) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = city.province,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                                    ) {
                                        Text(
                                            text = "☀️ تابستان: ${toPersianDigits(city.summerTemp)}°C",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                        Text(
                                            text = "❄️ زمستان: ${toPersianDigits(city.winterTemp)}°C",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space12))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.9f),
                        contentPadding = PaddingValues(horizontal = FarmDimens.Space8, vertical = FarmDimens.Space10),
                        shape = RoundedCornerShape(FarmDimens.RadiusMedium)
                    ) {
                        Text("انصراف", maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = {
                            if (selectedCityName.isNotBlank()) {
                                onCitySelected(selectedProvince, selectedCityName)
                                onDismiss()
                            }
                        },
                        enabled = selectedCityName.isNotBlank(),
                        modifier = Modifier.weight(1.3f),
                        contentPadding = PaddingValues(horizontal = FarmDimens.Space8, vertical = FarmDimens.Space10),
                        shape = RoundedCornerShape(FarmDimens.RadiusMedium)
                    ) {
                        Text("تأیید و ثبت شهر", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
