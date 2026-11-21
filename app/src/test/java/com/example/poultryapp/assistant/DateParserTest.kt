package com.example.poultryapp.assistant

import com.example.poultryapp.assistant.core.entities.DateParser
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DateParserTest {

    private val fixedNow = LocalDate.of(2026, 11, 7) // 1405/08/16 Jalali

    @Test
    fun testRelativeDates() {
        val todayFa = DateParser.extractDates("امروز ۱۲۰ تا تخم ثبت کن", fixedNow)
        assertEquals(1, todayFa.size)
        assertEquals(fixedNow, todayFa.first().date)

        val yesterdayFa = DateParser.extractDates("دیروز جمع کردم", fixedNow)
        assertEquals(1, yesterdayFa.size)
        assertEquals(fixedNow.minusDays(1), yesterdayFa.first().date)

        val dayBeforeFa = DateParser.extractDates("پریروز", fixedNow)
        assertEquals(1, dayBeforeFa.size)
        assertEquals(fixedNow.minusDays(2), dayBeforeFa.first().date)

        val threeDaysAgoFa = DateParser.extractDates("۳ روز پیش", fixedNow)
        assertEquals(1, threeDaysAgoFa.size)
        assertEquals(fixedNow.minusDays(3), threeDaysAgoFa.first().date)

        val todayEn = DateParser.extractDates("collected 90 eggs today", fixedNow)
        assertEquals(fixedNow, todayEn.first().date)

        val yesterdayEn = DateParser.extractDates("log 80 eggs yesterday", fixedNow)
        assertEquals(fixedNow.minusDays(1), yesterdayEn.first().date)
    }

    @Test
    fun testJalaliDateConversion() {
        // 2026-11-07 corresponds to 1405/08/16 in Jalali
        val (jy, jm, jd) = DateParser.gregorianToJalali(2026, 11, 7)
        assertEquals(1405, jy)
        assertEquals(8, jm)
        assertEquals(16, jd)

        val backGreg = DateParser.jalaliToGregorian(1405, 8, 16)
        assertNotNull(backGreg)
        assertEquals(LocalDate.of(2026, 11, 7), backGreg)
    }

    @Test
    fun testNumericJalaliDateParsing() {
        val parsed = DateParser.extractDates("تاریخ ۱۴۰۵/۰۸/۱۶ ثبت شد", fixedNow)
        assertEquals(1, parsed.size)
        assertEquals(LocalDate.of(2026, 11, 7), parsed.first().date)
    }

    @Test
    fun testPeriods() {
        val thisWeek = DateParser.extractDates("تخم این هفته چقدر بود", fixedNow)
        assertEquals(1, thisWeek.size)
        assertTrue(thisWeek.first().isPeriod)

        val lastMonth = DateParser.extractDates("هزینه ماه گذشته", fixedNow)
        assertEquals(1, lastMonth.size)
        assertTrue(lastMonth.first().isPeriod)
    }
}

