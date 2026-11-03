package com.example.poultryapp.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.ui.components.AutoResizeText
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poultryapp.data.local.RationIngredient
import com.example.poultryapp.data.local.RationItemType
import com.example.poultryapp.data.local.RationPlan
import com.example.poultryapp.data.local.RationPricingMode
import com.example.poultryapp.data.model.ComprehensiveFeedCatalog
import com.example.poultryapp.data.model.FeedCategory
import com.example.poultryapp.data.model.PoultryFeedRawItem
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.ui.components.AppTopBar
import com.example.poultryapp.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import java.text.DecimalFormat

private fun Any?.toPersianDigits(): String = com.example.poultryapp.engine.toPersianDigits(this)

private val moneyFormatter = DecimalFormat("#,###")

private fun formatMoneyFa(amount: Double): String {
    return moneyFormatter.format(amount.toLong()).toPersianDigits()
}

private fun formatDecimalFa(value: Double, decimals: Int = 1): String {
    val pattern = if (decimals <= 1) "#,##0.#" else "#,##0.##"
    return DecimalFormat(pattern).format(value).toPersianDigits()
}

// کاتالوگ آماده اقلام خوراک استاندارد مرغداری جهت انتخاب سریع
typealias PredefinedFeedRawItem = com.example.poultryapp.data.model.PoultryFeedRawItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RationBuilderScreen(
    rationPlans: List<RationPlan>,
    onAddPlan: (RationPlan) -> Unit,
    onEditPlan: (RationPlan) -> Unit,
    onDeletePlan: (Long) -> Unit,
    onDuplicatePlan: (RationPlan) -> Unit,
    onAddToWarehouse: (plan: RationPlan, quantityKg: Double, notes: String?) -> Unit,
    onOpenDrawer: () -> Unit,
    showHeader: Boolean = true,
    externalAddTrigger: Int = 0,
    customFeedIngredients: List<com.example.poultryapp.data.model.PoultryFeedRawItem> = emptyList(),
    onAddCustomIngredient: ((com.example.poultryapp.data.model.PoultryFeedRawItem) -> Unit)? = null,
    onDeleteCustomIngredient: ((String) -> Unit)? = null,
    onOpenVoiceAssistant: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTypeFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, FEED, SUPPLEMENT

    // Dialog States
    var showCreateEditDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<RationPlan?>(null) }

    LaunchedEffect(externalAddTrigger) {
        if (externalAddTrigger > 0) {
            planToEdit = null
            showCreateEditDialog = true
        }
    }
    var planForBatchCalculator by remember { mutableStateOf<RationPlan?>(null) }
    var planForWarehouseTransfer by remember { mutableStateOf<RationPlan?>(null) }
    var planForDetailsView by remember { mutableStateOf<RationPlan?>(null) }
    var planToDelete by remember { mutableStateOf<RationPlan?>(null) }

    // Filtered items
    val filteredPlans = remember(rationPlans, searchQuery, selectedTypeFilter) {
        rationPlans.filter { plan ->
            val matchQuery = searchQuery.isBlank() ||
                    plan.name.contains(searchQuery, ignoreCase = true) ||
                    plan.targetStage.contains(searchQuery, ignoreCase = true) ||
                    plan.ingredients.any { it.name.contains(searchQuery, ignoreCase = true) }

            val matchType = when (selectedTypeFilter) {
                "FEED" -> plan.itemType == RationItemType.FEED
                "SUPPLEMENT" -> plan.itemType == RationItemType.SUPPLEMENT
                else -> true
            }
            matchQuery && matchType
        }
    }

    val totalFeedsCount = rationPlans.count { it.itemType == RationItemType.FEED }
    val totalSupplementsCount = rationPlans.count { it.itemType == RationItemType.SUPPLEMENT }
    val avgFeedPrice = remember(rationPlans) {
        val feeds = rationPlans.filter { it.itemType == RationItemType.FEED && it.effectivePricePerKg > 0 }
        if (feeds.isNotEmpty()) feeds.map { it.effectivePricePerKg }.average() else 0.0
    }

    Scaffold(
        topBar = {
            if (showHeader) {
                AppTopBar(
                    title = "جیره‌ساز و فرمولاسیون دان",
                    subtitle = "${toPersianDigits(rationPlans.size)} فرمول ذخیره‌شده جیره و مکمل",
                    logoRes = com.example.poultryapp.R.drawable.ic_farm_ration,
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
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    planToEdit = null
                    showCreateEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        text = "فرمول جدید",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // کارت‌های آماری
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatMiniCard(
                                title = "فرمول‌های دان",
                                value = totalFeedsCount.toPersianDigits(),
                                icon = Icons.Default.Grain,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniCard(
                                title = "مکمل و پرمیکس",
                                value = totalSupplementsCount.toPersianDigits(),
                                icon = Icons.Default.Medication,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniCard(
                                title = "میانگین قیمت دان",
                                value = if (avgFeedPrice > 0) "${formatMoneyFa(avgFeedPrice)} ت" else "—",
                                icon = Icons.Default.MonetizationOn,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                    }
                }
            }

            // نوار جستجو و فیلترها
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("جستجو در نام فرمول، پرنده یا مواد تشکیل‌دهنده...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val isSelected = selectedTypeFilter == "ALL"
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTypeFilter = "ALL" },
                                label = {
                                    Text(
                                        text = "همه فرمول‌ها (${rationPlans.size.toPersianDigits()})",
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                        item {
                            val isSelected = selectedTypeFilter == "FEED"
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTypeFilter = "FEED" },
                                label = {
                                    Text(
                                        text = "دان کامل (${totalFeedsCount.toPersianDigits()})",
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Grain,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                        item {
                            val isSelected = selectedTypeFilter == "SUPPLEMENT"
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTypeFilter = "SUPPLEMENT" },
                                label = {
                                    Text(
                                        text = "مکمل و پرمیکس (${totalSupplementsCount.toPersianDigits()})",
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Medication,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }
                }
            }

            // لیست کارت‌های جیره
            if (filteredPlans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "هیچ فرمولی با این مشخصات یافت نشد." else "هنوز فرمول جیره‌ای ثبت نکرده‌اید.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FilledTonalButton(
                                onClick = {
                                    planToEdit = null
                                    showCreateEditDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ایجاد اولین فرمول جیره")
                            }
                        }
                    }
                }
            } else {
                items(filteredPlans, key = { it.id }) { plan ->
                    RationPlanCard(
                        plan = plan,
                        onViewDetails = { planForDetailsView = plan },
                        onEdit = {
                            planToEdit = plan
                            showCreateEditDialog = true
                        },
                        onDuplicate = { onDuplicatePlan(plan) },
                        onOpenBatchCalculator = { planForBatchCalculator = plan },
                        onAddToWarehouse = { planForWarehouseTransfer = plan },
                        onDelete = { planToDelete = plan }
                    )
                }
            }
        }
    }

    // ==========================================
    // دیالوگ‌ها
    // ==========================================

    // ۱. دیالوگ ساخت یا ویرایش جیره
    if (showCreateEditDialog) {
        RationFormulationDialog(
            initialPlan = planToEdit,
            customFeedIngredients = customFeedIngredients,
            onAddCustomIngredient = onAddCustomIngredient,
            onDeleteCustomIngredient = onDeleteCustomIngredient,
            onDismiss = { showCreateEditDialog = false },
            onSave = { savedPlan ->
                if (savedPlan.id == 0L) {
                    onAddPlan(savedPlan)
                    Toast.makeText(context, "فرمولاسیون «${savedPlan.name}» با موفقیت ذخیره شد.", Toast.LENGTH_SHORT).show()
                } else {
                    onEditPlan(savedPlan)
                    Toast.makeText(context, "تغییرات با موفقیت ذخیره شد.", Toast.LENGTH_SHORT).show()
                }
                showCreateEditDialog = false
            }
        )
    }

    // ۲. دیالوگ محاسبه تناژ ساخت برای میکسر
    if (planForBatchCalculator != null) {
        MixerBatchCalculatorDialog(
            plan = planForBatchCalculator!!,
            onDismiss = { planForBatchCalculator = null }
        )
    }

    // ۳. دیالوگ انتقال به انبار
    if (planForWarehouseTransfer != null) {
        AddRationToWarehouseDialog(
            plan = planForWarehouseTransfer!!,
            onDismiss = { planForWarehouseTransfer = null },
            onConfirm = { quantityKg, notes ->
                onAddToWarehouse(planForWarehouseTransfer!!, quantityKg, notes)
                Toast.makeText(context, "مقدار ${quantityKg.toPersianDigits()} کیلوگرم به انبار اضافه شد.", Toast.LENGTH_SHORT).show()
                planForWarehouseTransfer = null
            }
        )
    }

    // ۴. دیالوگ مشاهده جزئیات کامل فرمولاسیون
    if (planForDetailsView != null) {
        RationDetailsSheetDialog(
            plan = planForDetailsView!!,
            onDismiss = { planForDetailsView = null },
            onEdit = {
                planToEdit = planForDetailsView
                planForDetailsView = null
                showCreateEditDialog = true
            },
            onOpenBatch = {
                val p = planForDetailsView!!
                planForDetailsView = null
                planForBatchCalculator = p
            }
        )
    }

    // ۵. دیالوگ تایید حذف
    if (planToDelete != null) {
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text("حذف فرمولاسیون جیره", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف فرمول «${planToDelete!!.name}» اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlan(planToDelete!!.id)
                        planToDelete = null
                        Toast.makeText(context, "فرمول حذف شد.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

// ==========================================
// کارت نمایش جیره
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RationPlanCard(
    plan: RationPlan,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onOpenBatchCalculator: () -> Unit,
    onAddToWarehouse: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // سطر ۱: نشان‌های متادیتا (نوع، پرنده و مرحله) در کنار دکمه گزینه‌ها
            val (typeBg, typeColor, typeIcon, typeLabel) = if (plan.itemType == RationItemType.FEED) {
                Quad(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    MaterialTheme.colorScheme.primary,
                    Icons.Default.Grain,
                    "دان کامل"
                )
            } else {
                Quad(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    MaterialTheme.colorScheme.secondary,
                    Icons.Default.Medication,
                    "مکمل / کنسانتره"
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = typeBg,
                    border = BorderStroke(1.dp, typeColor.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                val birdLabel = getBirdLabelFa(plan.targetBird)
                if (birdLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$birdLabel (${plan.targetStage})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // منوی سه نقطه یا گزینه‌ها
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "گزینه‌ها", modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("ویرایش فرمول") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تکثیر (کپی)") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("محاسبه بار میکسر") },
                            leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenBatchCalculator()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ثبت در انبار") },
                            leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAddToWarehouse()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("حذف فرمول", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // سطر ۲: نام کامل فرمولاسیون (عرض ۱۰۰٪، بدون بریدگی و بدون سه نقطه، هرچقدر طولانی باشد کامل خوانده می‌شود)
            Text(
                text = plan.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // بخش هزینه و شاخص‌های تغذیه‌ای
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "قیمت تمام‌شده هر کیلوگرم:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (plan.effectivePricePerKg > 0) formatMoneyFa(plan.effectivePricePerKg) else "تعیین نشده",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (plan.effectivePricePerKg > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تومان",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // درصد پروتئین تخمینی و روش محاسبه
                    Column(horizontalAlignment = Alignment.End) {
                        plan.estimatedCrudeProteinPercent?.let { cp ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "پروتئین خام: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${formatDecimalFa(cp)}٪ CP",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        val pricingLabel = if (plan.pricingMode == RationPricingMode.AUTO_FROM_INGREDIENTS) {
                            "محاسبه از ${plan.ingredients.size.toPersianDigits()} قلم"
                        } else {
                            "قیمت دستی کلی"
                        }
                        Text(
                            text = pricingLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // نوار نشانگر توازن درصد جیره (۱۰۰٪)
            val totalPct = plan.totalPercentage
            val isBalanced = plan.isBalanced100Percent
            val gaugeColor = if (isBalanced) Color(0xFF2E7D32) else Color(0xFFE65100)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "توازن فرمول: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatDecimalFa(totalPct)}٪ از ۱۰۰٪",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                LinearProgressIndicator(
                    progress = { (totalPct / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = gaugeColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (isBalanced) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "متعادل",
                        tint = gaugeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // پیش‌نمایش اقلام جیره با چیدمان چندسطری روان (FlowRow) بدون کوچکترین کوتاه‌شدگی یا حذف نام
            if (plan.ingredients.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    plan.ingredients.forEach { ing ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ing.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${formatDecimalFa(ing.percentageOrAmount)}٪",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // دکمه‌های سریع عملیات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("جزئیات و آنالیز", style = MaterialTheme.typography.labelMedium)
                }

                FilledTonalButton(
                    onClick = onOpenBatchCalculator,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("محاسبه میکسر", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ==========================================
// دیالوگ حرفه‌ای ساخت و ویرایش جیره
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RationFormulationDialog(
    initialPlan: RationPlan?,
    customFeedIngredients: List<PoultryFeedRawItem> = emptyList(),
    onAddCustomIngredient: ((PoultryFeedRawItem) -> Unit)? = null,
    onDeleteCustomIngredient: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (RationPlan) -> Unit
) {
    val isEdit = initialPlan != null
    var name by rememberSaveable { mutableStateOf(initialPlan?.name ?: "") }
    var itemType by rememberSaveable { mutableStateOf(initialPlan?.itemType ?: RationItemType.FEED) }
    var targetBird by rememberSaveable { mutableStateOf(initialPlan?.targetBird ?: "BROILER") }
    var targetStage by rememberSaveable { mutableStateOf(initialPlan?.targetStage ?: "استارتر") }
    var pricingMode by rememberSaveable { mutableStateOf(initialPlan?.pricingMode ?: RationPricingMode.AUTO_FROM_INGREDIENTS) }
    var manualPriceText by rememberSaveable { mutableStateOf(if (initialPlan != null && initialPlan.manualPricePerKg > 0) initialPlan.manualPricePerKg.toLong().toString() else "") }
    var notes by rememberSaveable { mutableStateOf(initialPlan?.notes ?: "") }

    // اقلام فرمولاسیون
    var ingredients by remember {
        mutableStateOf(initialPlan?.ingredients ?: emptyList())
    }

    var showAddPredefinedSheet by remember { mutableStateOf(false) }
    var showCustomItemDialog by remember { mutableStateOf(false) }

    // محاسبات لحظه‌ای و آنالیز کامل جیره
    val totalPercentage = remember(ingredients) {
        ingredients.sumOf { it.percentageOrAmount }
    }
    val autoCalculatedPricePerKg = remember(ingredients, totalPercentage) {
        val totalKg = if (totalPercentage > 0) totalPercentage else 100.0
        val totalCost = ingredients.sumOf { it.percentageOrAmount * it.pricePerKg }
        if (totalKg > 0) totalCost / totalKg else 0.0
    }
    val estimatedCrudeProtein = remember(ingredients, totalPercentage) {
        val valid = ingredients.filter { it.crudeProteinPercent != null && (it.crudeProteinPercent ?: 0.0) > 0 }
        if (valid.isNotEmpty() && totalPercentage > 0) {
            valid.sumOf { it.percentageOrAmount * (it.crudeProteinPercent ?: 0.0) } / totalPercentage
        } else null
    }
    val estimatedEnergy = remember(ingredients, totalPercentage) {
        val valid = ingredients.filter { it.energyKcalKg != null && (it.energyKcalKg ?: 0.0) > 0 }
        if (valid.isNotEmpty() && totalPercentage > 0) {
            valid.sumOf { it.percentageOrAmount * (it.energyKcalKg ?: 0.0) } / totalPercentage
        } else null
    }
    val estimatedCalcium = remember(ingredients, totalPercentage) {
        val valid = ingredients.filter { it.calciumPercent != null && (it.calciumPercent ?: 0.0) > 0 }
        if (valid.isNotEmpty() && totalPercentage > 0) {
            valid.sumOf { it.percentageOrAmount * (it.calciumPercent ?: 0.0) } / totalPercentage
        } else null
    }
    val estimatedPhosphorus = remember(ingredients, totalPercentage) {
        val valid = ingredients.filter { it.availablePhosphorusPercent != null && (it.availablePhosphorusPercent ?: 0.0) > 0 }
        if (valid.isNotEmpty() && totalPercentage > 0) {
            valid.sumOf { it.percentageOrAmount * (it.availablePhosphorusPercent ?: 0.0) } / totalPercentage
        } else null
    }
    val estimatedMethionine = remember(ingredients, totalPercentage) {
        val valid = ingredients.filter { it.methioninePercent != null && (it.methioninePercent ?: 0.0) > 0 }
        if (valid.isNotEmpty() && totalPercentage > 0) {
            valid.sumOf { it.percentageOrAmount * (it.methioninePercent ?: 0.0) } / totalPercentage
        } else null
    }
    val estimatedLysine = remember(ingredients, totalPercentage) {
        val valid = ingredients.filter { it.lysinePercent != null && (it.lysinePercent ?: 0.0) > 0 }
        if (valid.isNotEmpty() && totalPercentage > 0) {
            valid.sumOf { it.percentageOrAmount * (it.lysinePercent ?: 0.0) } / totalPercentage
        } else null
    }
    val estimatedSodium = remember(ingredients, totalPercentage) {
        val valid = ingredients.filter { it.sodiumPercent != null && (it.sodiumPercent ?: 0.0) > 0 }
        if (valid.isNotEmpty() && totalPercentage > 0) {
            valid.sumOf { it.percentageOrAmount * (it.sodiumPercent ?: 0.0) } / totalPercentage
        } else null
    }
    val estimatedFiber = remember(ingredients, totalPercentage) {
        val valid = ingredients.filter { it.crudeFiberPercent != null && (it.crudeFiberPercent ?: 0.0) > 0 }
        if (valid.isNotEmpty() && totalPercentage > 0) {
            valid.sumOf { it.percentageOrAmount * (it.crudeFiberPercent ?: 0.0) } / totalPercentage
        } else null
    }
    val estimatedFat = remember(ingredients, totalPercentage) {
        val valid = ingredients.filter { it.fatPercent != null && (it.fatPercent ?: 0.0) > 0 }
        if (valid.isNotEmpty() && totalPercentage > 0) {
            valid.sumOf { it.percentageOrAmount * (it.fatPercent ?: 0.0) } / totalPercentage
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEdit) "ویرایش فرمول جیره" else "فرمولاسیون جدید جیره و مکمل",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "بستن")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (name.isBlank()) return@TextButton
                                val manualPrice = manualPriceText.toDoubleOrNull() ?: 0.0
                                val finalPlan = RationPlan(
                                    id = initialPlan?.id ?: 0L,
                                    name = name.trim(),
                                    itemType = itemType,
                                    targetBird = targetBird,
                                    targetStage = targetStage.trim(),
                                    pricingMode = pricingMode,
                                    manualPricePerKg = manualPrice,
                                    batchBaseKg = 100.0,
                                    ingredients = ingredients,
                                    notes = notes.trim().ifBlank { null },
                                    createdAtMillis = initialPlan?.createdAtMillis ?: System.currentTimeMillis(),
                                    updatedAtMillis = System.currentTimeMillis()
                                )
                                onSave(finalPlan)
                            },
                            enabled = name.isNotBlank() && ingredients.isNotEmpty()
                        ) {
                            Text("ذخیره", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // بخش ۱: مشخصات پایه
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "۱. مشخصات کلی جیره",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("نام جیره یا مکمل *") },
                                placeholder = { Text("مثال: دان رشد جوجه گوشتی فاز ۲") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // انتخاب نوع: دان یا مکمل
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = itemType == RationItemType.FEED,
                                    onClick = { itemType = RationItemType.FEED },
                                    label = { Text("دان و خوراک کامل") },
                                    leadingIcon = { Icon(Icons.Default.Grain, contentDescription = null) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = itemType == RationItemType.SUPPLEMENT,
                                    onClick = { itemType = RationItemType.SUPPLEMENT },
                                    label = { Text("مکمل و کنسانتره") },
                                    leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // گونه پرنده هدف
                            Column {
                                Text("گونه پرنده هدف:", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        FilterChip(
                                            selected = targetBird == "ALL",
                                            onClick = { targetBird = "ALL" },
                                            label = { Text("همه پرندگان") }
                                        )
                                    }
                                    items(GuideBird.entries) { bird ->
                                        FilterChip(
                                            selected = targetBird == bird.id,
                                            onClick = { targetBird = bird.id },
                                            label = { Text(bird.titleFa) }
                                        )
                                    }
                                }
                            }

                            // مرحله پرورش
                            OutlinedTextField(
                                value = targetStage,
                                onValueChange = { targetStage = it },
                                label = { Text("مرحله یا فاز پرورش") },
                                placeholder = { Text("مثال: استارتر، رشد، پیش‌دان، تخم‌گذاری...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // بخش ۲: شیوه قیمت‌گذاری جیره
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "۲. شیوه محاسبه قیمت هر کیلوگرم",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = pricingMode == RationPricingMode.AUTO_FROM_INGREDIENTS,
                                    onClick = { pricingMode = RationPricingMode.AUTO_FROM_INGREDIENTS },
                                    label = { Text("محاسبه هوشمند از اقلام") },
                                    leadingIcon = { Icon(Icons.Default.AutoMode, contentDescription = null) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = pricingMode == RationPricingMode.MANUAL_FLAT,
                                    onClick = { pricingMode = RationPricingMode.MANUAL_FLAT },
                                    label = { Text("قیمت دستی کلی") },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (pricingMode == RationPricingMode.AUTO_FROM_INGREDIENTS) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "قیمت تمام‌شده محاسبه‌شده هر کیلو:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (autoCalculatedPricePerKg > 0) "${formatMoneyFa(autoCalculatedPricePerKg)} تومان" else "هنوز قیمتی ثبت نشده",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Icon(
                                            Icons.Default.Calculate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = manualPriceText,
                                    onValueChange = { manualPriceText = it.filter { c -> c.isDigit() } },
                                    label = { Text("قیمت مستقیم هر کیلوگرم (تومان)") },
                                    placeholder = { Text("مثال: ۲۲۰۰۰") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // بخش ۳: اقلام و فرمولاسیون
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "۳. مواد تشکیل‌دهنده جیره (${ingredients.size.toPersianDigits()} قلم)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "درصد وزنی و قیمت هر کیلو از مواد را مشخص کنید",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // نوار توازن ۱۰۰٪
                                val is100 = Math.abs(totalPercentage - 100.0) < 0.5
                                Surface(
                                    color = if (is100) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFE65100).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "جمع: ${formatDecimalFa(totalPercentage)}٪",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (is100) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // دکمه‌های افزودن مواد
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { showAddPredefinedSheet = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("کاتالوگ اقلام آماده", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = { showCustomItemDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ماده دلخواه دستی", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // کارت آنالیز تغذیه‌ای زنده جیره
                            if (ingredients.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚡ آنالیز تغذیه‌ای زنده جیره (در ۱۰۰ کیلوگرم):",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "قیمت هر کیلو: ${formatMoneyFa(autoCalculatedPricePerKg)} ت",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            NutrientMetricBadge(
                                                label = "انرژی ME",
                                                value = if (estimatedEnergy != null) "${formatDecimalFa(estimatedEnergy, 0)} kcal" else "-",
                                                modifier = Modifier.weight(1f)
                                            )
                                            NutrientMetricBadge(
                                                label = "پروتئین خام",
                                                value = if (estimatedCrudeProtein != null) "${formatDecimalFa(estimatedCrudeProtein, 1)}٪" else "-",
                                                modifier = Modifier.weight(1f)
                                            )
                                            NutrientMetricBadge(
                                                label = "کلسیم Ca",
                                                value = if (estimatedCalcium != null) "${formatDecimalFa(estimatedCalcium, 2)}٪" else "-",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            NutrientMetricBadge(
                                                label = "فسفر قابل جذب",
                                                value = if (estimatedPhosphorus != null) "${formatDecimalFa(estimatedPhosphorus, 2)}٪" else "-",
                                                modifier = Modifier.weight(1f)
                                            )
                                            NutrientMetricBadge(
                                                label = "متیونین Met",
                                                value = if (estimatedMethionine != null) "${formatDecimalFa(estimatedMethionine, 2)}٪" else "-",
                                                modifier = Modifier.weight(1f)
                                            )
                                            NutrientMetricBadge(
                                                label = "لیزین Lys",
                                                value = if (estimatedLysine != null) "${formatDecimalFa(estimatedLysine, 2)}٪" else "-",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            NutrientMetricBadge(
                                                label = "سدیم Na",
                                                value = if (estimatedSodium != null) "${formatDecimalFa(estimatedSodium, 2)}٪" else "-",
                                                modifier = Modifier.weight(1f)
                                            )
                                            NutrientMetricBadge(
                                                label = "فیبر خام CF",
                                                value = if (estimatedFiber != null) "${formatDecimalFa(estimatedFiber, 1)}٪" else "-",
                                                modifier = Modifier.weight(1f)
                                            )
                                            NutrientMetricBadge(
                                                label = "چربی خام EE",
                                                value = if (estimatedFat != null) "${formatDecimalFa(estimatedFat, 1)}٪" else "-",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // لیست اقلام وارد شده با قابلیت ویرایش درصد و قیمت
                            if (ingredients.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "هنوز ماده‌ای به این جیره اضافه نشده است. از دکمه‌های بالا برای افزودن اقلام خوراک (مانند ذرت، سویا، روغن، مکمل و...) استفاده کنید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ingredients.forEachIndexed { index, ingredient ->
                                        EditableIngredientRow(
                                            ingredient = ingredient,
                                            onUpdate = { updated ->
                                                val list = ingredients.toMutableList()
                                                list[index] = updated
                                                ingredients = list
                                            },
                                            onDelete = {
                                                ingredients = ingredients.filterIndexed { i, _ -> i != index }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // بخش ۴: یادداشت‌ها
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("یادداشت و توصیه‌های مصرف جیره") },
                        placeholder = { Text("مثال: دان‌پاش‌ها هر روز ۳ بار چک شود...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
            }
        }
    }

    // شیت انتخاب از کاتالوگ جامع خوراک و ریزمغذی‌ها
    if (showAddPredefinedSheet) {
        PredefinedIngredientsCatalogDialog(
            customFeedIngredients = customFeedIngredients,
            onAddCustomIngredient = onAddCustomIngredient,
            onDeleteCustomIngredient = onDeleteCustomIngredient,
            onDismiss = { showAddPredefinedSheet = false },
            onSelect = { selectedItem ->
                // بررسی تکراری نبودن
                if (ingredients.none { it.name.trim().equals(selectedItem.name.trim(), ignoreCase = true) }) {
                    ingredients = ingredients + RationIngredient(
                        name = selectedItem.name,
                        percentageOrAmount = 5.0, // مقدار پیشنهادی اولیه
                        pricePerKg = selectedItem.defaultPricePerKg,
                        crudeProteinPercent = selectedItem.crudeProtein,
                        category = selectedItem.category.titleFa,
                        energyKcalKg = selectedItem.energyKcalKg,
                        calciumPercent = selectedItem.calcium,
                        availablePhosphorusPercent = selectedItem.availablePhosphorus,
                        methioninePercent = selectedItem.methionine,
                        lysinePercent = selectedItem.lysine,
                        sodiumPercent = selectedItem.sodium,
                        crudeFiberPercent = selectedItem.crudeFiber,
                        fatPercent = selectedItem.fat
                    )
                }
                showAddPredefinedSheet = false
            }
        )
    }

    // دیالوگ افزودن قلم دلخواه دستی با امکان تعریف و ذخیره مقادیر
    if (showCustomItemDialog) {
        AddCustomFeedIngredientDialog(
            onDismiss = { showCustomItemDialog = false },
            onSave = { customItem ->
                onAddCustomIngredient?.invoke(customItem)
                ingredients = ingredients + RationIngredient(
                    name = customItem.name,
                    percentageOrAmount = 5.0,
                    pricePerKg = customItem.defaultPricePerKg,
                    crudeProteinPercent = customItem.crudeProtein,
                    category = customItem.category.titleFa,
                    energyKcalKg = customItem.energyKcalKg,
                    calciumPercent = customItem.calcium,
                    availablePhosphorusPercent = customItem.availablePhosphorus,
                    methioninePercent = customItem.methionine,
                    lysinePercent = customItem.lysine,
                    sodiumPercent = customItem.sodium,
                    crudeFiberPercent = customItem.crudeFiber,
                    fatPercent = customItem.fat
                )
                showCustomItemDialog = false
            }
        )
    }
}

// نشانگر ریزمغذی در آنالیز زنده جیره
@Composable
fun NutrientMetricBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

// سطر ویرایش یک ماده در جیره با نمایش ویژگی‌های تغذیه‌ای
@Composable
fun EditableIngredientRow(
    ingredient: RationIngredient,
    onUpdate: (RationIngredient) -> Unit,
    onDelete: () -> Unit
) {
    var pctText by rememberSaveable(ingredient.id) { mutableStateOf(ingredient.percentageOrAmount.toString().removeSuffix(".0")) }
    var priceText by rememberSaveable(ingredient.id) { mutableStateOf(if (ingredient.pricePerKg > 0) ingredient.pricePerKg.toLong().toString() else "") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ingredient.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ingredient.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "حذف قلم",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // نمایش مشخصات تغذیه‌ای ماده
            val nutrientParts = mutableListOf<String>()
            ingredient.energyKcalKg?.let { if (it > 0) nutrientParts.add("⚡ ${formatDecimalFa(it, 0)} kcal") }
            ingredient.crudeProteinPercent?.let { if (it > 0) nutrientParts.add("🥩 پروتئین: ${formatDecimalFa(it, 1)}٪") }
            ingredient.calciumPercent?.let { if (it > 0) nutrientParts.add("🦴 کلسیم: ${formatDecimalFa(it, 2)}٪") }
            ingredient.availablePhosphorusPercent?.let { if (it > 0) nutrientParts.add("🧪 فسفر: ${formatDecimalFa(it, 2)}٪") }
            ingredient.methioninePercent?.let { if (it > 0) nutrientParts.add("🧬 متیونین: ${formatDecimalFa(it, 2)}٪") }
            ingredient.lysinePercent?.let { if (it > 0) nutrientParts.add("🧬 لیزین: ${formatDecimalFa(it, 2)}٪") }

            if (nutrientParts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nutrientParts.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // فیلد درصد
                OutlinedTextField(
                    value = pctText,
                    onValueChange = { input ->
                        pctText = input
                        val p = input.toDoubleOrNull() ?: 0.0
                        onUpdate(ingredient.copy(percentageOrAmount = p))
                    },
                    label = { Text("درصد (٪)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // فیلد قیمت هر کیلوگرم
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input ->
                        priceText = input.filter { it.isDigit() }
                        val pr = priceText.toDoubleOrNull() ?: 0.0
                        onUpdate(ingredient.copy(pricePerKg = pr))
                    },
                    label = { Text("قیمت هر کیلو (تومان)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.4f),
                    singleLine = true
                )
            }

            // سهم در هزینه نهایی
            val costContribution = (ingredient.percentageOrAmount * ingredient.pricePerKg) / 100.0
            if (costContribution > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "سهم در قیمت نهایی هر کیلو جیره: ${formatMoneyFa(costContribution)} تومان",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ==========================================
// کاتالوگ جامع اقلام خوراک و ریزمغذی‌ها
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredefinedIngredientsCatalogDialog(
    customFeedIngredients: List<PoultryFeedRawItem> = emptyList(),
    onAddCustomIngredient: ((PoultryFeedRawItem) -> Unit)? = null,
    onDeleteCustomIngredient: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSelect: (PoultryFeedRawItem) -> Unit
) {
    var searchCatQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf<FeedCategory?>(null) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PoultryFeedRawItem?>(null) }

    val allCatalogItems = remember(customFeedIngredients) {
        customFeedIngredients + ComprehensiveFeedCatalog.ITEMS
    }

    val filtered = remember(searchCatQuery, selectedCategoryFilter, allCatalogItems) {
        allCatalogItems.filter { item ->
            val matchesCategory = selectedCategoryFilter == null || item.category == selectedCategoryFilter
            val matchesQuery = searchCatQuery.isBlank() ||
                item.name.contains(searchCatQuery, ignoreCase = true) ||
                item.category.titleFa.contains(searchCatQuery, ignoreCase = true) ||
                item.description.contains(searchCatQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 720.dp)
                .fillMaxHeight(0.92f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // هدر
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "کاتالوگ جامع اقلام خوراک و ریزمغذی‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${allCatalogItems.size.toPersianDigits()} قلم خوراک با آنالیز آزمایشگاهی دقیق",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // جستجو و دکمه افزودن ماده سفارشی
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchCatQuery,
                        onValueChange = { searchCatQuery = it },
                        placeholder = { Text("جستجو در نام یا ویژگی‌های اقلام...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchCatQuery.isNotBlank()) {
                                IconButton(onClick = { searchCatQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { showAddCustomDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ماده جدید", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // تب‌های دسته‌بندی خوراک
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("همه (${allCatalogItems.size.toPersianDigits()})", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    items(FeedCategory.entries) { cat ->
                        val count = allCatalogItems.count { it.category == cat }
                        if (count > 0 || cat == FeedCategory.CUSTOM) {
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                leadingIcon = {
                                    Image(
                                        painter = painterResource(id = cat.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("${cat.titleFa} (${count.toPersianDigits()})", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // فهرست اقلام خوراک با تصاویر اختصاصی و ریزمغذی‌ها
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "موردی مطابق با جستجوی شما یافت نشد. می‌توانید با زدن دکمه «ماده جدید» آن را اضافه کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { item ->
                            CatalogFeedItemCard(
                                item = item,
                                onSelect = { onSelect(item) },
                                onDelete = if (item.isCustom && onDeleteCustomIngredient != null) {
                                    { itemToDelete = item }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomDialog) {
        AddCustomFeedIngredientDialog(
            onDismiss = { showAddCustomDialog = false },
            onSave = { newItem ->
                onAddCustomIngredient?.invoke(newItem)
                showAddCustomDialog = false
            }
        )
    }

    itemToDelete?.let { deletingItem ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("حذف ماده سفارشی") },
            text = { Text("آیا از حذف ماده سفارشی «${deletingItem.name}» از کاتالوگ اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomIngredient?.invoke(deletingItem.id)
                        itemToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

// کارت اختصاصی نمایش هر قلم در کاتالوگ با تصویر و کلیه مقادیر ریزمغذی
@Composable
private fun CatalogFeedItemCard(
    item: PoultryFeedRawItem,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        onClick = onSelect,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // آیکون وکتور اختصاصی ماده
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.name,
                            modifier = Modifier.size(30.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.isCustom) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "سفارشی",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = item.category.titleFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatMoneyFa(item.defaultPricePerKg)} ت",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "هر کیلوگرم",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف ماده سفارشی",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // شبکه مقادیر ریزمغذی‌ها
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CatalogMiniSpecBadge(label = "انرژی", value = "${formatDecimalFa(item.energyKcalKg, 0)} kcal", modifier = Modifier.weight(1f))
                CatalogMiniSpecBadge(label = "پروتئین", value = "${formatDecimalFa(item.crudeProtein)}٪", modifier = Modifier.weight(1f))
                CatalogMiniSpecBadge(label = "کلسیم", value = "${formatDecimalFa(item.calcium, 2)}٪", modifier = Modifier.weight(1f))
                CatalogMiniSpecBadge(label = "فسفر", value = "${formatDecimalFa(item.availablePhosphorus, 2)}٪", modifier = Modifier.weight(1f))
                CatalogMiniSpecBadge(label = "متیونین", value = "${formatDecimalFa(item.methionine, 2)}٪", modifier = Modifier.weight(1f))
                CatalogMiniSpecBadge(label = "لیزین", value = "${formatDecimalFa(item.lysine, 2)}٪", modifier = Modifier.weight(1f))
            }

            if (item.crudeFiber > 0 || item.fat > 0 || item.sodium > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CatalogMiniSpecBadge(label = "سدیم Na", value = "${formatDecimalFa(item.sodium, 2)}٪", modifier = Modifier.weight(1f))
                    CatalogMiniSpecBadge(label = "فیبر خام", value = "${formatDecimalFa(item.crudeFiber, 1)}٪", modifier = Modifier.weight(1f))
                    CatalogMiniSpecBadge(label = "چربی خام", value = "${formatDecimalFa(item.fat, 1)}٪", modifier = Modifier.weight(1f))
                }
            }

            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CatalogMiniSpecBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(text = value, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        }
    }
}

// دیالوگ افزودن قلم دلخواه سفارشی با کلیه مقادیر ریزمغذی
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomFeedIngredientDialog(
    onDismiss: () -> Unit,
    onSave: (PoultryFeedRawItem) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(FeedCategory.CUSTOM) }
    var priceText by rememberSaveable { mutableStateOf("") }
    var energyText by rememberSaveable { mutableStateOf("") }
    var proteinText by rememberSaveable { mutableStateOf("") }
    var calciumText by rememberSaveable { mutableStateOf("") }
    var phosphorusText by rememberSaveable { mutableStateOf("") }
    var methionineText by rememberSaveable { mutableStateOf("") }
    var lysineText by rememberSaveable { mutableStateOf("") }
    var sodiumText by rememberSaveable { mutableStateOf("") }
    var fiberText by rememberSaveable { mutableStateOf("") }
    var fatText by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 640.dp)
                .fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // هدر
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_feed_custom),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "افزودن ماده خوراکی سفارشی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "تعریف مشخصات تغذیه‌ای جهت ثبت در کاتالوگ و جیره‌سازی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // نام و قیمت
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام ماده خوراکی *") },
                        placeholder = { Text("مثال: پودر گوشت خالص، تفاله چغندر...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                        label = { Text("قیمت هر کیلو (تومان)") },
                        placeholder = { Text("مثال: ۲۲۰۰۰") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // انتخاب دسته‌بندی خوراک
                    Text(
                        text = "دسته‌بندی خوراک:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(FeedCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                leadingIcon = {
                                    Image(
                                        painter = painterResource(id = cat.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(cat.titleFa, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // هدر آنالیز تغذیه‌ای
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "مقادیر انرژی و ریزمغذی‌ها در هر کیلوگرم ماده (بر اساس برگه آزمایشگاه یا کاتالوگ):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    // انرژی و پروتئین
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = energyText,
                            onValueChange = { energyText = it },
                            label = { Text("انرژی (kcal/kg)") },
                            placeholder = { Text("مثال: ۲۹۵۰") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = proteinText,
                            onValueChange = { proteinText = it },
                            label = { Text("پروتئین خام (CP ٪)") },
                            placeholder = { Text("مثال: ۲۸.۵") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // کلسیم و فسفر
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = calciumText,
                            onValueChange = { calciumText = it },
                            label = { Text("کلسیم (Ca ٪)") },
                            placeholder = { Text("مثال: ۱.۲") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = phosphorusText,
                            onValueChange = { phosphorusText = it },
                            label = { Text("فسفر قابل جذب (٪)") },
                            placeholder = { Text("مثال: ۰.۴۵") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // متیونین و لیزین
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = methionineText,
                            onValueChange = { methionineText = it },
                            label = { Text("متیونین (Met ٪)") },
                            placeholder = { Text("مثال: ۰.۵۵") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = lysineText,
                            onValueChange = { lysineText = it },
                            label = { Text("لیزین (Lys ٪)") },
                            placeholder = { Text("مثال: ۱.۴") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // سدیم، فیبر، چربی
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sodiumText,
                            onValueChange = { sodiumText = it },
                            label = { Text("سدیم (Na ٪)") },
                            placeholder = { Text("مثال: ۰.۱۵") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = fiberText,
                            onValueChange = { fiberText = it },
                            label = { Text("فیبر خام (CF ٪)") },
                            placeholder = { Text("مثال: ۴.۰") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = fatText,
                            onValueChange = { fatText = it },
                            label = { Text("چربی خام (EE ٪)") },
                            placeholder = { Text("مثال: ۵.۰") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // توضیحات
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("توضیحات و نکات کاربردی (اختیاری)") },
                        placeholder = { Text("منبع تهیه، شرایط نگهداری، محدودیت مصرف...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // دکمه‌های اقدام
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("انصراف")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) return@Button
                            val item = PoultryFeedRawItem(
                                name = name.trim(),
                                category = selectedCategory,
                                energyKcalKg = energyText.toDoubleOrNull() ?: 0.0,
                                crudeProtein = proteinText.toDoubleOrNull() ?: 0.0,
                                calcium = calciumText.toDoubleOrNull() ?: 0.0,
                                availablePhosphorus = phosphorusText.toDoubleOrNull() ?: 0.0,
                                methionine = methionineText.toDoubleOrNull() ?: 0.0,
                                lysine = lysineText.toDoubleOrNull() ?: 0.0,
                                sodium = sodiumText.toDoubleOrNull() ?: 0.0,
                                crudeFiber = fiberText.toDoubleOrNull() ?: 0.0,
                                fat = fatText.toDoubleOrNull() ?: 0.0,
                                defaultPricePerKg = priceText.toDoubleOrNull() ?: 0.0,
                                description = description.trim(),
                                iconRes = selectedCategory.iconRes,
                                isCustom = true
                            )
                            onSave(item)
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ذخیره در کاتالوگ")
                    }
                }
            }
        }
    }
}

// برای سازگاری عقب‌رو
@Composable
fun CustomIngredientAddDialog(
    onDismiss: () -> Unit,
    onAdd: (RationIngredient) -> Unit
) {
    AddCustomFeedIngredientDialog(
        onDismiss = onDismiss,
        onSave = { item ->
            onAdd(
                RationIngredient(
                    name = item.name,
                    percentageOrAmount = 5.0,
                    pricePerKg = item.defaultPricePerKg,
                    crudeProteinPercent = item.crudeProtein,
                    category = item.category.titleFa,
                    energyKcalKg = item.energyKcalKg,
                    calciumPercent = item.calcium,
                    availablePhosphorusPercent = item.availablePhosphorus,
                    methioninePercent = item.methionine,
                    lysinePercent = item.lysine,
                    sodiumPercent = item.sodium,
                    crudeFiberPercent = item.crudeFiber,
                    fatPercent = item.fat
                )
            )
        }
    )
}

// ==========================================
// دیالوگ محاسبه تناژ ساخت برای میکسر مرغداری
// ==========================================

@Composable
fun MixerBatchCalculatorDialog(
    plan: RationPlan,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var totalBatchKgText by rememberSaveable { mutableStateOf("500") }
    val totalBatchKg = totalBatchKgText.toDoubleOrNull() ?: 500.0

    val totalPct = plan.totalPercentage
    val basePct = if (totalPct > 0) totalPct else 100.0

    val calculatedBatchCost = (totalBatchKg * plan.effectivePricePerKg)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .fillMaxHeight(0.88f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "محاسبه ساخت در میکسر",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // فیلد ورودی تناژ میکسر
                OutlinedTextField(
                    value = totalBatchKgText,
                    onValueChange = { totalBatchKgText = it.filter { c -> c.isDigit() } },
                    label = { Text("میزان کل دان مورد نیاز برای ساخت (کیلوگرم)") },
                    placeholder = { Text("مثال: ۵۰۰ یا ۲۰۰۰") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // کارت خلاصه هزینه بچ
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("هزینه کل این بار میکسر:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${formatMoneyFa(calculatedBatchCost)} تومان",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("قیمت هر کیلو دان:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${formatMoneyFa(plan.effectivePricePerKg)} تومان",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "وزن دقیق هر ماده برای ریختن در میکسر:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // جدول اقلام
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(plan.ingredients) { ing ->
                        val weightKg = (ing.percentageOrAmount / basePct) * totalBatchKg
                        val itemCost = weightKg * ing.pricePerKg

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ing.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "درصد فرمول: ${formatDecimalFa(ing.percentageOrAmount)}٪" +
                                                if (ing.pricePerKg > 0) " • هر کیلو: ${formatMoneyFa(ing.pricePerKg)} ت" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val weightDisplay = if (weightKg < 1.0) {
                                        "${(weightKg * 1000).toLong().toPersianDigits()} گرم"
                                    } else {
                                        "${formatDecimalFa(weightKg, 2)} کیلوگرم"
                                    }
                                    Text(
                                        text = weightDisplay,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (itemCost > 0) {
                                        Text(
                                            text = "${formatMoneyFa(itemCost)} ت",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // دکمه‌های کپی و اشتراک‌گذاری دستور میکسر
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val textBuilder = StringBuilder()
                            textBuilder.appendLine("📋 دستور ساخت جیره در میکسر: ${plan.name}")
                            textBuilder.appendLine("میزان کل بار: ${totalBatchKg.toPersianDigits()} کیلوگرم")
                            textBuilder.appendLine("قیمت تمام شده هر کیلو: ${formatMoneyFa(plan.effectivePricePerKg)} تومان")
                            textBuilder.appendLine("هزینه کل بچ: ${formatMoneyFa(calculatedBatchCost)} تومان")
                            textBuilder.appendLine("-------------------------")
                            plan.ingredients.forEach { ing ->
                                val wKg = (ing.percentageOrAmount / basePct) * totalBatchKg
                                val wStr = if (wKg < 1.0) "${(wKg * 1000).toLong()} گرم" else "${"%.2f".format(wKg)} کیلوگرم"
                                textBuilder.appendLine("• ${ing.name}: $wStr (${ing.percentageOrAmount}٪)")
                            }
                            clipboardManager.setText(AnnotatedString(textBuilder.toString()))
                            Toast.makeText(context, "دستور ساخت جیره کپی شد.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("کپی دستور")
                    }

                    Button(
                        onClick = {
                            val textBuilder = StringBuilder()
                            textBuilder.appendLine("📋 دستور ساخت جیره در میکسر: ${plan.name}")
                            textBuilder.appendLine("میزان کل بار: ${totalBatchKg.toPersianDigits()} کیلوگرم")
                            textBuilder.appendLine("-------------------------")
                            plan.ingredients.forEach { ing ->
                                val wKg = (ing.percentageOrAmount / basePct) * totalBatchKg
                                val wStr = if (wKg < 1.0) "${(wKg * 1000).toLong()} گرم" else "${"%.2f".format(wKg)} کیلوگرم"
                                textBuilder.appendLine("• ${ing.name}: $wStr")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "ارسال دستور ساخت میکسر"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اشتراک‌گذاری")
                    }
                }
            }
        }
    }
}

// ==========================================
// دیالوگ انتقال جیره به انبار
// ==========================================

@Composable
fun AddRationToWarehouseDialog(
    plan: RationPlan,
    onDismiss: () -> Unit,
    onConfirm: (quantityKg: Double, notes: String?) -> Unit
) {
    var quantityText by rememberSaveable { mutableStateOf("100") }
    var notesText by rememberSaveable { mutableStateOf("تولید شده از جیره‌ساز: ${plan.name}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت موجودی جیره در انبار", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "جیره «${plan.name}» با قیمت هر کیلوگرم ${formatMoneyFa(plan.effectivePricePerKg)} تومان به عنوان موجودی در انبار ذخیره خواهد شد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("مقدار موجودی آماده‌شده (کیلوگرم) *") },
                    placeholder = { Text("مثال: ۱۰۰ یا ۵۰۰") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("توضیحات انبار") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull() ?: 100.0
                    onConfirm(qty, notesText.trim().ifBlank { null })
                },
                enabled = quantityText.isNotBlank()
            ) {
                Text("ثبت در انبار")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

// ==========================================
// دیالوگ مشاهده جزئیات کامل فرمولاسیون
// ==========================================

@Composable
fun RationDetailsSheetDialog(
    plan: RationPlan,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onOpenBatch: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${plan.itemType.titleFa} • پرنده: ${getBirdLabelFa(plan.targetBird)} (${plan.targetStage})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // شاخص‌های کلیدی
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("قیمت تمام شده هر کیلو:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${formatMoneyFa(plan.effectivePricePerKg)} تومان",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        plan.estimatedCrudeProteinPercent?.let { cp ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("پروتئین خام:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${formatDecimalFa(cp)}٪",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("تعداد اقلام:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${plan.ingredients.size.toPersianDigits()} قلم",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // کارت آنالیز ارزش تغذیه‌ای و ریزمغذی‌ها
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "⚡ آنالیز ارزش تغذیه‌ای جیره (در هر ۱۰۰ کیلوگرم / درصد جیره):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            NutrientMetricBadge(
                                label = "انرژی ME",
                                value = plan.estimatedEnergyKcalKg?.let { "${formatDecimalFa(it, 0)} kcal" } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                            NutrientMetricBadge(
                                label = "پروتئین خام",
                                value = plan.estimatedCrudeProteinPercent?.let { "${formatDecimalFa(it, 1)}٪" } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                            NutrientMetricBadge(
                                label = "کلسیم Ca",
                                value = plan.estimatedCalciumPercent?.let { "${formatDecimalFa(it, 2)}٪" } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            NutrientMetricBadge(
                                label = "فسفر جذب",
                                value = plan.estimatedPhosphorusPercent?.let { "${formatDecimalFa(it, 2)}٪" } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                            NutrientMetricBadge(
                                label = "متیونین Met",
                                value = plan.estimatedMethioninePercent?.let { "${formatDecimalFa(it, 2)}٪" } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                            NutrientMetricBadge(
                                label = "لیزین Lys",
                                value = plan.estimatedLysinePercent?.let { "${formatDecimalFa(it, 2)}٪" } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            NutrientMetricBadge(
                                label = "سدیم Na",
                                value = plan.estimatedSodiumPercent?.let { "${formatDecimalFa(it, 2)}٪" } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                            NutrientMetricBadge(
                                label = "فیبر خام",
                                value = plan.estimatedFiberPercent?.let { "${formatDecimalFa(it, 1)}٪" } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                            NutrientMetricBadge(
                                label = "چربی خام",
                                value = plan.estimatedFatPercent?.let { "${formatDecimalFa(it, 1)}٪" } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "آنالیز و درصد تک‌تک اقلام در فرمول:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(plan.ingredients) { ing ->
                        val costPerKgOfFeed = (ing.percentageOrAmount * ing.pricePerKg) / 100.0
                        val costSharePct = if (plan.effectivePricePerKg > 0) {
                            (costPerKgOfFeed / plan.effectivePricePerKg) * 100.0
                        } else 0.0

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ing.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "قیمت هر کیلو: ${formatMoneyFa(ing.pricePerKg)} ت • سهم از هزینه جیره: ${formatDecimalFa(costSharePct)}٪",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${formatDecimalFa(ing.percentageOrAmount)}٪",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${formatMoneyFa(costPerKgOfFeed)} ت",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // یادداشت جیره
                plan.notes?.let { note ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "یادداشت: $note",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ویرایش")
                    }

                    Button(
                        onClick = onOpenBatch,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("محاسبه میکسر")
                    }
                }
            }
        }
    }
}

// کارت آماری کوچک
@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                AutoResizeText(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    targetFontSize = 10.sp,
                    minFontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            AutoResizeText(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                targetFontSize = 14.sp,
                minFontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
        }
    }
}

private fun getBirdLabelFa(birdId: String): String {
    return when (birdId) {
        "ALL" -> "همه پرندگان"
        "LAYER_HEN" -> "مرغ بومی / تخم‌گذار"
        "BROILER" -> "مرغ گوشتی"
        "TURKEY" -> "بوقلمون"
        "QUAIL" -> "بلدرچین"
        "DUCK" -> "اردک"
        "GOOSE" -> "غاز"
        else -> birdId
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
