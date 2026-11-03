package com.example.poultryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.poultryapp.R
import com.example.poultryapp.data.local.CustomBreed
import com.example.poultryapp.data.local.FarmContact
import com.example.poultryapp.data.local.FinancialTransaction
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.RationPlan
import com.example.poultryapp.data.local.WarehouseItem
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.ui.components.AppTopBar
import com.example.poultryapp.ui.components.FarmSubTabBar
import com.example.poultryapp.ui.components.FarmSubTabItem

enum class ManagementSubTab(val titleFa: String, val shortTitleFa: String, val icon: ImageVector, val drawableRes: Int) {
    WAREHOUSE("انبار و موجودی", "انبار", Icons.Default.Inventory2, R.drawable.ic_farm_warehouse),
    FINANCIAL("امور مالی", "امور مالی", Icons.Default.AccountBalanceWallet, R.drawable.ic_farm_financial),
    RATION_BUILDER("جیره‌ساز", "جیره‌ساز", Icons.Default.Calculate, R.drawable.ic_farm_ration),
    CONTACTS("مخاطبین تجاری", "مخاطبین", Icons.Default.ContactPhone, R.drawable.ic_farm_management),
    SIMULATORS("شبیه‌سازها", "شبیه‌سازها", Icons.Default.AutoGraph, R.drawable.ic_farm_dashboard)
}

@Composable
fun ManagementScreen(
    warehouseItems: List<WarehouseItem>,
    financialTransactions: List<FinancialTransaction>,
    flocks: List<Flock>,
    rationPlans: List<RationPlan> = emptyList(),
    contacts: List<FarmContact> = emptyList(),
    customBreeds: List<CustomBreed> = emptyList(),
    farmAmbientTemp: Double? = null,
    initialSubTab: ManagementSubTab = ManagementSubTab.WAREHOUSE,
    onSubTabSelected: (ManagementSubTab) -> Unit = {},
    onAddItem: (WarehouseItem) -> Unit,
    onEditItem: (WarehouseItem) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onAdjustStock: (id: Long, delta: Double) -> Unit,
    onDepleteStock: (Long) -> Unit = {},
    onConsumeStock: (itemId: Long, amount: Double, flockIds: List<Long>, notes: String?) -> Unit,
    onSellEgg: (itemId: Long, count: Double, pricePerUnit: Double, notes: String?) -> Unit,
    onAddTransaction: (FinancialTransaction) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onAddRationPlan: (RationPlan) -> Unit = {},
    onEditRationPlan: (RationPlan) -> Unit = {},
    onDeleteRationPlan: (Long) -> Unit = {},
    onDuplicateRationPlan: (RationPlan) -> Unit = {},
    onAddRationToWarehouse: (plan: RationPlan, quantityKg: Double, notes: String?) -> Unit = { _, _, _ -> },
    customFeedIngredients: List<com.example.poultryapp.data.model.PoultryFeedRawItem> = emptyList(),
    onAddCustomFeedIngredient: ((com.example.poultryapp.data.model.PoultryFeedRawItem) -> Unit)? = null,
    onDeleteCustomFeedIngredient: ((String) -> Unit)? = null,
    onAddContact: (FarmContact) -> Unit = {},
    onUpdateContact: (FarmContact) -> Unit = {},
    onDeleteContact: (Long) -> Unit = {},
    eggProductions: List<com.example.poultryapp.data.local.EggProduction> = emptyList(),
    mortalityLogs: List<com.example.poultryapp.data.local.FlockMortalityLog> = emptyList(),
    biosecurityChecks: List<com.example.poultryapp.data.local.BiosecurityCheckItem> = emptyList(),
    farmName: String = "",
    managerName: String = "",
    userRole: com.example.poultryapp.data.local.UserRole = com.example.poultryapp.data.local.UserRole.MANAGER,
    onOpenDrawer: () -> Unit,
    onOpenVoiceAssistant: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    val availableSubTabs = remember(userRole) {
        if (userRole == com.example.poultryapp.data.local.UserRole.WORKER) {
            listOf(ManagementSubTab.WAREHOUSE, ManagementSubTab.RATION_BUILDER, ManagementSubTab.CONTACTS, ManagementSubTab.SIMULATORS)
        } else {
            ManagementSubTab.entries
        }
    }
    val initialPageIndex = remember(initialSubTab, availableSubTabs) {
        val idx = availableSubTabs.indexOf(initialSubTab)
        if (idx >= 0) idx else 0
    }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { availableSubTabs.size }
    )

    var warehouseAddTrigger by remember { mutableIntStateOf(0) }
    var financialAddTrigger by remember { mutableIntStateOf(0) }
    var rationAddTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialSubTab, availableSubTabs) {
        val targetPage = availableSubTabs.indexOf(initialSubTab)
        if (targetPage >= 0 && pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage, availableSubTabs) {
        if (pagerState.currentPage in availableSubTabs.indices) {
            onSubTabSelected(availableSubTabs[pagerState.currentPage])
        }
    }

    val activeSubTab = if (pagerState.currentPage in availableSubTabs.indices) {
        availableSubTabs[pagerState.currentPage]
    } else ManagementSubTab.WAREHOUSE

    Scaffold(
        topBar = {
            Column {
                AppTopBar(
                    title = when (activeSubTab) {
                        ManagementSubTab.WAREHOUSE -> "مدیریت انبار و اقلام خوراک"
                        ManagementSubTab.FINANCIAL -> "امور مالی و سود و زیان"
                        ManagementSubTab.RATION_BUILDER -> "جیره‌ساز و فرمولاسیون دان"
                        ManagementSubTab.CONTACTS -> "دفترچه مخاطبین و شبکه تامین"
                        ManagementSubTab.SIMULATORS -> "شبیه‌سازها و محاسبات فارم"
                    },
                    subtitle = when (activeSubTab) {
                        ManagementSubTab.WAREHOUSE -> "${toPersianDigits(warehouseItems.size)} قلم کالا در انبار فارم"
                        ManagementSubTab.FINANCIAL -> "تراز مالی و ماشین‌حساب تخصصی فارم"
                        ManagementSubTab.RATION_BUILDER -> "${toPersianDigits(rationPlans.size)} فرمول ذخیره‌شده جیره و مکمل"
                        ManagementSubTab.CONTACTS -> "${toPersianDigits(contacts.size)} مخاطب و طرف‌حساب تجاری فارم"
                        ManagementSubTab.SIMULATORS -> "شبیه‌ساز تاریخ کشتار و توجیه‌پذیری سرمایه‌گذاری"
                    },
                    logoRes = when (activeSubTab) {
                        ManagementSubTab.WAREHOUSE -> R.drawable.ic_farm_warehouse
                        ManagementSubTab.FINANCIAL -> R.drawable.ic_farm_financial
                        ManagementSubTab.RATION_BUILDER -> R.drawable.ic_farm_ration
                        ManagementSubTab.CONTACTS -> R.drawable.ic_farm_management
                        ManagementSubTab.SIMULATORS -> R.drawable.ic_farm_dashboard
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "منوی برنامه")
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

                val subTabItems = remember(availableSubTabs) {
                    availableSubTabs.map {
                        FarmSubTabItem(title = it.shortTitleFa, icon = it.icon)
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
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (availableSubTabs[page]) {
                ManagementSubTab.WAREHOUSE -> WarehouseScreen(
                    warehouseItems = warehouseItems,
                    flocks = flocks,
                    customBreeds = customBreeds,
                    farmAmbientTemp = farmAmbientTemp,
                    onAddItem = onAddItem,
                    onEditItem = onEditItem,
                    onDeleteItem = onDeleteItem,
                    onAdjustStock = onAdjustStock,
                    onDepleteStock = onDepleteStock,
                    onConsumeStock = onConsumeStock,
                    onSellEgg = onSellEgg,
                    onOpenDrawer = onOpenDrawer,
                    showHeader = false,
                    externalAddTrigger = warehouseAddTrigger
                )

                ManagementSubTab.FINANCIAL -> FinancialScreen(
                    transactions = financialTransactions,
                    flocks = flocks,
                    warehouseItems = warehouseItems,
                    onAddTransaction = onAddTransaction,
                    onDeleteTransaction = onDeleteTransaction,
                    onOpenDrawer = onOpenDrawer,
                    showHeader = false,
                    externalAddTrigger = financialAddTrigger
                )

                ManagementSubTab.RATION_BUILDER -> RationBuilderScreen(
                    rationPlans = rationPlans,
                    onAddPlan = onAddRationPlan,
                    onEditPlan = onEditRationPlan,
                    onDeletePlan = onDeleteRationPlan,
                    onDuplicatePlan = onDuplicateRationPlan,
                    onAddToWarehouse = onAddRationToWarehouse,
                    onOpenDrawer = onOpenDrawer,
                    showHeader = false,
                    externalAddTrigger = rationAddTrigger,
                    customFeedIngredients = customFeedIngredients,
                    onAddCustomIngredient = onAddCustomFeedIngredient,
                    onDeleteCustomIngredient = onDeleteCustomFeedIngredient
                )

                ManagementSubTab.CONTACTS -> ContactsScreen(
                    contacts = contacts,
                    onAddContact = onAddContact,
                    onUpdateContact = onUpdateContact,
                    onDeleteContact = onDeleteContact
                )

                ManagementSubTab.SIMULATORS -> SimulatorsScreen(
                    flocks = flocks,
                    onOpenDrawer = onOpenDrawer,
                    showHeader = false
                )
            }
        }
    }
}
