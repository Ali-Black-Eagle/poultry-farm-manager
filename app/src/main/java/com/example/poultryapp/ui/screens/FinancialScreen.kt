package com.example.poultryapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.data.local.*
import com.example.poultryapp.engine.DailyNutritionEngine
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.*
import com.example.poultryapp.ui.components.AddManualTransactionDialog
import com.example.poultryapp.ui.components.AppTopBar
import com.example.poultryapp.ui.components.AutoResizeText
import com.example.poultryapp.ui.components.FarmStatusBadge
import java.util.Locale

@Composable
fun FinancialScreen(
    transactions: List<FinancialTransaction>,
    flocks: List<Flock>,
    warehouseItems: List<WarehouseItem>,
    onAddTransaction: (FinancialTransaction) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onOpenDrawer: () -> Unit,
    showHeader: Boolean = true,
    externalAddTrigger: Int = 0,
    onOpenVoiceAssistant: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(externalAddTrigger) {
        if (externalAddTrigger > 0) {
            showAddDialog = true
        }
    }
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) } // null = all
    var deletingTxId by remember { mutableStateOf<Long?>(null) }

    // Summary calculations
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountTomans }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountTomans }
    }
    val netProfit = totalIncome - totalExpense

    val filteredTransactions = remember(transactions, selectedFilter) {
        if (selectedFilter == null) transactions else transactions.filter { it.type == selectedFilter }
    }

    // Default values for Profit/Loss Calculator
    val totalFarmBirds = remember(flocks) { flocks.sumOf { it.effectiveBirdCount }.coerceAtLeast(1) }
    val defaultFeedPrice = remember(warehouseItems) {
        val feed = warehouseItems.find { it.category == WarehouseCategory.FEED && it.pricePerUnit > 0 }
        feed?.pricePerUnit?.toLong()?.toString() ?: "24000"
    }
    // Default egg price is 25,000 Tomans as requested
    val defaultEggPrice = "25000"

    // Calculator inputs
    var calcBirdCountText by remember(totalFarmBirds) { mutableStateOf(totalFarmBirds.toString()) }
    var calcDailyFeedGramsText by remember { mutableStateOf("115") }
    var calcFeedPriceText by remember(defaultFeedPrice) { mutableStateOf(defaultFeedPrice) }
    var calcLayingRateText by remember { mutableStateOf("85") }
    var calcEggPriceText by remember { mutableStateOf(defaultEggPrice) }
    var calcFeedSavingPercentText by remember { mutableStateOf("0") }
    var calcOtherCostsText by remember { mutableStateOf("0") }
    var calcTargetProfitPercentText by remember { mutableStateOf("") }
    var isCalculatorExpanded by remember { mutableStateOf(false) }

    // Helper to dynamically calculate egg price based on target profit margin
    fun recalculateEggPrice(
        targetProfitStr: String = calcTargetProfitPercentText,
        birdCountStr: String = calcBirdCountText,
        layingRateStr: String = calcLayingRateText,
        feedGramsStr: String = calcDailyFeedGramsText,
        feedPriceStr: String = calcFeedPriceText,
        feedSavingStr: String = calcFeedSavingPercentText,
        otherCostsStr: String = calcOtherCostsText
    ) {
        val targetPercent = targetProfitStr.toDoubleOrNull() ?: return
        val birds = birdCountStr.toIntOrNull() ?: return
        val layingRate = layingRateStr.toDoubleOrNull() ?: return
        val dEggs = (birds * (layingRate / 100.0)).toInt()
        val mEggs = dEggs * 30
        if (mEggs <= 0) return

        val feedGrams = feedGramsStr.toDoubleOrNull() ?: 0.0
        val feedPrice = feedPriceStr.toDoubleOrNull() ?: 0.0
        val feedSaving = (feedSavingStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
        val baseFeedCost = ((birds * feedGrams) / 1000.0) * feedPrice * 30.0
        val effectiveFeedCost = baseFeedCost * (1.0 - (feedSaving / 100.0))
        val otherCosts = otherCostsStr.toDoubleOrNull() ?: 0.0
        val totalCost = effectiveFeedCost + otherCosts

        // Desired Revenue = Total Cost * (1 + targetPercent / 100)
        val desiredRevenue = totalCost * (1.0 + (targetPercent / 100.0))
        val calculatedPrice = kotlin.math.round(desiredRevenue / mEggs).toLong().coerceAtLeast(0L)
        calcEggPriceText = calculatedPrice.toString()
    }

    // Calculator computation
    val calcBirds = calcBirdCountText.toIntOrNull() ?: 0
    val calcFeedGrams = calcDailyFeedGramsText.toDoubleOrNull() ?: 0.0
    val calcFeedPrice = calcFeedPriceText.toDoubleOrNull() ?: 0.0
    val calcFeedSavingPercent = (calcFeedSavingPercentText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val calcOtherCosts = calcOtherCostsText.toDoubleOrNull() ?: 0.0
    val calcLayingRate = calcLayingRateText.toDoubleOrNull() ?: 0.0
    val calcEggPrice = calcEggPriceText.toDoubleOrNull() ?: 0.0

    val dailyEggs = (calcBirds * (calcLayingRate / 100.0)).toInt()
    val monthlyEggs = dailyEggs * 30
    val dailyRevenue = dailyEggs * calcEggPrice
    val monthlyRevenue = dailyRevenue * 30.0

    val dailyFeedKg = (calcBirds * calcFeedGrams) / 1000.0
    val monthlyFeedKg = dailyFeedKg * 30.0
    val baseDailyFeedCost = dailyFeedKg * calcFeedPrice
    val baseMonthlyFeedCost = monthlyFeedKg * calcFeedPrice

    val dailyFeedSavings = baseDailyFeedCost * (calcFeedSavingPercent / 100.0)
    val monthlyFeedSavings = baseMonthlyFeedCost * (calcFeedSavingPercent / 100.0)

    val effectiveDailyFeedCost = baseDailyFeedCost - dailyFeedSavings
    val effectiveMonthlyFeedCost = baseMonthlyFeedCost - monthlyFeedSavings

    val dailyOtherCosts = calcOtherCosts / 30.0
    val monthlyOtherCosts = calcOtherCosts

    val totalDailyCost = effectiveDailyFeedCost + dailyOtherCosts
    val totalMonthlyCost = effectiveMonthlyFeedCost + monthlyOtherCosts

    val dailyNetProfit = dailyRevenue - totalDailyCost
    val monthlyNetProfit = monthlyRevenue - totalMonthlyCost

    val achievedProfitMarginPercent = if (totalMonthlyCost > 0) {
        (monthlyNetProfit / totalMonthlyCost) * 100.0
    } else 0.0

    // FCR = kg of feed / kg of eggs (assuming standard 60g egg = 0.06 kg)
    val dailyEggWeightKg = dailyEggs * 0.06
    val fcr = if (dailyEggWeightKg > 0.0) dailyFeedKg / dailyEggWeightKg else 0.0

    Scaffold(
        topBar = {
            if (showHeader) {
                AppTopBar(
                    title = "امور مالی و سود و زیان",
                    subtitle = "تراز مالی و ماشین‌حساب تخصصی فارم",
                    logoRes = com.example.poultryapp.R.drawable.ic_farm_financial,
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "منو")
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
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        text = "ثبت تراکنش",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = FarmDimens.Space16),
            verticalArrangement = Arrangement.spacedBy(FarmDimens.Space14)
        ) {
            item {
                Spacer(modifier = Modifier.height(FarmDimens.Space4))
            }

            // 1. Financial Overview Summary Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(FarmDimens.Space10)) {
                    // Net Profit / Loss Banner - Full width dedicated amount for arbitrarily large numbers
                    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    val bannerBgColor = if (isDark) {
                        if (netProfit >= 0) HealthGreenContainerDark else HealthRedContainerDark
                    } else {
                        if (netProfit >= 0) HealthGreenContainer.copy(alpha = 0.45f) else HealthRedContainer.copy(alpha = 0.45f)
                    }
                    val bannerBorderColor = if (isDark) {
                        if (netProfit >= 0) HealthGreenDark.copy(alpha = 0.4f) else HealthRedDark.copy(alpha = 0.4f)
                    } else {
                        if (netProfit >= 0) HealthGreen.copy(alpha = 0.6f) else HealthRed.copy(alpha = 0.6f)
                    }
                    val bannerAmountColor = if (isDark) {
                        if (netProfit >= 0) HealthGreenDark else HealthRedDark
                    } else {
                        if (netProfit >= 0) HealthGreen else HealthRed
                    }
                    val bannerPillBg = if (isDark) {
                        if (netProfit >= 0) Color(0xFF064E3B) else Color(0xFF451117)
                    } else {
                        if (netProfit >= 0) HealthGreen.copy(alpha = 0.2f) else HealthRed.copy(alpha = 0.2f)
                    }
                    val bannerPillTextColor = if (isDark) {
                        if (netProfit >= 0) HealthGreenTextDark else HealthRedTextDark
                    } else {
                        if (netProfit >= 0) HealthGreen else HealthRed
                    }

                    Surface(
                        shape = RoundedCornerShape(FarmDimens.CardRadius),
                        color = bannerBgColor,
                        border = BorderStroke(1.5.dp, bannerBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(FarmDimens.Space16)) {
                            // Line 1: Header Row with Icon, Title, and Pill Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = if (netProfit >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = bannerAmountColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (netProfit >= 0) "سود خالص تجمعی فارم" else "زیان خالص تجمعی فارم",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                    color = bannerPillBg
                                ) {
                                    Text(
                                        text = if (netProfit >= 0) "تراز مثبت" else "تراز منفی",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = bannerPillTextColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Line 2: The Grand Amount - Dedicated full width for ANY large number!
                            Text(
                                text = (if (netProfit >= 0) "+ " else "- ") + DailyNutritionEngine.formatTomans(kotlin.math.abs(netProfit)),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = bannerAmountColor,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            AutoResizeText(
                                text = "حاصل از فروش تخم‌مرغ منهای مصرف دان و هزینه‌های ثبت‌شده",
                                targetFontSize = 11.sp,
                                minFontSize = 8.5.sp,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Income & Expense Sub-Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space10)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) HealthGreenDark else HealthGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "کل درآمدها",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = DailyNutritionEngine.formatTomans(totalIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) HealthGreenDark else HealthGreen,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) HealthRedDark else HealthRed)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "کل هزینه‌ها",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = DailyNutritionEngine.formatTomans(totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) HealthRedDark else HealthRed,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // 2. Interactive Farm Profit & Loss Calculator Option
            item {
                Surface(
                    shape = RoundedCornerShape(FarmDimens.CardRadius),
                    color = if (isCalculatorExpanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        width = if (isCalculatorExpanded) 1.5.dp else 1.dp,
                        color = if (isCalculatorExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    shadowElevation = if (isCalculatorExpanded) 3.dp else 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCalculatorExpanded = !isCalculatorExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Calculate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ماشین‌حساب هوشمند سود و زیان و قیمت‌گذاری",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isCalculatorExpanded) "برای بستن ماشین‌حساب لمس کنید" else "محاسبه درصد سود هدف، قیمت تخم‌مرغ، صرفه‌جویی دان و هزینه‌ها (لمس کنید)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCalculatorExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { isCalculatorExpanded = !isCalculatorExpanded }) {
                                Icon(
                                    if (isCalculatorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isCalculatorExpanded) {
                            Spacer(modifier = Modifier.height(FarmDimens.Space14))

                            // Row 1: Bird count & Laying rate
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = calcBirdCountText,
                                    onValueChange = { input ->
                                        calcBirdCountText = input.filter { c -> c.isDigit() }
                                        if (calcTargetProfitPercentText.isNotBlank()) {
                                            recalculateEggPrice(birdCountStr = input)
                                        }
                                    },
                                    label = { Text("تعداد مرغ (قطعه)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = calcLayingRateText,
                                    onValueChange = { input ->
                                        calcLayingRateText = input.filter { c -> c.isDigit() || c == '.' }
                                        if (calcTargetProfitPercentText.isNotBlank()) {
                                            recalculateEggPrice(layingRateStr = input)
                                        }
                                    },
                                    label = { Text("درصد تخم‌گذاری (٪)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 2: Daily feed per bird & Feed price per kg
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = calcDailyFeedGramsText,
                                    onValueChange = { input ->
                                        calcDailyFeedGramsText = input.filter { c -> c.isDigit() || c == '.' }
                                        if (calcTargetProfitPercentText.isNotBlank()) {
                                            recalculateEggPrice(feedGramsStr = input)
                                        }
                                    },
                                    label = { Text("دان هر مرغ (گرم)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = calcFeedPriceText,
                                    onValueChange = { input ->
                                        calcFeedPriceText = input.filter { c -> c.isDigit() }
                                        if (calcTargetProfitPercentText.isNotBlank()) {
                                            recalculateEggPrice(feedPriceStr = input)
                                        }
                                    },
                                    label = { Text("قیمت هر کیلو دان (تومان)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 3: Feed savings % & Other costs (vaccine, vitamin, supplements)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = calcFeedSavingPercentText,
                                    onValueChange = { input ->
                                        calcFeedSavingPercentText = input.filter { c -> c.isDigit() || c == '.' }
                                        if (calcTargetProfitPercentText.isNotBlank()) {
                                            recalculateEggPrice(feedSavingStr = input)
                                        }
                                    },
                                    label = { Text("صرفه‌جویی در دان (٪)") },
                                    placeholder = { Text("پیش‌فرض ۰٪") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = calcOtherCostsText,
                                    onValueChange = { input ->
                                        calcOtherCostsText = input.filter { c -> c.isDigit() }
                                        if (calcTargetProfitPercentText.isNotBlank()) {
                                            recalculateEggPrice(otherCostsStr = input)
                                        }
                                    },
                                    label = { Text("بقیه هزینه‌ها: واکسن و...") },
                                    placeholder = { Text("تومان ماهانه") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 4: Target net profit % & Egg sale price
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = calcTargetProfitPercentText,
                                    onValueChange = { input ->
                                        val filtered = input.filter { c -> c.isDigit() || c == '.' }
                                        calcTargetProfitPercentText = filtered
                                        recalculateEggPrice(targetProfitStr = filtered)
                                    },
                                    label = { Text("میزان سود مورد نظر (٪)") },
                                    placeholder = { Text("مثلاً: ۳۰") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = calcEggPriceText,
                                    onValueChange = { input ->
                                        calcEggPriceText = input.filter { c -> c.isDigit() }
                                    },
                                    label = { Text("قیمت هر تخم‌مرغ (تومان)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Guidance notification banner
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (calcTargetProfitPercentText.isNotBlank()) {
                                            "بر اساس سود هدف ${toPersianDigits(calcTargetProfitPercentText)}٪، قیمت هر تخم‌مرغ معادل ${toPersianDigits(calcEggPriceText)} تومان تعیین شد."
                                        } else {
                                            "قیمت پیش‌فرض تخم‌مرغ: ۲۵,۰۰۰ تومان | با وارد کردن درصد سود هدف، قیمت به صورت خودکار محاسبه می‌شود."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(FarmDimens.Space14))

                            // Calculator Output Results Card
                            Surface(
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                    // 1. Egg Production & Feed Consumption - Two Balanced Columns
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Column 1: Egg Production
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "تولید تخم‌مرغ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "روزانه: ${toPersianDigits(dailyEggs)} عدد",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Text(
                                                    text = "ماهانه: ${toPersianDigits(monthlyEggs)} عدد",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Column 2: Feed Consumption
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "مصرف دان",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "روزانه: ${toPersianDigits(String.format(Locale.US, "%.1f", dailyFeedKg))} کیلو",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Text(
                                                    text = "ماهانه: ${toPersianDigits(String.format(Locale.US, "%.0f", monthlyFeedKg))} کیلو",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(
                                        thickness = 0.8.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 2. Financial Breakdown (Line by Line with ample horizontal clearance)
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        // Base Feed Cost
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (calcFeedSavingPercent > 0.0) "هزینه پایه دان:" else "هزینه دان مصرفی:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(FarmDimens.Space12))
                                            Text(
                                                text = DailyNutritionEngine.formatTomans(baseMonthlyFeedCost),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (calcFeedSavingPercent > 0.0) MaterialTheme.colorScheme.onSurfaceVariant else HealthRed,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        // Feed Savings (if any)
                                        if (calcFeedSavingPercent > 0.0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "صرفه‌جویی دان (${toPersianDigits(String.format(Locale.US, "%.0f", calcFeedSavingPercent))}٪):",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = HealthGreen,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(FarmDimens.Space12))
                                                Text(
                                                    text = "- ${DailyNutritionEngine.formatTomans(monthlyFeedSavings)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = HealthGreen,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            // Effective Feed Cost
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "هزینه خالص دان:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(FarmDimens.Space12))
                                                Text(
                                                    text = DailyNutritionEngine.formatTomans(effectiveMonthlyFeedCost),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = HealthRed,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Other Costs (Vaccine, Vitamins, Supplements)
                                        if (monthlyOtherCosts > 0.0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "سایر هزینه‌ها (دارو و مکمل):",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(FarmDimens.Space12))
                                                Text(
                                                    text = DailyNutritionEngine.formatTomans(monthlyOtherCosts),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Total Farm Monthly Cost
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "مجموع هزینه‌های فارم:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(FarmDimens.Space12))
                                            Text(
                                                text = DailyNutritionEngine.formatTomans(totalMonthlyCost),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = HealthRed,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        // Total Egg Sale Revenue
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "درآمد فروش تخم‌مرغ:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(FarmDimens.Space12))
                                            Text(
                                                text = DailyNutritionEngine.formatTomans(monthlyRevenue),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = HealthGreen,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 3. Net Monthly Profit Card - Dedicated spacious grand layout for arbitrarily large numbers
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (monthlyNetProfit >= 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            width = 1.3.dp,
                                            color = if (monthlyNetProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            // Line 1: Header with Title and Pill Badge
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Icon(
                                                        imageVector = if (monthlyNetProfit >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                        contentDescription = null,
                                                        tint = if (monthlyNetProfit >= 0) HealthGreen else HealthRed,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (monthlyNetProfit >= 0) "سود خالص ماهانه (۳۰ روزه)" else "زیان خالص ماهانه (۳۰ روزه)",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                                    color = if (monthlyNetProfit >= 0) HealthGreenContainer else MaterialTheme.colorScheme.errorContainer
                                                ) {
                                                    Text(
                                                        text = if (monthlyNetProfit >= 0) "سودده" else "زیان‌ده",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (monthlyNetProfit >= 0) HealthGreen else HealthRed,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Line 2: The Grand Amount - Dedicated full width for ANY large number!
                                            Text(
                                                text = DailyNutritionEngine.formatTomans(kotlin.math.abs(monthlyNetProfit)),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Black,
                                                color = if (monthlyNetProfit >= 0) HealthGreen else HealthRed,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(
                                                thickness = 0.8.dp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Line 3: Daily Profit and Profit Margin Percent
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${if (monthlyNetProfit >= 0) "سود روزانه:" else "زیان روزانه:"} ${DailyNutritionEngine.formatTomans(kotlin.math.abs(dailyNetProfit))}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Text(
                                                    text = "بازدهی: ${toPersianDigits(String.format(Locale.US, "%.1f", achievedProfitMarginPercent))}٪",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (monthlyNetProfit >= 0) HealthGreen else HealthRed,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    if (fcr > 0.0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "💡 ضریب تبدیل غذایی تخم‌مرغ (FCR): ${toPersianDigits(String.format(Locale.US, "%.2f", fcr))} کیلو دان به ازای هر کیلو تخم‌مرغ",
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

            // 3. Transactions Section Header & Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "دفترچه تراکنش‌های مالی (${toPersianDigits(filteredTransactions.size)} ثبت)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تراکنش جدید", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space8))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("همه تراکنش‌ها") }
                    )
                    FilterChip(
                        selected = selectedFilter == TransactionType.INCOME,
                        onClick = { selectedFilter = TransactionType.INCOME },
                        label = { Text("درآمدها") }
                    )
                    FilterChip(
                        selected = selectedFilter == TransactionType.EXPENSE,
                        onClick = { selectedFilter = TransactionType.EXPENSE },
                        label = { Text("هزینه‌ها") }
                    )
                }
            }

            // 4. Transactions List
            if (filteredTransactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(FarmDimens.CardRadius),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "هنوز تراکنش مالی ثبت نشده است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "با زدن «مصرف شد» روی دان یا «فروش رفت» روی تخم‌مرغ در انبار، تراکنش‌ها به صورت خودکار اینجا ثبت می‌شوند.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    val isIncome = tx.type == TransactionType.INCOME
                    Surface(
                        shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FarmDimens.Space14)
                        ) {
                            // Row 1: Icon, Full Title (جنس مصرف شده / عنوان کامل), and Delete Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isIncome) HealthGreenContainer else HealthRedContainer,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (isIncome) HealthGreen else HealthRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Full title without truncation - spans full line(s) naturally!
                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        softWrap = true
                                    )
                                }

                                IconButton(
                                    onClick = { deletingTxId = tx.id },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "حذف تراکنش",
                                        tint = HealthRed.copy(alpha = 0.75f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 2: Badges (Category + Full Date Badge + Related Flock)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Category Badge
                                FarmStatusBadge(
                                    text = tx.category.titleFa,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                // Dedicated Full Date Badge with calendar icon
                                Surface(
                                    shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = toPersianDigits(tx.dateJalaliStr),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Related Flock (if present)
                                if (!tx.relatedFlockName.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    ) {
                                        Text(
                                            text = "گله: ${tx.relatedFlockName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Row 3: Quantity / Notes if present
                            if (tx.quantity > 0 || !tx.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (tx.quantity > 0) {
                                        Text(
                                            text = "مقدار: ${toPersianDigits(tx.quantity)} ${tx.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (!tx.notes.isNullOrBlank()) {
                                        if (tx.quantity > 0) {
                                            Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Text(
                                            text = tx.notes,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            softWrap = true
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 4: Dedicated Full-Width Financial Amount Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isIncome) "مبلغ واریزی (درآمد):" else "مبلغ پرداختی (هزینه):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = (if (isIncome) "+ " else "- ") + DailyNutritionEngine.formatTomans(tx.amountTomans),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isIncome) HealthGreen else HealthRed,
                                    maxLines = 1,
                                    softWrap = false
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

    // Add Transaction Dialog
    if (showAddDialog) {
        AddManualTransactionDialog(
            flocks = flocks,
            onDismiss = { showAddDialog = false },
            onSave = {
                onAddTransaction(it)
                showAddDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingTxId != null) {
        AlertDialog(
            onDismissRequest = { deletingTxId = null },
            title = { Text("حذف تراکنش مالی", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف این رکورد مالی اطمینان دارید؟ این عمل قابل بازگشت نیست.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(deletingTxId!!)
                        deletingTxId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed)
                ) {
                    Text("حذف قطعی")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingTxId = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}
