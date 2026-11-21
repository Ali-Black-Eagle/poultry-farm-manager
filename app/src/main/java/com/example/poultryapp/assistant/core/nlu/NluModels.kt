package com.example.poultryapp.assistant.core.nlu

import com.example.poultryapp.assistant.core.normalize.Lang

enum class IntentId {
    ADD_EGGS,
    ADD_TRANSACTION,
    QUERY_EGGS,
    QUERY_FINANCE,
    ADD_FEED_WATER,
    QUERY_FEED_WATER,
    LOG_MORTALITY,
    QUERY_MORTALITY,
    SELL_BIRDS,
    SLAUGHTER_BIRDS,
    QUERY_FLOCKS,
    TRANSFER_BIRDS,
    QUERY_WAREHOUSE,
    CONSUME_WAREHOUSE,
    DIAGNOSE_DISEASE,
    QUERY_MEDICATION,
    ADD_SCHEDULE,
    QUERY_SCHEDULE,
    QUERY_CONTACTS,
    QUERY_WEATHER,
    UNDO_LAST,
    OPEN_SCREEN,
    HELP,
    GREETING,
    GENERAL_QUESTION,
    UNKNOWN
}

enum class SlotId {
    COUNT,
    DATE,
    FLOCK_ID,
    FLOCK_NAME,
    AMOUNT,
    SALE_AMOUNT_TOMANS,
    LIVE_WEIGHT_KG,
    CARCASS_WEIGHT_KG,
    TRANSACTION_TYPE,
    CURRENCY,
    CATEGORY,
    BROKEN_COUNT,
    NOTE,
    PERIOD,
    METRIC,
    TARGET_SCREEN,
    FEED_WATER_TYPE,
    MORTALITY_REASON,
    TARGET_FLOCK_ID,
    TARGET_FLOCK_NAME,
    WAREHOUSE_ITEM_ID,
    WAREHOUSE_ITEM_NAME,
    MEDICATION_NAME,
    DISEASE_NAME,
    SYMPTOM_TOKENS,
    CONTACT_NAME,
    SCHEDULE_TIME,
    SELECTED_FLOCK_IDS,
    FLOCK_ALLOCATIONS
}

data class FlockAllocation(
    val flockId: Long,
    val flockName: String,
    val quantity: Double
)

data class SlotValue(
    val rawSpan: String,
    val typedValue: Any
)

data class NluResult(
    val intent: IntentId,
    val confidence: Float,
    val margin: Float,
    val slots: Map<SlotId, SlotValue>,
    val alternatives: List<Pair<IntentId, Float>>,
    val lang: Lang,
    val templateText: String,
    val trace: List<String>
)

sealed interface RouterDecision {
    data class Execute(val intent: IntentId, val slots: Map<SlotId, SlotValue>) : RouterDecision
    data class AskSlot(val slot: SlotId, val partial: NluResult) : RouterDecision
    data class Clarify(val options: List<IntentId>, val partial: NluResult) : RouterDecision
    data class AskOnline(val text: String) : RouterDecision
    object Unsupported : RouterDecision
}

data class NluConfig(
    val acceptThreshold: Float = 0.70f,
    val acceptMargin: Float = 0.12f,
    val clarifyThreshold: Float = 0.45f
)

