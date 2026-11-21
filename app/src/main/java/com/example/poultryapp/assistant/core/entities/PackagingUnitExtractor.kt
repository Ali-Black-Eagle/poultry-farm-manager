package com.example.poultryapp.assistant.core.entities

enum class PoultryUnitType {
    EGG_TRAY,     // شانه (default 30)
    EGG_CARTON,   // کارتن (180 = 6 شانه)
    EGG_DOZEN,    // دوجین (12)
    FEED_SACK,    // کیسه / گونی (50 kg)
    FEED_TON,     // تن (1000 kg)
    FEED_KG,      // کیلو / کیلوگرم (1 kg)
    FEED_GRAM,    // گرم (0.001 kg)
    WATER_LITER,  // لیتر (1 L)
    WATER_ML      // میلی‌لیتر / سی‌سی (0.001 L)
}

data class ExtractedUnitQuantity(
    val baseQuantity: Double,
    val unitType: PoultryUnitType,
    val calculatedTotal: Double,
    val originalSpan: String,
    val startIndex: Int,
    val endIndex: Int
)

object PackagingUnitExtractor {

    private val WORD_BOUNDARY = """(?:\b|(?![a-zA-Z0-9\u0600-\u06FF]))"""

    private val UNIT_PATTERNS = listOf(
        // Egg Tray: شانه, tray
        Pair(Regex("""(\d+(?:\.\d+)?)\s*(?:شانه|شونه|trays?)$WORD_BOUNDARY"""), PoultryUnitType.EGG_TRAY),
        // Egg Carton: کارتن, carton
        Pair(Regex("""(\d+(?:\.\d+)?)\s*(?:کارتن|کارتون|cartons?)$WORD_BOUNDARY"""), PoultryUnitType.EGG_CARTON),
        // Egg Dozen: دوجین, dozen
        Pair(Regex("""(\d+(?:\.\d+)?)\s*(?:دوجین|dozens?)$WORD_BOUNDARY"""), PoultryUnitType.EGG_DOZEN),
        // Feed Sack: کیسه, گونی, sack, bag
        Pair(Regex("""(\d+(?:\.\d+)?)\s*(?:کیسه|گونی|sacks?|bags?)$WORD_BOUNDARY"""), PoultryUnitType.FEED_SACK),
        // Feed Ton: تن, ton, tonnes
        Pair(Regex("""(\d+(?:\.\d+)?)\s*(?:تن|tons?|tonnes?)$WORD_BOUNDARY"""), PoultryUnitType.FEED_TON),
        // Feed Kg: کیلو, کیلوگرم, kg, kilograms
        Pair(Regex("""(\d+(?:\.\d+)?)\s*(?:کیلوگرم|کیلو|kgs?|kilograms?)$WORD_BOUNDARY"""), PoultryUnitType.FEED_KG),
        // Feed Gram: گرم, grams, gr
        Pair(Regex("""(\d+(?:\.\d+)?)\s*(?:گرم|grams?|gr)$WORD_BOUNDARY"""), PoultryUnitType.FEED_GRAM),
        // Water Liter: لیتر, liter, liters
        Pair(Regex("""(\d+(?:\.\d+)?)\s*(?:لیتر|liters?)$WORD_BOUNDARY"""), PoultryUnitType.WATER_LITER),
        // Water Ml: میلی‌لیتر, میلی لیتر, سی‌سی, سی سی, ml, cc
        Pair(Regex("""(\d+(?:\.\d+)?)\s*(?:میلی‌لیتر|میلی لیتر|سی‌سی|سی سی|mls?|cc)$WORD_BOUNDARY"""), PoultryUnitType.WATER_ML)
    )

    fun extractUnits(
        text: String,
        eggsPerTray: Int = 30
    ): List<ExtractedUnitQuantity> {
        val results = mutableListOf<ExtractedUnitQuantity>()
        val normalized = com.example.poultryapp.assistant.core.normalize.TextNormalizer.normalize(text).normalized

        for ((regex, unitType) in UNIT_PATTERNS) {
            for (match in regex.findAll(normalized)) {
                val qty = match.groupValues[1].toDoubleOrNull() ?: continue
                val multiplier = when (unitType) {
                    PoultryUnitType.EGG_TRAY -> eggsPerTray.toDouble()
                    PoultryUnitType.EGG_CARTON -> (eggsPerTray * 6).toDouble()
                    PoultryUnitType.EGG_DOZEN -> 12.0
                    PoultryUnitType.FEED_SACK -> 50.0 // 50 kg
                    PoultryUnitType.FEED_TON -> 1000.0 // 1,000 kg
                    PoultryUnitType.FEED_KG -> 1.0 // 1 kg
                    PoultryUnitType.FEED_GRAM -> 0.001 // 0.001 kg (1 gram)
                    PoultryUnitType.WATER_LITER -> 1.0 // 1 liter
                    PoultryUnitType.WATER_ML -> 0.001 // 0.001 liter (1 ml)
                }
                val total = qty * multiplier
                results.add(
                    ExtractedUnitQuantity(
                        baseQuantity = qty,
                        unitType = unitType,
                        calculatedTotal = total,
                        originalSpan = match.value,
                        startIndex = match.range.first,
                        endIndex = match.range.last + 1
                    )
                )
            }
        }
        return results.sortedBy { it.startIndex }
    }
}
