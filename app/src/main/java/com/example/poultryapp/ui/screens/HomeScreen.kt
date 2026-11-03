package com.example.poultryapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.ui.components.AutoResizeText
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poultryapp.data.local.*
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.data.model.BreedCatalog
import com.example.poultryapp.data.model.ProductionType
import com.example.poultryapp.engine.*
import com.example.poultryapp.service.WeatherAlertCategory
import com.example.poultryapp.service.WeatherConditionType
import com.example.poultryapp.service.WeatherForecastDay
import com.example.poultryapp.service.WeatherResult
import com.example.poultryapp.service.WeatherThemePalette
import com.example.poultryapp.theme.*
import com.example.poultryapp.ui.components.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun HomeScreen(
    flocks: List<Flock>,
    warehouseItems: List<WarehouseItem>,
    schedules: List<CalendarSchedule>,
    chargesHistory: List<FeedWaterCharge>,
    customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList(),
    farmAmbientTemp: Double? = null,
    weatherResult: com.example.poultryapp.service.WeatherResult? = null,
    isWeatherLoading: Boolean = false,
    onRefreshWeather: (() -> Unit)? = null,
    eggProductions: List<EggProduction> = emptyList(),
    onLogEggProduction: ((flockIds: List<Long>, healthy: Int, broken: Int, dirty: Int, doubleYolk: Int, dateJalaliStr: String, notes: String?) -> Unit)? = null,
    onOpenDrawer: () -> Unit,
    onNavigateToFeedWater: (flockId: Long?) -> Unit,
    onNavigateToWarehouse: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFlocks: () -> Unit = {},
    onNavigateToManagement: (() -> Unit)? = null,
    onNavigateToGoldenBreeding: (() -> Unit)? = null,
    onOpenVoiceAssistant: () -> Unit = {}
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showRecordEggDialog by remember { mutableStateOf(false) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            currentTimeMillis = System.currentTimeMillis()
            while (true) {
                delay(20000L)
                currentTimeMillis = System.currentTimeMillis()
            }
        }
    }

    val jalaliNow = remember(currentTimeMillis / (1000 * 60 * 60)) { JalaliDateHelper.getJalaliDate() }
    val todayDayOfWeek = jalaliNow.dayOfWeek
    val tomorrowDayOfWeek = (todayDayOfWeek + 1) % 7
    val currentSeason = remember(jalaliNow) { JalaliDateHelper.getCurrentSeason() }

    // Filter schedules for today & tomorrow (memoized)
    val todaySchedules = remember(schedules, todayDayOfWeek, jalaliNow.day) {
        schedules.filter { s ->
            s.isEnabled && (
                    (s.scheduleMode == "WEEKLY" && s.daysOfWeek.contains(todayDayOfWeek)) ||
                            (s.scheduleMode == "MONTHLY" && s.dayOfMonth == jalaliNow.day)
                    )
        }
    }

    val tomorrowSchedules = remember(schedules, tomorrowDayOfWeek, jalaliNow.day) {
        schedules.filter { s ->
            s.isEnabled && (
                    (s.scheduleMode == "WEEKLY" && s.daysOfWeek.contains(tomorrowDayOfWeek)) ||
                            (s.scheduleMode == "MONTHLY" && s.dayOfMonth == jalaliNow.day + 1)
                    )
        }
    }

    // Today's charges calculations (memoized)
    val todayJalaliStr = jalaliNow.toStandardString()
    val todayFeedCharges = remember(chargesHistory, todayJalaliStr) {
        chargesHistory.filter { it.dateJalaliStr == todayJalaliStr && it.itemType == ChargeItemType.FEED }
    }
    val todayWaterCharges = remember(chargesHistory, todayJalaliStr) {
        chargesHistory.filter { it.dateJalaliStr == todayJalaliStr && it.itemType == ChargeItemType.WATER }
    }
    val totalFarmFeedTodayGrams = remember(todayFeedCharges) { todayFeedCharges.sumOf { it.chargedAmount } }
    val totalFarmWaterTodayMl = remember(todayWaterCharges) { todayWaterCharges.sumOf { it.chargedAmount } }

    val totalTodayFeedCost = remember(todayFeedCharges, warehouseItems) {
        todayFeedCharges.sumOf { charge ->
            val feedItem = warehouseItems.find { it.category == WarehouseCategory.FEED && it.assignedFlockIds.contains(charge.flockId) }
                ?: warehouseItems.find { it.category == WarehouseCategory.FEED }
            val pricePerKg = feedItem?.pricePerUnit ?: 0.0
            (charge.chargedAmount / 1000.0) * pricePerKg
        }
    }

    val todayEggProductions = remember(eggProductions, todayJalaliStr) {
        eggProductions.filter { it.dateJalaliStr == todayJalaliStr }
    }
    val todayHealthyEggs = remember(todayEggProductions) {
        todayEggProductions.sumOf { it.healthyCount }
    }
    val todayBrokenEggs = remember(todayEggProductions) {
        todayEggProductions.sumOf { it.brokenCount + it.dirtyCount }
    }

    val totalBirds = remember(flocks) { flocks.sumOf { it.effectiveBirdCount } }

    var showRecommendedDetailsDialog by remember { mutableStateOf(false) }
    var showAllFarmBirdsDialog by remember { mutableStateOf(false) }

    // Precomputed flock nutritional requirements map for instant O(1) lookups during scroll & dialog
    val flockRequirements = remember(flocks, customBreeds, currentSeason, farmAmbientTemp) {
        flocks.associate { flock ->
            val temp = flock.customTempCelsius ?: farmAmbientTemp
            flock.id to DailyNutritionEngine.calculateFlockRequirement(
                flock = flock,
                season = currentSeason,
                customBreeds = customBreeds,
                tempCelsius = temp
            )
        }
    }

    // Recommended daily consumption based on breed nutrition requirements for all active flocks (even when live monitoring is disabled)
    val totalRecommendedFeedGrams = remember(flocks, flockRequirements) {
        flocks.sumOf { flock ->
            flockRequirements[flock.id]?.totalFeedGrams ?: 0.0
        }
    }
    val totalRecommendedWaterMl = remember(flocks, flockRequirements) {
        flocks.sumOf { flock ->
            flockRequirements[flock.id]?.totalWaterMl ?: 0.0
        }
    }

    val recommendedFeedKg = totalRecommendedFeedGrams / 1000.0
    val recommendedFeedText = if (totalRecommendedFeedGrams > 0) {
        if (recommendedFeedKg >= 1.0) "${toPersianDigits(String.format(Locale.US, "%.1f", recommendedFeedKg))} کیلو"
        else "${toPersianDigits(totalRecommendedFeedGrams.toInt())} گرم"
    } else "۰ کیلو"

    val totalRecommendedFeedCost = remember(flocks, flockRequirements, warehouseItems) {
        flocks.sumOf { flock ->
            val req = flockRequirements[flock.id]
            val feedItem = warehouseItems.find { it.category == WarehouseCategory.FEED && it.assignedFlockIds.contains(flock.id) }
                ?: warehouseItems.find { it.category == WarehouseCategory.FEED }
            val pricePerKg = feedItem?.pricePerUnit ?: 0.0
            ((req?.totalFeedGrams ?: 0.0) / 1000.0) * pricePerKg
        }
    }

    val recommendedFeedCostText = if (totalRecommendedFeedCost > 0) {
        "ارزش تخمینی:\n${DailyNutritionEngine.formatTomans(totalRecommendedFeedCost)}"
    } else if (totalRecommendedFeedGrams > 0) {
        "ارزش تخمینی: بدون قیمت"
    } else null

    val recommendedWaterLit = totalRecommendedWaterMl / 1000.0
    val recommendedWaterText = if (totalRecommendedWaterMl > 0) {
        if (recommendedWaterLit >= 1.0) "${toPersianDigits(String.format(Locale.US, "%.1f", recommendedWaterLit))} لیتر"
        else "${toPersianDigits(totalRecommendedWaterMl.toInt())} میلی‌لیتر"
    } else "۰ لیتر"

    // Evaluate urgent alerts across farm
    val urgentAlerts = remember(flocks, warehouseItems, chargesHistory, currentTimeMillis, currentSeason, customBreeds, flockRequirements) {
        val alerts = mutableListOf<String>()

        // 1. Flock feed / water shortages (ONLY alert if there was an actual charge logged!)
        for (flock in flocks) {
            val dailyReq = flockRequirements[flock.id] ?: continue

            if (flock.isFeedEnabled) {
                val latestFeed = chargesHistory.filter { it.flockId == flock.id && it.itemType == ChargeItemType.FEED }
                    .maxByOrNull { it.chargeTimestampMillis }
                if (latestFeed != null && latestFeed.chargedAmount > 0.0) {
                    val feedStatus = LiveConsumptionTracker.calculateLiveStatus(
                        itemType = ChargeItemType.FEED,
                        chargedAmount = latestFeed.chargedAmount,
                        chargeTimestampMillis = latestFeed.chargeTimestampMillis,
                        dailyRequirementTotal = dailyReq.totalFeedGrams,
                        currentTimestampMillis = currentTimeMillis
                    )
                    if (feedStatus.isFinished) {
                        alerts.add("اتمام دان در گله «${flock.name}»")
                    } else if (feedStatus.isLowWarning) {
                        alerts.add("کمبود دان در گله «${flock.name}» (زیر ۲۰٪)")
                    }
                }
            }

            if (flock.isWaterEnabled) {
                val latestWater = chargesHistory.filter { it.flockId == flock.id && it.itemType == ChargeItemType.WATER }
                    .maxByOrNull { it.chargeTimestampMillis }
                if (latestWater != null && latestWater.chargedAmount > 0.0) {
                    val waterStatus = LiveConsumptionTracker.calculateLiveStatus(
                        itemType = ChargeItemType.WATER,
                        chargedAmount = latestWater.chargedAmount,
                        chargeTimestampMillis = latestWater.chargeTimestampMillis,
                        dailyRequirementTotal = dailyReq.totalWaterMl,
                        currentTimestampMillis = currentTimeMillis
                    )
                    if (waterStatus.isFinished) {
                        alerts.add("اتمام آب در گله «${flock.name}»")
                    } else if (waterStatus.isLowWarning) {
                        alerts.add("کمبود آب در گله «${flock.name}» (زیر ۲۰٪)")
                    }
                }
            }
        }

        // 2. Warehouse feed stock shortages (< 3 days)
        val feedItems = warehouseItems.filter { it.category == WarehouseCategory.FEED }
        for (feedItem in feedItems) {
            val assignedFlocks = flocks.filter { it.id in feedItem.assignedFlockIds }
            val dailyFeedGrams = assignedFlocks.sumOf { flockRequirements[it.id]?.totalFeedGrams ?: 0.0 }
            val dailyFeedKg = dailyFeedGrams / 1000.0
            if (dailyFeedKg > 0) {
                val daysRemaining = feedItem.currentStock / dailyFeedKg
                if (daysRemaining <= 3.0) {
                    alerts.add("موجودی دان «${feedItem.name}» فقط برای ${toPersianDigits(String.format(Locale.US, "%.1f", daysRemaining))} روز کافی است")
                }
            } else if (feedItem.minAlertThreshold > 0.0 && feedItem.currentStock <= feedItem.minAlertThreshold) {
                alerts.add("موجودی خوراک «${feedItem.name}» کمتر از حد نصاب انبار است")
            }
        }

        alerts
    }

    val flockIconPainter = painterResource(id = com.example.poultryapp.R.drawable.ic_farm_flocks)

    val homeFlockStatuses = remember(flocks, chargesHistory, currentTimeMillis, flockRequirements) {
        flocks.map { flock ->
            val temp = flock.customTempCelsius ?: farmAmbientTemp
            val dailyReq = flockRequirements[flock.id] ?: DailyNutritionEngine.calculateFlockRequirement(flock, season = currentSeason, customBreeds = customBreeds, tempCelsius = temp)
            val latestFeed = chargesHistory.filter { it.flockId == flock.id && it.itemType == ChargeItemType.FEED }
                .maxByOrNull { it.chargeTimestampMillis }
            val latestWater = chargesHistory.filter { it.flockId == flock.id && it.itemType == ChargeItemType.WATER }
                .maxByOrNull { it.chargeTimestampMillis }

            val feedStatus = LiveConsumptionTracker.calculateLiveStatus(
                itemType = ChargeItemType.FEED,
                chargedAmount = if (flock.isFeedEnabled) latestFeed?.chargedAmount ?: 0.0 else 0.0,
                chargeTimestampMillis = if (flock.isFeedEnabled) latestFeed?.chargeTimestampMillis ?: 0L else 0L,
                dailyRequirementTotal = dailyReq.totalFeedGrams,
                currentTimestampMillis = currentTimeMillis
            )

            val waterStatus = LiveConsumptionTracker.calculateLiveStatus(
                itemType = ChargeItemType.WATER,
                chargedAmount = if (flock.isWaterEnabled) latestWater?.chargedAmount ?: 0.0 else 0.0,
                chargeTimestampMillis = if (flock.isWaterEnabled) latestWater?.chargeTimestampMillis ?: 0L else 0L,
                dailyRequirementTotal = dailyReq.totalWaterMl,
                currentTimestampMillis = currentTimeMillis
            )

            HomeFlockStatus(flock, feedStatus, waterStatus)
        }
    }

    val layingDropAlerts = remember(flocks, eggProductions) {
        LayingBenchmarkEngine.evaluateAllFlocks(flocks, eggProductions)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "داشبورد مدیریت مزرعه",
                subtitle = "${jalaliNow.dayName}، ${toPersianDigits(jalaliNow.day)} ${jalaliNow.monthName} ${toPersianDigits(jalaliNow.year)} — فصل ${currentSeason.titleFa}",
                logoRes = com.example.poultryapp.R.drawable.ic_farm_dashboard,
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "منوی برنامه")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenVoiceAssistant) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "دستیار هوشمند",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenVoiceAssistant,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "دستیار هوشمند",
                        modifier = Modifier.size(22.dp)
                    )
                },
                text = {
                    Text(
                        text = "دستیار هوشمند",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = FarmDimens.Space16, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Note: Removed redundant top quick-action chips row per user request

            // 2. Urgent Attention / Alerts Banner (اخطارها و وضعیت‌های نیازمند اقدام)
            if (urgentAlerts.isNotEmpty()) {
                item {
                    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    Surface(
                        shape = RoundedCornerShape(FarmDimens.CardRadius),
                        color = if (isDark) HealthRedContainerDark else HealthRedContainer.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, if (isDark) HealthRedDark.copy(alpha = 0.4f) else HealthRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = if (isDark) HealthRedDark else HealthRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(FarmDimens.Space8))
                                Text(
                                    text = "نیازمند اقدام فوری (${toPersianDigits(urgentAlerts.size)} مورد):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) HealthRedTextDark else HealthRedText
                                )
                            }
                            Spacer(modifier = Modifier.height(FarmDimens.Space6))
                            urgentAlerts.take(3).forEach { alert ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) HealthRedDark else HealthRed)
                                    )
                                    Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                    Text(
                                        text = alert,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) HealthRedTextDark else HealthRedText,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (urgentAlerts.size > 3) {
                                Text(
                                    text = "و ${toPersianDigits(urgentAlerts.size - 3)} مورد دیگر...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) HealthRedTextDark.copy(alpha = 0.8f) else HealthRedText.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2.3 Farm Weather Forecast & Upcoming Climate Events (پیش‌بینی هواشناسی و رویدادهای اقلیمی فارم)
            item {
                FarmWeatherForecastCard(
                    weatherResult = weatherResult,
                    farmAmbientTemp = farmAmbientTemp,
                    isWeatherLoading = isWeatherLoading,
                    onRefreshWeather = onRefreshWeather
                )
            }

            // 2.4 Laying Drop Benchmark Alert (هشدار تحلیلی افت تولید تخم‌گذاری)
            if (layingDropAlerts.isNotEmpty()) {
                item {
                    LayingDropAlertCard(alerts = layingDropAlerts)
                }
            }

            // 2.5 Quick Daily Egg Logging Card (ثبت آسان تخم‌گذاری روز - فشرده و کم‌عرض)
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRecordEggDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = com.example.poultryapp.R.drawable.ic_farm_egg),
                            contentDescription = "ثبت تخم‌گذاری",
                            modifier = Modifier.size(26.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ثبت تخم‌گذاری امروز",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (todayHealthyEggs > 0) {
                                Text(
                                    text = "امروز: ${toPersianDigits(todayHealthyEggs)} سالم (${toPersianDigits(todayBrokenEggs)} شکسته/کثیف)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = HealthGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "ثبت سریع و افزودن خودکار به انبار",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            onClick = { showRecordEggDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("ثبت سریع", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Farm Vital KPI Grid (2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(FarmDimens.Space8)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                    ) {
                        FarmStatCard(
                            title = "کل پرندگان فارم",
                            value = totalBirds,
                            unit = "قطعه",
                            imageRes = com.example.poultryapp.R.drawable.ic_farm_flocks,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { showAllFarmBirdsDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        FarmStatCard(
                            title = "گله‌های فعال",
                            value = flocks.size,
                            unit = "گله",
                            imageRes = com.example.poultryapp.R.drawable.ic_farm_active_flocks,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            onClick = onNavigateToFlocks,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Header row for feed and water consumption (همیشه فعال، بدون نیاز به دکمه روشن/خاموش)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showRecommendedDetailsDialog = true }
                        ) {
                            Text(
                                text = "مصرف و شارژ امروز فارم",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "جزئیات مصرف استاندارد گله‌ها",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                    ) {
                        val feedKg = totalFarmFeedTodayGrams / 1000.0
                        val feedCostSubtitle = if (totalTodayFeedCost > 0) {
                            "ارزش: ${DailyNutritionEngine.formatTomans(totalTodayFeedCost)}"
                        } else if (totalFarmFeedTodayGrams > 0) {
                            "ارزش: قیمت ثبت نشده"
                        } else {
                            "ارزش: ۰ تومان"
                        }
                        FarmStatCard(
                            title = "مصرف دان امروز",
                            value = if (feedKg >= 1.0) String.format(Locale.US, "%.1f", feedKg) else totalFarmFeedTodayGrams.toInt(),
                            unit = if (feedKg >= 1.0) "کیلوگرم" else "گرم",
                            recommendedValue = recommendedFeedText,
                            recommendedSubtitle = recommendedFeedCostText,
                            subtitle = feedCostSubtitle,
                            imageRes = com.example.poultryapp.R.drawable.ic_farm_feed,
                            iconTint = FeedAmber,
                            modifier = Modifier.weight(1f)
                        )

                        val waterLit = totalFarmWaterTodayMl / 1000.0
                        val waterSubtitle = if (waterLit > 0) "کل شارژ آب فارم" else "مجموع فارم"
                        FarmStatCard(
                            title = "مصرف آب امروز",
                            value = if (waterLit >= 1.0) String.format(Locale.US, "%.1f", waterLit) else totalFarmWaterTodayMl.toInt(),
                            unit = if (waterLit >= 1.0) "لیتر" else "میلی‌لیتر",
                            recommendedValue = recommendedWaterText,
                            recommendedSubtitle = "نیاز کل فارم",
                            subtitle = waterSubtitle,
                            imageRes = com.example.poultryapp.R.drawable.ic_farm_water,
                            iconTint = WaterBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Warehouse Feed Stock Forecast Section
            item {
                SectionHeader(
                    title = "پیش‌بینی ماندگاری دان انبار",
                    subtitle = "تخمین دوام موجودی بر اساس مصرف روزانه گله‌ها",
                    iconRes = com.example.poultryapp.R.drawable.ic_farm_warehouse,
                    actionText = "مدیریت انبار",
                    onActionClick = onNavigateToWarehouse
                )

                val feedItems = warehouseItems.filter { it.category == WarehouseCategory.FEED }
                if (feedItems.isEmpty()) {
                    EmptyStateView(
                        imageRes = com.example.poultryapp.R.drawable.ic_farm_warehouse,
                        title = "دان و خوراکی در انبار ثبت نشده است",
                        message = "با تعریف انواع دان و اتصال گله‌ها، سیستم به صورت خودکار روزهای باقیمانده تا اتمام را محاسبه می‌کند.",
                        actionButtonText = "+ ثبت خوراک در انبار",
                        onActionClick = onNavigateToWarehouse
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(FarmDimens.Space6)) {
                        feedItems.forEach { feedItem ->
                            val assignedFlocks = flocks.filter { it.id in feedItem.assignedFlockIds }
                            val dailyFeedGrams = assignedFlocks.sumOf { flockRequirements[it.id]?.totalFeedGrams ?: 0.0 }
                            val dailyFeedKg = dailyFeedGrams / 1000.0

                            val daysRemaining = if (dailyFeedKg > 0) feedItem.currentStock / dailyFeedKg else Double.MAX_VALUE
                            val isCritical = daysRemaining <= 3.0
                            val isWarning = daysRemaining in 3.0..7.0

                            Surface(
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCritical) HealthRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(FarmDimens.Space12),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = feedItem.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val flocksDesc = if (assignedFlocks.isEmpty()) {
                                            "بدون گله منتسب"
                                        } else {
                                            "مصرف ${toPersianDigits(assignedFlocks.size)} گله (روزانه ${toPersianDigits(String.format(Locale.US, "%.1f", dailyFeedKg))} کیلو)"
                                        }
                                        Text(
                                            text = flocksDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        PersianValueUnit(
                                            value = feedItem.currentStock.toInt(),
                                            unit = feedItem.unit,
                                            valueStyle = MaterialTheme.typography.titleSmall,
                                            unitStyle = MaterialTheme.typography.labelSmall
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        if (daysRemaining < Double.MAX_VALUE) {
                                            FarmStatusBadge(
                                                text = "${toPersianDigits(String.format(Locale.US, "%.1f", daysRemaining))} روز دوام",
                                                color = when {
                                                    isCritical -> HealthRedText
                                                    isWarning -> HealthYellow
                                                    else -> HealthGreen
                                                },
                                                containerColor = when {
                                                    isCritical -> HealthRedContainer
                                                    isWarning -> HealthYellowContainer
                                                    else -> HealthGreenContainer
                                                }
                                            )
                                        } else {
                                            FarmStatusBadge(
                                                text = "نامشخص",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Live Remaining Feed & Water per Flock (Read-Only Monitoring)
            item {
                SectionHeader(
                    title = "مانده زنده دان و آب گله‌ها (نظارت)",
                    subtitle = "پایش تخمینی میزان باقیمانده در دانخوری و آبخوری",
                    iconRes = com.example.poultryapp.R.drawable.ic_farm_feed_water,
                    actionText = "ثبت شارژ",
                    onActionClick = { onNavigateToFeedWater(null) }
                )
            }

            if (flocks.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "هنوز گله‌ای ثبت نشده است. برای تعریف گله از منوی کشویی به بخش «گله‌ها» مراجعه کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(FarmDimens.Space14)
                        )
                    }
                }
            } else {
                items(
                    items = homeFlockStatuses,
                    key = { "home_flock_${it.flock.id}" },
                    contentType = { "home_flock" }
                ) { item ->
                    HomeReadOnlyFlockCard(
                        flock = item.flock,
                        feedStatus = item.feedStatus,
                        waterStatus = item.waterStatus,
                        iconPainter = flockIconPainter,
                        onClick = { onNavigateToFeedWater(item.flock.id) }
                    )
                }
            }

            // 6. Today & Tomorrow Calendar Tasks
            item {
                SectionHeader(
                    title = "برنامه‌ها و رویدادهای کاری",
                    subtitle = "وظایف واکسیناسیون، نظافت و تغذیه",
                    iconRes = com.example.poultryapp.R.drawable.ic_farm_calendar,
                    actionText = "تقویم کامل",
                    onActionClick = onNavigateToCalendar
                )

                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        if (todaySchedules.isEmpty() && tomorrowSchedules.isEmpty()) {
                            Text(
                                text = "هیچ وظیفه برنامه‌ریزی‌شده‌ای برای امروز و فردا ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            if (todaySchedules.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                    Text(
                                        text = "امروز (${jalaliNow.dayName}):",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(FarmDimens.Space4))
                                todaySchedules.forEach { s ->
                                    HomeScheduleRow(s)
                                }
                            }

                            if (tomorrowSchedules.isNotEmpty()) {
                                if (todaySchedules.isNotEmpty()) Spacer(modifier = Modifier.height(FarmDimens.Space10))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )
                                    Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                    Text(
                                        text = "فردا:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(FarmDimens.Space4))
                                tomorrowSchedules.forEach { s ->
                                    HomeScheduleRow(s)
                                }
                            }
                        }
                    }
                }
            }

            // 7. Golden Breeding Academy Banner / Entry Card
            if (onNavigateToGoldenBreeding != null) {
                item {
                    GoldenBreedingBannerCard(onClick = onNavigateToGoldenBreeding)
                }
            }

            // Safe area at bottom
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showRecommendedDetailsDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showRecommendedDetailsDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مصرف پیشنهادی امروز گله‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showRecommendedDetailsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "بستن")
                        }
                    }

                    Text(
                        text = "محاسبه استاندارد تغذیه بر اساس سن، نژاد، فصل ${currentSeason.titleFa} و تعداد پرندگان فارم:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        flocks.forEach { flock ->
                            val temp = flock.customTempCelsius ?: farmAmbientTemp
                            val req = flockRequirements[flock.id] ?: DailyNutritionEngine.calculateFlockRequirement(flock, season = currentSeason, customBreeds = customBreeds, tempCelsius = temp)
                            val flockFeedItem = warehouseItems.find { it.category == WarehouseCategory.FEED && it.assignedFlockIds.contains(flock.id) }
                                ?: warehouseItems.find { it.category == WarehouseCategory.FEED }
                            val flockPricePerKg = flockFeedItem?.pricePerUnit ?: 0.0
                            val flockFeedCost = (req.totalFeedGrams / 1000.0) * flockPricePerKg

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AutoResizeText(
                                            text = flock.name,
                                            targetFontSize = 14.sp,
                                            minFontSize = 10.sp,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(FarmDimens.Space8))
                                        Text(
                                            text = "${toPersianDigits(flock.effectiveBirdCount)} قطعه",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val feedCostText = if (flockFeedCost > 0) " (${DailyNutritionEngine.formatTomans(flockFeedCost)})" else ""
                                        AutoResizeText(
                                            text = "🌾 دان: ${req.formattedTotalFeed}$feedCostText",
                                            targetFontSize = 12.sp,
                                            minFontSize = 9.sp,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FeedAmber,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(FarmDimens.Space8))
                                        Text(
                                            text = "💧 آب: ${req.formattedTotalWater}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = WaterBlue,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مجموع پیشنهادی فارم:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            val totalCostText = if (totalRecommendedFeedCost > 0) " (${DailyNutritionEngine.formatTomans(totalRecommendedFeedCost)})" else ""
                            Text(
                                text = "$recommendedFeedText دان$totalCostText | $recommendedWaterText آب",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAllFarmBirdsDialog) {
        AllFarmBirdsDialog(
            flocks = flocks,
            customBreeds = customBreeds,
            currentSeason = currentSeason,
            farmAmbientTemp = farmAmbientTemp,
            onDismiss = { showAllFarmBirdsDialog = false }
        )
    }

    // 1-Click Daily Egg Record Dialog
    if (showRecordEggDialog) {
        RecordEggDialog(
            flocks = flocks,
            onDismiss = { showRecordEggDialog = false },
            onSave = { flockIds, healthy, broken, dirty, doubleYolk, dateJalaliStr, notes ->
                onLogEggProduction?.invoke(flockIds, healthy, broken, dirty, doubleYolk, dateJalaliStr, notes)
                showRecordEggDialog = false
            }
        )
    }
}

private data class HomeFlockStatus(
    val flock: Flock,
    val feedStatus: LiveTrackerStatus,
    val waterStatus: LiveTrackerStatus
)

/**
 * Clean, read-only flock status card for Dashboard
 */
@Composable
private fun HomeReadOnlyFlockCard(
    flock: Flock,
    feedStatus: LiveTrackerStatus,
    waterStatus: LiveTrackerStatus,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(FarmDimens.RadiusMedium),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(FarmDimens.Space12)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = iconPainter ?: painterResource(id = com.example.poultryapp.R.drawable.ic_farm_flocks),
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(FarmDimens.Space10))
                    Column {
                        Text(
                            text = flock.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${flock.species.titleFa} | ${toPersianDigits(flock.effectiveBirdCount)} قطعه",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (flock.isWithdrawalActive) {
                Spacer(modifier = Modifier.height(FarmDimens.Space8))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HealthRedContainer,
                    border = BorderStroke(1.dp, HealthRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⛔", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "دوره پرهیز دارویی فعال (${flock.activeMedicationName ?: "دارو"})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = HealthRedText
                            )
                            Text(
                                text = "باقیمانده: ${toPersianDigits(flock.withdrawalRemainingDays)} روز و ${toPersianDigits(flock.withdrawalRemainingHours % 24)} ساعت | تخم‌مرغ‌ها غیرقابل مصرف انسانی",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = HealthRedText.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmDimens.Space10))

            // Feed bar
            if (flock.isFeedEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌾 دان مانده: ${feedStatus.remainingAmountFa}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${toPersianDigits((feedStatus.progress * 100).toInt())}٪",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (feedStatus.isLowWarning || feedStatus.isFinished) HealthRed else FeedAmber
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { feedStatus.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(FarmDimens.RadiusFull)),
                    color = if (feedStatus.isLowWarning || feedStatus.isFinished) HealthRed else FeedAmber,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                Text(
                    text = "🌾 پایش دان غیرفعال است",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(FarmDimens.Space6))

            // Water bar
            if (flock.isWaterEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💧 آب مانده: ${waterStatus.remainingAmountFa}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${toPersianDigits((waterStatus.progress * 100).toInt())}٪",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (waterStatus.isLowWarning || waterStatus.isFinished) HealthRed else WaterBlue
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { waterStatus.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(FarmDimens.RadiusFull)),
                    color = if (waterStatus.isLowWarning || waterStatus.isFinished) HealthRed else WaterBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                Text(
                    text = "💧 پایش آب غیرفعال است",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Compact schedule task row
 */
@Composable
private fun HomeScheduleRow(schedule: CalendarSchedule) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(FarmDimens.Space6))
            AutoResizeText(
                text = schedule.title,
                targetFontSize = 12.sp,
                minFontSize = 9.sp,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        Text(
            text = schedule.timeDisplayFa,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Specification data structure for individual bird groups across the farm,
 * decoupled from flock structure.
 */
private data class FarmBirdSpec(
    val id: String,
    val originFlockName: String,
    val species: BirdSpecies,
    val breed: String,
    val productionType: ProductionType,
    val birdCount: Int,
    val ageDays: Int,
    val ageWeeks: Int,
    val ageDisplayFa: String,
    val ambientTempCelsius: Double,
    val dailyFeedPerBirdGrams: Double,
    val dailyWaterPerBirdMl: Double,
    val totalDailyFeedKg: Double,
    val totalDailyWaterLitres: Double
)

@Composable
private fun AllFarmBirdsDialog(
    flocks: List<Flock>,
    customBreeds: List<CustomBreed>,
    currentSeason: FarmSeason,
    farmAmbientTemp: Double?,
    onDismiss: () -> Unit
) {
    val allBirds = remember(flocks, customBreeds, currentSeason, farmAmbientTemp) {
        val list = mutableListOf<FarmBirdSpec>()
        flocks.forEach { flock ->
            val temp = flock.customTempCelsius ?: farmAmbientTemp ?: 22.0
            if (flock.subGroups.isNotEmpty()) {
                flock.subGroups.forEach { sg ->
                    val prodType = BreedCatalog.getProductionType(sg.species, sg.breed, customBreeds)
                    val req = DailyNutritionEngine.calculateDailyRequirement(
                        species = sg.species,
                        breedName = sg.breed,
                        ageDays = sg.ageDays,
                        birdCount = sg.birdCount,
                        season = currentSeason,
                        customBreeds = customBreeds,
                        tempCelsius = temp
                    )
                    list.add(
                        FarmBirdSpec(
                            id = sg.id,
                            originFlockName = flock.name,
                            species = sg.species,
                            breed = sg.breed,
                            productionType = prodType,
                            birdCount = sg.birdCount,
                            ageDays = sg.ageDays,
                            ageWeeks = sg.ageWeeks,
                            ageDisplayFa = sg.ageDisplayFa,
                            ambientTempCelsius = temp,
                            dailyFeedPerBirdGrams = req.perBirdFeedGrams,
                            dailyWaterPerBirdMl = req.perBirdWaterMl,
                            totalDailyFeedKg = req.totalFeedGrams / 1000.0,
                            totalDailyWaterLitres = req.totalWaterMl / 1000.0
                        )
                    )
                }
            } else {
                val prodType = BreedCatalog.getProductionType(flock.species, flock.breed, customBreeds)
                val req = DailyNutritionEngine.calculateDailyRequirement(
                    species = flock.species,
                    breedName = flock.breed,
                    ageDays = flock.ageDays,
                    birdCount = flock.effectiveBirdCount,
                    season = currentSeason,
                    customBreeds = customBreeds,
                    tempCelsius = temp
                )
                list.add(
                    FarmBirdSpec(
                        id = flock.id.toString(),
                        originFlockName = flock.name,
                        species = flock.species,
                        breed = flock.breed,
                        productionType = prodType,
                        birdCount = flock.effectiveBirdCount,
                        ageDays = flock.ageDays,
                        ageWeeks = flock.ageWeeks,
                        ageDisplayFa = flock.ageDisplayFa,
                        ambientTempCelsius = temp,
                        dailyFeedPerBirdGrams = req.perBirdFeedGrams,
                        dailyWaterPerBirdMl = req.perBirdWaterMl,
                        totalDailyFeedKg = req.totalFeedGrams / 1000.0,
                        totalDailyWaterLitres = req.totalWaterMl / 1000.0
                    )
                )
            }
        }
        list
    }

    var selectedSpecies by remember { mutableStateOf<BirdSpecies?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredBirds = remember(allBirds, selectedSpecies, searchQuery) {
        allBirds.filter { bird ->
            (selectedSpecies == null || bird.species == selectedSpecies) &&
                    (searchQuery.isBlank() ||
                            bird.breed.contains(searchQuery, ignoreCase = true) ||
                            bird.originFlockName.contains(searchQuery, ignoreCase = true))
        }
    }

    val totalBirdsCount = remember(allBirds) { allBirds.sumOf { it.birdCount } }
    val totalDailyFeedKg = remember(allBirds) { allBirds.sumOf { it.totalDailyFeedKg } }
    val totalDailyWaterL = remember(allBirds) { allBirds.sumOf { it.totalDailyWaterLitres } }
    val distinctBreedsCount = remember(allBirds) { allBirds.map { it.breed }.distinct().size }

    val availableSpecies = remember(allBirds) {
        allBirds.map { it.species }.distinct()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FarmDimens.Space16)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = com.example.poultryapp.R.drawable.ic_farm_flocks),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(FarmDimens.Space8))
                            Text(
                                text = "شناسنامه جامع پرندگان فارم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "مشخصات و آمار تمام پرندگان فارغ از ساختار گله‌ها",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space12))

                // Farm summary KPI row
                Surface(
                    shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FarmDimens.Space10),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "کل پرندگان",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${toPersianDigits(totalBirdsCount)} قطعه",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "تنوع نژادها",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${toPersianDigits(distinctBreedsCount)} نژاد",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "دان کل روزانه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${toPersianDigits(String.format(Locale.US, "%.1f", totalDailyFeedKg))} کیلو",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FeedAmber
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "آب کل روزانه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${toPersianDigits(String.format(Locale.US, "%.1f", totalDailyWaterL))} لیتر",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WaterBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space10))

                // Species Filter Chips
                if (availableSpecies.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSpecies == null,
                                onClick = { selectedSpecies = null },
                                label = { Text("همه (${toPersianDigits(totalBirdsCount)})") }
                            )
                        }
                        items(availableSpecies) { sp ->
                            val count = allBirds.filter { it.species == sp }.sumOf { it.birdCount }
                            FilterChip(
                                selected = selectedSpecies == sp,
                                onClick = { selectedSpecies = if (selectedSpecies == sp) null else sp },
                                label = { Text("${sp.titleFa} (${toPersianDigits(count)})") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(FarmDimens.Space8))
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجوی نام نژاد یا سالن...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(FarmDimens.Space10))

                // Birds List
                if (filteredBirds.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (allBirds.isEmpty()) "هیچ پرنده‌ای در فارم ثبت نشده است." else "پرنده‌ای با این مشخصات یافت نشد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(FarmDimens.Space10)
                    ) {
                        items(filteredBirds, key = { it.id }) { bird ->
                            val percentOfFarm = if (totalBirdsCount > 0) (bird.birdCount * 100) / totalBirdsCount else 0

                            Surface(
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                    // Top Row: Species, Production Type, Origin Salon
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = bird.species.titleFa,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when (bird.productionType) {
                                                    ProductionType.MEAT -> HealthRedContainer
                                                    ProductionType.LAYER -> FeedAmber.copy(alpha = 0.2f)
                                                    ProductionType.ORNAMENTAL -> MaterialTheme.colorScheme.tertiaryContainer
                                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                                }
                                            ) {
                                                Text(
                                                    text = bird.productionType.titleFa,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "📍 سالن: ${bird.originFlockName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(FarmDimens.Space8))

                                    // Breed title & bird count
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = bird.breed,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${toPersianDigits(bird.birdCount)} قطعه (${toPersianDigits(percentOfFarm)}٪)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(FarmDimens.Space4))

                                    // Age and Temperature Info
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "سن: ${bird.ageDisplayFa}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "دمای سالن: ${toPersianDigits(bird.ambientTempCelsius.toInt())}°C",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = FarmDimens.Space8),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )

                                    // Consumption Stats Grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "🌾 دان روزانه هر پرنده: ${toPersianDigits(bird.dailyFeedPerBirdGrams.toInt())} گرم",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = FeedAmber
                                            )
                                            Text(
                                                text = "مجموع دان این دسته: ${toPersianDigits(String.format(Locale.US, "%.2f", bird.totalDailyFeedKg))} کیلوگرم",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "💧 آب روزانه هر پرنده: ${toPersianDigits(bird.dailyWaterPerBirdMl.toInt())} ml",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = WaterBlue
                                            )
                                            Text(
                                                text = "مجموع آب این دسته: ${toPersianDigits(String.format(Locale.US, "%.1f", bird.totalDailyWaterLitres))} لیتر",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FarmWeatherForecastCard(
    weatherResult: com.example.poultryapp.service.WeatherResult?,
    farmAmbientTemp: Double?,
    isWeatherLoading: Boolean,
    onRefreshWeather: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val displayTemp = farmAmbientTemp ?: weatherResult?.temperatureCelsius
    val cityName = weatherResult?.cityName?.ifBlank { "ایستگاه مزرعه" } ?: "ایستگاه مزرعه"
    val province = weatherResult?.province ?: ""
    val condition = weatherResult?.conditionType ?: WeatherConditionType.CLEAR
    val upcomingAlerts = weatherResult?.upcomingAlerts ?: emptyList()
    val forecastDays = weatherResult?.forecastDays ?: emptyList()

    val conditionIcon = when (condition) {
        WeatherConditionType.RAIN, WeatherConditionType.HEAVY_RAIN, WeatherConditionType.DRIZZLE -> Icons.Default.WaterDrop
        WeatherConditionType.SNOW -> Icons.Default.AcUnit
        WeatherConditionType.CLOUDY, WeatherConditionType.PARTLY_CLOUDY, WeatherConditionType.FOGGY -> Icons.Default.CloudQueue
        WeatherConditionType.THUNDERSTORM -> Icons.Default.WaterDrop
        WeatherConditionType.WINDY -> Icons.Default.Thermostat
        WeatherConditionType.CLEAR -> Icons.Default.Thermostat
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val weatherPalette = remember(condition, displayTemp, isDark) {
        WeatherThemePalette.getThemeForWeather(
            condition = condition,
            tempCelsius = displayTemp,
            isDarkTheme = isDark
        )
    }
    val isCardDark = weatherPalette.isCardDark

    val cardGradient = weatherPalette.backgroundBrush
    val cardBorderBrush = weatherPalette.borderBrush

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "weather_chevron"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, cardBorderBrush),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardGradient)
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Minimized Status Bar (Always Visible, Clickable to toggle)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Top Row: Avatar + Title & Condition on Right, Temperature + Refresh + Chevron on Left
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Weather Condition Avatar Icon
                    Surface(
                        shape = CircleShape,
                        color = weatherPalette.iconBackground,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = conditionIcon,
                                contentDescription = null,
                                tint = weatherPalette.iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // City & Weather Condition Title
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AutoResizeText(
                                text = "اقلیم فارم ($cityName)",
                                targetFontSize = 14.sp,
                                minFontSize = 10.sp,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = weatherPalette.titleColor,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = weatherPalette.ambientEmoji,
                                fontSize = 12.sp
                            )
                        }
                        AutoResizeText(
                            text = condition.titleFa,
                            targetFontSize = 11.sp,
                            minFontSize = 8.5.sp,
                            style = MaterialTheme.typography.labelSmall,
                            color = weatherPalette.titleColor.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Live Temperature
                    Text(
                        text = if (displayTemp != null) "${toPersianDigits(String.format(Locale.US, "%.1f", displayTemp))}°C" else "--°C",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = weatherPalette.temperatureColor
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Refresh Button
                    if (onRefreshWeather != null) {
                        IconButton(
                            onClick = onRefreshWeather,
                            enabled = !isWeatherLoading,
                            modifier = Modifier.size(28.dp)
                        ) {
                            if (isWeatherLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = weatherPalette.titleColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "بروزرسانی",
                                    tint = weatherPalette.titleColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Expand / Collapse Chevron
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "بستن پیش‌بینی" else "مشاهده پیش‌بینی کامل",
                        tint = weatherPalette.titleColor.copy(alpha = 0.9f),
                        modifier = Modifier
                            .rotate(chevronRotation)
                            .size(20.dp)
                    )
                }

                // Sub-Row (Quick Metrics): Humidity, Wind, Forecast Hint
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chipBg = if (isCardDark) Color.Black.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.88f)
                    val chipBorder = BorderStroke(0.6.dp, if (isCardDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f))
                    val chipTextColor = if (isCardDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)

                    // Humidity Chip
                    if (weatherResult?.humidityPercent != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = chipBg,
                            border = chipBorder
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = if (isCardDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "رطوبت: ${toPersianDigits(weatherResult.humidityPercent)}٪",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = chipTextColor
                                )
                            }
                        }
                    }

                    // Wind Chip
                    if (weatherResult?.windSpeedKmH != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = chipBg,
                            border = chipBorder
                        ) {
                            Text(
                                text = "💨 باد: ${toPersianDigits(weatherResult.windSpeedKmH.toInt())} km/h",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = chipTextColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Daylight Duration Chip
                    if (weatherResult?.daylightDurationFa != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = chipBg,
                            border = chipBorder
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("☀️", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "طول روز: ${weatherResult.daylightDurationFa}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = chipTextColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Expand / Collapse Hint (High contrast, styled pill button)
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = weatherPalette.badgeBackground,
                        border = BorderStroke(0.6.dp, weatherPalette.iconTint.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "بستن جزئیات ▲" else "پیش‌بینی ۴ روزه ▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = weatherPalette.badgeTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Expanded Full Details (Smooth Spring & Fade Animation)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                ) + fadeIn(animationSpec = tween(280)),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(animationSpec = tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 2.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = weatherPalette.titleColor.copy(alpha = 0.2f)
                    )

                    // Photoperiod & Natural Daylight Section (طول روشنایی روز و برنامه نوری)
                    if (weatherResult?.daylightDurationFa != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCardDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFFFFBEB).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, if (isCardDark) Color(0xFFF59E0B).copy(alpha = 0.45f) else Color(0xFFD97706).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("☀️", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "مدت زمان روشنایی طبیعی روز ($cityName):",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCardDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                        )
                                    }
                                    Text(
                                        text = weatherResult.daylightDurationFa,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCardDark) Color.White else Color(0xFF78350F)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "طلوع: ${weatherResult.sunriseFa ?: "--:--"} | غروب: ${weatherResult.sunsetFa ?: "--:--"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCardDark) Color(0xFFCBD5E1) else Color(0xFF451A03)
                                    )
                                    if (weatherResult.supplementalLightingFa != null) {
                                        Text(
                                            text = "💡 نیاز نور مصنوعی گله تخم‌گذار: ${weatherResult.supplementalLightingFa}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCardDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 1: Upcoming Weather Event / Meteorological Alert
                    val highlightedAlert = upcomingAlerts.firstOrNull { it.category != WeatherAlertCategory.FAVORABLE }
                        ?: upcomingAlerts.firstOrNull()

                    if (highlightedAlert != null) {
                        val alertBgColor = when (highlightedAlert.category) {
                            WeatherAlertCategory.RAIN -> if (isCardDark) Color(0xFF0C4A6E).copy(alpha = 0.75f) else Color(0xFFE0F2FE)
                            WeatherAlertCategory.COLD_FROST -> if (isCardDark) Color(0xFF164E63).copy(alpha = 0.75f) else Color(0xFFE0F7FA)
                            WeatherAlertCategory.HEAT_WAVE -> if (isCardDark) Color(0xFF451A03).copy(alpha = 0.85f) else Color(0xFFFFF7ED)
                            WeatherAlertCategory.STORM_WIND -> if (isCardDark) Color(0xFF3B0764).copy(alpha = 0.75f) else Color(0xFFEDE7F6)
                            WeatherAlertCategory.FAVORABLE -> if (isCardDark) Color(0xFF064E3B).copy(alpha = 0.75f) else Color(0xFFE8F5E9)
                        }
                        val alertBorderColor = when (highlightedAlert.category) {
                            WeatherAlertCategory.RAIN -> if (isCardDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            WeatherAlertCategory.COLD_FROST -> if (isCardDark) Color(0xFF4DD0E1) else Color(0xFF00838F)
                            WeatherAlertCategory.HEAT_WAVE -> if (isCardDark) Color(0xFFFB923C) else Color(0xFFEA580C)
                            WeatherAlertCategory.STORM_WIND -> if (isCardDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
                            WeatherAlertCategory.FAVORABLE -> if (isCardDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                        }
                        val alertIcon = when (highlightedAlert.category) {
                            WeatherAlertCategory.RAIN -> Icons.Default.WaterDrop
                            WeatherAlertCategory.COLD_FROST -> Icons.Default.AcUnit
                            WeatherAlertCategory.HEAT_WAVE -> Icons.Default.Whatshot
                            WeatherAlertCategory.STORM_WIND -> Icons.Default.Thermostat
                            WeatherAlertCategory.FAVORABLE -> Icons.Default.CheckCircle
                        }
                        val alertIconTint = alertBorderColor

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = alertBgColor,
                            border = BorderStroke(1.dp, alertBorderColor.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = alertIconTint.copy(alpha = if (isCardDark) 0.25f else 0.15f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = alertIcon,
                                            contentDescription = null,
                                            tint = alertIconTint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = highlightedAlert.titleFa,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCardDark) Color.White else Color(0xFF0F172A)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = alertIconTint.copy(alpha = if (isCardDark) 0.3f else 0.2f)
                                        ) {
                                            Text(
                                                text = highlightedAlert.dayLabelFa,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = alertIconTint,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = highlightedAlert.descriptionFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCardDark) Color.White.copy(alpha = 0.9f) else Color(0xFF334155),
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    // Section 2: 4-Day Mini Weather Forecast Strip
                    if (forecastDays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            forecastDays.take(4).forEach { day ->
                                val dayIcon = when {
                                    day.precipitationProbPercent >= 35 || day.conditionType == WeatherConditionType.RAIN || day.conditionType == WeatherConditionType.HEAVY_RAIN -> Icons.Default.WaterDrop
                                    day.conditionType == WeatherConditionType.SNOW -> Icons.Default.AcUnit
                                    day.conditionType == WeatherConditionType.CLOUDY || day.conditionType == WeatherConditionType.PARTLY_CLOUDY -> Icons.Default.CloudQueue
                                    else -> Icons.Default.Thermostat
                                }
                                val dayIconTint = when {
                                    day.precipitationProbPercent >= 35 || day.conditionType == WeatherConditionType.RAIN -> if (isCardDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                    day.conditionType == WeatherConditionType.SNOW -> if (isCardDark) Color(0xFF4DD0E1) else Color(0xFF00ACC1)
                                    day.tempMax >= 30 -> if (isCardDark) Color(0xFFFB923C) else Color(0xFFE65100)
                                    else -> weatherPalette.titleColor
                                }

                                val shortCondLabel = when (day.conditionType) {
                                    WeatherConditionType.CLEAR -> "صاف"
                                    WeatherConditionType.PARTLY_CLOUDY -> "نیمه‌ابری"
                                    WeatherConditionType.CLOUDY -> "ابری"
                                    WeatherConditionType.FOGGY -> "مه‌آلود"
                                    WeatherConditionType.DRIZZLE -> "نم‌باران"
                                    WeatherConditionType.RAIN -> "بارانی"
                                    WeatherConditionType.HEAVY_RAIN -> "رگبار"
                                    WeatherConditionType.SNOW -> "برفی"
                                    WeatherConditionType.THUNDERSTORM -> "طوفانی"
                                    WeatherConditionType.WINDY -> "باد شدید"
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCardDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.88f),
                                    border = BorderStroke(0.6.dp, if (isCardDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = day.dayNameFa,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCardDark) Color.White else Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Icon(
                                            imageVector = dayIcon,
                                            contentDescription = null,
                                            tint = dayIconTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        if (day.precipitationProbPercent >= 20) {
                                            Text(
                                                text = "🌧 ${toPersianDigits(day.precipitationProbPercent)}٪",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCardDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                                fontSize = 9.sp
                                            )
                                        } else {
                                            Text(
                                                text = shortCondLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCardDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569),
                                                fontSize = 9.sp,
                                                maxLines = 1
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${toPersianDigits(day.tempMax.toInt())}° / ${toPersianDigits(day.tempMin.toInt())}°",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCardDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * بنر شیک و چشم‌نواز ورود به بخش «پرورش طلایی و آکادمی تخصصی» در داشبورد اصلی
 */
@Composable
fun GoldenBreedingBannerCard(
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF042618),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(Color(0xFF10B981), Color(0xFFFBBF24), Color(0xFF34D399))
            )
        ),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF04281A),
                                Color(0xFF064E3B).copy(alpha = 0.9f),
                                Color(0xFF063321)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // آیکون کاپ طلایی
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👑", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "آکادمی پرورش طلایی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFDE68A)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.25f),
                            border = BorderStroke(0.8.dp, Color(0xFF34D399))
                        ) {
                            Text(
                                text = "۰ تا ۱۰۰",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color(0xFFA7F3D0),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "آموزش گام‌به‌گام ۱۱ پرنده، آزمون‌های تعاملی و واژه‌نامه تخصصی",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD1FAE5)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "ورود به بازی",
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LayingDropAlertCard(alerts: List<com.example.poultryapp.data.local.LayingDropAlert>) {
    var expandedAlertId by remember { mutableStateOf<Long?>(alerts.firstOrNull()?.flockId) }

    Surface(
        shape = RoundedCornerShape(FarmDimens.CardRadius),
        color = HealthYellowContainer.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, HealthYellow.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(FarmDimens.Space12)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = HealthYellow,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(FarmDimens.Space8))
                Text(
                    text = "هشدار افت غیرعادی تخم‌گذاری (${toPersianDigits(alerts.size)} گله)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(FarmDimens.Space6))

            alerts.forEach { alert ->
                val isExpanded = expandedAlertId == alert.flockId
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            expandedAlertId = if (isExpanded) null else alert.flockId
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.flockName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "افت ${toPersianDigits(String.format(java.util.Locale.US, "%.1f", alert.dropDifferencePercent))}٪ نسبت به هفته پیش (۳ روز اخیر: ${toPersianDigits(String.format(java.util.Locale.US, "%.1f", alert.recentThreeDayAveragePercent))}٪)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HealthRedText
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "چک‌لیست ۴ گانه عیب‌یابی سالن:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            alert.diagnosticChecklist.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${toPersianDigits(idx + 1)}. ",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
