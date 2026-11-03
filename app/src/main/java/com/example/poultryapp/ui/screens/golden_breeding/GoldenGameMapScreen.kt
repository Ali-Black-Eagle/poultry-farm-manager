package com.example.poultryapp.ui.screens.golden_breeding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.R
import com.example.poultryapp.data.model.*
import com.example.poultryapp.data.repository.GoldenBreedingRepository
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.ui.components.AutoResizeText
import com.example.poultryapp.ui.screens.breeding_game.components.GoldenCardPreview
import com.example.poultryapp.ui.screens.golden_breeding.components.*

/**
 * نقشه ماجراجویی دو بعدی سطح بالا با جاده سنگ‌فرش، گره‌های متحرک جواهرنشان،
 * ساید کوئست‌های بحرانی و صندوقچه جوایز نقطه عطف
 */
@Composable
fun GoldenGameMapScreen(
    birdType: GoldenBirdType,
    seasonTier: GameSeasonTier = GameSeasonTier.NOVICE,
    onBackToSelection: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { GoldenBreedingRepository(context) }
    val isPro = seasonTier == GameSeasonTier.PROFESSIONAL
    val theme = remember(birdType, isPro) {
        if (isPro) BirdArtTheme.getProThemeForBird(birdType)
        else BirdArtTheme.getThemeForBird(birdType)
    }
    val stages = remember(birdType, seasonTier) {
        GoldenBreedingCatalog.getStagesForBird(birdType, seasonTier)
    }
    val sideQuests = remember(birdType) { GoldenBreedingCatalog.getSideQuestsForBird(birdType) }

    var refreshKey by remember { mutableIntStateOf(0) }
    val summary = remember(refreshKey, birdType, isPro) {
        if (isPro) repository.getProBirdProgressSummary(birdType)
        else repository.getBirdProgressSummary(birdType)
    }
    val unlockedCards = remember(refreshKey, birdType) { repository.getUnlockedCards(birdType) }

    // کش کردن پیشرفت مراحل و وضعیت سایدکوئست‌ها جهت اسکرول روان بدون I/O
    val stageProgressMap = remember(refreshKey, birdType, isPro, summary) {
        stages.associate { stage ->
            stage.id to (if (isPro) repository.getProStageProgress(birdType, stage) else repository.getStageProgress(birdType, stage))
        }
    }
    val sideQuestStarsMap = remember(refreshKey, birdType, summary) {
        repository.getAllSideQuestStars(birdType)
    }
    val sideQuestByUnlockStage = remember(sideQuests) {
        sideQuests.associateBy { it.unlockStageRequired }
    }

    // وضعیت دیالوگ‌ها و مودال‌ها
    var stageForIntro by remember { mutableStateOf<GoldenStage?>(null) }
    var activePlayingStage by remember { mutableStateOf<GoldenStage?>(null) }
    var victoryStageInfo by remember { mutableStateOf<Pair<GoldenStage, Int>?>(null) }
    var pendingMilestoneReward by remember { mutableStateOf<GoldenFarmReward?>(null) }

    var showVaultSheet by remember { mutableStateOf(false) }
    var showMedalsDialog by remember { mutableStateOf(false) }
    var showSideQuestsSheet by remember { mutableStateOf(false) }
    var activeSideQuest by remember { mutableStateOf<GoldenSideQuest?>(null) }
    var sideQuestVictoryInfo by remember { mutableStateOf<Pair<GoldenSideQuest, Int>?>(null) }

    val initialScrollIdx = remember(birdType, isPro, summary.currentActiveStageNumber) {
        (stages.size - summary.currentActiveStageNumber).coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIdx)
    var hasInitialScrolled by remember(birdType, isPro) { mutableStateOf(false) }

    // اسکرول نرم در صورت تغییر مرحله حین بازی
    LaunchedEffect(summary.currentActiveStageNumber, birdType, isPro) {
        val targetIdx = (stages.size - summary.currentActiveStageNumber).coerceAtLeast(0)
        if (!hasInitialScrolled) {
            hasInitialScrolled = true
        } else {
            listState.animateScrollToItem(targetIdx)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundBrush)
    ) {
        // ۱. لایه ذرات معلق محیطی اختصاصی پرنده (همگام با وضعیت اسکرول)
        AmbientParticleLayer(
            particleEmojis = theme.particleEmojis,
            isScrolling = listState.isScrollInProgress
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ۲. نوار ابزار بالای نقشه با رنگ‌آمیزی کامل تا بالای صفحه گوشی
            Surface(
                color = theme.headerSurfaceColor,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .fillMaxWidth()
                    ) {
                        // دکمه بازگشت به لیست پرندگان
                        OutlinedButton(
                            onClick = onBackToSelection,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.8f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "گونه‌ها", fontSize = 11.sp, fontWeight = FontWeight.Bold, softWrap = false)
                        }

                        // عنوان پرنده و تگ محیطی
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isPro) {
                                    Text(text = "👑", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                AutoResizeText(
                                    text = birdType.titleFa,
                                    targetFontSize = 14.sp,
                                    minFontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                            AutoResizeText(
                                text = if (isPro) "بورد فوق‌حرفه‌ای و مگافارمر" else theme.environmentTagFa,
                                targetFontSize = 10.5.sp,
                                minFontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                color = theme.primaryAccent,
                                maxLines = 1
                            )
                        }

                        // ستاره‌ها و دستاوردها (با محافظت ضد فشرده‌شدن)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = theme.primaryAccent.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.85f)),
                            modifier = Modifier
                                .wrapContentWidth()
                                .clickable { showVaultSheet = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                // مجموع ستاره‌های طلایی (مراحل اصلی + مراحل فرعی ادغام‌شده)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "مجموع ستاره‌ها",
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "\u200E${summary.totalStars.toPersianDigits()} / ${summary.maxPossibleStars.toPersianDigits()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFFFBEB),
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // ۳. نوار HUD تعاملی بازی (سکه‌ها، تالار جوایز، ساید کوئست‌ها) - طراحی کپسولی متقارن و ضد برش
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        // تالار مدال‌های افتخار (جایگزین لوح‌ها و جوایز)
                        val unlockedMedalsCount = remember(refreshKey, birdType, seasonTier) {
                            repository.getUnlockedMedalsCountForSeason(birdType, seasonTier)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF78350F).copy(alpha = 0.8f))
                                .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(12.dp))
                                .clickable { showMedalsDialog = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(text = "🏅", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            AutoResizeText(
                                text = "مدال‌ها (${unlockedMedalsCount.toPersianDigits()}/۳)",
                                targetFontSize = 11.5.sp,
                                minFontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFEF08A),
                                maxLines = 1
                            )
                        }

                        // ساید کوئست‌ها و مراحل فرعی
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF064E3B).copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                                .clickable { showSideQuestsSheet = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(text = "🛡️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            AutoResizeText(
                                text = "مراحل فرعی (${summary.completedSideQuestsCount.toPersianDigits()}/${sideQuests.size.toPersianDigits()})",
                                targetFontSize = 11.sp,
                                minFontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFA7F3D0),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // ۴. نقشه ماجراجویی دو بعدی با گره‌های متحرک و سنگ‌فرش
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // اثر هنری و منظره‌پردازی متناسب با اقلیم و اتمسفر گونه پرنده
                BirdMapAtmosphericBackdrop(
                    birdType = birdType,
                    isPro = isPro,
                    isScrolling = listState.isScrollInProgress
                )

                val reversedStages = remember(stages) { stages.reversed() }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // قله مسیر و کاپ استادی
                    item(key = "summit_trophy_header", contentType = "summit_header") {
                        SummitSignboard(
                            title = theme.summitTrophyTitleFa,
                            birdTitle = birdType.titleFa,
                            theme = theme,
                            onClick = {
                                val finalStage = stages.lastOrNull()
                                if (finalStage != null) stageForIntro = finalStage
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(
                        count = reversedStages.size,
                        key = { reversedStages[it].id },
                        contentType = { "stage_node" }
                    ) { index ->
                        val stage = reversedStages[index]
                        val progress = stageProgressMap[stage.id] ?: StageProgress(stage.id, isUnlocked = stage.stageNumber == 1, isCompleted = false, stars = 0, isCardUnlocked = false)
                        val isCurrentActive = stage.stageNumber == summary.currentActiveStageNumber

                        // چیدمان زیگزاگی نقشه
                        val bias = when (stage.stageNumber % 4) {
                            1 -> Alignment.End
                            2 -> Alignment.CenterHorizontally
                            3 -> Alignment.Start
                            else -> Alignment.CenterHorizontally
                        }

                        // مسیر و اتصالات منحصربه‌فرد بر اساس گونه پرنده (نردبان طنابی، برگ‌های شناور، صخره، رمل‌های شنی و ...)
                        BirdArtisanPathConnector(
                            birdType = birdType,
                            isCurrentActive = isCurrentActive
                        )

                        // ردیف گره مرحله و سن
                        Column(
                            horizontalAlignment = when (bias) {
                                Alignment.End -> Alignment.End
                                Alignment.Start -> Alignment.Start
                                else -> Alignment.CenterHorizontally
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = when (bias) {
                                    Alignment.End -> Arrangement.End
                                    Alignment.Start -> Arrangement.Start
                                    else -> Arrangement.Center
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (bias == Alignment.End) {
                                    BirdGrowthPill(
                                        emoji = stage.avatarEmoji,
                                        ageTitleFa = stage.ageLabelFa,
                                        daysRangeFa = stage.daysRangeFa,
                                        isUnlocked = progress.isUnlocked,
                                        theme = theme
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    BirdArtisanStageNode(
                                        stageNumber = stage.stageNumber,
                                        birdType = birdType,
                                        isUnlocked = progress.isUnlocked,
                                        isPassed = progress.isCompleted,
                                        isCurrentActive = isCurrentActive,
                                        stars = progress.stars,
                                        theme = theme,
                                        isScrolling = listState.isScrollInProgress,
                                        onClick = { stageForIntro = stage }
                                    )
                                } else if (bias == Alignment.Start) {
                                    BirdArtisanStageNode(
                                        stageNumber = stage.stageNumber,
                                        birdType = birdType,
                                        isUnlocked = progress.isUnlocked,
                                        isPassed = progress.isCompleted,
                                        isCurrentActive = isCurrentActive,
                                        stars = progress.stars,
                                        theme = theme,
                                        isScrolling = listState.isScrollInProgress,
                                        onClick = { stageForIntro = stage }
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    BirdGrowthPill(
                                        emoji = stage.avatarEmoji,
                                        ageTitleFa = stage.ageLabelFa,
                                        daysRangeFa = stage.daysRangeFa,
                                        isUnlocked = progress.isUnlocked,
                                        theme = theme
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BirdArtisanStageNode(
                                            stageNumber = stage.stageNumber,
                                            birdType = birdType,
                                            isUnlocked = progress.isUnlocked,
                                            isPassed = progress.isCompleted,
                                            isCurrentActive = isCurrentActive,
                                            stars = progress.stars,
                                            theme = theme,
                                            isScrolling = listState.isScrollInProgress,
                                            onClick = { stageForIntro = stage }
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        BirdGrowthPill(
                                            emoji = stage.avatarEmoji,
                                            ageTitleFa = stage.ageLabelFa,
                                            daysRangeFa = stage.daysRangeFa,
                                            isUnlocked = progress.isUnlocked,
                                            theme = theme
                                        )
                                    }
                                }
                            }

                            // فانوس ساید کوئست مرتبط در کنار این مرحله (بررسی O(1) سریع در حافظه بدون تأخیر دیسک)
                            val matchingSideQuest = sideQuestByUnlockStage[stage.stageNumber]
                            if (matchingSideQuest != null) {
                                val sqStars = sideQuestStarsMap[matchingSideQuest.id] ?: 0
                                val isSqDone = sqStars > 0
                                val isSqUnlocked = summary.currentActiveStageNumber >= matchingSideQuest.unlockStageRequired

                                Spacer(modifier = Modifier.height(4.dp))
                                SideQuestBeaconPill(
                                    quest = matchingSideQuest,
                                    starsEarned = sqStars,
                                    isCompleted = isSqDone,
                                    isUnlocked = isSqUnlocked,
                                    onClick = {
                                        if (isSqUnlocked) {
                                            activeSideQuest = matchingSideQuest
                                        } else {
                                            showSideQuestsSheet = true
                                        }
                                    }
                                )
                            }
                        }

                        // تابلوی میانه مسیر
                        if (stage.stageNumber == (stages.size / 2)) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = theme.headerSurfaceColor.copy(alpha = 0.92f)),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .border(1.5.dp, theme.primaryAccent, RoundedCornerShape(16.dp))
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "🌿", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "مسیر استادی در پرورش ${birdType.titleFa}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFFBEB),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                }
            }

            // ۵. نوار اکشن پایینی
            Surface(
                color = theme.headerSurfaceColor.copy(alpha = 0.96f),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val curStage = stages.find { it.stageNumber == summary.currentActiveStageNumber }
                                ?: stages.first()
                            stageForIntro = curStage
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "ورود به چالش",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مرحله ${summary.currentActiveStageNumber.toPersianDigits()}: ورود به چالش",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = { showVaultSheet = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Text(text = "📜", fontSize = 20.sp)
                    }
                }
            }
        }

        // مودال معرفی مرحله
        stageForIntro?.let { stage ->
            val progress = if (isPro) repository.getProStageProgress(birdType, stage) else repository.getStageProgress(birdType, stage)
            StageIntroModal(
                stage = stage,
                progress = progress,
                theme = theme,
                onStart = {
                    val target = stage
                    stageForIntro = null
                    activePlayingStage = target
                },
                onDismiss = { stageForIntro = null }
            )
        }

        // دیالوگ چالش‌های درون مرحله اصلی (با شافل رندوم گزینه‌ها)
        activePlayingStage?.let { stage ->
            MultiChallengeDialog(
                stage = stage,
                theme = theme,
                isPro = isPro,
                onCompleteStage = { earnedStars ->
                    if (isPro) {
                        repository.completeProStage(birdType, stage, earnedStars)
                    } else {
                        repository.completeStage(birdType, stage, earnedStars)
                    }
                    val newReward = if (!isPro) repository.checkNewMilestoneReward(birdType, stage.stageNumber) else null
                    refreshKey++
                    val completed = stage
                    activePlayingStage = null
                    victoryStageInfo = Pair(completed, earnedStars)
                    if (newReward != null) {
                        pendingMilestoneReward = newReward
                    }
                },
                onDismiss = { activePlayingStage = null }
            )
        }

        // دیالوگ چالش‌های ساید کوئست اضطراری
        activeSideQuest?.let { quest ->
            MultiChallengeDialog(
                stageTitleFa = quest.titleFa,
                avatarEmoji = quest.rewardBadgeIcon,
                challenges = quest.challenges,
                theme = theme,
                isPro = isPro,
                onCompleteStage = { earnedStars ->
                    repository.completeSideQuest(birdType, quest, earnedStars)
                    refreshKey++
                    val completedQuest = quest
                    activeSideQuest = null
                    sideQuestVictoryInfo = Pair(completedQuest, earnedStars)
                },
                onDismiss = { activeSideQuest = null }
            )
        }

        // دیالوگ جشن پیروزی ساید کوئست
        sideQuestVictoryInfo?.let { (quest, stars) ->
            SideQuestVictoryDialog(
                quest = quest,
                stars = stars,
                theme = theme,
                onDismiss = { sideQuestVictoryInfo = null }
            )
        }

        // دیالوگ پیروزی مرحله با ذرات کانفتی
        victoryStageInfo?.let { (stage, stars) ->
            VictoryCelebrationDialog(
                stage = stage,
                stars = stars,
                theme = theme,
                onClose = {
                    victoryStageInfo = null
                    // اگر پاداش عطف داشت، پس از بستن جشن بلافاصله صندوقچه باز می‌شود
                }
            )
        }

        // صندوقچه گنج طلایی در نقاط عطف
        if (victoryStageInfo == null && pendingMilestoneReward != null) {
            val rewardToClaim = pendingMilestoneReward!!
            MilestoneChestDialog(
                reward = rewardToClaim,
                onClaim = {
                    repository.claimReward(rewardToClaim)
                    refreshKey++
                    pendingMilestoneReward = null
                }
            )
        }

        // تالار مدال‌های افتخار
        if (showMedalsDialog) {
            GoldenMedalsDialog(
                birdType = birdType,
                initialSeason = seasonTier,
                repository = repository,
                onDismiss = { showMedalsDialog = false }
            )
        }

        // شیت ساید کوئست‌ها
        if (showSideQuestsSheet) {
            SideQuestSheet(
                birdType = birdType,
                currentStageNumber = summary.currentActiveStageNumber,
                repository = repository,
                theme = theme,
                onLaunchQuest = { quest ->
                    showSideQuestsSheet = false
                    activeSideQuest = quest
                },
                onDismiss = { showSideQuestsSheet = false }
            )
        }

        // گنجینه دانش پرنده
        if (showVaultSheet) {
            BirdKnowledgeVaultDialog(
                birdType = birdType,
                summary = summary,
                cards = unlockedCards,
                theme = theme,
                onDismiss = { showVaultSheet = false }
            )
        }
    }
}


/**
 * بج سن و دوره رشد پرنده
 */
@Composable
private fun BirdGrowthPill(
    emoji: String,
    ageTitleFa: String,
    daysRangeFa: String,
    isUnlocked: Boolean,
    theme: BirdArtTheme
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isUnlocked) Color(0xFFFFFBEB) else Color(0xFFE2E8F0)),
        modifier = Modifier
            .widthIn(max = 210.dp)
            .border(1.5.dp, if (isUnlocked) theme.primaryAccent.copy(alpha = 0.7f) else Color(0xFFCBD5E1), RoundedCornerShape(18.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                AutoResizeText(
                    text = ageTitleFa,
                    targetFontSize = 11.5.sp,
                    minFontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color(0xFF0F172A) else Color(0xFF64748B),
                    maxLines = 1
                )
                AutoResizeText(
                    text = daysRangeFa.toPersianDigits(),
                    targetFontSize = 10.5.sp,
                    minFontSize = 8.5.sp,
                    color = if (isUnlocked) Color(0xFFB45309) else Color(0xFF94A3B8),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * فانوس نوری ساید کوئست بر روی نقشه
 */
@Composable
private fun SideQuestBeaconPill(
    quest: GoldenSideQuest,
    starsEarned: Int,
    isCompleted: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCompleted) Color(0xFF064E3B).copy(alpha = 0.9f)
                else if (isUnlocked) Color(0xFF7F1D1D).copy(alpha = 0.9f)
                else Color(0xFF1E293B).copy(alpha = 0.7f)
            )
            .border(
                1.dp,
                if (isCompleted) Color(0xFF34D399)
                else if (isUnlocked) Color(0xFFF87171)
                else Color(0xFF475569),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.widthIn(min = 180.dp, max = 340.dp)
        ) {
            Text(text = quest.category.badgeEmoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isCompleted) "ساید کوئست: ${quest.rewardBadgeTitleFa}"
                else if (isUnlocked) "بحران: ${quest.titleFa} 🚨"
                else "ساید کوئست (قفل)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) Color(0xFFA7F3D0) else if (isUnlocked) Color(0xFFFECACA) else Color(0xFF94A3B8),
                maxLines = 2,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isCompleted) {
                Spacer(modifier = Modifier.width(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { idx ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (idx < starsEarned) Color(0xFF34D399) else Color(0xFF065F46),
                            modifier = Modifier.size(11.5.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * سنگ‌فرش‌های متصل‌کننده جاده ماجراجویی
 */
@Composable
private fun TactileMapCobblestones(color: Color, isCurrentActive: Boolean) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        repeat(4) { idx ->
            Box(
                modifier = Modifier
                    .size(if (isCurrentActive && idx == 1) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentActive) Color(0xFFFDE047).copy(alpha = 0.85f)
                        else color.copy(alpha = 0.75f)
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}

@Composable
private fun SummitSignboard(
    title: String,
    birdTitle: String,
    theme: BirdArtTheme,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF78350F)),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clickable { onClick() }
            .border(2.dp, Color(0xFFFDE047), RoundedCornerShape(22.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Text(text = "👑", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFEF08A),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "قله استادی در پرورش $birdTitle",
                fontSize = 12.sp,
                color = Color(0xFFFED7AA)
            )
        }
    }
}

@Composable
private fun StageIntroModal(
    stage: GoldenStage,
    progress: StageProgress,
    theme: BirdArtTheme,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stage.avatarEmoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "مرحله ${stage.stageNumber.toPersianDigits()}: ${stage.titleFa}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${stage.ageLabelFa} (${stage.daysRangeFa})".toPersianDigits(),
                        fontSize = 12.sp,
                        color = theme.primaryAccent
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = stage.missionBriefFa.toPersianDigits(),
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "تعداد چالش‌های این مرحله: ${stage.challenges.size.toPersianDigits()} چالش تخصصی",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.secondaryAccent
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent)
            ) {
                Text(text = "شروع چالش‌ها ➔", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "بستن")
            }
        }
    )
}

/**
 * جشن پیروزی مرحله همراه با موتور کانفتی و ذرات نوری
 */
@Composable
private fun VictoryCelebrationDialog(
    stage: GoldenStage,
    stars: Int,
    theme: BirdArtTheme,
    onClose: () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        ConfettiCelebrationCanvas()

        AlertDialog(
            onDismissRequest = onClose,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "🎉 مرحله با موفقیت فتح شد! 🎉", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF78350F))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        repeat(3) { idx ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (idx < stars) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(34.dp).padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = "کارت طلایی دانش زیر به گنجینه شما افزوده شد:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GoldenCardPreview(card = stage.goldenCard)
                }
            },
            confirmButton = {
                Button(
                    onClick = onClose,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.secondaryAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AutoResizeText(
                        text = "دریافت کارت و ادامه بازی ➔",
                        targetFontSize = 13.sp,
                        minFontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        )
    }
}

/**
 * جشن پیروزی ساید کوئست اضطراری
 */
@Composable
private fun SideQuestVictoryDialog(
    quest: GoldenSideQuest,
    stars: Int,
    theme: BirdArtTheme,
    onDismiss: () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        ConfettiCelebrationCanvas()

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "🛡️ بحران با موفقیت مهار شد!", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF065F46))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = quest.rewardBadgeIcon, fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        repeat(3) { idx ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (idx < stars) Color(0xFF10B981) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(32.dp).padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "نشان افتخار: ${quest.rewardBadgeTitleFa}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "مأموریت بحرانی با موفقیت مهار شد و ${stars.toPersianDigits()} ستاره سبز زمردی در کارنامه پرنده ثبت گردید 🎖️",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "بازگشت به نقشه فارم", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun BirdKnowledgeVaultDialog(
    birdType: GoldenBirdType,
    summary: BirdProgressSummary,
    cards: List<KnowledgeCard>,
    theme: BirdArtTheme,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            border = BorderStroke(1.5.dp, theme.primaryAccent.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // نوار بالای مودال
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = theme.primaryAccent.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "📜", fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "گنجینه دانش ${birdType.titleFa}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "راهنماهای علمی و تجربی پرورش طلایی",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // نوار وضعیت پیشرفت و رتبه
                Surface(
                    color = Color(0xFF111827),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // رتبه مزرعه‌دار
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎖️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "رتبه: ${summary.masteryTitleFa}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryAccent
                            )
                        }

                        // ستاره‌ها
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⭐ \u200E${summary.totalStars.toPersianDigits()} / ${summary.maxPossibleStars.toPersianDigits()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }

                        // تعداد کارت‌ها
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            Text(
                                text = "📖 ${cards.size.toPersianDigits()} کارت",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // لیست اسکرول‌شونده کارت‌ها
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (cards.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Text(text = "🔒", fontSize = 40.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "هنوز کارتی برای این پرنده باز نکرده‌اید!",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "با پاسخ به سوالات مراحل نقشه، کارت‌های طلایی تخصصی این گونه را آزاد کنید و در اینجا مطالعه نمایید.",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(cards) { card ->
                            GoldenCardPreview(card = card)
                        }
                    }
                }

                // فوتر با دکمه بستن در دسترس
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "بستن و ادامه بازی",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
