package com.example.poultryapp.ui.screens.breeding_game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.data.model.*
import com.example.poultryapp.data.repository.BreedingGameRepository
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.ui.screens.breeding_game.components.*

/**
 * صفحه اصلی بازی و مسیر یادگیری پرورش طیور (جایگزین راهنمای سنتی)
 * الهام‌گرفته از نقشه کندی‌کراش با مراحل واقعی زندگی و پرورش طیور
 */
@Composable
fun BreedingGameScreen(
    onOpenDrawer: () -> Unit,
    onOpenLegacyGuide: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val repository = remember { BreedingGameRepository(context) }

    // وضعیت پیشرفت و لودینگ محلی
    var refreshKey by remember { mutableIntStateOf(0) }
    val profile = remember(refreshKey) { repository.getFarmerProfile() }
    val allProgress = remember(refreshKey) { repository.getAllStagesProgress() }
    val unlockedCards = remember(refreshKey) { repository.getUnlockedKnowledgeCards() }

    // وضعیت دیالوگ‌ها
    var stageForIntro by remember { mutableStateOf<GameStage?>(null) }
    var activePlayingStage by remember { mutableStateOf<GameStage?>(null) }
    var victoryStageInfo by remember { mutableStateOf<Pair<GameStage, Int>?>(null) }
    var showKnowledgeVault by remember { mutableStateOf(false) }

    // کنترل اسکرول لیست نقشه
    val listState = rememberLazyListState()

    // اسکرول اولیه به سمت مرحله فعال در اولین ورود
    LaunchedEffect(profile.currentActiveStageId) {
        val targetIndex = (BreedingGameCatalog.stages.size - profile.currentActiveStageId).coerceAtLeast(0)
        listState.animateScrollToItem(targetIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF388E3C), // سبز چمنزار بالا
                        Color(0xFF4CAF50),
                        Color(0xFF81C784),
                        Color(0xFFA5D6A7),
                        Color(0xFFC8E6C9)  // سبز روشن پایین
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ۱. هدر بالایی نقشه (Top Bar)
            GameTopHeader(
                totalStars = profile.totalStars,
                maxStars = profile.maxPossibleStars,
                farmerRank = profile.farmerRankTitleFa,
                onProfileClick = { showKnowledgeVault = true }
            )

            // ۲. نقشه پر پیچ و خم مراحل (Scrollable Saga Map)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // قله مسیر و کاپ قهرمانی (بالای نقشه)
                    item {
                        GoldenTrophySummitSign(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .clickable {
                                    val summitStage = BreedingGameCatalog.stages.lastOrNull()
                                    if (summitStage != null) {
                                        stageForIntro = summitStage
                                    }
                                }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // مراحل ۱۵ تا ۱ (از بالا به پایین)
                    val reversedStages = BreedingGameCatalog.stages.reversed()
                    items(reversedStages.size) { index ->
                        val stage = reversedStages[index]
                        val progress = allProgress[stage.id] ?: return@items
                        val isCurrentActive = stage.id == profile.currentActiveStageId

                        // چیدمان زیگزاگی گره‌ها شبیه راه پر پیچ و خم کندی‌کراش
                        // باقیمانده مرحله: 0: مرکز، 1: راست، 2: مرکز، 3: چپ
                        val alignmentBias = when (stage.stageNumber % 4) {
                            1 -> Alignment.End
                            2 -> Alignment.CenterHorizontally
                            3 -> Alignment.Start
                            else -> Alignment.CenterHorizontally
                        }

                        // خطوط سنگ‌فرش مسیر
                        CobblestonePathStep()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = when (alignmentBias) {
                                Alignment.End -> Arrangement.End
                                Alignment.Start -> Arrangement.Start
                                else -> Arrangement.Center
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                        ) {
                            if (alignmentBias == Alignment.End) {
                                // کپسول رشد در سمت چپ گره
                                StageEvolutionPill(
                                    growthStage = stage.growthStage,
                                    ageTitleFa = stage.ageLabelFa,
                                    daysRangeFa = stage.daysRangeFa,
                                    isUnlocked = progress.isUnlocked
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                SagaStageNode(
                                    stage = stage,
                                    progress = progress,
                                    isCurrentActive = isCurrentActive,
                                    onClick = { stageForIntro = stage }
                                )
                            } else if (alignmentBias == Alignment.Start) {
                                // گره در سمت چپ، کپسول در سمت راست
                                SagaStageNode(
                                    stage = stage,
                                    progress = progress,
                                    isCurrentActive = isCurrentActive,
                                    onClick = { stageForIntro = stage }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                StageEvolutionPill(
                                    growthStage = stage.growthStage,
                                    ageTitleFa = stage.ageLabelFa,
                                    daysRangeFa = stage.daysRangeFa,
                                    isUnlocked = progress.isUnlocked
                                )
                            } else {
                                // در مرکز
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SagaStageNode(
                                        stage = stage,
                                        progress = progress,
                                        isCurrentActive = isCurrentActive,
                                        onClick = { stageForIntro = stage }
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    StageEvolutionPill(
                                        growthStage = stage.growthStage,
                                        ageTitleFa = stage.ageLabelFa,
                                        daysRangeFa = stage.daysRangeFa,
                                        isUnlocked = progress.isUnlocked
                                    )
                                }
                            }
                        }

                        // تابلوی چوبی تشویقی در میانه راه (بین مرحله ۶ و ۷)
                        if (stage.stageNumber == 6) {
                            Spacer(modifier = Modifier.height(10.dp))
                            WoodenMotivationalSign(
                                text = "با هر مرحله یک قدم به سوی یک مرغداری موفق‌تر!",
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                                    .fillMaxWidth(0.9f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // تابلوی شروع جاده در انتهای لیست (شروع از مرحله ۱)
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            WoodenStartSign()
                            Text(
                                text = "🐣 پذیرش جوجه یک‌روزه",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D)
                            )
                        }

                        // تابلوی چوبی مراحل بعدی و فصول پیشرفته
                        NextChaptersWoodenBoard(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }

            // ۳. نوار اکشن پایینی (Bottom Navigation)
            GameBottomActionBar(
                currentStageNumber = profile.currentActiveStageId,
                onBackClick = onOpenDrawer,
                onPlayActiveStage = {
                    val activeStage = BreedingGameCatalog.getStageById(profile.currentActiveStageId)
                        ?: BreedingGameCatalog.stages.first()
                    stageForIntro = activeStage
                },
                onOpenVault = { showKnowledgeVault = true }
            )
        }

        // دیالوگ معرفی مرحله
        stageForIntro?.let { stage ->
            val progress = allProgress[stage.id] ?: StageProgress(stage.id)
            StageIntroDialog(
                stage = stage,
                progress = progress,
                onStartChallenge = {
                    val target = stage
                    stageForIntro = null
                    activePlayingStage = target
                },
                onDismiss = { stageForIntro = null }
            )
        }

        // دیالوگ چالش و گیم‌پلی مرحله
        activePlayingStage?.let { stage ->
            StageChallengeDialog(
                stage = stage,
                onCompleteStage = { earnedStars ->
                    repository.completeStage(stage.id, earnedStars)
                    refreshKey++
                    val completedStage = stage
                    activePlayingStage = null
                    victoryStageInfo = Pair(completedStage, earnedStars)
                },
                onDismiss = { activePlayingStage = null }
            )
        }

        // دیالوگ پیروزی و اعطای کارت طلایی
        victoryStageInfo?.let { (stage, earnedStars) ->
            StageVictoryDialog(
                stage = stage,
                earnedStars = earnedStars,
                onContinueNext = {
                    victoryStageInfo = null
                }
            )
        }

        // مودال گنجینه دانش و پیشرفت مزرعه‌دار
        if (showKnowledgeVault) {
            KnowledgeVaultDialog(
                profile = profile,
                unlockedCards = unlockedCards,
                onOpenLegacyQnaBank = onOpenLegacyGuide,
                onDismiss = { showKnowledgeVault = false }
            )
        }
    }
}

/**
 * هدر بالایی نقشه بازی
 */
@Composable
private fun GameTopHeader(
    totalStars: Int,
    maxStars: Int,
    farmerRank: String,
    onProfileClick: () -> Unit
) {
    Surface(
        color = Color(0xFF0F2F14),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            // بخش لوگو و عنوان
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌿", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "مسیر طلایی پرورش طیور",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "از جوجه تا تخم‌گذاری، گام به گام با شما",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA7F3D0)
                    )
                }
            }

            // بخش ستاره‌ها و پروفایل
            Row(verticalAlignment = Alignment.CenterVertically) {
                // کپسول ستاره‌ها
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1B4324))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
                        .clickable { onProfileClick() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "ستاره",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${totalStars.toPersianDigits()}/${maxStars.toPersianDigits()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFBEB)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // آیکون پروفایل
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32))
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "پروفایل",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * سنگ‌فرش‌های متصل‌کننده مراحل
 */
@Composable
private fun CobblestonePathStep() {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF8DC).copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}

/**
 * نوار اکشن پایینی نقشه
 */
@Composable
private fun GameBottomActionBar(
    currentStageNumber: Int,
    onBackClick: () -> Unit,
    onPlayActiveStage: () -> Unit,
    onOpenVault: () -> Unit
) {
    Surface(
        color = Color(0xFF0F2F14).copy(alpha = 0.95f),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            // ۱. بازگشت
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4ADE80)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "بازگشت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // ۲. ورود به مرحله فعال جاری
            Button(
                onClick = onPlayActiveStage,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(text = "🎮", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "مرحله ${currentStageNumber.toPersianDigits()}: ورود به چالش",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // ۳. پیشرفت من
            Button(
                onClick = onOpenVault,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "پیشرفت من", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
