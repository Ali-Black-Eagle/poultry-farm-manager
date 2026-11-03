package com.example.poultryapp.ui.screens.golden_breeding.components

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.ui.components.AutoResizeText
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poultryapp.data.model.*
import com.example.poultryapp.data.repository.GoldenBreedingRepository
import com.example.poultryapp.engine.toPersianDigits
import java.util.Locale
import kotlin.math.roundToInt

private val persianLetters = listOf("الف", "ب", "ج", "د", "هـ", "و")

/**
 * مدل داخلی برای درهم‌آمیزی (Shuffle) رندوم گزینه‌ها و حفظ دقیق ایندکس گزینه صحیح
 */
private data class ShuffledChoice(
    val originalIndex: Int,
    val text: String
)

/**
 * دیالوگ چالش‌های تعاملی و کوئیزهای آموزشی بازی پرورش طلایی
 * نسخه فشرده و ارگونومیک: بدون فواصل اضافه، نمایش مستقیم گزینه‌ها در دید کاربر بدون نیاز به اسکرول
 */
@Composable
fun MultiChallengeDialog(
    stageTitleFa: String,
    avatarEmoji: String,
    challenges: List<GoldenChallenge>,
    theme: BirdArtTheme,
    isPro: Boolean = false,
    stageNumber: Int = 1,
    onCompleteStage: (earnedStars: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var challengeIndex by remember { mutableIntStateOf(0) }
    var mistakesCount by remember { mutableIntStateOf(0) }

    var selectedGlossaryTerm by remember { mutableStateOf<GlossaryTerm?>(null) }
    var showGlossaryBrowser by remember { mutableStateOf(false) }

    val currentChallenge = challenges.getOrNull(challengeIndex)

    if (currentChallenge == null) {
        val earnedStars = when (mistakesCount) {
            0 -> 3
            1 -> 2
            else -> 1
        }
        LaunchedEffect(Unit) {
            onCompleteStage(earnedStars)
        }
        return
    }

    fun handleMistake() {
        mistakesCount++
    }

    // گام خلاصه آموزشی
    val stepShortLabel = when {
        stageNumber in 1..3 -> "گام ۱: استقبال و برودر 🐣"
        stageNumber in 4..6 -> "گام ۲: استارتر و حرارت 💡"
        stageNumber in 7..9 -> "گام ۳: نور و واکسن 💨"
        stageNumber in 10..15 -> "گام ۴: رشد و پیش‌دان 🌾"
        stageNumber in 16..25 -> "گام ۵: سلامت و پاتولوژی 🩺"
        else -> "گام ۶: اوج تولید و تخم‌گذاری 🏆"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    // ۱. هدر یکپارچه تک‌سطری (بستن + پیشرفت + گام و واژه‌نامه)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // سمت راست (شروع در RTL): دکمه بستن
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(30.dp)
                                .clickable { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بستن",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // مرکز: شمارنده و نقطه‌های پیشرفت قطعه‌ای
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "چالش ${(challengeIndex + 1).toPersianDigits()} از ${challenges.size.toPersianDigits()}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                challenges.indices.forEach { idx ->
                                    val isPassed = idx < challengeIndex
                                    val isCurrent = idx == challengeIndex
                                    Box(
                                        modifier = Modifier
                                            .width(if (isCurrent) 16.dp else 7.dp)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                when {
                                                    isPassed -> Color(0xFF10B981)
                                                    isCurrent -> theme.primaryAccent
                                                    else -> Color(0xFFE2E8F0)
                                                }
                                            )
                                    )
                                }
                            }
                        }

                        // سمت چپ (پایان در RTL): نشانگر گام و میانبر واژه‌نامه
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!isPro) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, if (!isPro) Color(0xFFBBF7D0) else Color(0xFFFDE68A))
                            ) {
                                AutoResizeText(
                                    text = if (!isPro) stepShortLabel else "بورد تخصصی 👑",
                                    targetFontSize = 10.sp,
                                    minFontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isPro) Color(0xFF166534) else Color(0xFF92400E),
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0F9FF),
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                modifier = Modifier.clickable { showGlossaryBrowser = true }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = "واژه‌نامه",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ۲. بدنه اصلی چالش (سناریو / کوئیز / عیب‌یابی / اسلایدر)
                    key(challengeIndex) {
                        val isLastChallenge = challengeIndex == challenges.size - 1
                        when (currentChallenge) {
                            is GoldenChallenge.Scenario -> {
                                ScenarioItem(
                                    challenge = currentChallenge,
                                    theme = theme,
                                    isPro = isPro,
                                    isLastChallenge = isLastChallenge,
                                    onTermClick = { selectedGlossaryTerm = it },
                                    onAnswer = { isCorrect ->
                                        if (!isCorrect) handleMistake()
                                    },
                                    onNext = { challengeIndex++ }
                                )
                            }
                            is GoldenChallenge.SliderControl -> {
                                SliderItem(
                                    challenge = currentChallenge,
                                    theme = theme,
                                    isPro = isPro,
                                    isLastChallenge = isLastChallenge,
                                    onTermClick = { selectedGlossaryTerm = it },
                                    onSubmit = { isCorrect ->
                                        if (!isCorrect) handleMistake()
                                    },
                                    onNext = { challengeIndex++ }
                                )
                            }
                            is GoldenChallenge.QuickQuiz -> {
                                QuickQuizItem(
                                    challenge = currentChallenge,
                                    theme = theme,
                                    isPro = isPro,
                                    isLastChallenge = isLastChallenge,
                                    onTermClick = { selectedGlossaryTerm = it },
                                    onAnswer = { isCorrect ->
                                        if (!isCorrect) handleMistake()
                                    },
                                    onNext = { challengeIndex++ }
                                )
                            }
                            is GoldenChallenge.Diagnostics -> {
                                DiagnosticsItem(
                                    challenge = currentChallenge,
                                    theme = theme,
                                    isPro = isPro,
                                    isLastChallenge = isLastChallenge,
                                    onTermClick = { selectedGlossaryTerm = it },
                                    onAnswer = { isCorrect ->
                                        if (!isCorrect) handleMistake()
                                    },
                                    onNext = { challengeIndex++ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedGlossaryTerm?.let { term ->
        GlossaryTermDialog(term = term, onDismiss = { selectedGlossaryTerm = null })
    }

    if (showGlossaryBrowser) {
        GlossaryBrowserDialog(onDismiss = { showGlossaryBrowser = false })
    }
}

/**
 * سازگاری عقب‌رو برای فراخوانی با GoldenStage
 */
@Composable
fun MultiChallengeDialog(
    stage: GoldenStage,
    theme: BirdArtTheme,
    isPro: Boolean = false,
    onCompleteStage: (earnedStars: Int) -> Unit,
    onDismiss: () -> Unit
) {
    MultiChallengeDialog(
        stageTitleFa = stage.titleFa,
        avatarEmoji = stage.avatarEmoji,
        challenges = stage.challenges,
        theme = theme,
        isPro = isPro,
        stageNumber = stage.stageNumber,
        onCompleteStage = onCompleteStage,
        onDismiss = onDismiss
    )
}

/**
 * کامپوننت فشرده و ارگونومیک گزینه کوئیز با چیدمان استاندارد فارسی (RTL) و حروف الفبا
 */
@Composable
private fun QuizOptionCard(
    letterText: String,
    optionText: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    hasAnswered: Boolean,
    isPro: Boolean,
    primaryAccent: Color,
    onClick: () -> Unit
) {
    val showAsCorrect = hasAnswered && isCorrect && (!isPro || isSelected)
    val showAsWrong = hasAnswered && isSelected && !isCorrect
    val isDimmed = hasAnswered && !isSelected && !showAsCorrect

    val backgroundColor by animateColorAsState(
        targetValue = when {
            showAsCorrect -> Color(0xFFECFDF5)
            showAsWrong -> Color(0xFFFEF2F2)
            isSelected -> primaryAccent.copy(alpha = 0.08f)
            else -> Color.White
        },
        label = "bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            showAsCorrect -> Color(0xFF10B981)
            showAsWrong -> Color(0xFFEF4444)
            isSelected -> primaryAccent
            else -> Color(0xFFE2E8F0)
        },
        label = "border"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected || showAsCorrect) 1.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(if (showAsCorrect || showAsWrong) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !hasAnswered) { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .alpha(if (isDimmed) 0.5f else 1f)
        ) {
            // سمت راست (شروع در RTL): نشان دایره‌ای حروف الفبا
            Surface(
                shape = CircleShape,
                color = when {
                    showAsCorrect -> Color(0xFF10B981)
                    showAsWrong -> Color(0xFFEF4444)
                    isSelected -> primaryAccent
                    else -> Color(0xFFF1F5F9)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letterText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showAsCorrect || showAsWrong || isSelected) Color.White else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // مرکز: متن گزینه
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = optionText.toPersianDigits(),
                    fontSize = 12.5.sp,
                    fontWeight = if (showAsCorrect || isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        showAsCorrect -> Color(0xFF065F46)
                        showAsWrong -> Color(0xFF991B1B)
                        else -> Color(0xFF1E293B)
                    },
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Start
                )

                if (showAsCorrect && !isSelected && !isPro) {
                    Text(
                        text = "✅ پاسخ صحیح علمی",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // سمت چپ (پایان در RTL): آیکون وضعیت
            when {
                showAsCorrect -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "صحیح",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(19.dp)
                    )
                }
                showAsWrong -> {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "غلط",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(19.dp)
                    )
                }
                isSelected -> {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = "انتخاب‌شده",
                        tint = primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScenarioItem(
    challenge: GoldenChallenge.Scenario,
    theme: BirdArtTheme,
    isPro: Boolean,
    isLastChallenge: Boolean,
    onTermClick: (GlossaryTerm) -> Unit,
    onAnswer: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var hasAnswered by remember { mutableStateOf(false) }

    val shuffledChoices = remember(challenge) {
        challenge.optionsFa.mapIndexed { idx, txt -> ShuffledChoice(idx, txt) }.shuffled()
    }

    val challengeFullText = remember(challenge) {
        "${challenge.questionTitleFa} ${challenge.situationDescriptionFa} ${challenge.optionsFa.joinToString(" ")}"
    }
    val detectedTerms = remember(challengeFullText) {
        BreedingGlossary.findTermsInText(challengeFullText)
    }

    // کارت یکپارچه سناریو و سوال
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📋 سناریوی بحران و تصمیم مزرعه ${challenge.hintEmoji}".trim(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val fullScenarioText = if (challenge.situationDescriptionFa.isNotEmpty() && challenge.situationDescriptionFa != challenge.questionTitleFa) {
                "${challenge.questionTitleFa}: ${challenge.situationDescriptionFa}"
            } else {
                challenge.questionTitleFa
            }
            GlossaryAnnotatedText(
                text = fullScenarioText.toPersianDigits(),
                detectedTerms = detectedTerms,
                onTermClick = onTermClick,
                baseFontSize = 12.5f,
                baseColor = Color(0xFF0F172A),
                baseLineHeight = 20f
            )
        }
    }

    if (detectedTerms.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        GlossaryChipsRow(
            detectedTerms = detectedTerms,
            onTermClick = onTermClick
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    shuffledChoices.forEachIndexed { displayIndex, item ->
        val letter = persianLetters.getOrElse(displayIndex) { (displayIndex + 1).toPersianDigits() }
        val isChosen = selectedIndex == displayIndex
        val isCorrect = item.originalIndex == challenge.correctOptionIndex

        QuizOptionCard(
            letterText = letter,
            optionText = item.text,
            isSelected = isChosen,
            isCorrect = isCorrect,
            hasAnswered = hasAnswered,
            isPro = isPro,
            primaryAccent = theme.primaryAccent,
            onClick = {
                selectedIndex = displayIndex
                hasAnswered = true
                onAnswer(isCorrect)
            }
        )
    }

    if (hasAnswered) {
        val isUserCorrect = selectedIndex != null && shuffledChoices[selectedIndex!!].originalIndex == challenge.correctOptionIndex
        Spacer(modifier = Modifier.height(6.dp))
        ExamFeedbackBox(isCorrect = isUserCorrect, isPro = isPro, text = challenge.explanationFa)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.secondaryAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(
                text = if (isLastChallenge) "پایان مرحله و ثبت نتایج 🏆" else "چالش بعدی ➔",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun QuickQuizItem(
    challenge: GoldenChallenge.QuickQuiz,
    theme: BirdArtTheme,
    isPro: Boolean,
    isLastChallenge: Boolean,
    onTermClick: (GlossaryTerm) -> Unit,
    onAnswer: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var hasAnswered by remember { mutableStateOf(false) }

    val shuffledChoices = remember(challenge) {
        challenge.optionsFa.mapIndexed { idx, txt -> ShuffledChoice(idx, txt) }.shuffled()
    }

    val quizQuestion = challenge.questionFa.ifEmpty { challenge.situationDescriptionFa }
    val challengeFullText = remember(challenge) {
        "${challenge.questionTitleFa} $quizQuestion ${challenge.optionsFa.joinToString(" ")}"
    }
    val detectedTerms = remember(challengeFullText) {
        BreedingGlossary.findTermsInText(challengeFullText)
    }

    // کارت یکپارچه سوال
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "💡 سوال تخصصی پرورش",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D4ED8)
            )
            Spacer(modifier = Modifier.height(4.dp))
            val textToDisplay = if (quizQuestion.isNotEmpty() && quizQuestion != challenge.questionTitleFa) {
                "${challenge.questionTitleFa}: $quizQuestion"
            } else {
                challenge.questionTitleFa
            }
            GlossaryAnnotatedText(
                text = textToDisplay.toPersianDigits(),
                detectedTerms = detectedTerms,
                onTermClick = onTermClick,
                baseFontSize = 12.5f,
                baseColor = Color(0xFF0F172A),
                baseLineHeight = 20f
            )
        }
    }

    if (detectedTerms.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        GlossaryChipsRow(
            detectedTerms = detectedTerms,
            onTermClick = onTermClick
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    shuffledChoices.forEachIndexed { displayIndex, item ->
        val letter = persianLetters.getOrElse(displayIndex) { (displayIndex + 1).toPersianDigits() }
        val isChosen = selectedIndex == displayIndex
        val isCorrect = item.originalIndex == challenge.correctOptionIndex

        QuizOptionCard(
            letterText = letter,
            optionText = item.text,
            isSelected = isChosen,
            isCorrect = isCorrect,
            hasAnswered = hasAnswered,
            isPro = isPro,
            primaryAccent = theme.primaryAccent,
            onClick = {
                selectedIndex = displayIndex
                hasAnswered = true
                onAnswer(isCorrect)
            }
        )
    }

    if (hasAnswered) {
        val isUserCorrect = selectedIndex != null && shuffledChoices[selectedIndex!!].originalIndex == challenge.correctOptionIndex
        Spacer(modifier = Modifier.height(6.dp))
        ExamFeedbackBox(isCorrect = isUserCorrect, isPro = isPro, text = challenge.explanationFa)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.secondaryAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(
                text = if (isLastChallenge) "پایان مرحله و ثبت نتایج 🏆" else "چالش بعدی ➔",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun DiagnosticsItem(
    challenge: GoldenChallenge.Diagnostics,
    theme: BirdArtTheme,
    isPro: Boolean,
    isLastChallenge: Boolean,
    onTermClick: (GlossaryTerm) -> Unit,
    onAnswer: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var hasAnswered by remember { mutableStateOf(false) }

    val shuffledChoices = remember(challenge) {
        challenge.optionsFa.mapIndexed { idx, txt -> ShuffledChoice(idx, txt) }.shuffled()
    }

    val challengeFullText = remember(challenge) {
        "${challenge.questionTitleFa} ${challenge.symptomDescriptionFa} ${challenge.optionsFa.joinToString(" ")}"
    }
    val detectedTerms = remember(challengeFullText) {
        BreedingGlossary.findTermsInText(challengeFullText)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "🩺 عیب‌یابی بالینی و پاتولوژی ${challenge.clinicalSignEmoji}".trim(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(4.dp))
            val fullDiagText = if (challenge.symptomDescriptionFa.isNotEmpty() && challenge.symptomDescriptionFa != challenge.questionTitleFa) {
                "${challenge.questionTitleFa}: ${challenge.symptomDescriptionFa}"
            } else {
                challenge.questionTitleFa
            }
            GlossaryAnnotatedText(
                text = fullDiagText.toPersianDigits(),
                detectedTerms = detectedTerms,
                onTermClick = onTermClick,
                baseFontSize = 12.5f,
                baseColor = Color(0xFF0F172A),
                baseLineHeight = 20f
            )
        }
    }

    if (detectedTerms.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        GlossaryChipsRow(
            detectedTerms = detectedTerms,
            onTermClick = onTermClick
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    shuffledChoices.forEachIndexed { displayIndex, item ->
        val letter = persianLetters.getOrElse(displayIndex) { (displayIndex + 1).toPersianDigits() }
        val isChosen = selectedIndex == displayIndex
        val isCorrect = item.originalIndex == challenge.correctOptionIndex

        QuizOptionCard(
            letterText = letter,
            optionText = item.text,
            isSelected = isChosen,
            isCorrect = isCorrect,
            hasAnswered = hasAnswered,
            isPro = isPro,
            primaryAccent = theme.primaryAccent,
            onClick = {
                selectedIndex = displayIndex
                hasAnswered = true
                onAnswer(isCorrect)
            }
        )
    }

    if (hasAnswered) {
        val isUserCorrect = selectedIndex != null && shuffledChoices[selectedIndex!!].originalIndex == challenge.correctOptionIndex
        Spacer(modifier = Modifier.height(6.dp))
        ExamFeedbackBox(isCorrect = isUserCorrect, isPro = isPro, text = challenge.explanationFa)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.secondaryAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(
                text = if (isLastChallenge) "پایان مرحله و ثبت نتایج 🏆" else "چالش بعدی ➔",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * اسلایدر تنظیم پارامترهای سالن (دما، رطوبت، تهویه و...) با طراحی فشرده
 */
@Composable
private fun SliderItem(
    challenge: GoldenChallenge.SliderControl,
    theme: BirdArtTheme,
    isPro: Boolean,
    isLastChallenge: Boolean,
    onTermClick: (GlossaryTerm) -> Unit,
    onSubmit: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { GoldenBreedingRepository(context) }
    val hasIoTSensor = remember { repository.isFacilityOwned(GoldenBreedingRepository.FACILITY_IOT_SENSORS) }

    var sliderValue by remember { mutableFloatStateOf(challenge.initialValue) }
    var hasSubmitted by remember { mutableStateOf(false) }

    val challengeFullText = remember(challenge) {
        "${challenge.questionTitleFa} ${challenge.parameterNameFa} ${challenge.explanationFa}"
    }
    val detectedTerms = remember(challengeFullText) {
        BreedingGlossary.findTermsInText(challengeFullText)
    }

    val isDecimal = (challenge.maxValue - challenge.minValue <= 15f) ||
            (challenge.optimalMin % 1.0f != 0.0f) ||
            (challenge.optimalMax % 1.0f != 0.0f)

    val displayedUserVal = if (isDecimal) {
        String.format(Locale.US, "%.1f", (sliderValue * 10).roundToInt() / 10.0f)
    } else {
        sliderValue.roundToInt().toString()
    }

    val numericVal = displayedUserVal.toFloatOrNull() ?: sliderValue
    val isOptimal = numericVal in (challenge.optimalMin - 0.05f)..(challenge.optimalMax + 0.05f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasIoTSensor) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "📡 سنسور هوشمند فعال (کالیبراسیون بازه طلایی)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Text(
                text = "⚙️ ${challenge.questionTitleFa}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF166534),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Text(
                text = "$displayedUserVal ${challenge.unitFa}".toPersianDigits(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = if (hasSubmitted && isOptimal) Color(0xFF16A34A) else if (hasSubmitted) Color(0xFFDC2626) else theme.primaryAccent,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Slider(
                value = sliderValue,
                onValueChange = { if (!hasSubmitted) sliderValue = it },
                valueRange = challenge.minValue..challenge.maxValue,
                enabled = !hasSubmitted,
                colors = SliderDefaults.colors(
                    thumbColor = theme.primaryAccent,
                    activeTrackColor = theme.secondaryAccent
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (isDecimal) String.format(Locale.US, "%.1f %s", challenge.minValue, challenge.unitFa).toPersianDigits()
                    else "${challenge.minValue.toInt()} ${challenge.unitFa}".toPersianDigits(),
                    fontSize = 10.5.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = if (isDecimal) String.format(Locale.US, "%.1f %s", challenge.maxValue, challenge.unitFa).toPersianDigits()
                    else "${challenge.maxValue.toInt()} ${challenge.unitFa}".toPersianDigits(),
                    fontSize = 10.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }

    if (detectedTerms.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        GlossaryChipsRow(
            detectedTerms = detectedTerms,
            onTermClick = onTermClick
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (!hasSubmitted) {
        Button(
            onClick = {
                hasSubmitted = true
                onSubmit(isOptimal)
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(text = "بررسی و ثبت تنظیمات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    } else {
        val feedbackText = if (isOptimal) {
            "تنظیمات شما کاملاً دقیق و در محدوده بهینه فیزیولوژیک گله قرار گرفت.\n${challenge.explanationFa}"
        } else if (!isPro) {
            "تنظیمات ثبت‌شده خارج از محدوده استاندارد است! محدوده صحیح و بهینه: ${challenge.optimalMin} تا ${challenge.optimalMax} ${challenge.unitFa} می‌باشد.\n${challenge.explanationFa}"
        } else {
            "تنظیمات ثبت‌شده خارج از محدوده استاندارد است! در آزمون بورد فوق‌تخصصی، برای سنجش دقیق مهارت، بازه دقیق در حالت خطا لو داده نمی‌شود. لطفاً شرایط زیستی گله را مجدداً ارزیابی کنید."
        }

        ExamFeedbackBox(
            isCorrect = isOptimal,
            isPro = isPro,
            text = feedbackText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.secondaryAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(
                text = if (isLastChallenge) "پایان مرحله و ثبت نتایج 🏆" else "چالش بعدی ➔",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * باکس فیدبک پایان سوال با طراحی فشرده
 */
@Composable
private fun ExamFeedbackBox(isCorrect: Boolean, isPro: Boolean, text: String) {
    val containerColor = if (isCorrect) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
    val borderColor = if (isCorrect) Color(0xFFBBF7D0) else Color(0xFFFECACA)
    val titleText = if (isCorrect) {
        "تحلیل علمی (پاسخ کاملاً صحیح است):"
    } else if (!isPro) {
        "پاسخ نادرست — نکته آموزشی:"
    } else {
        "پاسخ نادرست — ارزیابی بورد تخصصی:"
    }
    val titleColor = if (isCorrect) Color(0xFF15803D) else Color(0xFFB91C1C)
    val contentColor = if (isCorrect) Color(0xFF166534) else Color(0xFF7F1D1D)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = titleText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (!isCorrect && isPro) {
                    "در آزمون بورد فوق‌تخصصی، جهت سنجش واقعی تخصص بالینی، پاسخ صحیح لو داده نمی‌شود. لطفاً شرایط فیزیولوژیک گله را مجدداً ارزیابی کرده و در تکرار مرحله با تحلیل دقیق‌تر پاسخ دهید."
                } else {
                    text.toPersianDigits()
                },
                fontSize = 11.5.sp,
                lineHeight = 18.sp,
                color = contentColor
            )
        }
    }
}
