package com.example.poultryapp.assistant.core.dialogue

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.NluResult
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue

sealed interface DialogueState {
    object Idle : DialogueState

    data class CollectingSlots(
        val targetIntent: IntentId,
        val collectedSlots: MutableMap<SlotId, SlotValue>,
        val missingSlot: SlotId,
        val partialNlu: NluResult
    ) : DialogueState

    data class SelectingFlocks(
        val targetIntent: IntentId,
        val collectedSlots: MutableMap<SlotId, SlotValue>,
        val availableFlocks: List<Pair<Long, String>>,
        val preselectedFlockIds: Set<Long> = emptySet(),
        val partialNlu: NluResult
    ) : DialogueState

    data class AwaitingConfirmation(
        val intent: IntentId,
        val slots: Map<SlotId, SlotValue>,
        val requiresExplicitCard: Boolean,
        val isVoice: Boolean
    ) : DialogueState

    data class AwaitingClarification(
        val options: List<IntentId>,
        val partialNlu: NluResult
    ) : DialogueState

    object Executing : DialogueState
}

