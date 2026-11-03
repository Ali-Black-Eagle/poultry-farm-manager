package com.example.poultryapp.engine

import com.example.poultryapp.data.local.DailyMarginalProfitPoint
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.SlaughterOptimizationResult
import com.example.poultryapp.data.model.BirdSpecies
import kotlin.math.max
import kotlin.math.roundToInt

object OptimalSlaughterEngine {

    /**
     * محاسبه تاریخ بهینه کشتار و پنجره طلایی ارسال به کشتارگاه بر مبنای سود حاشیه‌ای روزانه.
     */
    fun calculateOptimalSlaughter(
        flock: Flock,
        livePricePerKgTomans: Double = 82000.0,
        feedPricePerKgTomans: Double = 22000.0,
        dailyOverheadPerBirdTomans: Double = 600.0
    ): SlaughterOptimizationResult {
        val currentAge = flock.ageDays.coerceAtLeast(1)

        // بازه شبیه‌سازی سن پرنده بر اساس گونه (مثلاً برای مرغ ۳۰ تا ۵۵ روزگی)
        val minAge = when (flock.species) {
            BirdSpecies.CHICKEN -> 35
            BirdSpecies.QUAIL -> 35
            BirdSpecies.TURKEY -> 90
            BirdSpecies.DUCK -> 45
            else -> 35
        }
        val maxAge = when (flock.species) {
            BirdSpecies.CHICKEN -> 55
            BirdSpecies.QUAIL -> 50
            BirdSpecies.TURKEY -> 130
            BirdSpecies.DUCK -> 70
            else -> 60
        }

        val points = mutableListOf<DailyMarginalProfitPoint>()
        var bestProfitAge = minAge
        var maxProfit = Double.NEGATIVE_INFINITY

        var cumulativeFeedGrams = 0.0

        for (day in minAge..maxAge) {
            val weightKg = estimateWeightAtAge(flock.species, day)
            val prevWeightKg = estimateWeightAtAge(flock.species, day - 1)
            val dailyGainGrams = max(5.0, (weightKg - prevWeightKg) * 1000.0)

            val dailyFeedGrams = estimateFeedAtAge(flock.species, day)
            cumulativeFeedGrams += dailyFeedGrams

            val fcr = if (weightKg > 0) (cumulativeFeedGrams / 1000.0) / weightKg else 1.8

            val dailyRevenue = (dailyGainGrams / 1000.0) * livePricePerKgTomans
            val dailyFeedCost = (dailyFeedGrams / 1000.0) * feedPricePerKgTomans
            val dailyMarginalProfit = dailyRevenue - (dailyFeedCost + dailyOverheadPerBirdTomans)

            points.add(
                DailyMarginalProfitPoint(
                    ageDay = day,
                    bodyWeightKg = weightKg,
                    dailyWeightGainGrams = dailyGainGrams,
                    dailyFeedGrams = dailyFeedGrams,
                    dailyRevenueTomans = dailyRevenue,
                    dailyFeedCostTomans = dailyFeedCost,
                    dailyMarginalProfitTomans = dailyMarginalProfit,
                    fcr = fcr
                )
            )

            if (dailyMarginalProfit > maxProfit) {
                maxProfit = dailyMarginalProfit
                bestProfitAge = day
            }
        }

        val windowStart = (bestProfitAge - 1).coerceAtLeast(minAge)
        val windowEnd = (bestProfitAge + 1).coerceAtMost(maxAge)
        val goldenWindowStr = "روز ${toPersianDigits(windowStart)} تا ${toPersianDigits(windowEnd)}"

        val isOverdue = currentAge > windowEnd
        val recommendation = if (isOverdue) {
            "هشدار: گله از سن بهینه کشتار عبور کرده است. ضریب تبدیل (FCR) در حال افت شدید و سود روزانه منفی است. بارگیری فوری توصیه می‌شود."
        } else if (currentAge in windowStart..windowEnd) {
            "گله دقیقاً در «پنجره طلایی کشتار» قرار دارد! حداکثر سود حاشیه‌ای حاصل شده است."
        } else {
            "گله در حال رشد اقتصادی مطلوب است. زمان رسیدن به پنجره طلایی: حدود ${toPersianDigits(windowStart - currentAge)} روز دیگر."
        }

        return SlaughterOptimizationResult(
            currentAgeDays = currentAge,
            optimalHarvestAgeDays = bestProfitAge,
            goldenHarvestWindowRange = goldenWindowStr,
            dailyMarginalProfits = points,
            recommendationMessage = recommendation,
            isOverdueForSlaughter = isOverdue
        )
    }

    private fun estimateWeightAtAge(species: BirdSpecies, ageDays: Int): Double {
        return when (species) {
            BirdSpecies.CHICKEN -> {
                // منحنی رشد زیگموئیدی جوجه گوشتی رأس / کاب
                1.5 / (1.0 + Math.exp(-0.09 * (ageDays - 38))) * 2.2
            }
            BirdSpecies.QUAIL -> {
                0.25 / (1.0 + Math.exp(-0.12 * (ageDays - 30)))
            }
            BirdSpecies.TURKEY -> {
                14.0 / (1.0 + Math.exp(-0.06 * (ageDays - 85)))
            }
            BirdSpecies.DUCK -> {
                3.2 / (1.0 + Math.exp(-0.08 * (ageDays - 42)))
            }
            else -> {
                2.0 / (1.0 + Math.exp(-0.08 * (ageDays - 40)))
            }
        }
    }

    private fun estimateFeedAtAge(species: BirdSpecies, ageDays: Int): Double {
        return when (species) {
            BirdSpecies.CHICKEN -> {
                (70.0 + (ageDays - 20) * 4.2).coerceIn(80.0, 210.0)
            }
            BirdSpecies.QUAIL -> {
                (20.0 + (ageDays - 20) * 0.5).coerceIn(20.0, 35.0)
            }
            BirdSpecies.TURKEY -> {
                (200.0 + (ageDays - 60) * 4.0).coerceIn(200.0, 520.0)
            }
            BirdSpecies.DUCK -> {
                (120.0 + (ageDays - 30) * 2.5).coerceIn(100.0, 240.0)
            }
            else -> {
                (80.0 + (ageDays - 20) * 3.0).coerceIn(80.0, 180.0)
            }
        }
    }
}
