package com.example.poultryapp.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * مدیریت پخش صوتی گفتار (TTS فارسی آفلاین) برای دستیار هوشمند اپلیکیشن مرغداری.
 * این کلاس پاسخ‌ها و سوالات دستیار را با صدای فارسی پخش کرده و کنترل‌های Mute و چرخه حیات را فراهم می‌کند.
 */
class VoiceSpeaker(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = try {
        TextToSpeech(context.applicationContext, this)
    } catch (_: Exception) {
        null
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    var isInitialized: Boolean = false
        private set

    var isMuted: Boolean = false

    var isLanguageSupported: Boolean = true
        private set

    private var onSpeechDoneCallback: (() -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val farsiLocale = Locale.forLanguageTag("fa")
            var langResult = tts?.setLanguage(farsiLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // تلاش برای گونه fa-IR
                val farsiIrLocale = Locale.forLanguageTag("fa-IR")
                langResult = tts?.setLanguage(farsiIrLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    isLanguageSupported = false
                } else {
                    isLanguageSupported = true
                }
            } else {
                isLanguageSupported = true
            }

            try {
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.0f)
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            try {
                                onSpeechDoneCallback?.invoke()
                            } catch (_: Throwable) {}
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            try {
                                onSpeechDoneCallback?.invoke()
                            } catch (_: Throwable) {}
                        }
                    }
                })
            } catch (_: Exception) {}

            isInitialized = true
        } else {
            isLanguageSupported = false
            isInitialized = false
        }
    }

    /**
     * قرائت متن فارسی با فیلتر کردن علائم نگارشی نامناسب برای خوانش صوتی
     * به همراه callback پایان خوانش صوتی جهت هماهنگی با مکالمه پیوسته
     */
    fun speak(text: String?, onDone: (() -> Unit)? = null) {
        onSpeechDoneCallback = onDone
        if (isMuted || !isInitialized || text.isNullOrBlank()) {
            mainHandler.post { try { onDone?.invoke() } catch (_: Throwable) {} }
            return
        }
        if (!isLanguageSupported) {
            mainHandler.post { try { onDone?.invoke() } catch (_: Throwable) {} }
            return
        }

        val cleanText = cleanForSpeech(text)
        if (cleanText.isBlank()) {
            mainHandler.post { try { onDone?.invoke() } catch (_: Throwable) {} }
            return
        }

        try {
            val utteranceId = "VOICE_SPEAKER_UTTERANCE_${System.currentTimeMillis()}"
            val res = tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (res != TextToSpeech.SUCCESS) {
                mainHandler.post { try { onDone?.invoke() } catch (_: Throwable) {} }
            }
        } catch (_: Exception) {
            mainHandler.post { try { onDone?.invoke() } catch (_: Throwable) {} }
        }
    }

    /**
     * قطع فوری پخش صدا (مثلاً هنگام شروع به صحبت کاربر)
     */
    fun stop() {
        try {
            onSpeechDoneCallback = null
            tts?.stop()
        } catch (_: Exception) {}
    }

    /**
     * آزادسازی کامل منابع TTS هنگام بسته شدن شیت یا خروج
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }

    companion object {
        fun cleanForSpeech(input: String): String {
            return input
                .replace(Regex("[📋⚠️✅🎯💡*#_`~—•\\[\\]{}|]"), " ")
                .replace(":", " ")
                .replace("  ", " ")
                .trim()
        }
    }
}
