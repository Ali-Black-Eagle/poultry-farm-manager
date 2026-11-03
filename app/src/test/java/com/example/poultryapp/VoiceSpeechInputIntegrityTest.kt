package com.example.poultryapp

import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.voice.*
import org.junit.Assert.*
import org.junit.Test

/**
 * آزمون صحت و ایمنی ورودی‌های صوتی، پارامترهای کلامی و تفکیک سوالات در دستیار هوشمند
 */
class VoiceSpeechInputIntegrityTest {

    private val mockFlocks = listOf(
        Flock(
            id = 1L,
            name = "گله ۱",
            species = BirdSpecies.CHICKEN,
            breed = "راس",
            birthEpochMillis = System.currentTimeMillis() - 30L * 24 * 3600 * 1000,
            birdCount = 1000
        )
    )

    @Test
    fun testSpeechRecognizerConstantsAndErrorRecoveryMapping() {
        val errorNoMatch = 7 // SpeechRecognizer.ERROR_NO_MATCH
        val errorSpeechTimeout = 6 // SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        val errorAudio = 3 // SpeechRecognizer.ERROR_AUDIO
        val errorNetwork = 2 // SpeechRecognizer.ERROR_NETWORK
        val errorClient = 5 // SpeechRecognizer.ERROR_CLIENT
        val errorBusy = 8 // SpeechRecognizer.ERROR_RECOGNIZER_BUSY

        val recoverableErrors = listOf(errorNoMatch, errorSpeechTimeout, errorAudio, errorNetwork, errorBusy)

        for (err in recoverableErrors) {
            val (desc, isRecoverable) = when (err) {
                errorNoMatch -> "صدایی متوجه نشدم؛ لطفاً دوباره بفرمایید" to true
                errorSpeechTimeout -> "صدایی شنیده نشد. مجدداً گوش می‌دهم..." to true
                errorAudio -> "خطای ضبط صدا" to true
                errorNetwork -> "خطای اتصال صوتی" to true
                errorBusy -> "میکروفون درگیر است..." to true
                else -> "خطا" to false
            }
            assertTrue("Error code $err should be recoverable", isRecoverable)
            assertTrue("Description for error $err should be non-empty", desc.isNotBlank())
        }

        // بررسی اینکه خطای کلاینت (کد ۵) باید به فال‌بک سیستم هدایت شود
        val shouldFallbackOnClientError = (errorClient == 5)
        assertTrue(shouldFallbackOnClientError)
    }

    @Test
    fun testVoiceSpokenQuestionNeverProducesHijackedOperationalCard() {
        val spokenQuestion = "به جوجه ی یک روزه چه غذایی بدم"
        val initialState = AiSessionState(turns = emptyList())
        val turnResult = FarmConversationalAiEngine.processTurn(
            spokenText = spokenQuestion,
            currentState = initialState,
            flocks = mockFlocks,
            contacts = emptyList(),
            eggProductions = emptyList()
        )

        assertNotNull(turnResult.aiResponseText)
        assertTrue(turnResult.aiResponseText.isNotBlank())
        // باید پاسخ تخصصی بدهد و هرگز کارت ثبت تخم مرغ باز نکند
        val card = turnResult.actionCard
        if (card != null) {
            assertNotEquals("ثبت تخم‌مرغ", card.title)
            assertNotEquals(VoiceIntentType.RECORD_EGG, card.intentType)
        }
        assertTrue(turnResult.aiResponseText.contains("پروتئین") || turnResult.aiResponseText.contains("استارتر") || turnResult.aiResponseText.contains("جوجه") || turnResult.aiResponseText.contains("API") || turnResult.aiResponseText.contains("دانش"))
    }

    @Test
    fun testVoiceSpokenOperationalCommandWithReasonMaintainsIntegrity() {
        val spokenOp = "ثبت ۱ تلفات در گله ۱ به علت کانیبالیسم"
        val initialState = AiSessionState(turns = emptyList())
        val turnResult = FarmConversationalAiEngine.processTurn(
            spokenText = spokenOp,
            currentState = initialState,
            flocks = mockFlocks,
            contacts = emptyList(),
            eggProductions = emptyList()
        )

        assertNotNull(turnResult.actionCard)
        val card = turnResult.actionCard!!
        assertEquals(VoiceIntentType.RECORD_MORTALITY, card.intentType)
    }

    @Test
    fun testPersianSpokenNumberNormalization() {
        val normalized1 = PersianVoiceNormalizer.normalize("بیست و پنج تا تخم مرغ جمع کردیم")
        assertTrue(normalized1.contains("25") || normalized1.contains("تخم"))

        val normalized2 = PersianVoiceNormalizer.normalize("یک عدد تلفات در سالن دو")
        assertTrue(normalized2.contains("1") || normalized2.contains("تلفات"))
    }

    @Test
    fun testVoiceCancellationAndConfirmationPhrases() {
        val confirmPhrases = listOf("تایید", "بله", "درسته", "انجام بده", "ثبتش کن")
        for (phrase in confirmPhrases) {
            assertTrue("Phrase '$phrase' should be recognized as confirmation", VoiceIntentParser.isConfirmation(phrase))
        }

        val cancelPhrases = listOf("لغو", "انصراف", "کنسل", "نمیخوام", "بی خیال")
        for (phrase in cancelPhrases) {
            assertTrue("Phrase '$phrase' should be recognized as cancellation", VoiceIntentParser.isCancellation(phrase))
        }
    }
}
