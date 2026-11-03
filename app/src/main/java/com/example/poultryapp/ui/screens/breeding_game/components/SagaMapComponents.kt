package com.example.poultryapp.ui.screens.breeding_game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.data.model.ChickenGrowthStage
import com.example.poultryapp.data.model.GameStage
import com.example.poultryapp.data.model.StageProgress
import com.example.poultryapp.engine.toPersianDigits

/**
 * گره مرحله در نقشه بازی (Stage Milestone Node)
 */
@Composable
fun SagaStageNode(
    stage: GameStage,
    progress: StageProgress,
    isCurrentActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // پالس انیمیشنی برای مرحله جاری
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isPassed = progress.isCompleted
    val isUnlocked = progress.isUnlocked

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable(enabled = isUnlocked) { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .then(if (isCurrentActive) Modifier.scale(pulseScale) else Modifier)
        ) {
            // حلقه بیرونی نورانی برای مرحله فعال
            if (isCurrentActive) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4ADE80).copy(alpha = 0.6f),
                                    Color(0xFF22C55E).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // بدنه اصلی دایره گره
            val nodeBrush = when {
                isPassed -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF34D399), Color(0xFF059669))
                )
                isCurrentActive -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF4ADE80), Color(0xFF16A34A))
                )
                else -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF64748B), Color(0xFF334155))
                )
            }

            val borderColor = when {
                isPassed -> Color(0xFFA7F3D0)
                isCurrentActive -> Color(0xFFDCFCE7)
                else -> Color(0xFF94A3B8).copy(alpha = 0.5f)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .shadow(
                        elevation = if (isUnlocked) 8.dp else 2.dp,
                        shape = CircleShape,
                        spotColor = if (isUnlocked) Color(0xFF16A34A) else Color.Black
                    )
                    .clip(CircleShape)
                    .background(nodeBrush)
                    .border(3.dp, borderColor, CircleShape)
            ) {
                if (isUnlocked) {
                    Text(
                        text = stage.stageNumber.toPersianDigits(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stage.stageNumber.toPersianDigits(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "قفل",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ستاره‌های زیر گره در صورت تکمیل مرحله
        if (isPassed && progress.stars > 0) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .background(Color(0xFF14532D).copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                repeat(3) { index ->
                    val starFilled = index < progress.stars
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (starFilled) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * کپسول شناور اطلاعات سن و رشد جوجه در کنار هر گره
 */
@Composable
fun StageEvolutionPill(
    growthStage: ChickenGrowthStage,
    ageTitleFa: String,
    daysRangeFa: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color(0xFFFFFBEB) else Color(0xFFF1F5F9).copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 4.dp else 1.dp),
        modifier = modifier
            .border(
                1.5.dp,
                if (isUnlocked) Color(0xFFFDE68A) else Color(0xFFCBD5E1),
                RoundedCornerShape(20.dp)
            )
            .padding(1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // آواتار سن پرنده
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(growthStage.avatarTint).copy(alpha = if (isUnlocked) 0.9f else 0.4f))
            ) {
                Text(
                    text = growthStage.iconEmoji,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = ageTitleFa,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF64748B)
                )
                Text(
                    text = daysRangeFa.toPersianDigits(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isUnlocked) Color(0xFFB45309) else Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * تابلوی چوبی روستایی انگیزشی بالای جاده
 */
@Composable
fun WoodenMotivationalSign(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF8D5B28), Color(0xFF653D16))
                ),
                RoundedCornerShape(16.dp)
            )
            .border(2.dp, Color(0xFFD49B55), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "🌿", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Color(0xFFFFFBEB),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * تابلوی چوبی شروع جاده
 */
@Composable
fun WoodenStartSign(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF8B4513), Color(0xFF5C2C10))
                ),
                RoundedCornerShape(12.dp)
            )
            .border(1.5.dp, Color(0xFFCD853F), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "شروع ➔",
            color = Color(0xFFFFF8DC),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * تابلوی انتهای مسیر و کاپ قهرمانی
 */
@Composable
fun GoldenTrophySummitSign(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBEB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .border(2.5.dp, Color(0xFFF59E0B), RoundedCornerShape(22.dp))
            .padding(2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // روبان قرمز پایان مسیر
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFDC2626))
                    .padding(horizontal = 14.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "پایان مسیر طلایی",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "جام قهرمانی",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(46.dp)
            )

            Text(
                text = "اوج تخم‌گذاری و درآمد پایدار",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF78350F)
            )
            Text(
                text = "از هفته ۲۰ به بعد".toPersianDigits(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB45309)
            )
        }
    }
}

/**
 * تابلوی چوبی مراحل بعدی و فصول پیشرفته
 */
@Composable
fun NextChaptersWoodenBoard(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF6B4226)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .border(2.dp, Color(0xFFB8860B), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "مراحل بعدی شامل:",
                color = Color(0xFFFFE4B5),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val futureTopics = listOf(
                "مدیریت گله بالغ و جیره تخم‌گذاری" to Color(0xFFF59E0B),
                "تولید و جمع‌آوری تخم‌مرغ نطفه‌دار و خوراکی" to Color(0xFF0284C7),
                "واکسیناسیون پیشرفته و تست‌های آزمایشگاهی" to Color(0xFF10B981),
                "جوجه‌کشی مصنوعی و مدیریت دستگاه انکوباتور" to Color(0xFF8B5CF6),
                "بازاریابی، فروش و برندسازی تخم‌مرغ محلی" to Color(0xFFEC4899)
            )

            futureTopics.forEach { (topic, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = topic,
                        color = Color(0xFFFFF8DC),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
