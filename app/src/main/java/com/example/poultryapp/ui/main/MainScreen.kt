package com.example.poultryapp.ui.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poultryapp.data.model.DiseaseInfo
import com.example.poultryapp.theme.HealthRed
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.FarmGreenPrimary
import com.example.poultryapp.theme.PoultryAppTheme
import com.example.poultryapp.data.local.UserRole
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.poultryapp.ui.components.AppBottomNav
import com.example.poultryapp.ui.components.AppTab
import com.example.poultryapp.ui.components.AutoResizeText
import com.example.poultryapp.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    initialTargetTab: String? = null,
    initialTargetFlockId: Long? = null
) {
    val flocks by viewModel.flocks.collectAsStateWithLifecycle()
    val selectedFlockId by viewModel.selectedFlockId.collectAsStateWithLifecycle()
    val latestFeedCharge by viewModel.latestFeedCharge.collectAsStateWithLifecycle()
    val latestWaterCharge by viewModel.latestWaterCharge.collectAsStateWithLifecycle()
    val todayFeedTotal by viewModel.todayFeedTotal.collectAsStateWithLifecycle()
    val todayWaterTotal by viewModel.todayWaterTotal.collectAsStateWithLifecycle()
    val chargeHistory by viewModel.chargeHistory.collectAsStateWithLifecycle()
    val allFarmCharges by viewModel.allFarmCharges.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val warehouseItems by viewModel.warehouseItems.collectAsStateWithLifecycle()
    val eggProductions by viewModel.eggProductions.collectAsStateWithLifecycle()
    val financialTransactions by viewModel.financialTransactions.collectAsStateWithLifecycle()
    val birdTransfers by viewModel.birdTransfers.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsStateWithLifecycle()
    val customBreeds by viewModel.customBreeds.collectAsStateWithLifecycle()
    val weatherResult by viewModel.weatherState.collectAsStateWithLifecycle()
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsStateWithLifecycle()
    val rationPlans by viewModel.rationPlans.collectAsStateWithLifecycle()
    val customFeedIngredients by viewModel.customFeedIngredients.collectAsStateWithLifecycle()
    val mortalityLogs by viewModel.mortalityLogs.collectAsStateWithLifecycle()
    val farmContacts by viewModel.farmContacts.collectAsStateWithLifecycle()
    val biosecurityChecks by viewModel.biosecurityChecks.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    var showPinUnlockDialog by remember { mutableStateOf(false) }
    var showVoiceAssistantSheet by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var flocksSubTab by rememberSaveable {
        mutableStateOf(
            when (initialTargetTab) {
                "FEED_WATER" -> FlocksSubTab.FEED_WATER
                "CALENDAR" -> FlocksSubTab.CALENDAR
                "EGG_PRODUCTION" -> FlocksSubTab.EGG_PRODUCTION
                else -> FlocksSubTab.OVERVIEW
            }
        )
    }
    var managementSubTab by rememberSaveable { mutableStateOf(ManagementSubTab.WAREHOUSE) }
    var healthSubTab by rememberSaveable { mutableStateOf(HealthSubTab.DIAGNOSIS) }

    var currentTab by rememberSaveable {
        mutableStateOf(
            when (initialTargetTab) {
                "FEED_WATER", "CALENDAR", "FLOCKS", "EGG_PRODUCTION" -> AppTab.FLOCKS
                "HEALTH" -> AppTab.HEALTH
                "SETTINGS" -> AppTab.SETTINGS
                "WAREHOUSE", "MANAGEMENT", "FINANCIAL", "RATION_BUILDER" -> AppTab.MANAGEMENT
                else -> AppTab.HOME
            }
        )
    }

    var lastActiveBottomTab by rememberSaveable { mutableStateOf(currentTab) }
    if (currentTab != AppTab.GOLDEN_BREEDING) {
        lastActiveBottomTab = currentTab
    }

    LaunchedEffect(initialTargetTab) {
        when (initialTargetTab) {
            "FEED_WATER" -> {
                flocksSubTab = FlocksSubTab.FEED_WATER
                currentTab = AppTab.FLOCKS
            }
            "CALENDAR" -> {
                flocksSubTab = FlocksSubTab.CALENDAR
                currentTab = AppTab.FLOCKS
            }
            "EGG_PRODUCTION" -> {
                flocksSubTab = FlocksSubTab.EGG_PRODUCTION
                currentTab = AppTab.FLOCKS
            }
            "FLOCKS" -> {
                flocksSubTab = FlocksSubTab.OVERVIEW
                currentTab = AppTab.FLOCKS
            }
            "HEALTH" -> currentTab = AppTab.HEALTH
            "SETTINGS" -> currentTab = AppTab.SETTINGS
            "RATION_BUILDER" -> {
                managementSubTab = ManagementSubTab.RATION_BUILDER
                currentTab = AppTab.MANAGEMENT
            }
            "WAREHOUSE", "MANAGEMENT", "FINANCIAL" -> currentTab = AppTab.MANAGEMENT
            "HOME" -> currentTab = AppTab.HOME
            "VOICE_ASSISTANT" -> showVoiceAssistantSheet = true
        }
    }

    LaunchedEffect(initialTargetFlockId) {
        if (initialTargetFlockId != null) {
            viewModel.selectFlock(initialTargetFlockId)
        }
    }

    // Auto-refresh weather upon entering / resuming the app
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshWeather(forceOnline = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var selectedDiseaseDetail by remember { mutableStateOf<DiseaseInfo?>(null) }
    var hasDismissedInitialCityDialog by rememberSaveable { mutableStateOf(false) }

    // Resolve Dark theme setting
    val darkTheme = when (settings.themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    val isGameTab = (currentTab == AppTab.GOLDEN_BREEDING && selectedDiseaseDetail == null)
    val useDarkIcons = !darkTheme && !isGameTab

    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null && !view.isInEditMode) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = useDarkIcons
        }
    }

    // Back handling: drawer first, then disease detail, then back to HOME
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    if (selectedDiseaseDetail != null) {
        BackHandler {
            selectedDiseaseDetail = null
        }
    } else if (currentTab != AppTab.HOME && !drawerState.isOpen) {
        BackHandler {
            currentTab = AppTab.HOME
        }
    }

    PoultryAppTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(modifier = Modifier.fillMaxSize()) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !showVoiceAssistantSheet,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(320.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Header banner
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .statusBarsPadding()
                                        .padding(horizontal = 20.dp, vertical = 24.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_farm_dashboard),
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "مدیریت هوشمند طیور",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "سامانه پایش زنده، گله‌ها و انبارداری",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Drawer Items (Items not accessible from bottom navigation bar)
                            Text(
                                text = "تنظیمات و امکانات سیستمی",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )

                            val drawerNavItems = buildList {
                                add(Pair(AppTab.GOLDEN_BREEDING, "پرورش طلایی (بازی و آکادمی)"))
                                add(Pair(AppTab.BREEDING_GUIDE, "راهنمای جامع پرورش (سنتی)"))
                                if (userRole == UserRole.MANAGER || !settings.isWorkerModeEnabled) {
                                    add(Pair(AppTab.SETTINGS, "تنظیمات برنامه"))
                                }
                            }

                            drawerNavItems.forEach { (tab, label) ->
                                val isSelected = currentTab == tab
                                NavigationDrawerItem(
                                    label = {
                                        AutoResizeText(
                                            text = label,
                                            targetFontSize = 14.sp,
                                            minFontSize = 10.sp,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    },
                                    icon = {
                                        Image(
                                            painter = painterResource(id = tab.drawableRes),
                                            contentDescription = label,
                                            modifier = Modifier.size(32.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    },
                                    selected = isSelected,
                                    onClick = {
                                        currentTab = tab
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                            // Footer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = FarmGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "نسخه ۳.۲.۰ — کاملاً آفلاین",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "پایگاه داده رمزنگاری‌شده و محلی",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.navigationBarsPadding())
                        }
                    }
                }
            ) {
                if (selectedDiseaseDetail != null) {
                    DiseaseDetailScreen(
                        disease = selectedDiseaseDetail!!,
                        onBack = { selectedDiseaseDetail = null },
                        onConsultVet = {
                            selectedDiseaseDetail = null
                            currentTab = AppTab.HEALTH
                            healthSubTab = HealthSubTab.VET_CONSULT
                        },
                        onOpenVoiceAssistant = { showVoiceAssistantSheet = true }
                    )
                } else {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            AnimatedVisibility(
                                visible = currentTab != AppTab.GOLDEN_BREEDING,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(animationSpec = tween(220)),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                                ) + fadeOut(animationSpec = tween(160))
                            ) {
                                AppBottomNav(
                                    currentTab = if (currentTab == AppTab.GOLDEN_BREEDING) lastActiveBottomTab else currentTab,
                                    onTabSelected = { currentTab = it }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    if (targetState == AppTab.GOLDEN_BREEDING) {
                                        // ورود سینماتیک به پرورش طلایی: بزرگنمایی رو به جلو + لغزش ملایم و فید عمیق
                                        (scaleIn(initialScale = 0.90f, animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) +
                                         fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                                         slideInVertically(initialOffsetY = { it / 10 }, animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)))
                                            .togetherWith(
                                                scaleOut(targetScale = 0.96f, animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                                                fadeOut(animationSpec = tween(200))
                                            )
                                    } else if (initialState == AppTab.GOLDEN_BREEDING) {
                                        // بازگشت نرم از پرورش طلایی به داشبورد فارم: خروج ملایم بازی و بازگشت داشبورد
                                        (scaleIn(initialScale = 0.96f, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                         fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)))
                                            .togetherWith(
                                                scaleOut(targetScale = 0.92f, animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                                                slideOutVertically(targetOffsetY = { it / 10 }, animationSpec = tween(240)) +
                                                fadeOut(animationSpec = tween(200))
                                            )
                                    } else {
                                        // جابجایی بین سایر تب‌ها (نرم، بدون پرش و هماهنگ)
                                        (fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                         scaleIn(initialScale = 0.98f, animationSpec = tween(200, easing = FastOutSlowInEasing)))
                                            .togetherWith(
                                                fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
                                            )
                                    }
                                },
                                label = "mainTabAnimatedContent"
                            ) { tab ->
                                val bottomBarPadding = if (tab == AppTab.GOLDEN_BREEDING) 0.dp else innerPadding.calculateBottomPadding()
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = bottomBarPadding)
                                ) {
                                    when (tab) {
                                    AppTab.HOME -> HomeScreen(
                                        flocks = flocks,
                                        warehouseItems = warehouseItems,
                                        schedules = schedules,
                                        chargesHistory = allFarmCharges,
                                        customBreeds = customBreeds,
                                        farmAmbientTemp = settings.manualTempCelsius ?: weatherResult?.temperatureCelsius ?: settings.cachedTempCelsius,
                                        weatherResult = weatherResult,
                                        isWeatherLoading = isWeatherLoading,
                                        onRefreshWeather = { viewModel.refreshWeather(forceOnline = true) },
                                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                        onNavigateToFeedWater = { flockId ->
                                            if (flockId != null) viewModel.selectFlock(flockId)
                                            flocksSubTab = FlocksSubTab.FEED_WATER
                                            currentTab = AppTab.FLOCKS
                                        },
                                        onNavigateToWarehouse = { currentTab = AppTab.MANAGEMENT },
                                        onNavigateToCalendar = {
                                            flocksSubTab = FlocksSubTab.CALENDAR
                                            currentTab = AppTab.FLOCKS
                                        },
                                        onNavigateToFlocks = {
                                            flocksSubTab = FlocksSubTab.OVERVIEW
                                            currentTab = AppTab.FLOCKS
                                        },
                                        eggProductions = eggProductions,
                                        onLogEggProduction = { flockIds, healthy, broken, dirty, doubleYolk, dateJalaliStr, notes ->
                                            viewModel.logEggProductionForMultipleFlocks(flockIds, healthy, broken, dirty, doubleYolk, dateJalaliStr, notes)
                                        },
                                        onNavigateToManagement = { currentTab = AppTab.MANAGEMENT },
                                        onNavigateToGoldenBreeding = { currentTab = AppTab.GOLDEN_BREEDING },
                                        onOpenVoiceAssistant = { showVoiceAssistantSheet = true }
                                    )

                                    AppTab.FLOCKS, AppTab.FEED_WATER, AppTab.CALENDAR -> FlocksScreen(
                                        flocks = flocks,
                                        warehouseItems = warehouseItems,
                                        customBreeds = customBreeds,
                                        farmAmbientTemp = settings.manualTempCelsius ?: weatherResult?.temperatureCelsius ?: settings.cachedTempCelsius,
                                        initialSubTab = when (tab) {
                                            AppTab.FEED_WATER -> FlocksSubTab.FEED_WATER
                                            AppTab.CALENDAR -> FlocksSubTab.CALENDAR
                                            else -> flocksSubTab
                                        },
                                        onSubTabSelected = { flocksSubTab = it },
                                        eggProductions = eggProductions,
                                        birdTransfers = birdTransfers,
                                        onTransferBirds = { fromId, toId, count, notes ->
                                            viewModel.transferBirds(fromId, toId, count, notes)
                                        },
                                        onLogEggProduction = { flockIds, healthy, broken, dirty, doubleYolk, dateJalaliStr, notes ->
                                            viewModel.logEggProductionForMultipleFlocks(flockIds, healthy, broken, dirty, doubleYolk, dateJalaliStr, notes)
                                        },
                                        onLogDetailedEggProduction = { flockIds, fertile, tableEgg, broken, dirty, doubleYolk, trayCount, trayGross, _, dateStr, notes ->
                                            viewModel.logDetailedEggProductionForMultipleFlocks(
                                                flockIds = flockIds,
                                                totalFertile = fertile,
                                                totalTableEgg = tableEgg,
                                                totalBroken = broken,
                                                totalDirty = dirty,
                                                totalDoubleYolk = doubleYolk,
                                                totalTrayCount = trayCount,
                                                trayGrossWeightGrams = trayGross,
                                                dateJalaliStr = dateStr,
                                                notes = notes
                                            )
                                        },
                                        onLogMortality = { flockId, count, reason, diagHint, dateStr, notes ->
                                            viewModel.logFlockMortality(flockId, count, reason, diagHint, notes, dateStr)
                                        },
                                        onDeleteMortality = { viewModel.deleteMortalityLog(it) },
                                        userRole = userRole,
                                        mortalityLogs = mortalityLogs,
                                        onDeleteEggProduction = { viewModel.deleteEggProduction(it) },
                                        onAddFlock = { name, species, breed, ageDays, count, notes, subGroups, isFeedEnabled, isWaterEnabled, customTempCelsius ->
                                            viewModel.addFlock(
                                                name = name,
                                                species = species,
                                                breed = breed,
                                                ageDays = ageDays,
                                                birdCount = count,
                                                notes = notes,
                                                subGroups = subGroups,
                                                isFeedEnabled = isFeedEnabled,
                                                isWaterEnabled = isWaterEnabled,
                                                customTempCelsius = customTempCelsius
                                            )
                                        },
                                        onEditFlock = { flock ->
                                            viewModel.editFlock(flock)
                                        },
                                        onDeleteFlock = { flockId ->
                                            viewModel.deleteFlock(flockId)
                                        },
                                        onSelectFlockForCharge = { flock ->
                                            viewModel.selectFlock(flock.id)
                                            flocksSubTab = FlocksSubTab.FEED_WATER
                                        },
                                        onUpdateFlockCustomTemp = { flockId, temp ->
                                            viewModel.setFlockCustomTemperature(flockId, temp)
                                        },
                                        selectedFlockId = selectedFlockId,
                                        onSelectFlock = { viewModel.selectFlock(it) },
                                        latestFeedCharge = latestFeedCharge,
                                        latestWaterCharge = latestWaterCharge,
                                        todayFeedTotal = todayFeedTotal,
                                        todayWaterTotal = todayWaterTotal,
                                        chargeHistory = chargeHistory,
                                        settings = settings,
                                        weatherResult = weatherResult,
                                        isWeatherLoading = isWeatherLoading,
                                        onRefreshWeather = { viewModel.refreshWeather(forceOnline = true) },
                                        onSelectCity = { prov, city -> viewModel.setFarmLocation(prov, city) },
                                        onGpsDetect = {
                                            viewModel.detectLocationGps { success, msg ->
                                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        onChargeSubmit = { flockId, itemType, amount, notes ->
                                            viewModel.logCharge(flockId, itemType, amount, notes)
                                        },
                                        onDeleteCharge = { viewModel.deleteCharge(it) },
                                        onToggleFeedTracking = { flockId, isEnabled ->
                                            viewModel.toggleFlockFeedTracking(flockId, isEnabled)
                                        },
                                        onToggleWaterTracking = { flockId, isEnabled ->
                                            viewModel.toggleFlockWaterTracking(flockId, isEnabled)
                                        },
                                        schedules = schedules,
                                        onAddSchedule = { viewModel.addSchedule(it) },
                                        onEditSchedule = { viewModel.editSchedule(it) },
                                        onDeleteSchedule = { viewModel.deleteSchedule(it) },
                                        onToggleSchedule = { id, enabled -> viewModel.toggleSchedule(id, enabled) },
                                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                        onOpenVoiceAssistant = { showVoiceAssistantSheet = true }
                                    )

                                    AppTab.MANAGEMENT, AppTab.WAREHOUSE -> ManagementScreen(
                                        warehouseItems = warehouseItems,
                                        financialTransactions = financialTransactions,
                                        flocks = flocks,
                                        rationPlans = rationPlans,
                                        contacts = farmContacts,
                                        eggProductions = eggProductions,
                                        mortalityLogs = mortalityLogs,
                                        biosecurityChecks = biosecurityChecks,
                                        farmName = if (settings.farmCity.isNotBlank()) "فارم پرورش طیور ${settings.farmCity}" else "مزرعه پرورش طیور",
                                        managerName = "مدیر فارم",
                                        userRole = userRole,
                                        onAddContact = { viewModel.addFarmContact(it) },
                                        onUpdateContact = { viewModel.editFarmContact(it) },
                                        onDeleteContact = { viewModel.deleteFarmContact(it) },
                                        customBreeds = customBreeds,
                                        farmAmbientTemp = settings.manualTempCelsius ?: weatherResult?.temperatureCelsius ?: settings.cachedTempCelsius,
                                        initialSubTab = managementSubTab,
                                        onSubTabSelected = { managementSubTab = it },
                                        onAddItem = { viewModel.addWarehouseItem(it) },
                                        onEditItem = { viewModel.editWarehouseItem(it) },
                                        onDeleteItem = { viewModel.deleteWarehouseItem(it) },
                                        onAdjustStock = { id, delta -> viewModel.adjustWarehouseStock(id, delta) },
                                        onDepleteStock = { id -> viewModel.depleteWarehouseStock(id) },
                                        onConsumeStock = { itemId, amount, flockIds, notes ->
                                            viewModel.consumeWarehouseStock(itemId, amount, flockIds, notes)
                                        },
                                        onSellEgg = { itemId, count, pricePerUnit, notes ->
                                            viewModel.sellEggStock(itemId, count, pricePerUnit, notes)
                                        },
                                        onAddTransaction = { viewModel.addFinancialTransaction(it) },
                                        onDeleteTransaction = { viewModel.deleteFinancialTransaction(it) },
                                        onAddRationPlan = { viewModel.addRationPlan(it) },
                                        onEditRationPlan = { viewModel.editRationPlan(it) },
                                        onDeleteRationPlan = { viewModel.deleteRationPlan(it) },
                                        onDuplicateRationPlan = { viewModel.duplicateRationPlan(it) },
                                        onAddRationToWarehouse = { plan, qty, n -> viewModel.addRationToWarehouse(plan, qty, n) },
                                        customFeedIngredients = customFeedIngredients,
                                        onAddCustomFeedIngredient = { viewModel.addCustomFeedIngredient(it) },
                                        onDeleteCustomFeedIngredient = { viewModel.deleteCustomFeedIngredient(it) },
                                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                        onOpenVoiceAssistant = { showVoiceAssistantSheet = true }
                                    )

                                    AppTab.HEALTH -> HealthScreen(
                                        flocks = flocks,
                                        biosecurityChecks = biosecurityChecks,
                                        onToggleBiosecurityCheck = { id, comp -> viewModel.toggleBiosecurityCheck(id, comp) },
                                        onSetFlockMedicationWithdrawal = { flockId, days, med -> viewModel.setFlockMedicationWithdrawal(flockId, days, med) },
                                        onClearFlockMedicationWithdrawal = { flockId -> viewModel.clearFlockMedicationWithdrawal(flockId) },
                                        initialSubTab = healthSubTab,
                                        onSubTabSelected = { healthSubTab = it },
                                        onSelectDiseaseDetail = { disease ->
                                            selectedDiseaseDetail = disease
                                        },
                                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                        onOpenVoiceAssistant = { showVoiceAssistantSheet = true }
                                    )

                                    AppTab.SETTINGS -> SettingsScreen(
                                        settings = settings,
                                        customBreeds = customBreeds,
                                        weatherResult = weatherResult,
                                        isWeatherLoading = isWeatherLoading,
                                        onRefreshWeather = { viewModel.refreshWeather(forceOnline = true) },
                                        onSetFarmLocation = { prov, city -> viewModel.setFarmLocation(prov, city) },
                                        onDetectGpsLocation = { callback ->
                                            viewModel.detectLocationGps { success, msg ->
                                                callback(success, msg)
                                            }
                                        },
                                        onSaveSettings = { viewModel.saveSettings(it) },
                                        onExportBackup = { viewModel.exportBackupJson() },
                                        onImportBackup = { viewModel.importBackupJson(it) },
                                        onAddCustomBreed = { viewModel.addCustomBreed(it) },
                                        onEditCustomBreed = { viewModel.editCustomBreed(it) },
                                        onDeleteCustomBreed = { viewModel.deleteCustomBreed(it) },
                                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                        onOpenVoiceAssistant = { showVoiceAssistantSheet = true }
                                    )

                                    AppTab.BREEDING_GUIDE -> BreedingGuideScreen(
                                        flocks = flocks,
                                        selectedFlockId = selectedFlockId,
                                        customBreeds = customBreeds,
                                        farmAmbientTemp = settings.manualTempCelsius ?: weatherResult?.temperatureCelsius ?: settings.cachedTempCelsius,
                                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                        onOpenVoiceAssistant = { showVoiceAssistantSheet = true }
                                    )

                                    AppTab.GOLDEN_BREEDING -> Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF03160E))
                                    ) {
                                        com.example.poultryapp.ui.screens.golden_breeding.GoldenBreedingScreen(
                                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                            onBackToFarm = { currentTab = AppTab.HOME }
                                        )
                                    }
                                }
                            }

                            // دکمه شناور نشانگر و تغییر وضعیت نقش کارگر / مدیر فارم
                            if (settings.isWorkerModeEnabled && currentTab != AppTab.GOLDEN_BREEDING) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (userRole == UserRole.WORKER) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, if (userRole == UserRole.WORKER) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .statusBarsPadding()
                                        .padding(top = 8.dp, end = 12.dp)
                                        .clickable {
                                            if (userRole == UserRole.WORKER) {
                                                showPinUnlockDialog = true
                                            } else {
                                                viewModel.switchToWorkerMode()
                                                android.widget.Toast.makeText(context, "سوییچ به حالت کارگر سالن — تب مالی و تنظیمات قفل شدند", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (userRole == UserRole.WORKER) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = if (userRole == UserRole.WORKER) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (userRole == UserRole.WORKER) "کارگر سالن 🔒" else "مدیر فارم 👑",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (userRole == UserRole.WORKER) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

    // Prompt user on first launch to select city if not yet configured
    if (isSettingsLoaded && settings.farmCity.isBlank() && !hasDismissedInitialCityDialog) {
        com.example.poultryapp.ui.components.CitySelectionDialog(
            initialProvince = settings.farmProvince,
            initialCity = settings.farmCity,
            onDismiss = {
                hasDismissedInitialCityDialog = true
            },
            onCitySelected = { prov, city ->
                viewModel.setFarmLocation(prov, city)
                hasDismissedInitialCityDialog = true
                android.widget.Toast.makeText(context, "شهر $city با موفقیت به عنوان موقعیت فارم انتخاب شد", android.widget.Toast.LENGTH_SHORT).show()
            },
            onGpsDetectClick = {
                viewModel.detectLocationGps { success, msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
                hasDismissedInitialCityDialog = true
            }
        )
    }

    // دیالوگ احراز هویت پین‌کد یا کلید اضطراری جهت ورود به پنل مدیر فارم
    if (showPinUnlockDialog) {
        var enteredPin by remember { mutableStateOf("") }
        var isUsingRecoveryKey by remember { mutableStateOf(false) }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinUnlockDialog = false },
            icon = {
                Icon(
                    imageVector = if (isUsingRecoveryKey) Icons.Default.LockReset else Icons.Default.Lock,
                    contentDescription = null,
                    tint = FarmGreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (isUsingRecoveryKey) "بازیابی با کلید اضطراری" else "ورود به پنل مدیریت فارم",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isUsingRecoveryKey)
                            "کلید اضطراری ۶ رقمی که هنگام تنظیم پین صادر شده بود را وارد کنید:"
                        else
                            "برای دسترسی به اطلاعات مالی، انبار و تنظیمات، پین‌کد مدیریت را وارد کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= (if (isUsingRecoveryKey) 6 else 8)) {
                                enteredPin = it
                                pinError = null
                            }
                        },
                        label = { Text(if (isUsingRecoveryKey) "کلید اضطراری ۶ رقمی" else "پین‌کد ورود (۴ تا ۶ رقم)") },
                        isError = pinError != null,
                        supportingText = {
                            pinError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        visualTransformation = if (isUsingRecoveryKey) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            isUsingRecoveryKey = !isUsingRecoveryKey
                            enteredPin = ""
                            pinError = null
                        }
                    ) {
                        Text(
                            text = if (isUsingRecoveryKey) "← ورود با پین‌کد مدیریت" else "فراموشی پین؟ ورود با کلید بازیابی اضطراری",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = if (isUsingRecoveryKey) {
                            viewModel.unlockWithRecoveryKey(enteredPin)
                        } else {
                            viewModel.verifyManagerPin(enteredPin)
                        }
                        if (success) {
                            showPinUnlockDialog = false
                            android.widget.Toast.makeText(context, "ورود مدیر فارم تایید شد", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            pinError = if (isUsingRecoveryKey) "کلید بازیابی اضطراری نامعتبر است" else "پین‌کد مدیریت اشتباه است"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                ) {
                    Text("ورود")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPinUnlockDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showVoiceAssistantSheet) {
        val farmLiveContext = remember(
            flocks, eggProductions, mortalityLogs, warehouseItems,
            financialTransactions, allFarmCharges, schedules, farmContacts, weatherResult
        ) {
            com.example.poultryapp.engine.FarmLiveContext(
                flocks = flocks,
                eggProductions = eggProductions,
                mortalityLogs = mortalityLogs,
                warehouseItems = warehouseItems,
                financialTransactions = financialTransactions,
                allFarmCharges = allFarmCharges,
                schedules = schedules,
                contacts = farmContacts,
                weather = weatherResult
            )
        }

        com.example.poultryapp.voice.VoiceAssistantSheet(
            flocks = flocks,
            contacts = farmContacts,
            eggProductions = eggProductions,
            farmContext = farmLiveContext,
            userRole = userRole,
            onDismiss = { showVoiceAssistantSheet = false },
            onExecuteAction = { card ->
                com.example.poultryapp.voice.VoiceActionDispatcher.dispatch(
                    context = context,
                    viewModel = viewModel,
                    card = card,
                    userRole = userRole,
                    onNavigate = { targetTab ->
                        when {
                            targetTab.startsWith("DISEASE_DETAIL:") -> {
                                val dId = targetTab.removePrefix("DISEASE_DETAIL:")
                                val disease = com.example.poultryapp.data.model.DiseaseCatalog.allDiseases.find { it.id == dId }
                                if (disease != null) {
                                    selectedDiseaseDetail = disease
                                } else {
                                    healthSubTab = HealthSubTab.DIAGNOSIS
                                    currentTab = AppTab.HEALTH
                                }
                            }
                            targetTab == "HOME" -> currentTab = AppTab.HOME
                            targetTab == "FLOCKS" -> currentTab = AppTab.FLOCKS
                            targetTab == "FEED_WATER" -> currentTab = AppTab.FEED_WATER
                            targetTab == "CALENDAR" -> currentTab = AppTab.CALENDAR
                            targetTab == "GOLDEN_BREEDING" -> currentTab = AppTab.GOLDEN_BREEDING
                            targetTab == "WAREHOUSE" -> {
                                managementSubTab = ManagementSubTab.WAREHOUSE
                                currentTab = AppTab.MANAGEMENT
                            }
                            targetTab == "FINANCIAL" -> {
                                managementSubTab = ManagementSubTab.FINANCIAL
                                currentTab = AppTab.MANAGEMENT
                            }
                            targetTab == "RATION" || targetTab == "RATION_BUILDER" -> {
                                managementSubTab = ManagementSubTab.RATION_BUILDER
                                currentTab = AppTab.MANAGEMENT
                            }
                            targetTab == "CONTACTS" -> {
                                managementSubTab = ManagementSubTab.CONTACTS
                                currentTab = AppTab.MANAGEMENT
                            }
                            targetTab == "HEALTH" -> {
                                healthSubTab = HealthSubTab.DIAGNOSIS
                                currentTab = AppTab.HEALTH
                            }
                            targetTab == "MEDICATIONS" -> {
                                healthSubTab = HealthSubTab.DRUGS
                                currentTab = AppTab.HEALTH
                            }
                            targetTab == "BIOSECURITY" -> {
                                healthSubTab = HealthSubTab.BIOSECURITY
                                currentTab = AppTab.HEALTH
                            }
                            targetTab == "SIMULATORS" -> {
                                managementSubTab = ManagementSubTab.SIMULATORS
                                currentTab = AppTab.MANAGEMENT
                            }
                            targetTab == "SETTINGS" -> currentTab = AppTab.SETTINGS
                            targetTab == "KNOWLEDGE" -> currentTab = AppTab.BREEDING_GUIDE
                        }
                    }
                )
            }
        )
    }
}
}
}
}
}



