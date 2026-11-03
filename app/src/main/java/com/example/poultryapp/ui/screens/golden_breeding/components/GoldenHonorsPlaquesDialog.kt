package com.example.poultryapp.ui.screens.golden_breeding.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poultryapp.R
import com.example.poultryapp.data.model.GoldenPlaque
import com.example.poultryapp.data.repository.GoldenBreedingRepository
import com.example.poultryapp.engine.toPersianDigits

/**
 * دیالوگ سلطنتی تالار افتخارات و لوح‌های زرین بورد استادی
 */
@Composable
fun GoldenHonorsPlaquesDialog(
    repository: GoldenBreedingRepository,
    onDismiss: () -> Unit
) {
    var plaques by remember { mutableStateOf(repository.getAllPlaques()) }
    var selectedPlaqueForCertificate by remember { mutableStateOf<GoldenPlaque?>(null) }

    val unlockedCount = plaques.count { it.isUnlocked }
    val grandMasterPlaque = plaques.find { it.id == "plaque_grand_master" }
    val birdPlaques = plaques.filter { it.id != "plaque_grand_master" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            )
                        )
                    )
            ) {
                // هدر باشکوه با دکمه بستن
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF78350F),
                                    Color(0xFFB45309),
                                    Color(0xFF78350F)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "👑", fontSize = 22.sp)
                            Text(
                                text = "تالار افتخارات و لوح‌های زرین",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFEF08A)
                            )
                        }
                        Text(
                            text = "${unlockedCount.toPersianDigits()} از ${plaques.size.toPersianDigits()} لوح و دانشنامه رسمی کسب شده",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // کارت بزرگ دانشنامه استاد اعظم (Grand Master)
                    if (grandMasterPlaque != null) {
                        item {
                            GrandMasterPlaqueBanner(
                                plaque = grandMasterPlaque,
                                onClick = {
                                    if (grandMasterPlaque.isUnlocked) {
                                        selectedPlaqueForCertificate = grandMasterPlaque
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Text(
                            text = "لوح‌های استادی ${birdPlaques.size.toPersianDigits()} گونه پرنده (بورد فوق‌حرفه‌ای)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // لیست ۹ لوح زرین اختصاصی گونه‌ها
                    items(birdPlaques) { plaque ->
                        PlaqueItemCard(
                            plaque = plaque,
                            onClick = {
                                if (plaque.isUnlocked) {
                                    selectedPlaqueForCertificate = plaque
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    // دیالوگ نمایش گواهینامه و دانشنامه رسمی هنگام کلیک روی لوح باز شده
    selectedPlaqueForCertificate?.let { plaque ->
        PlaqueCertificateDialog(
            plaque = plaque,
            onDismiss = { selectedPlaqueForCertificate = null }
        )
    }
}

/**
 * بنر مجلل دانشنامه استاد اعظم در صدر تالار افتخارات
 */
@Composable
private fun GrandMasterPlaqueBanner(
    plaque: GoldenPlaque,
    onClick: () -> Unit
) {
    val isUnlocked = plaque.isUnlocked
    val borderBrush = if (isUnlocked) {
        Brush.linearGradient(
            listOf(
                Color(0xFFFEF08A),
                Color(0xFFF59E0B),
                Color(0xFFD97706),
                Color(0xFFFEF08A)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFF475569),
                Color(0xFF334155),
                Color(0xFF475569)
            )
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isUnlocked) 12.dp else 2.dp, RoundedCornerShape(20.dp))
            .clickable(enabled = isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, borderBrush),
        color = if (isUnlocked) Color(0xFF1E1B4B) else Color(0xFF1E293B).copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // نماد کاپ استاد اعظم
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isUnlocked) Brush.radialGradient(
                            listOf(Color(0xFFFBBF24), Color(0xFFB45309))
                        ) else Brush.radialGradient(
                            listOf(Color(0xFF64748B), Color(0xFF334155))
                        )
                    )
                    .border(
                        1.5.dp,
                        if (isUnlocked) Color(0xFFFEF08A) else Color(0xFF475569),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = plaque.imageRes),
                    contentDescription = plaque.titleFa,
                    modifier = Modifier.size(54.dp),
                    contentScale = ContentScale.Fit
                )
                if (!isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "قفل",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = plaque.titleFa,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) Color(0xFFFEF08A) else Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plaque.specialtyFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUnlocked) Color(0xFFE2E8F0) else Color(0xFF64748B),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                if (isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = "✨ مفتخر به نشان استاد اعظم (لمس برای مشاهده دانشنامه)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFEF08A),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Text(
                        text = "شرط اعطا: " + plaque.criteriaFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * کارت لوح زرین برای هر گونه پرنده
 */
@Composable
private fun PlaqueItemCard(
    plaque: GoldenPlaque,
    onClick: () -> Unit
) {
    val isUnlocked = plaque.isUnlocked

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isUnlocked) 1.5.dp else 1.dp,
            color = if (isUnlocked) Color(0xFFF59E0B) else Color(0xFF334155)
        ),
        color = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // تصویر پرنده و مدال
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isUnlocked) Brush.radialGradient(
                            listOf(Color(0xFF78350F), Color(0xFF1E293B))
                        ) else Brush.radialGradient(
                            listOf(Color(0xFF334155), Color(0xFF0F172A))
                        )
                    )
                    .border(
                        1.dp,
                        if (isUnlocked) Color(0xFFFBBF24) else Color(0xFF475569),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = plaque.imageRes),
                    contentDescription = plaque.titleFa,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
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
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = plaque.badgeEmoji, fontSize = 16.sp)
                    Text(
                        text = plaque.titleFa,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) Color(0xFFFEF08A) else Color(0xFF94A3B8),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = plaque.specialtyFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUnlocked) Color(0xFFCBD5E1) else Color(0xFF64748B),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                if (isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF065F46).copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = "🏅 کسب شده (${plaque.unlockedDateFa ?: "رسمی"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = "لمس برای گواهینامه 📜",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24),
                            fontSize = 9.sp
                        )
                    }
                } else {
                    Text(
                        text = "شرط: " + plaque.criteriaFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontSize = 9.5.sp
                    )
                }
            }
        }
    }
}

/**
 * دانشنامه رسمی و شکوهمند بورد استادی با تم گواهی طلایی، مهر موم برجسته و امضا
 */
@Composable
fun PlaqueCertificateDialog(
    plaque: GoldenPlaque,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFEFCE8),
            shadowElevation = 24.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFFBEB),
                                Color(0xFFFEF3C7),
                                Color(0xFFFDE68A)
                            )
                        )
                    )
                    // کادر تذهیب طلایی دو لایه
                    .border(
                        BorderStroke(
                            3.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFD97706),
                                    Color(0xFFF59E0B),
                                    Color(0xFF78350F),
                                    Color(0xFFF59E0B)
                                )
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(4.dp)
                    .border(1.dp, Color(0xFFB45309).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // دکمه بستن گوشه بالا
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "شماره دانشنامه: AGY-VET-${(plaque.id.hashCode() and 0xFFFF).toString().padStart(5, '0')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF78350F),
                            fontSize = 9.sp
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = Color(0xFF78350F)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // عنوان سربرگ دانشنامه
                    Text(
                        text = "آکادمی جامع پرورش طیور و علوم پرندگان",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "گواهینامه رسمی اعطای بورد استادی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF78350F),
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // نشان مدال طلایی مرکزی
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFEF08A),
                                        Color(0xFFF59E0B),
                                        Color(0xFFB45309)
                                    )
                                )
                            )
                            .border(2.dp, Color(0xFF78350F), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = plaque.imageRes),
                            contentDescription = plaque.titleFa,
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // نام لوح
                    Text(
                        text = plaque.titleFa,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = plaque.specialtyFa,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB45309),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // متن اعطا
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Text(
                            text = plaque.descriptionFa,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF451A03),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 19.sp,
                            fontSize = 11.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // فوتر و مهر رسمی
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // مهر موم قرمز آکادمی
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFFEF4444),
                                            Color(0xFF991B1B),
                                            Color(0xFF7F1D1D)
                                        )
                                    )
                                )
                                .border(1.5.dp, Color(0xFFFEF08A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "مهر رسمی", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(text = "★ بورد ★", fontSize = 8.sp, color = Color(0xFFFEF08A))
                            }
                        }

                        // امضای آکادمی و تاریخ
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "شورای عالی علمی آکادمی",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "تاریخ صدور: ${plaque.unlockedDateFa ?: "۱۴۰۳/۰۸"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "اعتبار: دائمی و بین‌المللی",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF16A34A),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF78350F)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "تأیید و بازگشت به تالار",
                            color = Color(0xFFFEF08A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
