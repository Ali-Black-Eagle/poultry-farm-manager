package com.example.poultryapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import com.example.poultryapp.R
import com.example.poultryapp.data.local.CustomBreed
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.model.AgeOrderedQnA
import com.example.poultryapp.data.model.BreedingConsultationCatalog
import com.example.poultryapp.data.model.ConsultationTicket
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.engine.PoultryTemperatureEngine
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.*
import com.example.poultryapp.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private fun Any?.toPersianDigits(): String = com.example.poultryapp.engine.toPersianDigits(this)

/**
 * Bird species for the guide
 */
enum class GuideBird(
    val id: String,
    val titleFa: String,
    val subtitleFa: String,
    val imageRes: Int,
    val isMeatOnly: Boolean = false,
    val adultWeightKg: Double = 1.8
) {
    LAYER_HEN("layer_hen", "مرغ بومی و تخم‌گذار", "تولید تخم‌مرغ محلی و خانگی", R.drawable.bird_layer_chicken, false, 1.9),
    BROILER("broiler", "مرغ گوشتی", "پرواربندی سریع ۴۵ روزه برای گوشت", R.drawable.bird_broiler_chicken, true, 2.5),
    TURKEY("turkey", "بوقلمون گوشتی", "پرورش پرواری سنگین‌وزن در سالن یا باغ", R.drawable.bird_turkey, true, 12.0),
    QUAIL("quail", "بلدرچین", "سریع‌ترین رشد، فضای اندک و تخم‌گذاری ۴۰ روزگی", R.drawable.bird_quail, false, 0.22),
    DUCK("duck", "اردک (مرغابی)", "پرورش مقاوم در حیاط، باغ و استخر", R.drawable.bird_duck, false, 2.8),
    GOOSE("goose", "غاز", "پرورش چراکننده و بسیار مقاوم در مرتع", R.drawable.bird_goose, false, 5.0)
}

/**
 * تب‌های صفحه اصلی راهنمای جامع پرورش (پیش از انتخاب گونه)
 */
enum class GuideMainTab(val titleFa: String, val icon: ImageVector, val badgeFa: String? = null) {
    BIRDS("گونه‌ها", Icons.Default.Widgets, null),
    QNA_BANK("بانک سوالات", Icons.Default.QuestionAnswer, null),
    CONTACT_EXPERT("مشاوره", Icons.Default.SupportAgent, null)
}

/**
 * زیرشاخه‌های درون راهنمای پرنده انتخابی
 */
enum class GuideBranch(val titleFa: String, val icon: ImageVector) {
    CALCULATOR("ماشین‌حساب", Icons.Default.Calculate),
    FEEDS_AND_SUPPLEMENTS("دان و مکمل", Icons.Default.Grain),
    QNA_BANK("سوالات", Icons.Default.QuestionAnswer),
    CONTACT_EXPERT("مشاوره", Icons.Default.SupportAgent)
}

/**
 * Age milestones for the calculator
 */
data class AgeMilestone(
    val id: String,
    val labelFa: String,
    val days: Int,
    val feedPerBirdGrams: Double,
    val supplementsFa: String,
    val beginnerTipsFa: String,
    val indoorDensityBirdsPerM2: Double,
    val outdoorPastureM2PerBird: Double,
    val idealTempRangeFa: String = "",
    val tolerableTempRangeFa: String = "",
    val tempAdviceFa: String = ""
)

/**
 * Feed Bag item
 */
data class GuideFeedItem(
    val titleFa: String,
    val proteinPercent: String,
    val imageRes: Int,
    val descriptionFa: String,
    val targetAgesFa: String
)

/**
 * Supplement item
 */
data class GuideSupplementItem(
    val titleFa: String,
    val purposeFa: String,
    val imageRes: Int,
    val dosageFa: String,
    val criticalBenefitsFa: String
)

@Composable
fun BreedingGuideScreen(
    flocks: List<Flock> = emptyList(),
    selectedFlockId: Long? = null,
    customBreeds: List<CustomBreed> = emptyList(),
    farmAmbientTemp: Double? = null,
    initialMainTab: GuideMainTab = GuideMainTab.BIRDS,
    onOpenDrawer: () -> Unit,
    onOpenVoiceAssistant: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedBird by rememberSaveable { mutableStateOf<GuideBird?>(null) }
    val mainPagerState = rememberPagerState(initialPage = initialMainTab.ordinal) { GuideMainTab.entries.size }
    val branchPagerState = rememberPagerState(initialPage = 0) { GuideBranch.entries.size }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialMainTab) {
        if (mainPagerState.currentPage != initialMainTab.ordinal) {
            mainPagerState.scrollToPage(initialMainTab.ordinal)
        }
    }

    // Back handling: bird detail first
    BackHandler(enabled = selectedBird != null) {
        selectedBird = null
    }

    // Dialog state for viewing feed/supplement details
    var selectedFeedDetail by remember { mutableStateOf<GuideFeedItem?>(null) }
    var selectedSupplementDetail by remember { mutableStateOf<GuideSupplementItem?>(null) }

    // Q&A Bank States
    var selectedBirdFilter by rememberSaveable { mutableStateOf<String>("ALL") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var expandedQnAIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    // Contact Expert Form States
    var ticketBird by rememberSaveable { mutableStateOf(GuideBird.LAYER_HEN) }
    var ticketAgeDaysText by rememberSaveable { mutableStateOf("") }
    var ticketFlockCountText by rememberSaveable { mutableStateOf("") }
    var ticketQuestionText by rememberSaveable { mutableStateOf("") }

    // Sync bird branch settings on swipe
    LaunchedEffect(branchPagerState.currentPage, selectedBird) {
        if (selectedBird != null) {
            val branch = GuideBranch.entries[branchPagerState.currentPage]
            if (branch == GuideBranch.QNA_BANK) {
                selectedBirdFilter = selectedBird!!.id
            } else if (branch == GuideBranch.CONTACT_EXPERT) {
                ticketBird = selectedBird!!
            }
        }
    }
    var userTickets by rememberSaveable {
        mutableStateOf(
            listOf(
                ConsultationTicket(
                    id = "sample_ticket_1",
                    bird = GuideBird.LAYER_HEN,
                    birdAgeDays = 210,
                    flockCount = 100,
                    questionText = "پوسته تخم‌مرغ‌ها نازک شده و چند عدد تخم بدون پوسته (لمبه) در لانه مشاهده شده است. چه اقدامی فوری نیاز است؟",
                    timestampFormatted = "۱۴۰۳/۰۷/۰۱ - ۱۰:۱۵",
                    statusText = "پاسخ داده شده",
                    instantAdvice = "۱- پودر صدف دریایی درشت به میزان ۳ تا ۴ درصد در دان عصرگاهی مصرف شود.\n۲- مکمل ویتامین AD3E به مدت ۳ روز متوالی در آب آشامیدنی تجویز گردد.\n۳- شدت و ساعات نور سالن روی ۱۶ ساعت ثابت نگه داشته شود."
                )
            )
        )
    }
    var newlyCreatedTicketAdvice by remember { mutableStateOf<ConsultationTicket?>(null) }

    val onSubmitUserTicket: () -> Unit = {
        if (ticketQuestionText.trim().length < 10) {
            Toast.makeText(context, "لطفاً سوال خود را با شرح کافی بنویسید (حداقل ۱۰ حرف)", Toast.LENGTH_SHORT).show()
        } else {
            val age = ticketAgeDaysText.toIntOrNull()
            val count = ticketFlockCountText.toIntOrNull()
            val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
            val advice = BreedingConsultationCatalog.generateInstantAdvice(
                bird = ticketBird,
                questionText = ticketQuestionText,
                ageDays = age,
                flockCount = count
            )
            val newTicket = ConsultationTicket(
                id = UUID.randomUUID().toString().take(8),
                bird = ticketBird,
                birdAgeDays = age,
                flockCount = count,
                questionText = ticketQuestionText.trim(),
                timestampFormatted = sdf.format(Date()).toPersianDigits(),
                statusText = "در حال بررسی کارشناس",
                instantAdvice = advice
            )
            userTickets = listOf(newTicket) + userTickets
            newlyCreatedTicketAdvice = newTicket
            ticketQuestionText = ""
            Toast.makeText(context, "سوال شما با موفقیت ثبت شد و پاسخ اولیه صادر گردید.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            if (selectedBird == null) {
                Column {
                    AppTopBar(
                        title = "راهنمای جامع پرورش طیور",
                        subtitle = "آموزش دوره‌ای، بانک سوالات و ارتباط با کارشناس",
                        logoRes = R.drawable.ic_farm_guide,
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
                    val mainTabItems = remember {
                        GuideMainTab.entries.map {
                            FarmSubTabItem(title = it.titleFa, icon = it.icon, badge = it.badgeFa)
                        }
                    }
                    FarmSubTabBar(
                        items = mainTabItems,
                        selectedIndex = mainPagerState.currentPage,
                        pagerState = mainPagerState,
                        onTabSelected = { page ->
                            coroutineScope.launch {
                                mainPagerState.animateScrollToPage(page)
                            }
                        }
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedBird = null }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بازگشت",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "پرورش ${selectedBird!!.titleFa}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedBird!!.subtitleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Branch Selector Tabs: ماشین‌حساب سن و دان / دان و مکمل‌ها / بانک سوال و جواب / ارتباط با کارشناس
                        val branchTabItems = remember {
                            GuideBranch.entries.map {
                                FarmSubTabItem(title = it.titleFa, icon = it.icon)
                            }
                        }
                        FarmSubTabBar(
                            items = branchTabItems,
                            selectedIndex = branchPagerState.currentPage,
                            pagerState = branchPagerState,
                            onTabSelected = { page ->
                                coroutineScope.launch {
                                    branchPagerState.animateScrollToPage(page)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedBird == null) {
                // ========================================================
                // صفحه ۱: حالت کلی (انتخاب گونه‌ها / بانک سوالات / ارتباط با کارشناس)
                // با قابلیت سوایپ افقی به چپ و راست بین برگه‌ها
                // ========================================================
                HorizontalPager(
                    state = mainPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (GuideMainTab.entries[page]) {
                        GuideMainTab.BIRDS -> {
                            BirdSelectionGrid(
                                birds = GuideBird.entries,
                                onSelectBird = { bird ->
                                    selectedBird = bird
                                    selectedBirdFilter = bird.id
                                    ticketBird = bird
                                    coroutineScope.launch {
                                        branchPagerState.scrollToPage(GuideBranch.CALCULATOR.ordinal)
                                    }
                                }
                            )
                        }
                        GuideMainTab.QNA_BANK -> {
                            GuideQnABankSection(
                                selectedBirdFilter = selectedBirdFilter,
                                onSelectBirdFilter = { selectedBirdFilter = it },
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                expandedQnAIds = expandedQnAIds,
                                onToggleExpand = { id ->
                                    expandedQnAIds = if (expandedQnAIds.contains(id)) expandedQnAIds - id else expandedQnAIds + id
                                }
                            )
                        }
                        GuideMainTab.CONTACT_EXPERT -> {
                            GuideContactExpertSection(
                                ticketBird = ticketBird,
                                onSelectBird = { ticketBird = it },
                                ageDaysText = ticketAgeDaysText,
                                onAgeChange = { ticketAgeDaysText = it },
                                flockCountText = ticketFlockCountText,
                                onFlockCountChange = { ticketFlockCountText = it },
                                questionText = ticketQuestionText,
                                onQuestionChange = { ticketQuestionText = it },
                                userTickets = userTickets,
                                onSubmitTicket = onSubmitUserTicket
                            )
                        }
                    }
                }
            } else {
                // ========================================================
                // صفحه ۲: بخش پرورش اختصاصی پرنده انتخابی (شاخه‌ها)
                // با قابلیت سوایپ افقی به چپ و راست بین شاخه‌ها
                // ========================================================
                HorizontalPager(
                    state = branchPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (GuideBranch.entries[page]) {
                        GuideBranch.CALCULATOR -> {
                            BirdCalculatorBranch(
                                bird = selectedBird!!,
                                onFeedClick = {
                                    coroutineScope.launch {
                                        branchPagerState.animateScrollToPage(GuideBranch.FEEDS_AND_SUPPLEMENTS.ordinal)
                                    }
                                }
                            )
                        }
                        GuideBranch.FEEDS_AND_SUPPLEMENTS -> {
                            FeedsAndSupplementsBranch(
                                onSelectFeed = { selectedFeedDetail = it },
                                onSelectSupplement = { selectedSupplementDetail = it }
                            )
                        }
                        GuideBranch.QNA_BANK -> {
                            GuideQnABankSection(
                                selectedBirdFilter = selectedBirdFilter,
                                onSelectBirdFilter = { selectedBirdFilter = it },
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                expandedQnAIds = expandedQnAIds,
                                onToggleExpand = { id ->
                                    expandedQnAIds = if (expandedQnAIds.contains(id)) expandedQnAIds - id else expandedQnAIds + id
                                }
                            )
                        }
                        GuideBranch.CONTACT_EXPERT -> {
                            GuideContactExpertSection(
                                ticketBird = ticketBird,
                                onSelectBird = { ticketBird = it },
                                ageDaysText = ticketAgeDaysText,
                                onAgeChange = { ticketAgeDaysText = it },
                                flockCountText = ticketFlockCountText,
                                onFlockCountChange = { ticketFlockCountText = it },
                                questionText = ticketQuestionText,
                                onQuestionChange = { ticketQuestionText = it },
                                userTickets = userTickets,
                                onSubmitTicket = onSubmitUserTicket
                            )
                        }
                    }
                }
            }
        }
    }

    // Feed Detail Dialog
    if (selectedFeedDetail != null) {
        AlertDialog(
            onDismissRequest = { selectedFeedDetail = null },
            title = {
                Text(
                    text = selectedFeedDetail!!.titleFa,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Image(
                            painter = painterResource(id = selectedFeedDetail!!.imageRes),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("درصد پروتئین خام:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(selectedFeedDetail!!.proteinPercent, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("دوره مصرف:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(selectedFeedDetail!!.targetAgesFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text("ویژگی و کاربرد:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(selectedFeedDetail!!.descriptionFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFeedDetail = null }) {
                    Text("متوجه شدم", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Supplement Detail Dialog
    if (selectedSupplementDetail != null) {
        AlertDialog(
            onDismissRequest = { selectedSupplementDetail = null },
            title = {
                Text(
                    text = selectedSupplementDetail!!.titleFa,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        Image(
                            painter = painterResource(id = selectedSupplementDetail!!.imageRes),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WaterBlueContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("دوز مصرف در آب یا دان:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = WaterBlueText)
                            Text(selectedSupplementDetail!!.dosageFa, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = WaterBlueText)
                        }
                    }

                    Text("هدف و فایده اصلی:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(selectedSupplementDetail!!.purposeFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)

                    Text("نکات طلایی مصرف:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(selectedSupplementDetail!!.criticalBenefitsFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSupplementDetail = null }) {
                    Text("بستن", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // دیالوگ نمایش پاسخ فوری کارشناس به سوال جدید کاربر
    if (newlyCreatedTicketAdvice != null) {
        val ticket = newlyCreatedTicketAdvice!!
        AlertDialog(
            onDismissRequest = { newlyCreatedTicketAdvice = null },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "ثبت موفق و راهنمای اولیه کارشناسی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "گونه: ${ticket.bird.titleFa}" +
                                (if (ticket.birdAgeDays != null) " | سن: ${ticket.birdAgeDays.toPersianDigits()} روز" else "") +
                                (if (ticket.flockCount != null) " | تعداد: ${ticket.flockCount.toPersianDigits()} قطعه" else ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ticket.questionText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "راهکار و توصیه کارشناس پرورش:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ticket.instantAdvice,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { newlyCreatedTicketAdvice = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("متوجه شدم")
                }
            }
        )
    }
}

/**
 * ========================================================
 * ۱. صفحه انتخاب پرنده با کارت‌های کاملاً مربعی (Square Grid)
 * ========================================================
 */
@Composable
private fun BirdSelectionGrid(
    birds: List<GuideBird>,
    onSelectBird: (GuideBird) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(birds, key = { it.id }) { bird ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .aspectRatio(1f) // Square card
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectBird(bird) }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Photo
                    Image(
                        painter = painterResource(id = bird.imageRes),
                        contentDescription = bird.titleFa,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient Scrim overlay at bottom for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.85f)
                                    ),
                                    startY = 100f
                                )
                            )
                    )

                    // Texts on card
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = bird.titleFa,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        AutoResizeText(
                            text = bird.subtitleFa,
                            targetFontSize = 11.sp,
                            minFontSize = 8.5.sp,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * ========================================================
 * ۲. شاخه ماشین‌حساب سن و دان و فضا (Calculator Branch)
 * ========================================================
 */
@Composable
private fun BirdCalculatorBranch(
    bird: GuideBird,
    onFeedClick: () -> Unit
) {
    var birdCountText by rememberSaveable { mutableStateOf("100") }
    val birdCount = birdCountText.toIntOrNull() ?: 100

    val milestones = remember(bird) { getMilestonesForBird(bird) }
    var selectedMilestoneId by rememberSaveable(bird) {
        mutableStateOf(milestones.firstOrNull()?.id ?: "day_1")
    }

    val activeMilestone = remember(milestones, selectedMilestoneId) {
        milestones.find { it.id == selectedMilestoneId } ?: milestones.first()
    }

    // Calculations
    val dailyFeedGramsTotal = birdCount * activeMilestone.feedPerBirdGrams
    val dailyFeedKg = dailyFeedGramsTotal / 1000.0
    val monthlyFeedKg = dailyFeedKg * 30.0
    val monthly50KgBags = (monthlyFeedKg / 50.0)

    val minIndoorAreaM2 = (birdCount / activeMilestone.indoorDensityBirdsPerM2).coerceAtLeast(1.0)
    val minPastureAreaM2 = (birdCount * activeMilestone.outdoorPastureM2PerBird).coerceAtLeast(1.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Input: Bird Count
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تعداد پرنده‌های شما:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(20, 50, 100, 500).forEach { preset ->
                                OutlinedButton(
                                    onClick = { birdCountText = preset.toString() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(FarmDimens.RadiusSmall)
                                ) {
                                    Text("${toPersianDigits(preset)}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = birdCountText,
                        onValueChange = { birdCountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("تعداد پرنده (قطعه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // 2. Input: Age Milestones
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "سن پرنده را انتخاب کنید:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        milestones.forEach { m ->
                            val isSelected = selectedMilestoneId == m.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMilestoneId = m.id },
                                label = {
                                    Text(
                                        text = m.labelFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Output 1: Daily and Monthly Feed Needs
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = FeedAmberContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, FeedAmber.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = FeedAmber, modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Grain, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("خوراک مورد نیاز برای ${toPersianDigits(birdCount)} قطعه پرنده", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = FeedAmberText)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Daily feed
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("مصرف دان روزانه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${toPersianDigits(if (dailyFeedKg < 10) "%.1f".format(dailyFeedKg) else dailyFeedKg.toInt())} کیلوگرم",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FeedAmberText
                                )
                                Text(
                                    text = "هر قطعه: ${toPersianDigits(activeMilestone.feedPerBirdGrams.toInt())} گرم",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Monthly feed
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("مصرف دان ماهانه (۳۰ روز)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${toPersianDigits(monthlyFeedKg.toInt())} کیلوگرم",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FeedAmberText
                                )
                                Text(
                                    text = "معادل ${toPersianDigits("%.1f".format(monthly50KgBags))} گونی ۵۰ کیلویی",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Output 2: Minimum Space Required (Sleeping/Shelter & Pasture Range)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = HealthGreenContainer, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.HomeWork, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حداقل مساحت مورد نیاز برای گله", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sleeping Shelter
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("فضای مسقف خواب و زمستان", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${toPersianDigits(if (minIndoorAreaM2 < 10) "%.1f".format(minIndoorAreaM2) else minIndoorAreaM2.toInt())} متر مربع",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "تراکم: ${toPersianDigits(activeMilestone.indoorDensityBirdsPerM2.toInt())} قطعه/m²",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Outdoor Pasture
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = HealthGreenContainer.copy(alpha = 0.35f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("محوطه باز چرای پرنده", style = MaterialTheme.typography.labelSmall, color = HealthGreenText)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${toPersianDigits(if (minPastureAreaM2 < 10) "%.1f".format(minPastureAreaM2) else minPastureAreaM2.toInt())} متر مربع",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HealthGreenText
                                )
                                Text(
                                    text = "برای هر پرنده: ${toPersianDigits(activeMilestone.outdoorPastureM2PerBird)} m²",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HealthGreenText.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4.5 Output: Ideal Standard Temperature for this Age
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Thermostat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "دمای استاندارد سالن در این سن",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = activeMilestone.idealTempRangeFa,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (activeMilestone.tolerableTempRangeFa.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "محدوده تحمل ایمن پرنده: ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeMilestone.tolerableTempRangeFa,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (activeMilestone.tempAdviceFa.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeMilestone.tempAdviceFa,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // 5. Output 3: Required Supplements for this Age
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = WaterBlueContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, WaterBlue.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = WaterBlue, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مکمل‌های ضروری برای سن ${activeMilestone.labelFa}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WaterBlueText)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activeMilestone.supplementsFa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = onFeedClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("مشاهده راهنمای کامل دان و مکمل‌ها ←", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 6. Output 4: Critical Tips for Beginners in this Age
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نکات حیاتی و تجربی که افراد تازه‌کار باید بدانند", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = activeMilestone.beginnerTipsFa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * ========================================================
 * ۳. شاخه دان و مکمل‌ها (Feeds & Supplements Branch)
 * ========================================================
 */
@Composable
private fun FeedsAndSupplementsBranch(
    onSelectFeed: (GuideFeedItem) -> Unit,
    onSelectSupplement: (GuideSupplementItem) -> Unit
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Feeds, 1 = Supplements

    val feeds = remember { getProFarmFeeds() }
    val supplements = remember { getVeterinarySupplements() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle Buttons (دان‌های پروفارم / مکمل‌ها)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    leadingIcon = { Icon(Icons.Default.Grain, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("دان‌های پروفارم (ProFarm)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("مکمل‌های ضروری پرورشی", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (subTab == 0) {
            // FEEDS GRID (Square Cards with ProFarm bags)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(feeds, key = { it.titleFa }) { feed ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectFeed(feed) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = feed.imageRes),
                                contentDescription = feed.titleFa,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Top protein badge
                            Surface(
                                shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = feed.proteinPercent,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Bottom Title Gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = feed.titleFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "پروفارم • جزئیات و مصرف",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // SUPPLEMENTS GRID (Square Cards without brand)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(supplements, key = { it.titleFa }) { supp ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectSupplement(supp) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = supp.imageRes),
                                contentDescription = supp.titleFa,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Bottom Title Gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = supp.titleFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    AutoResizeText(
                                        text = supp.purposeFa,
                                        targetFontSize = 10.sp,
                                        minFontSize = 8.sp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1
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

/**
 * Feeds catalog data (ProFarm brand)
 */
private fun getProFarmFeeds(): List<GuideFeedItem> {
    return listOf(
        GuideFeedItem(
            titleFa = "دان استارتر پروفارم",
            proteinPercent = "پروتئین ۲۱-۲۲٪",
            imageRes = R.drawable.feed_starter,
            descriptionFa = "دان پودری بسیار نرم و پرانرژی با قابلیت هضم عالی، حاوی اسیدهای آمینه ضروری متیونین و لیزین جهت رشد یکنواخت جوجه در حساس‌ترین روزها.",
            targetAgesFa = "روز اول تا سن ۱۴ روزگی جوجه"
        ),
        GuideFeedItem(
            titleFa = "پیش‌دان پروفارم",
            proteinPercent = "پروتئین ۲۰-۲۱٪",
            imageRes = R.drawable.feed_pre,
            descriptionFa = "کرامبل دانه ریز با بالانس دقیق کلسیم و فسفر، استحکام بخشیدن به اسکلت و تسریع کامل پردرآوری جوجه‌ها.",
            targetAgesFa = "هفته دوم تا هفته چهارم (روز ۱۵ تا ۲۸)"
        ),
        GuideFeedItem(
            titleFa = "میان‌دان پروفارم",
            proteinPercent = "پروتئین ۱۸-۱۹٪",
            imageRes = R.drawable.feed_grower,
            descriptionFa = "پلیت سایز متوسط پرورشی جهت عضله‌سازی، افزایش مقاومت بدنی و تأمین انرژی پایدار دوره رشد.",
            targetAgesFa = "ماه دوم (سن ۲۹ تا ۶۰ روزگی)"
        ),
        GuideFeedItem(
            titleFa = "پایان‌دان پروفارم",
            proteinPercent = "پروتئین ۱۶-۱۷٪",
            imageRes = R.drawable.feed_finisher,
            descriptionFa = "جیره پرواربندی بدون آنتی‌بیوتیک با طعم عالی، افزایش حداکثری وزن پیش از ذبح یا بلوغ پرنده.",
            targetAgesFa = "سن ۲ ماهگی به بعد تا ذبح یا ورود به پیش‌تولید"
        ),
        GuideFeedItem(
            titleFa = "دان تخم‌گذار پروفارم",
            proteinPercent = "پروتئین ۱۶.۵-۱۷.۵٪",
            imageRes = R.drawable.feed_layer,
            descriptionFa = "جیره تخصصی تولید تخم‌مرغ با ۳.۸٪ کلسیم فعال و رنگدانه‌های طبیعی گیاهی جهت زرده پررنگ و پوسته فوق‌العاده محکم.",
            targetAgesFa = "شروع تخم‌گذاری (از ماه پنجم به بعد)"
        ),
        GuideFeedItem(
            titleFa = "دان اقتصادی پروفارم (۱۰٪)",
            proteinPercent = "پروتئین ۱۰٪",
            imageRes = R.drawable.feed_economy,
            descriptionFa = "جیره نگهدارنده ارزان‌قیمت و باصرفه برای پرندگان بومی، چرای آزاد در باغ، دوره پرریزی (لک) و مرغ‌های غیرتولیدی.",
            targetAgesFa = "پرندگان بالغ بومی در شرایط چرا و نگهداری کم‌هزینه"
        )
    )
}

/**
 * Supplements catalog data (Unbranded)
 */
private fun getVeterinarySupplements(): List<GuideSupplementItem> {
    return listOf(
        GuideSupplementItem(
            titleFa = "مولتی‌ویتامین + الکترولیت",
            purposeFa = "رفع فوری استرس، گرمازدگی و جابجایی",
            imageRes = R.drawable.supp_multivitamin,
            dosageFa = "۱ گرم در هر لیتر آب آشامیدنی (۳ روز متوالی)",
            criticalBenefitsFa = "حیاتی‌ترین مکمل برای روز اول ورود جوجه‌ها، زمان واکسیناسیون و روزهای گرم تابستان جهت جلوگیری از شوک و افت ناگهانی راندمان."
        ),
        GuideSupplementItem(
            titleFa = "پروبیوتیک پرورشی",
            purposeFa = "تقویت پرزهای روده، هضم کامل و ضد اسهال",
            imageRes = R.drawable.supp_probiotic,
            dosageFa = "۰.۵ تا ۱ گرم در هر کیلوگرم دان یا لیتر آب",
            criticalBenefitsFa = "باکتری‌های مفید روده را جایگزین میکروب‌های بیماری‌زا می‌کند، بوی نامطبوع گاز آمونیاک مدفوع را به شدت کاهش می‌دهد و نیاز به دارو را به صفر نزدیک می‌کند."
        ),
        GuideSupplementItem(
            titleFa = "ویتامین بی کمپلکس (B-Complex)",
            purposeFa = "سلامت سیستم عصبی و ممانعت از فلجی پاها",
            imageRes = R.drawable.supp_b_complex,
            dosageFa = "۱ سی‌سی در هر لیتر آب آشامیدنی",
            criticalBenefitsFa = "کمبود ویتامین‌های گروه B به خصوص B2 باعث پیچ‌خوردگی انگشتان پای جوجه (Curled-toe paralysis) می‌شود. مصرف این شربت سلامت راه رفتن جوجه را تضمین می‌کند."
        ),
        GuideSupplementItem(
            titleFa = "نوتری‌وت (رویان دارو)",
            purposeFa = "مکمل رشد حاوی اسیدهای آمینه و ویتامین‌های نایاب",
            imageRes = R.drawable.supp_nutrivet,
            dosageFa = "۱ تا ۲ گرم در هر لیتر آب آشامیدنی",
            criticalBenefitsFa = "افزایش محسوس یکنواختی گله، رفع کزکردگی و تسریع وزن‌گیری در دوره رشد و جبران عقب‌ماندگی رشدی."
        ),
        GuideSupplementItem(
            titleFa = "پودر صدف و کلسیم خوراکی",
            purposeFa = "استحکام استخوان و تولید پوسته تخم‌مرغ ضخیم",
            imageRes = R.drawable.supp_calcium_oyster,
            dosageFa = "مخلوط آزاد در جیره یا در ظرف دانخوری مجزا",
            criticalBenefitsFa = "جلوگیری قطعی از تخم‌مرغ‌های لمبه (بدون پوسته یا پوسته نازک) و مانع از شکستگی استخوان پای مرغ‌های تخم‌گذار در دوران پیک تولید."
        )
    )
}

/**
 * Returns tailored milestones for each bird species with scientific temperature integration
 */
private fun getMilestonesForBird(bird: GuideBird): List<AgeMilestone> {
    val rawMilestones = when (bird) {
        GuideBird.QUAIL -> listOf(
            AgeMilestone(
                id = "day_1",
                labelFa = "۱ روزه (ورود)",
                days = 1,
                feedPerBirdGrams = 4.0,
                supplementsFa = "آب‌شکر ۵٪ در ۲ ساعت اول + مولتی‌ویتامین الکترولیت. دان استارتر نرم پودری با ۲۴٪ پروتئین.",
                beginnerTipsFa = "دمای محفظه زیر لامپ باید ۳۶-۳۷ درجه باشد. به علت جثه کوچک جوجه بلدرچین، در آبخوری تیله یا سنگ‌ریزه بگذارید تا غرق نشوند!",
                indoorDensityBirdsPerM2 = 80.0,
                outdoorPastureM2PerBird = 0.05
            ),
            AgeMilestone(
                id = "day_10",
                labelFa = "۱۰ روزه",
                days = 10,
                feedPerBirdGrams = 9.0,
                supplementsFa = "ویتامین B-Complex در آب (هفته‌ای ۲ بار) جهت سلامت اعصاب و پاها.",
                beginnerTipsFa = "دما روزانه نیم درجه کاهش یابد (حدود ۳۱ درجه). جوجه‌ها بسیار پرتحرک هستند؛ مراقب باشید از لبه کارتن بیرون نپرند.",
                indoorDensityBirdsPerM2 = 60.0,
                outdoorPastureM2PerBird = 0.08
            ),
            AgeMilestone(
                id = "day_30",
                labelFa = "۳۰ روزه (۱ ماهه)",
                days = 30,
                feedPerBirdGrams = 18.0,
                supplementsFa = "پروبیوتیک و پودر صدف ریز در دان برای استحکام اسکلت.",
                beginnerTipsFa = "دما ۲۲ تا ۲۴ درجه. پرنده‌ها پرهای کامل دارند و آماده ورود به قفس‌های شیب‌دار یا سالن اصلی هستند.",
                indoorDensityBirdsPerM2 = 35.0,
                outdoorPastureM2PerBird = 0.1
            ),
            AgeMilestone(
                id = "day_45",
                labelFa = "۴۵ روزه (شروع تخم‌گذاری)",
                days = 45,
                feedPerBirdGrams = 28.0,
                supplementsFa = "دان تخم‌گذار بلدرچین + کلسیم و ویتامین AD3E برای تخم‌های پرشمار.",
                beginnerTipsFa = "۱۶ ساعت نور ثابت روزانه الزامی است. بلدرچین‌ها از سن ۴۰ تا ۴۵ روزگی روزانه یک عدد تخم می‌گذارند!",
                indoorDensityBirdsPerM2 = 25.0,
                outdoorPastureM2PerBird = 0.15
            )
        )

        GuideBird.BROILER -> listOf(
            AgeMilestone(
                id = "day_1",
                labelFa = "۱ روزه",
                days = 1,
                feedPerBirdGrams = 15.0,
                supplementsFa = "آب‌شکر ۵٪ در ۲ ساعت اول + مولتی‌ویتامین و الکترولیت. دان سوپر استارتر کرامبل.",
                beginnerTipsFa = "دمای سالن ۳۳ تا ۳۵ درجه. کف سالن حتماً ۵ سانتی‌متر پوشال درشت نجاری بریزید (هرگز روزنامه صاف نگذارید).",
                indoorDensityBirdsPerM2 = 35.0,
                outdoorPastureM2PerBird = 0.1
            ),
            AgeMilestone(
                id = "day_10",
                labelFa = "۱۰ روزه",
                days = 10,
                feedPerBirdGrams = 35.0,
                supplementsFa = "قطره چشمی واکسن برونشیت/نیوکاسل + ویتامین B-Complex در آب.",
                beginnerTipsFa = "دما ۳۰ درجه. تهویه ملایم شروع شود تا بوی آمونیاک از سالن خارج شود ولی باد مستقیم به جوجه نخورد.",
                indoorDensityBirdsPerM2 = 22.0,
                outdoorPastureM2PerBird = 0.15
            ),
            AgeMilestone(
                id = "day_30",
                labelFa = "۳۰ روزه (۱ ماهه)",
                days = 30,
                feedPerBirdGrams = 110.0,
                supplementsFa = "پروبیوتیک در دان + میان‌دان مرغ گوشتی با پروتئین ۱۹٪.",
                beginnerTipsFa = "دما ۲۳ تا ۲۵ درجه. سرعت رشد فوق‌العاده بالاست؛ دانخوری‌ها هرگز نباید خالی بمانند.",
                indoorDensityBirdsPerM2 = 12.0,
                outdoorPastureM2PerBird = 0.25
            ),
            AgeMilestone(
                id = "day_45",
                labelFa = "۴۲-۴۵ روزه (پایان دوره)",
                days = 45,
                feedPerBirdGrams = 175.0,
                supplementsFa = "پایان‌دان بدون دارو به همراه آب خنک تمیز.",
                beginnerTipsFa = "وزن پرنده به حدود ۲.۵ تا ۲.۸ کیلوگرم رسیده و آماده ذبح است. در ساعات پایانی دوره از استرس گرما جلوگیری کنید.",
                indoorDensityBirdsPerM2 = 9.0,
                outdoorPastureM2PerBird = 0.3
            )
        )

        GuideBird.TURKEY -> listOf(
            AgeMilestone(
                id = "day_1",
                labelFa = "۱ روزه",
                days = 1,
                feedPerBirdGrams = 20.0,
                supplementsFa = "آب‌شکر ۵٪ + مولتی‌ویتامین و الکترولیت. دان سوپر استارتر بوقلمون با ۲۸٪ پروتئین خالص.",
                beginnerTipsFa = "دمای زیر لامپ ۳۶ تا ۳۷ درجه. جوجه بوقلمون بینایی ضعیفی دارد؛ منقار هر جوجه را تک‌تک در آبخوری بزنید تا یاد بگیرند.",
                indoorDensityBirdsPerM2 = 25.0,
                outdoorPastureM2PerBird = 0.3
            ),
            AgeMilestone(
                id = "day_10",
                labelFa = "۱۰ روزه",
                days = 10,
                feedPerBirdGrams = 45.0,
                supplementsFa = "ویتامین B-Complex برای جلوگیری از تاول مفصلی و فلجی پاها.",
                beginnerTipsFa = "دما ۳۳ تا ۳۴ درجه. حلقه برودر را بزرگ‌تر کنید تا جوجه‌ها روی هم تلنبار نشوند.",
                indoorDensityBirdsPerM2 = 15.0,
                outdoorPastureM2PerBird = 0.5
            ),
            AgeMilestone(
                id = "day_30",
                labelFa = "۳۰ روزه (۱ ماهه)",
                days = 30,
                feedPerBirdGrams = 120.0,
                supplementsFa = "دان رشد بوقلمون (پروتئین ۲۴٪) + پروبیوتیک روده.",
                beginnerTipsFa = "دما ۲۶ تا ۲۸ درجه. بستر خیس را فوراً تعویض کنید؛ بوقلمون به بستر آلوده بسیار حساس است.",
                indoorDensityBirdsPerM2 = 8.0,
                outdoorPastureM2PerBird = 1.0
            ),
            AgeMilestone(
                id = "day_60",
                labelFa = "۶۰ روزه (۲ ماهه)",
                days = 60,
                feedPerBirdGrams = 240.0,
                supplementsFa = "میان‌دان بوقلمون + مکمل‌های تقویتی کلسیم و فسفر استخوان‌سازی.",
                beginnerTipsFa = "دما ۲۰ تا ۲۲ درجه. بوقلمون‌ها بسیار پرطاقت می‌شوند و می‌توانند ساعاتی در حیاط یا مرتع چرا کنند.",
                indoorDensityBirdsPerM2 = 4.0,
                outdoorPastureM2PerBird = 2.0
            ),
            AgeMilestone(
                id = "day_90",
                labelFa = "۹۰ روزه (۳ ماهه)",
                days = 90,
                feedPerBirdGrams = 350.0,
                supplementsFa = "پایان‌دان بوقلمون + علوفه خردشده تمیز.",
                beginnerTipsFa = "کنترل وزن و سلامت پاها؛ تأمین دانخوری‌های سنگین تا واژگون نشوند.",
                indoorDensityBirdsPerM2 = 3.0,
                outdoorPastureM2PerBird = 3.0
            ),
            AgeMilestone(
                id = "day_120",
                labelFa = "۱۲۰ روزه (۴ ماهه)",
                days = 120,
                feedPerBirdGrams = 420.0,
                supplementsFa = "پایان‌دان پرواری + غلات سبوس‌دار.",
                beginnerTipsFa = "وزن پرنده به حدود ۹ تا ۱۱ کیلوگرم می‌رسد. در تابستان آبخوری‌ها همیشه در سایه خنک باشند.",
                indoorDensityBirdsPerM2 = 2.5,
                outdoorPastureM2PerBird = 3.5
            ),
            AgeMilestone(
                id = "day_150",
                labelFa = "۱۵۰ روزه (بلوغ/کشتار)",
                days = 150,
                feedPerBirdGrams = 480.0,
                supplementsFa = "جیره نگهداری یا پرواری پایانی بدون دارو.",
                beginnerTipsFa = "بلوغ کامل یا آماده‌سازی برای کشتار با حداکثر وزن گوشتی (۱۲ تا ۱۴ کیلوگرم).",
                indoorDensityBirdsPerM2 = 2.0,
                outdoorPastureM2PerBird = 4.0
            )
        )

        GuideBird.DUCK -> listOf(
            AgeMilestone(
                id = "day_1",
                labelFa = "۱ روزه (ورود)",
                days = 1,
                feedPerBirdGrams = 18.0,
                supplementsFa = "آب ولرم با ۵٪ شکر + مولتی‌ویتامین و الکترولیت. دان سوپر استارتر خیس‌خورده یا کرامبل ریز.",
                beginnerTipsFa = "دمای محفظه زیر لامپ ۳۰ تا ۳۲ درجه. اردک‌ها بسیار به آب علاقه دارند اما جوجه ۱ روزه نباید خیس شود زیرا چربی پرها هنوز تشکیل نشده است.",
                indoorDensityBirdsPerM2 = 25.0,
                outdoorPastureM2PerBird = 0.2
            ),
            AgeMilestone(
                id = "day_10",
                labelFa = "۱۰ روزه",
                days = 10,
                feedPerBirdGrams = 50.0,
                supplementsFa = "دان استارتر با پروتئین ۲۰٪ + ویتامین B-Complex در آب برای تقویت اعصاب پا.",
                beginnerTipsFa = "دما ۲۸ تا ۳۰ درجه. بستر اردک‌ها خیلی زود مرطوب می‌شود؛ پوشال خشک روزانه اضافه کنید.",
                indoorDensityBirdsPerM2 = 15.0,
                outdoorPastureM2PerBird = 0.5
            ),
            AgeMilestone(
                id = "day_30",
                labelFa = "۳۰ روزه (۱ ماهه)",
                days = 30,
                feedPerBirdGrams = 110.0,
                supplementsFa = "دان رشد اردک + سبزیجات و کاهو خردشده تمیز در آب.",
                beginnerTipsFa = "دما ۲۰ تا ۲۳ درجه. پرهای اصلی رشد کرده‌اند و جوجه‌ها می‌توانند در استخر کم‌عمق شنا کنند.",
                indoorDensityBirdsPerM2 = 8.0,
                outdoorPastureM2PerBird = 1.0
            ),
            AgeMilestone(
                id = "day_60",
                labelFa = "۶۰ روزه (۲ ماهه)",
                days = 60,
                feedPerBirdGrams = 150.0,
                supplementsFa = "میان‌دان پلت یا مخلوط غلات سبوس‌دار + پودر صدف.",
                beginnerTipsFa = "دما ۱۵ تا ۲۰ درجه. در نژادهای گوشتی مانند پکنی آماده کشتار (حدود ۲.۸ تا ۳ کیلوگرم) است.",
                indoorDensityBirdsPerM2 = 5.0,
                outdoorPastureM2PerBird = 2.0
            ),
            AgeMilestone(
                id = "day_150",
                labelFa = "۱۵۰ روزه (تخم‌گذاری)",
                days = 150,
                feedPerBirdGrams = 175.0,
                supplementsFa = "دان تخم‌گذار ویژه با کلسیم ۳.۵٪ + صدف درشت و سبزیجات تازه.",
                beginnerTipsFa = "شروع دوره تخم‌گذاری؛ اردک‌ها صبح زود قبل از طلوع آفتاب تخم می‌گذارند. لانه تمیز در گوشه‌های کف سالن آماده کنید.",
                indoorDensityBirdsPerM2 = 4.0,
                outdoorPastureM2PerBird = 3.0
            )
        )

        GuideBird.GOOSE -> listOf(
            AgeMilestone(
                id = "day_1",
                labelFa = "۱ روزه (ورود)",
                days = 1,
                feedPerBirdGrams = 22.0,
                supplementsFa = "آب‌شکر ۵٪ ولرم + مولتی‌ویتامین و الکترولیت. دان سوپر استارتر خردشده نرم.",
                beginnerTipsFa = "دمای زیر لامپ ۳۰ تا ۳۲ درجه. جوجه غازها جثه درشتی دارند اما در ۳ روز اول به سرمای زمین و کوران باد حساسند.",
                indoorDensityBirdsPerM2 = 18.0,
                outdoorPastureM2PerBird = 0.5
            ),
            AgeMilestone(
                id = "day_10",
                labelFa = "۱۰ روزه",
                days = 10,
                feedPerBirdGrams = 65.0,
                supplementsFa = "دان رشد جوجه غاز + علف سبز، شبدر یا یونجه ریزشده تازه.",
                beginnerTipsFa = "دما ۲۷ تا ۲۹ درجه. غازها پرندگانی کاملاً علف‌خوار هستند؛ معرفی علوفه تازه از این سن ضروری است.",
                indoorDensityBirdsPerM2 = 10.0,
                outdoorPastureM2PerBird = 1.0
            ),
            AgeMilestone(
                id = "day_30",
                labelFa = "۳۰ روزه (۱ ماهه)",
                days = 30,
                feedPerBirdGrams = 160.0,
                supplementsFa = "علوفه و مرتع آزاد + غلات شکسته و سبوس شب‌ها.",
                beginnerTipsFa = "دما ۱۹ تا ۲۲ درجه. جوجه‌ها می‌توانند ساعات زیادی در چراگاه، باغ یا استخر پرسه بزنند.",
                indoorDensityBirdsPerM2 = 5.0,
                outdoorPastureM2PerBird = 3.0
            ),
            AgeMilestone(
                id = "day_60",
                labelFa = "۶۰ روزه (۲ ماهه)",
                days = 60,
                feedPerBirdGrams = 240.0,
                supplementsFa = "چرای آزاد روزانه + مکمل گندم و جو در عصر.",
                beginnerTipsFa = "غازها تقریباً خودکفا می‌شوند و بخش عمده خوراکشان را از علف‌های هرز باغ و مرتع تأمین می‌کنند.",
                indoorDensityBirdsPerM2 = 3.0,
                outdoorPastureM2PerBird = 5.0
            ),
            AgeMilestone(
                id = "day_120",
                labelFa = "۱۲۰ روزه (۴ ماهگی/بلوغ)",
                days = 120,
                feedPerBirdGrams = 300.0,
                supplementsFa = "پایان‌دان پرواری یا جیره نگهداری غلات به همراه صدف معدنی.",
                beginnerTipsFa = "وزن پرنده به ۴.۵ تا ۶ کیلوگرم می‌رسد. غاز در برابر سرمای زمستان و برف به شدت مقاوم است.",
                indoorDensityBirdsPerM2 = 2.0,
                outdoorPastureM2PerBird = 6.0
            )
        )

        else -> listOf(
            // DEFAULT: LAYER / NATIVE CHICKEN
            AgeMilestone(
                id = "day_1",
                labelFa = "۱ روزه (ورود)",
                days = 1,
                feedPerBirdGrams = 12.0,
                supplementsFa = "آب ولرم با ۵٪ شکر در ۲ ساعت اول + مولتی‌ویتامین و الکترولیت. دان سوپر استارتر نرم پودری.",
                beginnerTipsFa = "دمای زیر لامپ ۳۳ تا ۳۵ درجه باشد. کف سالن حتماً ۵ سانت پوشال چوب درشت نجاری بریزید (روزنامه صاف پاها را فلج می‌کند).",
                indoorDensityBirdsPerM2 = 35.0,
                outdoorPastureM2PerBird = 0.1
            ),
            AgeMilestone(
                id = "day_10",
                labelFa = "۱۰ روزه",
                days = 10,
                feedPerBirdGrams = 24.0,
                supplementsFa = "ویتامین B-Complex در آب (هفته‌ای یک‌بار) + قطره چشمی نیوکاسل-برونشیت.",
                beginnerTipsFa = "دما به ۳۰ تا ۳۲ درجه کاهش یابد. از هفته دوم چراغ را شب‌ها حداقل ۴ الی ۶ ساعت خاموش کنید تا جوجه‌ها استراحت کنند.",
                indoorDensityBirdsPerM2 = 25.0,
                outdoorPastureM2PerBird = 0.2
            ),
            AgeMilestone(
                id = "day_30",
                labelFa = "۳۰ روزه (۱ ماهه)",
                days = 30,
                feedPerBirdGrams = 45.0,
                supplementsFa = "دان پیش‌دان کرامبل + پروبیوتیک روده در آب برای هضم کامل.",
                beginnerTipsFa = "دما ۲۴ تا ۲۶ درجه. جوجه‌ها پرهای اصلی را درآورده‌اند و می‌توانند در روزهای آفتابی چند ساعت وارد حیاط شوند.",
                indoorDensityBirdsPerM2 = 16.0,
                outdoorPastureM2PerBird = 0.5
            ),
            AgeMilestone(
                id = "day_60",
                labelFa = "۶۰ روزه (۲ ماهه)",
                days = 60,
                feedPerBirdGrams = 70.0,
                supplementsFa = "میان‌دان پلت یا مخلوط دان بومی + سبزیجات و یونجه خردشده تازه.",
                beginnerTipsFa = "دما ۲۰ تا ۲۲ درجه (دمای معمولی اتاق). نصب نشیمنگاه چوبی (چوب خواب) در ارتفاع ۵۰ سانتی‌متری دیوار سالن.",
                indoorDensityBirdsPerM2 = 10.0,
                outdoorPastureM2PerBird = 1.0
            ),
            AgeMilestone(
                id = "day_90",
                labelFa = "۹۰ روزه (۳ ماهه)",
                days = 90,
                feedPerBirdGrams = 85.0,
                supplementsFa = "دان رشد نیمچه + پودر صدف درشت در ظرف جداگانه جهت تقویت سنگدان.",
                beginnerTipsFa = "نیمچه‌ها بسیار فعال هستند. از چاق شدن بیش از حد مرغ‌ها با نان خشک روغنی جلوگیری کنید تا در آینده تخم‌گذاری مختل نشود.",
                indoorDensityBirdsPerM2 = 8.0,
                outdoorPastureM2PerBird = 1.5
            ),
            AgeMilestone(
                id = "day_120",
                labelFa = "۱۲۰ روزه (۴ ماهه)",
                days = 120,
                feedPerBirdGrams = 95.0,
                supplementsFa = "پولت تخم‌گذار + ویتامین AD3E برای تحریک تکامل تخمدان‌ها.",
                beginnerTipsFa = "نصب لانه‌های تخم‌گذاری چوبی یا پلاستیکی (Nest box) در گوشه‌های تاریک سالن با پوشال تمیز (۱ لانه برای هر ۵ مرغ).",
                indoorDensityBirdsPerM2 = 7.0,
                outdoorPastureM2PerBird = 2.0
            ),
            AgeMilestone(
                id = "day_150",
                labelFa = "۱۵۰ روزه (۵ ماهه)",
                days = 150,
                feedPerBirdGrams = 105.0,
                supplementsFa = "تغییر جیره به دان تخم‌گذار ویژه با کلسیم ۳.۸٪ و پودر صدف.",
                beginnerTipsFa = "تاج مرغ‌ها قرمز پررنگ می‌شود و اولین تخم‌های ریز (تخم پولت) گذاشته می‌شوند. برنامه نوری را به ۱۶ ساعت در روز برسانید.",
                indoorDensityBirdsPerM2 = 6.0,
                outdoorPastureM2PerBird = 2.5
            ),
            AgeMilestone(
                id = "day_180",
                labelFa = "شروع تولید کامل (۶ ماه به بعد)",
                days = 180,
                feedPerBirdGrams = 115.0,
                supplementsFa = "دان تخم‌گذار کامل + پودر صدف آزاد + مولتی‌ویتامین ماهی یک‌بار.",
                beginnerTipsFa = "پیک تخم‌گذاری! هرگز نور شب را قطع نکنید و تخم‌ها را روزی ۳ بار جمع‌آوری کنید تا کثیف یا شکسته نشوند.",
                indoorDensityBirdsPerM2 = 5.0,
                outdoorPastureM2PerBird = 3.0
            )
        )
    }

    val species = when (bird) {
        GuideBird.LAYER_HEN -> BirdSpecies.CHICKEN
        GuideBird.BROILER -> BirdSpecies.CHICKEN
        GuideBird.TURKEY -> BirdSpecies.TURKEY
        GuideBird.QUAIL -> BirdSpecies.QUAIL
        GuideBird.DUCK -> BirdSpecies.DUCK
        GuideBird.GOOSE -> BirdSpecies.GOOSE
    }
    val breedName = when (bird) {
        GuideBird.LAYER_HEN -> "مرغ تخم‌گذار بومی"
        GuideBird.BROILER -> "راس ۳۰۸"
        GuideBird.TURKEY -> "بوقلمون گوشتی"
        GuideBird.QUAIL -> "بلدرچین ژاپنی"
        GuideBird.DUCK -> "اردک بومی"
        GuideBird.GOOSE -> "غاز خاکستری"
    }

    return rawMilestones.map { m ->
        val tempSpec = PoultryTemperatureEngine.getIdealTemperature(
            species = species,
            breedName = breedName,
            ageDays = m.days
        )
        m.copy(
            idealTempRangeFa = "${toPersianDigits(tempSpec.minComfortCelsius.toInt())} تا ${toPersianDigits(tempSpec.maxComfortCelsius.toInt())}°C",
            tolerableTempRangeFa = "${toPersianDigits(tempSpec.minTolerableCelsius.toInt())} تا ${toPersianDigits(tempSpec.maxTolerableCelsius.toInt())}°C",
            tempAdviceFa = tempSpec.adviceFa
        )
    }
}

/**
 * ========================================================
 * بخش بانک جامع سوال و جواب (مرتب‌شده دقیق بر اساس سن پرنده)
 * ========================================================
 */
@Composable
private fun GuideQnABankSection(
    selectedBirdFilter: String,
    onSelectBirdFilter: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    expandedQnAIds: Set<String>,
    onToggleExpand: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val allQnAs = remember { BreedingConsultationCatalog.getAllQnAs() }

    val filteredQnAs = remember(selectedBirdFilter, searchQuery) {
        allQnAs.filter { item ->
            val matchBird = when (selectedBirdFilter) {
                "ALL" -> true
                else -> item.bird.id == selectedBirdFilter
            }

            val matchSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                item.question.lowercase().contains(q) ||
                        item.answer.lowercase().contains(q) ||
                        item.ageStageFa.lowercase().contains(q) ||
                        item.categoryFa.lowercase().contains(q) ||
                        item.bird.titleFa.lowercase().contains(q)
            }

            matchBird && matchSearch
        }.sortedBy { it.ageOrder }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "جستجو در بانک بیش از ۲۰۰ سوال و جواب...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedBirdFilter == "ALL",
                            onClick = { onSelectBirdFilter("ALL") },
                            label = {
                                Text(
                                    text = "همه پرندگان (${allQnAs.size.toPersianDigits()})",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Widgets,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    items(GuideBird.entries) { bird ->
                        val isSelected = selectedBirdFilter == bird.id
                        val count = remember(bird) { allQnAs.count { it.bird == bird } }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectBirdFilter(bird.id) },
                            label = {
                                Text(
                                    text = "${bird.titleFa} (${count.toPersianDigits()})",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = {
                                Image(
                                    painter = painterResource(id = bird.imageRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                )
                            }
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AutoResizeText(
                            text = "ترتیب زمانی سن پرنده",
                            targetFontSize = 12.sp,
                            minFontSize = 9.sp,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${filteredQnAs.size.toPersianDigits()} پرسش و پاسخ",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        if (filteredQnAs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "موردی با این عبارت یافت نشد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(filteredQnAs, key = { it.id }) { qna ->
                val isExpanded = expandedQnAIds.contains(qna.id)
                GuideQnACardItem(
                    qna = qna,
                    isExpanded = isExpanded,
                    onToggle = { onToggleExpand(qna.id) },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString("${qna.question}\n\nپاسخ کارشناسی:\n${qna.answer}"))
                        Toast.makeText(context, "پرسش و پاسخ کپی شد", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

/**
 * کارت اختصاصی نمایش هر پرسش و پاسخ
 */
@Composable
private fun GuideQnACardItem(
    qna: AgeOrderedQnA,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onCopy: () -> Unit
) {
    val ageBadgeColor = when {
        qna.ageOrder < 0 -> Color(0xFF673AB7) // جوجه‌کشی
        qna.ageOrder <= 7 -> Color(0xFFE65100) // روزهای اول (بسیار حساس)
        qna.ageOrder <= 30 -> Color(0xFF1565C0) // آغازین و رشد اولیه
        qna.ageOrder <= 90 -> Color(0xFF2E7D32) // رشد و نیمچگی
        else -> Color(0xFFC2185B) // بلوغ، تخم‌گذاری یا کشتار
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isExpanded) 1.dp else 0.5.dp,
            color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .animateContentSize()
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ageBadgeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ageBadgeColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = qna.ageStageFa,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ageBadgeColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = qna.bird.titleFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = qna.categoryFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 1.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "پرسش",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = qna.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "بستن پاسخ" else "مشاهده پاسخ",
                    tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(22.dp)
                        .padding(top = 2.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FarmGreenPrimary.copy(alpha = 0.18f),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(top = 1.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "پاسخ",
                                tint = FarmGreenPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = qna.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        lineHeight = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCopy,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "کپی پاسخ",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * ========================================================
 * بخش ارتباط با کارشناس پرورش (ساده، صریح و بدون اضافات)
 * ========================================================
 */
@Composable
private fun GuideContactExpertSection(
    ticketBird: GuideBird,
    onSelectBird: (GuideBird) -> Unit,
    ageDaysText: String,
    onAgeChange: (String) -> Unit,
    flockCountText: String,
    onFlockCountChange: (String) -> Unit,
    questionText: String,
    onQuestionChange: (String) -> Unit,
    userTickets: List<ConsultationTicket>,
    onSubmitTicket: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ContactSupport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ارتباط با کارشناس پرورش",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مشخصات پرنده و سوال خود را ارسال نمایید؛ پاسخ تشخیصی اولیه بلافاصله صادر شده و سوال در صف کارشناسان قرار می‌گیرد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ثبت سوال جدید",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "نوع پرنده:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(GuideBird.entries) { bird ->
                            val isSelected = ticketBird == bird
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectBird(bird) },
                                label = { Text(bird.titleFa, style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = {
                                    Image(
                                        painter = painterResource(id = bird.imageRes),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ageDaysText,
                            onValueChange = { if (it.length <= 4) onAgeChange(it.filter { char -> char.isDigit() }) },
                            label = { Text("سن پرنده (روز)") },
                            placeholder = { Text("مثلاً ۴۵") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = flockCountText,
                            onValueChange = { if (it.length <= 6) onFlockCountChange(it.filter { char -> char.isDigit() }) },
                            label = { Text("تعداد گله (قطعه)") },
                            placeholder = { Text("مثلاً ۵۰۰") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = questionText,
                        onValueChange = onQuestionChange,
                        label = { Text("شرح کامل سوال، علائم یا مشکل گله") },
                        placeholder = { Text("شرح دهید چه مشکلی در گله پیش آمده است (تغذیه، بیماری، تلفات، افت تولید، دمای سالن و...)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSubmitTicket,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ثبت و ارسال به کارشناس پرورش",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "ارتباط تلفنی و پیامکی مستقیم",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "در شرایط اضطراری یا نیاز به مشاوره فوری می‌توانید مستقیماً با خط پشتیبانی تماس بگیرید یا پیام ارسال نمایید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:02191000000")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "امکان برقراری تماس یافت نشد", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تماس تلفنی", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:09120000000")
                                    putExtra("sms_body", "سلام، در خصوص پرورش ${ticketBird.titleFa} نیاز به راهنمایی دارم.")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "امکان ارسال پیامک یافت نشد", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ارسال پیامک", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = "تاریخچه سوالات ارسالی شما (${userTickets.size.toPersianDigits()})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(userTickets, key = { it.id }) { ticket ->
            GuideUserTicketCardItem(ticket = ticket)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

/**
 * کارت نمایش تیکت ارسال‌شده کاربر
 */
@Composable
private fun GuideUserTicketCardItem(ticket: ConsultationTicket) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = ticket.bird.titleFa,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (ticket.birdAgeDays != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سن: ${ticket.birdAgeDays.toPersianDigits()} روز",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (ticket.statusText.contains("پاسخ")) FarmGreenPrimary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = ticket.statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ticket.statusText.contains("پاسخ")) FarmGreenPrimary
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AutoResizeText(
                text = ticket.questionText,
                targetFontSize = 14.sp,
                minFontSize = 11.sp,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) 100 else 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.timestampFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = if (expanded) "بستن توضیحات" else "مشاهده پاسخ کارشناسی",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "راهنمایی تشخیصی و توصیه کارشناسی:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ticket.instantAdvice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
