package com.example.poultryapp.assistant.core.router

import com.example.poultryapp.assistant.core.entities.*
import com.example.poultryapp.assistant.core.nlu.*
import com.example.poultryapp.assistant.core.normalize.TextNormalizer
import java.time.LocalDate

class AssistantRouter(
    private val config: NluConfig = NluConfig()
) {

    companion object {
        val FLOCK_SELECTION_REQUIRED_INTENTS = setOf(
            IntentId.ADD_EGGS,
            IntentId.ADD_FEED_WATER,
            IntentId.LOG_MORTALITY,
            IntentId.SELL_BIRDS,
            IntentId.SLAUGHTER_BIRDS
        )

        private val CARCASS_WEIGHT_REGEX = Regex(
            """(?:وزن[\s\u200C]*لاشه|لاشه(?:[\s\u200C]*(?:به|با)?[\s\u200C]*وزن)?|carcass(?:\s*weight)?)\s*[:：]?\s*(\d+(?:\.\d+)?)\s*(کیلوگرم|کیلو|گرم|kgs?|kilograms?|grams?|gr)(?![a-zA-Z0-9\u0600-\u06FF])""",
            RegexOption.IGNORE_CASE
        )

        private val LIVE_WEIGHT_REGEX = Regex(
            """(?:وزن[\s\u200C]*زنده|(?:به|با)[\s\u200C]*وزن(?:\s*کل)?|وزن(?:\s*کل)?|live[\s\u200C]*weight|weight)\s*[:：]?\s*(\d+(?:\.\d+)?)\s*(کیلوگرم|کیلو|گرم|kgs?|kilograms?|grams?|gr)(?![a-zA-Z0-9\u0600-\u06FF])""",
            RegexOption.IGNORE_CASE
        )
    }

    fun route(
        rawText: String,
        availableFlocks: List<Pair<Long, String>> = emptyList(),
        referenceDate: LocalDate = LocalDate.now(),
        availableWarehouseItems: List<Pair<Long, String>> = emptyList()
    ): RouterDecision {
        val norm = TextNormalizer.normalize(rawText)
        if (norm.normalized.isBlank()) {
            return RouterDecision.Unsupported
        }

        val slots = mutableMapOf<SlotId, SlotValue>()
        var template = norm.normalized

        // 1. Extract Dates & Periods
        val dates = DateParser.extractDates(template, referenceDate)
        for (d in dates.reversed()) {
            if (d.isPeriod && d.periodStart != null && d.periodEnd != null) {
                slots[SlotId.PERIOD] = SlotValue(d.originalSpan, Pair(d.periodStart, d.periodEnd))
            } else {
                slots[SlotId.DATE] = SlotValue(d.originalSpan, d.date)
            }
            template = template.replaceRange(d.startIndex, d.endIndex, "<DATE>")
        }

        // 1.5 Extract explicit carcass & live weights BEFORE PackagingUnitExtractor and NumberParser
        // so weight digits (e.g. "وزن زنده ۲۴۰ کیلو و وزن لاشه ۱۷۵ کیلو") never collide with bird COUNT.
        val carcassMatches = CARCASS_WEIGHT_REGEX.findAll(template).toList()
        for (m in carcassMatches.reversed()) {
            val rawNum = m.groupValues[1].toDoubleOrNull() ?: continue
            val unit = m.groupValues[2].lowercase()
            val kg = if (unit.startsWith("گرم") || unit.startsWith("gr")) rawNum / 1000.0 else rawNum
            slots[SlotId.CARCASS_WEIGHT_KG] = SlotValue(m.value, kg)
            template = template.replaceRange(m.range.first, m.range.last + 1, " وزن لاشه <WEIGHT> ")
        }

        val liveWeightMatches = LIVE_WEIGHT_REGEX.findAll(template).toList()
        for (m in liveWeightMatches.reversed()) {
            val rawNum = m.groupValues[1].toDoubleOrNull() ?: continue
            val unit = m.groupValues[2].lowercase()
            val kg = if (unit.startsWith("گرم") || unit.startsWith("gr")) rawNum / 1000.0 else rawNum
            if (!slots.containsKey(SlotId.LIVE_WEIGHT_KG)) {
                slots[SlotId.LIVE_WEIGHT_KG] = SlotValue(m.value, kg)
            }
            template = template.replaceRange(m.range.first, m.range.last + 1, " وزن زنده <WEIGHT> ")
        }
        template = template.replace(Regex("""\s+"""), " ").trim()

        // 2. Extract Packaging Units (Trays, Cartons, Sacks, Feed Kg, Ton)
        val normKey = norm.matchKey
        val hasBirdExitCues = (normKey.contains("فروش") || normKey.contains("فروختم") ||
                normKey.contains("کشتار") || normKey.contains("ذبح") || normKey.contains("سربرید") ||
                normKey.contains("مرغ") || normKey.contains("پرنده") || normKey.contains("جوجه") ||
                normKey.contains("بوقلمون") || normKey.contains("بلدرچین") ||
                normKey.contains("slaughter") || normKey.contains("butcher") ||
                normKey.contains("sold birds") || normKey.contains("sell birds")) &&
                !normKey.contains("دان") && !normKey.contains("خوراک") && !normKey.contains("آب") &&
                !normKey.contains("انبار") && !normKey.contains("مصرف") && !normKey.contains("برداشت") &&
                !normKey.contains("تخم") && !normKey.contains("feed") && !normKey.contains("water") &&
                !normKey.contains("warehouse") && !normKey.contains("egg")

        val units = PackagingUnitExtractor.extractUnits(template)
        for (u in units.reversed()) {
            val countVal: Any = if (u.calculatedTotal % 1.0 == 0.0) u.calculatedTotal.toInt() else u.calculatedTotal
            if (hasBirdExitCues && (u.unitType == PoultryUnitType.FEED_KG || u.unitType == PoultryUnitType.FEED_GRAM)) {
                if (!slots.containsKey(SlotId.LIVE_WEIGHT_KG)) {
                    slots[SlotId.LIVE_WEIGHT_KG] = SlotValue(u.originalSpan, u.calculatedTotal)
                }
                template = template.replaceRange(u.startIndex, u.endIndex, "<WEIGHT>")
            } else if (u.unitType == PoultryUnitType.EGG_TRAY || u.unitType == PoultryUnitType.EGG_CARTON || u.unitType == PoultryUnitType.EGG_DOZEN) {
                slots[SlotId.COUNT] = SlotValue(u.originalSpan, countVal)
                template = template.replaceRange(u.startIndex, u.endIndex, "<NUM>")
            } else {
                slots[SlotId.COUNT] = SlotValue(u.originalSpan, countVal)
                slots[SlotId.AMOUNT] = SlotValue(u.originalSpan, u.calculatedTotal)
                template = template.replaceRange(u.startIndex, u.endIndex, "<NUM>")
            }
        }

        // 3. Extract Flocks BEFORE standalone numbers so digits inside flock names (e.g. "سالن 2")
        // never pollute COUNT/MONEY extraction.
        val matchedFlocks = EntityResolver.resolveAllFlocks(template, availableFlocks)
        if (matchedFlocks.size == 1) {
            val flock = matchedFlocks[0]
            slots[SlotId.FLOCK_ID] = SlotValue(flock.originalSpan, flock.flockId)
            slots[SlotId.FLOCK_NAME] = SlotValue(flock.originalSpan, flock.flockName)
        } else if (matchedFlocks.size > 1) {
            val ids = matchedFlocks.map { it.flockId }.distinct()
            val names = matchedFlocks.joinToString("، ") { it.flockName }
            slots[SlotId.SELECTED_FLOCK_IDS] = SlotValue(names, ids)
        }
        if (matchedFlocks.isNotEmpty()) {
            val uniqueSpans = matchedFlocks
                .filter { it.startIndex >= 0 && it.endIndex > it.startIndex && it.endIndex <= template.length }
                .map { it.startIndex to it.endIndex }
                .distinct()
                .sortedByDescending { it.first }
            for ((startIdx, endIdx) in uniqueSpans) {
                template = template.replaceRange(startIdx, endIdx, "<FLOCK>")
            }
        }

        // 4. Extract Standalone Numbers (outside dates, weights, units, and flock names)
        val numbers = NumberParser.extractNumbers(template)

        // 5. Extract Money
        val money = MoneyParser.extractMoney(template, numbers)
        for (m in money.reversed()) {
            slots[SlotId.AMOUNT] = SlotValue(m.originalSpan, m.amountInTomans)
            slots[SlotId.SALE_AMOUNT_TOMANS] = SlotValue(m.originalSpan, m.amountInTomans)
            slots[SlotId.CURRENCY] = SlotValue(m.currency.symbolFa, m.currency)
            template = template.replaceRange(m.startIndex, m.endIndex, "<MONEY>")
        }

        // Remaining standalone numbers for count (even if money already filled AMOUNT/SALE_AMOUNT_TOMANS)
        if (!slots.containsKey(SlotId.COUNT)) {
            val standaloneNum = NumberParser.extractNumbers(template).firstOrNull()
            if (standaloneNum != null) {
                val numVal: Any = if (standaloneNum.isInteger) standaloneNum.intValue else standaloneNum.value
                slots[SlotId.COUNT] = SlotValue(standaloneNum.originalSpan, numVal)
                if (!slots.containsKey(SlotId.AMOUNT)) {
                    slots[SlotId.AMOUNT] = SlotValue(standaloneNum.originalSpan, standaloneNum.value)
                }
                template = template.replaceRange(standaloneNum.startIndex, standaloneNum.endIndex, "<NUM>")
            }
        }

        // 6. Extract Category
        val cat = EntityResolver.resolveCategory(norm.normalized)
        if (cat != null) {
            slots[SlotId.CATEGORY] = SlotValue(cat.originalSpan, cat.categoryKey)
            if (cat.isExpense) {
                slots[SlotId.TRANSACTION_TYPE] = SlotValue("EXPENSE", "EXPENSE")
            } else {
                slots[SlotId.TRANSACTION_TYPE] = SlotValue("INCOME", "INCOME")
            }
            template = template.replace(cat.originalSpan, "<CATEGORY>")
        }

        // 7. Domain Entity Extractions (Feed/Water, Mortality, Navigation, Medication, Symptoms)
        val feedWaterType = EntityResolver.resolveFeedWaterType(norm.normalized)
        if (feedWaterType != null) {
            slots[SlotId.FEED_WATER_TYPE] = SlotValue(feedWaterType, feedWaterType)
        }

        val screenTarget = EntityResolver.resolveScreenTarget(norm.normalized)
        if (screenTarget != null) {
            slots[SlotId.TARGET_SCREEN] = SlotValue(screenTarget.first, screenTarget.first + (screenTarget.second?.let { ":$it" } ?: ""))
        }

        val med = EntityResolver.resolveMedication(norm.normalized)
        if (med != null) {
            slots[SlotId.MEDICATION_NAME] = SlotValue(med, med)
        }

        val syms = EntityResolver.resolveSymptoms(norm.normalized)
        if (syms.isNotEmpty()) {
            slots[SlotId.SYMPTOM_TOKENS] = SlotValue(syms.joinToString("، "), syms)
        }

        // Warehouse Item Extraction
        val wh = EntityResolver.resolveWarehouseItem(norm.normalized, availableWarehouseItems)
        if (wh != null) {
            slots[SlotId.WAREHOUSE_ITEM_ID] = SlotValue(wh.second, wh.first)
            slots[SlotId.WAREHOUSE_ITEM_NAME] = SlotValue(wh.second, wh.second)
        }

        // Schedule Time Extraction (e.g. ساعت ۹ or ساعت 14:30)
        val timeMatch = Regex("""(?:ساعت\s*|at\s*)(\d{1,2})(?::(\d{2}))?""").find(norm.normalized)
        if (timeMatch != null) {
            val h = timeMatch.groupValues[1].toIntOrNull() ?: 9
            val m = timeMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            slots[SlotId.SCHEDULE_TIME] = SlotValue(timeMatch.value, Pair(h, m))
        }

        // 8. Score with NLU Engine
        val nluResult = IntentScorer.score(
            templateText = template,
            extractedSlots = slots,
            tokens = norm.tokens,
            lang = norm.lang,
            config = config
        )

        if (nluResult.intent == IntentId.LOG_MORTALITY || norm.normalized.contains("تلفات")) {
            val reason = EntityResolver.resolveMortalityReason(norm.normalized)
            slots[SlotId.MORTALITY_REASON] = SlotValue(reason, reason)
        }

        if (nluResult.intent == IntentId.TRANSFER_BIRDS) {
            val (fromFlock, toFlock) = EntityResolver.resolveFlocksForTransfer(norm.normalized, availableFlocks)
            if (fromFlock != null) {
                slots[SlotId.FLOCK_ID] = SlotValue(fromFlock.originalSpan, fromFlock.flockId)
                slots[SlotId.FLOCK_NAME] = SlotValue(fromFlock.originalSpan, fromFlock.flockName)
            }
            if (toFlock != null) {
                slots[SlotId.TARGET_FLOCK_ID] = SlotValue(toFlock.originalSpan, toFlock.flockId)
                slots[SlotId.TARGET_FLOCK_NAME] = SlotValue(toFlock.originalSpan, toFlock.flockName)
            }
        }

        // 9. Decide routing based on thresholds
        if (nluResult.intent == IntentId.GENERAL_QUESTION) {
            return RouterDecision.AskOnline(rawText)
        }

        if (nluResult.confidence >= config.acceptThreshold && nluResult.margin >= config.acceptMargin) {
            // Check missing required slots & mandatory multi-flock selection
            val requiresFlockSelection =
                nluResult.intent in FLOCK_SELECTION_REQUIRED_INTENTS && availableFlocks.size > 1
            val hasSingleFlockTarget = slots.containsKey(SlotId.FLOCK_ID)
            val hasMultiAllocations =
                (slots[SlotId.FLOCK_ALLOCATIONS]?.typedValue as? List<*>)?.isNotEmpty() == true
            val hasSelectedMultiFlocks =
                (slots[SlotId.SELECTED_FLOCK_IDS]?.typedValue as? List<*>)?.isNotEmpty() == true

            val missing = when {
                requiresFlockSelection && hasSelectedMultiFlocks && !hasMultiAllocations ->
                    SlotId.FLOCK_ALLOCATIONS
                requiresFlockSelection && !hasSingleFlockTarget && !hasMultiAllocations ->
                    SlotId.FLOCK_ID
                else -> when (nluResult.intent) {
                    IntentId.ADD_EGGS -> if (!slots.containsKey(SlotId.COUNT)) SlotId.COUNT else null
                    IntentId.ADD_TRANSACTION -> if (!slots.containsKey(SlotId.AMOUNT)) SlotId.AMOUNT else null
                    IntentId.ADD_FEED_WATER -> if (!slots.containsKey(SlotId.COUNT)) SlotId.COUNT else null
                    IntentId.LOG_MORTALITY -> if (!slots.containsKey(SlotId.COUNT)) SlotId.COUNT else null
                    IntentId.SELL_BIRDS -> if (!slots.containsKey(SlotId.COUNT)) SlotId.COUNT else null
                    IntentId.SLAUGHTER_BIRDS -> if (!slots.containsKey(SlotId.COUNT)) SlotId.COUNT else null
                    IntentId.TRANSFER_BIRDS -> if (!slots.containsKey(SlotId.COUNT)) SlotId.COUNT else null
                    IntentId.CONSUME_WAREHOUSE -> if (!slots.containsKey(SlotId.COUNT)) SlotId.COUNT else null
                    else -> null
                }
            }

            return if (missing != null) {
                RouterDecision.AskSlot(missing, nluResult)
            } else {
                RouterDecision.Execute(nluResult.intent, slots)
            }
        }

        if (nluResult.confidence >= config.clarifyThreshold) {
            val options = mutableListOf(nluResult.intent)
            nluResult.alternatives.forEach { options.add(it.first) }
            return RouterDecision.Clarify(options.distinct(), nluResult)
        }

        return RouterDecision.AskOnline(rawText)
    }
}

