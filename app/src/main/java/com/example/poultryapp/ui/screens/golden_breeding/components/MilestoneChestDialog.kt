package com.example.poultryapp.ui.screens.golden_breeding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poultryapp.R
import com.example.poultryapp.data.model.GoldenFarmReward
import com.example.poultryapp.data.model.resolveImageRes
import com.example.poultryapp.engine.toPersianDigits

/**
 * دیالوگ سینمایی باز شدن صندوقچه گنج طلایی در نقاط عطف مراحل بازی
 */
@Composable
fun MilestoneChestDialog(
    reward: GoldenFarmReward,
    onClaim: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chest_glow")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chest_scale"
    )

    Dialog(
        onDismissRequest = onClaim,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // کانفتی پس‌زمینه
            ConfettiCelebrationCanvas()

            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(
                        2.5.dp,
                        Brush.linearGradient(
                            listOf(Color(0xFFFDE047), Color(0xFFD97706), Color(0xFFFDE047))
                        ),
                        RoundedCornerShape(32.dp)
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(22.dp)
                ) {
                    // بج هدر
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF78350F))
                            .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🎁 پاداش استثنایی نقطه عطف مرحله ${reward.unlockStageNumber.toPersianDigits()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFEF08A)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // تصویر صندوقچه طلایی با انیمیشن تپنده
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(150.dp)
                            .scale(scaleAnim)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.game_chest_gold),
                            contentDescription = "صندوقچه طلایی",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "صندوقچه طلایی گشوده شد!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "ابزار پیشرفته مزرعه به گنجه شما اضافه گردید",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // کارت تجهیزات آزاد شده با پوستر ۲ بعدی بازی
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // تصویر هنری ۲ بعدی جایزه
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.5.dp, Color(0xFFFBBF24), RoundedCornerShape(14.dp))
                                ) {
                                    Image(
                                        painter = painterResource(id = reward.resolveImageRes()),
                                        contentDescription = reward.titleFa,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${reward.iconEmoji} ${reward.titleFa}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = reward.categoryFa,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = reward.descriptionFa,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFFCBD5E1)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // اثر فنی در مزرعه
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "⚡ مزیت در فارم: ${reward.technicalImpactFa}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // نشان افتخار ثبت شده در تالار جوایز
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFD97706).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(text = "🏆", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "این نشان با موفقیت در تالار جوایز ثبت گردید",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE047)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // دکمه دریافت
                    Button(
                        onClick = onClaim,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "دریافت جایزه و ادامه ماجراجویی ✨",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E1B4B)
                        )
                    }
                }
            }
        }
    }
}
