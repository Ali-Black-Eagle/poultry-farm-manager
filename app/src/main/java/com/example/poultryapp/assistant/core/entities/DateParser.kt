package com.example.poultryapp.assistant.core.entities

import java.time.DayOfWeek
import java.time.LocalDate

data class ExtractedDate(
    val date: LocalDate,
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val originalSpan: String,
    val startIndex: Int,
    val endIndex: Int,
    val isPeriod: Boolean = false,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null
)

object DateParser {

    private val JALALI_MONTHS = mapOf(
        "فروردین" to 1, "اردیبهشت" to 2, "خرداد" to 3,
        "تیر" to 4, "مرداد" to 5, "شهریور" to 6,
        "مهر" to 7, "آبان" to 8, "آذر" to 9,
        "دی" to 10, "بهمن" to 11, "اسفند" to 12
    )

    private val PERSIAN_WEEKDAYS = mapOf(
        "شنبه" to DayOfWeek.SATURDAY,
        "یکشنبه" to DayOfWeek.SUNDAY,
        "دوشنبه" to DayOfWeek.MONDAY,
        "سه شنبه" to DayOfWeek.TUESDAY,
        "سهشنبه" to DayOfWeek.TUESDAY,
        "چهارشنبه" to DayOfWeek.WEDNESDAY,
        "پنجشنبه" to DayOfWeek.THURSDAY,
        "جمعه" to DayOfWeek.FRIDAY
    )

    private val ENGLISH_WEEKDAYS = mapOf(
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY
    )

    /**
     * Extracts date spans from normalized text.
     */
    fun extractDates(text: String, now: LocalDate = LocalDate.now()): List<ExtractedDate> {
        val results = mutableListOf<ExtractedDate>()
        val normalized = com.example.poultryapp.assistant.core.normalize.TextNormalizer.normalize(text).normalized
        val lower = normalized.lowercase()

        // 1. Relative keywords: today, yesterday, day before yesterday
        checkRelativeKeyword(lower, "امروز", now, results)
        checkRelativeKeyword(lower, "today", now, results)

        checkRelativeKeyword(lower, "دیروز", now.minusDays(1), results)
        checkRelativeKeyword(lower, "yesterday", now.minusDays(1), results)

        checkRelativeKeyword(lower, "پریروز", now.minusDays(2), results)
        checkRelativeKeyword(lower, "day before yesterday", now.minusDays(2), results)

        // 2. N days ago / N روز پیش
        val nDaysAgoPersian = Regex("""(\d+)\s*روز\s*پیش""")
        for (m in nDaysAgoPersian.findAll(lower)) {
            val days = m.groupValues[1].toLongOrNull() ?: continue
            val target = now.minusDays(days)
            addDate(results, target, m.value, m.range.first, m.range.last + 1)
        }

        val nDaysAgoEnglish = Regex("""(\d+)\s*days?\s*ago""")
        for (m in nDaysAgoEnglish.findAll(lower)) {
            val days = m.groupValues[1].toLongOrNull() ?: continue
            val target = now.minusDays(days)
            addDate(results, target, m.value, m.range.first, m.range.last + 1)
        }

        // 3. This week / last week (Persian week starts Saturday, Gregorian Sunday/Monday)
        val thisWeekSpan = findWeekPeriod(now, isCurrentWeek = true)
        checkPeriodKeyword(lower, "این هفته", thisWeekSpan.first, thisWeekSpan.second, results)
        checkPeriodKeyword(lower, "this week", thisWeekSpan.first, thisWeekSpan.second, results)

        val lastWeekSpan = findWeekPeriod(now.minusWeeks(1), isCurrentWeek = false)
        checkPeriodKeyword(lower, "هفته گذشته", lastWeekSpan.first, lastWeekSpan.second, results)
        checkPeriodKeyword(lower, "هفته پیش", lastWeekSpan.first, lastWeekSpan.second, results)
        checkPeriodKeyword(lower, "last week", lastWeekSpan.first, lastWeekSpan.second, results)

        // 4. This month / last month (Jalali month by default)
        val thisMonthSpan = findMonthPeriod(now, isCurrentMonth = true)
        checkPeriodKeyword(lower, "این ماه", thisMonthSpan.first, thisMonthSpan.second, results)
        checkPeriodKeyword(lower, "this month", thisMonthSpan.first, thisMonthSpan.second, results)

        val lastMonthSpan = findMonthPeriod(now.minusMonths(1), isCurrentMonth = false)
        checkPeriodKeyword(lower, "ماه گذشته", lastMonthSpan.first, lastMonthSpan.second, results)
        checkPeriodKeyword(lower, "ماه پیش", lastMonthSpan.first, lastMonthSpan.second, results)
        checkPeriodKeyword(lower, "last month", lastMonthSpan.first, lastMonthSpan.second, results)

        // 5. Weekdays (e.g., شنبه, دوشنبه, monday)
        for ((name, targetDow) in PERSIAN_WEEKDAYS + ENGLISH_WEEKDAYS) {
            val idx = lower.indexOf(name)
            if (idx >= 0) {
                // Find nearest past date matching target day of week
                var candidate = now
                while (candidate.dayOfWeek != targetDow) {
                    candidate = candidate.minusDays(1)
                }
                addDate(results, candidate, name, idx, idx + name.length)
            }
        }

        // 6. Absolute numeric Jalali date: YYYY/MM/DD (e.g. 1403/08/16 or 1403-08-16)
        val numericDateRegex = Regex("""(?<!\d)(1[34]\d{2})[/-](\d{1,2})[/-](\d{1,2})(?!\d)""")
        for (m in numericDateRegex.findAll(lower)) {
            val y = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            val d = m.groupValues[3].toInt()
            val greg = jalaliToGregorian(y, month, d)
            if (greg != null) {
                addDate(results, greg, m.value, m.range.first, m.range.last + 1)
            }
        }

        // 7. Jalali day + month name: e.g. "16 آبان"
        for ((monthName, monthNum) in JALALI_MONTHS) {
            val dayMonthRegex = Regex("""\b(\d{1,2})\s+""" + monthName + """\b""")
            for (m in dayMonthRegex.findAll(lower)) {
                val d = m.groupValues[1].toInt()
                val currentJalali = gregorianToJalali(now.year, now.monthValue, now.dayOfMonth)
                val greg = jalaliToGregorian(currentJalali.first, monthNum, d)
                if (greg != null) {
                    addDate(results, greg, m.value, m.range.first, m.range.last + 1)
                }
            }
        }

        // 8. ISO Gregorian date: YYYY-MM-DD
        val isoRegex = Regex("""\b(20\d{2})-(\d{2})-(\d{2})\b""")
        for (m in isoRegex.findAll(lower)) {
            val y = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            val d = m.groupValues[3].toInt()
            try {
                val greg = LocalDate.of(y, month, d)
                addDate(results, greg, m.value, m.range.first, m.range.last + 1)
            } catch (_: Exception) {}
        }

        return results.sortedBy { it.startIndex }
    }

    private fun checkRelativeKeyword(text: String, keyword: String, target: LocalDate, out: MutableList<ExtractedDate>) {
        val idx = text.indexOf(keyword)
        if (idx >= 0) {
            addDate(out, target, keyword, idx, idx + keyword.length)
        }
    }

    private fun checkPeriodKeyword(text: String, keyword: String, start: LocalDate, end: LocalDate, out: MutableList<ExtractedDate>) {
        val idx = text.indexOf(keyword)
        if (idx >= 0) {
            val j = gregorianToJalali(start.year, start.monthValue, start.dayOfMonth)
            out.add(
                ExtractedDate(
                    date = start,
                    jalaliYear = j.first,
                    jalaliMonth = j.second,
                    jalaliDay = j.third,
                    originalSpan = keyword,
                    startIndex = idx,
                    endIndex = idx + keyword.length,
                    isPeriod = true,
                    periodStart = start,
                    periodEnd = end
                )
            )
        }
    }

    private fun addDate(out: MutableList<ExtractedDate>, date: LocalDate, span: String, start: Int, end: Int) {
        val overlaps = out.any { it.startIndex < end && it.endIndex > start }
        if (!overlaps) {
            val j = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
            out.add(
                ExtractedDate(
                    date = date,
                    jalaliYear = j.first,
                    jalaliMonth = j.second,
                    jalaliDay = j.third,
                    originalSpan = span,
                    startIndex = start,
                    endIndex = end
                )
            )
        }
    }

    private fun findWeekPeriod(ref: LocalDate, isCurrentWeek: Boolean): Pair<LocalDate, LocalDate> {
        // Persian week starts on Saturday
        var cur = ref
        while (cur.dayOfWeek != DayOfWeek.SATURDAY) {
            cur = cur.minusDays(1)
        }
        val weekStart = cur
        val weekEnd = if (isCurrentWeek) ref else weekStart.plusDays(6)
        return weekStart to weekEnd
    }

    private fun findMonthPeriod(ref: LocalDate, isCurrentMonth: Boolean): Pair<LocalDate, LocalDate> {
        val j = gregorianToJalali(ref.year, ref.monthValue, ref.dayOfMonth)
        val monthStart = jalaliToGregorian(j.first, j.second, 1) ?: ref
        val monthEnd = if (isCurrentMonth) ref else {
            val daysInMonth = if (j.second <= 6) 31 else if (j.second <= 11) 30 else if (isJalaliLeapYear(j.first)) 30 else 29
            jalaliToGregorian(j.first, j.second, daysInMonth) ?: ref
        }
        return monthStart to monthEnd
    }

    // Pure Kotlin Jalali <-> Gregorian conversion algorithms (delegated to verified JalaliDateHelper)

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val j = com.example.poultryapp.engine.JalaliDateHelper.gregorianToJalali(gy, gm, gd)
        return Triple(j.year, j.month, j.day)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): LocalDate? {
        return try {
            val (gy, gm, gd) = com.example.poultryapp.engine.JalaliDateHelper.jalaliToGregorian(jy, jm, jd)
            LocalDate.of(gy, gm, gd)
        } catch (_: Exception) {
            null
        }
    }

    fun isJalaliLeapYear(jy: Int): Boolean {
        val breaks = intArrayOf(-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178)
        var bl = breaks.size
        var jp = breaks[0]
        var jm = 0
        var jump = 0
        for (j in 1 until bl) {
            jm = breaks[j]
            jump = jm - jp
            if (jy >= jp && jy < jm) break
            jp = jm
        }
        var n = jy - jp
        if (jump - n < 6) n = n - jump + (jump + 4) / 33 * 33
        var leap = ((n + 1) % 33 - 1) % 4
        if (leap == -1) leap = 4
        return leap == 0
    }

    fun formatToStandardJalali(date: LocalDate): String {
        val (jy, jm, jd) = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
        return String.format(java.util.Locale.US, "%04d/%02d/%02d", jy, jm, jd)
    }

    fun toEpochMillis(date: LocalDate): Long {
        return date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
