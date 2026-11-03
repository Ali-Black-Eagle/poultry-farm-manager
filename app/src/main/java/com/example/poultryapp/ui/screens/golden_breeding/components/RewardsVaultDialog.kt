package com.example.poultryapp.ui.screens.golden_breeding.components

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import com.example.poultryapp.R
import com.example.poultryapp.data.model.BirdProgressSummary
import com.example.poultryapp.data.model.GoldenBirdType
import com.example.poultryapp.data.model.GoldenBreedingCatalog
import com.example.poultryapp.data.model.GoldenFarmReward
import com.example.poultryapp.data.model.resolveImageRes
import com.example.poultryapp.data.repository.GoldenBreedingRepository
import com.example.poultryapp.engine.toPersianDigits

/**
 * تالار افتخارات، جوایز و تجهیزات طلایی باز شده مزرعه
 */
@Composable
fun RewardsVaultDialog(
    birdType: GoldenBirdType,
    summary: BirdProgressSummary,
    repository: GoldenBreedingRepository,
    theme: BirdArtTheme,
    onDismiss: () -> Unit
) {
    val allRewards = remember(birdType) { GoldenBreedingCatalog.getRewardsForBird(birdType) }
    val maxUnlockedStage = summary.currentActiveStageNumber
    val isMaster = summary.completedStagesCount >= birdType.totalStages

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f)
                .padding(vertical = 12.dp)
                .border(2.dp, Color(0xFFFBBF24).copy(alpha = 0.5f), RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // هدر تالار افتخارات
                Surface(
                    color = Color(0xFF1E293B),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = Color(0xFF94A3B8))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "تالار جوایز و تجهیزات طلایی",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFBBF24)
                            )
                            Text(
                                text = "پرورش ${birdType.titleFa}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // نشان جوایز به دست آمده
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF78350F))
                                .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = "🏆", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${summary.unlockedRewardsCount.toPersianDigits()} / ${allRewards.size.toPersianDigits()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFEF08A)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // کارت بزرگ کاپ قهرمانی و وضعیت استادی
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    2.dp,
                                    Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFA855F7))),
                                    RoundedCornerShape(24.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = if (isMaster) R.drawable.game_trophy_master else R.drawable.game_medal_expert),
                                    contentDescription = "نشان استادی",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = summary.masteryTitleFa,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFDE047)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${summary.completedStagesCount.toPersianDigits()} از ${birdType.totalStages.toPersianDigits()} مرحله تکمیل شده",
                                        fontSize = 12.sp,
                                        color = Color(0xFFC7D2FE)
                                    )
                                    if (summary.maxPossibleSideQuestStars > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${summary.completedSideQuestsCount.toPersianDigits()} مأموریت بحران مهار شده",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "🛠️ تجهیزات طلایی باز شده مزرعه",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // لیست تمام جوایز
                    items(allRewards) { reward ->
                        val isUnlocked = reward.unlockStageNumber <= maxUnlockedStage

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .border(
                                    1.dp,
                                    if (isUnlocked) Color(0xFFFBBF24).copy(alpha = 0.6f) else Color(0xFF334155),
                                    RoundedCornerShape(18.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // پوستر ۲ بعدی هنری جایزه و تجهیزات
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A))
                                                .border(
                                                    1.5.dp,
                                                    if (isUnlocked) Color(0xFFFBBF24) else Color(0xFF475569),
                                                    RoundedCornerShape(14.dp)
                                                )
                                        ) {
                                            Image(
                                                painter = painterResource(id = reward.resolveImageRes()),
                                                contentDescription = reward.titleFa,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .alpha(if (isUnlocked) 1f else 0.35f)
                                            )
                                            if (!isUnlocked) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.5f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = "قفل",
                                                        tint = Color(0xFFCBD5E1),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${reward.iconEmoji} ${reward.titleFa}",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isUnlocked) Color.White else Color(0xFF94A3B8)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isUnlocked) reward.categoryFa else "آزادسازی در مرحله ${reward.unlockStageNumber.toPersianDigits()}",
                                                fontSize = 11.sp,
                                                color = if (isUnlocked) Color(0xFFF59E0B) else Color(0xFFEF4444)
                                            )
                                        }
                                    }

                                    if (isUnlocked) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF059669).copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(text = "فعال در فارم", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                        }
                                    }
                                }

                                if (isUnlocked) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = reward.descriptionFa,
                                        fontSize = 11.5.sp,
                                        lineHeight = 17.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "⚡ اثر فنی: ${reward.technicalImpactFa}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
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
