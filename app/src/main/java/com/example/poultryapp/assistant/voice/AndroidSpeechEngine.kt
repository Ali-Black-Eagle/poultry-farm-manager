package com.example.poultryapp.assistant.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.poultryapp.assistant.core.normalize.Lang
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidSpeechEngine(
    private val context: Context
) : SpeechEngine {

    private val appContext: Context = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var activeRecognizer: SpeechRecognizer? = null

    override suspend fun availability(lang: Lang): SpeechAvailability {
        val defaultAvailable = runCatching { SpeechRecognizer.isRecognitionAvailable(appContext) }
            .getOrDefault(false)
        if (defaultAvailable || findGoogleRecognitionComponents(appContext).isNotEmpty()) {
            return SpeechAvailability.Ready
        }
        return SpeechAvailability.Unsupported
    }

    override fun listen(lang: Lang): Flow<SpeechEvent> = callbackFlow {
        val defaultAvailable = runCatching { SpeechRecognizer.isRecognitionAvailable(appContext) }
            .getOrDefault(false)
        val googleCandidates = findGoogleRecognitionComponents(appContext)

        if (!defaultAvailable && googleCandidates.isEmpty()) {
            trySend(
                SpeechEvent.Error(
                    errorCode = -1,
                    message = SpeechErrorMapper.messageFor(-1, "سرویس تشخیص گفتار روی این دستگاه فعال نیست."),
                    isLanguageUnsupported = true
                )
            )
            close()
            awaitClose {}
            return@callbackFlow
        }

        var isFinished = false
        var currentStage = AttemptStage.DEFAULT_HYBRID
        var activeComponent: ComponentName? = null
        var nextGoogleIndex = 0
        var lastPartialText = ""
        var attemptToken = 0

        fun finishWithError(errorCode: Int, customMessage: String? = null, forceFallbackBanner: Boolean = false) {
            if (isFinished) return
            isFinished = true
            val isUnsupported = forceFallbackBanner || SpeechErrorMapper.isLanguageUnsupported(errorCode)
            trySend(
                SpeechEvent.Error(
                    errorCode = errorCode,
                    message = SpeechErrorMapper.messageFor(errorCode, customMessage),
                    isLanguageUnsupported = isUnsupported
                )
            )
            close()
        }

        fun finishWithFinal(text: String) {
            if (isFinished) return
            isFinished = true
            trySend(SpeechEvent.Final(text.trim()))
            close()
        }

        fun startAttempt(stage: AttemptStage, component: ComponentName?) {
            if (isFinished) return
            cleanupRecognizer(activeRecognizer)
            activeRecognizer = null

            val myToken = ++attemptToken
            currentStage = stage
            activeComponent = component

            val recognizer = try {
                if (component != null) {
                    SpeechRecognizer.createSpeechRecognizer(appContext, component)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(appContext)
                }
            } catch (e: Exception) {
                if ((stage == AttemptStage.DEFAULT_HYBRID || stage == AttemptStage.GOOGLE_SERVICE) &&
                    nextGoogleIndex < googleCandidates.size
                ) {
                    startAttempt(AttemptStage.GOOGLE_SERVICE, googleCandidates[nextGoogleIndex++])
                } else {
                    finishWithError(
                        errorCode = -1,
                        customMessage = e.localizedMessage ?: "امکان راه‌اندازی تشخیص گفتار وجود ندارد.",
                        forceFallbackBanner = true
                    )
                }
                return
            }

            activeRecognizer = recognizer

            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    if (isFinished || myToken != attemptToken) return
                    trySend(SpeechEvent.Level(rmsdB))
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (isFinished || myToken != attemptToken) return

                    if (SpeechErrorMapper.isSpeechTimeoutOrNoMatch(error) && lastPartialText.isNotBlank()) {
                        finishWithFinal(lastPartialText)
                        return
                    }

                    val nextStage = SpeechErrorMapper.nextFallbackStage(
                        errorCode = error,
                        currentStage = currentStage,
                        hasGoogleService = nextGoogleIndex < googleCandidates.size,
                        hasMoreGoogleCandidates = nextGoogleIndex < googleCandidates.size
                    )

                    when (nextStage) {
                        AttemptStage.GOOGLE_SERVICE -> {
                            val nextComponent = googleCandidates.getOrNull(nextGoogleIndex++)
                            if (nextComponent != null) {
                                startAttempt(AttemptStage.GOOGLE_SERVICE, nextComponent)
                            } else {
                                finishWithError(error)
                            }
                        }

                        AttemptStage.OFFLINE_RETRY -> {
                            startAttempt(AttemptStage.OFFLINE_RETRY, activeComponent)
                        }

                        AttemptStage.DEFAULT_HYBRID, null -> {
                            finishWithError(error)
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    if (isFinished || myToken != attemptToken) return
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val resolvedText = matches?.firstOrNull { it.isNotBlank() }
                        ?: lastPartialText.takeIf { it.isNotBlank() }

                    if (!resolvedText.isNullOrBlank()) {
                        finishWithFinal(resolvedText)
                    } else {
                        finishWithError(SpeechRecognizer.ERROR_NO_MATCH)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (isFinished || myToken != attemptToken) return
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull { it.isNotBlank() }.orEmpty()
                    if (text.isNotBlank()) {
                        lastPartialText = text
                        trySend(SpeechEvent.Partial(text))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }

            recognizer.setRecognitionListener(listener)

            val intent = buildRecognitionIntent(
                lang = lang,
                preferOffline = (stage == AttemptStage.OFFLINE_RETRY)
            )

            try {
                recognizer.startListening(intent)
            } catch (e: Exception) {
                if ((stage == AttemptStage.DEFAULT_HYBRID || stage == AttemptStage.GOOGLE_SERVICE) &&
                    nextGoogleIndex < googleCandidates.size
                ) {
                    startAttempt(AttemptStage.GOOGLE_SERVICE, googleCandidates[nextGoogleIndex++])
                } else {
                    finishWithError(
                        errorCode = -1,
                        customMessage = e.localizedMessage ?: "خطا در شروع ضبط صدا",
                        forceFallbackBanner = true
                    )
                }
            }
        }

        runOnMain {
            if (defaultAvailable) {
                startAttempt(AttemptStage.DEFAULT_HYBRID, null)
            } else if (googleCandidates.isNotEmpty()) {
                startAttempt(AttemptStage.GOOGLE_SERVICE, googleCandidates[nextGoogleIndex++])
            }
        }

        awaitClose {
            isFinished = true
            runOnMain {
                cleanupRecognizer(activeRecognizer)
                activeRecognizer = null
            }
        }
    }

    override fun stop() {
        runOnMain {
            try {
                activeRecognizer?.stopListening()
            } catch (_: Exception) {}
        }
    }

    private fun buildRecognitionIntent(lang: Lang, preferOffline: Boolean): Intent {
        val langTag = if (lang == Lang.EN) "en-US" else "fa-IR"
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("fa-IR", "en-US"))
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
        }
    }

    private fun findGoogleRecognitionComponents(context: Context): List<ComponentName> {
        val pm = context.packageManager
        val queryIntent = Intent(RecognitionService.SERVICE_INTERFACE)
        val services = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentServices(
                    queryIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentServices(queryIntent, PackageManager.MATCH_ALL)
            }
        } catch (_: Exception) {
            emptyList()
        }

        // Prefer full Google speech services over com.google.android.as (Android System Intelligence)
        // because Android System Intelligence is on-device only and commonly lacks fa-IR support.
        val preferredPackages = listOf(
            "com.google.android.tts",
            "com.google.android.googlequicksearchbox"
        )

        val candidates = mutableListOf<ComponentName>()
        for (pkg in preferredPackages) {
            services
                .filter { it.serviceInfo?.packageName == pkg }
                .forEach { info ->
                    val si = info.serviceInfo ?: return@forEach
                    val component = ComponentName(si.packageName, si.name)
                    if (!candidates.contains(component)) {
                        candidates.add(component)
                    }
                }
        }
        return candidates
    }

    private fun cleanupRecognizer(recognizer: SpeechRecognizer?) {
        if (recognizer == null) return
        try {
            recognizer.setRecognitionListener(null)
        } catch (_: Exception) {}
        try {
            recognizer.cancel()
        } catch (_: Exception) {}
        try {
            recognizer.destroy()
        } catch (_: Exception) {}
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
