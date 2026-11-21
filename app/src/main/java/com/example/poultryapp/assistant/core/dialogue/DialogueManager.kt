package com.example.poultryapp.assistant.core.dialogue

import com.example.poultryapp.assistant.core.entities.EntityResolver
import com.example.poultryapp.assistant.core.entities.NumberParser
import com.example.poultryapp.assistant.core.entities.PackagingUnitExtractor
import com.example.poultryapp.assistant.core.entities.ResolvedFlock
import com.example.poultryapp.assistant.core.nlu.*
import com.example.poultryapp.assistant.core.normalize.TextNormalizer
import com.example.poultryapp.assistant.core.router.AssistantRouter

sealed interface DialogueResponse {
    data class PromptSlot(val slot: SlotId, val promptTextFa: String, val promptTextEn: String) : DialogueResponse
    data class SelectFlocks(
        val intent: IntentId,
        val slots: Map<SlotId, SlotValue>,
        val preselectedFlockIds: Set<Long> = emptySet(),
        val promptTextFa: String,
        val promptTextEn: String
    ) : DialogueResponse
    data class ConfirmAction(val intent: IntentId, val slots: Map<SlotId, SlotValue>, val isVoice: Boolean) : DialogueResponse
    data class ExecuteDirect(val intent: IntentId, val slots: Map<SlotId, SlotValue>) : DialogueResponse
    data class ClarifyOptions(val options: List<IntentId>, val slots: Map<SlotId, SlotValue> = emptyMap()) : DialogueResponse
    data class AnswerOnline(val queryText: String) : DialogueResponse
    data class Message(val textFa: String, val textEn: String) : DialogueResponse
}

class DialogueManager(
    private val router: AssistantRouter,
    private val alwaysConfirmEggs: Boolean = false
) {
    var state: DialogueState = DialogueState.Idle
        private set

    private val YES_WORDS = setOf("بله", "اره", "آره", "تایید", "باشه", "حتما", "بزن", "yes", "yeah", "ok", "okay", "sure", "confirm")
    private val NO_WORDS = setOf("نه", "نخیر", "لغو", "کنسل", "بیخیال", "no", "cancel", "stop", "nope")

    fun processInput(
        rawText: String,
        isVoice: Boolean,
        availableFlocks: List<Pair<Long, String>> = emptyList(),
        availableWarehouseItems: List<Pair<Long, String>> = emptyList()
    ): DialogueResponse {
        val norm = TextNormalizer.normalize(rawText)
        val matchKey = norm.matchKey

        // 1. Check Cancellation in any state
        if (NO_WORDS.any { matchKey == it || matchKey.startsWith("$it ") }) {
            state = DialogueState.Idle
            return DialogueResponse.Message("عملیات لغو شد.", "Operation cancelled.")
        }

        // 2. Check Confirmation if AwaitingConfirmation
        val currentState = state
        if (currentState is DialogueState.AwaitingConfirmation) {
            if (YES_WORDS.any { matchKey == it || matchKey.startsWith("$it ") }) {
                state = DialogueState.Idle
                return DialogueResponse.ExecuteDirect(currentState.intent, currentState.slots)
            }
        }

        // 3. Handle Flock Selection follow-up if SelectingFlocks
        if (currentState is DialogueState.SelectingFlocks) {
            val activeFlocks = if (availableFlocks.isNotEmpty()) availableFlocks else currentState.availableFlocks
            val matchedFlocks = EntityResolver.resolveAllFlocks(rawText, activeFlocks, allowBareAllKeyword = true)
            if (matchedFlocks.isNotEmpty()) {
                val updatedSlots = currentState.collectedSlots.toMutableMap()
                extractCountFromFollowUp(rawText, matchedFlocks)?.let { (span, numVal, doubleVal) ->
                    updatedSlots[SlotId.COUNT] = SlotValue(span, numVal)
                    if (currentState.targetIntent == IntentId.ADD_FEED_WATER) {
                        updatedSlots[SlotId.AMOUNT] = SlotValue(span, doubleVal)
                    }
                }

                if (matchedFlocks.size == 1) {
                    val chosen = matchedFlocks[0]
                    updatedSlots[SlotId.FLOCK_ID] = SlotValue(chosen.originalSpan, chosen.flockId)
                    updatedSlots[SlotId.FLOCK_NAME] = SlotValue(chosen.originalSpan, chosen.flockName)
                    updatedSlots.remove(SlotId.SELECTED_FLOCK_IDS)

                    return if (updatedSlots.containsKey(SlotId.COUNT)) {
                        buildPostSlotExecutionOrConfirm(currentState.targetIntent, updatedSlots, isVoice)
                    } else {
                        state = DialogueState.CollectingSlots(
                            targetIntent = currentState.targetIntent,
                            collectedSlots = updatedSlots,
                            missingSlot = SlotId.COUNT,
                            partialNlu = currentState.partialNlu
                        )
                        val (pFa, pEn) = promptForCount(currentState.targetIntent, chosen.flockName)
                        DialogueResponse.PromptSlot(SlotId.COUNT, pFa, pEn)
                    }
                } else {
                    val selectedIds = matchedFlocks.map { it.flockId }.toSet()
                    val names = matchedFlocks.joinToString("، ") { it.flockName }
                    updatedSlots.remove(SlotId.FLOCK_ID)
                    updatedSlots.remove(SlotId.FLOCK_NAME)
                    updatedSlots[SlotId.SELECTED_FLOCK_IDS] = SlotValue(names, selectedIds.toList())

                    state = currentState.copy(
                        collectedSlots = updatedSlots,
                        availableFlocks = activeFlocks,
                        preselectedFlockIds = selectedIds
                    )
                    return DialogueResponse.SelectFlocks(
                        intent = currentState.targetIntent,
                        slots = updatedSlots,
                        preselectedFlockIds = selectedIds,
                        promptTextFa = "گله‌های انتخاب‌شده آماده هستند. لطفاً مقدار دقیق هر گله را در کارت زیر وارد و تایید کنید:",
                        promptTextEn = "Selected flocks updated. Please enter exact quantities per flock and confirm."
                    )
                }
            } else if (norm.tokens.size <= 3 && !norm.matchKey.contains("?")) {
                val countUpdate = extractCountFromFollowUp(rawText, emptyList())
                if (countUpdate != null) {
                    val updatedSlots = currentState.collectedSlots.toMutableMap()
                    updatedSlots[SlotId.COUNT] = SlotValue(countUpdate.first, countUpdate.second)
                    if (currentState.targetIntent == IntentId.ADD_FEED_WATER) {
                        updatedSlots[SlotId.AMOUNT] = SlotValue(countUpdate.first, countUpdate.third)
                    }
                    state = currentState.copy(collectedSlots = updatedSlots)
                    return DialogueResponse.SelectFlocks(
                        intent = currentState.targetIntent,
                        slots = updatedSlots,
                        preselectedFlockIds = currentState.preselectedFlockIds,
                        promptTextFa = "مقدار ثبت شد؛ لطفاً گله یا گله‌های مورد نظر را انتخاب و تایید کنید:",
                        promptTextEn = "Quantity updated. Please select target flock(s) and confirm:"
                    )
                }
            }
        }

        // 4. Handle Slot follow-up if CollectingSlots
        if (currentState is DialogueState.CollectingSlots) {
            if (currentState.missingSlot == SlotId.COUNT) {
                val unit = PackagingUnitExtractor.extractUnits(norm.normalized).firstOrNull()
                if (unit != null) {
                    val countVal: Any = if (unit.calculatedTotal % 1.0 == 0.0) unit.calculatedTotal.toInt() else unit.calculatedTotal
                    currentState.collectedSlots[SlotId.COUNT] = SlotValue(unit.originalSpan, countVal)
                    if (currentState.targetIntent == IntentId.ADD_FEED_WATER) {
                        currentState.collectedSlots[SlotId.AMOUNT] = SlotValue(unit.originalSpan, unit.calculatedTotal)
                    }
                    return evaluatePostSlotFilling(currentState, isVoice, availableFlocks)
                }
                val num = NumberParser.extractNumbers(norm.normalized).firstOrNull()
                if (num != null) {
                    val numVal: Any = if (num.isInteger) num.intValue else num.value
                    currentState.collectedSlots[SlotId.COUNT] = SlotValue(num.originalSpan, numVal)
                    if (currentState.targetIntent == IntentId.ADD_FEED_WATER) {
                        currentState.collectedSlots[SlotId.AMOUNT] = SlotValue(num.originalSpan, num.value)
                    }
                    return evaluatePostSlotFilling(currentState, isVoice, availableFlocks)
                }
            } else if (currentState.missingSlot == SlotId.AMOUNT) {
                val num = NumberParser.extractNumbers(norm.normalized).firstOrNull()
                if (num != null) {
                    currentState.collectedSlots[SlotId.AMOUNT] = SlotValue(num.originalSpan, num.value)
                    return evaluatePostSlotFilling(currentState, isVoice, availableFlocks)
                }
            }
        }

        // 5. Default: Route via AssistantRouter
        val decision = router.route(rawText, availableFlocks, availableWarehouseItems = availableWarehouseItems)

        return when (decision) {
            is RouterDecision.Execute -> {
                buildPostSlotExecutionOrConfirm(decision.intent, decision.slots, isVoice)
            }

            is RouterDecision.AskSlot -> {
                val collected = decision.partial.slots.toMutableMap()
                if (decision.slot == SlotId.FLOCK_ID || decision.slot == SlotId.FLOCK_ALLOCATIONS) {
                    val preselected = (collected[SlotId.SELECTED_FLOCK_IDS]?.typedValue as? List<*>)
                        ?.mapNotNull { (it as? Number)?.toLong() }
                        ?.toSet()
                        ?: emptySet()
                    state = DialogueState.SelectingFlocks(
                        targetIntent = decision.partial.intent,
                        collectedSlots = collected,
                        availableFlocks = availableFlocks,
                        preselectedFlockIds = preselected,
                        partialNlu = decision.partial
                    )
                    val (promptFa, promptEn) = promptForFlocks(
                        intent = decision.partial.intent,
                        hasPreselected = preselected.isNotEmpty()
                    )
                    DialogueResponse.SelectFlocks(
                        intent = decision.partial.intent,
                        slots = collected,
                        preselectedFlockIds = preselected,
                        promptTextFa = promptFa,
                        promptTextEn = promptEn
                    )
                } else {
                    state = DialogueState.CollectingSlots(
                        targetIntent = decision.partial.intent,
                        collectedSlots = collected,
                        missingSlot = decision.slot,
                        partialNlu = decision.partial
                    )
                    val flockName = collected[SlotId.FLOCK_NAME]?.typedValue as? String
                    val (promptFa, promptEn) = when (decision.slot) {
                        SlotId.COUNT -> promptForCount(decision.partial.intent, flockName)
                        SlotId.AMOUNT -> "مبلغ تراکنش چقدر است؟" to "What is the transaction amount?"
                        else -> "لطفاً اطلاعات را تکمیل کنید." to "Please provide the missing details."
                    }
                    DialogueResponse.PromptSlot(decision.slot, promptFa, promptEn)
                }
            }

            is RouterDecision.Clarify -> {
                state = DialogueState.AwaitingClarification(decision.options, decision.partial)
                DialogueResponse.ClarifyOptions(decision.options, decision.partial.slots)
            }

            is RouterDecision.AskOnline -> {
                state = DialogueState.Idle
                DialogueResponse.AnswerOnline(decision.text)
            }

            is RouterDecision.Unsupported -> {
                state = DialogueState.Idle
                DialogueResponse.Message("متوجه این دستور نشدم.", "I did not understand that command.")
            }
        }
    }

    private fun evaluatePostSlotFilling(
        collectingState: DialogueState.CollectingSlots,
        isVoice: Boolean,
        availableFlocks: List<Pair<Long, String>>
    ): DialogueResponse {
        val intent = collectingState.targetIntent
        val slots = collectingState.collectedSlots

        val requiresFlockSelection = intent in AssistantRouter.FLOCK_SELECTION_REQUIRED_INTENTS && availableFlocks.size > 1
        val hasSingleFlock = slots.containsKey(SlotId.FLOCK_ID)
        val hasMultiAllocations = (slots[SlotId.FLOCK_ALLOCATIONS]?.typedValue as? List<*>)?.isNotEmpty() == true

        if (requiresFlockSelection && !hasSingleFlock && !hasMultiAllocations) {
            val preselected = (slots[SlotId.SELECTED_FLOCK_IDS]?.typedValue as? List<*>)
                ?.mapNotNull { (it as? Number)?.toLong() }
                ?.toSet()
                ?: emptySet()
            state = DialogueState.SelectingFlocks(
                targetIntent = intent,
                collectedSlots = slots,
                availableFlocks = availableFlocks,
                preselectedFlockIds = preselected,
                partialNlu = collectingState.partialNlu
            )
            val (promptFa, promptEn) = promptForFlocks(intent, hasPreselected = preselected.isNotEmpty())
            return DialogueResponse.SelectFlocks(
                intent = intent,
                slots = slots,
                preselectedFlockIds = preselected,
                promptTextFa = promptFa,
                promptTextEn = promptEn
            )
        }

        return buildPostSlotExecutionOrConfirm(intent, slots, isVoice)
    }

    private fun buildPostSlotExecutionOrConfirm(
        intent: IntentId,
        slots: Map<SlotId, SlotValue>,
        isVoice: Boolean
    ): DialogueResponse {
        val saleAmount = (slots[SlotId.SALE_AMOUNT_TOMANS]?.typedValue as? Number)?.toDouble() ?: 0.0
        val shouldConfirm = when (intent) {
            IntentId.ADD_TRANSACTION -> true
            IntentId.TRANSFER_BIRDS -> true
            IntentId.CONSUME_WAREHOUSE -> true
            IntentId.SELL_BIRDS -> isVoice || saleAmount > 0.0
            IntentId.SLAUGHTER_BIRDS -> isVoice
            IntentId.ADD_EGGS -> isVoice || alwaysConfirmEggs
            else -> false
        }

        return if (shouldConfirm) {
            state = DialogueState.AwaitingConfirmation(
                intent = intent,
                slots = slots,
                requiresExplicitCard = true,
                isVoice = isVoice
            )
            DialogueResponse.ConfirmAction(intent, slots, isVoice)
        } else {
            state = DialogueState.Idle
            DialogueResponse.ExecuteDirect(intent, slots)
        }
    }

    private fun promptForCount(intent: IntentId, flockName: String? = null): Pair<String, String> {
        val prefixFa = if (!flockName.isNullOrBlank()) "برای «$flockName» " else ""
        val suffixEn = if (!flockName.isNullOrBlank()) " for '$flockName'" else ""
        return when (intent) {
            IntentId.ADD_FEED_WATER ->
                "${prefixFa}چه مقدار (کیلوگرم یا لیتر) شارژ کردید؟" to "How much feed or water was charged$suffixEn?"
            IntentId.LOG_MORTALITY ->
                "${prefixFa}چند قطعه تلفات داشتید؟" to "How many dead birds to log$suffixEn?"
            IntentId.SELL_BIRDS ->
                "${prefixFa}چند قطعه پرنده فروخته‌اید؟" to "How many birds were sold$suffixEn?"
            IntentId.SLAUGHTER_BIRDS ->
                "${prefixFa}چند قطعه پرنده کشتار شده است؟" to "How many birds were slaughtered$suffixEn?"
            IntentId.TRANSFER_BIRDS ->
                "چند پرنده را می‌خواهید جابجا کنید؟" to "How many birds to transfer?"
            IntentId.CONSUME_WAREHOUSE ->
                "چه مقدار از انبار مصرف شده است؟" to "How much was consumed from warehouse?"
            else ->
                "${prefixFa}چند تا تخم‌مرغ جمع کردید؟" to "How many eggs did you collect$suffixEn?"
        }
    }

    private fun promptForFlocks(intent: IntentId, hasPreselected: Boolean): Pair<String, String> {
        if (hasPreselected) {
            return "لطفاً مقدار دقیق هر یک از گله‌های انتخاب‌شده را وارد و تایید کنید:" to
                "Please enter the exact quantity for each selected flock and confirm:"
        }
        return when (intent) {
            IntentId.ADD_FEED_WATER ->
                "شما چند گله دارید. لطفاً گله یا گله‌های مورد نظر را برای ثبت شارژ دان/آب انتخاب و مقدار هر گله را مشخص کنید:" to
                    "You have multiple flocks. Please select target flock(s) and specify exact feed/water amount:"
            IntentId.LOG_MORTALITY ->
                "شما چند گله دارید. لطفاً گله یا گله‌های مورد نظر را برای ثبت تلفات انتخاب و تعداد هر گله را مشخص کنید:" to
                    "You have multiple flocks. Please select target flock(s) and specify exact mortality count:"
            IntentId.SELL_BIRDS ->
                "شما چند گله دارید. لطفاً گله یا گله‌های مورد نظر را برای ثبت فروش پرنده انتخاب و تعداد هر گله را مشخص کنید:" to
                    "You have multiple flocks. Please select target flock(s) and specify exact bird count sold:"
            IntentId.SLAUGHTER_BIRDS ->
                "شما چند گله دارید. لطفاً گله یا گله‌های مورد نظر را برای ثبت کشتار پرنده انتخاب و تعداد هر گله را مشخص کنید:" to
                    "You have multiple flocks. Please select target flock(s) and specify exact bird count slaughtered:"
            else ->
                "شما چند گله دارید. لطفاً گله یا گله‌های مورد نظر را برای ثبت تخم‌گذاری انتخاب و تعداد هر گله را مشخص کنید:" to
                    "You have multiple flocks. Please select target flock(s) and specify exact egg count:"
        }
    }

    private fun extractCountFromFollowUp(
        rawText: String,
        matchedFlocks: List<ResolvedFlock>
    ): Triple<String, Any, Double>? {
        var cleaned = rawText
        val spans = matchedFlocks
            .filter { it.startIndex >= 0 && it.endIndex > it.startIndex && it.endIndex <= rawText.length }
            .map { it.startIndex to it.endIndex }
            .distinct()
            .sortedByDescending { it.first }
        for ((start, end) in spans) {
            cleaned = cleaned.replaceRange(start, end, " ")
        }
        val normCleaned = TextNormalizer.normalize(cleaned).normalized
        if (normCleaned.isBlank()) return null

        val unit = PackagingUnitExtractor.extractUnits(normCleaned).firstOrNull()
        if (unit != null) {
            val numVal: Any = if (unit.calculatedTotal % 1.0 == 0.0) unit.calculatedTotal.toInt() else unit.calculatedTotal
            return Triple(unit.originalSpan, numVal, unit.calculatedTotal)
        }

        val num = NumberParser.extractNumbers(normCleaned).firstOrNull() ?: return null
        val numVal: Any = if (num.isInteger) num.intValue else num.value
        return Triple(num.originalSpan, numVal, num.value)
    }

    fun beginFlockSelection(
        intent: IntentId,
        slots: Map<SlotId, SlotValue>,
        availableFlocks: List<Pair<Long, String>>
    ): DialogueResponse.SelectFlocks {
        val mutableSlots = slots.toMutableMap()
        val preselected = (mutableSlots[SlotId.SELECTED_FLOCK_IDS]?.typedValue as? List<*>)
            ?.mapNotNull { (it as? Number)?.toLong() }
            ?.toSet()
            ?: emptySet()
        state = DialogueState.SelectingFlocks(
            targetIntent = intent,
            collectedSlots = mutableSlots,
            availableFlocks = availableFlocks,
            preselectedFlockIds = preselected,
            partialNlu = NluResult(
                intent = intent,
                confidence = 1.0f,
                margin = 1.0f,
                slots = mutableSlots,
                alternatives = emptyList(),
                lang = com.example.poultryapp.assistant.core.normalize.Lang.FA,
                templateText = "",
                trace = emptyList()
            )
        )
        val (promptFa, promptEn) = promptForFlocks(intent, hasPreselected = preselected.isNotEmpty())
        return DialogueResponse.SelectFlocks(
            intent = intent,
            slots = mutableSlots,
            preselectedFlockIds = preselected,
            promptTextFa = promptFa,
            promptTextEn = promptEn
        )
    }

    fun reset() {
        state = DialogueState.Idle
    }
}
