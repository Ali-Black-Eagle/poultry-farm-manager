package com.example.poultryapp.engine

import java.util.Calendar
import java.util.TimeZone

enum class FarmSeason(val titleFa: String) {
    SPRING("بهار"),
    SUMMER("تابستان"),
    AUTUMN("پاییز"),
    WINTER("زمستان")
}

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    val dayOfWeek: Int get() = JalaliDateHelper.getDayOfWeek(year, month, day)
    val dayName: String get() = JalaliDateHelper.getDayNameFa(dayOfWeek)
    val monthName: String get() = JalaliDateHelper.getMonthNameFa(month)

    override fun toString(): String = "${toPersianDigits(year)}/${toPersianDigits("%02d".format(month))}/${toPersianDigits("%02d".format(day))}"
    fun toStandardString(): String = "%04d/%02d/%02d".format(year, month, day)
}

fun toPersianDigits(number: Any?): String {
    if (number == null) return ""
    val str = number.toString()
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val builder = StringBuilder()
    for (ch in str) {
        if (ch in '0'..'9') {
            builder.append(persianDigits[ch - '0'])
        } else {
            builder.append(ch)
        }
    }
    return builder.toString()
}

@JvmName("toPersianDigitsExtension")
fun Any?.toPersianDigits(): String = toPersianDigits(this)

object JalaliDateHelper {

    private val tehranTimeZone = TimeZone.getTimeZone("Asia/Tehran")

    /**
     * Converts epoch milliseconds to Jalali Date.
     */
    fun getJalaliDate(epochMillis: Long = System.currentTimeMillis()): JalaliDate {
        val calendar = Calendar.getInstance(tehranTimeZone).apply {
            timeInMillis = epochMillis
        }
        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1
        val gd = calendar.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gy, gm, gd)
    }

    /**
     * Detects current season based on Jalali month:
     * 1-3: Spring, 4-6: Summer, 7-9: Autumn, 10-12: Winter
     */
    fun getCurrentSeason(epochMillis: Long = System.currentTimeMillis()): FarmSeason {
        val jalali = getJalaliDate(epochMillis)
        return when (jalali.month) {
            in 1..3 -> FarmSeason.SPRING
            in 4..6 -> FarmSeason.SUMMER
            in 7..9 -> FarmSeason.AUTUMN
            else -> FarmSeason.WINTER
        }
    }

    /**
     * Calculates age in days and weeks from birth timestamp.
     */
    fun calculateAge(birthEpochMillis: Long, currentEpochMillis: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val diffMillis = (currentEpochMillis - birthEpochMillis).coerceAtLeast(0)
        val days = (diffMillis / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val weeks = ((days - 1) / 7 + 1).coerceAtLeast(1)
        return Pair(days, weeks)
    }

    /**
     * Format elapsed duration into Persian readable string, e.g.:
     * "۴ ساعت و ۵۵ دقیقه" or "۳ روز و ۲ ساعت"
     */
    fun formatElapsedDurationFa(durationMillis: Long): String {
        val totalMinutes = (durationMillis / (1000 * 60)).coerceAtLeast(0)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60

        return when {
            days > 0 && hours > 0 -> "${toPersianDigits(days)} روز و ${toPersianDigits(hours)} ساعت"
            days > 0 -> "${toPersianDigits(days)} روز"
            hours > 0 && minutes > 0 -> "${toPersianDigits(hours)} ساعت و ${toPersianDigits(minutes)} دقیقه"
            hours > 0 -> "${toPersianDigits(hours)} ساعت"
            minutes > 0 -> "${toPersianDigits(minutes)} دقیقه"
            else -> "کمتر از یک دقیقه"
        }
    }

    /**
     * Accurate Gregorian to Jalali converter algorithm
     */
    fun gregorianToJalali(gY: Int, gM: Int, gD: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy = gY - 1600
        var gm = gM - 1
        var gd = gD - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400

        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0..11) {
            if (jDayNo < jDaysInMonth[i]) {
                jm = i
                break
            }
            jDayNo -= jDaysInMonth[i]
        }
        val jd = jDayNo + 1

        return JalaliDate(jy, jm + 1, jd)
    }

    /**
     * Jalali to Gregorian converter for reverse date calculations
     */
    fun jalaliToGregorian(jY: Int, jM: Int, jD: Int): Triple<Int, Int, Int> {
        var jy = jY - 979
        var jm = jM - 1
        var jd = jD - 1

        var jDayNo = 365 * jy + (jy / 33) * 8 + ((jy % 33 + 3) / 4)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        for (i in 0 until jm) {
            jDayNo += jDaysInMonth[i]
        }
        jDayNo += jd

        var gDayNo = jDayNo + 79

        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524

            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        val gDaysInMonth = intArrayOf(
            31,
            if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )
        var gm = 0
        for (i in 0..11) {
            if (gDayNo < gDaysInMonth[i]) {
                gm = i
                break
            }
            gDayNo -= gDaysInMonth[i]
        }
        val gd = gDayNo + 1
        return Triple(gy, gm + 1, gd)
    }

    /**
     * Converts Jalali Year, Month, Day to epoch millis (midnight Asia/Tehran)
     */
    fun jalaliToEpochMillis(jY: Int, jM: Int, jD: Int): Long {
        val (gy, gm, gd) = jalaliToGregorian(jY, jM, jD)
        val cal = Calendar.getInstance(tehranTimeZone).apply {
            set(Calendar.YEAR, gy)
            set(Calendar.MONTH, gm - 1)
            set(Calendar.DAY_OF_MONTH, gd)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    val monthNamesFa = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    fun getMonthNameFa(month: Int): String {
        return monthNamesFa.getOrElse(month - 1) { "" }
    }

    val daysNamesFa = listOf(
        "شنبه", "یکشنبه", "دوشنبه",
        "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    fun getDayNameFa(dayOfWeek: Int): String {
        return daysNamesFa.getOrElse(dayOfWeek) { "" }
    }

    /**
     * Checks if a Jalali year is a leap year by checking if 30th of Esfand exists.
     */
    fun isJalaliLeapYear(year: Int): Boolean {
        val (gy, gm, gd) = jalaliToGregorian(year, 12, 30)
        val back = gregorianToJalali(gy, gm, gd)
        return back.year == year && back.month == 12 && back.day == 30
    }

    /**
     * Returns total days in the given Jalali month (31 for months 1..6, 30 for 7..11, 29/30 for 12)
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isJalaliLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    /**
     * Returns Shamsi weekday index for the 1st day of the specified month:
     * 0: شنبه (Sat), 1: یکشنبه (Sun), 2: دوشنبه (Mon), 3: سه‌شنبه (Tue),
     * 4: چهارشنبه (Wed), 5: پنج‌شنبه (Thu), 6: جمعه (Fri)
     */
    fun getFirstDayOfWeekOfMonth(year: Int, month: Int): Int {
        val epoch = jalaliToEpochMillis(year, month, 1)
        val cal = Calendar.getInstance(tehranTimeZone).apply {
            timeInMillis = epoch
        }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    /**
     * Returns Shamsi weekday index for a specific Jalali date:
     * 0: شنبه .. 6: جمعه
     */
    fun getDayOfWeek(year: Int, month: Int, day: Int): Int {
        val epoch = jalaliToEpochMillis(year, month, day)
        val cal = Calendar.getInstance(tehranTimeZone).apply {
            timeInMillis = epoch
        }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }
}

