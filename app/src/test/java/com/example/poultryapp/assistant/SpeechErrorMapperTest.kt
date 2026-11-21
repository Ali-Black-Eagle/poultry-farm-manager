package com.example.poultryapp.assistant

import com.example.poultryapp.assistant.voice.AttemptStage
import com.example.poultryapp.assistant.voice.SpeechErrorMapper
import com.example.poultryapp.assistant.voice.SpeechEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechErrorMapperTest {

    @Test
    fun `maps all standard Android speech error codes to localized Persian text`() {
        val codes = (1..14).toList()
        for (code in codes) {
            val msg = SpeechErrorMapper.messageFor(code)
            assertTrue("Message for code $code should be non-blank", msg.isNotBlank())
            assertFalse("Message for known code $code should not fall back to raw numeric code", msg.contains("(کد: $code)"))
        }

        assertTrue(SpeechErrorMapper.messageFor(12).contains("فارسی"))
        assertTrue(SpeechErrorMapper.messageFor(13).contains("فارسی"))
        assertTrue(SpeechErrorMapper.messageFor(9).contains("میکروفون"))
        assertTrue(SpeechErrorMapper.messageFor(2).contains("اینترنت"))
        assertEquals("پیام سفارشی", SpeechErrorMapper.messageFor(-1, "پیام سفارشی"))
        assertTrue(SpeechErrorMapper.messageFor(-1).isNotBlank())
        assertTrue(SpeechErrorMapper.messageFor(99).contains("99"))
    }

    @Test
    fun `classifies language unsupported network and timeout errors accurately`() {
        assertTrue(SpeechErrorMapper.isLanguageUnsupported(12))
        assertTrue(SpeechErrorMapper.isLanguageUnsupported(13))
        assertTrue(SpeechErrorMapper.isLanguageUnsupported(14))
        assertFalse(SpeechErrorMapper.isLanguageUnsupported(2))
        assertFalse(SpeechErrorMapper.isLanguageUnsupported(7))
        assertFalse(SpeechErrorMapper.isLanguageUnsupported(9))

        assertTrue(SpeechErrorMapper.isNetworkError(1))
        assertTrue(SpeechErrorMapper.isNetworkError(2))
        assertFalse(SpeechErrorMapper.isNetworkError(12))

        assertTrue(SpeechErrorMapper.isSpeechTimeoutOrNoMatch(6))
        assertTrue(SpeechErrorMapper.isSpeechTimeoutOrNoMatch(7))
        assertFalse(SpeechErrorMapper.isSpeechTimeoutOrNoMatch(12))

        assertTrue(SpeechEvent.Error(12, "err").isLanguageUnsupported)
        assertTrue(SpeechEvent.Error(13, "err").isLanguageUnsupported)
        assertFalse(SpeechEvent.Error(7, "err").isLanguageUnsupported)
    }

    @Test
    fun `nextFallbackStage handles Google fallback offline retry and terminates cleanly`() {
        // Error 12/13/14 from default recognizer falls back to Google RecognitionService when installed
        assertEquals(
            AttemptStage.GOOGLE_SERVICE,
            SpeechErrorMapper.nextFallbackStage(12, AttemptStage.DEFAULT_HYBRID, hasGoogleService = true)
        )
        assertEquals(
            AttemptStage.GOOGLE_SERVICE,
            SpeechErrorMapper.nextFallbackStage(13, AttemptStage.DEFAULT_HYBRID, hasGoogleService = true)
        )
        assertNull(
            SpeechErrorMapper.nextFallbackStage(12, AttemptStage.DEFAULT_HYBRID, hasGoogleService = false)
        )

        // Stepping through multiple Google service candidates terminates once exhausted
        assertEquals(
            AttemptStage.GOOGLE_SERVICE,
            SpeechErrorMapper.nextFallbackStage(
                errorCode = 12,
                currentStage = AttemptStage.GOOGLE_SERVICE,
                hasGoogleService = true,
                hasMoreGoogleCandidates = true
            )
        )
        assertNull(
            SpeechErrorMapper.nextFallbackStage(
                errorCode = 12,
                currentStage = AttemptStage.GOOGLE_SERVICE,
                hasGoogleService = true,
                hasMoreGoogleCandidates = false
            )
        )

        // Network errors retry once in offline mode
        assertEquals(
            AttemptStage.OFFLINE_RETRY,
            SpeechErrorMapper.nextFallbackStage(2, AttemptStage.DEFAULT_HYBRID, hasGoogleService = true)
        )
        assertEquals(
            AttemptStage.OFFLINE_RETRY,
            SpeechErrorMapper.nextFallbackStage(1, AttemptStage.GOOGLE_SERVICE, hasGoogleService = true)
        )

        // Offline retry never loops on any error
        assertNull(SpeechErrorMapper.nextFallbackStage(2, AttemptStage.OFFLINE_RETRY, hasGoogleService = true))
        assertNull(SpeechErrorMapper.nextFallbackStage(12, AttemptStage.OFFLINE_RETRY, hasGoogleService = true))

        // Non-recoverable client/permission/no-match errors do not retry
        assertNull(SpeechErrorMapper.nextFallbackStage(9, AttemptStage.DEFAULT_HYBRID, hasGoogleService = true))
        assertNull(SpeechErrorMapper.nextFallbackStage(7, AttemptStage.DEFAULT_HYBRID, hasGoogleService = true))
        assertNull(SpeechErrorMapper.nextFallbackStage(6, AttemptStage.DEFAULT_HYBRID, hasGoogleService = true))
        assertNull(SpeechErrorMapper.nextFallbackStage(8, AttemptStage.DEFAULT_HYBRID, hasGoogleService = true))
    }
}

