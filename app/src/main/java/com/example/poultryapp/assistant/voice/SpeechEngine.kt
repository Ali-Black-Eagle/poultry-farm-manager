package com.example.poultryapp.assistant.voice

import com.example.poultryapp.assistant.core.normalize.Lang
import kotlinx.coroutines.flow.Flow

sealed interface SpeechAvailability {
    object Ready : SpeechAvailability
    object NeedsModelDownload : SpeechAvailability
    object Unsupported : SpeechAvailability
}

sealed interface SpeechEvent {
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    data class Level(val rmsDb: Float) : SpeechEvent
    data class Error(
        val errorCode: Int,
        val message: String,
        val isLanguageUnsupported: Boolean = SpeechErrorMapper.isLanguageUnsupported(errorCode)
    ) : SpeechEvent
}

interface SpeechEngine {
    suspend fun availability(lang: Lang): SpeechAvailability
    fun listen(lang: Lang): Flow<SpeechEvent>
    fun stop()
}

