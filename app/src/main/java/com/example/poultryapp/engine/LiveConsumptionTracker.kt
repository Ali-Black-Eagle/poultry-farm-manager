package com.example.poultryapp.engine

enum class ChargeItemType(val titleFa: String) {
    FEED("دان"),
    WATER("آب")
}

data class LiveTrackerStatus(
    val itemType: ChargeItemType,
    val chargedAmount: Double,
    val remainingAmount: Double,
    val progress: Float, // 0.0f to 1.0f
    val isFinished: Boolean,
    val isLowWarning: Boolean,
    val elapsedTimeFa: String,
    val remainingAmountFa: String,
    val statusTextFa: String,
    val timeToFinishMillis: Long,
    val finishedDurationFa: String?,
    val emptyTimestampMillis: Long = 0L
)

object LiveConsumptionTracker {

    /**
     * Calculates the live status of Feed or Water for a flock.
     *
     * @param itemType FEED or WATER
     * @param chargedAmount The initial amount given in this charge (grams or ml)
     * @param chargeTimestampMillis Exact epoch timestamp when the charge was performed
     * @param dailyRequirementTotal Total daily requirement for the flock (grams or ml)
     * @param lowThresholdPercent Percentage threshold to trigger low warning (default 15.0)
     * @param currentTimestampMillis Current time in millis (default now)
     */
    fun calculateLiveStatus(
        itemType: ChargeItemType,
        chargedAmount: Double,
        chargeTimestampMillis: Long,
        dailyRequirementTotal: Double,
        lowThresholdPercent: Double = 15.0,
        currentTimestampMillis: Long = System.currentTimeMillis()
    ): LiveTrackerStatus {
        if (chargeTimestampMillis <= 0L) {
            return LiveTrackerStatus(
                itemType = itemType,
                chargedAmount = 0.0,
                remainingAmount = 0.0,
                progress = 0f,
                isFinished = false,
                isLowWarning = false,
                elapsedTimeFa = "شارژ نشده",
                remainingAmountFa = if (itemType == ChargeItemType.FEED) "۰ گرم" else "۰ میلی‌لیتر",
                statusTextFa = "هنوز شارژ اولیه ثبت نشده است",
                timeToFinishMillis = 0L,
                finishedDurationFa = null,
                emptyTimestampMillis = 0L
            )
        }

        if (chargedAmount <= 0.0) {
            val elapsedMillis = (currentTimestampMillis - chargeTimestampMillis).coerceAtLeast(0L)
            val elapsedFa = JalaliDateHelper.formatElapsedDurationFa(elapsedMillis)
            return LiveTrackerStatus(
                itemType = itemType,
                chargedAmount = 0.0,
                remainingAmount = 0.0,
                progress = 0f,
                isFinished = true,
                isLowWarning = false,
                elapsedTimeFa = "$elapsedFa از تخلیه ${itemType.titleFa} می‌گذرد",
                remainingAmountFa = if (itemType == ChargeItemType.FEED) "۰ گرم (تخلیه شده)" else "۰ میلی‌لیتر (تخلیه شده)",
                statusTextFa = "منبع کاملاً تخلیه و دور ریخته شد (آماده شارژ تمیز)",
                timeToFinishMillis = 0L,
                finishedDurationFa = elapsedFa,
                emptyTimestampMillis = chargeTimestampMillis
            )
        }

        val elapsedMillis = (currentTimestampMillis - chargeTimestampMillis).coerceAtLeast(0L)
        val elapsedTimeFa = "${JalaliDateHelper.formatElapsedDurationFa(elapsedMillis)} از شارژ ${itemType.titleFa} می‌گذرد"

        // Rate of consumption in amount per millisecond:
        // Assume daily consumption occurs over a 24-hour day = 86,400,000 millis
        // (Active flock rate during lighting hours, baseline 24h daily distribution)
        val totalDailyRate = dailyRequirementTotal.coerceAtLeast(1.0)
        val ratePerMilli = totalDailyRate / (24L * 60 * 60 * 1000)

        // Estimated consumed amount since charge
        val consumedAmount = elapsedMillis * ratePerMilli
        val remainingAmount = (chargedAmount - consumedAmount).coerceAtLeast(0.0)

        val progress = if (chargedAmount > 0.0) {
            (remainingAmount / chargedAmount).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        val percent = progress * 100.0
        val isFinished = remainingAmount <= 0.01
        val isLowWarning = !isFinished && percent <= lowThresholdPercent

        // Formatted remaining amount
        val formattedRemaining = if (itemType == ChargeItemType.FEED) {
            DailyNutritionEngine.formatFeed(remainingAmount)
        } else {
            DailyNutritionEngine.formatWater(remainingAmount)
        }

        // Calculate time to finish or time since finish
        val totalTimeToDepleteMillis = (chargedAmount / ratePerMilli).toLong()
        val emptyTimestampMillis = chargeTimestampMillis + totalTimeToDepleteMillis

        val (statusText, finishedDurationFa, timeToFinishMillis) = if (isFinished) {
            val millisSinceEmpty = (currentTimestampMillis - emptyTimestampMillis).coerceAtLeast(0L)
            val durFa = JalaliDateHelper.formatElapsedDurationFa(millisSinceEmpty)
            Triple(
                "تمام شده ($durFa از اتمام ${itemType.titleFa} می‌گذرد)",
                durFa,
                0L
            )
        } else {
            val remainingMillis = (emptyTimestampMillis - currentTimestampMillis).coerceAtLeast(0L)
            val finishDurFa = JalaliDateHelper.formatElapsedDurationFa(remainingMillis)
            val warningPrefix = if (isLowWarning) "هشدار کمبود! " else ""
            Triple(
                "$warningPrefix$formattedRemaining ${itemType.titleFa} باقی مانده (حدود $finishDurFa تا اتمام)",
                null,
                remainingMillis
            )
        }

        return LiveTrackerStatus(
            itemType = itemType,
            chargedAmount = chargedAmount,
            remainingAmount = remainingAmount,
            progress = progress,
            isFinished = isFinished,
            isLowWarning = isLowWarning,
            elapsedTimeFa = elapsedTimeFa,
            remainingAmountFa = formattedRemaining,
            statusTextFa = statusText,
            timeToFinishMillis = timeToFinishMillis,
            finishedDurationFa = finishedDurationFa,
            emptyTimestampMillis = emptyTimestampMillis
        )
    }
}
