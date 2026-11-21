package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue

sealed interface ValidationResult {
    object Valid : ValidationResult
    data class Warning(val messageFa: String, val messageEn: String) : ValidationResult
    data class Rejected(val reasonFa: String, val reasonEn: String) : ValidationResult
}

data class ActionCardPreview(
    val titleFa: String,
    val titleEn: String,
    val fields: List<Pair<String, String>>, // Label to value
    val isExpense: Boolean? = null
)

data class ActionResult(
    val success: Boolean,
    val messageFa: String,
    val messageEn: String,
    val createdRecordId: Long? = null,
    val undoToken: String? = null
)

interface AssistantAction<P : Any> {
    val intent: IntentId
    fun requiredSlots(): List<SlotId>
    fun fromSlots(slots: Map<SlotId, SlotValue>): P
    suspend fun validate(params: P): ValidationResult
    fun preview(params: P): ActionCardPreview
    suspend fun execute(params: P): ActionResult
    suspend fun undo(undoToken: String): Boolean
}

class ActionRegistry {
    private val actions = mutableMapOf<IntentId, AssistantAction<*>>()

    fun register(action: AssistantAction<*>) {
        actions[action.intent] = action
    }

    @Suppress("UNCHECKED_CAST")
    fun <P : Any> get(intent: IntentId): AssistantAction<P>? {
        return actions[intent] as? AssistantAction<P>
    }
}

