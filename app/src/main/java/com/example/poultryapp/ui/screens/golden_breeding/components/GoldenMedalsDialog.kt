package com.example.poultryapp.ui.screens.golden_breeding.components

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.ui.components.AutoResizeText
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poultryapp.data.model.GameSeasonTier
import com.example.poultryapp.data.model.GoldenBirdType
import com.example.poultryapp.data.model.GoldenMedal
import com.example.poultryapp.data.model.MedalCategory
import com.example.poultryapp.data.repository.GoldenBreedingRepository
import com.example.poultryapp.engine.toPersianDigits
import kotlinx.coroutines.launch

/**
 * دیالوگ تالار مدال‌های افتخار آکادمی پرورش طلایی
 * همراه با قابلیت انتخاب تمامی ۱۲ گونه پرنده، سوایپ افقی بین فصل‌ها
 * و دکمه‌های مرتب و چشم‌نواز فصل ۱ (مقدماتی) و فصل ۲ (بورد استادی)
 */
@Composable
fun GoldenMedalsDialog(
    birdType: GoldenBirdType = GoldenBirdType.LAYER_HEN,
    initialSeason: GameSeasonTier = GameSeasonTier.NOVICE,
    repository: GoldenBreedingRepository,
    onDismiss: () -> Unit
) {
    var currentBirdType by remember(birdType) { mutableStateOf(birdType) }
    var selectedMedalForDetails by remember { mutableStateOf<GoldenMedal?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = if (initialSeason == GameSeasonTier.PROFESSIONAL) 1 else 0,
        pageCount = { 2 }
    )

    val noviceMedals = remember(currentBirdType) {
        repository.getMedalsForBird(currentBirdType, GameSeasonTier.NOVICE)
    }
    val proMedals = remember(currentBirdType) {
        repository.getMedalsForBird(currentBirdType, GameSeasonTier.PROFESSIONAL)
    }

    val noviceUnlockedCount = noviceMedals.count { it.isUnlocked }
    val proUnlockedCount = proMedals.count { it.isUnlocked }
    val activeUnlockedCount = if (pagerState.currentPage == 0) noviceUnlockedCount else proUnlockedCount

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 8.dp)
                .border(
                    2.dp,
                    Brush.linearGradient(
                        listOf(Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF3B82F6))
                    ),
                    RoundedCornerShape(26.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ۱. هدر دیالوگ مدال‌ها
                Surface(
                    color = Color(0xFF1C2541),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = Color(0xFF94A3B8)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🏅", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                AutoResizeText(
                                    text = "مدال‌های افتخار ${currentBirdType.titleFa}",
                                    targetFontSize = 15.sp,
                                    minFontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFEF08A),
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "${activeUnlockedCount.toPersianDigits()} از ۳ مدال این فصل کسب شده",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pagerState.currentPage == 0) Color(0xFF34D399) else Color(0xFFFBBF24)
                            )
                        }

                        // بج پرنده انتخابی
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFFFBBF24), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = currentBirdType.avatarEmoji, fontSize = 20.sp)
                        }
                    }
                }

                // ۲. نوار اسکرول‌پذیر افقی انتخاب ۱۲ گونه پرنده
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(GoldenBirdType.entries) { bird ->
                        val isSelected = bird == currentBirdType
                        val birdMedalsUnlocked = remember(bird, repository) {
                            repository.getUnlockedMedalsCountForSeason(bird, GameSeasonTier.NOVICE) +
                            repository.getUnlockedMedalsCountForSeason(bird, GameSeasonTier.PROFESSIONAL)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B).copy(alpha = 0.7f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF475569).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.clickable { currentBirdType = bird }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(text = bird.avatarEmoji, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = bird.titleFa,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFFEF08A) else Color(0xFFCBD5E1)
                                )
                                if (birdMedalsUnlocked > 0) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF047857),
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = birdMedalsUnlocked.toPersianDigits(),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ۳. دکمه‌های کپسولی شکیل و مرتب انتخاب دو فصل
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF111827))
                        .border(1.dp, Color(0xFF374151), RoundedCornerShape(16.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // تب فصل ۱: دوره مقدماتی
                    val isNoviceActive = pagerState.currentPage == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                if (isNoviceActive) Brush.horizontalGradient(listOf(Color(0xFF065F46), Color(0xFF047857)))
                                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .border(
                                width = if (isNoviceActive) 1.2.dp else 0.dp,
                                color = if (isNoviceActive) Color(0xFF34D399) else Color.Transparent,
                                shape = RoundedCornerShape(13.dp)
                            )
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🌱", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "فصل ۱: مقدماتی",
                                fontSize = 12.sp,
                                fontWeight = if (isNoviceActive) FontWeight.Black else FontWeight.Bold,
                                color = if (isNoviceActive) Color(0xFFECFDF5) else Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isNoviceActive) Color(0xFF064E3B) else Color(0xFF1F2937),
                                border = BorderStroke(0.6.dp, if (isNoviceActive) Color(0xFF10B981) else Color(0xFF4B5563))
                            ) {
                                Text(
                                    text = "${noviceUnlockedCount.toPersianDigits()}/۳",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isNoviceActive) Color(0xFFA7F3D0) else Color(0xFF9CA3AF),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // تب فصل ۲: بورد استادی
                    val isProActive = pagerState.currentPage == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                if (isProActive) Brush.horizontalGradient(listOf(Color(0xFF78350F), Color(0xFF92400E)))
                                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .border(
                                width = if (isProActive) 1.2.dp else 0.dp,
                                color = if (isProActive) Color(0xFFFBBF24) else Color.Transparent,
                                shape = RoundedCornerShape(13.dp)
                            )
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "👑", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "فصل ۲: استادی",
                                fontSize = 12.sp,
                                fontWeight = if (isProActive) FontWeight.Black else FontWeight.Bold,
                                color = if (isProActive) Color(0xFFFEF08A) else Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isProActive) Color(0xFF451A03) else Color(0xFF1F2937),
                                border = BorderStroke(0.6.dp, if (isProActive) Color(0xFFF59E0B) else Color(0xFF4B5563))
                            ) {
                                Text(
                                    text = "${proUnlockedCount.toPersianDigits()}/۳",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isProActive) Color(0xFFFDE68A) else Color(0xFF9CA3AF),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // راهنمای سوایپ بین فصل‌ها
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (pagerState.currentPage == 0) "👈 برای مشاهده مدال‌های بورد استادی به چپ سوایپ کنید 👉"
                               else "👈 برای بازگشت به مدال‌های دوره مقدماتی به راست سوایپ کنید 👉",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ۴. پیجر افقی با قابلیت سوایپ دستی بین مدال‌های دو فصل
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val seasonMedals = if (page == 0) noviceMedals else proMedals

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(seasonMedals) { medal ->
                            MedalItemCard(
                                medal = medal,
                                onClick = { selectedMedalForDetails = medal }
                            )
                        }
                    }
                }
            }
        }
    }

    // دیالوگ جزییات و دانشنامه مدال
    selectedMedalForDetails?.let { medal ->
        MedalDetailsDialog(
            medal = medal,
            birdType = currentBirdType,
            onDismiss = { selectedMedalForDetails = null }
        )
    }
}

/**
 * کارت بزرگ و باوقار مدال افتخار با تم و تصویرسازی متمایز مفهومی
 */
@Composable
private fun MedalItemCard(
    medal: GoldenMedal,
    onClick: () -> Unit
) {
    val isUnlocked = medal.isUnlocked

    val cardBorder = if (isUnlocked) {
        BorderStroke(1.5.dp, medal.primaryColor)
    } else {
        BorderStroke(1.dp, Color(0xFF334155))
    }

    val cardBg = if (isUnlocked) {
        Brush.horizontalGradient(
            listOf(
                medal.secondaryColor.copy(alpha = 0.85f),
                Color(0xFF0F172A).copy(alpha = 0.95f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF1E293B).copy(alpha = 0.7f),
                Color(0xFF0F172A).copy(alpha = 0.8f)
            )
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // سطر هدر کارت مدال: نوع نشان و وضعیت
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // تگ رتبه مدال
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isUnlocked) medal.primaryColor.copy(alpha = 0.25f) else Color(0xFF334155).copy(alpha = 0.5f),
                    border = BorderStroke(0.8.dp, if (isUnlocked) medal.primaryColor else Color(0xFF64748B))
                ) {
                    Text(
                        text = medal.tierTitleFa,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) medal.primaryColor else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // نشان وضعیت بازگشایی
                if (isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF047857),
                        border = BorderStroke(0.8.dp, Color(0xFF34D399))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "اعطا شد ✅", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "قفل (${medal.progressPercentage.toPersianDigits()}٪)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // بخش بدنه کارت: تصویر مفهومی مدال و توضیحات
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // آیکون و تصویرسازی اختصاصی مدال
                MedalGraphicVisual(medal = medal, modifier = Modifier.size(62.dp))

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medal.titleFa,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isUnlocked) Color.White else Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = medal.subtitleFa,
                        fontSize = 11.5.sp,
                        color = if (isUnlocked) Color(0xFFCBD5E1) else Color(0xFF64748B),
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // نوار پیشرفت یا تاریخ دریافت
            if (isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📜 مفتخر به کسب این نشان زرین",
                        fontSize = 11.sp,
                        color = Color(0xFFA7F3D0)
                    )
                    Text(
                        text = medal.unlockedDate?.let { "تاریخ: ${it.toPersianDigits()}" } ?: "مرحله تکمیل شد",
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AutoResizeText(
                            text = medal.requirementDescriptionFa,
                            targetFontSize = 10.5.sp,
                            minFontSize = 8.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${medal.progressCurrent.toPersianDigits()} / ${medal.progressMax.toPersianDigits()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = medal.primaryColor
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { (medal.progressCurrent.toFloat() / medal.progressMax.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = medal.primaryColor,
                        trackColor = Color(0xFF334155)
                    )
                }
            }
        }
    }
}

/**
 * تصویرسازی مفهومی و بصری متمایز برای هر مدال با استفاده از آرت‌ورک‌های اختصاصی سه‌بعدی
 */
@Composable
private fun MedalGraphicVisual(
    medal: GoldenMedal,
    modifier: Modifier = Modifier
) {
    val isUnlocked = medal.isUnlocked

    // گرادیان دایره مرکزی متناسب با تم اختصاصی مدال
    val glowBrush = when (medal.category) {
        MedalCategory.SIDE_QUEST -> Brush.radialGradient(listOf(Color(0xFF34D399).copy(alpha = 0.5f), Color(0xFF064E3B)))
        MedalCategory.NOVICE_CYCLE -> Brush.radialGradient(listOf(Color(0xFFFBBF24).copy(alpha = 0.5f), Color(0xFF1E3A8A)))
        MedalCategory.MASTERY_3STAR -> Brush.radialGradient(listOf(Color(0xFFFEF08A).copy(alpha = 0.5f), Color(0xFF991B1B)))
        MedalCategory.PRO_PATHOLOGY -> Brush.radialGradient(listOf(Color(0xFF22D3EE).copy(alpha = 0.5f), Color(0xFF0F172A)))
        MedalCategory.PRO_CLIMATE -> Brush.radialGradient(listOf(Color(0xFFFB923C).copy(alpha = 0.5f), Color(0xFF581C87)))
        MedalCategory.PRO_GRANDMASTER -> Brush.radialGradient(listOf(Color(0xFFC084FC).copy(alpha = 0.5f), Color(0xFF78350F)))
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isUnlocked) glowBrush else Brush.radialGradient(listOf(Color(0xFF334155), Color(0xFF0F172A))))
            .border(
                2.5.dp,
                if (isUnlocked) medal.primaryColor else Color(0xFF475569),
                CircleShape
            )
            .shadow(if (isUnlocked) 10.dp else 0.dp, CircleShape, spotColor = medal.primaryColor),
        contentAlignment = Alignment.Center
    ) {
        // تصویر سه‌بعدی و رندر اختصاصی مدال
        Image(
            painter = painterResource(id = medal.imageRes),
            contentDescription = medal.titleFa,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .alpha(if (isUnlocked) 1f else 0.35f)
        )

        // لایه محافظ و آیکون قفل در حالت بازنشده
        if (!isUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "قفل",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * دیالوگ نمایش مشخصات دانشنامه و جزئیات هر مدال
 */
@Composable
private fun MedalDetailsDialog(
    medal: GoldenMedal,
    birdType: GoldenBirdType,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(24.dp),
        icon = {
            MedalGraphicVisual(medal = medal, modifier = Modifier.size(90.dp))
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = medal.titleFa,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFEF08A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${medal.tierTitleFa} • ${birdType.titleFa}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = medal.primaryColor
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = medal.conceptDescriptionFa,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFFE2E8F0),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (medal.isUnlocked) Color(0xFF10B981) else Color(0xFF475569)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (medal.isUnlocked) "وضعیت: این مدال با موفقیت کسب گردیده است 🎖️" else "شرط اعطا: ${medal.requirementDescriptionFa}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (medal.isUnlocked) Color(0xFF34D399) else Color(0xFFFBBF24),
                            textAlign = TextAlign.Center
                        )
                        if (medal.isUnlocked && medal.unlockedDate != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تاریخ ثبت دانشنامه: ${medal.unlockedDate.toPersianDigits()}",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = medal.primaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "متوجه شدم", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}
