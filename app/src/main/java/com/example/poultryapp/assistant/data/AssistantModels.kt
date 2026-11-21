package com.example.poultryapp.assistant.data

data class AssistantMessageEntity(
    val id: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val role: String, // "USER" or "ASSISTANT" or "AI"
    val text: String,
    val lang: String = "fa",
    val intent: String? = null,
    val source: String = "LOCAL" // "LOCAL" or "ONLINE"
)

data class AssistantActionLogEntity(
    val id: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val intent: String,
    val paramsJson: String,
    val resultToken: String?,
    val status: String = "SUCCESS",
    val undone: Boolean = false
)

data class AssistantLearnedPhraseEntity(
    val normalizedText: String,
    val intent: String,
    val slotsTemplateJson: String = "{}",
    val useCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

data class AssistantAliasEntity(
    val id: Long = 0L,
    val kind: String, // "FLOCK" or "CATEGORY"
    val aliasNormalized: String,
    val targetId: Long
)

