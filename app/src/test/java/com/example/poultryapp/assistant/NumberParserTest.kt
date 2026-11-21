package com.example.poultryapp.assistant

import com.example.poultryapp.assistant.core.entities.NumberParser
import org.junit.Assert.*
import org.junit.Test

class NumberParserTest {

    @Test
    fun testPersianNumberWordsTable() {
        val testCases = listOf(
            "صفر" to 0.0, "یک" to 1.0, "دو" to 2.0, "سه" to 3.0, "چهار" to 4.0,
            "پنج" to 5.0, "شش" to 6.0, "شیش" to 6.0, "هفت" to 7.0, "هشت" to 8.0, "نه" to 9.0,
            "ده" to 10.0, "یازده" to 11.0, "دوازده" to 12.0, "سیزده" to 13.0, "چهارده" to 14.0,
            "پانزده" to 15.0, "پونزده" to 15.0, "شانزده" to 16.0, "شونزده" to 16.0, "هفده" to 17.0,
            "هجده" to 18.0, "نوزده" to 19.0, "بیست" to 20.0, "سی" to 30.0, "چهل" to 40.0,
            "پنجاه" to 50.0, "شصت" to 60.0, "هفتاد" to 70.0, "هشتاد" to 80.0, "نود" to 90.0,
            "صد" to 100.0, "یکصد" to 100.0, "دویست" to 200.0, "سیصد" to 300.0, "چهارصد" to 400.0,
            "پانصد" to 500.0, "پونصد" to 500.0, "ششصد" to 600.0, "هفتصد" to 700.0, "هشتصد" to 800.0,
            "نهصد" to 900.0, "هزار" to 1000.0, "میلیون" to 1000000.0, "میلیارد" to 1000000000.0
        )

        for ((phrase, expected) in testCases) {
            val extracted = NumberParser.extractNumbers(phrase)
            assertFalse("Failed for '$phrase'", extracted.isEmpty())
            assertEquals("Value mismatch for '$phrase'", expected, extracted.first().value, 0.001)
        }
    }

    @Test
    fun testCompoundPersianNumbers() {
        val compoundCases = listOf(
            "صد و بیست" to 120.0,
            "دو هزار و پانصد" to 2500.0,
            "یک میلیون و پانصد هزار" to 1500000.0,
            "یک میلیون و نیم" to 1500000.0,
            "یک و نیم میلیون" to 1500000.0,
            "دو میلیون و نیم" to 2500000.0,
            "پانصد و پنجاه هزار" to 550000.0,
            "پونصد هزار" to 500000.0,
            "دویست و سی و پنج" to 235.0,
            "سه هزار و دویست و چهل و پنج" to 3245.0,
            "ده میلیون و دویست هزار" to 10200000.0
        )

        for ((phrase, expected) in compoundCases) {
            val extracted = NumberParser.extractNumbers(phrase)
            assertFalse("Failed for compound phrase '$phrase'", extracted.isEmpty())
            assertEquals("Mismatch for '$phrase'", expected, extracted.first().value, 0.001)
        }
    }

    @Test
    fun testEnglishNumbersAndSuffixes() {
        val englishCases = listOf(
            "120" to 120.0,
            "2500" to 2500.0,
            "1.5" to 1.5,
            "2k" to 2000.0,
            "1.5m" to 1500000.0,
            "3 million" to 3000000.0,
            "one hundred twenty" to 120.0,
            "two thousand five hundred" to 2500.0,
            "fifteen hundred" to 1500.0,
            "forty five" to 45.0,
            "one million and five hundred thousand" to 1500000.0
        )

        for ((phrase, expected) in englishCases) {
            val extracted = NumberParser.extractNumbers(phrase)
            assertFalse("Failed for English '$phrase'", extracted.isEmpty())
            assertEquals("Mismatch for '$phrase'", expected, extracted.first().value, 0.001)
        }
    }
}

