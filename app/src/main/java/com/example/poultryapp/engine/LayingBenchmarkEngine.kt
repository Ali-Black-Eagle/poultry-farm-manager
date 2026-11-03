package com.example.poultryapp.engine

import com.example.poultryapp.data.local.EggProduction
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.LayingDropAlert
import java.util.Locale

object LayingBenchmarkEngine {

    /**
     * بررسی افت غیرعادی تخم‌گذاری بر اساس میانگین متحرک ۳ روزه نسبت به میانگین ۷ روز پیشین.
     * اگر افت تولید بیش از ۳.۵٪ باشد، اخطار تحلیلی به همراه چک‌لیست عیب‌یابی صادر می‌شود.
     */
    fun evaluateLayingDrop(
        flock: Flock,
        eggProductions: List<EggProduction>
    ): LayingDropAlert? {
        val flockLogs = eggProductions
            .filter { it.flockId == flock.id }
            .sortedByDescending { it.timestampMillis }

        if (flockLogs.size < 4) return null // داده کافی برای تحلیل مقایسه‌ای نیست

        val birdCount = flock.effectiveBirdCount.coerceAtLeast(1)

        // میانگین متحرک ۳ روز اخیر (Recent 3 Days)
        val recentThreeDays = flockLogs.take(3)
        val recentThreeDayAvgPercent = recentThreeDays.map {
            (it.totalCount.toDouble() / birdCount.toDouble()) * 100.0
        }.average()

        // میانگین ۷ روز پیشین (Previous Week Average)
        val priorWeekLogs = flockLogs.drop(3).take(7)
        if (priorWeekLogs.isEmpty()) return null

        val priorWeekAvgPercent = priorWeekLogs.map {
            (it.totalCount.toDouble() / birdCount.toDouble()) * 100.0
        }.average()

        val dropDiff = priorWeekAvgPercent - recentThreeDayAvgPercent

        // آستانه اخطار افت غیرعادی: بیش از ۳.۵٪ کاهش
        if (dropDiff >= 3.5) {
            val checklist = listOf(
                "ساعات روشنایی: بررسی تایمر و خاموشی احتمالی لامپ‌های سالن در ۴۸ ساعت گذشته",
                "کیفیت و فشار آبخوری‌ها: شوری، بو یا انسداد خطوط نیپل که باعث کم‌آبی پرنده شده باشد",
                "کیفیت دان و ترکیب کلسیم: تازگی دان، بوی کپک‌زدگی، یا عدم تعادل مکمل کربنات کلسیم",
                "پایش سلامت و تهویه: بررسی علائم بالینی، خس‌خس سینه و تغییر شکل یا رنگ پوسته تخم‌مرغ"
            )

            val todayJalali = JalaliDateHelper.getJalaliDate().toStandardString()

            return LayingDropAlert(
                flockId = flock.id,
                flockName = flock.name,
                recentThreeDayAveragePercent = recentThreeDayAvgPercent,
                priorWeekAveragePercent = priorWeekAvgPercent,
                dropDifferencePercent = dropDiff,
                dateJalaliStr = todayJalali,
                diagnosticChecklist = checklist
            )
        }

        return null
    }

    /**
     * ارزیابی سراسری تمام گله‌های تخم‌گذار فارم جهت کشف هشدارهای افت تولید.
     */
    fun evaluateAllFlocks(
        flocks: List<Flock>,
        eggProductions: List<EggProduction>
    ): List<LayingDropAlert> {
        return flocks.mapNotNull { flock ->
            evaluateLayingDrop(flock, eggProductions)
        }
    }
}
