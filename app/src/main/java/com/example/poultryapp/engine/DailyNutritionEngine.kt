package com.example.poultryapp.engine

import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.data.model.BreedCatalog
import com.example.poultryapp.data.model.ProductionType
import java.util.Locale

data class FlockDailyRequirement(
    val perBirdFeedGrams: Double,
    val perBirdWaterMl: Double,
    val totalFeedGrams: Double,
    val totalWaterMl: Double,
    val formattedFeedPerBird: String,
    val formattedWaterPerBird: String,
    val formattedTotalFeed: String,
    val formattedTotalWater: String,
    val season: FarmSeason,
    val week: Int
)

data class FeedProjection30Days(
    val totalFeedKg: Double,
    val totalEstimatedCostTomans: Double,
    val formattedTotalFeed: String,
    val formattedCostTomans: String,
    val assignedFlocksCount: Int,
    val totalBirdsCount: Int,
    val dailyAverageFeedKg: Double,
    val formattedDailyAverageFeed: String
)

object DailyNutritionEngine {

    fun getCurrentSeason(): FarmSeason = JalaliDateHelper.getCurrentSeason()

    /**
     * Calculates combined daily feed and water requirement for a Flock,
     * summing up all individual sub-groups (breeds and ages) with precision.
     */
    fun calculateFlockRequirement(
        flock: com.example.poultryapp.data.local.Flock,
        season: FarmSeason = JalaliDateHelper.getCurrentSeason(),
        customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList(),
        tempCelsius: Double? = null
    ): FlockDailyRequirement {
        val effectiveTemp = flock.customTempCelsius ?: tempCelsius
        if (flock.subGroups.isNotEmpty()) {
            var sumFeedGrams = 0.0
            var sumWaterMl = 0.0
            var totalBirds = 0
            for (sg in flock.subGroups) {
                val req = calculateDailyRequirement(
                    species = sg.species,
                    breedName = sg.breed,
                    ageDays = sg.ageDays,
                    birdCount = sg.birdCount,
                    season = season,
                    customBreeds = customBreeds,
                    tempCelsius = effectiveTemp
                )
                sumFeedGrams += req.totalFeedGrams
                sumWaterMl += req.totalWaterMl
                totalBirds += sg.birdCount
            }
            val effectiveBirds = totalBirds.coerceAtLeast(1)
            val avgFeedPerBird = sumFeedGrams / effectiveBirds
            val avgWaterPerBird = sumWaterMl / effectiveBirds
            val avgAgeDays = flock.subGroups.map { it.ageDays }.average().toInt()
            val week = ((avgAgeDays.coerceAtLeast(1) - 1) / 7 + 1)

            return FlockDailyRequirement(
                perBirdFeedGrams = avgFeedPerBird,
                perBirdWaterMl = avgWaterPerBird,
                totalFeedGrams = sumFeedGrams,
                totalWaterMl = sumWaterMl,
                formattedFeedPerBird = formatFeed(avgFeedPerBird),
                formattedWaterPerBird = formatWater(avgWaterPerBird),
                formattedTotalFeed = formatFeed(sumFeedGrams),
                formattedTotalWater = formatWater(sumWaterMl),
                season = season,
                week = week
            )
        } else {
            return calculateDailyRequirement(
                species = flock.species,
                breedName = flock.breed,
                ageDays = flock.ageDays,
                birdCount = flock.effectiveBirdCount,
                season = season,
                customBreeds = customBreeds,
                tempCelsius = effectiveTemp
            )
        }
    }

    /**
     * Calculates daily feed (g) and water (ml) per bird and flock total.
     */
    fun calculateDailyRequirement(
        species: BirdSpecies,
        breedName: String,
        ageDays: Int,
        birdCount: Int,
        season: FarmSeason = JalaliDateHelper.getCurrentSeason(),
        customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList(),
        tempCelsius: Double? = null
    ): FlockDailyRequirement {
        val week = ((ageDays.coerceAtLeast(1) - 1) / 7 + 1)
        val prodType = BreedCatalog.getProductionType(species, breedName, customBreeds)

        val feedMultiplier = if (tempCelsius != null) {
            when {
                tempCelsius < 10.0 -> 1.08
                tempCelsius < 18.0 -> 1.04
                tempCelsius <= 26.0 -> 1.00
                tempCelsius <= 32.0 -> 0.95
                else -> 0.88
            }
        } else {
            1.00
        }

        val waterMultiplier = if (tempCelsius != null) {
            SurvivalCalculationEngine.calculateWaterTemperatureMultiplier(tempCelsius)
        } else {
            when (season) {
                FarmSeason.SUMMER -> 1.25
                FarmSeason.WINTER -> 0.95
                FarmSeason.SPRING, FarmSeason.AUTUMN -> 1.00
            }
        }

        val customBreed = customBreeds.find {
            it.species == species && it.name.trim().equals(breedName.trim(), ignoreCase = true)
        }

        val finalPerBirdFeed: Double
        val finalPerBirdWater: Double

        if (customBreed != null) {
            val (rawFeed, rawWater) = getCustomBreedFeedAndWater(customBreed, ageDays.coerceAtLeast(1), week)
            finalPerBirdFeed = (rawFeed * feedMultiplier).coerceAtLeast(1.0)
            finalPerBirdWater = (rawWater * waterMultiplier).coerceAtLeast(2.0)
        } else {
            val baseFeedGrams = getBaseFeedGramsForDay(species, prodType, ageDays.coerceAtLeast(1), breedName)
            val waterToFeedRatio = when (species) {
                BirdSpecies.DUCK, BirdSpecies.GOOSE -> 2.6
                BirdSpecies.TURKEY, BirdSpecies.GUINEA_FOWL -> 2.1
                BirdSpecies.QUAIL -> 1.9
                BirdSpecies.CHICKEN -> 2.0
            }

            finalPerBirdFeed = (baseFeedGrams * feedMultiplier).coerceAtLeast(1.0)
            finalPerBirdWater = (baseFeedGrams * waterToFeedRatio * waterMultiplier).coerceAtLeast(2.0)
        }

        val totalFeedGrams = finalPerBirdFeed * birdCount.coerceAtLeast(1)
        val totalWaterMl = finalPerBirdWater * birdCount.coerceAtLeast(1)

        return FlockDailyRequirement(
            perBirdFeedGrams = finalPerBirdFeed,
            perBirdWaterMl = finalPerBirdWater,
            totalFeedGrams = totalFeedGrams,
            totalWaterMl = totalWaterMl,
            formattedFeedPerBird = formatFeed(finalPerBirdFeed),
            formattedWaterPerBird = formatWater(finalPerBirdWater),
            formattedTotalFeed = formatFeed(totalFeedGrams),
            formattedTotalWater = formatWater(totalWaterMl),
            season = season,
            week = week
        )
    }

    /**
     * Resolves feed and water for a custom breed with smooth continuous day-by-day interpolation.
     */
    private fun getCustomBreedFeedAndWater(
        customBreed: com.example.poultryapp.data.local.CustomBreed,
        ageDays: Int,
        week: Int
    ): Pair<Double, Double> {
        // Priority 1: User explicitly configured daily rates
        if (customBreed.rateInputMode == "DAYS" && customBreed.dailyRates.isNotEmpty()) {
            val sorted = customBreed.dailyRates.sortedBy { it.day }
            val exact = sorted.find { it.day == ageDays }
            if (exact != null) {
                return Pair(exact.dailyFeedGrams, exact.dailyWaterMl)
            }
            if (ageDays <= sorted.first().day) {
                return Pair(sorted.first().dailyFeedGrams, sorted.first().dailyWaterMl)
            }
            if (ageDays >= sorted.last().day) {
                return Pair(customBreed.adultFeedGrams, customBreed.adultWaterMl)
            }
            for (i in 0 until sorted.size - 1) {
                val r1 = sorted[i]
                val r2 = sorted[i + 1]
                if (ageDays in r1.day..r2.day) {
                    val ratio = (ageDays - r1.day).toDouble() / (r2.day - r1.day).toDouble()
                    val f = r1.dailyFeedGrams + ratio * (r2.dailyFeedGrams - r1.dailyFeedGrams)
                    val w = r1.dailyWaterMl + ratio * (r2.dailyWaterMl - r1.dailyWaterMl)
                    return Pair(f, w)
                }
            }
            return Pair(sorted.last().dailyFeedGrams, sorted.last().dailyWaterMl)
        }

        // Priority 2: Weekly rates configured by user
        if (customBreed.weeklyRates.isNotEmpty()) {
            val weekRate = customBreed.weeklyRates.find { it.week == week }
            val rawFeed = weekRate?.dailyFeedGrams ?: customBreed.adultFeedGrams
            val rawWater = weekRate?.dailyWaterMl ?: customBreed.adultWaterMl
            return Pair(rawFeed, rawWater)
        }

        // Priority 3: Fallback from daily rates if present
        if (customBreed.dailyRates.isNotEmpty()) {
            val sorted = customBreed.dailyRates.sortedBy { it.day }
            val exact = sorted.find { it.day == ageDays }
            if (exact != null) {
                return Pair(exact.dailyFeedGrams, exact.dailyWaterMl)
            }
            if (ageDays in sorted.first().day..sorted.last().day) {
                for (i in 0 until sorted.size - 1) {
                    val r1 = sorted[i]
                    val r2 = sorted[i + 1]
                    if (ageDays in r1.day..r2.day) {
                        val ratio = (ageDays - r1.day).toDouble() / (r2.day - r1.day).toDouble()
                        val f = r1.dailyFeedGrams + ratio * (r2.dailyFeedGrams - r1.dailyFeedGrams)
                        val w = r1.dailyWaterMl + ratio * (r2.dailyWaterMl - r1.dailyWaterMl)
                        return Pair(f, w)
                    }
                }
            }
        }

        return Pair(customBreed.adultFeedGrams, customBreed.adultWaterMl)
    }

    /**
     * Generates recommended standard weekly rates (Week 1..10) as a template for custom breeds.
     */
    fun generateStandardWeeklyRates(
        species: BirdSpecies,
        prodType: ProductionType,
        weeks: Int = 10,
        breedName: String = ""
    ): List<com.example.poultryapp.data.local.WeeklyConsumptionRate> {
        val waterRatio = when (species) {
            BirdSpecies.DUCK, BirdSpecies.GOOSE -> 2.6
            BirdSpecies.TURKEY, BirdSpecies.GUINEA_FOWL -> 2.1
            BirdSpecies.QUAIL -> 1.9
            BirdSpecies.CHICKEN -> 2.0
        }
        return (1..weeks).map { w ->
            // Midpoint of week: day (w-1)*7 + 4
            val day = ((w - 1) * 7 + 4).coerceAtLeast(1)
            val feed = getBaseFeedGramsForDay(species, prodType, day, breedName)
            val water = feed * waterRatio
            com.example.poultryapp.data.local.WeeklyConsumptionRate(
                week = w,
                dailyFeedGrams = Math.round(feed * 10.0) / 10.0,
                dailyWaterMl = Math.round(water * 10.0) / 10.0
            )
        }
    }

    /**
     * Generates recommended standard daily rates (Day 1..days) as a template for custom breeds.
     */
    fun generateStandardDailyRates(
        species: BirdSpecies,
        prodType: ProductionType,
        days: Int = 42,
        breedName: String = ""
    ): List<com.example.poultryapp.data.local.DailyConsumptionRate> {
        val waterRatio = when (species) {
            BirdSpecies.DUCK, BirdSpecies.GOOSE -> 2.6
            BirdSpecies.TURKEY, BirdSpecies.GUINEA_FOWL -> 2.1
            BirdSpecies.QUAIL -> 1.9
            BirdSpecies.CHICKEN -> 2.0
        }
        return (1..days).map { d ->
            val feed = getBaseFeedGramsForDay(species, prodType, d, breedName)
            val water = feed * waterRatio
            com.example.poultryapp.data.local.DailyConsumptionRate(
                day = d,
                dailyFeedGrams = Math.round(feed * 10.0) / 10.0,
                dailyWaterMl = Math.round(water * 10.0) / 10.0
            )
        }
    }

    /**
     * Converts weekly rates to continuous day-by-day rates.
     */
    fun convertWeeklyToDailyRates(
        weeklyRates: List<com.example.poultryapp.data.local.WeeklyConsumptionRate>,
        adultFeed: Double,
        adultWater: Double,
        maxDays: Int = 60
    ): List<com.example.poultryapp.data.local.DailyConsumptionRate> {
        if (weeklyRates.isEmpty()) {
            return (1..maxDays).map { d ->
                com.example.poultryapp.data.local.DailyConsumptionRate(d, adultFeed, adultWater)
            }
        }
        val sorted = weeklyRates.sortedBy { it.week }
        return (1..maxDays).map { day ->
            val week = ((day - 1) / 7) + 1
            val exactWeek = sorted.find { it.week == week }
            val feed: Double
            val water: Double
            if (week > sorted.last().week) {
                feed = adultFeed
                water = adultWater
            } else {
                val currentFeed = exactWeek?.dailyFeedGrams ?: adultFeed
                val currentWater = exactWeek?.dailyWaterMl ?: adultWater
                val nextWeek = sorted.find { it.week == week + 1 }
                if (nextWeek != null) {
                    val progress = ((day - 1) % 7) / 7.0
                    feed = currentFeed + (nextWeek.dailyFeedGrams - currentFeed) * progress
                    water = currentWater + (nextWeek.dailyWaterMl - currentWater) * progress
                } else {
                    feed = currentFeed
                    water = currentWater
                }
            }
            com.example.poultryapp.data.local.DailyConsumptionRate(
                day = day,
                dailyFeedGrams = Math.round(feed * 10.0) / 10.0,
                dailyWaterMl = Math.round(water * 10.0) / 10.0
            )
        }
    }

    /**
     * Converts daily rates to weekly rates (averaging days in each week).
     */
    fun convertDailyToWeeklyRates(
        dailyRates: List<com.example.poultryapp.data.local.DailyConsumptionRate>,
        adultFeed: Double,
        adultWater: Double
    ): List<com.example.poultryapp.data.local.WeeklyConsumptionRate> {
        if (dailyRates.isEmpty()) return emptyList()
        val maxDay = dailyRates.maxOf { it.day }
        val maxWeek = ((maxDay - 1) / 7) + 1
        return (1..maxWeek).map { w ->
            val startDay = (w - 1) * 7 + 1
            val endDay = w * 7
            val weekDays = dailyRates.filter { it.day in startDay..endDay }
            val avgFeed = if (weekDays.isNotEmpty()) weekDays.map { it.dailyFeedGrams }.average() else adultFeed
            val avgWater = if (weekDays.isNotEmpty()) weekDays.map { it.dailyWaterMl }.average() else adultWater
            com.example.poultryapp.data.local.WeeklyConsumptionRate(
                week = w,
                dailyFeedGrams = Math.round(avgFeed * 10.0) / 10.0,
                dailyWaterMl = Math.round(avgWater * 10.0) / 10.0
            )
        }
    }

    fun getDefaultAdultFeedGrams(species: BirdSpecies, prodType: ProductionType): Double {
        return getBaseFeedGramsForDay(species, prodType, 175)
    }

    fun getDefaultAdultWaterMl(species: BirdSpecies, prodType: ProductionType): Double {
        val feed = getDefaultAdultFeedGrams(species, prodType)
        val waterRatio = when (species) {
            BirdSpecies.DUCK, BirdSpecies.GOOSE -> 2.6
            BirdSpecies.TURKEY, BirdSpecies.GUINEA_FOWL -> 2.1
            BirdSpecies.QUAIL -> 1.9
            BirdSpecies.CHICKEN -> 2.0
        }
        return feed * waterRatio
    }

    fun getBaseAdultDailyFeed(
        species: BirdSpecies,
        breedName: String = "",
        prodType: ProductionType = ProductionType.DUAL
    ): Double {
        return getBaseFeedGramsForDay(species, prodType, 224, breedName)
    }

    fun getBaseAdultDailyWater(
        species: BirdSpecies,
        breedName: String = "",
        prodType: ProductionType = ProductionType.DUAL
    ): Double {
        val feed = getBaseAdultDailyFeed(species, breedName, prodType)
        val waterRatio = when (species) {
            BirdSpecies.DUCK, BirdSpecies.GOOSE -> 2.6
            BirdSpecies.TURKEY, BirdSpecies.GUINEA_FOWL -> 2.1
            BirdSpecies.QUAIL -> 1.9
            BirdSpecies.CHICKEN -> 2.0
        }
        return feed * waterRatio
    }

    /**
     * Helper for smooth linear interpolation between age (in days) checkpoints.
     */
    private fun interpolateDaily(day: Int, checkpoints: List<Pair<Int, Double>>): Double {
        if (checkpoints.isEmpty()) return 50.0
        val safeDay = day.coerceAtLeast(1)
        if (safeDay <= checkpoints.first().first) return checkpoints.first().second
        if (safeDay >= checkpoints.last().first) return checkpoints.last().second
        for (i in 0 until checkpoints.size - 1) {
            val (d1, f1) = checkpoints[i]
            val (d2, f2) = checkpoints[i + 1]
            if (safeDay in d1..d2) {
                val ratio = (safeDay - d1).toDouble() / (d2 - d1).toDouble()
                return f1 + ratio * (f2 - f1)
            }
        }
        return checkpoints.last().second
    }

    /**
     * Scientific reference base feed intake in grams per day based on species, production type, breed
     * and exact age in days (continuous day-by-day growth curve).
     */
    fun getBaseFeedGramsForDay(
        species: BirdSpecies,
        prodType: ProductionType,
        ageDays: Int,
        breedName: String = ""
    ): Double {
        val b = breedName.trim()
        if (b.contains("آمروک")) {
            // Amrock heritage dual-purpose standards
            return interpolateDaily(
                ageDays,
                listOf(
                    1 to 10.0,
                    7 to 15.0,
                    14 to 25.0,
                    21 to 38.0,
                    28 to 52.0,
                    42 to 72.0,
                    56 to 88.0,
                    84 to 102.0,
                    112 to 115.0,
                    140 to 124.0,
                    154 to 130.0
                )
            )
        }
        if (b.contains("بلک استار") || b.contains("لوهمن") || b.contains("ایزا") ||
            b.contains("قهوه‌ای") || b.contains("براون") || b.contains("هارکو") ||
            b.contains("سکس لینک") || b.contains("بلک سکس")
        ) {
            // Brown Layer / Black Star pullet rearing standards (ISA Brown / Lohmann Brown / Black Sex-Link)
            // Adult body weight: ~2.1 - 2.4 kg.
            // Day 49 (Week 7) target: 63.0g/day per pullet.
            return interpolateDaily(
                ageDays,
                listOf(
                    1 to 12.0,
                    7 to 16.0,
                    14 to 24.0,
                    21 to 33.0,
                    28 to 42.0,
                    35 to 50.0,
                    42 to 57.0,
                    49 to 63.0,
                    56 to 70.0,
                    70 to 80.0,
                    84 to 88.0,
                    98 to 95.0,
                    112 to 102.0,
                    126 to 110.0,
                    140 to 116.0,
                    154 to 125.0
                )
            )
        }
        if (b.contains("برهما")) {
            // Brahma giant ornamental heritage standards (slow steady growth up to maturity at ~32 weeks)
            return interpolateDaily(
                ageDays,
                listOf(
                    1 to 12.0,
                    7 to 18.0,
                    14 to 28.0,
                    21 to 40.0,
                    28 to 52.0,
                    35 to 64.0,
                    42 to 76.0,
                    49 to 86.0,
                    56 to 96.0,
                    70 to 112.0,
                    84 to 125.0,
                    112 to 138.0,
                    140 to 148.0,
                    168 to 156.0,
                    196 to 162.0,
                    224 to 165.0
                )
            )
        }
        if (b.contains("استرالوپ") || b.contains("پلیموت") || b.contains("رود آیلند") ||
            b.contains("سوسکس") || b.contains("کوشین")
        ) {
            // Heavy heritage dual-purpose breeds (Australorp, Plymouth Rock, RIR, Sussex)
            // Adult body weight: ~2.5 - 3.5 kg.
            return interpolateDaily(
                ageDays,
                listOf(
                    1 to 12.0,
                    7 to 17.0,
                    14 to 26.0,
                    21 to 36.0,
                    28 to 46.0,
                    35 to 55.0,
                    42 to 64.0,
                    49 to 71.0,
                    56 to 78.0,
                    70 to 86.0,
                    84 to 95.0,
                    112 to 110.0,
                    140 to 125.0,
                    154 to 135.0
                )
            )
        }
        if (b.contains("گلپایگانی") || b.contains("مرندی") || b.contains("لاری") ||
            b.contains("بومی") || b.contains("محلی")
        ) {
            // Iranian Native & Dual-purpose breeds (Golpayegani, Marandi, Native)
            return interpolateDaily(
                ageDays,
                listOf(
                    1 to 10.0,
                    7 to 14.0,
                    14 to 22.0,
                    21 to 30.0,
                    28 to 38.0,
                    35 to 46.0,
                    42 to 54.0,
                    49 to 60.0,
                    56 to 66.0,
                    70 to 74.0,
                    84 to 80.0,
                    112 to 92.0,
                    140 to 105.0,
                    154 to 112.0
                )
            )
        }
        if (b.contains("شاخدار") || b.contains("گینه")) {
            return getGuineaFowlBaseFeedForDay(prodType, ageDays)
        }

        return when (species) {
            BirdSpecies.CHICKEN -> {
                when (prodType) {
                    ProductionType.MEAT -> {
                        // Broiler standards (Ross 308 / Cobb 500)
                        interpolateDaily(
                            ageDays,
                            listOf(
                                1 to 15.0,
                                7 to 34.0,
                                14 to 62.0,
                                21 to 98.0,
                                28 to 138.0,
                                35 to 172.0,
                                42 to 198.0,
                                49 to 212.0,
                                56 to 218.0
                            )
                        )
                    }
                    ProductionType.LAYER -> {
                        // Light White Layer standards (Hy-Line W-36 / LSL / Leghorn)
                        interpolateDaily(
                            ageDays,
                            listOf(
                                1 to 11.0,
                                7 to 15.0,
                                14 to 22.0,
                                21 to 29.0,
                                28 to 36.0,
                                35 to 42.0,
                                42 to 48.0,
                                49 to 53.0,
                                56 to 58.0,
                                70 to 65.0,
                                84 to 73.0,
                                112 to 88.0,
                                140 to 102.0,
                                154 to 115.0
                            )
                        )
                    }
                    ProductionType.DUAL, ProductionType.NATIVE, ProductionType.ORNAMENTAL -> {
                        // General dual-purpose / native / ornamental fallback
                        interpolateDaily(
                            ageDays,
                            listOf(
                                1 to 10.0,
                                7 to 14.0,
                                14 to 22.0,
                                21 to 30.0,
                                28 to 38.0,
                                35 to 46.0,
                                42 to 54.0,
                                49 to 60.0,
                                56 to 66.0,
                                70 to 74.0,
                                84 to 80.0,
                                112 to 92.0,
                                140 to 105.0,
                                154 to 112.0
                            )
                        )
                    }
                }
            }
            BirdSpecies.GUINEA_FOWL -> {
                getGuineaFowlBaseFeedForDay(prodType, ageDays)
            }
            BirdSpecies.DUCK -> {
                when (prodType) {
                    ProductionType.MEAT -> {
                        // Pekin meat duck
                        interpolateDaily(
                            ageDays,
                            listOf(
                                1 to 25.0,
                                7 to 50.0,
                                14 to 95.0,
                                28 to 160.0,
                                49 to 210.0,
                                63 to 230.0
                            )
                        )
                    }
                    else -> {
                        // Layer/Native duck
                        interpolateDaily(
                            ageDays,
                            listOf(
                                1 to 20.0,
                                14 to 45.0,
                                42 to 100.0,
                                84 to 145.0,
                                112 to 165.0
                            )
                        )
                    }
                }
            }
            BirdSpecies.TURKEY -> {
                // Turkey standards (Broad Breasted White / Bronze)
                interpolateDaily(
                    ageDays,
                    listOf(
                        1 to 25.0,
                        14 to 55.0,
                        28 to 110.0,
                        56 to 230.0,
                        84 to 360.0,
                        112 to 480.0,
                        140 to 575.0,
                        154 to 620.0
                    )
                )
            }
            BirdSpecies.GOOSE -> {
                // Goose standards
                interpolateDaily(
                    ageDays,
                    listOf(
                        1 to 35.0,
                        14 to 80.0,
                        35 to 170.0,
                        70 to 280.0,
                        90 to 320.0
                    )
                )
            }
            BirdSpecies.QUAIL -> {
                // Quail standards (Coturnix Japanese)
                interpolateDaily(
                    ageDays,
                    listOf(
                        1 to 4.0,
                        7 to 8.0,
                        14 to 14.0,
                        28 to 22.0,
                        42 to 32.0
                    )
                )
            }
        }
    }

    /**
     * Standards for Guinea Fowl (Numida meleagris) day-by-day continuous curve.
     */
    private fun getGuineaFowlBaseFeedForDay(prodType: ProductionType, ageDays: Int): Double {
        return when (prodType) {
            ProductionType.MEAT -> {
                interpolateDaily(
                    ageDays,
                    listOf(
                        1 to 10.0,
                        7 to 16.0,
                        14 to 25.0,
                        21 to 37.0,
                        28 to 50.0,
                        35 to 64.0,
                        42 to 78.0,
                        49 to 90.0,
                        70 to 102.0,
                        84 to 110.0
                    )
                )
            }
            else -> {
                interpolateDaily(
                    ageDays,
                    listOf(
                        1 to 8.0,
                        7 to 11.0,
                        14 to 20.0,
                        21 to 35.0,
                        28 to 45.0,
                        42 to 60.0,
                        56 to 74.0,
                        84 to 87.0,
                        112 to 96.0,
                        140 to 105.0
                    )
                )
            }
        }
    }

    /**
     * Retained for backward-compatibility with week-based references.
     */
    fun getBaseFeedGrams(species: BirdSpecies, prodType: ProductionType, week: Int, breedName: String = ""): Double {
        val day = ((week - 1) * 7 + 4).coerceAtLeast(1)
        return getBaseFeedGramsForDay(species, prodType, day, breedName)
    }

    /**
     * Calculates smart, dynamic 30-day feed requirement taking into account
     * continuous chick growth day-by-day (age increases by 1 each day, so week
     * and daily feed consumption increase dynamically rather than staying static).
     */
    fun calculate30DayFeedProjection(
        assignedFlocks: List<com.example.poultryapp.data.local.Flock>,
        pricePerKg: Double = 0.0,
        season: FarmSeason = JalaliDateHelper.getCurrentSeason(),
        customBreeds: List<com.example.poultryapp.data.local.CustomBreed> = emptyList(),
        tempCelsius: Double? = null
    ): FeedProjection30Days {
        if (assignedFlocks.isEmpty()) {
            return FeedProjection30Days(
                totalFeedKg = 0.0,
                totalEstimatedCostTomans = 0.0,
                formattedTotalFeed = "۰ کیلوگرم",
                formattedCostTomans = "۰ تومان",
                assignedFlocksCount = 0,
                totalBirdsCount = 0,
                dailyAverageFeedKg = 0.0,
                formattedDailyAverageFeed = "۰ کیلوگرم"
            )
        }

        var sumTotalFeedGramsOver30Days = 0.0
        var totalBirds = 0

        for (flock in assignedFlocks) {
            totalBirds += flock.effectiveBirdCount
            val effectiveTemp = flock.customTempCelsius ?: tempCelsius
            for (dayOffset in 1..30) {
                if (flock.subGroups.isNotEmpty()) {
                    for (sg in flock.subGroups) {
                        val req = calculateDailyRequirement(
                            species = sg.species,
                            breedName = sg.breed,
                            ageDays = sg.ageDays + dayOffset,
                            birdCount = sg.birdCount,
                            season = season,
                            customBreeds = customBreeds,
                            tempCelsius = effectiveTemp
                        )
                        sumTotalFeedGramsOver30Days += req.totalFeedGrams
                    }
                } else {
                    val req = calculateDailyRequirement(
                        species = flock.species,
                        breedName = flock.breed,
                        ageDays = flock.ageDays + dayOffset,
                        birdCount = flock.effectiveBirdCount,
                        season = season,
                        customBreeds = customBreeds,
                        tempCelsius = effectiveTemp
                    )
                    sumTotalFeedGramsOver30Days += req.totalFeedGrams
                }
            }
        }

        val totalFeedKg = sumTotalFeedGramsOver30Days / 1000.0
        val totalCost = totalFeedKg * pricePerKg.coerceAtLeast(0.0)
        val dailyAverageKg = totalFeedKg / 30.0

        return FeedProjection30Days(
            totalFeedKg = totalFeedKg,
            totalEstimatedCostTomans = totalCost,
            formattedTotalFeed = formatFeed(sumTotalFeedGramsOver30Days),
            formattedCostTomans = formatTomans(totalCost),
            assignedFlocksCount = assignedFlocks.size,
            totalBirdsCount = totalBirds,
            dailyAverageFeedKg = dailyAverageKg,
            formattedDailyAverageFeed = formatFeed(dailyAverageKg * 1000.0)
        )
    }

    /**
     * Formats Tomans currency with Persian digits and comma grouping.
     */
    fun formatTomans(amount: Double): String {
        val rounded = kotlin.math.round(amount).toLong()
        val formatter = java.text.NumberFormat.getIntegerInstance(Locale.US)
        val formatted = formatter.format(rounded)
        return "${toPersianDigits(formatted)} تومان"
    }

    /**
     * Formats feed weight in grams or kilograms with Persian digits.
     */
    fun formatFeed(grams: Double): String {
        return if (grams >= 1000.0) {
            val kg = grams / 1000.0
            val formatted = String.format(Locale.US, "%.2f", kg)
            "${toPersianDigits(formatted)} کیلوگرم"
        } else {
            val g = kotlin.math.round(grams).toInt()
            "${toPersianDigits(g)} گرم"
        }
    }

    /**
     * Formats water volume in milliliters or liters with Persian digits.
     */
    fun formatWater(ml: Double): String {
        return if (ml >= 1000.0) {
            val liters = ml / 1000.0
            val formatted = String.format(Locale.US, "%.2f", liters)
            "${toPersianDigits(formatted)} لیتر"
        } else {
            val m = kotlin.math.round(ml).toInt()
            "${toPersianDigits(m)} میلی‌لیتر"
        }
    }
}
