package com.example.poultryapp.data.local

import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.data.model.ProductionType
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.JalaliDateHelper

data class WeeklyConsumptionRate(
    val week: Int,
    val dailyFeedGrams: Double,
    val dailyWaterMl: Double
) {
    val ageDaysRangeFa: String
        get() {
            val startDay = (week - 1) * 7 + 1
            val endDay = week * 7
            return "روز ${com.example.poultryapp.engine.toPersianDigits(startDay)} تا ${com.example.poultryapp.engine.toPersianDigits(endDay)}"
        }
}

data class DailyConsumptionRate(
    val day: Int,
    val dailyFeedGrams: Double,
    val dailyWaterMl: Double
) {
    val dayDisplayFa: String
        get() = "روز ${com.example.poultryapp.engine.toPersianDigits(day)}"
}

data class CustomBreed(
    val id: Long = 0L,
    val name: String,
    val species: BirdSpecies,
    val productionType: ProductionType,
    val adultFeedGrams: Double,
    val adultWaterMl: Double,
    val weeklyRates: List<WeeklyConsumptionRate> = emptyList(),
    val dailyRates: List<DailyConsumptionRate> = emptyList(),
    val rateInputMode: String = "WEEKS", // "WEEKS" or "DAYS"
    val notes: String? = null
)

data class FlockSubGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val species: BirdSpecies,
    val breed: String,
    val birdCount: Int,
    val birthEpochMillis: Long = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000),
    val notes: String? = null
) {
    constructor(
        id: String = java.util.UUID.randomUUID().toString(),
        species: BirdSpecies,
        breed: String,
        birdCount: Int,
        ageDays: Int,
        notes: String? = null,
        birthEpochMillis: Long = System.currentTimeMillis() - (ageDays.coerceAtLeast(1) * 24L * 60 * 60 * 1000)
    ) : this(
        id = id,
        species = species,
        breed = breed,
        birdCount = birdCount,
        birthEpochMillis = birthEpochMillis,
        notes = notes
    )

    val ageDays: Int
        get() = JalaliDateHelper.calculateAge(birthEpochMillis).first

    val ageWeeks: Int
        get() = JalaliDateHelper.calculateAge(birthEpochMillis).second

    val ageDisplayFa: String
        get() = "${com.example.poultryapp.engine.toPersianDigits(ageWeeks)} هفته (${com.example.poultryapp.engine.toPersianDigits(ageDays)} روز)"
}

data class Flock(
    val id: Long = 0L,
    val name: String,
    val species: BirdSpecies,
    val breed: String,
    val birthEpochMillis: Long,
    val birdCount: Int,
    val photoUri: String? = null,
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val subGroups: List<FlockSubGroup> = emptyList(),
    val isFeedEnabled: Boolean = true,
    val isWaterEnabled: Boolean = true,
    val customTempCelsius: Double? = null,
    val medicationWithdrawalEndMillis: Long? = null,
    val activeMedicationName: String? = null
) {
    val isWithdrawalActive: Boolean
        get() = (medicationWithdrawalEndMillis ?: 0L) > System.currentTimeMillis()

    val withdrawalRemainingHours: Long
        get() = if (isWithdrawalActive) {
            val diff = (medicationWithdrawalEndMillis ?: 0L) - System.currentTimeMillis()
            (diff / (1000 * 60 * 60)).coerceAtLeast(0)
        } else 0L

    val withdrawalRemainingDays: Long
        get() = (withdrawalRemainingHours / 24).coerceAtLeast(0)

    val effectiveBirdCount: Int
        get() = if (subGroups.isNotEmpty()) subGroups.sumOf { it.birdCount } else birdCount

    val ageDays: Int
        get() = if (subGroups.isNotEmpty()) {
            subGroups.map { it.ageDays }.average().toInt().coerceAtLeast(1)
        } else {
            JalaliDateHelper.calculateAge(birthEpochMillis).first
        }

    val ageWeeks: Int
        get() = ((ageDays - 1) / 7 + 1).coerceAtLeast(1)

    val ageDisplayFa: String
        get() = "${com.example.poultryapp.engine.toPersianDigits(ageWeeks)} هفته (${com.example.poultryapp.engine.toPersianDigits(ageDays)} روز)"
}

enum class WarehouseCategory(val titleFa: String) {
    FEED("دان و خوراک"),
    VITAMIN("ویتامین و مکمل"),
    PROBIOTIC("پروبیوتیک"),
    MEDICINE("دارو و واکسن"),
    DISINFECTANT("ضدعفونی‌کننده"),
    EGG("تخم‌مرغ"),
    OTHER("سایر");

    companion object {
        fun fromString(value: String?): WarehouseCategory {
            if (value == null) return OTHER
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

data class WarehouseItem(
    val id: Long = 0L,
    val name: String,
    val category: WarehouseCategory,
    val currentStock: Double,
    val unit: String = "کیلوگرم",
    val pricePerUnit: Double = 0.0,
    val assignedFlockIds: List<Long> = emptyList(),
    val minAlertThreshold: Double = 50.0,
    val isForSale: Boolean = false,
    val notes: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class EggProduction(
    val id: Long = 0L,
    val flockId: Long,
    val flockName: String,
    val fertileCount: Int = 0,
    val tableEggCount: Int = 0,
    val healthyCount: Int = if (fertileCount + tableEggCount > 0) fertileCount + tableEggCount else 0,
    val brokenCount: Int = 0,
    val dirtyCount: Int = 0,
    val doubleYolkCount: Int = 0,
    val totalCount: Int = (if (fertileCount + tableEggCount > 0) fertileCount + tableEggCount else healthyCount) + brokenCount + dirtyCount + doubleYolkCount,
    val trayCount: Double = 0.0,
    val trayGrossWeightGrams: Double? = null,
    val netEggAverageWeightGrams: Double? = null,
    val layingRatePercent: Double = 0.0,
    val dateJalaliStr: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    constructor(
        id: Long = 0L,
        flockId: Long,
        flockName: String,
        healthyCount: Int,
        brokenCount: Int = 0,
        dirtyCount: Int = 0,
        doubleYolkCount: Int = 0,
        totalCount: Int = healthyCount + brokenCount + dirtyCount + doubleYolkCount,
        layingRatePercent: Double = 0.0,
        dateJalaliStr: String,
        timestampMillis: Long = System.currentTimeMillis(),
        notes: String? = null
    ) : this(
        id = id,
        flockId = flockId,
        flockName = flockName,
        fertileCount = 0,
        tableEggCount = healthyCount,
        healthyCount = healthyCount,
        brokenCount = brokenCount,
        dirtyCount = dirtyCount,
        doubleYolkCount = doubleYolkCount,
        totalCount = totalCount,
        trayCount = 0.0,
        trayGrossWeightGrams = null,
        netEggAverageWeightGrams = null,
        layingRatePercent = layingRatePercent,
        dateJalaliStr = dateJalaliStr,
        timestampMillis = timestampMillis,
        notes = notes
    )
}

enum class MortalityReason(val titleFa: String, val isTrueMortality: Boolean) {
    DISEASE("بیماری بالینی / عفونی", true),
    HEAT_COLD_STRESS("استرس دمایی / خفگی و قطعی فن", true),
    CANNIBALISM("کانیبالیسم و خودخوری", true),
    PREDATOR_ACCIDENT("شکارچی / حوادث فیزیکی سالن", true),
    CULLING("حذف انتخابی / پرنده ضعیف", true),
    LIVE_SALE("فروش پرنده زنده به عنوان محصول", false);

    companion object {
        fun fromString(value: String?): MortalityReason {
            if (value == null) return DISEASE
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DISEASE
        }
    }
}

data class FlockMortalityLog(
    val id: Long = 0L,
    val flockId: Long,
    val flockName: String,
    val count: Int,
    val reason: MortalityReason,
    val diagnosisHint: String? = null,
    val dateJalaliStr: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val notes: String? = null
)

enum class ContactCategory(val titleFa: String) {
    SLAUGHTERHOUSE("کشتارگاه و خریدار زنده"),
    EGG_BUYER("عمده‌فروش تخم‌مرغ"),
    FEED_SUPPLIER("کارخانه دان و تامین نهاده"),
    VET_PHARMACY("دامپزشک و داروخانه"),
    HATCHERY("جوجه‌کشی و هچری"),
    TECHNICIAN("تکنسین تهویه و تاسیسات"),
    OTHER("سایر همکاران");

    companion object {
        fun fromString(value: String?): ContactCategory {
            if (value == null) return OTHER
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

data class FarmContact(
    val id: Long = 0L,
    val fullName: String,
    val businessName: String? = null,
    val category: ContactCategory,
    val phonePrimary: String,
    val phoneSecondary: String? = null,
    val address: String? = null,
    val provinceCity: String? = null,
    val balanceTomans: Double = 0.0,
    val paymentTermsNotes: String? = null,
    val ratingStars: Int = 5,
    val createdAtMillis: Long = System.currentTimeMillis()
)


enum class TransactionType(val titleFa: String) {
    INCOME("درآمد"),
    EXPENSE("هزینه")
}

enum class TransactionCategory(val titleFa: String) {
    EGG_SALE("فروش تخم‌مرغ"),
    FEED_CONSUMPTION("مصرف دان"),
    MED_SUPPLEMENT("مصرف مکمل و دارو"),
    BIRD_SALE("فروش پرنده"),
    FEED_PURCHASE("خرید دان و خوراک"),
    MED_PURCHASE("خرید دارو و واکسن"),
    EQUIPMENT("تجهیزات و نگهداری"),
    OTHER_INCOME("سایر درآمدها"),
    OTHER_EXPENSE("سایر هزینه‌ها");

    companion object {
        fun fromString(value: String?): TransactionCategory {
            if (value == null) return OTHER_EXPENSE
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER_EXPENSE
        }
    }
}

data class FinancialTransaction(
    val id: Long = 0L,
    val title: String,
    val amountTomans: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val dateJalaliStr: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val relatedFlockId: Long? = null,
    val relatedFlockName: String? = null,
    val relatedWarehouseItemId: Long? = null,
    val quantity: Double = 0.0,
    val unit: String = "",
    val unitPriceTomans: Double = 0.0,
    val notes: String? = null
)

data class BirdTransfer(
    val id: Long = 0L,
    val fromFlockId: Long,
    val fromFlockName: String,
    val toFlockId: Long,
    val toFlockName: String,
    val birdCount: Int,
    val dateJalaliStr: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val notes: String? = null
)

data class FeedWaterCharge(
    val id: Long = 0L,
    val flockId: Long,
    val itemType: ChargeItemType,
    val chargedAmount: Double,
    val chargeTimestampMillis: Long,
    val dateJalaliStr: String,
    val notes: String? = null
)

data class CalendarSchedule(
    val id: Long = 0L,
    val flockId: Long? = null,
    val flockName: String? = null,
    val scheduleType: String, // ویتامین، واکسن، دارو، ضدعفونی، مکمل، سایر
    val title: String,
    val notes: String? = null,
    val daysOfWeek: List<Int> = emptyList(), // 0: شنبه, 1: یکشنبه, 2: دوشنبه, 3: سه‌شنبه, 4: چهارشنبه, 5: پنجشنبه, 6: جمعه
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val scheduleMode: String = "WEEKLY", // "WEEKLY" or "MONTHLY"
    val dayOfMonth: Int? = null // 1..31
) {
    val timeDisplayFa: String
        get() = "${com.example.poultryapp.engine.toPersianDigits("%02d".format(hour))}:${com.example.poultryapp.engine.toPersianDigits("%02d".format(minute))}"

    val daysDisplayFa: String
        get() {
            if (scheduleMode == "MONTHLY" && dayOfMonth != null) {
                return "روز ${com.example.poultryapp.engine.toPersianDigits(dayOfMonth)} هر ماه"
            }
            val names = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
            return if (daysOfWeek.size == 7) {
                "هر روز هفته"
            } else if (daysOfWeek.isEmpty()) {
                "بدون تکرار هفتگی"
            } else {
                daysOfWeek.sorted().joinToString("، ") { names.getOrElse(it) { "" } }
            }
        }
}

data class AppSettings(
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val alarmSound: String = "DEFAULT",
    val feedWaterAlertsEnabled: Boolean = true,
    val calendarAlertsEnabled: Boolean = true,
    val morningChargeAlertEnabled: Boolean = true,
    val morningChargeHour: Int = 7,
    val morningChargeMinute: Int = 0,
    val lowThresholdPercent: Double = 15.0,
    val farmProvince: String = "",
    val farmCity: String = "",
    val farmLatitude: Double? = null,
    val farmLongitude: Double? = null,
    val cachedTempCelsius: Double? = null,
    val cachedHumidityPercent: Int? = null,
    val cachedWindSpeedKmH: Double? = null,
    val cachedWeatherCode: Int? = null,
    val lastWeatherFetchMillis: Long = 0L,
    val isOnlineWeatherEnabled: Boolean = true,
    val manualTempCelsius: Double? = null,
    val isWorkerModeEnabled: Boolean = false,
    val managerPinHash: String = "",
    val managerRecoveryKey: String = "",
    val recoveryKey: String = managerRecoveryKey,
    val isBiometricEnabled: Boolean = true,
    val autoLockTimeoutMinutes: Int = 3,
    val isAutoAddEggsToWarehouseEnabled: Boolean = true
)

enum class UserRole(val titleFa: String) {
    MANAGER("مدیر / مالک فارم"),
    WORKER("کارگر سالن")
}


enum class RationItemType(val titleFa: String) {
    FEED("دان و جیره کامل"),
    SUPPLEMENT("مکمل و کنسانتره");

    companion object {
        fun fromString(value: String?): RationItemType {
            if (value == null) return FEED
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FEED
        }
    }
}

enum class RationPricingMode(val titleFa: String) {
    AUTO_FROM_INGREDIENTS("محاسبه خودکار از قیمت تک‌تک اقلام"),
    MANUAL_FLAT("تعیین دستی و کلی قیمت");

    companion object {
        fun fromString(value: String?): RationPricingMode {
            if (value == null) return AUTO_FROM_INGREDIENTS
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AUTO_FROM_INGREDIENTS
        }
    }
}

data class RationIngredient(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val percentageOrAmount: Double,
    val pricePerKg: Double = 0.0,
    val crudeProteinPercent: Double? = null,
    val category: String = "غلات",
    val energyKcalKg: Double? = null,
    val calciumPercent: Double? = null,
    val availablePhosphorusPercent: Double? = null,
    val methioninePercent: Double? = null,
    val lysinePercent: Double? = null,
    val sodiumPercent: Double? = null,
    val crudeFiberPercent: Double? = null,
    val fatPercent: Double? = null
)

data class RationPlan(
    val id: Long = 0L,
    val name: String,
    val itemType: RationItemType,
    val targetBird: String = "ALL",
    val targetStage: String = "رشد",
    val pricingMode: RationPricingMode = RationPricingMode.AUTO_FROM_INGREDIENTS,
    val manualPricePerKg: Double = 0.0,
    val batchBaseKg: Double = 100.0,
    val ingredients: List<RationIngredient> = emptyList(),
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val totalPercentage: Double
        get() = ingredients.sumOf { it.percentageOrAmount }

    val isBalanced100Percent: Boolean
        get() = Math.abs(totalPercentage - 100.0) < 0.5

    val effectivePricePerKg: Double
        get() = if (pricingMode == RationPricingMode.MANUAL_FLAT) {
            manualPricePerKg
        } else {
            val totalKg = if (totalPercentage > 0) totalPercentage else 100.0
            val totalCost = ingredients.sumOf { it.percentageOrAmount * it.pricePerKg }
            if (totalKg > 0) totalCost / totalKg else 0.0
        }

    val estimatedCrudeProteinPercent: Double?
        get() = calculateWeightedNutrient { it.crudeProteinPercent }

    val estimatedEnergyKcalKg: Double?
        get() = calculateWeightedNutrient { it.energyKcalKg }

    val estimatedCalciumPercent: Double?
        get() = calculateWeightedNutrient { it.calciumPercent }

    val estimatedPhosphorusPercent: Double?
        get() = calculateWeightedNutrient { it.availablePhosphorusPercent }

    val estimatedMethioninePercent: Double?
        get() = calculateWeightedNutrient { it.methioninePercent }

    val estimatedLysinePercent: Double?
        get() = calculateWeightedNutrient { it.lysinePercent }

    val estimatedSodiumPercent: Double?
        get() = calculateWeightedNutrient { it.sodiumPercent }

    val estimatedFiberPercent: Double?
        get() = calculateWeightedNutrient { it.crudeFiberPercent }

    val estimatedFatPercent: Double?
        get() = calculateWeightedNutrient { it.fatPercent }

    private inline fun calculateWeightedNutrient(selector: (RationIngredient) -> Double?): Double? {
        if (totalPercentage <= 0.0) return null
        var hasValue = false
        var weightedSum = 0.0
        for (ing in ingredients) {
            val v = selector(ing)
            if (v != null && v > 0.0) {
                hasValue = true
                weightedSum += v * ing.percentageOrAmount
            }
        }
        return if (hasValue) weightedSum / totalPercentage else null
    }
}

// ==================== ADVANCED FEATURE DATA MODELS ====================

data class BiosecurityCheckItem(
    val id: String,
    val season: com.example.poultryapp.engine.FarmSeason,
    val title: String,
    val description: String,
    val priority: String = "HIGH", // URGENT, HIGH, MEDIUM
    val isCompleted: Boolean = false,
    val completedDateJalaliStr: String? = null
)

data class LayingDropAlert(
    val flockId: Long,
    val flockName: String,
    val recentThreeDayAveragePercent: Double,
    val priorWeekAveragePercent: Double,
    val dropDifferencePercent: Double,
    val dateJalaliStr: String,
    val diagnosticChecklist: List<String>
)

data class FeasibilityInput(
    val species: BirdSpecies = BirdSpecies.CHICKEN,
    val breedName: String = "بومی تخم‌گذار / گوشتی",
    val birdCount: Int = 1000,
    val chickPriceTomans: Double = 35000.0,
    val feedPricePerKgTomans: Double = 22000.0,
    val estimatedMortalityPercent: Double = 5.0,
    val cycleDays: Int = 45,
    val targetWeightKg: Double = 2.6,
    val liveSalePricePerKgTomans: Double = 85000.0,
    val fixedOverheadTomans: Double = 15000000.0
)

data class FeasibilityResult(
    val totalWorkingCapitalTomans: Double,
    val totalFeedKg: Double,
    val totalRevenueTomans: Double,
    val netProfitTomans: Double,
    val breakEvenDay: Int,
    val roiPercent: Double,
    val profitPerBirdTomans: Double
)

data class DailyMarginalProfitPoint(
    val ageDay: Int,
    val bodyWeightKg: Double,
    val dailyWeightGainGrams: Double,
    val dailyFeedGrams: Double,
    val dailyRevenueTomans: Double,
    val dailyFeedCostTomans: Double,
    val dailyMarginalProfitTomans: Double,
    val fcr: Double
)

data class SlaughterOptimizationResult(
    val currentAgeDays: Int,
    val optimalHarvestAgeDays: Int,
    val goldenHarvestWindowRange: String,
    val dailyMarginalProfits: List<DailyMarginalProfitPoint>,
    val recommendationMessage: String,
    val isOverdueForSlaughter: Boolean
)

data class FlockObservationNote(
    val id: Long = 0L,
    val flockId: Long,
    val flockName: String,
    val note: String,
    val authorRole: UserRole = UserRole.MANAGER,
    val category: String = "OBSERVATION", // OBSERVATION, SHIFT_HANDOVER, MAINTENANCE, BIOSECURITY
    val dateJalaliStr: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

