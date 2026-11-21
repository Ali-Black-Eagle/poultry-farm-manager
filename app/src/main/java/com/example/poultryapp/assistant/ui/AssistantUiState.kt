package com.example.poultryapp.assistant.ui

import com.example.poultryapp.assistant.actions.ActionCardPreview
import com.example.poultryapp.assistant.core.nlu.IntentId

data class ChatUiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER", "ASSISTANT", "AI"
    val text: String,
    val isAi: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class FlockPickerOption(
    val flockId: Long,
    val flockName: String,
    val birdCount: Int,
    val speciesTitleFa: String = "",
    val isWithdrawalActive: Boolean = false,
    val withdrawalMedName: String? = null
)

data class PendingFlockSelectionState(
    val selectionId: String = java.util.UUID.randomUUID().toString(),
    val intent: IntentId,
    val titleFa: String,
    val unitLabelFa: String,
    val allowDecimal: Boolean,
    val initialTotalCount: Double? = null,
    val preselectedFlockIds: Set<Long> = emptySet(),
    val flocks: List<FlockPickerOption>,
    val extraSummaryLine: String? = null
)

data class AssistantUiState(
    val messages: List<ChatUiMessage> = emptyList(),
    val inputText: String = "",
    val isListening: Boolean = false,
    val rmsLevel: Float = 0.0f,
    val pendingConfirmationCard: ActionCardPreview? = null,
    val pendingFlockSelection: PendingFlockSelectionState? = null,
    val clarifyOptions: List<IntentId> = emptyList(),
    val isOfflineMode: Boolean = true,
    val undoSnackbarMessage: String? = null,
    val undoToken: String? = null,
    val isExecuting: Boolean = false,
    val voiceErrorMessage: String? = null,
    val showSystemVoiceFallback: Boolean = false
)

