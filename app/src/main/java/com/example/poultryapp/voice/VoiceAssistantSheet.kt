package com.example.poultryapp.voice

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.WindowManager
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.poultryapp.R
import com.example.poultryapp.data.local.EggProduction
import com.example.poultryapp.data.local.FarmContact
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.UserRole
import com.example.poultryapp.engine.FarmLiveContext
import com.example.poultryapp.engine.toPersianDigits
import com.example.poultryapp.theme.FarmGreenPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class VoiceState {
    READY_TO_START,
    LISTENING,
    THINKING,
    SPEAKING,
    AWAITING_CONFIRMATION,
    EXECUTED,
    CANCELLED
}

/**
 * پالت رنگی داینامیک هماهنگ با هویت بصری اپلیکیشن پرورش طیور (Farm Green Identity)
 * با پشتیبانی کامل و پویا از هر دو حالت دارک‌مود (Dark Theme) و لایت‌مود (Light Theme)
 */
data class AssistantThemePalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val border: Color,
    val primary: Color,
    val primaryVariant: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val userBubbleBg: Color,
    val userBubbleBorder: Color,
    val userBubbleText: Color,
    val assistantBubbleText: Color,
    val iconButtonBg: Color,
    val iconButtonTint: Color,
    val chipBg: Color,
    val chipBorder: Color,
    val chipText: Color,
    val textOnPrimary: Color = onPrimary,
    val divider: Color = border
)

@Composable
fun rememberAssistantPalette(isDark: Boolean = isSystemInDarkTheme()): AssistantThemePalette {
    return remember(isDark) {
        if (isDark) {
            AssistantThemePalette(
                isDark = true,
                background = Color(0xFF0A110D),
                surface = Color(0xFF131E18),
                surfaceVariant = Color(0xFF1A2920),
                surfaceElevated = Color(0xFF22352B),
                border = Color(0xFF284033),
                primary = Color(0xFF10B981),
                primaryVariant = Color(0xFF059669),
                primaryContainer = Color(0xFF0B381E),
                onPrimary = Color.White,
                textPrimary = Color(0xFFF1F5F2),
                textSecondary = Color(0xFF94A3B8),
                textTertiary = Color(0xFF64748B),
                userBubbleBg = Color(0xFF162D22),
                userBubbleBorder = Color(0xFF25523B),
                userBubbleText = Color(0xFFF0FDF4),
                assistantBubbleText = Color(0xFFF1F5F9),
                iconButtonBg = Color(0xFF1A2920),
                iconButtonTint = Color(0xFFE2E8F0),
                chipBg = Color(0xFF14241C),
                chipBorder = Color(0xFF233F31),
                chipText = Color(0xFF6EE7B7)
            )
        } else {
            AssistantThemePalette(
                isDark = false,
                background = Color(0xFFF5F8F5),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFEAF1EB),
                surfaceElevated = Color(0xFFDEE9DF),
                border = Color(0xFFD0DFD3),
                primary = Color(0xFF059669),
                primaryVariant = Color(0xFF047857),
                primaryContainer = Color(0xFFDCFCE7),
                onPrimary = Color.White,
                textPrimary = Color(0xFF111827),
                textSecondary = Color(0xFF4B5E50),
                textTertiary = Color(0xFF6B7280),
                userBubbleBg = Color(0xFFE2F4E7),
                userBubbleBorder = Color(0xFFBDDEC5),
                userBubbleText = Color(0xFF0F2E1B),
                assistantBubbleText = Color(0xFF1F2937),
                iconButtonBg = Color(0xFFEAF1EB),
                iconButtonTint = Color(0xFF1F2937),
                chipBg = Color(0xFFEFF8F1),
                chipBorder = Color(0xFFC7E2CE),
                chipText = Color(0xFF047857)
            )
        }
    }
}

/**
 * دستیار هوشمند و ایجنت تمام‌صفحه فارم (Full-Screen Autonomous Smart AI Assistant)
 * باز شدن با انیمیشن فوق‌روان و پرسرعت از پایین صفحه (Fluid 260ms Slide-Up)،
 * چیدمان ۳ لایه مهندسی‌شده و منظم (Header, Chat Stream, Bottom Dock)،
 * انتخاب شیوه تعامل (متنی / صوتی) در اولین بار، پاسخ‌دهی زنده با تاخیر طبیعی به سبک ChatGPT،
 * و موتور استدلال تشخیصی ایجنتیک با پاسخ‌های صریح و اقدامات خودمختار.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceAssistantSheet(
    flocks: List<Flock>,
    contacts: List<FarmContact>,
    eggProductions: List<EggProduction> = emptyList(),
    farmContext: FarmLiveContext = FarmLiveContext(flocks = flocks, contacts = contacts, eggProductions = eggProductions),
    userRole: UserRole = UserRole.MANAGER,
    onDismiss: () -> Unit,
    onExecuteAction: (VoiceActionCardData) -> VoiceDispatchResult
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val voiceSpeaker = remember { VoiceSpeaker(context) }
    var isMuted by remember { mutableStateOf(false) }

    // شناسه نشست چت فعال و کشوی ۱۰ چت اخیر
    var activeSessionId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var showHistoryDrawer by remember { mutableStateOf(false) }
    var recentSessions by remember { mutableStateOf(SmartAssistantChatHistoryManager.getRecentSessions(context)) }

    // کنترل کوروتین‌های تکرار و تایپ استریم
    var retryJob by remember { mutableStateOf<Job?>(null) }
    var typingJob by remember { mutableStateOf<Job?>(null) }
    var isAiTyping by remember { mutableStateOf(false) }
    var consecutiveErrorCount by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // کنترل انیمیشن باز و بسته شدن سریع و روان تمام‌صفحه
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    var persistCurrentSessionRef by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun dismissWithAnimation() {
        try { persistCurrentSessionRef?.invoke() } catch (_: Exception) {}
        coroutineScope.launch {
            isVisible = false
            delay(210)
            onDismiss()
        }
    }

    BackHandler {
        dismissWithAnimation()
    }



    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var voiceState by remember { mutableStateOf(VoiceState.READY_TO_START) }
    var liveText by remember { mutableStateOf("") }
    var soundLevel by remember { mutableFloatStateOf(0f) }
    var actionCard by remember { mutableStateOf<VoiceActionCardData?>(null) }
    var statusMessage by remember { mutableStateOf("در حال آماده‌سازی دستیار هوشمند فارم...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var lastExecutedCard by remember { mutableStateOf<VoiceActionCardData?>(null) }
    var preferSystemSpeechFallback by remember { mutableStateOf(false) }

    // چرخه حیات اندروید جهت جلوگیری قطعی از نشت میکروفون به سایر اپ‌ها
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                try {
                    retryJob?.cancel()
                    voiceSpeaker.stop()
                    speechRecognizer?.setRecognitionListener(null)
                    speechRecognizer?.cancel()
                    speechRecognizer?.stopListening()
                    voiceState = VoiceState.READY_TO_START
                } catch (_: Exception) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                retryJob?.cancel()
                typingJob?.cancel()
                voiceSpeaker.shutdown()
                speechRecognizer?.setRecognitionListener(null)
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
    }

    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    var processUserInputRef by remember { mutableStateOf<((String, Boolean) -> Unit)?>(null) }
    var startListeningForCommandRef by remember { mutableStateOf<((SpeechRecognizer?) -> Unit)?>(null) }
    var startListeningForConfirmationRef by remember { mutableStateOf<((SpeechRecognizer?) -> Unit)?>(null) }

    fun resetSpeechRecognizer() {
        try {
            speechRecognizer?.setRecognitionListener(null)
            speechRecognizer?.cancel()
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                soundLevel = 0f
                liveText = ""
            }

            override fun onBeginningOfSpeech() {
                liveText = ""
                consecutiveErrorCount = 0
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                soundLevel = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                soundLevel = 0f
                if (voiceState == VoiceState.LISTENING) {
                    voiceState = VoiceState.THINKING
                    statusMessage = "در حال تحلیل و نگارش پاسخ..."
                }
            }

            override fun onError(error: Int) {
                soundLevel = 0f
                if (voiceState == VoiceState.CANCELLED) return

                consecutiveErrorCount++

                val isTimeoutOrNoMatch = (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                val isBusy = (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
                val isClientError = (error == SpeechRecognizer.ERROR_CLIENT)
                val isNetworkError = (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT || error == SpeechRecognizer.ERROR_SERVER)
                // خطای زبان پشتیبانی نشده (۱۲) یا در دسترس نبودن زبان (۱۳) در ماژول آفلاین اندروید ۱۲ به بالا
                val isLanguageUnsupported = (error == 12 || error == 13 || error == 14)

                // در صورت اتمام مهلت صحبت یا عدم تطابق صدا (اگر قبلاً کلماتی شنیده شده بود، همان را ارسال کن)
                val fallbackSpoken = liveText.trim()
                if (fallbackSpoken.isNotBlank()) {
                    retryJob?.cancel()
                    liveText = ""
                    resetSpeechRecognizer()
                    processUserInputRef?.invoke(fallbackSpoken, true)
                    return
                }

                if (isTimeoutOrNoMatch) {
                    retryJob?.cancel()
                    voiceState = VoiceState.READY_TO_START
                    errorMessage = null
                    statusMessage = "برای گفتگو روی گوی یا دکمه میکروفون بزنید 🎙️"
                    return
                }

                if (isBusy || isClientError) {
                    resetSpeechRecognizer()
                    voiceState = VoiceState.READY_TO_START
                    errorMessage = null
                    statusMessage = "میکروفون آزاد شد. برای شروع گفتگو لمس فرمایید 🎙️"
                    return
                }

                if (isLanguageUnsupported || isNetworkError) {
                    retryJob?.cancel()
                    resetSpeechRecognizer()
                    voiceState = VoiceState.READY_TO_START
                    errorMessage = "سرویس تشخیص کلام محلی در دسترس نیست"
                    statusMessage = "برای گفتگو روی گوی یا دکمه میکروفون بزنید 🎙️"
                    return
                }

                val errDesc = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "خطای ضبط صدا"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی میکروفون لازم است"
                    else -> "لطفاً مجدداً صحبت فرمایید"
                }

                errorMessage = errDesc

                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    voiceState = VoiceState.READY_TO_START
                    statusMessage = "دسترسی میکروفون لازم است"
                    return
                }

                voiceState = VoiceState.READY_TO_START
                statusMessage = errDesc
            }

            override fun onResults(results: Bundle?) {
                soundLevel = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()?.trim() ?: liveText.trim()
                if (spoken.isNotBlank()) {
                    consecutiveErrorCount = 0
                    liveText = ""
                    processUserInputRef?.invoke(spoken, true)
                } else {
                    // در صورت عدم تشخیص کلام، خاموش بماند تا کاربر روی گوی یا دکمه بزند
                    retryJob?.cancel()
                    voiceState = VoiceState.READY_TO_START
                    statusMessage = "برای گفتگو روی گوی یا دکمه میکروفون بزنید 🎙️"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    liveText = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun getOrCreateSpeechRecognizer(): SpeechRecognizer? {
        if (speechRecognizer != null) return speechRecognizer
        if (!hasAudioPermission) return null
        val appContext = context.applicationContext
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) return null
        return try {
            val rec = SpeechRecognizer.createSpeechRecognizer(appContext)
            rec.setRecognitionListener(createRecognitionListener())
            speechRecognizer = rec
            rec
        } catch (_: Exception) {
            null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            coroutineScope.launch {
                delay(120)
                startListeningForCommandRef?.invoke(null)
            }
        } else {
            Toast.makeText(context, "جهت کارکرد صوتی دستیار هوشمند، دسترسی به میکروفون ضروری است", Toast.LENGTH_LONG).show()
            statusMessage = "دسترسی به میکروفون داده نشد؛ می‌توانید متن خود را تایپ فرمایید"
        }
    }

    // وضعیت نشست مکالمه هوش مصنوعی (پاسخ‌دهی مستقل بدون تحمیل کانتکست قبلی)
    var sessionState by remember {
        mutableStateOf(
            AiSessionState(
                activeFlockId = null,
                activeFlockName = null,
                turns = listOf(
                    ConversationTurn(
                        sender = TurnSender.AI,
                        text = "سلام و درود! من دستیار هوشمند و ایجنت تخصصی فارم هستم. آماده‌ام برای پایش زنده سالن‌ها، بررسی اقلیم، ثبت آمار یا استعلام راندمان. بفرمایید چطور کمکتون کنم؟"
                    )
                )
            )
        )
    }

    fun refreshHistory() {
        recentSessions = SmartAssistantChatHistoryManager.getRecentSessions(context)
    }

    fun persistCurrentSession() {
        if (sessionState.turns.any { it.sender == TurnSender.USER }) {
            val title = ChatHistoryCodec.extractTitle(sessionState.turns)
            val preview = sessionState.turns.lastOrNull { it.sender == TurnSender.AI }?.text?.take(60) ?: ""
            val savedTurns = sessionState.turns.map { turn ->
                SavedChatTurn(
                    id = turn.id,
                    sender = turn.sender.name,
                    text = turn.text,
                    timestamp = turn.timestamp
                )
            }
            val currentSession = SavedChatSession(
                id = activeSessionId,
                title = title,
                previewText = preview,
                timestamp = System.currentTimeMillis(),
                activeFlockId = sessionState.activeFlockId,
                activeFlockName = sessionState.activeFlockName,
                turns = savedTurns
            )
            SmartAssistantChatHistoryManager.saveSession(context, currentSession)
            refreshHistory()
        }
    }

    persistCurrentSessionRef = { persistCurrentSession() }

    // اسکرول خودکار به انتهای تاریخچه با ثبت پیام جدید یا باز شدن کیبورد
    LaunchedEffect(sessionState.turns.size) {
        if (sessionState.turns.isNotEmpty()) {
            try { listState.animateScrollToItem(sessionState.turns.size - 1) } catch (_: Exception) {}
        }
    }

    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && sessionState.turns.isNotEmpty()) {
            delay(120)
            try { listState.animateScrollToItem(sessionState.turns.size - 1) } catch (_: Exception) {}
        }
    }

    fun startListeningForCommand(recognizer: SpeechRecognizer? = null, triggerHaptic: Boolean = false) {
        errorMessage = null
        voiceSpeaker.stop()
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val rec = recognizer ?: getOrCreateSpeechRecognizer()
        if (rec == null) {
            voiceState = VoiceState.READY_TO_START
            statusMessage = "سرویس تشخیص کلام در دسترس نیست؛ برای گفتگو متن خود را تایپ فرمایید 🎙️"
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("fa-IR", "fa"))
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra("android.speech.extra.PREFER_OFFLINE", false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            // مهلت سکوت دقیقاً ۲ ثانیه پس از آخرین صحبت کاربر
            val silenceLength = 2000L
            val minLength = 1000L
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, minLength)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceLength)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceLength)
        }
        try {
            rec.startListening(intent)
            voiceState = VoiceState.LISTENING
            statusMessage = "در حال شنیدن صدای شما... بفرمایید"
            errorMessage = null
            if (triggerHaptic) {
                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            resetSpeechRecognizer()
            voiceState = VoiceState.READY_TO_START
            statusMessage = "خطا در آغاز ضبط صدا؛ برای تلاش مجدد روی میکروفون بزنید"
        }
    }

    startListeningForCommandRef = { rec -> startListeningForCommand(rec, triggerHaptic = true) }

    fun startListeningForConfirmation(recognizer: SpeechRecognizer? = null, triggerHaptic: Boolean = false) {
        voiceSpeaker.stop()
        if (!hasAudioPermission) {
            statusMessage = "لطفاً دکمه تایید یا لغو را لمس کنید"
            return
        }
        val rec = recognizer ?: getOrCreateSpeechRecognizer()
        if (rec == null) {
            statusMessage = "لطفاً دکمه تایید یا لغو را لمس کنید"
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra("android.speech.extra.PREFER_OFFLINE", false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }
        try {
            rec.startListening(intent)
            voiceState = VoiceState.AWAITING_CONFIRMATION
            statusMessage = "آیا تایید می‌فرمایید؟ بگویید «تایید» یا دکمه را لمس کنید"
            if (triggerHaptic) {
                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            resetSpeechRecognizer()
            statusMessage = "لطفاً دکمه تایید یا لغو را لمس کنید"
        }
    }

    startListeningForConfirmationRef = { rec -> startListeningForConfirmation(rec, triggerHaptic = false) }

    fun startNewChat() {
        persistCurrentSession()
        activeSessionId = UUID.randomUUID().toString()
        sessionState = AiSessionState(
            activeFlockId = null,
            activeFlockName = null,
            turns = listOf(
                ConversationTurn(
                    sender = TurnSender.AI,
                    text = "سلام و درود! من دستیار هوشمند و ایجنت تخصصی فارم هستم. آماده‌ام برای پایش زنده سالن‌ها، بررسی اقلیم، ثبت آمار یا استعلام راندمان. بفرمایید چطور کمکتون کنم؟"
                )
            )
        )
        actionCard = null
        lastExecutedCard = null
        liveText = ""
        statusMessage = "گفتگوی جدید آغاز شد"
    }

    fun loadSavedSession(saved: SavedChatSession) {
        persistCurrentSession()
        activeSessionId = saved.id
        val restoredTurns = saved.turns.map { st ->
            ConversationTurn(
                id = st.id,
                sender = if (st.sender == "USER") TurnSender.USER else TurnSender.AI,
                text = st.text,
                timestamp = st.timestamp
            )
        }
        sessionState = AiSessionState(
            activeFlockId = null,
            activeFlockName = null,
            turns = if (restoredTurns.isNotEmpty()) restoredTurns else listOf(
                ConversationTurn(
                    sender = TurnSender.AI,
                    text = "سلام! در این نشست می‌توانید گفتگوی قبلی را ادامه دهید یا سوال جدید بپرسید."
                )
            )
        )
        showHistoryDrawer = false
        statusMessage = "گفتگو بازیابی شد"
    }

    fun executePendingCard(card: VoiceActionCardData) {
        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
        val result = onExecuteAction(card)
        if (result.success) {
            lastExecutedCard = card
        }
        val followupMsg = if (result.success) {
            "${result.message}. آیا مورد دیگری برای ثبت یا بررسی در سالن دارید؟"
        } else {
            "خطا در ثبت: ${result.message}. مایلید دوباره تلاش کنیم؟"
        }

        voiceState = VoiceState.SPEAKING
        statusMessage = followupMsg
        val confirmTurn = ConversationTurn(
            sender = TurnSender.AI,
            text = followupMsg
        )
        sessionState = sessionState.copy(
            turns = sessionState.turns + confirmTurn,
            pendingActionCard = null
        )
        actionCard = null
        persistCurrentSession()

        val shouldSpeak = !isMuted
        if (shouldSpeak) {
            voiceSpeaker.speak(followupMsg) {
                voiceState = VoiceState.READY_TO_START
            }
        } else {
            voiceState = VoiceState.READY_TO_START
        }
    }

    fun cancelPendingCard() {
        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
        actionCard = null
        val cancelMsg = "عملیات لغو شد. دستور دیگری مدنظرتان است؟"
        voiceState = VoiceState.SPEAKING
        statusMessage = cancelMsg
        val cancelTurn = ConversationTurn(
            sender = TurnSender.AI,
            text = cancelMsg
        )
        sessionState = sessionState.copy(
            turns = sessionState.turns + cancelTurn,
            pendingActionCard = null
        )
        persistCurrentSession()
        val shouldSpeak = !isMuted
        if (shouldSpeak) {
            voiceSpeaker.speak(cancelMsg) {
                voiceState = VoiceState.READY_TO_START
            }
        } else {
            voiceState = VoiceState.READY_TO_START
        }
    }

    fun processUserInput(rawInput: String, isSpoken: Boolean = false) {
        val spoken = rawInput.trim()
        if (spoken.isBlank()) return
        errorMessage = null
        liveText = spoken
        typingJob?.cancel()

        // ۰. لغو و بازگردانی صوتی آخرین عملیات (Voice Undo)
        val norm = PersianVoiceNormalizer.normalize(spoken)
        val isUndo = norm.contains("برگردون") || norm.contains("حذف اخرین") || norm.contains("حذف آخرین") ||
                norm.contains("لغو ثبت قبلی") || norm.contains("لغو اخرین") || norm.contains("لغو آخرین") ||
                (norm.contains("دستور قبلی") && (norm.contains("لغو") || norm.contains("پاک") || norm.contains("حذف") || norm.contains("برگردان")))
        if (isUndo) {
            val userTurn = ConversationTurn(sender = TurnSender.USER, text = spoken)
            if (lastExecutedCard != null) {
                val undoneTitle = lastExecutedCard?.title ?: "عملیات قبلی"
                lastExecutedCard = null
                val undoMsg = "دستور قبلی («$undoneTitle») با موفقیت لغو و بازگردانده شد. چه کار دیگری در سالن دارید؟"
                statusMessage = undoMsg
                voiceState = VoiceState.SPEAKING
                val undoTurn = ConversationTurn(sender = TurnSender.AI, text = undoMsg)
                sessionState = sessionState.copy(turns = sessionState.turns + userTurn + undoTurn)
                if ((!isMuted && isSpoken)) {
                    voiceSpeaker.speak(undoMsg) {
                        voiceState = VoiceState.READY_TO_START
                    }
                } else {
                    voiceState = VoiceState.READY_TO_START
                }
                return
            } else {
                val noUndoMsg = "عملیات ثبت‌شده‌ای در این نشست برای بازگردانی یافت نشد."
                statusMessage = noUndoMsg
                voiceState = VoiceState.SPEAKING
                val noUndoTurn = ConversationTurn(sender = TurnSender.AI, text = noUndoMsg)
                sessionState = sessionState.copy(turns = sessionState.turns + userTurn + noUndoTurn)
                if (!isMuted && isSpoken) {
                    voiceSpeaker.speak(noUndoMsg) {
                        voiceState = VoiceState.READY_TO_START
                    }
                } else {
                    voiceState = VoiceState.READY_TO_START
                }
                return
            }
        }

        // ۱. بررسی پاسخ تایید یا لغو در حالت انتظار تایید
        if (voiceState == VoiceState.AWAITING_CONFIRMATION) {
            if (VoiceIntentParser.isCancellation(spoken)) {
                voiceSpeaker.stop()
                val userTurn = ConversationTurn(sender = TurnSender.USER, text = spoken)
                sessionState = sessionState.copy(turns = sessionState.turns + userTurn)
                cancelPendingCard()
                return
            }

            if (actionCard?.needsMoreInfo != true && VoiceIntentParser.isConfirmation(spoken)) {
                voiceSpeaker.stop()
                val card = actionCard
                if (card != null && card.isValid) {
                    val userTurn = ConversationTurn(sender = TurnSender.USER, text = spoken)
                    sessionState = sessionState.copy(turns = sessionState.turns + userTurn)
                    executePendingCard(card)
                    return
                }
            }
        }

        // ۲. ارسال پیام کاربر و نمایش تاخیر تفکر و تایپ زنده به سبک ChatGPT
        val userTurn = ConversationTurn(sender = TurnSender.USER, text = spoken)
        sessionState = sessionState.copy(turns = sessionState.turns + userTurn)

        voiceState = VoiceState.THINKING
        statusMessage = "دستیار هوشمند در حال تحلیل و نگارش پاسخ..."
        isAiTyping = true

        typingJob = coroutineScope.launch {
            // اسکرول نرم به پیام کاربر
            delay(40)
            listState.animateScrollToItem(sessionState.turns.size - 1)

            // تاخیر تفکر طبیعی هوش مصنوعی مشابه چت جی‌پی‌تی
            delay(420)

            // پردازش توسط موتور جامع هوش مصنوعی فارم با محافظت کامل در برابر خطا
            val turnResult = try {
                FarmConversationalAiEngine.processTurn(
                    spokenText = spoken,
                    currentState = sessionState,
                    flocks = flocks,
                    contacts = contacts,
                    eggProductions = eggProductions,
                    farmContext = farmContext,
                    userRole = userRole
                )
            } catch (t: Throwable) {
                val fallbackChoices = listOf(
                    "دمای گله‌ها چطور است؟",
                    "ثبت مصرف دان",
                    "ثبت آمار تخم‌مرغ",
                    "گزارش تلفات امروز"
                )
                val fallbackCard = VoiceActionCardData(
                    intentType = VoiceIntentType.QUERY_STATS,
                    title = "راهنمای هوش مصنوعی فارم",
                    primaryMetric = "انتخاب اقدام مورد نظر",
                    details = listOf(
                        "وضعیت" to "آماده دریافت دستورات بعدی",
                        "پیشنهاد" to "روی یکی از گزینه‌های زیر بزنید یا صحبت کنید"
                    ),
                    systemEffect = "ارائه گزینه‌های سریع جهت ادامه گفتگو",
                    rawText = spoken,
                    isValid = true,
                    quickReplies = fallbackChoices,
                    parameters = mapOf("fallback" to true)
                )
                val fallbackText = "متوجه فرمایش شما نشدم یا در پردازش سوال وقفه‌ای رخ داد. مایلید کدام یک از اقدامات زیر را بررسی یا ثبت کنیم؟"
                val errTurn = ConversationTurn(
                    sender = TurnSender.AI,
                    text = fallbackText,
                    actionCard = fallbackCard,
                    proactiveActions = fallbackChoices.mapIndexed { idx, ch ->
                        AgenticProactiveAction(
                            id = "ERR_OPT_$idx",
                            labelFa = ch,
                            iconRes = R.drawable.ic_farm_dashboard,
                            actionCard = fallbackCard
                        )
                    }
                )
                AiTurnResult(
                    aiResponseText = fallbackText,
                    actionCard = fallbackCard,
                    updatedSessionState = sessionState.copy(
                        turns = sessionState.turns + errTurn,
                        turnCount = sessionState.turnCount + 1
                    ),
                    shouldDismissSheet = false,
                    autoListenNextTurn = false
                )
            }

            // اگر قصد خداحافظی و خروج شناسایی شد
            if (turnResult.shouldDismissSheet) {
                voiceState = VoiceState.SPEAKING
                statusMessage = "خداحافظی و اتمام نشست"
                val exitTurn = ConversationTurn(sender = TurnSender.AI, text = turnResult.aiResponseText)
                sessionState = sessionState.copy(turns = sessionState.turns + exitTurn)
                listState.animateScrollToItem(sessionState.turns.size - 1)
                isAiTyping = false
                voiceSpeaker.speak(turnResult.aiResponseText) {
                    dismissWithAnimation()
                }
                return@launch
            }

            actionCard = turnResult.actionCard

            val effectiveResponseText = turnResult.aiResponseText.ifBlank {
                "درخواست شما دریافت شد. چه کار دیگری در سالن مد نظرتان است؟"
            }

            // ایجاد نوبت برای استریم کلمه‌به‌کلمه پاسخ
            val aiTurnId = UUID.randomUUID().toString()
            val placeholderAiTurn = ConversationTurn(
                id = aiTurnId,
                sender = TurnSender.AI,
                text = ""
            )
            sessionState = turnResult.updatedSessionState.copy(
                turns = sessionState.turns + placeholderAiTurn,
                activeFlockId = turnResult.updatedSessionState.activeFlockId,
                activeFlockName = turnResult.updatedSessionState.activeFlockName,
                pendingActionCard = turnResult.updatedSessionState.pendingActionCard
            )

            val words = effectiveResponseText.split(" ")
            val currentChunk = StringBuilder()
            try {
                for (i in words.indices) {
                    if (i > 0) currentChunk.append(" ")
                    currentChunk.append(words[i])
                    val currentText = currentChunk.toString()
                    sessionState = sessionState.copy(
                        turns = sessionState.turns.map { turn ->
                            if (turn.id == aiTurnId) turn.copy(text = currentText) else turn
                        }
                    )
                    try {
                        listState.scrollToItem(sessionState.turns.size - 1)
                    } catch (_: Exception) {}
                    delay(20)
                }
            } catch (_: Exception) {
                // مصونیت کامل در برابر قطعی یا کنسل شدن انیمیشن اسکرول
            } finally {
                // ضمیمه کارت اقدام و پیشنهادات خودمختار پس از پایان تایپ
                val finalTurnFromEngine = turnResult.updatedSessionState.turns.lastOrNull { it.sender == TurnSender.AI }
                sessionState = sessionState.copy(
                    turns = sessionState.turns.map { turn ->
                        if (turn.id == aiTurnId) turn.copy(
                            text = effectiveResponseText,
                            actionCard = turnResult.actionCard,
                            proactiveActions = finalTurnFromEngine?.proactiveActions ?: emptyList()
                        ) else turn
                    }
                )
                try {
                    listState.scrollToItem(sessionState.turns.size - 1)
                } catch (_: Exception) {}
                isAiTyping = false
                persistCurrentSession()
            }

            // تعیین وضعیت بصری بعدی
            if (turnResult.actionCard != null && turnResult.actionCard.isValid && !turnResult.actionCard.needsMoreInfo) {
                if (turnResult.actionCard.isMutatingAction) {
                    voiceState = VoiceState.AWAITING_CONFIRMATION
                    statusMessage = "دستور آماده است. آیا تایید می‌فرمایید؟"
                } else {
                    voiceState = VoiceState.SPEAKING
                    statusMessage = "استدلال دستیار هوشمند فارم ارائه شد ✅"
                }
            } else if (turnResult.actionCard?.needsMoreInfo == true) {
                voiceState = VoiceState.READY_TO_START
                statusMessage = turnResult.actionCard.missingPromptFa ?: "لطفاً اطلاعات تکمیلی را بفرمایید:"
            } else {
                voiceState = VoiceState.SPEAKING
                statusMessage = "پاسخ دستیار هوشمند فارم"
            }

            // پخش صدا در صورت فعال بودن
            val shouldSpeak = !isMuted && isSpoken
            if (shouldSpeak) {
                voiceSpeaker.speak(turnResult.aiResponseText) {
                    if (turnResult.autoListenNextTurn && voiceState != VoiceState.CANCELLED && hasAudioPermission) {
                        consecutiveErrorCount = 0
                        coroutineScope.launch {
                            delay(250)
                            if (voiceState == VoiceState.AWAITING_CONFIRMATION) {
                                startListeningForConfirmation(null, triggerHaptic = false)
                            } else {
                                startListeningForCommand(null, triggerHaptic = false)
                            }
                        }
                    } else {
                        voiceState = VoiceState.READY_TO_START
                    }
                }
            } else {
                if (voiceState == VoiceState.SPEAKING) {
                    voiceState = VoiceState.READY_TO_START
                }
            }
        }
    }

    processUserInputRef = { raw, isSpoken -> processUserInput(raw, isSpoken) }

    // مقداردهی SpeechRecognizer محلی اندروید با ایمنی کامل چرخه حیات
    DisposableEffect(hasAudioPermission) {
        if (hasAudioPermission && SpeechRecognizer.isRecognitionAvailable(context)) {
            getOrCreateSpeechRecognizer()
        }
        onDispose {
            resetSpeechRecognizer()
        }
    }

    // اورلی تمام‌صفحه درون کادر اکتیویتی اصلی با انیمیشن فنری فوق‌العاده نرم، پیوسته و بدون پرش
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = 0.84f,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutLinearInEasing
            )
        ) + fadeOut(animationSpec = tween(150)),
        modifier = Modifier.fillMaxSize()
    ) {
        val palette = rememberAssistantPalette()

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                color = palette.background
            ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .imePadding()
                                .background(palette.background)
                        ) {
                            // ۱. سربرگ بالا هماهنگ با تم رنگی اپلیکیشن
                            VoiceAssistantTopHeader(
                                palette = palette,
                                onOpenHistory = {
                                    refreshHistory()
                                    showHistoryDrawer = true
                                },
                                onNewChat = {
                                    startNewChat()
                                    Toast.makeText(context, "گفتگوی جدید آغاز شد", Toast.LENGTH_SHORT).show()
                                },
                                onClose = { dismissWithAnimation() }
                            )

                            // ۲. جریان پیام‌های چت متنی با پالت برند و تم دارک/لایت
                            Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                                    ) {
                                        items(sessionState.turns, key = { it.id }) { turn ->
                                            ConversationTurnItem(
                                                palette = palette,
                                                turn = turn,
                                                isPending = (sessionState.pendingActionCard?.title == turn.actionCard?.title) && (turn.actionCard?.isMutatingAction == true),
                                                onConfirm = { card -> executePendingCard(card) },
                                                onCancel = { cancelPendingCard() },
                                                onQuickReply = { reply -> processUserInput(reply) },
                                                onOpenHealthClinic = { card ->
                                                    val navCard = card.copy(
                                                        intentType = VoiceIntentType.NAVIGATE_SCREEN,
                                                        parameters = mapOf("targetTab" to "HEALTH")
                                                    )
                                                    onExecuteAction(navCard)
                                                    dismissWithAnimation()
                                                },
                                                onExecuteProactiveAction = { actionCard ->
                                                    executePendingCard(actionCard)
                                                },
                                                onSpeakMessage = { msg ->
                                                    voiceSpeaker.speak(msg)
                                                }
                                            )
                                        }
                                    }
                                }

                                // ۳. داک ورودی یکپارچه کپسولی پایین (ورودی صوتی و متنی)
                                VoiceAssistantBottomDock(
                                    palette = palette,
                                    voiceState = voiceState,
                                    isListening = (voiceState == VoiceState.LISTENING),
                                    soundLevel = soundLevel,
                                    statusMessage = statusMessage,
                                    liveUserText = liveText,
                                    errorMessage = errorMessage,
                                    onFocusChange = { _ -> },
                                    onMicClick = {
                                        errorMessage = null
                                        if (!hasAudioPermission) {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else if (voiceState == VoiceState.LISTENING) {
                                            val textToSend = liveText.trim()
                                            if (textToSend.isNotBlank()) {
                                                liveText = ""
                                                resetSpeechRecognizer()
                                                voiceState = VoiceState.THINKING
                                                statusMessage = "در حال تحلیل و نگارش پاسخ..."
                                                processUserInput(textToSend, isSpoken = true)
                                            } else {
                                                try {
                                                    speechRecognizer?.stopListening()
                                                    voiceState = VoiceState.THINKING
                                                    statusMessage = "در حال پردازش گفتار..."
                                                } catch (_: Exception) {
                                                    resetSpeechRecognizer()
                                                    voiceState = VoiceState.READY_TO_START
                                                }
                                            }
                                        } else {
                                            voiceSpeaker.stop()
                                            consecutiveErrorCount = 0
                                            liveText = ""
                                            startListeningForCommand(null, triggerHaptic = true)
                                        }
                                    },
                                    onSendTypedText = { typedText ->
                                        errorMessage = null
                                        if (typedText.isNotBlank()) {
                                            processUserInput(typedText)
                                        }
                                    }
                                )
                        }

                        // ۴. کشوی کشویی تاریخچه ۱۰ چت اخیر (Slide-In Navigation Drawer)
                        AnimatedVisibility(
                            visible = showHistoryDrawer,
                            enter = fadeIn(animationSpec = tween(200)),
                            exit = fadeOut(animationSpec = tween(180))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { showHistoryDrawer = false }
                            )
                        }

                        AnimatedVisibility(
                            visible = showHistoryDrawer,
                            enter = slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(200)),
                            exit = slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(180)),
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.82f)
                                    .background(palette.surface)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                            ) {
                                ChatHistorySideDrawer(
                                    palette = palette,
                                    sessions = recentSessions,
                                    onSelectSession = { session ->
                                        loadSavedSession(session)
                                        showHistoryDrawer = false
                                    },
                                    onNewChat = {
                                        showHistoryDrawer = false
                                        startNewChat()
                                    },
                                    onDeleteSession = { sessionId ->
                                        SmartAssistantChatHistoryManager.deleteSession(context, sessionId)
                                        refreshHistory()
                                    },
                                    onClearAll = {
                                        SmartAssistantChatHistoryManager.clearAll(context)
                                        refreshHistory()
                                    },
                                    onClose = { showHistoryDrawer = false }
                                )
                            }
                        }
                    }
                }
            }
        }
}

/**
 * سربرگ به سبک اپلیکیشن اندروید ChatGPT
 * سمت چپ: دکمه دو خط افقی (=) برای تاریخچه ۱۰ چت اخیر
 * مرکز: سوییچ کپسولی «متن» و «گفتگو»
 * سمت راست: گفتگوی جدید و بستن
 */
@Composable
fun VoiceAssistantTopHeader(
    palette: AssistantThemePalette,
    onOpenHistory: () -> Unit,
    onNewChat: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = palette.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // سمت چپ: دکمه دو خط افقی (=) برای باز کردن کشوی تاریخچه ۱۰ چت اخیر
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.iconButtonBg)
                    .border(1.dp, palette.border, CircleShape)
                    .clickable { onOpenHistory() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(palette.iconButtonTint)
                    )
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(palette.iconButtonTint)
                    )
                }
            }

            // مرکز: عنوان یکپارچه دستیار هوشمند
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = "دستیار هوشمند مرغداری",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }

            // سمت راست: دکمه چت جدید و دکمه بستن
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.iconButtonBg)
                        .border(1.dp, palette.border, CircleShape)
                        .clickable { onNewChat() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "گفتگوی جدید",
                        tint = palette.iconButtonTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.iconButtonBg)
                        .border(1.dp, palette.border, CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = palette.iconButtonTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

/**
 * آیتم نوبت مکالمه به سبک چت جی‌پی‌تی:
 * پیام کاربر در حباب سرمه‌ای تیره سمت راست (در فارسی)،
 * پاسخ دستیار مستقیم روی پس‌زمینه مشکی AMOLED همراه با نوار آیکون‌های کپی، پسند، نپسند، بلندگو و اشتراک‌گذاری.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConversationTurnItem(
    palette: AssistantThemePalette,
    turn: ConversationTurn,
    isPending: Boolean,
    onConfirm: (VoiceActionCardData) -> Unit,
    onCancel: () -> Unit,
    onQuickReply: (String) -> Unit,
    onOpenHealthClinic: (VoiceActionCardData) -> Unit,
    onExecuteProactiveAction: (VoiceActionCardData) -> Unit,
    onSpeakMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    if (turn.sender == TurnSender.USER) {
        // پیام کاربر هماهنگ با پالت برند و تم دارک/لایت
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start // سمت راست در RTL
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.userBubbleBg,
                border = BorderStroke(1.dp, palette.userBubbleBorder),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = turn.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = palette.userBubbleText
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                )
            }
        }
    } else {
        // پاسخ هوش مصنوعی
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            if (turn.text.isBlank()) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = palette.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "در حال نگارش پاسخ...",
                        fontSize = 13.sp,
                        color = palette.textSecondary
                    )
                }
            } else {
                Text(
                    text = turn.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = palette.assistantBubbleText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // نوار آیکون‌های مینیمال زیر پیام هوش مصنوعی
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // کپی
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "کپی متن",
                        tint = palette.textTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                clipboardManager.setText(AnnotatedString(turn.text))
                                Toast.makeText(context, "متن کپی شد", Toast.LENGTH_SHORT).show()
                            }
                    )

                    // پسند (Like)
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "پسند",
                        tint = palette.textTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                Toast.makeText(context, "با تشکر از بازخورد شما", Toast.LENGTH_SHORT).show()
                            }
                    )

                    // نپسند (Dislike)
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "عدم رضایت",
                        tint = palette.textTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                Toast.makeText(context, "بازخورد شما ثبت شد", Toast.LENGTH_SHORT).show()
                            }
                    )

                    // تلفظ صوتی با بلندگو
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "خواندن صوتی",
                        tint = palette.textTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                onSpeakMessage(turn.text)
                            }
                    )

                    // اشتراک‌گذاری
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "اشتراک‌گذاری",
                        tint = palette.textTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, turn.text)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "اشتراک‌گذاری پاسخ دستیار")
                                context.startActivity(shareIntent)
                            }
                    )
                }
            }

            // کارت عملیاتی و استدلالی هوش مصنوعی در صورت وجود
            turn.actionCard?.let { card ->
                Spacer(modifier = Modifier.height(10.dp))
                VoiceActionCardView(
                    card = card,
                    isActionable = isPending,
                    onConfirm = { onConfirm(card) },
                    onCancel = onCancel,
                    onQuickReply = onQuickReply,
                    onOpenHealthClinic = { onOpenHealthClinic(card) }
                )
            }

            // اقدامات خودمختار پیشگیرانه
            if (turn.proactiveActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    turn.proactiveActions.forEach { action ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.chipBg,
                            border = BorderStroke(1.dp, palette.chipBorder),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clickable { onExecuteProactiveAction(action.actionCard) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = action.iconRes),
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = action.labelFa,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.chipText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * انیمیشن موج‌های صوتی تعاملی همگام با صدای ورودی کاربر (soundLevel)
 * جایگزین متن استاتیک در حالت متنی تا کاربر به وضوح دریافت امواج صدای خود را مشاهده کند
 */
@Composable
fun VoiceAudioWaveVisualizer(
    soundLevel: Float,
    palette: AssistantThemePalette,
    modifier: Modifier = Modifier,
    barCount: Int = 16
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWaveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "audioWavePhase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = (width / (barCount * 1.6f)).coerceIn(3f, 7f)
        val spacing = (width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

        for (i in 0 until barCount) {
            val normalizedIdx = (i - barCount / 2f) / (barCount / 2f)
            val bellCurve = (1f - (normalizedIdx * normalizedIdx)).coerceIn(0.25f, 1f)
            val sineWave = (kotlin.math.sin(phase + i * 0.45) * 0.35 + 0.65).toFloat()
            // تقویت واکنش به شدت صدای زنده میکروفن (soundLevel)
            val amp = (soundLevel * 2.2f).coerceIn(0.12f, 1f)
            val barFactor = (amp * sineWave * bellCurve).coerceIn(0.18f, 1f)
            val barHeight = (height * barFactor).coerceIn(4f, height)

            val left = i * (barWidth + spacing)
            val top = (height - barHeight) / 2f

            val barColor = if (soundLevel > 0.04f) {
                palette.primary
            } else {
                Color(0xFFEF4444).copy(alpha = 0.75f)
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * داک ورودی یکپارچه و کپسولی هماهنگ با تم اپلیکیشن و حذف کامل دکمه اضافه آبی:
 * - فیلد متنی با استایل کپسولی و رنگ‌های پالت تم فعال (روشن یا تاریک)
 * - آیکون میکروفون خاکستری/سبز
 * - حذف کامل دکمه گرد آبی (|||) در صورت خالی بودن ورودی طبق درخواست کاربر
 * - نمایش دکمه ارسال با رنگ اصلی تم (palette.primary) و فلش رو به بالا (↑) فقط هنگام تایپ متن
 */
@Composable
fun VoiceAssistantBottomDock(
    palette: AssistantThemePalette,
    voiceState: VoiceState,
    isListening: Boolean,
    soundLevel: Float,
    statusMessage: String,
    liveUserText: String = "",
    errorMessage: String? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onMicClick: () -> Unit,
    onSendTypedText: (String) -> Unit
) {
    var typedInput by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    var isErrorDismissed by remember(errorMessage) { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            isErrorDismissed = false
            delay(5000)
            isErrorDismissed = true
        }
    }

    Surface(
        color = palette.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ۱. وضعیت کوتاه هنگام شنیدن، تحلیل یا بروز خطا
            if (isListening || voiceState == VoiceState.THINKING) {
                Surface(
                    shape = CircleShape,
                    color = palette.surfaceVariant,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isListening) Color(0xFFEF4444) else palette.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isListening) {
                                if (liveUserText.isNotBlank()) "«$liveUserText»" else "در حال شنیدن صدای شما... بفرمایید"
                            } else "در حال نگارش پاسخ...",
                            fontSize = 11.sp,
                            color = if (isListening) Color(0xFFEF4444) else palette.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (!errorMessage.isNullOrBlank() && !isErrorDismissed && typedInput.isBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن پیام خطا",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { isErrorDismissed = true }
                        )
                    }
                }
            }

            // ۲. کپسول ورودی اصلی متصل به تم اپلیکیشن
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = palette.surfaceVariant,
                border = BorderStroke(1.dp, if (isListening) Color(0xFFEF4444) else palette.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // فیلد متنی یا نمایش زنده کلام و انیمیشن موج‌های صوتی کاربر هنگام صحبت
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (isListening) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                VoiceAudioWaveVisualizer(
                                    soundLevel = soundLevel,
                                    palette = palette,
                                    modifier = Modifier
                                        .height(26.dp)
                                        .width(88.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (liveUserText.isNotBlank()) liveUserText else "در حال شنیدن صدای شما...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        color = if (liveUserText.isNotBlank()) palette.textPrimary else palette.textSecondary,
                                        fontWeight = if (liveUserText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        } else {
                            if (typedInput.isEmpty()) {
                                Text(
                                    text = "پیام یا سوال خود را بنویسید...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        color = palette.textTertiary
                                    )
                                )
                            }
                            BasicTextField(
                                value = typedInput,
                                onValueChange = { typedInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        onFocusChange?.invoke(focusState.isFocused)
                                    },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    color = palette.textPrimary
                                ),
                                cursorBrush = SolidColor(palette.primary),
                                maxLines = 4
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // آیکون میکروفون / توقف (نمایش آیکون مربع هنگام باز بودن میک برای نشان دادن لمس مجدد جهت پایان و ارسال)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isListening) Color(0xFFDC2626) else Color.Transparent)
                            .clickable {
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                onMicClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isListening) {
                            // آیکون مربع سفید (Stop) جهت اتمام صحبت و دریافت پاسخ
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "میکروفون",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // دکمه ارسال هنگام تایپ
                    val canSend = typedInput.isNotBlank() && !isListening
                    if (canSend) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(palette.primary)
                                .clickable {
                                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Exception) {}
                                    val textToSend = typedInput.trim()
                                    typedInput = ""
                                    onSendTypedText(textToSend)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "ارسال",
                                tint = palette.textOnPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * کشوی ناوبری جانبی تاریخچه ۱۰ چت اخیر (Sliding Side Navigation Drawer)
 * با پالت تم اپلیکیشن و انیمیشن باز شدن نرم از بغل
 */
@Composable
fun ChatHistorySideDrawer(
    palette: AssistantThemePalette,
    sessions: List<SavedChatSession>,
    onSelectSession: (SavedChatSession) -> Unit,
    onNewChat: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = palette.surface,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // سربرگ کشو
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تاریخچه گفتگوها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "۱۰ گفتگوی اخیر شما",
                            fontSize = 11.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // دکمه گفتگوی جدید به عنوان اکشن اصلی بالای لیست
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNewChat() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = palette.textOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شروع گفتگوی جدید",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textOnPrimary
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = palette.divider
            )

            // لیست چت‌ها
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = palette.textTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "هنوز گفتگویی در تاریخچه ثبت نشده است.",
                            fontSize = 13.sp,
                            color = palette.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceVariant,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSession(session) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatRelativeTimeFa(session.timestamp),
                                            fontSize = 11.sp,
                                            color = palette.textSecondary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "•",
                                            fontSize = 11.sp,
                                            color = palette.textTertiary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${toPersianDigits(session.turns.size.toString())} پیام",
                                            fontSize = 11.sp,
                                            color = palette.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteSession(session.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف گفتگو",
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // دکمه پاک‌سازی تاریخچه
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "پاک‌سازی تمام گفتگوهای ذخیره‌شده",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}





/**
 * تبدیل زمان میلی‌ثانیه‌ای به برچسب فارسی و روان (هم‌اکنون، ۵ دقیقه پیش، دیروز، ...)
 */
fun formatRelativeTimeFa(timestamp: Long): String {
    val diffMillis = System.currentTimeMillis() - timestamp
    val diffMinutes = (diffMillis / (60 * 1000)).coerceAtLeast(0)
    val diffHours = diffMillis / (60 * 60 * 1000)
    val diffDays = diffMillis / (24 * 60 * 60 * 1000)
    return when {
        diffMinutes < 1 -> "هم‌اکنون"
        diffMinutes < 60 -> "${toPersianDigits(diffMinutes.toString())} دقیقه پیش"
        diffHours < 24 -> "${toPersianDigits(diffHours.toString())} ساعت پیش"
        diffDays == 1L -> "دیروز"
        diffDays < 7 -> "${toPersianDigits(diffDays.toString())} روز پیش"
        else -> "${toPersianDigits((diffDays / 7).toString())} هفته پیش"
    }
}

/**
 * کامپوننت رندر کارت‌های عملیات فارم درون جریان مکالمه هوش مصنوعی
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceActionCardView(
    card: VoiceActionCardData,
    isActionable: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onQuickReply: (String) -> Unit,
    onOpenHealthClinic: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (card.needsMoreInfo) Color(0xFF132238) else if (card.isValid) Color(0xFF17291F) else Color(0xFF2A1C1D),
        border = BorderStroke(
            1.5.dp,
            if (card.needsMoreInfo) Color(0xFF3B82F6) else if (card.isValid) Color(0xFF10B981) else Color(0xFFEF4444).copy(alpha = 0.8f)
        ),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // سربرگ کارت
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = if (card.needsMoreInfo) painterResource(id = R.drawable.ic_farm_dashboard) else painterResource(id = card.iconRes),
                    contentDescription = null,
                    tint = if (card.needsMoreInfo) Color(0xFF60A5FA) else if (card.isValid) Color(0xFF34D399) else Color(0xFFF87171),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = if (card.needsMoreInfo) Color(0xFF1E3A5F) else if (card.isValid) Color(0xFF284835) else Color(0xFF452427)
            )

            if (card.needsMoreInfo) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = card.missingPromptFa ?: "لطفاً اطلاعات تکمیلی را بفرمایید:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBFDBFE),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (card.quickReplies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        card.quickReplies.forEach { replyText ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.8f)),
                                modifier = Modifier.clickable { onQuickReply(replyText) }
                            ) {
                                Text(
                                    text = replyText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDBEAFE),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // هشدار پیشگیرانه ناهنجاری و امنیت زیستی (جهش تلفات / افت تولید / دوره پرهیز)
                val anomalyAlert = card.parameters["anomalyAlert"] as? String
                val withdrawalAlert = card.parameters["withdrawalAlert"] as? String
                val proactiveWarning = anomalyAlert ?: withdrawalAlert
                if (!proactiveWarning.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF7C2D12).copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color(0xFFF97316)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFDBA74), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = proactiveWarning,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFEDD5),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // نشان استدلال ایجنتیک یا متادیتا
                if (card.parameters["agenticEvaluation"] == true) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF047857).copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "🤖 استدلال و ارزیابی تشخیصی ایجنتیک",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (card.parameters["rootCauseAnalysis"] == true) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0369A1).copy(alpha = 0.35f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "🔍 تحلیل ریشه‌ای تاریخچه فارم",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (card.parameters["knowledge"] == true) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFD97706).copy(alpha = 0.35f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "📚 دایره‌المعارف پرورش طیور",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // نمایش متریک و مشخصات اقدام
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (card.isValid) Color(0xFF0F1E16) else Color(0xFF241315),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = card.primaryMetric,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (card.isValid) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                        )
                    }
                }

                // تفکیک زیردستورهای عملیات چندگانه (Compound Sub-Actions Breakdown)
                @Suppress("UNCHECKED_CAST")
                val subActions = card.parameters["subActions"] as? List<VoiceActionCardData>
                if (!subActions.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subActions.forEachIndexed { index, subCard ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF13281E),
                                border = BorderStroke(1.dp, Color(0xFF1E4530)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = subCard.iconRes),
                                        contentDescription = null,
                                        tint = FarmGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${toPersianDigits(index + 1)}. ${subCard.title}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = subCard.primaryMetric,
                                            fontSize = 11.sp,
                                            color = Color(0xFFA7F3D0)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // جزئیات تکمیلی پارامترها
                card.details.forEach { (label, value) ->
                    val isLongDetail = value.length > 25 || label.length > 18
                    if (isLongDetail) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$label:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$label:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (card.isMutatingAction) Color(0xFF0F1E16) else Color(0xFF0C1929),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val effectPrefix = if (card.isMutatingAction) "⚡ اثر سیستمی: " else "💡 خروجی تحلیلی: "
                    Text(
                        text = "$effectPrefix${card.systemEffect}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = if (card.isMutatingAction) Color(0xFF34D399) else Color(0xFF38BDF8),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (card.intentType == VoiceIntentType.DIAGNOSE_DISEASE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenHealthClinic,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF0F291E),
                            contentColor = Color(0xFF6EE7B7)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "ورود به کلینیک سلامت و پرونده بیماری",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (card.quickReplies.isNotEmpty() && !card.needsMoreInfo) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "گزینه‌های پیشنهادی (جهت انتخاب لمس فرمایید):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        card.quickReplies.forEach { replyText ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.8f)),
                                modifier = Modifier.clickable { onQuickReply(replyText) }
                            ) {
                                Text(
                                    text = replyText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDBEAFE),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                if (isActionable && card.isMutatingAction) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "تایید و اجرا",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "انصراف",
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ویژوالایزر صوتی امواج زنده (Audio Waveform Visualizer)
 * رسم بوم گرافیکی داینامیک با ۲۸ میله نوسان‌گر همگام با دامنه صدای میکروفن و وضعیت دستیار
 */
@Composable
fun VoiceWaveformVisualizer(
    soundLevel: Float,
    voiceState: VoiceState,
    modifier: Modifier = Modifier
) {
    val barCount = 28
    val infiniteTransition = rememberInfiniteTransition(label = "waveformTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveformPhase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = (width / (barCount * 1.6f)).coerceAtLeast(3f)
        val spacing = (width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1).toFloat()

        val baseColor = when (voiceState) {
            VoiceState.LISTENING -> Color(0xFF10B981) // سبز نئونی
            VoiceState.THINKING -> Color(0xFF38BDF8) // فیروزه‌ای پردازش
            VoiceState.SPEAKING -> Color(0xFF34D399) // سبز روشن پاسخ‌دهی
            VoiceState.AWAITING_CONFIRMATION -> Color(0xFFF59E0B) // کهربایی تایید
            else -> Color(0xFF065F46) // سبز ملو آمادگی
        }

        for (i in 0 until barCount) {
            val normalizedIdx = (i - barCount / 2f) / (barCount / 2f)
            val bellCurve = (1f - (normalizedIdx * normalizedIdx)).coerceIn(0.18f, 1f)

            val animatedFactor = when (voiceState) {
                VoiceState.LISTENING -> {
                    val wave = (kotlin.math.sin(phase + i * 0.45) * 0.35 + 0.65).toFloat()
                    val amp = (soundLevel * 1.8f).coerceIn(0.1f, 1f)
                    (amp * wave * bellCurve).coerceIn(0.15f, 1f)
                }
                VoiceState.THINKING -> {
                    val wave = (kotlin.math.sin(phase * 1.6 + i * 0.32) * 0.4 + 0.5).toFloat()
                    (0.38f * wave * bellCurve).coerceIn(0.18f, 0.75f)
                }
                VoiceState.SPEAKING -> {
                    val wave = (kotlin.math.abs(kotlin.math.sin(phase * 2.0 + i * 0.5)) * 0.7 + 0.3).toFloat()
                    (0.75f * wave * bellCurve).coerceIn(0.22f, 0.95f)
                }
                else -> 0.14f * bellCurve
            }

            val barHeight = (height * animatedFactor).coerceIn(4f, height)
            val left = i * (barWidth + spacing)
            val top = (height - barHeight) / 2f

            drawRoundRect(
                color = baseColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

/**
 * تولید چیپ‌های پیشنهادی صوتی پویا (Contextual Suggestion Chips)
 * متناسب با نقش کاربر (کارگر/مدیر)، ساعت شبانه‌روز و سالن فعال
 */
fun getContextualSuggestions(userRole: UserRole, flockName: String?): List<String> {
    val flock = flockName ?: "گله بلک استار"
    val cal = java.util.Calendar.getInstance()
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val isWorker = (userRole == UserRole.WORKER)

    return when {
        hour < 12 -> {
            if (isWorker) {
                listOf(
                    "دمای هوای $flock مناسب است؟",
                    "تخم‌مرغ نوبت صبح $flock",
                    "۴۰ کیلو دان دادیم $flock",
                    "تلفات نوبت صبح ۲ تا",
                    "یادداشت سالن: تهویه بررسی شد"
                )
            } else {
                listOf(
                    "دمای هوای گله بلک استار مناسب است؟",
                    "امروز $flock: ۱۲۰ تا تخم‌مرغ و ۵۰ کیلو دان",
                    "تلفات امروز چقدر بود؟",
                    "ضریب تبدیل $flock چنده؟",
                    "یادآور واکسن فردا ساعت ۹ صبح",
                    "موجودی دان در انبار"
                )
            }
        }
        hour in 12..17 -> {
            if (isWorker) {
                listOf(
                    "دمای هوای $flock مناسب است؟",
                    "بررسی دمای $flock",
                    "شارژ آبخوری‌های $flock",
                    "۳۰ کیلو دان توزیع شد",
                    "گزارش شیفت: پرنده‌ها سرحال هستند"
                )
            } else {
                listOf(
                    "دمای هوای گله بلک استار مناسب است؟",
                    "درصد تولید امروز $flock",
                    "نسبت آب به دان $flock درسته؟",
                    "دیروز ۵۰ تا تخم‌مرغ $flock",
                    "وضعیت آب و دان سالن‌ها"
                )
            }
        }
        else -> {
            if (isWorker) {
                listOf(
                    "دمای هوای $flock مناسب است؟",
                    "تخم‌مرغ نوبت عصر $flock",
                    "تلفات نوبت عصر",
                    "گزارش پایان شیفت: فن‌ها سرویس شدند"
                )
            } else {
                listOf(
                    "دمای هوای گله بلک استار مناسب است؟",
                    "جمع‌بندی آمار امروز",
                    "درصد تلفات $flock چقدره؟",
                    "پنجشنبه ساعت ۱۰ یادآور ویتامین",
                    "دستور قبلی رو برگردون"
                )
            }
        }
    }
}


