package com.example.poultryapp.voice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poultryapp.assistant.actions.ActionCardPreview
import com.example.poultryapp.assistant.core.nlu.FlockAllocation
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.ui.AssistantUiState
import com.example.poultryapp.assistant.ui.AssistantViewModel
import com.example.poultryapp.assistant.ui.ChatUiMessage
import com.example.poultryapp.assistant.ui.PendingFlockSelectionState
import com.example.poultryapp.assistant.ui.theme.AssistantTheme
import com.example.poultryapp.assistant.ui.theme.LocalAssistantColors
import com.example.poultryapp.engine.toPersianDigits
import kotlinx.coroutines.delay

@Composable
fun VoiceAssistantSheet(
    isWorkerMode: Boolean = false,
    onDismiss: () -> Unit,
    onNavigate: ((tab: String, subTab: String?) -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    val viewModel = remember {
        AssistantViewModel(context).apply {
            this.isWorkerMode = isWorkerMode
            onNavigateToScreen = { tab, subTab ->
                onNavigate?.invoke(tab, subTab)
                onDismiss()
            }
        }
    }
    LaunchedEffect(isWorkerMode) {
        viewModel.isWorkerMode = isWorkerMode
    }
    VoiceAssistantSheetContent(viewModel = viewModel, onDismiss = onDismiss)
}

@Composable
fun VoiceAssistantSheet(
    onDismiss: () -> Unit,
    onNavigate: ((tab: String, subTab: String?) -> Unit)? = null
) {
    VoiceAssistantSheet(isWorkerMode = false, onDismiss = onDismiss, onNavigate = onNavigate)
}

@Composable
fun VoiceAssistantSheet(
    viewModel: AssistantViewModel,
    onDismiss: () -> Unit
) {
    VoiceAssistantSheetContent(viewModel = viewModel, onDismiss = onDismiss)
}

@Composable
fun VoiceAssistantSheet(
    flocks: Any? = null,
    contacts: Any? = null,
    eggProductions: Any? = null,
    farmContext: Any? = null,
    userRole: Any? = null,
    onDismiss: () -> Unit,
    onExecuteAction: ((Any) -> Any)? = null
) {
    val context = LocalContext.current.applicationContext
    val viewModel = remember { AssistantViewModel(context) }
    VoiceAssistantSheetContent(viewModel = viewModel, onDismiss = onDismiss)
}

@Composable
private fun VoiceAssistantSheetContent(
    viewModel: AssistantViewModel,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = true) {
        onDismiss()
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            viewModel.onMicPermissionDenied()
        }
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            if (spokenText.isNotEmpty()) {
                viewModel.submitVoiceResult(spokenText)
            }
        }
    }

    val launchSystemSpeechDialog: () -> Unit = {
        viewModel.dismissVoiceError()
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "دستور یا سوال خود را به فارسی بیان کنید")
        }
        try {
            systemSpeechLauncher.launch(speechIntent)
        } catch (_: Exception) {
            viewModel.onSystemVoiceUnavailable()
        }
    }

    // Auto-scroll when messages or interactive cards arrive
    LaunchedEffect(uiState.messages.size, uiState.pendingConfirmationCard, uiState.pendingFlockSelection) {
        val extraItems = (if (uiState.pendingConfirmationCard != null) 1 else 0) +
            (if (uiState.pendingFlockSelection != null) 1 else 0)
        val totalItems = uiState.messages.size + extraItems
        if (totalItems > 0) {
            listState.animateScrollToItem((totalItems - 1).coerceAtLeast(0))
        }
    }

    // Auto-dismiss undo snackbar after 8 seconds
    LaunchedEffect(uiState.undoSnackbarMessage) {
        if (uiState.undoSnackbarMessage != null) {
            delay(8000)
            viewModel.dismissUndoSnackbar()
        }
    }

    AssistantTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .align(Alignment.BottomCenter)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Top Drag Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    // Header
                    AssistantTopHeader(
                        isOffline = uiState.isOfflineMode,
                        onClearChat = { viewModel.clearChatHistory() },
                        onClose = onDismiss
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Message List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            ConversationMessageBubble(msg = msg)
                        }

                        // Pending Interactive Multi-Flock Selection Card
                        uiState.pendingFlockSelection?.let { flockState ->
                            item(key = "flock_selection_${flockState.selectionId}") {
                                FlockSelectionCardView(
                                    state = flockState,
                                    onSubmit = { allocations -> viewModel.submitFlockSelection(allocations) },
                                    onCancel = { viewModel.cancelPendingFlockSelection() }
                                )
                            }
                        }

                        // Pending Confirmation Card (Section 13.7)
                        uiState.pendingConfirmationCard?.let { card ->
                            item {
                                ConfirmationActionCardView(
                                    card = card,
                                    onConfirm = { viewModel.confirmPendingAction() },
                                    onCancel = { viewModel.cancelPendingAction() }
                                )
                            }
                        }
                    }

                    // Clarify Chips (When intent is ambiguous)
                    if (uiState.clarifyOptions.isNotEmpty()) {
                        ClarifyChipsRow(
                            options = uiState.clarifyOptions,
                            onSelect = { viewModel.selectClarifyOption(it) }
                        )
                    }

                    // Undo Snackbar (8 seconds)
                    AnimatedVisibility(
                        visible = uiState.undoSnackbarMessage != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.inverseSurface,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "عملیات با موفقیت انجام شد",
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    fontSize = 13.sp
                                )
                                TextButton(
                                    onClick = { viewModel.undoLastAction() }
                                ) {
                                    Text(
                                        text = "بازگردانی (Undo)",
                                        color = MaterialTheme.colorScheme.inversePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Voice Recognition Transient Error Banner + System Fallback
                    AnimatedVisibility(
                        visible = uiState.voiceErrorMessage != null,
                        enter = slideInVertically { it / 2 } + fadeIn(),
                        exit = slideOutVertically { it / 2 } + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MicOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = uiState.voiceErrorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissVoiceError() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "بستن",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (uiState.showSystemVoiceFallback) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        FilledTonalButton(
                                            onClick = launchSystemSpeechDialog,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RecordVoiceOver,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "باز کردن میکروفون سیستم",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Dock (Input Field & Voice Mic)
                    AssistantBottomDock(
                        inputText = uiState.inputText,
                        isListening = uiState.isListening,
                        rmsLevel = uiState.rmsLevel,
                        onTextChanged = { viewModel.onInputTextChanged(it) },
                        onSend = {
                            keyboardController?.hide()
                            viewModel.sendTextMessage()
                        },
                        onMicClick = {
                            keyboardController?.hide()
                            if (uiState.isListening) {
                                viewModel.stopListening()
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.startListening()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AssistantTopHeader(
    isOffline: Boolean,
    onClearChat: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Close Button
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Center: Title + Offline Pill
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "دستیار هوشمند فارم",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isOffline) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "حالت آفلاین: فرامین فارم فعال است",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Right: Clear Chat Button
        IconButton(onClick = onClearChat) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Clear Chat",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ConversationMessageBubble(msg: ChatUiMessage) {
    val isUser = msg.sender == "USER"
    val colors = LocalAssistantColors.current

    val bubbleBg = when {
        isUser -> colors.userBubble
        msg.isAi -> colors.aiBubble
        else -> colors.botBubble
    }

    val bubbleOnColor = when {
        isUser -> colors.onUserBubble
        msg.isAi -> colors.onAiBubble
        else -> colors.onBotBubble
    }

    val shape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = shape,
            color = bubbleBg,
            tonalElevation = if (isUser) 2.dp else 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (msg.isAi) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = "هوش مصنوعی آنلاین",
                            style = MaterialTheme.typography.labelSmall,
                            color = bubbleOnColor.copy(alpha = 0.7f)
                        )
                    }
                }

                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDirection = TextDirection.Content
                    ),
                    color = bubbleOnColor,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun ConfirmationActionCardView(
    card: ActionCardPreview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = card.titleFa,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Fields list
            for ((label, value) in card.fields) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Confirm & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("انصراف")
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onConfirm()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تایید و ثبت")
                }
            }
        }
    }
}

private fun normalizeNumericInput(raw: String, allowDecimal: Boolean): String {
    val sb = StringBuilder(raw.length)
    var hasDot = false
    for (ch in raw) {
        val mapped = when (ch) {
            in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
            in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
            '٫', '/' -> if (allowDecimal) '.' else null
            '.' -> if (allowDecimal) '.' else null
            else -> if (ch in '0'..'9') ch else null
        } ?: continue
        if (mapped == '.') {
            if (!hasDot && sb.isNotEmpty()) {
                sb.append('.')
                hasDot = true
            } else if (!hasDot && sb.isEmpty()) {
                sb.append("0.")
                hasDot = true
            }
        } else {
            sb.append(mapped)
        }
    }
    return sb.toString()
}

private fun formatInitialQuantity(value: Double, allowDecimal: Boolean): String {
    return if (!allowDecimal || value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
    }
}

private fun formatDisplayQuantity(value: Double, allowDecimal: Boolean): String {
    val formatted = if (!allowDecimal || value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
    }
    return toPersianDigits(formatted)
}

private fun parsePositiveQuantity(text: String, allowDecimal: Boolean): Double? {
    if (text.isBlank()) return null
    val norm = normalizeNumericInput(text, allowDecimal)
    if (norm.isBlank() || norm == ".") return null
    return if (allowDecimal) {
        norm.toDoubleOrNull()?.takeIf { it > 0.0 }
    } else {
        norm.toIntOrNull()?.takeIf { it > 0 }?.toDouble()
    }
}

@Composable
fun FlockSelectionCardView(
    state: PendingFlockSelectionState,
    onSubmit: (List<FlockAllocation>) -> Unit,
    onCancel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedIds by remember(state.selectionId) { mutableStateOf(state.preselectedFlockIds) }
    var quantityTexts by remember(state.selectionId) {
        mutableStateOf<Map<Long, String>>(
            if (state.preselectedFlockIds.size == 1 && state.initialTotalCount != null && state.initialTotalCount > 0.0) {
                val onlyId = state.preselectedFlockIds.first()
                mapOf(onlyId to formatInitialQuantity(state.initialTotalCount, state.allowDecimal))
            } else {
                emptyMap()
            }
        )
    }
    var autoFilledSingleFlockId by remember(state.selectionId) {
        mutableStateOf<Long?>(
            if (state.preselectedFlockIds.size == 1 && state.initialTotalCount != null && state.initialTotalCount > 0.0) {
                state.preselectedFlockIds.first()
            } else {
                null
            }
        )
    }
    var lastPreselectedIds by remember(state.selectionId) { mutableStateOf(state.preselectedFlockIds) }

    LaunchedEffect(state.selectionId, state.preselectedFlockIds) {
        if (state.preselectedFlockIds != lastPreselectedIds) {
            lastPreselectedIds = state.preselectedFlockIds
            val nextQty = quantityTexts.toMutableMap()
            val prevAutoId = autoFilledSingleFlockId
            if (prevAutoId != null && state.preselectedFlockIds.size != 1) {
                nextQty.remove(prevAutoId)
                autoFilledSingleFlockId = null
            }
            selectedIds = state.preselectedFlockIds
            if (selectedIds.size == 1 && state.initialTotalCount != null && state.initialTotalCount > 0.0) {
                val onlyId = selectedIds.first()
                if (nextQty[onlyId].isNullOrBlank()) {
                    nextQty[onlyId] = formatInitialQuantity(state.initialTotalCount, state.allowDecimal)
                    autoFilledSingleFlockId = onlyId
                }
            }
            quantityTexts = nextQty
        }
    }

    LaunchedEffect(state.selectionId, state.initialTotalCount) {
        val count = state.initialTotalCount ?: return@LaunchedEffect
        if (count > 0.0 && selectedIds.size == 1) {
            val onlyId = selectedIds.first()
            if (quantityTexts[onlyId].isNullOrBlank() || autoFilledSingleFlockId == onlyId) {
                quantityTexts = quantityTexts + (onlyId to formatInitialQuantity(count, state.allowDecimal))
                autoFilledSingleFlockId = onlyId
            }
        }
    }

    val toggleFlock: (Long) -> Unit = { flockId ->
        val nextSelected = if (flockId in selectedIds) {
            selectedIds - flockId
        } else {
            selectedIds + flockId
        }
        val nextQty = quantityTexts.toMutableMap()

        if (flockId !in nextSelected) {
            nextQty.remove(flockId)
            if (autoFilledSingleFlockId == flockId) {
                autoFilledSingleFlockId = null
            }
        }

        if (nextSelected.size == 1) {
            val onlyId = nextSelected.first()
            if (nextQty[onlyId].isNullOrBlank() && state.initialTotalCount != null && state.initialTotalCount > 0.0) {
                nextQty[onlyId] = formatInitialQuantity(state.initialTotalCount, state.allowDecimal)
                autoFilledSingleFlockId = onlyId
            }
        } else {
            val autoId = autoFilledSingleFlockId
            if (autoId != null) {
                nextQty.remove(autoId)
                autoFilledSingleFlockId = null
            }
        }

        selectedIds = nextSelected
        quantityTexts = nextQty
    }

    val selectedFlocks = state.flocks.filter { it.flockId in selectedIds }
    val parsedPerFlock = selectedFlocks.associate { flock ->
        flock.flockId to parsePositiveQuantity(quantityTexts[flock.flockId].orEmpty(), state.allowDecimal)
    }
    val hasBirdCountOverLimit = state.intent in setOf(
        IntentId.LOG_MORTALITY,
        IntentId.SELL_BIRDS,
        IntentId.SLAUGHTER_BIRDS
    ) && selectedFlocks.any { flock ->
        val qty = parsedPerFlock[flock.flockId]
        qty != null && qty.toInt() > flock.birdCount
    }
    val canSubmit = selectedFlocks.isNotEmpty() &&
        selectedFlocks.all { parsedPerFlock[it.flockId] != null } &&
        !hasBirdCountOverLimit
    val enteredTotal = selectedFlocks.sumOf { parsedPerFlock[it.flockId] ?: 0.0 }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Title + Selected Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.titleFa,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedFlocks.isNotEmpty()) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = "${toPersianDigits(selectedFlocks.size)} از ${toPersianDigits(state.flocks.size)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedFlocks.isNotEmpty()) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Context Metadata & Reference Total
            if (!state.extraSummaryLine.isNullOrBlank() || state.initialTotalCount != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.extraSummaryLine?.let { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.initialTotalCount?.let { spokenTotal ->
                        Text(
                            text = "مقدار گفته‌شده: ${formatDisplayQuantity(spokenTotal, state.allowDecimal)} ${state.unitLabelFa}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Helper Chips: Select All / Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allSelected = selectedIds.size == state.flocks.size && state.flocks.isNotEmpty()
                FilterChip(
                    selected = allSelected,
                    onClick = {
                        val allIds = state.flocks.map { it.flockId }.toSet()
                        val nextQty = quantityTexts.toMutableMap()
                        val autoId = autoFilledSingleFlockId
                        if (allIds.size > 1 && autoId != null) {
                            nextQty.remove(autoId)
                            autoFilledSingleFlockId = null
                        } else if (allIds.size == 1 && state.initialTotalCount != null && state.initialTotalCount > 0.0) {
                            val onlyId = allIds.first()
                            if (nextQty[onlyId].isNullOrBlank()) {
                                nextQty[onlyId] = formatInitialQuantity(state.initialTotalCount, state.allowDecimal)
                                autoFilledSingleFlockId = onlyId
                            }
                        }
                        selectedIds = allIds
                        quantityTexts = nextQty
                    },
                    label = { Text("انتخاب همه") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                FilterChip(
                    selected = false,
                    enabled = selectedIds.isNotEmpty(),
                    onClick = {
                        selectedIds = emptySet()
                        quantityTexts = emptyMap<Long, String>()
                        autoFilledSingleFlockId = null
                    },
                    label = { Text("پاک‌کردن") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Text(
                text = when {
                    selectedFlocks.isEmpty() ->
                        "هیچ گله‌ای به صورت خودکار انتخاب نشده است. گله یا گله‌های مورد نظر را انتخاب کنید:"
                    selectedFlocks.size == 1 ->
                        "مقدار گله انتخاب‌شده را بررسی یا ویرایش کنید:"
                    else ->
                        "مقدار دقیق هر یک از گله‌های انتخاب‌شده را جداگانه وارد کنید:"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Flock Checklist + Per-Flock Numeric Inputs
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.flocks.forEach { flock ->
                    val isSelected = flock.flockId in selectedIds
                    val rawQtyText = quantityTexts[flock.flockId].orEmpty()
                    val parsedQty = parsedPerFlock[flock.flockId]
                    val exceedsLiveBirds = state.intent in setOf(
                        IntentId.LOG_MORTALITY,
                        IntentId.SELL_BIRDS,
                        IntentId.SLAUGHTER_BIRDS
                    ) &&
                        isSelected &&
                        parsedQty != null &&
                        parsedQty.toInt() > flock.birdCount

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = when {
                                exceedsLiveBirds -> MaterialTheme.colorScheme.error
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { toggleFlock(flock.flockId) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = flock.flockName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val metaParts = buildList {
                                        if (flock.speciesTitleFa.isNotBlank()) add(flock.speciesTitleFa)
                                        add("${toPersianDigits(flock.birdCount)} قطعه")
                                    }
                                    Text(
                                        text = metaParts.joinToString("  •  "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (flock.isWithdrawalActive &&
                                        (state.intent == IntentId.SELL_BIRDS || state.intent == IntentId.SLAUGHTER_BIRDS)
                                    ) {
                                        val medText = flock.withdrawalMedName?.takeIf { it.isNotBlank() } ?: "دارو"
                                        Text(
                                            text = "⚠️ دوره پرهیز دارویی فعال ($medText)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = isSelected) {
                                OutlinedTextField(
                                    value = rawQtyText,
                                    onValueChange = { input ->
                                        val cleaned = normalizeNumericInput(input, state.allowDecimal)
                                        if (autoFilledSingleFlockId == flock.flockId) {
                                            autoFilledSingleFlockId = null
                                        }
                                        quantityTexts = quantityTexts + (flock.flockId to cleaned)
                                    },
                                    singleLine = true,
                                    isError = exceedsLiveBirds,
                                    label = { Text("مقدار برای ${flock.flockName}") },
                                    placeholder = { Text(if (state.allowDecimal) "مثلاً ۲۵.۵" else "مثلاً ۱۲۰") },
                                    suffix = { Text(state.unitLabelFa) },
                                    supportingText = if (exceedsLiveBirds) {
                                        {
                                            Text(
                                                "از موجودی زنده گله (${toPersianDigits(flock.birdCount)} قطعه) بیشتر است!",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = if (state.allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { keyboardController?.hide() }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Live Total Footer
            if (selectedFlocks.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مجموع واردشده:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatDisplayQuantity(enteredTotal, state.allowDecimal)} ${state.unitLabelFa}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Confirm & Cancel Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        onCancel()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("انصراف")
                }
                Button(
                    enabled = canSubmit,
                    onClick = {
                        keyboardController?.hide()
                        val allocations = selectedFlocks.mapNotNull { flock ->
                            val qty = parsedPerFlock[flock.flockId] ?: return@mapNotNull null
                            FlockAllocation(
                                flockId = flock.flockId,
                                flockName = flock.flockName,
                                quantity = qty
                            )
                        }
                        if (allocations.isNotEmpty()) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSubmit(allocations)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تایید و ثبت")
                }
            }
        }
    }
}

@Composable
fun ClarifyChipsRow(
    options: List<IntentId>,
    onSelect: (IntentId) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { intent ->
            val label = when (intent) {
                IntentId.ADD_EGGS -> "ثبت تخم‌مرغ"
                IntentId.ADD_TRANSACTION -> "ثبت هزینه/درآمد"
                IntentId.QUERY_EGGS -> "آمار تخم‌مرغ"
                IntentId.QUERY_FINANCE -> "آمار مالی"
                IntentId.ADD_FEED_WATER -> "ثبت شارژ دان/آب"
                IntentId.QUERY_FEED_WATER -> "وضعیت دان و آب"
                IntentId.LOG_MORTALITY -> "ثبت تلفات"
                IntentId.SELL_BIRDS -> "فروش پرنده"
                IntentId.SLAUGHTER_BIRDS -> "کشتار پرنده"
                IntentId.QUERY_MORTALITY -> "آمار تلفات"
                IntentId.QUERY_FLOCKS -> "وضعیت گله‌ها"
                IntentId.TRANSFER_BIRDS -> "انتقال پرنده"
                IntentId.QUERY_WAREHOUSE -> "موجودی انبار"
                IntentId.CONSUME_WAREHOUSE -> "برداشت از انبار"
                IntentId.DIAGNOSE_DISEASE -> "تشخیص بیماری"
                IntentId.QUERY_MEDICATION -> "دارو و دوز"
                IntentId.ADD_SCHEDULE -> "ثبت یادآور"
                IntentId.QUERY_SCHEDULE -> "برنامه امروز"
                IntentId.QUERY_CONTACTS -> "دفترچه تماس"
                IntentId.QUERY_WEATHER -> "دما و تهویه"
                IntentId.UNDO_LAST -> "بازگردانی آخرین کار"
                IntentId.OPEN_SCREEN -> "باز کردن صفحه"
                IntentId.HELP -> "راهنما"
                IntentId.GREETING -> "سلام"
                IntentId.GENERAL_QUESTION -> "سوال از هوش مصنوعی"
                else -> intent.name
            }
            SuggestionChip(
                onClick = { onSelect(intent) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun AssistantBottomDock(
    inputText: String,
    isListening: Boolean,
    rmsLevel: Float,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    val micScale by animateFloatAsState(
        targetValue = if (isListening) 1.15f + (rmsLevel.coerceIn(0f, 10f) / 50f) else 1.0f,
        label = "micScale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputText.isEmpty() && !isListening) {
                    Text(
                        text = "دستور یا سوال خود را بنویسید...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isListening && inputText.isEmpty()) {
                    Text(
                        text = "در حال شنیدن...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                BasicTextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textDirection = TextDirection.Content
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Send or Mic Button
            if (inputText.isNotBlank()) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(44.dp)
                        .scale(micScale)
                        .clip(CircleShape)
                        .background(
                            if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer
                        )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "Voice",
                        tint = if (isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
