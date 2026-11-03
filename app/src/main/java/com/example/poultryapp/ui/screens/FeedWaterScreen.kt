package com.example.poultryapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.data.local.FeedWaterCharge
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.engine.*
import com.example.poultryapp.theme.*
import com.example.poultryapp.ui.components.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
fun FeedWaterScreen(
    flocks: List<Flock>,
    selectedFlockId: Long?,
    onSelectFlock: (Long) -> Unit,
    latestFeedCharge: FeedWaterCharge?,
    latestWaterCharge: FeedWaterCharge?,
    todayFeedTotal: Double,
    todayWaterTotal: Double,
    chargeHistory: List<FeedWaterCharge>,
    onChargeSubmit: (flockId: Long, itemType: ChargeItemType, amount: Double, notes: String?) -> Unit,
    onDeleteCharge: ((chargeId: Long) -> Unit)? = null,
    onToggleFeedTracking: ((flockId: Long, isEnabled: Boolean) -> Unit)? = null,
    onToggleWaterTracking: ((flockId: Long, isEnabled: Boolean) -> Unit)? = null,
    onNavigateToFlocks: () -> Unit,
    customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList(),
    settings: com.example.poultryapp.data.local.AppSettings = com.example.poultryapp.data.local.AppSettings(),
    weatherResult: com.example.poultryapp.service.WeatherResult? = null,
    isWeatherLoading: Boolean = false,
    onRefreshWeather: (() -> Unit)? = null,
    onSetManualTemp: ((Double?) -> Unit)? = null,
    onUpdateFlockCustomTemp: ((flockId: Long, tempCelsius: Double?) -> Unit)? = null,
    onSelectCity: ((province: String, city: String) -> Unit)? = null,
    onGpsDetect: (() -> Unit)? = null,
    onOpenDrawer: (() -> Unit)? = null,
    showTopBar: Boolean = true
) {
    var chargingType by remember { mutableStateOf<ChargeItemType?>(null) }
    var emptyingType by remember { mutableStateOf<ChargeItemType?>(null) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showFlockTempDialog by remember { mutableStateOf(false) }
    var deletingChargeId by remember { mutableStateOf<Long?>(null) }

    // Lifecycle-aware live clock ticker: updates every 15s when active on screen, zero CPU usage when backgrounded
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            currentTimeMillis = System.currentTimeMillis()
            while (true) {
                delay(15000L)
                currentTimeMillis = System.currentTimeMillis()
            }
        }
    }

    val selectedFlock = remember(flocks, selectedFlockId) {
        flocks.find { it.id == selectedFlockId } ?: flocks.firstOrNull()
    }

    // Resolve ambient temperature based on selected flock's custom temp, manual override, online weather, or city seasonal average
    val (effectiveTempCelsius, tempSourceFa) = remember(selectedFlock, weatherResult, settings.farmCity, settings.cachedTempCelsius, settings.manualTempCelsius) {
        if (selectedFlock?.customTempCelsius != null) {
            Pair(selectedFlock.customTempCelsius, "دمای اختصاصی سالن (${selectedFlock.name})")
        } else if (settings.manualTempCelsius != null) {
            Pair(settings.manualTempCelsius, "تنظیم دستی فارم")
        } else if (weatherResult != null) {
            Pair(weatherResult.temperatureCelsius, weatherResult.sourceDescription)
        } else if (settings.cachedTempCelsius != null) {
            Pair(settings.cachedTempCelsius, "آنلاین ذخیره‌شده")
        } else if (settings.farmCity.isNotBlank()) {
            val city = com.example.poultryapp.data.model.IranCitiesCatalog.findCityByName(settings.farmCity)
            if (city != null) {
                val currentSeason = JalaliDateHelper.getCurrentSeason()
                Pair(city.getTempForSeason(currentSeason), "میانگین فصل ${currentSeason.titleFa}")
            } else {
                Pair(22.0, "پیش‌فرض")
            }
        } else {
            Pair(null, "شهر ثبت نشده")
        }
    }

    val feedWaterIconPainter = painterResource(id = com.example.poultryapp.R.drawable.ic_farm_feed_water)
    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.US) }

    Scaffold(
        topBar = {
            if (showTopBar) {
                AppTopBar(
                    title = "پایش دان و آب زنده",
                    subtitle = selectedFlock?.let { "گله: ${it.name} (${it.species.titleFa} - ${toPersianDigits(it.effectiveBirdCount)} قطعه)" } ?: "انتخاب گله",
                    logoRes = com.example.poultryapp.R.drawable.ic_farm_feed_water,
                    navigationIcon = onOpenDrawer?.let {
                        {
                            IconButton(onClick = it) {
                                Icon(Icons.Default.Menu, contentDescription = "منو")
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showTopBar) padding else PaddingValues(0.dp))
        ) {
            if (flocks.isEmpty()) {
                EmptyStateView(
                    imageRes = com.example.poultryapp.R.drawable.ic_farm_feed_water,
                    title = "گله‌ای برای شارژ وجود ندارد",
                    message = "ابتدا از بخش گله‌ها یک گله ثبت کنید تا نیازهای دان و آب روزانه آن محاسبه و پایش زنده آغاز شود.",
                    actionButtonText = "+ ثبت گله جدید",
                    onActionClick = onNavigateToFlocks
                )
            } else if (selectedFlock != null) {
                val currentSeason = remember(currentTimeMillis / (1000 * 60 * 60)) { DailyNutritionEngine.getCurrentSeason() }
                val dailyReq = remember(selectedFlock, currentSeason, customBreeds, effectiveTempCelsius) {
                    DailyNutritionEngine.calculateFlockRequirement(
                        flock = selectedFlock,
                        season = currentSeason,
                        customBreeds = customBreeds,
                        tempCelsius = effectiveTempCelsius
                    )
                }

                // Ensure charges belong strictly to the currently selected flock to prevent race condition flashes
                val currentFlockFeedCharge = if (latestFeedCharge?.flockId == selectedFlock.id) latestFeedCharge else null
                val currentFlockWaterCharge = if (latestWaterCharge?.flockId == selectedFlock.id) latestWaterCharge else null

                // Calculate Live Status for Feed (memoized)
                val feedChargedAmount = if (selectedFlock.isFeedEnabled) currentFlockFeedCharge?.chargedAmount ?: 0.0 else 0.0
                val feedChargeTimestamp = if (selectedFlock.isFeedEnabled) currentFlockFeedCharge?.chargeTimestampMillis ?: 0L else 0L
                val feedDailyReq = dailyReq.totalFeedGrams

                val feedStatus = remember(feedChargedAmount, feedChargeTimestamp, feedDailyReq, currentTimeMillis) {
                    LiveConsumptionTracker.calculateLiveStatus(
                        itemType = ChargeItemType.FEED,
                        chargedAmount = feedChargedAmount,
                        chargeTimestampMillis = feedChargeTimestamp,
                        dailyRequirementTotal = feedDailyReq,
                        currentTimestampMillis = currentTimeMillis
                    )
                }

                // Calculate Live Status for Water (memoized)
                val waterChargedAmount = if (selectedFlock.isWaterEnabled) currentFlockWaterCharge?.chargedAmount ?: 0.0 else 0.0
                val waterChargeTimestamp = if (selectedFlock.isWaterEnabled) currentFlockWaterCharge?.chargeTimestampMillis ?: 0L else 0L
                val waterDailyReq = dailyReq.totalWaterMl

                val waterStatus = remember(waterChargedAmount, waterChargeTimestamp, waterDailyReq, currentTimeMillis) {
                    LiveConsumptionTracker.calculateLiveStatus(
                        itemType = ChargeItemType.WATER,
                        chargedAmount = waterChargedAmount,
                        chargeTimestampMillis = waterChargeTimestamp,
                        dailyRequirementTotal = waterDailyReq,
                        currentTimestampMillis = currentTimeMillis
                    )
                }

                // Scientific survival state when water or feed is finished
                // Note: Only calculate when actual charge data for this flock exists and is depleted, preventing flash on switch
                val survivalState = remember(selectedFlock, waterStatus, feedStatus, effectiveTempCelsius, tempSourceFa, currentTimeMillis, currentFlockWaterCharge, currentFlockFeedCharge) {
                    val isWaterDepleted = selectedFlock.isWaterEnabled && waterStatus.isFinished && currentFlockWaterCharge != null
                    val isFeedDepleted = selectedFlock.isFeedEnabled && feedStatus.isFinished && currentFlockFeedCharge != null

                    if (effectiveTempCelsius != null && (isWaterDepleted || isFeedDepleted)) {
                        SurvivalCalculationEngine.calculateSurvivalState(
                            flock = selectedFlock,
                            isWaterFinished = isWaterDepleted,
                            waterEmptyEpochMillis = waterStatus.emptyTimestampMillis,
                            isFeedFinished = isFeedDepleted,
                            feedEmptyEpochMillis = feedStatus.emptyTimestampMillis,
                            tempCelsius = effectiveTempCelsius,
                            tempSource = tempSourceFa,
                            currentMillis = currentTimeMillis
                        )
                    } else null
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = FarmDimens.Space16,
                        end = FarmDimens.Space16,
                        top = FarmDimens.Space8,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(FarmDimens.Space10)
                ) {
                    // Alert banner if city is not registered AND no manual temperature is set
                    if (settings.farmCity.isBlank() && settings.manualTempCelsius == null) {
                        item(key = "missing_city_banner") {
                            MissingCityAlertBanner(
                                onSelectCityClick = { showCityDialog = true }
                            )
                        }
                    }

                    if (effectiveTempCelsius != null) {
                        item(key = "ambient_temp_chip") {
                            AmbientWeatherBar(
                                cityName = if (selectedFlock.customTempCelsius != null) "سالن: ${selectedFlock.name}" else if (settings.farmCity.isNotBlank()) settings.farmCity else "سالن فارم",
                                temperatureCelsius = effectiveTempCelsius,
                                isOnline = selectedFlock.customTempCelsius == null && weatherResult?.isOnline == true && settings.manualTempCelsius == null,
                                isManual = selectedFlock.customTempCelsius != null,
                                sourceDescription = tempSourceFa,
                                onRefreshClick = onRefreshWeather,
                                onManualClick = { showFlockTempDialog = true },
                                onCityClick = { showCityDialog = true },
                                isLoading = isWeatherLoading
                            )
                        }
                    } else {
                        item(key = "ambient_temp_chip_fallback") {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Thermostat,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "دمای سالن: نامشخص",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "برای دقت محاسبات، شهر یا دمای سالن را تنظیم کنید",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { showFlockTempDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("تنظیم دمای سالن", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Red Survival Countdown Card (active when water or feed is depleted)
                    if (survivalState != null) {
                        item(key = "survival_timer_card") {
                            SurvivalTimerCard(
                                survivalState = survivalState,
                                onEmergencyChargeClick = { type -> chargingType = type }
                            )
                        }
                    }
                    // Flocks horizontal selector (if multiple flocks)
                    if (flocks.size > 1) {
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                            ) {
                                items(flocks, key = { it.id }) { flock ->
                                    val isSelected = flock.id == selectedFlock.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSelectFlock(flock.id) },
                                        label = {
                                            Text(
                                                text = "${flock.name} (${toPersianDigits(flock.effectiveBirdCount)})",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        shape = RoundedCornerShape(FarmDimens.ChipRadius),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Daily Requirement Card Header (Structured 2-column grid layout)
                    item {
                        Surface(
                            shape = RoundedCornerShape(FarmDimens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                                AutoResizeText(
                                    text = "نیاز استاندارد روزانه (فصل ${dailyReq.season.titleFa})",
                                    targetFontSize = 15.sp,
                                    minFontSize = 11.sp,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "سن گله: ${selectedFlock.ageDisplayFa}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ظرفیت: ${toPersianDigits(selectedFlock.effectiveBirdCount)} قطعه",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(FarmDimens.Space10))

                                // Side-by-side metric cards (Feed & Water)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space10)
                                ) {
                                    // Feed Requirement
                                    Surface(
                                        shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                        color = FeedAmberContainer.copy(alpha = 0.4f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(FarmDimens.Space10)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Grain,
                                                    contentDescription = null,
                                                    tint = FeedAmber,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(FarmDimens.Space4))
                                                Text(
                                                    text = "دان مورد نیاز گله",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = FeedAmberText
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(FarmDimens.Space4))
                                            Text(
                                                text = dailyReq.formattedTotalFeed,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "هر پرنده: ${dailyReq.formattedFeedPerBird}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Water Requirement
                                    Surface(
                                        shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                        color = WaterBlueContainer.copy(alpha = 0.4f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(FarmDimens.Space10)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.WaterDrop,
                                                    contentDescription = null,
                                                    tint = WaterBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(FarmDimens.Space4))
                                                Text(
                                                    text = "آب مورد نیاز گله",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WaterBlueText
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(FarmDimens.Space4))
                                            Text(
                                                text = dailyReq.formattedTotalWater,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "هر پرنده: ${dailyReq.formattedWaterPerBird}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(FarmDimens.Space10))

                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                Spacer(modifier = Modifier.height(FarmDimens.Space6))

                                // Registered total today summary
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مجموع شارژ امروز:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(FarmDimens.BadgeRadius),
                                            color = FeedAmberContainer.copy(alpha = 0.8f)
                                        ) {
                                            Text(
                                                text = "${DailyNutritionEngine.formatFeed(todayFeedTotal)} دان",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = FeedAmberText,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(FarmDimens.BadgeRadius),
                                            color = WaterBlueContainer.copy(alpha = 0.8f)
                                        ) {
                                            Text(
                                                text = "${DailyNutritionEngine.formatWater(todayWaterTotal)} آب",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = WaterBlueText,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Feed Live Tracker Card
                    item {
                        LiveChargeCard(
                            itemType = ChargeItemType.FEED,
                            status = feedStatus,
                            flockName = selectedFlock.name,
                            isEnabled = selectedFlock.isFeedEnabled,
                            onToggleEnabled = onToggleFeedTracking?.let { toggle ->
                                { isChecked -> toggle(selectedFlock.id, isChecked) }
                            },
                            onChargeClick = { chargingType = ChargeItemType.FEED },
                            onEmptyClick = { emptyingType = ChargeItemType.FEED }
                        )
                    }

                    // Water Live Tracker Card
                    item {
                        LiveChargeCard(
                            itemType = ChargeItemType.WATER,
                            status = waterStatus,
                            flockName = selectedFlock.name,
                            isEnabled = selectedFlock.isWaterEnabled,
                            onToggleEnabled = onToggleWaterTracking?.let { toggle ->
                                { isChecked -> toggle(selectedFlock.id, isChecked) }
                            },
                            onChargeClick = { chargingType = ChargeItemType.WATER },
                            onEmptyClick = { emptyingType = ChargeItemType.WATER }
                        )
                    }

                    // Charge History Timeline
                    item {
                        SectionHeader(
                            title = "تاریخچه شارژهای اخیر",
                            subtitle = "ثبت مراجعات و مقادیر شارژشده",
                            iconRes = com.example.poultryapp.R.drawable.ic_farm_feed_water
                        )
                    }

                    if (chargeHistory.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "هنوز شارژی برای این گله ثبت نشده است. از دکمه‌های بالا برای شارژ اولیه استفاده کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(FarmDimens.Space14)
                                )
                            }
                        }
                    } else {
                        items(
                            items = chargeHistory.take(15),
                            key = { it.id },
                            contentType = { it.itemType.name }
                        ) { charge ->
                            val isFeed = charge.itemType == ChargeItemType.FEED
                            val isDiscard = charge.chargedAmount <= 0.0
                            val accentColor = if (isFeed) FeedAmber else WaterBlue

                            Surface(
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isDiscard) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = FarmDimens.Space12, vertical = FarmDimens.Space10),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f, fill = false),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isDiscard) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else accentColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, if (isDiscard) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            if (isDiscard) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            } else {
                                                Image(
                                                    painter = feedWaterIconPainter,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(FarmDimens.Space12))
                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            AutoResizeText(
                                                text = if (isDiscard) "تخلیه و دور ریختن ${if (isFeed) "دان" else "آب"}"
                                                       else if (isFeed) "شارژ دان" else "شارژ آب",
                                                targetFontSize = 14.sp,
                                                minFontSize = 10.sp,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDiscard) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            val timeStr = timeFormatter.format(java.util.Date(charge.chargeTimestampMillis))
                                            val subtitle = if (!charge.notes.isNullOrBlank()) "${charge.dateJalaliStr} | ساعت ${toPersianDigits(timeStr)} (${charge.notes})"
                                                           else "${charge.dateJalaliStr} | ساعت ${toPersianDigits(timeStr)}"
                                            AutoResizeText(
                                                text = subtitle,
                                                targetFontSize = 12.sp,
                                                minFontSize = 8.5.sp,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isDiscard) {
                                            Surface(
                                                shape = RoundedCornerShape(FarmDimens.BadgeRadius),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                            ) {
                                                Text(
                                                    text = "تخلیه کامل",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        } else {
                                            PersianValueUnit(
                                                value = if (isFeed) {
                                                    val kg = charge.chargedAmount / 1000.0
                                                    if (kg >= 1.0) String.format(java.util.Locale.US, "%.2f", kg) else charge.chargedAmount.toInt()
                                                } else {
                                                    val lit = charge.chargedAmount / 1000.0
                                                    if (lit >= 1.0) String.format(java.util.Locale.US, "%.2f", lit) else charge.chargedAmount.toInt()
                                                },
                                                unit = if (isFeed) {
                                                    if (charge.chargedAmount >= 1000.0) "کیلوگرم" else "گرم"
                                                } else {
                                                    if (charge.chargedAmount >= 1000.0) "لیتر" else "میلی‌لیتر"
                                                },
                                                valueStyle = MaterialTheme.typography.titleSmall,
                                                valueColor = accentColor
                                            )
                                        }

                                        if (onDeleteCharge != null) {
                                            Spacer(modifier = Modifier.width(FarmDimens.Space4))
                                            IconButton(
                                                onClick = { deletingChargeId = charge.id },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف شارژ",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom safe spacer
                    item {
                        Spacer(modifier = Modifier.height(FarmDimens.Space32))
                    }
                }
            }
        }
    }

    // Charge Dialog
    if (chargingType != null && selectedFlock != null) {
        val dailyReq = DailyNutritionEngine.calculateFlockRequirement(
            flock = selectedFlock,
            customBreeds = customBreeds,
            tempCelsius = effectiveTempCelsius
        )
        val isFeed = chargingType == ChargeItemType.FEED
        val latest = if (isFeed) {
            if (latestFeedCharge?.flockId == selectedFlock.id) latestFeedCharge else null
        } else {
            if (latestWaterCharge?.flockId == selectedFlock.id) latestWaterCharge else null
        }
        val reqTotal = if (isFeed) dailyReq.totalFeedGrams else dailyReq.totalWaterMl
        val curStatus = LiveConsumptionTracker.calculateLiveStatus(
            itemType = chargingType!!,
            chargedAmount = latest?.chargedAmount ?: 0.0,
            chargeTimestampMillis = latest?.chargeTimestampMillis ?: 0L,
            dailyRequirementTotal = reqTotal,
            currentTimestampMillis = currentTimeMillis
        )
        ChargeDialog(
            itemType = chargingType!!,
            flockName = selectedFlock.name,
            dailyRequirementTotal = reqTotal,
            currentRemainingAmount = curStatus.remainingAmount,
            onDismiss = { chargingType = null },
            onSubmit = { amount, discardLeftover ->
                val note = if (discardLeftover) "شارژ تمیز پس از دور ریختن باقیمانده" else null
                onChargeSubmit(selectedFlock.id, chargingType!!, amount, note)
                chargingType = null
            },
            onDiscardLeftoverOnly = {
                onChargeSubmit(selectedFlock.id, chargingType!!, 0.0, "تخلیه و دور ریختن باقیمانده")
                chargingType = null
            }
        )
    }

    // Empty / Discard Leftover Confirmation Dialog
    if (emptyingType != null && selectedFlock != null) {
        val dailyReq = DailyNutritionEngine.calculateFlockRequirement(
            flock = selectedFlock,
            customBreeds = customBreeds,
            tempCelsius = effectiveTempCelsius
        )
        val isFeed = emptyingType == ChargeItemType.FEED
        val latest = if (isFeed) {
            if (latestFeedCharge?.flockId == selectedFlock.id) latestFeedCharge else null
        } else {
            if (latestWaterCharge?.flockId == selectedFlock.id) latestWaterCharge else null
        }
        val reqTotal = if (isFeed) dailyReq.totalFeedGrams else dailyReq.totalWaterMl
        val curStatus = LiveConsumptionTracker.calculateLiveStatus(
            itemType = emptyingType!!,
            chargedAmount = latest?.chargedAmount ?: 0.0,
            chargeTimestampMillis = latest?.chargeTimestampMillis ?: 0L,
            dailyRequirementTotal = reqTotal,
            currentTimestampMillis = currentTimeMillis
        )
        EmptyConfirmDialog(
            itemType = emptyingType!!,
            flockName = selectedFlock.name,
            remainingAmountFa = curStatus.remainingAmountFa,
            onDismiss = { emptyingType = null },
            onConfirm = {
                onChargeSubmit(selectedFlock.id, emptyingType!!, 0.0, "تخلیه و دور ریختن باقیمانده")
                emptyingType = null
            }
        )
    }

    // City Selection Modal Dialog
    if (showCityDialog) {
        CitySelectionDialog(
            initialProvince = settings.farmProvince,
            initialCity = settings.farmCity,
            onDismiss = { showCityDialog = false },
            onCitySelected = { prov, city ->
                onSelectCity?.invoke(prov, city)
                showCityDialog = false
            },
            onGpsDetectClick = {
                onGpsDetect?.invoke()
                showCityDialog = false
            }
        )
    }

    // Delete Charge Confirmation Dialog
    if (deletingChargeId != null) {
        AlertDialog(
            onDismissRequest = { deletingChargeId = null },
            title = { Text("حذف رکورد شارژ") },
            text = { Text("آیا از حذف این شارژ اطمینان دارید؟ با حذف این مورد، وضعیت زنده و مصرف امروز تصحیح می‌شود.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cid = deletingChargeId ?: return@TextButton
                        onDeleteCharge?.invoke(cid)
                        deletingChargeId = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingChargeId = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Flock Custom Temperature Dialog (Directly sets temperature for the selected flock)
    if (showFlockTempDialog && selectedFlock != null) {
        val farmAmbientTemp = settings.manualTempCelsius ?: weatherResult?.temperatureCelsius ?: settings.cachedTempCelsius
        FlockTemperatureDialog(
            flockName = selectedFlock.name,
            currentCustomTemp = selectedFlock.customTempCelsius,
            farmAmbientTemp = farmAmbientTemp,
            onDismiss = { showFlockTempDialog = false },
            onConfirm = { newTemp ->
                onUpdateFlockCustomTemp?.invoke(selectedFlock.id, newTemp)
                showFlockTempDialog = false
            }
        )
    }
}
