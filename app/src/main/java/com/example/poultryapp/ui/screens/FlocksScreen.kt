package com.example.poultryapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.ui.components.AutoResizeText
import kotlinx.coroutines.launch
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.FlockSubGroup
import com.example.poultryapp.data.local.WarehouseCategory
import com.example.poultryapp.data.local.WarehouseItem
import com.example.poultryapp.data.local.EggProduction
import com.example.poultryapp.data.local.BirdTransfer
import com.example.poultryapp.data.local.FlockMortalityLog
import com.example.poultryapp.data.local.MortalityReason
import com.example.poultryapp.data.local.UserRole
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.engine.DailyNutritionEngine
import com.example.poultryapp.engine.FarmSeason
import com.example.poultryapp.engine.PoultryTemperatureEngine
import com.example.poultryapp.engine.toPersianDigits
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.poultryapp.data.local.CalendarSchedule
import com.example.poultryapp.data.local.FeedWaterCharge
import com.example.poultryapp.data.local.AppSettings
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.service.WeatherResult
import com.example.poultryapp.theme.*
import com.example.poultryapp.ui.components.*

enum class FlocksSubTab(val titleFa: String, val icon: ImageVector) {
    OVERVIEW("گله‌ها", Icons.Default.Groups),
    EGG_PRODUCTION("تخم‌گذاری", Icons.Default.Circle),
    MORTALITY("آمار تلفات", Icons.Default.TrendingDown),
    SALES_SLAUGHTER("فروش و کشتار", Icons.Default.ShoppingCart),
    FEED_WATER("دان و آب", Icons.Default.WaterDrop),
    CALENDAR("تقویم فارم", Icons.Default.CalendarMonth)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlocksScreen(
    flocks: List<Flock>,
    warehouseItems: List<WarehouseItem> = emptyList(),
    customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList(),
    farmAmbientTemp: Double? = null,
    initialSubTab: FlocksSubTab = FlocksSubTab.OVERVIEW,
    onSubTabSelected: (FlocksSubTab) -> Unit = {},
    userRole: UserRole = UserRole.MANAGER,
    mortalityLogs: List<FlockMortalityLog> = emptyList(),
    birdSales: List<com.example.poultryapp.data.local.BirdSaleLog> = emptyList(),
    birdSlaughters: List<com.example.poultryapp.data.local.BirdSlaughterLog> = emptyList(),
    eggProductions: List<EggProduction> = emptyList(),
    birdTransfers: List<BirdTransfer> = emptyList(),
    onTransferBirds: (fromFlockId: Long, toFlockId: Long, count: Int, notes: String?) -> Unit = { _, _, _, _ -> },
    onLogEggProduction: (flockIds: List<Long>, healthy: Int, broken: Int, dirty: Int, doubleYolk: Int, dateJalaliStr: String, notes: String?) -> Unit = { _, _, _, _, _, _, _ -> },
    onLogDetailedEggProduction: ((flockIds: List<Long>, fertile: Int, tableEgg: Int, broken: Int, dirty: Int, doubleYolk: Int, trayCount: Double, trayGrossWeightGrams: Double?, netEggAverageWeightGrams: Double?, dateJalaliStr: String, notes: String?) -> Unit)? = null,
    onLogMortality: ((flockId: Long, count: Int, reason: MortalityReason, diagnosisHint: String?, dateJalaliStr: String, notes: String?) -> Unit)? = null,
    onDeleteMortality: ((Long) -> Unit)? = null,
    onSellBirds: ((flockId: Long, birdCount: Int, liveWeightKg: Double?, unitPriceTomans: Double?, totalRevenueTomans: Double, buyerName: String?, dateJalaliStr: String, notes: String?, createFinanceTransaction: Boolean) -> Unit)? = null,
    onDeleteBirdSale: ((Long) -> Unit)? = null,
    onSlaughterBirds: ((flockId: Long, birdCount: Int, birdAgeDays: Int, liveWeightKg: Double?, carcassWeightKg: Double?, dateJalaliStr: String, notes: String?) -> Unit)? = null,
    onDeleteBirdSlaughter: ((Long) -> Unit)? = null,
    onDeleteEggProduction: (Long) -> Unit = {},
    onAddFlock: (
        name: String,
        species: BirdSpecies,
        breed: String,
        ageDays: Int,
        birdCount: Int,
        notes: String?,
        subGroups: List<FlockSubGroup>,
        isFeedEnabled: Boolean,
        isWaterEnabled: Boolean,
        customTempCelsius: Double?
    ) -> Unit,
    onEditFlock: (flock: Flock) -> Unit,
    onDeleteFlock: (flockId: Long) -> Unit,
    onSelectFlockForCharge: (Flock) -> Unit = {},
    onUpdateFlockCustomTemp: ((flockId: Long, tempCelsius: Double?) -> Unit)? = null,
    // Feed & Water subtab dependencies
    selectedFlockId: Long? = null,
    onSelectFlock: ((Long) -> Unit)? = null,
    latestFeedCharge: FeedWaterCharge? = null,
    latestWaterCharge: FeedWaterCharge? = null,
    todayFeedTotal: Double = 0.0,
    todayWaterTotal: Double = 0.0,
    chargeHistory: List<FeedWaterCharge> = emptyList(),
    settings: AppSettings? = null,
    weatherResult: WeatherResult? = null,
    isWeatherLoading: Boolean = false,
    onRefreshWeather: (() -> Unit)? = null,
    onSelectCity: ((province: String, city: String) -> Unit)? = null,
    onGpsDetect: (() -> Unit)? = null,
    onChargeSubmit: ((flockId: Long, itemType: ChargeItemType, amount: Double, notes: String?) -> Unit)? = null,
    onDeleteCharge: ((chargeId: Long) -> Unit)? = null,
    onToggleFeedTracking: ((flockId: Long, isEnabled: Boolean) -> Unit)? = null,
    onToggleWaterTracking: ((flockId: Long, isEnabled: Boolean) -> Unit)? = null,
    // Calendar subtab dependencies
    schedules: List<CalendarSchedule> = emptyList(),
    onAddSchedule: ((CalendarSchedule) -> Unit)? = null,
    onEditSchedule: ((CalendarSchedule) -> Unit)? = null,
    onDeleteSchedule: ((Long) -> Unit)? = null,
    onToggleSchedule: ((Long, Boolean) -> Unit)? = null,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenVoiceAssistant: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val pagerState = rememberPagerState(
        initialPage = initialSubTab.ordinal,
        pageCount = { FlocksSubTab.entries.size }
    )

    LaunchedEffect(initialSubTab) {
        if (pagerState.currentPage != initialSubTab.ordinal) {
            pagerState.scrollToPage(initialSubTab.ordinal)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        onSubTabSelected(FlocksSubTab.entries[pagerState.currentPage])
    }

    val activeSubTab = FlocksSubTab.entries[pagerState.currentPage]
    var showAddDialog by remember { mutableStateOf(false) }
    var showRecordEggDialog by remember { mutableStateOf(false) }
    var showLogMortalityDialog by remember { mutableStateOf(false) }
    var mortalityTargetFlockId by remember { mutableStateOf<Long?>(null) }
    var showSellBirdsDialog by remember { mutableStateOf(false) }
    var sellTargetFlockId by remember { mutableStateOf<Long?>(null) }
    var showSlaughterBirdsDialog by remember { mutableStateOf(false) }
    var slaughterTargetFlockId by remember { mutableStateOf<Long?>(null) }
    var transferringSourceFlock by remember { mutableStateOf<Flock?>(null) }
    var editingFlock by remember { mutableStateOf<Flock?>(null) }
    var deletingFlockId by remember { mutableStateOf<Long?>(null) }
    var deletingEggProductionId by remember { mutableStateOf<Long?>(null) }
    var deletingMortalityId by remember { mutableStateOf<Long?>(null) }
    var deletingBirdSaleId by remember { mutableStateOf<Long?>(null) }
    var deletingBirdSlaughterId by remember { mutableStateOf<Long?>(null) }
    var selectedMortalityFlockFilterId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exitViewMode by rememberSaveable { mutableStateOf("ALL") } // "ALL", "SALES", "SLAUGHTERS"
    var selectedExitFlockFilterId by rememberSaveable { mutableStateOf<Long?>(null) }
    var tempDialogFlock by remember { mutableStateOf<Flock?>(null) }
    var viewingThermalGuideForFlock by remember { mutableStateOf<Pair<Flock, com.example.poultryapp.engine.ThermalStressGuide>?>(null) }
    var expandedFlockIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val currentSeason: FarmSeason = remember { DailyNutritionEngine.getCurrentSeason() }

    val totalBirds = remember(flocks) { flocks.sumOf { it.effectiveBirdCount } }
    val bioMortalityLogs = remember(mortalityLogs) {
        com.example.poultryapp.data.local.FlockExitAnalytics.filterBiologicalMortality(mortalityLogs)
    }

    val flockRequirements = remember(flocks, currentSeason, customBreeds, farmAmbientTemp) {
        flocks.associate { flock ->
            val temp = flock.customTempCelsius ?: farmAmbientTemp
            flock.id to DailyNutritionEngine.calculateFlockRequirement(flock, season = currentSeason, customBreeds = customBreeds, tempCelsius = temp)
        }
    }

    val flockIconPainter = painterResource(id = com.example.poultryapp.R.drawable.ic_farm_flocks)

    Scaffold(
        topBar = {
            Column {
                AppTopBar(
                    title = when (activeSubTab) {
                        FlocksSubTab.OVERVIEW -> "مدیریت گله‌ها"
                        FlocksSubTab.EGG_PRODUCTION -> "ثبت و آمار تخم‌گذاری"
                        FlocksSubTab.MORTALITY -> "آمار و پایش تلفات فارم"
                        FlocksSubTab.SALES_SLAUGHTER -> "فروش و کشتار پرنده"
                        FlocksSubTab.FEED_WATER -> "پایش دان و آب زنده"
                        FlocksSubTab.CALENDAR -> "تقویم و برنامه‌ریزی فارم"
                    },
                    subtitle = when (activeSubTab) {
                        FlocksSubTab.OVERVIEW -> "${toPersianDigits(flocks.size)} سالن فعال | ${toPersianDigits(totalBirds)} قطعه پرنده"
                        FlocksSubTab.EGG_PRODUCTION -> "${toPersianDigits(eggProductions.size)} روز ثبت‌شده | مجموع ${toPersianDigits(eggProductions.sumOf { it.healthyCount })} عدد تخم‌مرغ سالم"
                        FlocksSubTab.MORTALITY -> "${toPersianDigits(bioMortalityLogs.sumOf { it.count })} قطعه کل تلفات زیستی"
                        FlocksSubTab.SALES_SLAUGHTER -> "${toPersianDigits(birdSales.sumOf { it.birdCount })} فروش | ${toPersianDigits(birdSlaughters.sumOf { it.birdCount })} کشتار"
                        FlocksSubTab.FEED_WATER -> "پایش نرخ مصرف، وضعیت زنده و شارژ جیره"
                        FlocksSubTab.CALENDAR -> "${toPersianDigits(schedules.size)} برنامه و یادآور فعال"
                    },
                    logoRes = when (activeSubTab) {
                        FlocksSubTab.OVERVIEW -> com.example.poultryapp.R.drawable.ic_farm_flocks
                        FlocksSubTab.EGG_PRODUCTION -> com.example.poultryapp.R.drawable.ic_farm_egg
                        FlocksSubTab.MORTALITY -> com.example.poultryapp.R.drawable.ic_farm_health
                        FlocksSubTab.SALES_SLAUGHTER -> com.example.poultryapp.R.drawable.ic_farm_flocks
                        FlocksSubTab.FEED_WATER -> com.example.poultryapp.R.drawable.ic_farm_feed_water
                        FlocksSubTab.CALENDAR -> com.example.poultryapp.R.drawable.ic_farm_calendar
                    },
                    navigationIcon = onOpenDrawer?.let {
                        {
                            IconButton(onClick = it) {
                                Icon(Icons.Default.Menu, contentDescription = "منو")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { onOpenVoiceAssistant?.invoke() }) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "دستیار هوشمند",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )

                // SubTab Switcher Pinned Under TopBar
                val subTabItems = remember {
                    FlocksSubTab.entries.map {
                        FarmSubTabItem(
                            title = it.titleFa,
                            icon = if (it == FlocksSubTab.EGG_PRODUCTION) null else it.icon,
                            drawableRes = if (it == FlocksSubTab.EGG_PRODUCTION) com.example.poultryapp.R.drawable.ic_farm_egg else null
                        )
                    }
                }
                FarmSubTabBar(
                    items = subTabItems,
                    selectedIndex = pagerState.currentPage,
                    pagerState = pagerState,
                    onTabSelected = { page ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (activeSubTab == FlocksSubTab.OVERVIEW) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "افزودن گله")
                }
            } else if (activeSubTab == FlocksSubTab.EGG_PRODUCTION) {
                FloatingActionButton(
                    onClick = { showRecordEggDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "ثبت تخم‌گذاری")
                }
            } else if (activeSubTab == FlocksSubTab.MORTALITY) {
                FloatingActionButton(
                    onClick = {
                        mortalityTargetFlockId = flocks.firstOrNull()?.id
                        showLogMortalityDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "ثبت تلفات جدید")
                }
            } else if (activeSubTab == FlocksSubTab.SALES_SLAUGHTER) {
                FloatingActionButton(
                    onClick = {
                        if (exitViewMode == "SLAUGHTERS") {
                            slaughterTargetFlockId = selectedExitFlockFilterId ?: flocks.firstOrNull()?.id
                            showSlaughterBirdsDialog = true
                        } else {
                            sellTargetFlockId = selectedExitFlockFilterId ?: flocks.firstOrNull()?.id
                            showSellBirdsDialog = true
                        }
                    },
                    containerColor = if (exitViewMode == "SLAUGHTERS") MaterialTheme.colorScheme.secondary else HealthGreen,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Icon(Icons.Default.Add, contentDescription = if (exitViewMode == "SLAUGHTERS") "ثبت کشتار" else "ثبت فروش")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (FlocksSubTab.entries[page]) {
                    FlocksSubTab.OVERVIEW -> {
                    if (flocks.isEmpty()) {
                        EmptyStateView(
                            imageRes = com.example.poultryapp.R.drawable.ic_farm_flocks,
                            title = "هیچ گله‌ای ثبت نشده است",
                            message = "برای شروع پایش مصرف دان و آب، برنامه‌های کاری و محاسبه خوراک، اولین گله خود را اضافه کنید.",
                            actionButtonText = "+ افزودن اولین گله",
                            onActionClick = { showAddDialog = true }
                        )
                    } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = FarmDimens.Space16,
                        end = FarmDimens.Space16,
                        top = FarmDimens.Space8,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
                ) {
                    items(
                        items = flocks,
                        key = { it.id },
                        contentType = { "flock_item" }
                    ) { flock ->
                        val req = flockRequirements[flock.id] ?: DailyNutritionEngine.calculateFlockRequirement(
                            flock,
                            season = currentSeason,
                            customBreeds = customBreeds,
                            tempCelsius = flock.customTempCelsius ?: farmAmbientTemp
                        )
                        val idealTempSpec = remember(flock.species, flock.breed, flock.ageDays) {
                            PoultryTemperatureEngine.getIdealTemperature(
                                species = flock.species,
                                breedName = flock.breed,
                                ageDays = flock.ageDays
                            )
                        }
                        val isExpanded = expandedFlockIds.contains(flock.id)
                        val chevronRotation by animateFloatAsState(
                            targetValue = if (isExpanded) 180f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "chevron_rotation"
                        )

                        Surface(
                            shape = RoundedCornerShape(FarmDimens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isExpanded) 1.5.dp else 1.dp,
                                if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(FarmDimens.Space14)
                            ) {
                                // Always-Visible Minimized Header: Clickable to expand/collapse
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            expandedFlockIds = if (isExpanded) {
                                                expandedFlockIds - flock.id
                                            } else {
                                                expandedFlockIds + flock.id
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Flock Salon Avatar Icon
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Image(
                                            painter = flockIconPainter,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(FarmDimens.Space10))

                                    Column(modifier = Modifier.weight(1f)) {
                                        // Line 1: Flock Name + Bird count badge
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            AutoResizeText(
                                                text = flock.name,
                                                targetFontSize = 15.sp,
                                                minFontSize = 11.sp,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(FarmDimens.Space8))
                                            FarmStatusBadge(
                                                text = "${toPersianDigits(flock.effectiveBirdCount)} قطعه",
                                                color = MaterialTheme.colorScheme.primary,
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(FarmDimens.Space4))

                                        // Line 2: Species & Age + Temperature badge
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            AutoResizeText(
                                                text = "${flock.species.titleFa} | سن: ${flock.ageDisplayFa}",
                                                targetFontSize = 12.sp,
                                                minFontSize = 9.sp,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(FarmDimens.Space6))

                                            val hasCustomTemp = flock.customTempCelsius != null
                                            val displayTemp = flock.customTempCelsius ?: farmAmbientTemp
                                            val isCold = displayTemp != null && displayTemp < idealTempSpec.minTolerableCelsius
                                            val isHot = displayTemp != null && displayTemp > idealTempSpec.maxTolerableCelsius
                                            val isIdeal = displayTemp != null && !isCold && !isHot

                                            val tempBadgeTextColor = when {
                                                isCold -> Color(0xFF00838F) // آبی یخی تیره
                                                isHot -> Color(0xFFC62828) // قرمز تیره
                                                isIdeal -> Color(0xFF2E7D32) // سبز تیره
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                            val tempBadgeBgColor = when {
                                                isCold -> Color(0xFFE0F7FA) // آبی یخی ملایم
                                                isHot -> Color(0xFFFFEBEE) // قرمز ملایم
                                                isIdeal -> Color(0xFFE8F5E9) // سبز ملایم
                                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            }
                                            val tempBadgeBorderColor = when {
                                                isCold -> Color(0xFF80DEEA)
                                                isHot -> Color(0xFFEF9A9A)
                                                isIdeal -> Color(0xFF81C784)
                                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            }
                                            val tempBadgeIcon = when {
                                                isCold -> Icons.Default.AcUnit // دانه برف
                                                isHot -> Icons.Default.Whatshot // شعله آتش
                                                else -> Icons.Default.Thermostat
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = tempBadgeBgColor,
                                                border = BorderStroke(1.dp, tempBadgeBorderColor)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = tempBadgeIcon,
                                                        contentDescription = null,
                                                        tint = tempBadgeTextColor,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = if (displayTemp != null) "${toPersianDigits(displayTemp)}°C" else "تنظیم‌نشده",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = tempBadgeTextColor,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(FarmDimens.Space6))

                                    // Expand / Collapse Chevron Button
                                    IconButton(
                                        onClick = {
                                            expandedFlockIds = if (isExpanded) {
                                                expandedFlockIds - flock.id
                                            } else {
                                                expandedFlockIds + flock.id
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "بستن" else "مشاهده جزئیات",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.rotate(chevronRotation)
                                        )
                                    }
                                }

                                // Expandable Full Details: Animated visibility with smooth slide & fade
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = FarmDimens.Space10),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )

                                        // Salon Temperature Setting & Scientific Standard Card
                                        val hasCustomFlockTemp = flock.customTempCelsius != null
                                        val effectiveFlockTemp = flock.customTempCelsius ?: farmAmbientTemp
                                        val isCold = effectiveFlockTemp != null && effectiveFlockTemp < idealTempSpec.minTolerableCelsius
                                        val isHot = effectiveFlockTemp != null && effectiveFlockTemp > idealTempSpec.maxTolerableCelsius
                                        val isIdeal = effectiveFlockTemp != null && !isCold && !isHot

                                        val statusThemeColor = when {
                                            isCold -> if (isDark) Color(0xFF38BDF8) else Color(0xFF00838F) // آبی یخی
                                            isHot -> if (isDark) Color(0xFFF87171) else Color(0xFFC62828) // قرمز
                                            isIdeal -> if (isDark) Color(0xFF34D399) else Color(0xFF2E7D32) // سبز زمردی
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        val statusBgColor = when {
                                            isCold -> if (isDark) Color(0xFF082F49) else Color(0xFFE0F7FA)
                                            isHot -> if (isDark) Color(0xFF2E1216) else Color(0xFFFFEBEE)
                                            isIdeal -> if (isDark) Color(0xFF0F2E20) else Color(0xFFE8F5E9)
                                            else -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                        val statusBorderColor = when {
                                            isCold -> if (isDark) Color(0xFF0284C7).copy(alpha = 0.4f) else Color(0xFF80DEEA)
                                            isHot -> if (isDark) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFFEF9A9A)
                                            isIdeal -> if (isDark) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFF81C784)
                                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        }
                                        val statusIcon = when {
                                            isCold -> Icons.Default.AcUnit
                                            isHot -> Icons.Default.Whatshot
                                            else -> Icons.Default.Thermostat
                                        }

                                        val thermalGuide = remember(flock.species, effectiveFlockTemp, idealTempSpec) {
                                            if (effectiveFlockTemp != null) {
                                                PoultryTemperatureEngine.getThermalStressGuide(
                                                    species = flock.species,
                                                    currentTemp = effectiveFlockTemp,
                                                    spec = idealTempSpec
                                                )
                                            } else null
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                // Header Row: Icon + Title + Set/Change Button
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
                                                            color = statusBgColor,
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = statusIcon,
                                                                    contentDescription = null,
                                                                    tint = statusThemeColor,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(
                                                                text = "پایش اقلیم و دمای سالن",
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = if (hasCustomFlockTemp) "دمای اختصاصی ثبت‌شده سالن" else "دمای هماهنگ با ایستگاه فارم",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    OutlinedButton(
                                                        onClick = { tempDialogFlock = flock },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Text(
                                                            text = if (hasCustomFlockTemp) "تغییر دما" else "تنظیم دما",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Two Clean Comparison Tiles Side by Side
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Current Temp Tile
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else statusBgColor.copy(alpha = 0.5f),
                                                        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f) else statusBorderColor.copy(alpha = 0.5f)),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                                            Text(
                                                                text = "دمای ثبت‌شده سالن",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = if (effectiveFlockTemp != null) "${toPersianDigits(effectiveFlockTemp)}°C" else "تنظیم‌نشده",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = statusThemeColor
                                                            )
                                                        }
                                                    }

                                                    // Standard Ideal Temp Tile
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                                        modifier = Modifier.weight(1.2f)
                                                    ) {
                                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                                            Text(
                                                                text = "استاندارد علمی این سن",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = toPersianDigits(idealTempSpec.formattedRange),
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDark) Color(0xFF34D399) else MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }

                                                // Status Evaluation Message
                                                if (effectiveFlockTemp != null) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = statusBgColor,
                                                        border = BorderStroke(1.dp, statusBorderColor)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = statusIcon,
                                                                contentDescription = null,
                                                                tint = statusThemeColor,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            val statusMsg = when {
                                                                isCold -> "دمای سالن ${toPersianDigits("%.1f".format(idealTempSpec.minTolerableCelsius - effectiveFlockTemp))}°C سردتر از حد استاندارد است"
                                                                isHot -> "استرس گرمایی! دمای سالن ${toPersianDigits("%.1f".format(effectiveFlockTemp - idealTempSpec.maxTolerableCelsius))}°C بالاتر از حد مجاز است"
                                                                effectiveFlockTemp in idealTempSpec.minComfortCelsius..idealTempSpec.maxComfortCelsius -> "دمای سالن در محدوده ایده‌آل و آسایش حرارتی پرنده است"
                                                                else -> "دمای سالن در محدوده امن، مجاز و بدون استرس برای پرنده است"
                                                            }
                                                            Text(
                                                                text = statusMsg,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = statusThemeColor
                                                            )
                                                        }
                                                    }
                                                }

                                                // Action Button when temperature is NOT ideal (Cold or Hot Stress)
                                                if (thermalGuide != null) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    val thermalCardBg = if (isDark) {
                                                        if (thermalGuide.isHeatStress) Color(0xFF2E1216) else Color(0xFF082F49)
                                                    } else {
                                                        if (thermalGuide.isHeatStress) Color(0xFFFFF3E0) else Color(0xFFE0F7FA)
                                                    }
                                                    val thermalCardBorder = if (isDark) {
                                                        if (thermalGuide.isHeatStress) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFF0284C7).copy(alpha = 0.4f)
                                                    } else {
                                                        if (thermalGuide.isHeatStress) Color(0xFFFFB74D) else Color(0xFF80DEEA)
                                                    }
                                                    val thermalAccent = if (isDark) {
                                                        if (thermalGuide.isHeatStress) Color(0xFFF87171) else Color(0xFF38BDF8)
                                                    } else {
                                                        if (thermalGuide.isHeatStress) Color(0xFFE65100) else Color(0xFF006064)
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = thermalCardBg,
                                                        border = BorderStroke(1.dp, thermalCardBorder),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { viewingThermalGuideForFlock = Pair(flock, thermalGuide) }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (thermalGuide.isHeatStress) Icons.Default.Whatshot else Icons.Default.AcUnit,
                                                                    contentDescription = null,
                                                                    tint = thermalAccent,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = if (thermalGuide.isHeatStress)
                                                                        "راهکارهای فوری مقابله با استرس گرمایی گله"
                                                                    else
                                                                        "راهکارهای فوری جبران سرما و حفظ دمای گله",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = thermalAccent
                                                                )
                                                            }
                                                            Text(
                                                                text = "مشاهده راهکارها ←",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (thermalGuide.isHeatStress) Color(0xFFE65100) else Color(0xFF006064)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Subgroups Details (if multi-breed/multi-age)
                                        if (flock.subGroups.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(FarmDimens.Space8))
                                            Surface(
                                                shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(FarmDimens.Space8)) {
                                                    Text(
                                                        text = "ترکیب نژادها و سنین گله:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    flock.subGroups.forEach { sg ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 1.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            AutoResizeText(
                                                                text = "• ${sg.breed} (${sg.species.titleFa})",
                                                                targetFontSize = 12.sp,
                                                                minFontSize = 9.sp,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )
                                                            Spacer(modifier = Modifier.width(FarmDimens.Space8))
                                                            Text(
                                                                text = "${toPersianDigits(sg.birdCount)} قطعه | سن: ${toPersianDigits(sg.ageDays)} روز",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(FarmDimens.Space10))

                                        val feedItem = warehouseItems.find { it.category == WarehouseCategory.FEED && it.assignedFlockIds.contains(flock.id) }
                                            ?: warehouseItems.find { it.category == WarehouseCategory.FEED }
                                        val dailyFeedCost = if (feedItem != null && feedItem.pricePerUnit > 0) {
                                            (req.totalFeedGrams / 1000.0) * feedItem.pricePerUnit
                                        } else 0.0

                                        // Daily Requirements Grid (2 balanced columns: Feed & Water)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                                        ) {
                                            // Feed requirement
                                            Surface(
                                                shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                                color = if (isDark) Color(0xFF23180D) else FeedAmberContainer.copy(alpha = 0.35f),
                                                border = BorderStroke(1.dp, if (isDark) Color(0xFFF59E0B).copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(FarmDimens.Space8),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Grain,
                                                        contentDescription = null,
                                                        tint = if (isDark) Color(0xFFFBBF24) else FeedAmber,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                                    Column {
                                                        Text(
                                                            text = "نیاز روزانه دان",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = req.formattedTotalFeed,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                        Text(
                                                            text = if (userRole != UserRole.WORKER && dailyFeedCost > 0) "هزینه: ${DailyNutritionEngine.formatTomans(dailyFeedCost)}" else "پایش فعال",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isDark) Color(0xFFFCD34D) else (if (userRole != UserRole.WORKER && dailyFeedCost > 0) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant),
                                                            fontWeight = if (userRole != UserRole.WORKER && dailyFeedCost > 0) FontWeight.SemiBold else FontWeight.Normal,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            }

                                            // Water requirement
                                            Surface(
                                                shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                                color = if (isDark) Color(0xFF0B1E2E) else WaterBlueContainer.copy(alpha = 0.35f),
                                                border = BorderStroke(1.dp, if (isDark) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF0284C7).copy(alpha = 0.2f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(FarmDimens.Space8),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.WaterDrop,
                                                        contentDescription = null,
                                                        tint = if (isDark) Color(0xFF38BDF8) else WaterBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                                    Column {
                                                        Text(
                                                            text = "نیاز روزانه آب",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = req.formattedTotalWater,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                        Text(
                                                            text = "سهم هر پرنده: ${req.formattedWaterPerBird}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isDark) Color(0xFF7DD3FC) else WaterBlueText,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(FarmDimens.Space8))

                                        // Notes if any
                                        if (!flock.notes.isNullOrBlank()) {
                                            Text(
                                                text = "یادداشت: ${flock.notes}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = FarmDimens.Space6)
                                            )
                                        }

                                        // Footer Tier 1: Tracking Status Badges & Edit/Delete Icons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6)) {
                                                FarmStatusBadge(
                                                    text = if (flock.isFeedEnabled) "پایش دان: فعال" else "دان: غیرفعال",
                                                    color = if (flock.isFeedEnabled) HealthGreen else MaterialTheme.colorScheme.outline,
                                                    containerColor = if (flock.isFeedEnabled) HealthGreenContainer else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                FarmStatusBadge(
                                                    text = if (flock.isWaterEnabled) "پایش آب: فعال" else "آب: غیرفعال",
                                                    color = if (flock.isWaterEnabled) WaterBlue else MaterialTheme.colorScheme.outline,
                                                    containerColor = if (flock.isWaterEnabled) WaterBlueContainer else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { editingFlock = flock },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "ویرایش",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { deletingFlockId = flock.id },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "حذف",
                                                        tint = HealthRed,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(FarmDimens.Space8))

                                        // Quick Flock Exit & Transfer Actions (Wrapped safely via FlowRow)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    sellTargetFlockId = flock.id
                                                    showSellBirdsDialog = true
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HealthGreen),
                                                border = BorderStroke(1.dp, HealthGreen.copy(alpha = 0.6f)),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("💰 فروش", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    slaughterTargetFlockId = flock.id
                                                    showSlaughterBirdsDialog = true
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("🔪 کشتار", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    mortalityTargetFlockId = flock.id
                                                    showLogMortalityDialog = true
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("📉 تلفات", style = MaterialTheme.typography.labelSmall)
                                            }
                                            if (flocks.size > 1) {
                                                OutlinedButton(
                                                    onClick = { transferringSourceFlock = flock },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("انتقال", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(FarmDimens.Space8))

                                        // Footer Tier 2: Full-Width Primary Action (Monitor & Charge Jireh)
                                        FilledTonalButton(
                                            onClick = {
                                                onSelectFlock?.invoke(flock.id)
                                                onSelectFlockForCharge(flock)
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(FlocksSubTab.FEED_WATER.ordinal)
                                                }
                                            },
                                            shape = RoundedCornerShape(FarmDimens.ButtonRadius),
                                            contentPadding = PaddingValues(horizontal = FarmDimens.Space12, vertical = 6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Default.Grain,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "پایش و شارژ جیره (دان و آب)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom padding for FAB and nav bar
                    item {
                        Spacer(modifier = Modifier.height(FarmDimens.Space40))
                    }
                }
            }
        }
        FlocksSubTab.EGG_PRODUCTION -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = FarmDimens.Space16,
                    end = FarmDimens.Space16,
                    top = FarmDimens.Space8,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
            ) {
                // Top KPI summary
                item {
                    val warehouseEggStock = warehouseItems.filter { it.category == WarehouseCategory.EGG }.sumOf { it.currentStock }
                    val totalHealthy = eggProductions.sumOf { it.healthyCount }
                    val totalLoss = eggProductions.sumOf { it.brokenCount + it.dirtyCount }
                    val avgRate = if (eggProductions.isNotEmpty()) eggProductions.map { it.layingRatePercent }.average() else 0.0

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(FarmDimens.CardRadius),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                    Text("مجموع تخم‌مرغ سالم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${toPersianDigits(totalHealthy)} عدد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(FarmDimens.CardRadius),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                    Text("موجودی فعلی در انبار", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${toPersianDigits(warehouseEggStock.toInt())} عدد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(FarmDimens.CardRadius),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                    Text("میانگین راندمان تولید", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${toPersianDigits(String.format(java.util.Locale.US, "%.1f", avgRate))}٪", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(FarmDimens.CardRadius),
                                color = if (isDark) HealthRedContainerDark else HealthRedContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isDark) HealthRedDark.copy(alpha = 0.35f) else HealthRed.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                    Text(
                                        text = "ضایعات (شکسته/کثیف)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) HealthRedTextDark else HealthRedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${toPersianDigits(totalLoss)} عدد",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) HealthRedDark else HealthRed
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Button Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(FarmDimens.CardRadius),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRecordEggDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = FarmDimens.Space16, vertical = FarmDimens.Space12),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(FarmDimens.Space8))
                                Text(
                                    text = "ثبت آمار تخم‌گذاری روزانه جدید",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "+ ثبت",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                if (eggProductions.isEmpty()) {
                    item {
                        EmptyStateView(
                            imageRes = com.example.poultryapp.R.drawable.ic_farm_egg,
                            title = "هیچ آماری برای تخم‌گذاری ثبت نشده است",
                            message = "ثبت روزانه تخم‌مرغ‌ها به شما امکان می‌دهد راندمان تولید هر گله، ضایعات و موجودی انبار را با دقت کنترل کنید.",
                            actionButtonText = "+ ثبت اولین تخم‌گذاری",
                            onActionClick = { showRecordEggDialog = true }
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "سوابق ثبت تخم‌گذاری (${toPersianDigits(eggProductions.size)} روز)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(items = eggProductions, key = { "egg_${it.id}" }) { prod ->
                        Surface(
                            shape = RoundedCornerShape(FarmDimens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = prod.flockName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = prod.dateJalaliStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { deletingEggProductionId = prod.id },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = HealthRed, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Healthy
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = HealthGreenContainer,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("سالم", style = MaterialTheme.typography.labelSmall, color = HealthGreenText)
                                            Text(toPersianDigits(prod.healthyCount), fontWeight = FontWeight.Bold, color = HealthGreenText)
                                        }
                                    }

                                    // Broken
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = HealthRedContainer,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("شکسته", style = MaterialTheme.typography.labelSmall, color = HealthRedText)
                                            Text(toPersianDigits(prod.brokenCount), fontWeight = FontWeight.Bold, color = HealthRedText)
                                        }
                                    }

                                    // Dirty
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("کثیف", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(toPersianDigits(prod.dirtyCount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    // Double Yolk
                                    if (prod.doubleYolkCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("دوزرده", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                Text(toPersianDigits(prod.doubleYolkCount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            }
                                        }
                                    }

                                    // Laying Rate %
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("راندمان", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                "${toPersianDigits(String.format(java.util.Locale.US, "%.1f", prod.layingRatePercent))}٪",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                if (!prod.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "یادداشت: ${prod.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Bird Transfers Section
                if (birdTransfers.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "سوابق جابجایی پرندگان بین سالن‌ها (${toPersianDigits(birdTransfers.size)} مورد)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(items = birdTransfers, key = { "transfer_${it.id}" }) { transfer ->
                        Surface(
                            shape = RoundedCornerShape(FarmDimens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(FarmDimens.Space12),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Sync,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(FarmDimens.Space10))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "انتقال ${toPersianDigits(transfer.birdCount)} قطعه از «${transfer.fromFlockName}» به «${transfer.toFlockName}»",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تاریخ: ${transfer.dateJalaliStr}" + if (!transfer.notes.isNullOrBlank()) " | ${transfer.notes}" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(FarmDimens.Space40))
                }
            }
        }
        FlocksSubTab.MORTALITY -> {
            val todayJalali = remember { com.example.poultryapp.engine.JalaliDateHelper.getJalaliDate().toStandardString() }
            val totalMortalityCount = remember(bioMortalityLogs) { bioMortalityLogs.sumOf { it.count } }
            val todayMortalityCount = remember(bioMortalityLogs, todayJalali) {
                bioMortalityLogs.filter { it.dateJalaliStr == todayJalali }.sumOf { it.count }
            }
            val overallMortalityRate = remember(totalBirds, bioMortalityLogs, birdSales, birdSlaughters) {
                com.example.poultryapp.data.local.FlockExitAnalytics.calculateCumulativeMortalityRatePercent(
                    currentLiveBirds = totalBirds,
                    mortalityLogs = bioMortalityLogs,
                    birdSales = birdSales,
                    birdSlaughters = birdSlaughters
                )
            }
            val filteredMortalityLogs = remember(bioMortalityLogs, selectedMortalityFlockFilterId) {
                if (selectedMortalityFlockFilterId == null) bioMortalityLogs
                else bioMortalityLogs.filter { it.flockId == selectedMortalityFlockFilterId }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = FarmDimens.Space16,
                    end = FarmDimens.Space16,
                    top = FarmDimens.Space8,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
            ) {
                // Top KPI Summary Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(FarmDimens.CardRadius),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                    Text("کل تلفات زیستی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${toPersianDigits(totalMortalityCount)} قطعه",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(FarmDimens.CardRadius),
                                color = if (todayMortalityCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (todayMortalityCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                    Text("تلفات ثبت‌شده امروز", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${toPersianDigits(todayMortalityCount)} قطعه",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (todayMortalityCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(FarmDimens.CardRadius),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(FarmDimens.Space12),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("نرخ تجمعی تلفات واقعی فارم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${toPersianDigits(String.format(java.util.Locale.US, "%.2f", overallMortalityRate))}٪",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (overallMortalityRate > 5.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "فروش و کشتار در این شاخص لحاظ نمی‌شوند",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        mortalityTargetFlockId = flocks.firstOrNull()?.id
                                        showLogMortalityDialog = true
                                    },
                                    enabled = flocks.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ثبت تلفات جدید", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Flock filter chips (if more than 1 flock)
                if (flocks.size > 1) {
                    item {
                        Column {
                            Text("فیلتر بر اساس سالن/گله:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item(key = "mort_filter_all") {
                                    FilterChip(
                                        selected = selectedMortalityFlockFilterId == null,
                                        onClick = { selectedMortalityFlockFilterId = null },
                                        label = { Text("همه سالن‌ها") }
                                    )
                                }
                                items(items = flocks, key = { "mort_filter_${it.id}" }) { f ->
                                    FilterChip(
                                        selected = selectedMortalityFlockFilterId == f.id,
                                        onClick = { selectedMortalityFlockFilterId = f.id },
                                        label = { Text(f.name) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Mortality records list
                if (filteredMortalityLogs.isEmpty()) {
                    item {
                        EmptyStateView(
                            imageRes = com.example.poultryapp.R.drawable.ic_farm_health,
                            title = "هیچ تلفاتی ثبت نشده است",
                            message = "خوشبختانه تاکنون هیچ تلفات زیستی در این بخش ثبت نشده است. فروش و کشتار پرندگان در تب «فروش و کشتار» ثبت می‌شود.",
                            actionButtonText = "+ ثبت تلفات جدید",
                            onActionClick = {
                                mortalityTargetFlockId = flocks.firstOrNull()?.id
                                showLogMortalityDialog = true
                            }
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "سوابق ثبت‌شده تلفات زیستی:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(
                        items = filteredMortalityLogs,
                        key = { "bio_mort_${it.id}" }
                    ) { log ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = "${toPersianDigits(log.count)} قطعه",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = log.flockName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = toPersianDigits(log.dateJalaliStr),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = log.reason.titleFa,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { deletingMortalityId = log.id },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف رکورد",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (!log.diagnosisHint.isNullOrBlank() || !log.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val fullNote = listOfNotNull(
                                        log.diagnosisHint?.takeIf { it.isNotBlank() },
                                        log.notes?.takeIf { it.isNotBlank() }
                                    ).joinToString(" | ")
                                    Text(
                                        text = "توضیحات: $fullNote",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(FarmDimens.Space40))
                }
            }
        }
        FlocksSubTab.SALES_SLAUGHTER -> {
            val isWorker = userRole == UserRole.WORKER
            val filteredSales = remember(birdSales, selectedExitFlockFilterId) {
                if (selectedExitFlockFilterId == null) birdSales
                else birdSales.filter { it.flockId == selectedExitFlockFilterId }
            }
            val filteredSlaughters = remember(birdSlaughters, selectedExitFlockFilterId) {
                if (selectedExitFlockFilterId == null) birdSlaughters
                else birdSlaughters.filter { it.flockId == selectedExitFlockFilterId }
            }
            val totalSoldBirds = remember(filteredSales) { filteredSales.sumOf { it.birdCount } }
            val totalSaleRevenue = remember(filteredSales) { filteredSales.sumOf { it.totalRevenueTomans } }
            val totalSlaughteredBirds = remember(filteredSlaughters) { filteredSlaughters.sumOf { it.birdCount } }
            val avgCarcassYield = remember(filteredSlaughters) {
                filteredSlaughters.mapNotNull { it.carcassYieldPercent }.takeIf { it.isNotEmpty() }?.average()
            }

            val timelineItems = remember(filteredSales, filteredSlaughters, exitViewMode) {
                val combined = mutableListOf<BirdExitUiItem>()
                if (exitViewMode != "SLAUGHTERS") {
                    filteredSales.forEach { combined.add(BirdExitUiItem.Sale(it)) }
                }
                if (exitViewMode != "SALES") {
                    filteredSlaughters.forEach { combined.add(BirdExitUiItem.Slaughter(it)) }
                }
                combined.sortedByDescending { it.timestampMillis }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = FarmDimens.Space16,
                    end = FarmDimens.Space16,
                    top = FarmDimens.Space8,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
            ) {
                // Separate KPI Summary Cards: Sale (Financial) vs Slaughter (Technical, NO money)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sale KPI Card (Green)
                        Surface(
                            shape = RoundedCornerShape(FarmDimens.CardRadius),
                            color = if (isDark) HealthGreenContainerDark else HealthGreenContainer,
                            border = BorderStroke(1.dp, (if (isDark) HealthGreenDark else HealthGreen).copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = if (isDark) HealthGreenDark else HealthGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "فروش پرنده",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) HealthGreenTextDark else HealthGreenText
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${toPersianDigits(totalSoldBirds)} قطعه",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) HealthGreenDark else HealthGreen
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (!isWorker && totalSaleRevenue > 0) {
                                    Text(
                                        text = "${toPersianDigits(String.format(java.util.Locale.US, "%,.0f", totalSaleRevenue))} تومان",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) HealthGreenTextDark else HealthGreenText
                                    )
                                } else {
                                    Text(
                                        text = if (isWorker) "مبالغ مالی مخفی (کارگر)" else "${toPersianDigits(filteredSales.size)} نوبت فروش",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Slaughter KPI Card (Amber / Secondary - Strictly NON-financial)
                        Surface(
                            shape = RoundedCornerShape(FarmDimens.CardRadius),
                            color = if (isDark) FeedAmberContainerDark else FeedAmberContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, (if (isDark) FeedAmberDark else FeedAmber).copy(alpha = 0.45f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ContentCut,
                                        contentDescription = null,
                                        tint = if (isDark) FeedAmberDark else FeedAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "کشتار پرنده",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) FeedAmberDark else FeedAmberText
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${toPersianDigits(totalSlaughteredBirds)} قطعه",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) FeedAmberDark else FeedAmber
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (avgCarcassYield != null) {
                                        "میانگین راندمان لاشه: ${toPersianDigits(String.format(java.util.Locale.US, "%.1f", avgCarcassYield))}٪"
                                    } else {
                                        "${toPersianDigits(filteredSlaughters.size)} نوبت کشتار (غیردرآمدی)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) FeedAmberDark.copy(alpha = 0.9f) else FeedAmberText
                                )
                            }
                        }
                    }
                }

                // Quick Actions Row: Separate buttons for Sell & Slaughter
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                sellTargetFlockId = selectedExitFlockFilterId ?: flocks.firstOrNull()?.id
                                showSellBirdsDialog = true
                            },
                            enabled = flocks.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = HealthGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ثبت فروش پرنده", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = {
                                slaughterTargetFlockId = selectedExitFlockFilterId ?: flocks.firstOrNull()?.id
                                showSlaughterBirdsDialog = true
                            },
                            enabled = flocks.isNotEmpty(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isDark) FeedAmberContainerDark else FeedAmberContainer,
                                contentColor = if (isDark) FeedAmberDark else FeedAmberText
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ثبت کشتار پرنده", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Mode filter chips (All / Sales / Slaughters)
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "exit_mode_all") {
                            FilterChip(
                                selected = exitViewMode == "ALL",
                                onClick = { exitViewMode = "ALL" },
                                label = { Text("همه (${toPersianDigits(filteredSales.size + filteredSlaughters.size)})") }
                            )
                        }
                        item(key = "exit_mode_sales") {
                            FilterChip(
                                selected = exitViewMode == "SALES",
                                onClick = { exitViewMode = "SALES" },
                                label = { Text("💰 فروش‌ها (${toPersianDigits(filteredSales.size)})") }
                            )
                        }
                        item(key = "exit_mode_slaughters") {
                            FilterChip(
                                selected = exitViewMode == "SLAUGHTERS",
                                onClick = { exitViewMode = "SLAUGHTERS" },
                                label = { Text("🔪 کشتارها (${toPersianDigits(filteredSlaughters.size)})") }
                            )
                        }
                    }
                }

                // Flock filter chips
                if (flocks.size > 1) {
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item(key = "exit_flock_all") {
                                FilterChip(
                                    selected = selectedExitFlockFilterId == null,
                                    onClick = { selectedExitFlockFilterId = null },
                                    label = { Text("همه سالن‌ها") }
                                )
                            }
                            items(items = flocks, key = { "exit_flock_${it.id}" }) { f ->
                                FilterChip(
                                    selected = selectedExitFlockFilterId == f.id,
                                    onClick = { selectedExitFlockFilterId = f.id },
                                    label = { Text(f.name) }
                                )
                            }
                        }
                    }
                }

                if (timelineItems.isEmpty()) {
                    item {
                        EmptyStateView(
                            imageRes = com.example.poultryapp.R.drawable.ic_farm_flocks,
                            title = "هنوز فروش یا کشتاری ثبت نشده است",
                            message = "فروش پرندگان (سودآور با امکان ثبت خودکار درآمد) و کشتار پرندگان (همراه با سن پرنده، تاریخ و راندمان لاشه) به صورت کاملاً جداگانه از تلفات در اینجا ثبت می‌شوند.",
                            actionButtonText = "+ ثبت فروش پرنده",
                            onActionClick = {
                                if (flocks.isNotEmpty()) {
                                    sellTargetFlockId = flocks.firstOrNull()?.id
                                    showSellBirdsDialog = true
                                }
                            }
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "سوابق فروش و کشتار (${toPersianDigits(timelineItems.size)} مورد):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(items = timelineItems, key = { it.key }) { item ->
                        when (item) {
                            is BirdExitUiItem.Sale -> {
                                val sale = item.log
                                val saleAccent = if (isDark) HealthGreenDark else HealthGreen
                                val saleBadgeBg = if (isDark) HealthGreenContainerDark else HealthGreenContainer
                                val saleBadgeText = if (isDark) HealthGreenTextDark else HealthGreenText
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, saleAccent.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = saleBadgeBg,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                ) {
                                                    Text(
                                                        text = "${toPersianDigits(sale.birdCount)} قطعه",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = saleAccent,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = sale.flockName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "تاریخ فروش: ${toPersianDigits(sale.dateJalaliStr)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = saleBadgeBg
                                                ) {
                                                    Text(
                                                        text = "فروش پرنده",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = saleBadgeText,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { deletingBirdSaleId = sale.id },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "حذف رکورد فروش",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        val detailChips = buildList {
                                            if (sale.liveWeightKg != null && sale.liveWeightKg > 0.0) {
                                                val wStr = if (sale.liveWeightKg % 1.0 == 0.0) sale.liveWeightKg.toInt().toString() else String.format(java.util.Locale.US, "%.1f", sale.liveWeightKg)
                                                add("وزن زنده: ${toPersianDigits(wStr)} کیلوگرم")
                                            }
                                            if (!sale.buyerName.isNullOrBlank()) {
                                                add("خریدار: ${sale.buyerName}")
                                            }
                                            if (!isWorker && sale.totalRevenueTomans > 0.0) {
                                                add("مبلغ کل: ${toPersianDigits(String.format(java.util.Locale.US, "%,.0f", sale.totalRevenueTomans))} تومان")
                                            }
                                            if (!isWorker && sale.unitPriceTomans != null && sale.unitPriceTomans > 0.0) {
                                                val unitLabel = if (sale.liveWeightKg != null && sale.liveWeightKg > 0.0) "قیمت هر کیلو" else "قیمت هر قطعه"
                                                add("$unitLabel: ${toPersianDigits(String.format(java.util.Locale.US, "%,.0f", sale.unitPriceTomans))} تومان")
                                            }
                                            if (!isWorker && sale.linkedTransactionId != null) {
                                                add("سند درآمد ثبت‌شده")
                                            }
                                        }
                                        if (detailChips.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = detailChips.joinToString("  •  "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = saleBadgeText,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (!sale.notes.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "توضیحات: ${sale.notes}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            is BirdExitUiItem.Slaughter -> {
                                val slaughter = item.log
                                val slAccent = if (isDark) FeedAmberDark else FeedAmber
                                val slBadgeBg = if (isDark) FeedAmberContainerDark else FeedAmberContainer
                                val slBadgeText = if (isDark) FeedAmberDark else FeedAmberText
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, slAccent.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = slBadgeBg,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                ) {
                                                    Text(
                                                        text = "${toPersianDigits(slaughter.birdCount)} قطعه",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = slAccent,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = slaughter.flockName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "تاریخ کشتار: ${toPersianDigits(slaughter.dateJalaliStr)}  •  سن پرنده: ${toPersianDigits(slaughter.birdAgeDays)} روز",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = slBadgeBg
                                                ) {
                                                    Text(
                                                        text = "کشتار پرنده",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = slBadgeText,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { deletingBirdSlaughterId = slaughter.id },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "حذف رکورد کشتار",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        val techChips = buildList {
                                            if (slaughter.liveWeightKg != null && slaughter.liveWeightKg > 0.0) {
                                                val w = if (slaughter.liveWeightKg % 1.0 == 0.0) slaughter.liveWeightKg.toInt().toString() else String.format(java.util.Locale.US, "%.1f", slaughter.liveWeightKg)
                                                add("وزن زنده: ${toPersianDigits(w)} کیلوگرم")
                                            }
                                            if (slaughter.carcassWeightKg != null && slaughter.carcassWeightKg > 0.0) {
                                                val cw = if (slaughter.carcassWeightKg % 1.0 == 0.0) slaughter.carcassWeightKg.toInt().toString() else String.format(java.util.Locale.US, "%.1f", slaughter.carcassWeightKg)
                                                add("وزن لاشه: ${toPersianDigits(cw)} کیلوگرم")
                                            }
                                            val yieldPct = slaughter.carcassYieldPercent
                                            if (yieldPct != null) {
                                                add("راندمان لاشه: ${toPersianDigits(String.format(java.util.Locale.US, "%.1f", yieldPct))}٪")
                                            }
                                        }
                                        if (techChips.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = techChips.joinToString("  •  "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = slBadgeText,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (!slaughter.notes.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "توضیحات/کشتارگاه: ${slaughter.notes}",
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

                item {
                    Spacer(modifier = Modifier.height(FarmDimens.Space40))
                }
            }
        }
        FlocksSubTab.FEED_WATER -> {
            if (settings != null) {
                FeedWaterScreen(
                    flocks = flocks,
                    selectedFlockId = selectedFlockId,
                    onSelectFlock = { onSelectFlock?.invoke(it) },
                    latestFeedCharge = latestFeedCharge,
                    latestWaterCharge = latestWaterCharge,
                    todayFeedTotal = todayFeedTotal,
                    todayWaterTotal = todayWaterTotal,
                    chargeHistory = chargeHistory,
                    customBreeds = customBreeds,
                    settings = settings,
                    weatherResult = weatherResult,
                    isWeatherLoading = isWeatherLoading,
                    onRefreshWeather = { onRefreshWeather?.invoke() },
                    onSelectCity = { prov, city -> onSelectCity?.invoke(prov, city) },
                    onGpsDetect = { onGpsDetect?.invoke() },
                    onChargeSubmit = { fId, iType, amt, notes -> onChargeSubmit?.invoke(fId, iType, amt, notes) },
                    onDeleteCharge = onDeleteCharge,
                    onUpdateFlockCustomTemp = onUpdateFlockCustomTemp,
                    onToggleFeedTracking = onToggleFeedTracking,
                    onToggleWaterTracking = onToggleWaterTracking,
                    onNavigateToFlocks = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(FlocksSubTab.OVERVIEW.ordinal)
                        }
                    },
                    onOpenDrawer = onOpenDrawer,
                    showTopBar = false
                )
            }
        }
        FlocksSubTab.CALENDAR -> {
            CalendarScreen(
                schedules = schedules,
                flocks = flocks,
                onAddSchedule = { onAddSchedule?.invoke(it) },
                onEditSchedule = { onEditSchedule?.invoke(it) },
                onDeleteSchedule = { onDeleteSchedule?.invoke(it) },
                onToggleSchedule = { id, enabled -> onToggleSchedule?.invoke(id, enabled) },
                onOpenDrawer = onOpenDrawer,
                showTopBar = false
            )
        }
    }
}
}
}

    // Add Flock Dialog
    if (showAddDialog) {
        FlockFormDialog(
            flock = null,
            existingFlocks = flocks,
            customBreeds = customBreeds,
            onDismiss = { showAddDialog = false },
            onSave = { name, species, breed, ageDays, count, notes, subGroups, isFeedEnabled, isWaterEnabled, customTempCelsius ->
                onAddFlock(name, species, breed, ageDays, count, notes, subGroups, isFeedEnabled, isWaterEnabled, customTempCelsius)
                showAddDialog = false
            }
        )
    }

    // Edit Flock Dialog
    if (editingFlock != null) {
        FlockFormDialog(
            flock = editingFlock,
            existingFlocks = flocks,
            customBreeds = customBreeds,
            onDismiss = { editingFlock = null },
            onSave = { name, species, breed, ageDays, count, notes, subGroups, isFeedEnabled, isWaterEnabled, customTempCelsius ->
                val primaryBirthMillis = subGroups.firstOrNull()?.birthEpochMillis ?: editingFlock!!.birthEpochMillis
                onEditFlock(
                    editingFlock!!.copy(
                        name = name,
                        species = species,
                        breed = breed,
                        birthEpochMillis = primaryBirthMillis,
                        birdCount = count,
                        notes = notes,
                        subGroups = subGroups,
                        isFeedEnabled = isFeedEnabled,
                        isWaterEnabled = isWaterEnabled,
                        customTempCelsius = customTempCelsius
                    )
                )
                editingFlock = null
            }
        )
    }

    // Custom Temperature Dialog for Salon
    if (tempDialogFlock != null) {
        val targetFlock = tempDialogFlock!!
        FlockTemperatureDialog(
            flockName = targetFlock.name,
            currentCustomTemp = targetFlock.customTempCelsius,
            farmAmbientTemp = farmAmbientTemp,
            onDismiss = { tempDialogFlock = null },
            onConfirm = { newTemp ->
                if (onUpdateFlockCustomTemp != null) {
                    onUpdateFlockCustomTemp(targetFlock.id, newTemp)
                } else {
                    onEditFlock(targetFlock.copy(customTempCelsius = newTemp))
                }
                tempDialogFlock = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingFlockId != null) {
        AlertDialog(
            onDismissRequest = { deletingFlockId = null },
            title = {
                Text(
                    text = "حذف گله",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از حذف این گله اطمینان دارید؟ تمامی سوابق شارژ دان و آب این گله نیز حذف خواهند شد.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFlock(deletingFlockId!!)
                        deletingFlockId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("حذف قطعی", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deletingFlockId = null },
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(FarmDimens.DialogRadius)
        )
    }

    // Bird Transfer Dialog
    if (transferringSourceFlock != null) {
        TransferBirdsDialog(
            flocks = flocks,
            sourceFlockId = transferringSourceFlock?.id,
            onDismiss = { transferringSourceFlock = null },
            onTransfer = { fromId, toId, count, notes ->
                onTransferBirds(fromId, toId, count, notes)
                transferringSourceFlock = null
            }
        )
    }

    // Record Egg Dialog
    if (showRecordEggDialog) {
        RecordEggDialog(
            flocks = flocks,
            onDismiss = { showRecordEggDialog = false },
            onSave = { flockIds, healthy, broken, dirty, doubleYolk, dateJalaliStr, notes ->
                onLogEggProduction(flockIds, healthy, broken, dirty, doubleYolk, dateJalaliStr, notes)
                showRecordEggDialog = false
            },
            onSaveDetailed = { flockIds, fertile, tableEgg, broken, dirty, doubleYolk, trayCount, trayGross, netAvg, dateStr, notes ->
                if (onLogDetailedEggProduction != null) {
                    onLogDetailedEggProduction(flockIds, fertile, tableEgg, broken, dirty, doubleYolk, trayCount, trayGross, netAvg, dateStr, notes)
                } else {
                    onLogEggProduction(flockIds, fertile + tableEgg, broken, dirty, doubleYolk, dateStr, notes)
                }
                showRecordEggDialog = false
            }
        )
    }

    // Log Mortality Dialog
    if (showLogMortalityDialog) {
        LogMortalityDialog(
            flocks = flocks,
            initialFlockId = mortalityTargetFlockId,
            onDismiss = { showLogMortalityDialog = false },
            onSave = { flockId, count, reason, diagHint, dateStr, notes ->
                onLogMortality?.invoke(flockId, count, reason, diagHint, dateStr, notes)
                showLogMortalityDialog = false
            }
        )
    }

    // Delete Egg Production Dialog
    if (deletingEggProductionId != null) {
        AlertDialog(
            onDismissRequest = { deletingEggProductionId = null },
            title = {
                Text(
                    text = "حذف رکورد تخم‌گذاری",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از حذف این رکورد تخم‌گذاری اطمینان دارید؟",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEggProduction(deletingEggProductionId!!)
                        deletingEggProductionId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("حذف رکورد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deletingEggProductionId = null },
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(FarmDimens.DialogRadius)
        )
    }

    // Delete Mortality Dialog
    if (deletingMortalityId != null) {
        AlertDialog(
            onDismissRequest = { deletingMortalityId = null },
            title = {
                Text(
                    text = "حذف رکورد تلفات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از حذف این رکورد تلفات اطمینان دارید؟ با حذف این رکورد، تعداد پرندگان کسر شده به صورت خودکار به آمار زنده گله بازگردانده می‌شود.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletingMortalityId?.let { id ->
                            onDeleteMortality?.invoke(id)
                        }
                        deletingMortalityId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("حذف و بازگردانی پرندگان", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deletingMortalityId = null },
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(FarmDimens.DialogRadius)
        )
    }

    // Sell Birds Dialog
    if (showSellBirdsDialog) {
        SellBirdsDialog(
            flocks = flocks,
            initialFlockId = sellTargetFlockId,
            isWorkerMode = (userRole == UserRole.WORKER),
            onDismiss = { showSellBirdsDialog = false },
            onSave = { flockId, count, liveWeightKg, unitPriceTomans, totalRevenueTomans, buyerName, dateStr, notes, createFinanceTx ->
                onSellBirds?.invoke(
                    flockId,
                    count,
                    liveWeightKg,
                    unitPriceTomans,
                    totalRevenueTomans,
                    buyerName,
                    dateStr,
                    notes,
                    createFinanceTx
                )
                showSellBirdsDialog = false
            }
        )
    }

    // Slaughter Birds Dialog
    if (showSlaughterBirdsDialog) {
        SlaughterBirdsDialog(
            flocks = flocks,
            initialFlockId = slaughterTargetFlockId,
            onDismiss = { showSlaughterBirdsDialog = false },
            onSave = { flockId, count, birdAgeDays, liveWeightKg, carcassWeightKg, dateStr, notes ->
                onSlaughterBirds?.invoke(
                    flockId,
                    count,
                    birdAgeDays,
                    liveWeightKg,
                    carcassWeightKg,
                    dateStr,
                    notes
                )
                showSlaughterBirdsDialog = false
            }
        )
    }

    // Delete Bird Sale Confirmation Dialog
    if (deletingBirdSaleId != null) {
        AlertDialog(
            onDismissRequest = { deletingBirdSaleId = null },
            title = {
                Text(
                    text = "حذف رکورد فروش پرنده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از حذف این رکورد فروش اطمینان دارید؟ پرندگان فروخته‌شده به آمار زنده گله بازگردانده می‌شوند و در صورت وجود سند درآمد متصل، آن تراکنش مالی نیز حذف خواهد شد.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletingBirdSaleId?.let { id ->
                            onDeleteBirdSale?.invoke(id)
                        }
                        deletingBirdSaleId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("حذف و بازگردانی", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deletingBirdSaleId = null },
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(FarmDimens.DialogRadius)
        )
    }

    // Delete Bird Slaughter Confirmation Dialog
    if (deletingBirdSlaughterId != null) {
        AlertDialog(
            onDismissRequest = { deletingBirdSlaughterId = null },
            title = {
                Text(
                    text = "حذف رکورد کشتار پرنده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از حذف این رکورد کشتار اطمینان دارید؟ با حذف این رکورد، تعداد پرندگان به آمار زنده گله بازگردانده می‌شود.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletingBirdSlaughterId?.let { id ->
                            onDeleteBirdSlaughter?.invoke(id)
                        }
                        deletingBirdSlaughterId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("حذف و بازگردانی پرندگان", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deletingBirdSlaughterId = null },
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(FarmDimens.DialogRadius)
        )
    }

    // Thermal Stress Management Solutions Dialog
    if (viewingThermalGuideForFlock != null) {
        val (targetFlock, guide) = viewingThermalGuideForFlock!!
        ThermalStressGuideDialog(
            flock = targetFlock,
            guide = guide,
            onDismiss = { viewingThermalGuideForFlock = null }
        )
    }
}

private sealed interface BirdExitUiItem {
    val key: String
    val timestampMillis: Long
    val dateJalaliStr: String

    data class Sale(val log: com.example.poultryapp.data.local.BirdSaleLog) : BirdExitUiItem {
        override val key: String = "sale_${log.id}"
        override val timestampMillis: Long = log.timestampMillis
        override val dateJalaliStr: String = log.dateJalaliStr
    }

    data class Slaughter(val log: com.example.poultryapp.data.local.BirdSlaughterLog) : BirdExitUiItem {
        override val key: String = "slaughter_${log.id}"
        override val timestampMillis: Long = log.timestampMillis
        override val dateJalaliStr: String = log.dateJalaliStr
    }
}

/**
 * دیالوگ راهکارهای فوری و جامع برای بهبود شرایط سالن و پرندگان در زمان سرما یا گرمازدگی
 */
@Composable
private fun ThermalStressGuideDialog(
    flock: Flock,
    guide: com.example.poultryapp.engine.ThermalStressGuide,
    onDismiss: () -> Unit
) {
    val isHeat = guide.isHeatStress
    val themeColor = if (isHeat) Color(0xFFE65100) else Color(0xFF006064)
    val containerColor = if (isHeat) Color(0xFFFFF3E0) else Color(0xFFE0F7FA)
    val borderColor = if (isHeat) Color(0xFFFFB74D) else Color(0xFF80DEEA)
    val headerIcon = if (isHeat) Icons.Default.Whatshot else Icons.Default.AcUnit

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = guide.titleFa,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "گله «${flock.name}» (${flock.species.titleFa} | ${flock.ageDisplayFa})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary Callout
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = containerColor.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = guide.summaryFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 20.sp
                    )
                }

                Text(
                    text = "اقدامات فوری و دستورالعمل اجرایی:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Action cards
                guide.actionItems.forEachIndexed { index, item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = themeColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = toPersianDigits(index + 1),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.titleFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "بخش: ${item.categoryFa}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = themeColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.descriptionFa,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(FarmDimens.ButtonRadius)
            ) {
                Text("متوجه شدم، بستن", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(FarmDimens.DialogRadius)
    )
}
