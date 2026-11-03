package com.example.poultryapp

import com.example.poultryapp.voice.PersianVoiceNormalizer
import com.example.poultryapp.voice.PoultryPhoneticCorrector
import org.junit.Assert.*
import org.junit.Test

class PoultryPhoneticCorrectorTest {

    @Test
    fun testDamerauLevenshteinDistance_AndSimilarity() {
        val dist1 = PoultryPhoneticCorrector.damerauLevenshteinDistance("انروفلوکساسین", "انروفلوکساسین")
        assertEquals(0, dist1)

        // Transposition test: "کوکسیدیوز" vs "کوکیسدیوز"
        val dist2 = PoultryPhoneticCorrector.damerauLevenshteinDistance("کوکسیدیوز", "کوکیسدیوز")
        assertEquals(1, dist2)

        val sim = PoultryPhoneticCorrector.similarity("کوکسیدیوز", "کوکیسدیوز")
        assertTrue("Similarity with single transposition should be high", sim > 0.85)
    }

    @Test
    fun testPhoneticNormalize_Homophones() {
        // س / ص / ث
        assertEquals(
            PoultryPhoneticCorrector.phoneticNormalize("سین"),
            PoultryPhoneticCorrector.phoneticNormalize("صین")
        )
        assertEquals(
            PoultryPhoneticCorrector.phoneticNormalize("ثبت"),
            PoultryPhoneticCorrector.phoneticNormalize("سبت")
        )

        // ت / ط
        assertEquals(
            PoultryPhoneticCorrector.phoneticNormalize("طوطی"),
            PoultryPhoneticCorrector.phoneticNormalize("توتی")
        )
    }

    @Test
    fun testSpeechErrorsCorrection_PoultryTerms() {
        val corrected1 = PoultryPhoneticCorrector.correctSpeechErrors("جوجه های سویا راس سالن یک")
        assertTrue("Should convert 'سویا راس' to 'سویه راس'", corrected1.contains("سویه راس"))

        val corrected2 = PoultryPhoneticCorrector.correctSpeechErrors("علائم بیماری کوک سید یوز چیست")
        assertTrue("Should merge broken 'کوک سید یوز' into 'کوکسیدیوز'", corrected2.contains("کوکسیدیوز"))

        val corrected3 = PoultryPhoneticCorrector.correctSpeechErrors("شکم جوجه ها باد کرده و اب اورده و اسید گرفتن")
        assertTrue("Should correct 'اسید' to 'آسیت' when abdomen ascites symptoms are present", corrected3.contains("آسیت"))
    }

    @Test
    fun testPersianVoiceNormalizer_Integration() {
        val normalized = PersianVoiceNormalizer.normalize("جوجه سویا کاب 500 در سولن ۲")
        assertTrue(normalized.contains("سویه کاب"))
        assertTrue(normalized.contains("سالن 2"))
    }
}

