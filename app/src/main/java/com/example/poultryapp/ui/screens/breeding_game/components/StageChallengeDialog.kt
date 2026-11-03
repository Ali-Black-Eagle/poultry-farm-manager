package com.example.poultryapp.ui.screens.breeding_game.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poultryapp.data.model.*
import com.example.poultryapp.engine.toPersianDigits

/**
 * پنجره معرفی مرحله و شروع ماموریت (Stage Intro Modal)
 */
@Composable
fun StageIntroDialog(
    stage: GameStage,
    progress: StageProgress,
    onStartChallenge: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .border(2.5.dp, Color(0xFFF59E0B), RoundedCornerShape(24.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // هدر سن و نماد پرنده
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
                            )
                        )
                        .border(3.dp, Color(0xFFF59E0B), CircleShape)
                ) {
                    Text(
                        text = stage.growthStage.iconEmoji,
                        fontSize = 38.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "مرحله ${stage.stageNumber.toPersianDigits()}: ${stage.titleFa}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${stage.ageLabelFa} (${stage.daysRangeFa})".toPersianDigits(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // کادر خلاصه ماموریت
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎯", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ماموریت و هدف یادگیری:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stage.missionBriefFa.toPersianDigits(),
                            fontSize = 12.5.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // نمایش ستاره‌های قبلی
                if (progress.isCompleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "بهترین امتیاز شما: ",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        repeat(3) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < progress.stars) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // دکمه‌های اکشن
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "بستن", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onStartChallenge,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(
                            text = "شروع چالش و بازی ➔",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * صفحه تعاملی چالش‌های درون مرحله (Gameplay Experience)
 */
@Composable
fun StageChallengeDialog(
    stage: GameStage,
    onCompleteStage: (earnedStars: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var challengeIndex by remember { mutableIntStateOf(0) }
    var mistakesCount by remember { mutableIntStateOf(0) }

    val currentChallenge = stage.challenges.getOrNull(challengeIndex)

    if (currentChallenge == null) {
        // پایان تمام چالش‌ها
        val stars = when (mistakesCount) {
            0 -> 3
            1 -> 2
            else -> 1
        }
        LaunchedEffect(Unit) {
            onCompleteStage(stars)
        }
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // نوار بالای چالش
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "چالش ${(challengeIndex + 1).toPersianDigits()} از ${stage.challenges.size.toPersianDigits()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }

                    Text(
                        text = stage.growthStage.iconEmoji,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // عنوان چالش
                Text(
                    text = currentChallenge.questionTitleFa,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // اجرای محتوای چالش بر اساس نوع
                when (currentChallenge) {
                    is StageChallenge.Scenario -> {
                        ScenarioChallengeView(
                            challenge = currentChallenge,
                            onOptionSelected = { isCorrect ->
                                if (!isCorrect) mistakesCount++
                            },
                            onNext = { challengeIndex++ }
                        )
                    }
                    is StageChallenge.SliderControl -> {
                        SliderChallengeView(
                            challenge = currentChallenge,
                            onSubmit = { isCorrect ->
                                if (!isCorrect) mistakesCount++
                            },
                            onNext = { challengeIndex++ }
                        )
                    }
                    is StageChallenge.QuickQuiz -> {
                        QuickQuizChallengeView(
                            challenge = currentChallenge,
                            onOptionSelected = { isCorrect ->
                                if (!isCorrect) mistakesCount++
                            },
                            onNext = { challengeIndex++ }
                        )
                    }
                }
            }
        }
    }
}

/**
 * نمای چالش سناریوی رفتارشناسی
 */
@Composable
private fun ScenarioChallengeView(
    challenge: StageChallenge.Scenario,
    onOptionSelected: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var hasAnswered by remember { mutableStateOf(false) }

    // شافل رندوم گزینه‌ها در هر باز شدن سناریو
    val shuffledChoices = remember(challenge) {
        challenge.optionsFa.mapIndexed { idx, txt -> ShuffledGameOption(idx, txt) }.shuffled()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Text(text = challenge.visualHintEmoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = challenge.situationDescriptionFa.toPersianDigits(),
                fontSize = 13.5.sp,
                lineHeight = 22.sp,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Medium
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "اقدام صحیح و راه‌حل چیست؟",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF475569)
    )

    Spacer(modifier = Modifier.height(8.dp))

    shuffledChoices.forEachIndexed { displayIndex, item ->
        val isChosen = selectedOptionIndex == displayIndex
        val isCorrect = item.originalIndex == challenge.correctOptionIndex

        val cardBg = when {
            !hasAnswered -> if (isChosen) Color(0xFFE2E8F0) else Color.White
            isCorrect -> Color(0xFFDCFCE7)
            isChosen && !isCorrect -> Color(0xFFFEE2E2)
            else -> Color.White
        }

        val borderClr = when {
            !hasAnswered -> if (isChosen) Color(0xFF2563EB) else Color(0xFFE2E8F0)
            isCorrect -> Color(0xFF16A34A)
            isChosen && !isCorrect -> Color(0xFFDC2626)
            else -> Color(0xFFE2E8F0)
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(1.5.dp, borderClr, RoundedCornerShape(14.dp))
                .clickable(enabled = !hasAnswered) {
                    selectedOptionIndex = displayIndex
                    hasAnswered = true
                    onOptionSelected(isCorrect)
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                if (hasAnswered) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else if (isChosen) Icons.Default.Cancel else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isCorrect) Color(0xFF16A34A) else if (isChosen) Color(0xFFDC2626) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isChosen) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isChosen) Color(0xFF2563EB) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.text.toPersianDigits(),
                    fontSize = 12.5.sp,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (hasAnswered) {
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "نکته علمی و دلیل:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = challenge.explanationFa.toPersianDigits(),
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF1E3A8A)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "ادامه چالش بعدی ➔", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/**
 * نمای چالش اسلایدر تنظیم ابزار سالن
 */
@Composable
private fun SliderChallengeView(
    challenge: StageChallenge.SliderControl,
    onSubmit: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(challenge.initialValue) }
    var hasSubmitted by remember { mutableStateOf(false) }

    val isOptimal = sliderValue in challenge.optimalMin..challenge.optimalMax

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = challenge.parameterNameFa,
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "${sliderValue.toInt()} ${challenge.unitFa}".toPersianDigits(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = if (hasSubmitted && isOptimal) Color(0xFF16A34A) else if (hasSubmitted) Color(0xFFDC2626) else Color(0xFF0F172A),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Slider(
                value = sliderValue,
                onValueChange = { if (!hasSubmitted) sliderValue = it },
                valueRange = challenge.minValue..challenge.maxValue,
                enabled = !hasSubmitted,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF16A34A),
                    activeTrackColor = Color(0xFF22C55E)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${challenge.minValue.toInt()} ${challenge.unitFa}".toPersianDigits(),
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "${challenge.maxValue.toInt()} ${challenge.unitFa}".toPersianDigits(),
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (!hasSubmitted) {
        Button(
            onClick = {
                hasSubmitted = true
                onSubmit(isOptimal)
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "بررسی و ثبت تنظیمات", fontWeight = FontWeight.Bold, color = Color.White)
        }
    } else {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isOptimal) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isOptimal) "آفرین! تنظیمات کاملاً دقیق بود ✅" else "دقت کنید! بازه ایده‌آل ${challenge.optimalMin.toInt()} تا ${challenge.optimalMax.toInt()} ${challenge.unitFa} است ⚠️".toPersianDigits(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOptimal) Color(0xFF14532D) else Color(0xFF7F1D1D)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = challenge.explanationFa.toPersianDigits(),
                    fontSize = 12.sp,
                    color = if (isOptimal) Color(0xFF166534) else Color(0xFF991B1B)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "ادامه چالش بعدی ➔", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/**
 * نمای چالش کوئیز سرعتی
 */
@Composable
private fun QuickQuizChallengeView(
    challenge: StageChallenge.QuickQuiz,
    onOptionSelected: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var hasAnswered by remember { mutableStateOf(false) }

    // شافل رندوم گزینه‌ها در هر باز شدن کوییز
    val shuffledChoices = remember(challenge) {
        challenge.optionsFa.mapIndexed { idx, txt -> ShuffledGameOption(idx, txt) }.shuffled()
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = challenge.questionFa.toPersianDigits(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            color = Color(0xFF0F172A)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    shuffledChoices.forEachIndexed { displayIndex, item ->
        val isChosen = selectedOptionIndex == displayIndex
        val isCorrect = item.originalIndex == challenge.correctOptionIndex

        val cardBg = when {
            !hasAnswered -> Color.White
            isCorrect -> Color(0xFFDCFCE7)
            isChosen && !isCorrect -> Color(0xFFFEE2E2)
            else -> Color.White
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(
                    1.dp,
                    if (hasAnswered && isCorrect) Color(0xFF16A34A) else Color(0xFFE2E8F0),
                    RoundedCornerShape(12.dp)
                )
                .clickable(enabled = !hasAnswered) {
                    selectedOptionIndex = displayIndex
                    hasAnswered = true
                    onOptionSelected(isCorrect)
                }
        ) {
            Text(
                text = item.text.toPersianDigits(),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    if (hasAnswered) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = challenge.explanationFa.toPersianDigits(),
            fontSize = 12.sp,
            color = Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "پایان مرحله و دریافت نتایج ➔", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/**
 * پنجره جشن پیروزی، اعطای ستاره‌ها و آنلاک کارت طلایی دانش
 */
@Composable
fun StageVictoryDialog(
    stage: GameStage,
    earnedStars: Int,
    onContinueNext: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(14.dp)
                .border(3.dp, Color(0xFFF59E0B), RoundedCornerShape(26.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "🎉 تبریک! مرحله با موفقیت پاس شد 🎉", fontSize = 14.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF78350F))

                Spacer(modifier = Modifier.height(10.dp))

                // نمایش ستاره‌های کسب شده
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val filled = index < earnedStars
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (filled) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                            modifier = Modifier
                                .size(if (index == 1) 48.dp else 38.dp)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }

                Text(
                    text = "${earnedStars.toPersianDigits()} ستاره دریافت شد!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309),
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // کارت طلایی دانش آنلاک شده
                GoldenCardPreview(card = stage.goldenCard)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onContinueNext,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "دریافت کارت طلایی و بازگشت به نقشه ➔",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * پیش‌نمایش کارت طلایی دانش
 */
@Composable
fun GoldenCardPreview(card: KnowledgeCard, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category & Badge Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = "🏆 ${card.categoryFa}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = "کارت طلایی دانش",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = card.titleFa,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                lineHeight = 24.sp
            )

            if (card.subtitleFa.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.subtitleFa,
                    fontSize = 13.5.sp,
                    color = Color(0xFF475569),
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            HorizontalDivider(
                color = Color(0xFFFDE68A),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            card.effectiveRules.forEach { rule ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "•",
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rule.toPersianDigits(),
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            if (card.practicalFormulaFa != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = card.practicalFormulaFa.toPersianDigits(),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F),
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            val tipText = card.effectiveProTip.ifBlank { card.proTipFa }
            if (tipText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 نکته طلایی:\n$tipText".toPersianDigits(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF065F46),
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * مدل جابجایی رندوم گزینه‌ها برای جلوگیری از موقعیت ثابت پاسخ
 */
private data class ShuffledGameOption(
    val originalIndex: Int,
    val text: String
)

