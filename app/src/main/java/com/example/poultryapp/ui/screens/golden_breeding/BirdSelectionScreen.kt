package com.example.poultryapp.ui.screens.golden_breeding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.poultryapp.data.model.GameSeasonTier
import com.example.poultryapp.data.model.GoldenBirdType
import com.example.poultryapp.data.repository.GoldenBreedingRepository
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.ui.components.AutoResizeText
import com.example.poultryapp.ui.screens.golden_breeding.components.BirdArtTheme
import com.example.poultryapp.ui.screens.golden_breeding.components.GoldenHonorsPlaquesDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * صفحه انتخاب گونه‌های پرندگان در آکادمی پرورش طلایی
 * با قابلیت سوایپ افقی پیوسته و روان بین فصل ۱ (نوآموز) و فصل ۲ (بورد فوق‌حرفه‌ای 👑)،
 * بازگشت دکمه نقشه با تم اختصاصی هر پرنده، قفل شرطی فصل حرفه‌ای بر مبنای ۳ ستاره کردن تمام مراحل فصل ۱
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BirdSelectionScreen(
    selectedSeason: GameSeasonTier = GameSeasonTier.NOVICE,
    onSeasonChanged: (GameSeasonTier) -> Unit = {},
    onSelectBird: (GoldenBirdType, GameSeasonTier) -> Unit,
    onOpenDrawer: () -> Unit,
    onBackToFarm: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val repository = remember { GoldenBreedingRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showMedalsDialog by remember { mutableStateOf(false) }
    var selectedBirdForMedals by remember { mutableStateOf<GoldenBirdType?>(null) }
    var lockedBirdForDialog by remember { mutableStateOf<GoldenBirdType?>(null) }

    // راه‌اندازی پیجر افقی با سوایپ پیوسته
    val initialPage = if (selectedSeason == GameSeasonTier.PROFESSIONAL) 1 else 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })

    // محاسبه پیشرفت پیوسته برای انیمیشن‌های روان و بدون گلیچ
    val swipeProgress = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
    val currentSeason = if (pagerState.currentPage == 1) GameSeasonTier.PROFESSIONAL else GameSeasonTier.NOVICE

    var previousPage by remember { mutableIntStateOf(pagerState.currentPage) }

    LaunchedEffect(pagerState.currentPage) {
        onSeasonChanged(currentSeason)
        if (pagerState.currentPage != previousPage) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            previousPage = pagerState.currentPage
        }
    }

    // رنگ‌های زمینه و هدر با درآمیختگی پیوسته (lerp) جهت دگرگونی کامل تم فصل ۱ (سبز ارگانیک) به فصل ۲ (آبسیدین و طلایی)
    val bgTop = lerp(Color(0xFF042618), Color(0xFF030712), swipeProgress)
    val bgMid = lerp(Color(0xFF063E28), Color(0xFF0B0F19), swipeProgress)
    val bgBottom = lerp(Color(0xFF021F14), Color(0xFF1E1B4B), swipeProgress)
    val headerBg = lerp(Color(0xFF032819), Color(0xFF030712), swipeProgress)
    val titleColor = lerp(Color(0xFF34D399), Color(0xFFFDE047), swipeProgress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgMid, bgBottom)))
    ) {
        // ۱. لایه ذرات پرتال سینماتیک در حال سوایپ بین دو فصل
        PortalBurstParticleLayer(swipeProgress = swipeProgress)

        Column(modifier = Modifier.fillMaxSize()) {
            // هدر بالای صفحه با دکمه دسترسی مستقیم به تالار افتخارات و رنگ‌آمیزی کامل تا بالای صفحه
            Surface(
                color = headerBg,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBackToFarm != null) {
                            IconButton(onClick = onBackToFarm) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بازگشت به فارم",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = onOpenDrawer) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "منو", tint = Color.White)
                        }
                    }

                    // ستون میانی عنوان آکادمی
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (swipeProgress > 0.5f) "👑" else "🌱",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AutoResizeText(
                                text = if (swipeProgress > 0.5f) "بورد فوق‌تخصصی پرورش" else "آکادمی پرورش طلایی",
                                targetFontSize = 15.sp,
                                minFontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = titleColor,
                                maxLines = 1
                            )
                        }
                        AutoResizeText(
                            text = if (swipeProgress > 0.5f) "۳۰۰ مرحله بورد دامپزشکی و مهندسی مگافارم" else "گونه مورد نظر را جهت ورود به دوره انتخاب کنید",
                            targetFontSize = 10.5.sp,
                            minFontSize = 8.5.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }

                    // دکمه تالار مدال‌های افتخار
                    val totalUnlockedMedals = remember(showMedalsDialog) { repository.getTotalUnlockedMedalsCount() }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (swipeProgress > 0.5f) Color(0xFF78350F) else Color(0xFF065F46),
                        border = BorderStroke(1.2.dp, if (swipeProgress > 0.5f) Color(0xFFFBBF24) else Color(0xFF34D399)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .wrapContentWidth()
                            .clickable {
                                selectedBirdForMedals = GoldenBirdType.LAYER_HEN
                                showMedalsDialog = true
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "🏅", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            AutoResizeText(
                                text = "مدال‌ها (${totalUnlockedMedals.toPersianDigits()})",
                                targetFontSize = 11.5.sp,
                                minFontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFEF08A),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // انتخابگر تب فصل‌ها با استایل کپسولی لغزان و هاله نوری
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF030712).copy(alpha = 0.88f))
                    .border(
                        width = 1.2.dp,
                        color = lerp(Color(0xFF10B981), Color(0xFFFBBF24), swipeProgress).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // تب ۱: مقدماتی (ارگانیک و سرسبز)
                val noviceWeight = (1f - swipeProgress).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF064E3B).copy(alpha = 0.25f + 0.75f * noviceWeight),
                                    Color(0xFF059669).copy(alpha = 0.25f + 0.75f * noviceWeight)
                                )
                            )
                        )
                        .border(
                            width = if (noviceWeight > 0.5f) 1.5.dp else 0.5.dp,
                            color = if (noviceWeight > 0.5f) Color(0xFF34D399) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌱", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        AutoResizeText(
                            text = "فصل ۱: مقدماتی",
                            targetFontSize = 12.5.sp,
                            minFontSize = 9.sp,
                            fontWeight = if (noviceWeight > 0.5f) FontWeight.Black else FontWeight.Bold,
                            color = lerp(Color(0xFF94A3B8), Color.White, noviceWeight),
                            maxLines = 1
                        )
                    }
                }

                // تب ۲: بورد استادی 👑 (اشرافی و طلای ۲۴ عیار)
                val proWeight = swipeProgress.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF78350F).copy(alpha = 0.25f + 0.75f * proWeight),
                                    Color(0xFFB45309).copy(alpha = 0.25f + 0.75f * proWeight)
                                )
                            )
                        )
                        .border(
                            width = if (proWeight > 0.5f) 1.5.dp else 0.5.dp,
                            color = if (proWeight > 0.5f) Color(0xFFFBBF24) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👑", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        AutoResizeText(
                            text = "فصل ۲: بورد استادی",
                            targetFontSize = 12.5.sp,
                            minFontSize = 9.sp,
                            fontWeight = if (proWeight > 0.5f) FontWeight.Black else FontWeight.Bold,
                            color = lerp(Color(0xFF94A3B8), Color(0xFFFEF08A), proWeight),
                            maxLines = 1
                        )
                    }
                }
            }

            // راهنمای سوایپ پیوسته با رنگ تطبیق‌پذیر
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (swipeProgress > 0.5f) "👈 برای بازگشت به فصل مقدماتی سوایپ کنید 👉" else "👈 برای ورود به بورد استادی به چپ سوایپ کنید 👉",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // بدنه اصلی پیجر افقی با ترنزیشن سه‌بعدی Tilt & Perspective
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageSeason = if (page == 1) GameSeasonTier.PROFESSIONAL else GameSeasonTier.NOVICE
                val isProSeason = pageSeason == GameSeasonTier.PROFESSIONAL

                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // چرخش سه‌بعدی حول محور Y متناسب با جابجایی دست
                            rotationY = -18f * pageOffset
                            // افکت عمق فضایی
                            val scale = 1f - (absOffset * 0.08f)
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - (absOffset * 0.40f)
                            cameraDistance = 14f * density
                        },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // کارت بنر توضیحات فصل جاری با طراحی کاملاً متمایز
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isProSeason) Color(0xFF0F172A).copy(alpha = 0.95f) else Color(0xFF063A26).copy(alpha = 0.95f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.8.dp,
                                    if (isProSeason) Color(0xFFFBBF24) else Color(0xFF10B981).copy(alpha = 0.8f),
                                    RoundedCornerShape(20.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    text = if (isProSeason) "👑" else "🌱",
                                    fontSize = 34.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isProSeason)
                                            "بورد فوق‌حرفه‌ای و استادی دامپزشکی طیور"
                                        else
                                            "مسیر جامع آموزش و پرورش طبیعی طیور",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isProSeason) Color(0xFFFEF08A) else Color(0xFFECFDF5)
                                    )
                                    Text(
                                        text = if (isProSeason)
                                            "۳۰۰ مرحله فوق‌تخصصی (۲۵ مرحله برای هر پرنده) در ۵ ماژول: پاتولوژی، تهویه، تغذیه، امنیت زیستی و مگافارم. (نیازمند ۳ ستاره کردن تمام مراحل فصل مقدماتی)"
                                        else
                                            "مراحل مهارتی و کاربردی پرورش ۱۲ گونه طیور همراه با چالش‌های روزمره فارم، نقشه‌های هنری و بازگشایی لوح‌های زرین.",
                                        fontSize = 11.5.sp,
                                        color = if (isProSeason) Color(0xFFCBD5E1) else Color(0xFFA7F3D0),
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // لیست تمام ۱۲ گونه پرنده در فصل مربوطه
                    items(GoldenBirdType.values()) { bird ->
                        val isProUnlocked = remember(bird) { repository.isProSeasonUnlocked(bird) }
                        val novice3StarCount = remember(bird) { repository.getNoviceThreeStarCount(bird) }
                        val unlockedMedalsCount = remember(bird, isProSeason) {
                            if (isProSeason) repository.getUnlockedMedalsCountForSeason(bird, GameSeasonTier.PROFESSIONAL)
                            else repository.getUnlockedMedalsCountForSeason(bird, GameSeasonTier.NOVICE)
                        }

                        val summary = remember(bird, isProSeason) {
                            if (isProSeason) repository.getProBirdProgressSummary(bird)
                            else repository.getBirdProgressSummary(bird)
                        }
                        val theme = remember(bird, isProSeason) {
                            if (isProSeason) BirdArtTheme.getProThemeForBird(bird)
                            else BirdArtTheme.getThemeForBird(bird)
                        }

                        val isCardLocked = isProSeason && !isProUnlocked

                        BirdSelectionCard(
                            bird = bird,
                            isProSeason = isProSeason,
                            isLocked = isCardLocked,
                            novice3StarCount = novice3StarCount,
                            unlockedMedalsCount = unlockedMedalsCount,
                            summary = summary,
                            theme = theme,
                            onMedalsClick = {
                                selectedBirdForMedals = bird
                                showMedalsDialog = true
                            },
                            onCardClick = {
                                if (isCardLocked) {
                                    lockedBirdForDialog = bird
                                } else {
                                    onSelectBird(bird, pageSeason)
                                }
                            },
                            onActionClick = {
                                if (isCardLocked) {
                                    lockedBirdForDialog = bird
                                } else {
                                    onSelectBird(bird, pageSeason)
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // دیالوگ اطلاع‌رسانی قفل بودن فصل حرفه‌ای
        lockedBirdForDialog?.let { lockedBird ->
            val novice3StarCount = remember(lockedBird) { repository.getNoviceThreeStarCount(lockedBird) }
            val totalNoviceStages = lockedBird.totalStages

            AlertDialog(
                onDismissRequest = { lockedBirdForDialog = null },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Text(text = "👑", fontSize = 38.sp)
                },
                title = {
                    Text(
                        text = "بورد فوق‌تخصصی ${lockedBird.titleFa} قفل است",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFEF08A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "برای ورود به بورد استادی و کسب لوح زرین، باید تمام مراحل فصل مقدماتی این پرنده را با ۳ ستاره کامل فتح کنید.",
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "⭐ وضعیت فعلی: ", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                Text(
                                    text = "${novice3StarCount.toPersianDigits()} از ${totalNoviceStages.toPersianDigits()} مرحله ۳ ستاره شده",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            lockedBirdForDialog = null
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AutoResizeText(
                            text = "ورود به فصل مقدماتی برای ۳ ستاره کردن ➔",
                            targetFontSize = 12.sp,
                            minFontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { lockedBirdForDialog = null }) {
                        Text("متوجه شدم", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            )
        }

        // دیالوگ تالار مدال‌های افتخار
        if (showMedalsDialog) {
            com.example.poultryapp.ui.screens.golden_breeding.components.GoldenMedalsDialog(
                birdType = selectedBirdForMedals ?: GoldenBirdType.LAYER_HEN,
                initialSeason = currentSeason,
                repository = repository,
                onDismiss = {
                    showMedalsDialog = false
                    selectedBirdForMedals = null
                }
            )
        }
    }
}

/**
 * سازگاری عقب‌رو برای صفحاتی که متد بدون فصل را فراخوانی می‌کنند
 */
@Composable
fun BirdSelectionScreen(
    onSelectBird: (GoldenBirdType) -> Unit,
    onOpenDrawer: () -> Unit
) {
    BirdSelectionScreen(
        selectedSeason = GameSeasonTier.NOVICE,
        onSeasonChanged = {},
        onSelectBird = { bird, _ -> onSelectBird(bird) },
        onOpenDrawer = onOpenDrawer
    )
}

/**
 * لایه ذرات پرتال سینماتیک در حال سوایپ بین دو فصل
 * ذرات طلایی شاهانه و جوانه‌های سبز ارگانیک هنگام گذر در فضا پخش می‌شوند
 */
@Composable
private fun PortalBurstParticleLayer(swipeProgress: Float) {
    val burstIntensity = (1f - (abs(swipeProgress - 0.5f) * 2f)).coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "portalParticles")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleRotation"
    )

    if (burstIntensity > 0.04f) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1.5f)
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = size.minDimension * 0.55f

            val particleCount = 32
            for (i in 0 until particleCount) {
                val seed = i * 137.5f
                val angleDeg = (seed + animPhase * (if (i % 2 == 0) 1.2f else -0.8f)) % 360f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val distanceRatio = ((i.toFloat() / particleCount.toFloat()) + (animPhase / 360f)) % 1f
                val distance = distanceRatio * maxRadius

                val px = centerX + (cos(angleRad) * distance).toFloat()
                val py = centerY + (sin(angleRad) * distance).toFloat()

                val isGold = (i % 2 == 0)
                val baseColor = if (isGold) Color(0xFFFBBF24) else Color(0xFF34D399)
                val particleColor = baseColor.copy(alpha = (burstIntensity * 0.85f).coerceIn(0f, 1f))

                val pRadius = (2.2f + (i % 5) * 1.4f) * (0.7f + burstIntensity * 0.5f)
                drawCircle(
                    color = particleColor,
                    radius = pRadius,
                    center = Offset(px, py)
                )
            }
        }
    }
}

/**
 * کارت انتخاب گونه پرنده همراه با نوار پیشرفت و دکمه اختصاصی ورود به نقشه
 * طراحی کاملاً تفکیک شده:
 * - فصل ۱: دشت سبز ارگانیک و زمردین مزرعه (Emerald Glass)
 * - فصل ۲: گلس‌مورفیسم شاهانه شب آبسیدین با طلای ۲۴ عیار (Obsidian & 24K Gold VIP)
 */
@Composable
private fun BirdSelectionCard(
    bird: GoldenBirdType,
    isProSeason: Boolean,
    isLocked: Boolean,
    novice3StarCount: Int,
    unlockedMedalsCount: Int = 0,
    summary: com.example.poultryapp.data.model.BirdProgressSummary,
    theme: BirdArtTheme,
    onMedalsClick: () -> Unit = {},
    onCardClick: () -> Unit,
    onActionClick: () -> Unit
) {
    val totalStagesCount = if (isProSeason) 25 else bird.totalStages
    val targetProgress = (summary.completedStagesCount.toFloat() / totalStagesCount.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "birdProgressBar"
    )
    val percentage = (targetProgress * 100f).roundToInt()

    // استایل‌های تفکیک‌شده دو فصل با گرادیان دونیمه (نصف رنگ فصل و نصف رنگ تم پرنده)
    val seasonBaseColor = if (isProSeason) Color(0xFF080D1A) else Color(0xFF042B1A)
    // ترکیب ملایم با رنگ اختصاصی پرنده برای نیمه دیگر جهت ایجاد کنتراست عمیق و حفظ خوانایی کامل متن‌ها
    val birdThemeShade = lerp(
        if (isProSeason) Color(0xFF0F172A) else Color(0xFF062016),
        theme.primaryAccent,
        0.36f
    )

    val cardBackgroundBrush = when {
        isLocked -> Brush.horizontalGradient(
            listOf(Color(0xFF0F172A).copy(alpha = 0.94f), Color(0xFF1E293B).copy(alpha = 0.94f))
        )
        else -> Brush.horizontalGradient(
            listOf(
                birdThemeShade.copy(alpha = 0.95f),
                lerp(birdThemeShade, seasonBaseColor, 0.5f).copy(alpha = 0.95f),
                seasonBaseColor.copy(alpha = 0.96f)
            )
        )
    }

    val cardBorder = when {
        isLocked -> BorderStroke(1.5.dp, Color(0xFF334155))
        isProSeason -> BorderStroke(
            2.dp,
            Brush.horizontalGradient(
                listOf(
                    theme.primaryAccent.copy(alpha = 0.85f),
                    Color(0xFFFBBF24),
                    Color(0xFFFEF08A)
                )
            )
        )
        else -> BorderStroke(
            1.6.dp,
            Brush.horizontalGradient(
                listOf(
                    theme.primaryAccent.copy(alpha = 0.85f),
                    Color(0xFF10B981).copy(alpha = 0.75f),
                    Color(0xFF34D399).copy(alpha = 0.85f)
                )
            )
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isProSeason && !isLocked) 10.dp else 6.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = if (isLocked) Color.Black else if (isProSeason) Color(0xFFF59E0B) else theme.primaryAccent
            )
            .background(brush = cardBackgroundBrush, shape = RoundedCornerShape(22.dp))
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // تگ وضعیت محیطی یا قفل بورد (با اولویت کمکی و خلاصه شدن منعطف در صورت لزوم)
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isLocked -> Color(0xFF334155).copy(alpha = 0.7f)
                                isProSeason -> Color(0xFF78350F).copy(alpha = 0.75f)
                                else -> Color(0xFF064E3B).copy(alpha = 0.75f)
                            }
                        )
                        .border(
                            width = 0.8.dp,
                            color = when {
                                isLocked -> Color(0xFF64748B)
                                isProSeason -> Color(0xFFFBBF24)
                                else -> Color(0xFF34D399)
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    AutoResizeText(
                        text = when {
                            isLocked -> "🔒 قفل بورد استادی"
                            isProSeason -> "👑 بورد ۲۵ مرحله‌ای استادی"
                            else -> "🌱 ${theme.environmentTagFa}"
                        },
                        targetFontSize = 11.sp,
                        minFontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isLocked -> Color(0xFFCBD5E1)
                            isProSeason -> Color(0xFFFEF08A)
                            else -> Color(0xFFA7F3D0)
                        },
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // مجموع ستاره‌های طلایی (مراحل اصلی + مراحل فرعی) با اولویت قطعی تخصیص فضا (هرگز فشرده یا پنهان نمی‌شود)
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = Color(0xFF78350F).copy(alpha = 0.70f),
                    border = BorderStroke(0.9.dp, Color(0xFFFBBF24).copy(alpha = 0.65f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "مجموع ستاره‌ها",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(13.5.dp)
                        )
                        Spacer(modifier = Modifier.width(3.5.dp))
                        Text(
                            text = "\u200E${summary.totalStars.toPersianDigits()} / ${summary.maxPossibleStars.toPersianDigits()}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFFBEB),
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // تصویر پرنده با فریم اختصاصی فصل
                Box(modifier = Modifier.size(68.dp)) {
                    Image(
                        painter = painterResource(id = bird.imageRes),
                        contentDescription = bird.titleFa,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = if (isProSeason && !isLocked) 2.dp else 1.5.dp,
                                color = when {
                                    isLocked -> Color(0xFF64748B)
                                    isProSeason -> Color(0xFFFBBF24)
                                    else -> theme.primaryAccent
                                },
                                shape = RoundedCornerShape(18.dp)
                            )
                    )
                    if (isProSeason && !isLocked) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF78350F),
                            border = BorderStroke(1.dp, Color(0xFFFEF08A)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "👑", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bird.titleFa,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isProSeason && !isLocked) Color(0xFFFEF08A) else Color.White
                        )
                        if (isProSeason) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "👑", fontSize = 14.sp)
                        }
                    }
                    Text(
                        text = if (isProSeason) "چالش‌های بورد فوق‌تخصصی و لوح زرین" else bird.subtitleFa,
                        fontSize = 12.sp,
                        color = if (isProSeason) Color(0xFFE2E8F0) else Color(0xFFCBD5E1),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Text(
                        text = if (isLocked) {
                            "۳ ستاره: ${novice3StarCount.toPersianDigits()} از ${bird.totalStages.toPersianDigits()} مرحله مقدماتی".toPersianDigits()
                        } else if (isProSeason) {
                            "۲۵ مرحله مستقل از سن در ۵ ماژول تخصصی (۷۵ چالش)".toPersianDigits()
                        } else {
                            bird.cycleDescriptionFa.toPersianDigits()
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isLocked -> Color(0xFFFBBF24)
                            isProSeason -> Color(0xFFFDE68A)
                            else -> theme.secondaryAccent
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // نوار پیشرفت بصری دو لایه با درصد تکمیل
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summary.masteryTitleFa,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProSeason) Color(0xFFFEF08A) else Color(0xFFA7F3D0)
                    )
                    Text(
                        text = "${summary.completedStagesCount.toPersianDigits()} از ${totalStagesCount.toPersianDigits()} مرحله (${percentage.toPersianDigits()}٪)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isLocked -> Color(0xFF94A3B8)
                            isProSeason -> Color(0xFFFBBF24)
                            else -> theme.primaryAccent
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (isProSeason) Color(0xFF1E1B4B) else Color(0xFF064E3B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    if (isProSeason) listOf(Color(0xFFB45309), Color(0xFFF59E0B), Color(0xFFFEF08A))
                                    else listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFFA7F3D0))
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ردیف اکشن‌های دوگانه: دکمه مدال‌ها + دکمه ورود به نقشه
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // دکمه بازگشایی تالار مدال‌های این پرنده
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.85f),
                    border = BorderStroke(1.2.dp, Color(0xFFFBBF24).copy(alpha = 0.65f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .height(46.dp)
                        .clickable { onMedalsClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(text = "🏅", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        AutoResizeText(
                            text = "مدال‌ها (${unlockedMedalsCount.toPersianDigits()}/۳)",
                            targetFontSize = 11.sp,
                            minFontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFEF08A),
                            maxLines = 1
                        )
                    }
                }

                // دکمه اختصاصی و برجسته رفتن به نقشه با گرادیان تم هر پرنده
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .shadow(
                            elevation = if (isLocked) 2.dp else 8.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = when {
                                isLocked -> Color.Black
                                isProSeason -> Color(0xFFF59E0B)
                                else -> theme.primaryAccent
                            }
                        )
                        .background(
                            brush = when {
                                isLocked -> Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                                isProSeason -> Brush.horizontalGradient(
                                    listOf(
                                        theme.primaryAccent,
                                        lerp(theme.primaryAccent, Color(0xFFB45309), 0.5f),
                                        Color(0xFFB45309)
                                    )
                                )
                                else -> Brush.horizontalGradient(
                                    listOf(
                                        theme.primaryAccent,
                                        lerp(theme.primaryAccent, Color(0xFF059669), 0.5f),
                                        Color(0xFF059669)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            width = if (isLocked) 1.dp else 1.5.dp,
                            brush = when {
                                isLocked -> Brush.horizontalGradient(listOf(Color(0xFF475569), Color(0xFF475569)))
                                isProSeason -> Brush.horizontalGradient(listOf(theme.primaryAccent, Color(0xFFFEF08A)))
                                else -> Brush.horizontalGradient(listOf(theme.primaryAccent, Color(0xFF6EE7B7)))
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "قفل",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            AutoResizeText(
                                text = "قفل: نیازمند ۳ ستاره فصل ۱ (${novice3StarCount.toPersianDigits()}/${bird.totalStages.toPersianDigits()})",
                                targetFontSize = 11.sp,
                                minFontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            AutoResizeText(
                                text = if (isProSeason) "ورود به بورد ${bird.titleFa} 👑 ➔" else "ورود به نقشه ${bird.titleFa} ➔",
                                targetFontSize = 12.5.sp,
                                minFontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isProSeason) Color(0xFFFEF08A) else Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

