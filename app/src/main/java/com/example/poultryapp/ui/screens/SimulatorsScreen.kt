package com.example.poultryapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.data.local.FeasibilityInput
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.engine.*
import com.example.poultryapp.theme.FarmDimens
import com.example.poultryapp.theme.FarmGreenPrimary
import com.example.poultryapp.ui.components.AppTopBar
import java.util.Locale

enum class SimulatorSubTab(val titleFa: String) {
    SLAUGHTER_OPTIMIZER("کشتار بهینه"),
    INVESTMENT_FEASIBILITY("توجیه سرمایه")
}

@Composable
fun SimulatorsScreen(
    flocks: List<Flock>,
    onOpenDrawer: () -> Unit,
    showHeader: Boolean = true
) {
    var currentSubTab by rememberSaveable { mutableStateOf(SimulatorSubTab.SLAUGHTER_OPTIMIZER) }

    val content = @Composable {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = currentSubTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    SimulatorSubTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = currentSubTab.ordinal == index,
                            onClick = { currentSubTab = tab },
                            text = {
                                Text(
                                    text = tab.titleFa,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (currentSubTab.ordinal == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            AnimatedContent(targetState = currentSubTab, label = "tab") { tab ->
                when (tab) {
                    SimulatorSubTab.SLAUGHTER_OPTIMIZER -> SlaughterOptimizerTab(flocks = flocks)
                    SimulatorSubTab.INVESTMENT_FEASIBILITY -> InvestmentFeasibilityTab()
                }
            }
        }
    }

    if (showHeader) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = "شبیه‌سازها و ابزارهای مهندسی فارم",
                    subtitle = "شبیه‌ساز تاریخ بهینه کشتار و توجیه‌پذیری سرمایه‌گذاری احداث گله",
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "منو")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                content()
            }
        }
    } else {
        content()
    }
}



@Composable
private fun SlaughterOptimizerTab(flocks: List<Flock>) {
    var selectedFlockId by remember { mutableStateOf(flocks.firstOrNull()?.id ?: 0L) }
    val activeFlock = flocks.find { it.id == selectedFlockId } ?: flocks.firstOrNull() ?: Flock(
        name = "گله گوشتی نمونه",
        species = BirdSpecies.CHICKEN,
        breed = "رأس ۳۰۸",
        birthEpochMillis = System.currentTimeMillis() - (41L * 24 * 60 * 60 * 1000),
        birdCount = 2000
    )

    var livePriceText by remember { mutableStateOf("85000") }
    var feedPriceText by remember { mutableStateOf("22000") }

    val livePrice = livePriceText.toDoubleOrNull() ?: 85000.0
    val feedPrice = feedPriceText.toDoubleOrNull() ?: 22000.0

    val optimization = remember(activeFlock, livePrice, feedPrice) {
        OptimalSlaughterEngine.calculateOptimalSlaughter(
            flock = activeFlock,
            livePricePerKgTomans = livePrice,
            feedPricePerKgTomans = feedPrice
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(FarmDimens.Space16),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // انتخاب گله
        if (flocks.size > 1) {
            item {
                Text("انتخاب گله هدف:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(flocks) { f ->
                        FilterChip(
                            selected = f.id == activeFlock.id,
                            onClick = { selectedFlockId = f.id },
                            label = { Text(f.name) }
                        )
                    }
                }
            }
        }

        // کارت خلاصه تصمیم و پنجره طلایی کشتار
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (optimization.isOverdueForSlaughter) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.5.dp, if (optimization.isOverdueForSlaughter) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سن جاری گله: ${toPersianDigits(optimization.currentAgeDays)} روز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "پنجره طلایی: ${optimization.goldenHarvestWindowRange}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = optimization.recommendationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ورودی‌های قیمت بازار
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = livePriceText,
                        onValueChange = { livePriceText = it.filter { c -> c.isDigit() } },
                        label = { Text("قیمت زنده هر کیلو (تومان)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = feedPriceText,
                        onValueChange = { feedPriceText = it.filter { c -> c.isDigit() } },
                        label = { Text("قیمت دان هر کیلو (تومان)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

        // جدول سود حاشیه‌ای روزانه
        item {
            Text(
                text = "📊 منحنی سود حاشیه‌ای روزانه (سود خالص هر روز نگهداری پرنده):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(optimization.dailyMarginalProfits) { point ->
            val isPeak = point.ageDay == optimization.optimalHarvestAgeDays
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isPeak) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = if (isPeak) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "روز ${toPersianDigits(point.ageDay)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isPeak) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "★ نقطه اوج سود",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "وزن: ${toPersianDigits(String.format(Locale.US, "%.2f", point.bodyWeightKg))} kg | دان روزانه: ${toPersianDigits(point.dailyFeedGrams.toInt())} g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${toPersianDigits(point.dailyMarginalProfitTomans.toInt())} تومان/روز",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (point.dailyMarginalProfitTomans >= 0) FarmGreenPrimary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "FCR: ${toPersianDigits(String.format(Locale.US, "%.2f", point.fcr))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentFeasibilityTab() {
    var birdCountText by remember { mutableStateOf("1000") }
    var chickPriceText by remember { mutableStateOf("38000") }
    var feedPriceText by remember { mutableStateOf("22000") }
    var liveSalePriceText by remember { mutableStateOf("85000") }
    var targetWeightText by remember { mutableStateOf("2.6") }
    var cycleDaysText by remember { mutableStateOf("45") }

    val birdCount = birdCountText.toIntOrNull() ?: 1000
    val chickPrice = chickPriceText.toDoubleOrNull() ?: 38000.0
    val feedPrice = feedPriceText.toDoubleOrNull() ?: 22000.0
    val livePrice = liveSalePriceText.toDoubleOrNull() ?: 85000.0
    val targetWeight = targetWeightText.toDoubleOrNull() ?: 2.6
    val cycleDays = cycleDaysText.toIntOrNull() ?: 45

    val input = remember(birdCount, chickPrice, feedPrice, livePrice, targetWeight, cycleDays) {
        FeasibilityInput(
            birdCount = birdCount,
            chickPriceTomans = chickPrice,
            feedPricePerKgTomans = feedPrice,
            liveSalePricePerKgTomans = livePrice,
            targetWeightKg = targetWeight,
            cycleDays = cycleDays
        )
    }

    val result = remember(input) {
        InvestmentFeasibilityEngine.calculateFeasibility(input)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(FarmDimens.Space16),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // کارت خلاصه خروجی اقتصادی
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "پیش‌بینی توجیه‌پذیری و سودآوری دوره",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeasibilityStat("سرمایه در گردش کل:", DailyNutritionEngine.formatTomans(result.totalWorkingCapitalTomans))
                        FeasibilityStat("سود خالص پیش‌بینی:", DailyNutritionEngine.formatTomans(result.netProfitTomans))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeasibilityStat("بازده سرمایه (ROI):", "${toPersianDigits(String.format(Locale.US, "%.1f", result.roiPercent))}٪")
                        FeasibilityStat("سود هر پرنده:", DailyNutritionEngine.formatTomans(result.profitPerBirdTomans))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeasibilityStat("کل دان مورد نیاز:", "${toPersianDigits(result.totalFeedKg.toInt())} کیلوگرم")
                        FeasibilityStat("نقطه سر‌به‌سر دوره:", if (result.breakEvenDay != null) "روز ${toPersianDigits(result.breakEvenDay)}" else "حاصل نمی‌شود (زیان‌ده)")
                    }
                }
            }
        }

        // ورودی‌های شبیه‌ساز
        item {
            Text("پارامترهای سرمایه‌گذاری و هزینه‌ها:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = birdCountText,
                            onValueChange = { birdCountText = it.filter { c -> c.isDigit() } },
                            label = { Text("تعداد قطعه جوجه") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = chickPriceText,
                            onValueChange = { chickPriceText = it.filter { c -> c.isDigit() } },
                            label = { Text("قیمت هر قطعه (تومان)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = feedPriceText,
                            onValueChange = { feedPriceText = it.filter { c -> c.isDigit() } },
                            label = { Text("قیمت دان هر کیلو (تومان)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = liveSalePriceText,
                            onValueChange = { liveSalePriceText = it.filter { c -> c.isDigit() } },
                            label = { Text("قیمت فروش هر کیلو (تومان)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetWeightText,
                            onValueChange = { targetWeightText = it },
                            label = { Text("وزن نهایی هدف (kg)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cycleDaysText,
                            onValueChange = { cycleDaysText = it.filter { c -> c.isDigit() } },
                            label = { Text("طول دوره پرورش (روز)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeasibilityStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
