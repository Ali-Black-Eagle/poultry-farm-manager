package com.example.poultryapp.engine

import com.example.poultryapp.data.local.Flock
import java.util.Locale
import kotlin.math.max

enum class SurvivalThreatType(val titleFa: String) {
    WATER_DEPLETED("قطع کامل آب (دهیدراتاسیون حاد)"),
    FEED_DEPLETED("اتمام کامل دان (گرسنگی متابولیک)"),
    BOTH_DEPLETED("قطع همزمان آب و دان (بحران حیاتی)")
}

enum class SurvivalRiskLevel(val titleFa: String, val badgeFa: String) {
    CRITICAL("بحرانی - خطر آنی تلفات", "بحرانی"),
    HIGH("بسیار پرخطر - شروع استرس شدید", "بسیار پرخطر"),
    WARNING("هشدار - افت شدید عملکرد و آغاز خطر", "هشدار خطر")
}

data class SurvivalCountdownState(
    val threatType: SurvivalThreatType,
    val riskLevel: SurvivalRiskLevel,
    val totalSurvivalHours: Double,
    val deadlineEpochMillis: Long,
    val remainingMillis: Long,
    val isExpired: Boolean,
    val formattedCountdown: String, // HH:MM:SS
    val explanationFa: String,
    val tempUsedCelsius: Double,
    val tempSourceFa: String
)

object SurvivalCalculationEngine {

    /**
     * Calculates the scientific water intake multiplier based on ambient temperature.
     * Standard baseline is 20°C (1.0).
     * Above 24°C, birds start evaporative panting, dramatically increasing water consumption.
     */
    fun calculateWaterTemperatureMultiplier(tempCelsius: Double): Double {
        return when {
            tempCelsius <= 10.0 -> 0.80
            tempCelsius <= 20.0 -> 0.80 + (tempCelsius - 10.0) * (0.20 / 10.0) // 0.80 -> 1.00
            tempCelsius <= 25.0 -> 1.00 + (tempCelsius - 20.0) * (0.15 / 5.0)  // 1.00 -> 1.15 (+3%/°C)
            tempCelsius <= 30.0 -> 1.15 + (tempCelsius - 25.0) * (0.25 / 5.0)  // 1.15 -> 1.40 (+5%/°C)
            tempCelsius <= 35.0 -> 1.40 + (tempCelsius - 30.0) * (0.40 / 5.0)  // 1.40 -> 1.80 (+8%/°C)
            tempCelsius <= 40.0 -> 1.80 + (tempCelsius - 35.0) * (0.60 / 5.0)  // 1.80 -> 2.40 (+12%/°C)
            else -> 2.40 + (tempCelsius - 40.0) * 0.15                         // Extreme heat >40°C
        }
    }

    /**
     * Calculates survival hours until the first mortality under WATER deprivation.
     * Factors:
     * - Species & Breed
     * - Age (young chicks have very low reserve and high surface area)
     * - Ambient temperature (heat stress causes hyperthermia and rapid dehydration)
     */
    fun calculateWaterSurvivalHours(speciesFa: String, breedFa: String, ageDays: Int, tempCelsius: Double): Double {
        // 1. Base species survival hours at 20°C
        val s = speciesFa.trim()
        val b = breedFa.trim()
        val baseHours = when {
            s.contains("شاخدار") || s.contains("گینه") || b.contains("شاخدار") || b.contains("گینه") -> 52.0 // Highly drought-resistant savannah fowl
            s.contains("اردک") || s.contains("غاز") || b.contains("اردک") || b.contains("غاز") || b.contains("پکین") || b.contains("کمپبل") || b.contains("مسکووی") || b.contains("ملارد") -> 28.0 // Waterfowl dehydrate rapidly
            s.contains("بلدرچین") || b.contains("بلدرچین") || b.contains("ژاپنی") || b.contains("تگزاس") -> 24.0 // Small mass, high metabolic rate
            s.contains("بوقلمون") || b.contains("بوقلمون") || b.contains("بیوتی") || b.contains("نیکولاس") || b.contains("برنز") -> 40.0
            s.contains("شترمرغ") -> 48.0
            s.contains("کبک") -> 30.0
            s.contains("تخم") || b.contains("تخم") || b.contains("لگهورن") || b.contains("های‌لاین") || b.contains("هایلاین") || b.contains("لوهمن") || b.contains("ال‌اس‌ال") || b.contains("ایزا") || b.contains("بلک استار") -> 44.0 // Layer chicken
            s.contains("بومی") || s.contains("محلی") || b.contains("بومی") || b.contains("محلی") || b.contains("مرندی") || b.contains("گلپایگانی") || b.contains("لاری") -> 48.0 // Native hardy birds
            else -> 38.0 // Broiler chicken (high growth metabolism)
        }

        // Breed adjustments
        val breedFactor = when {
            b.contains("راس") || b.contains("کاب") || b.contains("آرین") || b.contains("آربر") -> 0.90 // Fast growing broilers
            b.contains("لگهورن") || b.contains("های‌لاین") || b.contains("هایلاین") || b.contains("لوهمن") || b.contains("ال‌اس‌ال") -> 1.05
            b.contains("آمروک") || b.contains("پلیموت") || b.contains("سوسکس") || b.contains("کوشین") -> 1.10 // Robust dual-purpose heritage breed
            b.contains("بومی") || b.contains("مرندی") || b.contains("گلپایگانی") || b.contains("لاری") -> 1.15
            else -> 1.0
        }

        // Age factor: Chicks < 7 days have only ~45% survival window, adult birds have full
        val ageFactor = when {
            ageDays <= 7 -> 0.45
            ageDays <= 14 -> 0.65
            ageDays <= 28 -> 0.82
            ageDays <= 42 -> 0.95
            else -> 1.0
        }

        // Temperature factor on dehydration rate
        val tempFactor = when {
            tempCelsius <= 15.0 -> 1.10
            tempCelsius <= 22.0 -> 1.00
            tempCelsius <= 26.0 -> 0.80
            tempCelsius <= 30.0 -> 0.58
            tempCelsius <= 34.0 -> 0.38
            tempCelsius <= 38.0 -> 0.22
            tempCelsius <= 42.0 -> 0.14
            else -> 0.09 // Above 42°C: acute heat stroke within 2-4 hours
        }

        return max(2.0, baseHours * breedFactor * ageFactor * tempFactor)
    }

    /**
     * Calculates survival hours until the first mortality under FEED deprivation.
     * Feed deprivation allows much longer survival than water, but severe cold burns energy faster.
     */
    fun calculateFeedSurvivalHours(speciesFa: String, breedFa: String, ageDays: Int, tempCelsius: Double): Double {
        val s = speciesFa.trim()
        val b = breedFa.trim()
        val baseHours = when {
            s.contains("شاخدار") || s.contains("گینه") || b.contains("شاخدار") || b.contains("گینه") -> 140.0 // Guinea fowl high metabolic endurance
            s.contains("بلدرچین") || b.contains("بلدرچین") || b.contains("ژاپنی") || b.contains("تگزاس") -> 55.0  // ~2.3 days
            s.contains("اردک") || s.contains("غاز") || b.contains("اردک") || b.contains("غاز") || b.contains("پکین") || b.contains("ملارد") -> 96.0 // ~4 days
            s.contains("بوقلمون") || b.contains("بوقلمون") || b.contains("بیوتی") || b.contains("نیکولاس") -> 110.0 // ~4.5 days
            b.contains("آمروک") || b.contains("پلیموت") -> 150.0  // Amrock heritage endurance
            s.contains("تخم") || b.contains("تخم") || b.contains("لگهورن") || b.contains("های‌لاین") || b.contains("هایلاین") || b.contains("لوهمن") -> 155.0 // ~6.5 days
            s.contains("بومی") || s.contains("محلی") || b.contains("بومی") || b.contains("محلی") || b.contains("مرندی") || b.contains("گلپایگانی") || b.contains("لاری") -> 160.0 // ~6.5 days
            else -> 105.0 // Broilers ~4.4 days
        }

        val ageFactor = when {
            ageDays <= 7 -> 0.40  // Chicks burn yolk/liver glycogen in 1-2 days
            ageDays <= 14 -> 0.60
            ageDays <= 28 -> 0.80
            else -> 1.0
        }

        // In cold weather, birds burn 30-40% more fat to keep warm
        val coldFactor = when {
            tempCelsius < 5.0 -> 0.65
            tempCelsius < 15.0 -> 0.82
            tempCelsius < 28.0 -> 1.00
            else -> 0.88 // Severe heat stress also induces metabolic crisis
        }

        return max(12.0, baseHours * ageFactor * coldFactor)
    }

    fun calculateWaterSurvivalHours(species: com.example.poultryapp.data.model.BirdSpecies, breedFa: String, ageDays: Int, tempCelsius: Double): Double {
        val spFa = when (species) {
            com.example.poultryapp.data.model.BirdSpecies.GUINEA_FOWL -> "مرغ شاخدار"
            com.example.poultryapp.data.model.BirdSpecies.DUCK -> "اردک"
            com.example.poultryapp.data.model.BirdSpecies.GOOSE -> "غاز"
            com.example.poultryapp.data.model.BirdSpecies.TURKEY -> "بوقلمون"
            com.example.poultryapp.data.model.BirdSpecies.QUAIL -> "بلدرچین"
            com.example.poultryapp.data.model.BirdSpecies.CHICKEN -> "مرغ"
        }
        return calculateWaterSurvivalHours(spFa, breedFa, ageDays, tempCelsius)
    }

    fun calculateFeedSurvivalHours(species: com.example.poultryapp.data.model.BirdSpecies, breedFa: String, ageDays: Int, tempCelsius: Double): Double {
        val spFa = when (species) {
            com.example.poultryapp.data.model.BirdSpecies.GUINEA_FOWL -> "مرغ شاخدار"
            com.example.poultryapp.data.model.BirdSpecies.DUCK -> "اردک"
            com.example.poultryapp.data.model.BirdSpecies.GOOSE -> "غاز"
            com.example.poultryapp.data.model.BirdSpecies.TURKEY -> "بوقلمون"
            com.example.poultryapp.data.model.BirdSpecies.QUAIL -> "بلدرچین"
            com.example.poultryapp.data.model.BirdSpecies.CHICKEN -> "مرغ"
        }
        return calculateFeedSurvivalHours(spFa, breedFa, ageDays, tempCelsius)
    }

    /**
     * Computes the live Survival Countdown state for a flock when feed or water is exhausted.
     *
     * @param flock The flock entity.
     * @param isWaterFinished Whether the water feeder/tank is empty.
     * @param waterEmptyEpochMillis When water ran out.
     * @param isFeedFinished Whether the feed feeder is empty.
     * @param feedEmptyEpochMillis When feed ran out.
     * @param tempCelsius Ambient temperature in °C.
     * @param tempSource Description of temperature source (e.g., "آنلاین (اصفهان)" or "میانگین پاییز").
     * @param currentMillis Current epoch time.
     */
    fun calculateSurvivalState(
        flock: Flock,
        isWaterFinished: Boolean,
        waterEmptyEpochMillis: Long,
        isFeedFinished: Boolean,
        feedEmptyEpochMillis: Long,
        tempCelsius: Double,
        tempSource: String,
        currentMillis: Long = System.currentTimeMillis()
    ): SurvivalCountdownState? {
        if (!isWaterFinished && !isFeedFinished) {
            return null
        }

        val (ageDays, _) = JalaliDateHelper.calculateAge(flock.birthEpochMillis, currentMillis)

        val waterSurvivalHours = if (isWaterFinished) {
            calculateWaterSurvivalHours(flock.species, flock.breed, ageDays, tempCelsius)
        } else Double.MAX_VALUE

        val feedSurvivalHours = if (isFeedFinished) {
            calculateFeedSurvivalHours(flock.species, flock.breed, ageDays, tempCelsius)
        } else Double.MAX_VALUE

        val waterDeadlineMillis = if (isWaterFinished) {
            val emptyTime = if (waterEmptyEpochMillis > 0) waterEmptyEpochMillis else if (flock.createdAtMillis > 0) flock.createdAtMillis else currentMillis
            emptyTime + (waterSurvivalHours * 3600 * 1000).toLong()
        } else Long.MAX_VALUE

        val feedDeadlineMillis = if (isFeedFinished) {
            val emptyTime = if (feedEmptyEpochMillis > 0) feedEmptyEpochMillis else if (flock.createdAtMillis > 0) flock.createdAtMillis else currentMillis
            emptyTime + (feedSurvivalHours * 3600 * 1000).toLong()
        } else Long.MAX_VALUE

        // The most critical deadline determines flock survival
        val threatType: SurvivalThreatType
        val activeDeadlineMillis: Long
        val totalHours: Double

        if (isWaterFinished && isFeedFinished) {
            threatType = SurvivalThreatType.BOTH_DEPLETED
            if (waterDeadlineMillis <= feedDeadlineMillis) {
                activeDeadlineMillis = waterDeadlineMillis
                totalHours = waterSurvivalHours
            } else {
                activeDeadlineMillis = feedDeadlineMillis
                totalHours = feedSurvivalHours
            }
        } else if (isWaterFinished) {
            threatType = SurvivalThreatType.WATER_DEPLETED
            activeDeadlineMillis = waterDeadlineMillis
            totalHours = waterSurvivalHours
        } else {
            threatType = SurvivalThreatType.FEED_DEPLETED
            activeDeadlineMillis = feedDeadlineMillis
            totalHours = feedSurvivalHours
        }

        val remainingMillis = activeDeadlineMillis - currentMillis
        val isExpired = remainingMillis <= 0

        val riskLevel = when {
            isExpired -> SurvivalRiskLevel.CRITICAL
            remainingMillis < 3 * 3600 * 1000L -> SurvivalRiskLevel.CRITICAL
            remainingMillis < 8 * 3600 * 1000L -> SurvivalRiskLevel.HIGH
            else -> SurvivalRiskLevel.WARNING
        }

        val formattedCountdown = formatCountdown(max(0L, remainingMillis))

        val explanationFa = buildExplanation(
            flock = flock,
            ageDays = ageDays,
            tempCelsius = tempCelsius,
            threatType = threatType,
            totalHours = totalHours,
            isExpired = isExpired
        )

        return SurvivalCountdownState(
            threatType = threatType,
            riskLevel = riskLevel,
            totalSurvivalHours = totalHours,
            deadlineEpochMillis = activeDeadlineMillis,
            remainingMillis = remainingMillis,
            isExpired = isExpired,
            formattedCountdown = formattedCountdown,
            explanationFa = explanationFa,
            tempUsedCelsius = tempCelsius,
            tempSourceFa = tempSource
        )
    }

    fun formatCountdown(millis: Long): String {
        val totalSeconds = max(0L, millis) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun buildExplanation(
        flock: Flock,
        ageDays: Int,
        tempCelsius: Double,
        threatType: SurvivalThreatType,
        totalHours: Double,
        isExpired: Boolean
    ): String {
        val speciesFa = flock.species.titleFa
        val breedStr = if (flock.breed.isNotBlank()) "نژاد ${flock.breed}" else ""
        val flockDesc = listOf("گله $speciesFa", breedStr).filter { it.isNotBlank() }.joinToString(" ")
        val hoursRounded = toPersianDigits("%.1f".format(Locale.US, totalHours))
        val tempFormatted = toPersianDigits("%.1f".format(Locale.US, tempCelsius))
        val ageFa = toPersianDigits(ageDays)

        return when {
            isExpired -> "⚠️ زمان مجاز بقا به پایان رسیده است! پرندگان در شرایط مرگ فوری ناشی از کمبود قرار دارند. فوراً منبع را تأمین کنید."
            threatType == SurvivalThreatType.WATER_DEPLETED || threatType == SurvivalThreatType.BOTH_DEPLETED -> {
                val heatEffect = if (tempCelsius >= 30.0) {
                    "به دلیل دمای بالای محیط (${tempFormatted}°C) و له‌له زدن پرنده، سرعت از دست رفتن آب بدن بسیار بالاست."
                } else if (tempCelsius <= 15.0) {
                    "دمای خنک محیط (${tempFormatted}°C) سرعت تلفات را اندکی کاهش داده اما دهیدراتاسیون کلیوی آغاز شده است."
                } else {
                    "در دمای معتدل ${tempFormatted}°C، آستانه تحمل پرنده تا اولین تلفات محاسبه شده است."
                }
                "$flockDesc در سن $ageFa روزگی، در دمای ${tempFormatted}°C حداکثر حدود $hoursRounded ساعت پس از قطع آب زنده می‌ماند. $heatEffect"
            }
            else -> {
                "$flockDesc در سن $ageFa روزگی، در دمای ${tempFormatted}°C حداکثر تا $hoursRounded ساعت بدون دان مقاومت متابولیک دارد."
            }
        }
    }
}
