package com.example.poultryapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.WarehouseCategory
import com.example.poultryapp.data.local.WarehouseItem
import com.example.poultryapp.engine.DailyNutritionEngine
import com.example.poultryapp.engine.FarmSeason
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.*
import com.example.poultryapp.ui.components.*
import java.util.Locale

@Composable
fun WarehouseScreen(
    warehouseItems: List<WarehouseItem>,
    flocks: List<Flock>,
    onAddItem: (WarehouseItem) -> Unit,
    onEditItem: (WarehouseItem) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onAdjustStock: (id: Long, delta: Double) -> Unit,
    onDepleteStock: (Long) -> Unit = {},
    onConsumeStock: (itemId: Long, amount: Double, flockIds: List<Long>, notes: String?) -> Unit = { _, _, _, _ -> },
    onSellEgg: (itemId: Long, count: Double, pricePerUnit: Double, notes: String?) -> Unit = { _, _, _, _ -> },
    customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList(),
    farmAmbientTemp: Double? = null,
    onOpenDrawer: () -> Unit,
    showHeader: Boolean = true,
    externalAddTrigger: Int = 0
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(externalAddTrigger) {
        if (externalAddTrigger > 0) {
            showAddDialog = true
        }
    }
    var editingItem by remember { mutableStateOf<WarehouseItem?>(null) }
    var deletingItemId by remember { mutableStateOf<Long?>(null) }
    var depletingItem by remember { mutableStateOf<WarehouseItem?>(null) }
    var sellingEggItem by remember { mutableStateOf<WarehouseItem?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<WarehouseCategory?>(null) }

    val currentSeason: FarmSeason = remember { DailyNutritionEngine.getCurrentSeason() }

    val flockRequirements = remember(flocks, currentSeason, customBreeds, farmAmbientTemp) {
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

    val filteredItems = remember(warehouseItems, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            warehouseItems
        } else {
            warehouseItems.filter { it.category == selectedCategoryFilter }
        }
    }

    val feedIconPainter = painterResource(id = com.example.poultryapp.R.drawable.ic_farm_feed)
    val warehouseIconPainter = painterResource(id = com.example.poultryapp.R.drawable.ic_farm_warehouse)

    Scaffold(
        topBar = {
            if (showHeader) {
                AppTopBar(
                    title = "مدیریت انبار و اقلام خوراک",
                    subtitle = "${toPersianDigits(warehouseItems.size)} قلم کالا در انبار فارم",
                    logoRes = com.example.poultryapp.R.drawable.ic_farm_warehouse,
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "منوی برنامه")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "افزودن کالا",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(FarmDimens.ButtonRadius)
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن کالا")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (warehouseItems.isEmpty()) {
                EmptyStateView(
                    imageRes = com.example.poultryapp.R.drawable.ic_farm_warehouse,
                    title = "انبار فارم خالی است",
                    message = "میزان دان، ویتامین‌ها، پروبیوتیک‌ها، داروها و مکمل‌های مصرفی را ثبت کنید تا سیستم مدت دوام خوراک را محاسبه کرده و هشدارهای کمبود را به موقع اعلام کند.",
                    actionButtonText = "+ افزودن اولین قلم کالا",
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
                    verticalArrangement = Arrangement.spacedBy(FarmDimens.Space10)
                ) {
                    // Category Filter Chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6)
                        ) {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("همه (${toPersianDigits(warehouseItems.size)})") },
                                shape = RoundedCornerShape(FarmDimens.ChipRadius),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedCategoryFilter == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            WarehouseCategory.entries.forEach { cat ->
                                val count = warehouseItems.count { it.category == cat }
                                val isSelected = selectedCategoryFilter == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategoryFilter = if (isSelected) null else cat },
                                    label = { Text("${cat.titleFa} (${toPersianDigits(count)})") },
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

                    // Warehouse Items List
                    items(
                        items = filteredItems,
                        key = { it.id },
                        contentType = { it.category.name }
                    ) { item ->
                        val isFeed = item.category == WarehouseCategory.FEED
                        val assignedFlocks = flocks.filter { it.id in item.assignedFlockIds }
                        val dailyConsumptionKg = if (isFeed) {
                            val totalGrams = assignedFlocks.sumOf { flockRequirements[it.id]?.totalFeedGrams ?: 0.0 }
                            totalGrams / 1000.0
                        } else 0.0

                        val daysRemaining = if (isFeed && dailyConsumptionKg > 0) {
                            item.currentStock / dailyConsumptionKg
                        } else Double.MAX_VALUE

                        val alertsEnabledForItem = item.minAlertThreshold > 0.0
                        val isCritical = alertsEnabledForItem && ((isFeed && daysRemaining <= 3.0) || item.currentStock <= item.minAlertThreshold)
                        val isWarning = alertsEnabledForItem && isFeed && daysRemaining in 3.0..7.0

                        Surface(
                            shape = RoundedCornerShape(FarmDimens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isCritical) HealthRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(FarmDimens.Space14)
                            ) {
                                // Header: Category Icon, Name, Days Badge & Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isFeed) FeedAmberContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Image(
                                                painter = if (isFeed) feedIconPainter else warehouseIconPainter,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(FarmDimens.Space12))
                                        Column(modifier = Modifier.weight(1f)) {
                                            AutoResizeText(
                                                text = item.name,
                                                targetFontSize = 15.sp,
                                                minFontSize = 11.sp,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6)
                                            ) {
                                                FarmStatusBadge(
                                                    text = item.category.titleFa,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                if (item.isForSale) {
                                                    FarmStatusBadge(
                                                        text = "🏷️ کالای فروشی",
                                                        color = HealthGreen,
                                                        containerColor = HealthGreenContainer
                                                    )
                                                }
                                                if (!item.notes.isNullOrBlank()) {
                                                    AutoResizeText(
                                                        text = item.notes,
                                                        targetFontSize = 12.sp,
                                                        minFontSize = 9.sp,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Action buttons (Edit / Delete)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { editingItem = item }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "ویرایش",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(onClick = { deletingItemId = item.id }) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "حذف",
                                                tint = HealthRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(FarmDimens.Space10))

                                // Stock & Days Remaining Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "موجودی فعلی در انبار:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        PersianValueUnit(
                                            value = item.currentStock.toInt(),
                                            unit = item.unit,
                                            valueStyle = MaterialTheme.typography.titleLarge,
                                            unitStyle = MaterialTheme.typography.bodySmall,
                                            valueColor = if (isCritical) HealthRedText else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (item.pricePerUnit > 0) {
                                            Text(
                                                text = "${if (item.isForSale) "قیمت فروش" else "قیمت"} هر ${item.unit}: ${DailyNutritionEngine.formatTomans(item.pricePerUnit)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (item.currentStock > 0) {
                                                Text(
                                                    text = "ارزش کل موجودی: ${DailyNutritionEngine.formatTomans(item.currentStock * item.pricePerUnit)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "قیمت واحد ثبت نشده",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    if (isFeed) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            if (daysRemaining < Double.MAX_VALUE) {
                                                FarmStatusBadge(
                                                    text = "حدود ${toPersianDigits(String.format(Locale.US, "%.1f", daysRemaining))} روز ماندگاری",
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
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "مصرف روزانه: ${toPersianDigits(String.format(Locale.US, "%.1f", dailyConsumptionKg))} کیلو",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                FarmStatusBadge(
                                                    text = "بدون گله تخصیص‌یافته",
                                                    color = MaterialTheme.colorScheme.outline,
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Assigned Flocks & 30-Day Dynamic Smart Feed Projection
                                if (isFeed && assignedFlocks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(FarmDimens.Space8))
                                    Text(
                                        text = "گله‌های مصرف‌کننده: " + assignedFlocks.joinToString("، ") { it.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    val projection = remember(item, assignedFlocks, customBreeds, currentSeason, farmAmbientTemp) {
                                        DailyNutritionEngine.calculate30DayFeedProjection(
                                            assignedFlocks = assignedFlocks,
                                            pricePerKg = item.pricePerUnit,
                                            season = currentSeason,
                                            customBreeds = customBreeds,
                                            tempCelsius = farmAmbientTemp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(FarmDimens.Space8))
                                    Surface(
                                        shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(FarmDimens.Space10)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Grain,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(FarmDimens.Space6))
                                                Text(
                                                    text = "تخمین هوشمند ۳۰ روز آینده (با احتساب رشد جوجه‌ها):",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(FarmDimens.Space6))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "کل دان مورد نیاز ۳۰ روز:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = projection.formattedTotalFeed,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "قیمت تخمینی ۳۰ روز:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = if (item.pricePerUnit > 0) projection.formattedCostTomans else "قیمت واحد ثبت نشده",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.pricePerUnit > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(FarmDimens.Space4))
                                            Text(
                                                text = "💡 میانگین مصرف: روزانه ${projection.formattedDailyAverageFeed} (مصرف روزانه به دلیل رشد جوجه‌ها افزایش می‌یابد)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(FarmDimens.Space10))
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(FarmDimens.Space8))

                                // Primary Action: Sell (Eggs)
                                if (item.category == WarehouseCategory.EGG) {
                                    Button(
                                        onClick = { sellingEggItem = item },
                                        enabled = item.currentStock > 0,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = FarmDimens.Space12, vertical = 6.dp),
                                        shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فروش رفت (ثبت درآمد حاصل از تخم‌مرغ)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(FarmDimens.Space8))
                                }

                                // Quick Stock Adjustments (+1, +5, +25, -1, -5, -25)
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "افزایش / کاهش سریع موجودی:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(FarmDimens.Space4))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space4),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { onAdjustStock(item.id, -25.0) },
                                            enabled = item.currentStock >= 25.0,
                                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text("-۲۵", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { onAdjustStock(item.id, -5.0) },
                                            enabled = item.currentStock >= 5.0,
                                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text("-۵", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { onAdjustStock(item.id, -1.0) },
                                            enabled = item.currentStock >= 1.0,
                                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text("-۱", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }

                                        FilledTonalButton(
                                            onClick = { onAdjustStock(item.id, 1.0) },
                                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text("+۱", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }

                                        FilledTonalButton(
                                            onClick = { onAdjustStock(item.id, 5.0) },
                                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text("+۵", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }

                                        FilledTonalButton(
                                            onClick = { onAdjustStock(item.id, 25.0) },
                                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text("+۲۵", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(FarmDimens.Space6))

                                    // "اتمام همه" Button (Deplete All)
                                    OutlinedButton(
                                        onClick = { depletingItem = item },
                                        enabled = item.currentStock > 0.0,
                                        shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (item.currentStock > 0.0) HealthRed else MaterialTheme.colorScheme.outline
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.RemoveCircleOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "اتمام همه (تخلیه کامل موجودی کالا)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom safe spacer
                    item {
                        Spacer(modifier = Modifier.height(FarmDimens.Space40))
                    }
                }
            }
        }
    }

    // Add Warehouse Item Dialog
    if (showAddDialog) {
        WarehouseItemDialog(
            item = null,
            flocks = flocks,
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                onAddItem(newItem)
                showAddDialog = false
            }
        )
    }

    // Edit Warehouse Item Dialog
    if (editingItem != null) {
        WarehouseItemDialog(
            item = editingItem,
            flocks = flocks,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                onEditItem(updated)
                editingItem = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingItemId != null) {
        AlertDialog(
            onDismissRequest = { deletingItemId = null },
            title = {
                Text(
                    text = "حذف کالا از انبار",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از حذف این کالا از انبار اطمینان دارید؟ محاسبات ماندگاری دان مرتبط با این قلم حذف خواهد شد.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(deletingItemId!!)
                        deletingItemId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("حذف قطعی", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deletingItemId = null },
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(FarmDimens.DialogRadius)
        )
    }



    // Deplete All Confirmation Dialog
    if (depletingItem != null) {
        val target = depletingItem!!
        val isSale = target.isForSale && target.pricePerUnit > 0.0
        val totalRevenue = target.currentStock * target.pricePerUnit
        AlertDialog(
            onDismissRequest = { depletingItem = null },
            icon = {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = null,
                    tint = HealthRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "اتمام موجودی کالا",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "آیا از صفر کردن کامل موجودی «${target.name}» (${toPersianDigits(target.currentStock.toInt())} ${target.unit}) اطمینان دارید؟",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isSale) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HealthGreenContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💰 این کالا فروشی است. با تأیید اتمام، مبلغ ${DailyNutritionEngine.formatTomans(totalRevenue)} به عنوان سود و درآمد حاصل از فروش در بخش مالی ثبت خواهد شد.",
                                style = MaterialTheme.typography.labelSmall,
                                color = HealthGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDepleteStock(target.id)
                        depletingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("تأیید و صفر کردن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { depletingItem = null },
                    shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                ) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(FarmDimens.DialogRadius)
        )
    }

    // Sell Eggs Dialog
    if (sellingEggItem != null) {
        SellEggDialog(
            item = sellingEggItem!!,
            onDismiss = { sellingEggItem = null },
            onConfirm = { count, price, notes ->
                onSellEgg(sellingEggItem!!.id, count, price, notes)
                sellingEggItem = null
            }
        )
    }
}
