package com.example.poultryapp.ui.screens.golden_breeding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.ui.components.AutoResizeText
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poultryapp.data.model.GoldenBirdType
import com.example.poultryapp.data.model.GoldenBreedingCatalog
import com.example.poultryapp.data.model.GoldenSideQuest
import com.example.poultryapp.data.repository.GoldenBreedingRepository
import com.example.poultryapp.engine.toPersianDigits

/**
 * باتم شیت / پنجره مأموریت‌های جانبی و اضطراری (ساید کوئست‌ها) برای هر پرنده
 */
@Composable
fun SideQuestSheet(
    birdType: GoldenBirdType,
    currentStageNumber: Int,
    repository: GoldenBreedingRepository,
    theme: BirdArtTheme,
    onLaunchQuest: (GoldenSideQuest) -> Unit,
    onDismiss: () -> Unit
) {
    val sideQuests = remember(birdType) { GoldenBreedingCatalog.getSideQuestsForBird(birdType) }
    val totalSideQuestStars = remember(birdType, sideQuests) {
        sideQuests.sumOf { repository.getSideQuestStars(birdType, it.id) }
    }
    val maxSideQuestStars = sideQuests.size * 3
    val completedQuestsCount = remember(birdType, sideQuests) {
        sideQuests.count { repository.isSideQuestCompleted(birdType, it.id) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
                .border(2.dp, Color(0xFFDC2626).copy(alpha = 0.6f), RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // هدر مأموریت‌های جانبی
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
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = Color(0xFF94A3B8))
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🚨", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مأموریت‌های جانبی و اضطراری",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF87171)
                                )
                            }
                            AutoResizeText(
                                text = "چالش‌های شیوع بیماری، بحران سالن و هچری ${birdType.titleFa}",
                                targetFontSize = 11.sp,
                                minFontSize = 8.5.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }

                        Text(text = birdType.avatarEmoji, fontSize = 24.sp, modifier = Modifier.padding(end = 4.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    item {
                        // بنر خلاصه ستاره‌های زمردی و پیشرفت مراحل فرعی
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.85f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.2.dp, Color(0xFF10B981), RoundedCornerShape(18.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "ستاره‌های مهار بحران",
                                            fontSize = 11.sp,
                                            color = Color(0xFFA7F3D0)
                                        )
                                        Text(
                                            text = "${totalSideQuestStars.toPersianDigits()} ستاره کسب‌شده (محاسبه در کل)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFECFDF5)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF047857),
                                    modifier = Modifier.defaultMinSize(minWidth = 75.dp)
                                ) {
                                    Text(
                                        text = "${completedQuestsCount.toPersianDigits()} / ${sideQuests.size.toPersianDigits()} مأموریت",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        softWrap = false,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF991B1B), RoundedCornerShape(16.dp))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚠️", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ساید کوئست‌ها شامل سناریوهای بحرانی و پرریسک مرغداری هستند. با حل هر مأموریت نشان افتخار و سکه‌های طلایی پاداش بگیرید!",
                                    fontSize = 11.5.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFFFECACA)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    items(sideQuests) { quest ->
                        val isCompleted = repository.isSideQuestCompleted(birdType, quest.id)
                        val starsEarned = repository.getSideQuestStars(birdType, quest.id)
                        val isUnlocked = currentStageNumber >= quest.unlockStageRequired

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompleted) Color(0xFF064E3B)
                                else if (isUnlocked) Color(0xFF1E293B)
                                else Color(0xFF0F172A).copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(enabled = isUnlocked || isCompleted) { onLaunchQuest(quest) }
                                .border(
                                    1.5.dp,
                                    if (isCompleted) Color(0xFF10B981)
                                    else if (isUnlocked) Color(quest.category.colorHex)
                                    else Color(0xFF334155),
                                    RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // دسته‌بندی
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(quest.category.colorHex).copy(alpha = 0.25f))
                                            .border(1.dp, Color(quest.category.colorHex), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${quest.category.badgeEmoji} ${quest.category.titleFa}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    // نشان پاداش
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🪙 +${quest.rewardCoins.toPersianDigits()}", fontSize = 11.sp, color = Color(0xFFFBBF24))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = quest.titleFa,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isUnlocked || isCompleted) Color.White else Color(0xFF64748B)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "🎯 ${quest.targetAgeFa}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF59E0B)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = quest.emergencyScenarioFa,
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.sp,
                                    color = if (isUnlocked || isCompleted) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // سطر ۱: نشان افتخار
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = quest.rewardBadgeIcon, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    AutoResizeText(
                                        text = "نشان پاداش: ${quest.rewardBadgeTitleFa}",
                                        targetFontSize = 11.5.sp,
                                        minFontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF93C5FD),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // سطر ۲: دکمه‌ها و ستاره‌ها (با حداقل عرض و بدون امکان فشرده‌شدن)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isCompleted) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                repeat(3) { idx ->
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (idx < starsEarned) Color(0xFF10B981) else Color(0xFF064E3B),
                                                        modifier = Modifier.size(16.dp).padding(horizontal = 0.5.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF065F46))
                                                    .border(0.8.dp, Color(0xFF34D399), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "مهار شد (${starsEarned.toPersianDigits()} ★ سبز) ✅",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFA7F3D0),
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onLaunchQuest(quest) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 90.dp)
                                                .height(36.dp)
                                        ) {
                                            Text(text = "تکرار 🔄", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White, softWrap = false)
                                        }
                                    } else if (isUnlocked) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(text = "وضعیت: آماده عملیات 🚨", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onLaunchQuest(quest) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 120.dp)
                                                .height(38.dp)
                                        ) {
                                            Text(text = "ورود به عملیات ➔", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White, softWrap = false)
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "نیازمند رسیدن به مرحله ${quest.unlockStageRequired.toPersianDigits()}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
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
    }
}
