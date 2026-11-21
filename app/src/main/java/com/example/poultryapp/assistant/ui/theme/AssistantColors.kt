package com.example.poultryapp.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AssistantColors(
    val userBubble: Color,
    val onUserBubble: Color,
    val botBubble: Color,
    val onBotBubble: Color,
    val aiBubble: Color,
    val onAiBubble: Color,
    val card: Color,
    val onCard: Color,
    val cardBorder: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val inputBar: Color
)

val LocalAssistantColors = staticCompositionLocalOf {
    AssistantColors(
        userBubble = Color(0xFFD3E4FF),
        onUserBubble = Color(0xFF001C38),
        botBubble = Color(0xFFEAECEF),
        onBotBubble = Color(0xFF1A1C1E),
        aiBubble = Color(0xFFE0E2EC),
        onAiBubble = Color(0xFF181C22),
        card = Color(0xFFE3E5E8),
        onCard = Color(0xFF1A1C1E),
        cardBorder = Color(0xFFC4C7C5),
        success = Color(0xFFC4EED0),
        onSuccess = Color(0xFF072711),
        warning = Color(0xFFFFE082),
        onWarning = Color(0xFF251A00),
        error = Color(0xFFFFDAD6),
        onError = Color(0xFF410002),
        inputBar = Color(0xFFF3F4F6)
    )
}

@Composable
fun AssistantTheme(content: @Composable () -> Unit) {
    val s = MaterialTheme.colorScheme
    val colors = remember(s) {
        AssistantColors(
            userBubble = s.primaryContainer,
            onUserBubble = s.onPrimaryContainer,
            botBubble = s.surfaceContainerHigh,
            onBotBubble = s.onSurface,
            aiBubble = s.secondaryContainer,
            onAiBubble = s.onSecondaryContainer,
            card = s.surfaceContainerHighest,
            onCard = s.onSurface,
            cardBorder = s.outlineVariant,
            success = s.tertiaryContainer,
            onSuccess = s.onTertiaryContainer,
            warning = s.secondaryContainer,
            onWarning = s.onSecondaryContainer,
            error = s.errorContainer,
            onError = s.onErrorContainer,
            inputBar = s.surfaceContainer
        )
    }

    CompositionLocalProvider(
        LocalAssistantColors provides colors,
        content = content
    )
}

