package com.example.poultryapp.engine

import com.example.poultryapp.data.local.FeasibilityInput
import com.example.poultryapp.data.local.FeasibilityResult
import com.example.poultryapp.data.model.BirdSpecies
import kotlin.math.roundToInt

object InvestmentFeasibilityEngine {

    /**
     * شبیه‌سازی جامع توجیه‌پذیری اقتصادی و سرمایه‌گذاری برای احداث یا جوجه‌ریزی دوره جدید.
     */
    fun calculateFeasibility(input: FeasibilityInput): FeasibilityResult {
        val count = input.birdCount.coerceAtLeast(10)
        val mortalityRatio = (input.estimatedMortalityPercent / 100.0).coerceIn(0.01, 0.40)
        val survivingBirds = (count * (1.0 - mortalityRatio)).roundToInt().coerceAtLeast(1)

        // برآورد ضریب تبدیل غذایی (FCR)
        val estimatedFcr = when (input.species) {
            BirdSpecies.CHICKEN -> 1.75
            BirdSpecies.QUAIL -> 2.6
            BirdSpecies.TURKEY -> 2.4
            BirdSpecies.DUCK -> 2.2
            else -> 1.9
        }

        // کل دان مصرفی دوره
        val totalLiveWeightProducedKg = survivingBirds * input.targetWeightKg
        val totalFeedKg = totalLiveWeightProducedKg * estimatedFcr

        // برآورد هزینه‌ها
        val chickCost = count * input.chickPriceTomans
        val feedCost = totalFeedKg * input.feedPricePerKgTomans
        val overheadCost = input.fixedOverheadTomans

        val totalWorkingCapital = chickCost + feedCost + overheadCost

        // درآمد ناخالص
        val totalRevenue = totalLiveWeightProducedKg * input.liveSalePricePerKgTomans

        // سود خالص
        val netProfit = totalRevenue - totalWorkingCapital

        // بازده سرمایه‌گذاری (ROI)
        val roi = if (totalWorkingCapital > 0) (netProfit / totalWorkingCapital) * 100.0 else 0.0

        // سود خالص به ازای هر قطعه پرنده زنده
        val profitPerBird = if (survivingBirds > 0) netProfit / survivingBirds else 0.0

        // نقطه سر به سر (روز رسیدن به برابری سرمایه با ارزش گله)
        val breakEvenDay = if (netProfit > 0 && totalRevenue > totalWorkingCapital && input.cycleDays > 10) {
            ((totalWorkingCapital / totalRevenue) * input.cycleDays).roundToInt()
                .coerceIn(1, input.cycleDays)
        } else if (netProfit > 0 && totalRevenue > totalWorkingCapital) {
            input.cycleDays
        } else {
            null
        }

        return FeasibilityResult(
            totalWorkingCapitalTomans = totalWorkingCapital,
            totalFeedKg = totalFeedKg,
            totalRevenueTomans = totalRevenue,
            netProfitTomans = netProfit,
            breakEvenDay = breakEvenDay,
            roiPercent = roi,
            profitPerBirdTomans = profitPerBird
        )
    }
}
