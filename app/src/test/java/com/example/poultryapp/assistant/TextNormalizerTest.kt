package com.example.poultryapp.assistant

import com.example.poultryapp.assistant.core.normalize.Lang
import com.example.poultryapp.assistant.core.normalize.TextNormalizer
import org.junit.Assert.*
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun testArabicToPersianHarmonization() {
        val raw = "ي ك أ إ ة"
        val norm = TextNormalizer.normalize(raw)
        assertEquals("ی ک ا ا ه", norm.normalized)
    }

    @Test
    fun testPersianAndArabicDigitsToAscii() {
        val raw = "۱۲۳۴۵۶۷۸۹۰ و ٠١٢٣٤٥٦٧٨٩"
        val norm = TextNormalizer.normalize(raw)
        assertEquals("1234567890 و 0123456789", norm.normalized)
    }

    @Test
    fun testMomayyezAndThousandsSeparator() {
        val raw = "۱٬۵۰۰٬۰۰۰ و ۱٫۵"
        val norm = TextNormalizer.normalize(raw)
        assertEquals("1500000 و 1.5", norm.normalized)
    }

    @Test
    fun testPersianCounterSplit() {
        val raw = "۱۲۰تا تخم مرغ"
        val norm = TextNormalizer.normalize(raw)
        assertTrue(norm.normalized.contains("120 تا"))
    }

    @Test
    fun testDiacriticsAndTatweelRemoved() {
        val raw = "تَخْمِـ مَرْغ"
        val norm = TextNormalizer.normalize(raw)
        assertEquals("تخم مرغ", norm.normalized)
    }

    @Test
    fun testZwnjHandling() {
        val raw1 = "تخم‌مرغ"
        val raw2 = "تخم مرغ"
        val raw3 = "تخممرغ"
        val n1 = TextNormalizer.normalize(raw1)
        val n2 = TextNormalizer.normalize(raw2)
        val n3 = TextNormalizer.normalize(raw3)

        assertEquals("تخممرغ", n1.matchKey)
        assertEquals("تخم مرغ", n2.matchKey)
        assertEquals("تخممرغ", n3.matchKey)
        assertTrue(n1.tokens.contains("تخم") && n1.tokens.contains("مرغ"))
    }

    @Test
    fun testLanguageDetection() {
        val fa1 = TextNormalizer.normalize("امروز ۱۲۰ تا تخم مرغ اضافه کن")
        val en1 = TextNormalizer.normalize("add 120 eggs today")
        val mixed = TextNormalizer.normalize("add 120 تا تخم مرغ")

        assertEquals(Lang.FA, fa1.lang)
        assertEquals(Lang.EN, en1.lang)
        assertEquals(Lang.FA, mixed.lang)
    }

    @Test
    fun testDamerauLevenshteinDistance() {
        assertEquals(0, TextNormalizer.damerauLevenshteinDistance("egg", "egg"))
        assertEquals(1, TextNormalizer.damerauLevenshteinDistance("tekhm", "tokhm")) // substitution
        assertEquals(1, TextNormalizer.damerauLevenshteinDistance("ca", "ac")) // transposition
        assertTrue(TextNormalizer.isFuzzyMatch("تخممرغ", "تخم مرغ".replace(" ", "")))
        assertTrue(TextNormalizer.isFuzzyMatch("feed", "fead"))
    }
}
