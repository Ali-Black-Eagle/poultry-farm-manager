package com.example.poultryapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.poultryapp.data.model.*
import com.example.poultryapp.engine.DiseaseDiagnosisEngine
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.*
import com.example.poultryapp.ui.components.*

enum class HealthSubTab(val titleFa: String, val icon: ImageVector) {
    DIAGNOSIS("تشخیص", Icons.Default.Healing),
    DRUGS("داروها", Icons.Default.Medication),
    SUPPLEMENTS("مکمل‌ها", Icons.Default.Vaccines),
    BIOSECURITY("امنیت زیستی", Icons.Default.Shield),
    VET_CONSULT("دامپزشک", Icons.Default.SupportAgent)
}

@Composable
fun HealthScreen(
    flocks: List<com.example.poultryapp.data.local.Flock> = emptyList(),
    biosecurityChecks: List<com.example.poultryapp.data.local.BiosecurityCheckItem> = emptyList(),
    onToggleBiosecurityCheck: ((id: String, completed: Boolean) -> Unit)? = null,
    onSetFlockMedicationWithdrawal: ((flockId: Long, days: Int, medName: String) -> Unit)? = null,
    onClearFlockMedicationWithdrawal: ((flockId: Long) -> Unit)? = null,
    initialSubTab: HealthSubTab = HealthSubTab.DIAGNOSIS,
    onSubTabSelected: (HealthSubTab) -> Unit = {},
    onSelectDiseaseDetail: (DiseaseInfo) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenVoiceAssistant: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialSubTab.ordinal,
        pageCount = { HealthSubTab.entries.size }
    )

    LaunchedEffect(initialSubTab) {
        if (pagerState.currentPage != initialSubTab.ordinal) {
            pagerState.scrollToPage(initialSubTab.ordinal)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        onSubTabSelected(HealthSubTab.entries[pagerState.currentPage])
    }

    val activeSubTab = HealthSubTab.entries[pagerState.currentPage]

    // Diagnosis State
    var selectedSpecies by remember { mutableStateOf(BirdSpecies.CHICKEN) }
    var selectedGrowthStage by remember { mutableStateOf(GrowthStage.ALL_STAGES) }
    var selectedSymptomIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCategory by remember { mutableStateOf(SymptomCategory.RESPIRATORY) }
    var diagnosticResults by remember { mutableStateOf<List<DiagnosticResult>?>(null) }

    // Drug State
    var drugSearchQuery by remember { mutableStateOf("") }
    var selectedDrugCategory by remember { mutableStateOf<MedicationCategory?>(null) }

    // Supplement State
    var suppSearchQuery by remember { mutableStateOf("") }
    var selectedSuppCategory by remember { mutableStateOf<MedicationCategory?>(null) }

    var selectedMedicationForDetail by remember { mutableStateOf<PoultryMedication?>(null) }

    var showWithdrawalDialog by remember { mutableStateOf(false) }
    var selectedWithdrawalFlockId by remember { mutableStateOf<Long?>(null) }
    var withdrawalMedName by remember { mutableStateOf("") }
    var withdrawalDaysText by remember { mutableStateOf("7") }

    val allSymptoms = remember { DiseaseCatalog.allSymptoms }
    val filteredDrugs = remember(drugSearchQuery, selectedDrugCategory) {
        MedicationCatalog.searchDrugs(drugSearchQuery, selectedDrugCategory)
    }
    val filteredSupplements = remember(suppSearchQuery, selectedSuppCategory) {
        MedicationCatalog.searchSupplements(suppSearchQuery, selectedSuppCategory)
    }

    Scaffold(
        topBar = {
            Column {
                AppTopBar(
                    title = "سلامت، درمان و داروها",
                    subtitle = "موتور تخصصی غربالگری بیماری‌ها، بانک داروها و خدمات دامپزشکی",
                    logoRes = com.example.poultryapp.R.drawable.ic_farm_health,
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

                // Sub-navigation Switcher Pinned Under TopBar
                val subTabItems = remember {
                    HealthSubTab.entries.map {
                        FarmSubTabItem(title = it.titleFa, icon = it.icon)
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
    ) { padding ->
        var symptomSearchQuery by remember { mutableStateOf("") }
        val categorySymptoms = remember(selectedCategory, symptomSearchQuery) {
            if (symptomSearchQuery.isBlank()) {
                DiseaseCatalog.getSymptomsForCategory(selectedCategory)
            } else {
                val normQuery = com.example.poultryapp.voice.PersianVoiceNormalizer.normalize(symptomSearchQuery)
                DiseaseCatalog.allSymptoms.filter { sym ->
                    val normTitle = com.example.poultryapp.voice.PersianVoiceNormalizer.normalize(sym.titleFa)
                    normTitle.contains(normQuery) || sym.voiceAliases.any { it.contains(normQuery) }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (HealthSubTab.entries[page]) {
                HealthSubTab.DIAGNOSIS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = FarmDimens.Space16, vertical = FarmDimens.Space8),
                        verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
                    ) {
                        // 0. Direct Vet Consultation Action Banner
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(HealthSubTab.VET_CONSULT.ordinal)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.SupportAgent,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "مشاوره و ویزیت تخصصی با دامپزشک",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "تماس تلفنی، پیامکی یا ثبت تیکت مستقیم بالینی",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // 1. Species Selector Header
                        item {
                            SectionHeader(
                                title = "۱) گونه پرنده مورد معاینه",
                                subtitle = "انتخاب گونه جهت فیلتر بیماری‌های مرتبط"
                            )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6)
                    ) {
                        items(BirdSpecies.entries, key = { it.name }) { sp ->
                            val isSelected = sp == selectedSpecies
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSpecies = sp
                                    diagnosticResults = null
                                },
                                label = {
                                    Text(
                                        text = sp.titleFa,
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

                // 1.5 Growth Stage / Age Filter
                item {
                    SectionHeader(
                        title = "۲) مرحله پرورشی و سن پرنده",
                        subtitle = "فیلتر اختصاصی بیماری‌ها بر اساس سن زیستی و دوره پرورش"
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6)
                    ) {
                        items(GrowthStage.entries, key = { it.name }) { stage ->
                            val isSelected = stage == selectedGrowthStage
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedGrowthStage = stage
                                    diagnosticResults = null
                                },
                                label = {
                                    Text(
                                        text = stage.titleFa,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(FarmDimens.ChipRadius),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }
                }

                // 2. Symptom Search & Category Tabs
                item {
                    SectionHeader(
                        title = "۳) انتخاب یا جستجوی علائم بالینی مشاهده‌شده",
                        subtitle = "علائم ظاهری، مدفوع و رفتار پرنده را جستجو یا علامت بزنید"
                    )

                    OutlinedTextField(
                        value = symptomSearchQuery,
                        onValueChange = { symptomSearchQuery = it },
                        placeholder = {
                            Text(
                                text = "جستجوی سریع نشانه بالینی",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (symptomSearchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { symptomSearchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "پاک کردن جستجو", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (symptomSearchQuery.isBlank()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6)
                        ) {
                            items(SymptomCategory.entries, key = { it.name }) { cat ->
                                val isSelected = cat == selectedCategory
                                val countInCat = selectedSymptomIds.count { id ->
                                    DiseaseCatalog.allSymptoms.any { it.id == id && it.category == cat }
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(cat.titleFa)
                                            if (countInCat > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                                ) {
                                                    Text(
                                                        text = toPersianDigits(countInCat),
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(FarmDimens.ChipRadius)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "نتایج جستجو برای: «$symptomSearchQuery» (${toPersianDigits(categorySymptoms.size)} نشانه یافت شد)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Symptoms Checkbox Grid
                items(categorySymptoms, key = { it.id }) { symptom ->
                    val isChecked = selectedSymptomIds.contains(symptom.id)
                    Surface(
                        shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                        color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSymptomIds = if (isChecked) {
                                    selectedSymptomIds - symptom.id
                                } else {
                                    selectedSymptomIds + symptom.id
                                }
                                diagnosticResults = null
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = FarmDimens.Space12, vertical = FarmDimens.Space6),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = symptom.titleFa,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedSymptomIds = if (checked) {
                                        selectedSymptomIds + symptom.id
                                    } else {
                                        selectedSymptomIds - symptom.id
                                    }
                                    diagnosticResults = null
                                }
                            )
                        }
                    }
                }

                // Selected Symptoms Summary Bar & Action Button
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(FarmDimens.Space8)
                    ) {
                        if (selectedSymptomIds.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${toPersianDigits(selectedSymptomIds.size)} نشانه انتخاب شده است.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        selectedSymptomIds = emptySet()
                                        diagnosticResults = null
                                    }
                                ) {
                                    Text("پاک‌کردن همه علائم", color = HealthRed, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                diagnosticResults = DiseaseDiagnosisEngine.diagnose(
                                    species = selectedSpecies,
                                    selectedSymptomIds = selectedSymptomIds,
                                    growthStage = selectedGrowthStage
                                )
                            },
                            enabled = selectedSymptomIds.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(FarmDimens.ButtonRadius)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(FarmDimens.Space8))
                            Text(
                                text = "تحلیل و غربالگری تشخیصی بیماری",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Diagnostic Results Section
                diagnosticResults?.let { results ->
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = FarmDimens.Space8),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        SectionHeader(
                            title = "۳) نتایج غربالگری بالینی",
                            subtitle = if (results.isEmpty()) "هیچ بیماری منطبقی با علائم فوق یافت نشد."
                            else "بیماری‌های محتمل بر اساس علائم ثبت‌شده (به ترتیب احتمال):"
                        )
                    }

                    if (results.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.CheckCircle,
                                title = "بیماری شاخصی یافت نشد",
                                message = "علائم ثبت‌شده با الگوهای بیماری‌های شایع در پایگاه داده مطابقت قطعی ندارد. در صورت تداوم، با دامپزشک مشورت نمایید."
                            )
                        }
                        item {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(HealthSubTab.VET_CONSULT.ordinal)
                                    }
                                },
                                shape = RoundedCornerShape(FarmDimens.ButtonRadius),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("مشاوره مستقیم با دامپزشک فارم")
                            }
                        }
                    } else {
                        items(results, key = { it.disease.id }) { result ->
                            val disease = result.disease
                            val probabilityColor = when {
                                result.probabilityPercent >= 65 -> HealthRed
                                result.probabilityPercent >= 40 -> FeedAmber
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Surface(
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, probabilityColor.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectDiseaseDetail(disease) }
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space12)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = disease.nameFa,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (result.isNutritional) FeedAmber.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (result.isNutritional) "کمبود تغذیه‌ای" else "عفونی",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (result.isNutritional) FeedAmber else MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${disease.scientificName} • عامل: ${disease.pathogenType.titleFa}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (result.ageCompatibilityNoteFa.isNotBlank()) {
                                                Text(
                                                    text = "تحلیل سنی: ${result.ageCompatibilityNoteFa}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                                            color = probabilityColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, probabilityColor.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = "${toPersianDigits(result.probabilityPercent)}٪ احتمال",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = probabilityColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(FarmDimens.Space8))

                                    // نوار پیشرفت گرافیکی احتمال انطباق
                                    LinearProgressIndicator(
                                        progress = { result.probabilityPercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = probabilityColor,
                                        trackColor = probabilityColor.copy(alpha = 0.15f)
                                    )

                                    if (disease.isUrgentEmergency) {
                                        Spacer(modifier = Modifier.height(FarmDimens.Space6))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "هشدار اورژانسی فارم (خطر تلفات بالا یا سرایت فوری)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFEF4444),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(FarmDimens.Space8))

                                    val matchedTitles: List<String> = result.matchedSymptoms.map { symOrTitle ->
                                        allSymptoms.find { it.id == symOrTitle }?.titleFa ?: symOrTitle
                                    }
                                    if (matchedTitles.isNotEmpty()) {
                                        Column {
                                            Text(
                                                text = "علائم هم‌پوشان شناسایی‌شده:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                matchedTitles.forEach { title ->
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                                    ) {
                                                        Text(
                                                            text = title,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    val matchedDrugs = remember(disease.commonDrugsFa) {
                                        MedicationCatalog.findMatchingMedications(disease.commonDrugsFa)
                                    }
                                    if (matchedDrugs.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(FarmDimens.Space6))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "داروهای مرتبط: ",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.horizontalScroll(rememberScrollState())
                                            ) {
                                                matchedDrugs.take(4).forEach { med ->
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                                        modifier = Modifier.clickable { selectedMedicationForDetail = med }
                                                    ) {
                                                        Text(
                                                            text = med.tradeNameFa,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(FarmDimens.Space8))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(HealthSubTab.VET_CONSULT.ordinal)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("مشاوره با دامپزشک", style = MaterialTheme.typography.labelSmall)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { onSelectDiseaseDetail(disease) }
                                        ) {
                                            Text(
                                                text = "مشاهده پروتکل درمان و داروها",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(FarmDimens.Space4))
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = HealthRed.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, HealthRed.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.MedicalServices,
                                        contentDescription = null,
                                        tint = HealthRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "توصیه حیاتی دامپزشکی و تجویز درمان",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HealthRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "غربالگری نرم‌افزار راهنمای بالینی اولیه است. تجویز دوز قطعی آنتی‌بیوتیک، درمان‌های ترکیبی یا واکسیناسیون اضطراری نیازمند بررسی تخصصی است.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(HealthSubTab.VET_CONSULT.ordinal)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ارتباط با دامپزشک و ارسال شرح‌حال گله", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Bottom safe space
                item {
                    Spacer(modifier = Modifier.height(FarmDimens.Space40))
                }
            }
        }

                // ==========================================
                // SUB-TAB 2: DRUGS (داروهای درمانی)
                // ==========================================
                HealthSubTab.DRUGS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = FarmDimens.Space16, vertical = FarmDimens.Space8),
                        verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
                    ) {
                        item {
                            SectionHeader(
                                title = "داروخانه تخصصی طیور (درمان بیماری‌ها)",
                                subtitle = "آنتی‌بیوتیک‌ها، ضدکوکسیدیوز، تنفسی، واکسن‌ها، دوز مصرف و دوره پرهیز"
                            )

                            // Search Input
                            OutlinedTextField(
                                value = drugSearchQuery,
                                onValueChange = { drugSearchQuery = it },
                                placeholder = { Text("جستجوی نام دارو، بیماری یا ماده مؤثره...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (drugSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { drugSearchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                singleLine = true
                            )
                        }

                        // Category Filter Chips
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedDrugCategory == null,
                                        onClick = { selectedDrugCategory = null },
                                        label = { Text("همه داروها (${toPersianDigits(MedicationCatalog.allDrugs.size)})") },
                                        shape = RoundedCornerShape(FarmDimens.ChipRadius)
                                    )
                                }
                                items(MedicationCatalog.drugCategories, key = { it.name }) { cat ->
                                    val isSelected = selectedDrugCategory == cat
                                    val count = MedicationCatalog.allDrugs.count { it.category == cat }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedDrugCategory = if (isSelected) null else cat },
                                        label = { Text("${cat.titleFa} (${toPersianDigits(count)})") },
                                        shape = RoundedCornerShape(FarmDimens.ChipRadius)
                                    )
                                }
                            }
                        }

                        if (filteredDrugs.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.Medication,
                                    title = "دارویی یافت نشد",
                                    message = "موردی با عبارت جستجوی فوق در بخش داروها پیدا نشد."
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = "نمایش ${toPersianDigits(filteredDrugs.size)} قلم داروی درمانی تخصصی:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            items(filteredDrugs, key = { it.id }) { med ->
                                MedicationCard(
                                    medication = med,
                                    onClick = { selectedMedicationForDetail = med }
                                )
                            }
                        }

                        // Bottom safe space
                        item {
                            Spacer(modifier = Modifier.height(FarmDimens.Space40))
                        }
                    }
                }

                // ==========================================
                // SUB-TAB 3: SUPPLEMENTS (مکمل‌ها و تقویتی‌ها)
                // ==========================================
                HealthSubTab.SUPPLEMENTS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = FarmDimens.Space16, vertical = FarmDimens.Space8),
                        verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
                    ) {
                        item {
                            SectionHeader(
                                title = "بانک مکمل‌ها و ویتامین‌های پرورشی",
                                subtitle = "مولتی‌ویتامین‌ها، الکترولیت، ضد استرس، پروبیوتیک و مواد معدنی"
                            )

                            // Search Input
                            OutlinedTextField(
                                value = suppSearchQuery,
                                onValueChange = { suppSearchQuery = it },
                                placeholder = { Text("جستجوی نام مکمل، ویتامین یا ترکیب تقویتی...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (suppSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { suppSearchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                singleLine = true
                            )
                        }

                        // Category Filter Chips
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FarmDimens.Space6)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedSuppCategory == null,
                                        onClick = { selectedSuppCategory = null },
                                        label = { Text("همه مکمل‌ها (${toPersianDigits(MedicationCatalog.allSupplements.size)})") },
                                        shape = RoundedCornerShape(FarmDimens.ChipRadius)
                                    )
                                }
                                items(MedicationCatalog.supplementCategories, key = { it.name }) { cat ->
                                    val isSelected = selectedSuppCategory == cat
                                    val count = MedicationCatalog.allSupplements.count { it.category == cat }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSuppCategory = if (isSelected) null else cat },
                                        label = { Text("${cat.titleFa} (${toPersianDigits(count)})") },
                                        shape = RoundedCornerShape(FarmDimens.ChipRadius)
                                    )
                                }
                            }
                        }

                        if (filteredSupplements.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.Vaccines,
                                    title = "مکملی یافت نشد",
                                    message = "موردی با عبارت جستجوی فوق در بخش مکمل‌ها پیدا نشد."
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = "نمایش ${toPersianDigits(filteredSupplements.size)} قلم مکمل و ویتامین پرورشی:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            items(filteredSupplements, key = { it.id }) { med ->
                                MedicationCard(
                                    medication = med,
                                    onClick = { selectedMedicationForDetail = med }
                                )
                            }
                        }

                        // Bottom safe space
                        item {
                            Spacer(modifier = Modifier.height(FarmDimens.Space40))
                        }
                    }
                }

                // ==========================================
                // SUB-TAB 4: BIOSECURITY (امنیت زیستی و پرهیز دارویی)
                // ==========================================
                HealthSubTab.BIOSECURITY -> {
                    val seasonalBio = remember {
                        com.example.poultryapp.engine.SeasonalBiosecurityEngine.getCurrentSeasonTheme()
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = FarmDimens.Space16, vertical = FarmDimens.Space8),
                        verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
                    ) {
                        // 1. Seasonal Biosecurity Theme & Risk Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(FarmDimens.CardRadius),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(FarmDimens.Space14)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Shield,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = seasonalBio.titleFa,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = seasonalBio.subtitleFa,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = seasonalBio.primaryRiskFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        // 2. Medication Withdrawal Period Tracking Header & Action
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "پایش پرهیز دارویی گله‌ها (Withdrawal Tracking)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "منع توزیع تخم‌مرغ و ذبح در دوره اثر آنتی‌بیوتیک",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        selectedWithdrawalFlockId = flocks.firstOrNull()?.id
                                        withdrawalMedName = ""
                                        withdrawalDaysText = "7"
                                        showWithdrawalDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = HealthRed)
                                ) {
                                    Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ثبت پرهیز", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 3. Flock Withdrawal Status Cards
                        if (flocks.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "گله‌ای تعریف نشده است.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        } else {
                            items(flocks, key = { "flock_with_${it.id}" }) { flock ->
                                Surface(
                                    shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                                    color = if (flock.isWithdrawalActive) HealthRedContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (flock.isWithdrawalActive) HealthRed.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
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
                                                text = flock.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (flock.isWithdrawalActive) {
                                                Text(
                                                    text = "⛔ دارو: ${flock.activeMedicationName ?: "نامشخص"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = HealthRedText,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "باقیمانده: ${toPersianDigits(flock.withdrawalRemainingDays)} روز و ${toPersianDigits(flock.withdrawalRemainingHours % 24)} ساعت | تخم‌مرغ غیرخوراکی",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = HealthRedText
                                                )
                                            } else {
                                                Text(
                                                    text = "وضعیت: عاری از منع دارویی (محصولات سالم و قابل توزیع)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = HealthGreen
                                                )
                                            }
                                        }

                                        if (flock.isWithdrawalActive) {
                                            OutlinedButton(
                                                onClick = { onClearFlockMedicationWithdrawal?.invoke(flock.id) },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, HealthRed),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HealthRed)
                                            ) {
                                                Text("پایان پرهیز", style = MaterialTheme.typography.labelSmall)
                                            }
                                        } else {
                                            TextButton(
                                                onClick = {
                                                    selectedWithdrawalFlockId = flock.id
                                                    withdrawalMedName = ""
                                                    withdrawalDaysText = "7"
                                                    showWithdrawalDialog = true
                                                }
                                            ) {
                                                Text("+ ثبت پرهیز", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Biosecurity Interactive Checklist Header
                        item {
                            SectionHeader(
                                title = "چک‌لیست تدابیر قرنطینه و بهداشت مزرعه",
                                subtitle = "کنترل تردد، حوضچه‌های ضدعفونی، مهار پرندگان وحشی و جوندگان"
                            )
                        }

                        if (biosecurityChecks.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "موردی در چک‌لیست امنیت زیستی بارگذاری نشده است.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        } else {
                            items(biosecurityChecks, key = { it.id }) { checkItem ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (checkItem.isCompleted) HealthGreenContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (checkItem.isCompleted) HealthGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onToggleBiosecurityCheck?.invoke(checkItem.id, !checkItem.isCompleted)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checkItem.isCompleted,
                                            onCheckedChange = { checked ->
                                                onToggleBiosecurityCheck?.invoke(checkItem.id, checked)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                ) {
                                                    Text(
                                                        text = checkItem.season.titleFa,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = checkItem.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = checkItem.description,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (checkItem.isCompleted && checkItem.completedDateJalaliStr != null) {
                                                Text(
                                                    text = "تکمیل شده در: ${toPersianDigits(checkItem.completedDateJalaliStr)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = HealthGreen
                                                )
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

                // ==========================================
                // SUB-TAB 5: VET CONSULT (ارتباط با دامپزشک)
                // ==========================================
                HealthSubTab.VET_CONSULT -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = FarmDimens.Space16, vertical = FarmDimens.Space8),
                        verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
                    ) {
                        item {
                            VetConsultationSection()
                        }
                        item {
                            Spacer(modifier = Modifier.height(FarmDimens.Space40))
                        }
                    }
                }
            }
        }
    }

    // Modal dialog for setting flock medication withdrawal
    if (showWithdrawalDialog) {
        Dialog(onDismissRequest = { showWithdrawalDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ثبت دوره پرهیز دارویی گله",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HealthRed
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "انتخاب گله مورد نظر:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(flocks) { f ->
                            FilterChip(
                                selected = selectedWithdrawalFlockId == f.id,
                                onClick = { selectedWithdrawalFlockId = f.id },
                                label = { Text(f.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = withdrawalMedName,
                        onValueChange = { withdrawalMedName = it },
                        label = { Text("نام دارو یا آنتی‌بیوتیک") },
                        placeholder = { Text("مثلاً داکسی‌سایکلین، انروسین، تایلوزین...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = withdrawalDaysText,
                        onValueChange = { withdrawalDaysText = it },
                        label = { Text("مدت دوره پرهیز (روز)") },
                        placeholder = { Text("مثلاً ۷") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showWithdrawalDialog = false }) {
                            Text("انصراف")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val fid = selectedWithdrawalFlockId
                                val days = withdrawalDaysText.toIntOrNull() ?: 7
                                if (fid != null && withdrawalMedName.isNotBlank()) {
                                    onSetFlockMedicationWithdrawal?.invoke(fid, days, withdrawalMedName.trim())
                                    showWithdrawalDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
                            enabled = selectedWithdrawalFlockId != null && withdrawalMedName.isNotBlank()
                        ) {
                            Text("تایید و فعال‌سازی پرهیز")
                        }
                    }
                }
            }
        }
    }

    // Modal dialog for medication clinical details
    selectedMedicationForDetail?.let { med ->
        MedicationDetailDialog(
            medication = med,
            onDismiss = { selectedMedicationForDetail = null }
        )
    }
}

/**
 * Clean, informative card for a medication / supplement in the list
 */
@Composable
private fun MedicationCard(
    medication: PoultryMedication,
    onClick: () -> Unit
) {
    val categoryColor = Color(medication.category.badgeColorHex)

    Surface(
        shape = RoundedCornerShape(FarmDimens.RadiusMedium),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(FarmDimens.Space12)) {
            // Header Row: Category Badge + Route Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = categoryColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = medication.category.titleFa,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = medication.administrationRoute.titleFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmDimens.Space8))

            // Trade & Generic Names
            Text(
                text = medication.tradeNameFa,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = medication.genericNameEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(FarmDimens.Space6))

            // Indication teaser
            AutoResizeText(
                text = "🎯 کاربرد: ${medication.indicationsFa}",
                targetFontSize = 12.sp,
                minFontSize = 9.sp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(FarmDimens.Space4))

            // Dosage teaser
            AutoResizeText(
                text = "⚖️ دوز مصرف: ${medication.dosageFa}",
                targetFontSize = 12.sp,
                minFontSize = 9.sp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )

            // Withdrawal period warning if any
            if (medication.withdrawalPeriodFa.contains("روز") && !medication.withdrawalPeriodFa.contains("۰ روز") && !medication.withdrawalPeriodFa.contains("فاقد")) {
                Spacer(modifier = Modifier.height(FarmDimens.Space4))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = HealthRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AutoResizeText(
                        text = "پرهیز از ذبح/مصرف: ${medication.withdrawalPeriodFa}",
                        targetFontSize = 11.sp,
                        minFontSize = 8.5.sp,
                        style = MaterialTheme.typography.labelSmall,
                        color = HealthRed,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // Active Ingredients Teaser
            AutoResizeText(
                text = "🧪 مواد مؤثره: ${medication.activeIngredientsFa}",
                targetFontSize = 12.sp,
                minFontSize = 9.sp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(FarmDimens.Space4))

            // Packaging
            AutoResizeText(
                text = "📦 شکل دارویی: ${medication.formAndPackaging}",
                targetFontSize = 11.sp,
                minFontSize = 8.5.sp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(FarmDimens.Space8))

            // Anti-distortion, clean full-width action button
            Surface(
                shape = RoundedCornerShape(FarmDimens.RadiusSmall),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FarmDimens.Space12, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مشاهده شناسنامه و مشخصات کامل",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        softWrap = false,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * SUB-TAB 3: Veterinarian Consultation Coming Soon Section
 */
@Composable
private fun VetConsultationSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FarmDimens.Space12)
    ) {
        // Hero Card
        Surface(
            shape = RoundedCornerShape(FarmDimens.CardRadius),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(FarmDimens.Space20),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FarmDimens.Space12))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "به زودی فعال می‌شود",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "سامانه ارتباط مستقیم با دامپزشک متخصص طیور",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(FarmDimens.Space6))

                Text(
                    text = "در بروزرسانی بعدی، امکان ویزیت آنلاین، بررسی تصاویر تلفات و کالبدگشایی و دریافت نسخه رسمی دارویی برای پرورش‌دهندگان فراهم خواهد شد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // Preview of Features
        SectionHeader(
            title = "خدماتی که در این بخش ارائه خواهند شد:",
            subtitle = "امکانات در حال توسعه و اتصال به کلینیک‌های معتبر دامپزشکی"
        )

        val plannedFeatures = listOf(
            Triple(
                Icons.Default.PhoneInTalk,
                "مشاوره صوتی و متنی آنلاین",
                "ارتباط مستقیم با دامپزشکان متخصص بهداشت و بیماری‌های طیور در سراسر کشور"
            ),
            Triple(
                Icons.Default.AddPhotoAlternate,
                "ارسال عکس کالبدگشایی و مدفوع",
                "آپلود تصاویر اندام‌های درگیر، چینه‌دان، سکوم و مدفوع جهت ارزیابی تشخیصی فوری"
            ),
            Triple(
                Icons.Default.Description,
                "تجویز پروتکل رسمی دارویی",
                "صدور نسخه الکترونیکی اختصاصی بر اساس وزن، سن و گونه گله شما"
            ),
            Triple(
                Icons.Default.NotificationsActive,
                "یادآور دوز و مهار مقاومت آنتی‌بیوتیکی",
                "تنظیم آلارم برای اتمام دقیق دوره‌های آنتی‌بیوتیکی و اعلام زمان پایان دوره پرهیز"
            )
        )

        plannedFeatures.forEach { (icon, title, desc) ->
            Surface(
                shape = RoundedCornerShape(FarmDimens.RadiusMedium),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(FarmDimens.Space12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(FarmDimens.Space12))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Emergency Veterinary Protocol Card
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        Surface(
            shape = RoundedCornerShape(FarmDimens.RadiusMedium),
            color = if (isDark) HealthRedContainerDark else HealthRedContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, if (isDark) HealthRedDark.copy(alpha = 0.4f) else HealthRed.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(FarmDimens.Space12),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Emergency,
                    contentDescription = null,
                    tint = if (isDark) HealthRedDark else HealthRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(FarmDimens.Space8))
                Column {
                    Text(
                        text = "اقدام فوری در شرایط تلفات ناگهانی و حاد:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) HealthRedDark else HealthRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "در صورت مشاهده تلفات تصاعدی یا علائم مشکوک به آنفلوانزای فوق حاد یا نیوکاسل تنفسی، سریعاً پرندگان درگیر را قرنطینه کامل کرده، لاشه‌ها را در کیسه نایلونی دربسته ایزوله نگه دارید و با نزدیک‌ترین اداره یا شبکه دامپزشکی منطقه تماس بگیرید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
