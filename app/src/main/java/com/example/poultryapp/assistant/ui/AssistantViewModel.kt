package com.example.poultryapp.assistant.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poultryapp.assistant.actions.*
import com.example.poultryapp.assistant.core.dialogue.*
import com.example.poultryapp.assistant.core.entities.DateParser
import com.example.poultryapp.assistant.core.nlu.FlockAllocation
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.assistant.core.normalize.Lang
import com.example.poultryapp.assistant.core.router.AssistantRouter
import com.example.poultryapp.assistant.data.AssistantDatabaseHelper
import com.example.poultryapp.assistant.data.AssistantMessageEntity
import com.example.poultryapp.assistant.voice.AndroidSpeechEngine
import com.example.poultryapp.assistant.voice.SpeechEngine
import com.example.poultryapp.assistant.voice.SpeechEvent
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.MortalityReason
import com.example.poultryapp.data.repository.*
import com.example.poultryapp.engine.toPersianDigits
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssistantViewModel(
    private val context: Context,
    private val eggRepo: EggProductionRepository = EggProductionRepository(context),
    private val financialRepo: FinancialRepository = FinancialRepository(context),
    private val flockRepo: FlockRepository = FlockRepository(context),
    private val feedWaterRepo: FeedWaterRepository = FeedWaterRepository(context),
    private val warehouseRepo: WarehouseRepository = WarehouseRepository(context),
    private val mortalityRepo: MortalityRepository = MortalityRepository(context),
    private val birdExitRepo: BirdExitRepository = BirdExitRepository(context),
    private val scheduleRepo: ScheduleRepository = ScheduleRepository(context),
    private val contactRepo: FarmContactRepository = FarmContactRepository(context),
    private val settingsRepo: SettingsRepository = SettingsRepository(context),
    private val speechEngine: SpeechEngine = AndroidSpeechEngine(context),
    var isWorkerMode: Boolean = false,
    var onNavigateToScreen: ((tab: String, subTab: String?) -> Unit)? = null
) : ViewModel() {

    private val dbHelper = AssistantDatabaseHelper.getInstance(context)
    private val router = AssistantRouter()
    private val dialogueManager = DialogueManager(router)
    private val actionRegistry = ActionRegistry()

    private var pendingIntent: IntentId? = null
    private var pendingSlots: Map<SlotId, SlotValue>? = null
    private var pendingHasAlreadyWarned: Boolean = false
    private var lastActionRecord: Pair<IntentId, String>? = null
    private var listeningJob: Job? = null

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        // Register Actions across all application domains
        actionRegistry.register(AddEggsAction(eggRepo, flockRepo))
        actionRegistry.register(AddTransactionAction(financialRepo, flockRepo))
        actionRegistry.register(QueryEggsAction(eggRepo))
        actionRegistry.register(QueryFinanceAction(financialRepo))
        actionRegistry.register(UndoAction(actionRegistry) { lastActionRecord })
        actionRegistry.register(OpenScreenAction { tab, subTab -> onNavigateToScreen?.invoke(tab, subTab) })
        actionRegistry.register(AddFeedWaterAction(feedWaterRepo, flockRepo))
        actionRegistry.register(QueryFeedWaterAction(feedWaterRepo, flockRepo))
        actionRegistry.register(LogMortalityAction(mortalityRepo, flockRepo))
        actionRegistry.register(SellBirdsAction(birdExitRepo, flockRepo))
        actionRegistry.register(SlaughterBirdsAction(birdExitRepo, flockRepo))
        actionRegistry.register(QueryMortalityAction(mortalityRepo, birdExitRepo))
        actionRegistry.register(QueryFlocksAction(flockRepo))
        actionRegistry.register(TransferBirdsAction(flockRepo))
        actionRegistry.register(QueryWarehouseAction(warehouseRepo))
        actionRegistry.register(ConsumeWarehouseAction(financialRepo, warehouseRepo, flockRepo))
        actionRegistry.register(DiagnoseDiseaseAction())
        actionRegistry.register(QueryMedicationAction())
        actionRegistry.register(AddScheduleAction(context, scheduleRepo, flockRepo))
        actionRegistry.register(QueryScheduleAction(scheduleRepo))
        actionRegistry.register(QueryContactsAction(contactRepo))
        actionRegistry.register(QueryWeatherAction(settingsRepo))
        actionRegistry.register(GreetingAction())
        actionRegistry.register(HelpAction())

        loadHistoricalMessages()
    }

    private fun loadHistoricalMessages() {
        val welcomeFa = "سلام! من دستیار هوشمند فارم شما هستم. می‌توانید آمارهای تخم‌مرغ، هزینه‌ها یا سوالات پرورشی را از من بپرسید."
        viewModelScope.launch {
            val mapped = withContext(Dispatchers.IO) {
                try {
                    dbHelper.getAllMessages().map {
                        ChatUiMessage(
                            id = "db_${it.id}",
                            sender = it.role,
                            text = it.text,
                            isAi = it.source == "ONLINE",
                            timestamp = it.createdAt
                        )
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }
            if (mapped.isNotEmpty()) {
                _uiState.update { it.copy(messages = mapped) }
            } else {
                _uiState.update {
                    it.copy(
                        messages = listOf(
                            ChatUiMessage(sender = "ASSISTANT", text = welcomeFa)
                        )
                    )
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update {
            it.copy(
                inputText = text,
                voiceErrorMessage = null,
                showSystemVoiceFallback = false
            )
        }
    }

    fun sendTextMessage(text: String? = null) {
        val messageText = text ?: _uiState.value.inputText
        if (messageText.isBlank()) return

        _uiState.update {
            it.copy(
                inputText = "",
                voiceErrorMessage = null,
                showSystemVoiceFallback = false
            )
        }
        appendMessage("USER", messageText, isAi = false)

        processInputInternal(messageText, isVoice = false)
    }

    fun startListening() {
        if (_uiState.value.isListening) {
            stopListening()
            return
        }

        listeningJob?.cancel()
        _uiState.update {
            it.copy(
                isListening = true,
                rmsLevel = 0f,
                voiceErrorMessage = null,
                showSystemVoiceFallback = false
            )
        }
        listeningJob = viewModelScope.launch {
            try {
                speechEngine.listen(Lang.FA).collect { event ->
                    when (event) {
                        is SpeechEvent.Level -> {
                            _uiState.update { it.copy(rmsLevel = event.rmsDb) }
                        }
                        is SpeechEvent.Partial -> {
                            _uiState.update { it.copy(inputText = event.text) }
                        }
                        is SpeechEvent.Final -> {
                            submitVoiceResult(event.text)
                        }
                        is SpeechEvent.Error -> {
                            _uiState.update {
                                it.copy(
                                    isListening = false,
                                    rmsLevel = 0f,
                                    voiceErrorMessage = event.message,
                                    showSystemVoiceFallback = event.isLanguageUnsupported || event.errorCode == -1
                                )
                            }
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isListening = false, rmsLevel = 0f) }
            }
        }
    }

    fun stopListening() {
        speechEngine.stop()
        listeningJob?.cancel()
        _uiState.update { it.copy(isListening = false, rmsLevel = 0f) }
    }

    fun submitVoiceResult(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        _uiState.update {
            it.copy(
                isListening = false,
                rmsLevel = 0f,
                inputText = "",
                voiceErrorMessage = null,
                showSystemVoiceFallback = false
            )
        }
        appendMessage("USER", cleaned, isAi = false)
        processInputInternal(cleaned, isVoice = true)
    }

    fun dismissVoiceError() {
        _uiState.update { it.copy(voiceErrorMessage = null, showSystemVoiceFallback = false) }
    }

    fun onMicPermissionDenied() {
        _uiState.update {
            it.copy(
                isListening = false,
                rmsLevel = 0f,
                voiceErrorMessage = "دسترسی میکروفون داده نشد. برای استفاده از دستیار صوتی، مجوز میکروفون را تایید کنید.",
                showSystemVoiceFallback = false
            )
        }
    }

    fun onSystemVoiceUnavailable() {
        _uiState.update {
            it.copy(
                isListening = false,
                rmsLevel = 0f,
                voiceErrorMessage = "سرویس تشخیص گفتار فارسی روی دستگاه یافت نشد. لطفاً برنامه Google یا Speech Services را نصب یا به‌روزرسانی کنید، یا پیام خود را تایپ کنید.",
                showSystemVoiceFallback = false
            )
        }
    }

    private fun isRestrictedInWorkerMode(intent: IntentId): Boolean {
        return isWorkerMode && intent in setOf(
            IntentId.ADD_TRANSACTION,
            IntentId.QUERY_FINANCE,
            IntentId.CONSUME_WAREHOUSE,
            IntentId.SELL_BIRDS
        )
    }

    private fun rejectWorkerModeAction() {
        pendingIntent = null
        pendingSlots = null
        pendingHasAlreadyWarned = false
        dialogueManager.reset()
        _uiState.update {
            it.copy(
                pendingConfirmationCard = null,
                pendingFlockSelection = null,
                clarifyOptions = emptyList()
            )
        }
        appendMessage(
            "ASSISTANT",
            "دسترسی به عملیات و اطلاعات مالی در حالت کارگر محدود شده است. لطفاً ابتدا از تنظیمات با رمز مدیریت وارد شوید.",
            isAi = false
        )
    }

    private fun formatCompactDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    private fun processInputInternal(text: String, isVoice: Boolean) {
        viewModelScope.launch {
            val fullFlocks = flockRepo.getAllFlocks()
            val flocks = fullFlocks.map { it.id to it.name }
            val warehouseItems = warehouseRepo.getAllWarehouseItems().map { it.id to it.name }
            val response = dialogueManager.processInput(
                rawText = text,
                isVoice = isVoice,
                availableFlocks = flocks,
                availableWarehouseItems = warehouseItems
            )

            when (response) {
                is DialogueResponse.ConfirmAction -> {
                    if (isRestrictedInWorkerMode(response.intent)) {
                        rejectWorkerModeAction()
                        return@launch
                    }
                    val action = actionRegistry.get<Any>(response.intent)
                    if (action != null) {
                        val params = action.fromSlots(response.slots)
                        val validation = action.validate(params)
                        if (validation is ValidationResult.Rejected) {
                            pendingIntent = null
                            pendingSlots = null
                            pendingHasAlreadyWarned = false
                            dialogueManager.reset()
                            _uiState.update {
                                it.copy(
                                    pendingConfirmationCard = null,
                                    pendingFlockSelection = null,
                                    clarifyOptions = emptyList()
                                )
                            }
                            appendMessage("ASSISTANT", validation.reasonFa, isAi = false)
                            return@launch
                        }
                        pendingIntent = response.intent
                        pendingSlots = response.slots
                        pendingHasAlreadyWarned = validation is ValidationResult.Warning
                        val preview = action.preview(params)
                        _uiState.update {
                            it.copy(
                                pendingConfirmationCard = preview,
                                pendingFlockSelection = null,
                                clarifyOptions = emptyList()
                            )
                        }
                        if (validation is ValidationResult.Warning) {
                            appendMessage(
                                "ASSISTANT",
                                "⚠️ هشدار: ${validation.messageFa}\nدر صورت اطمینان دکمه «تایید و ثبت» را بزنید.",
                                isAi = false
                            )
                        }
                    } else {
                        pendingIntent = response.intent
                        pendingSlots = response.slots
                        pendingHasAlreadyWarned = false
                        _uiState.update {
                            it.copy(
                                pendingFlockSelection = null,
                                clarifyOptions = emptyList()
                            )
                        }
                    }
                }

                is DialogueResponse.SelectFlocks -> {
                    if (isRestrictedInWorkerMode(response.intent)) {
                        rejectWorkerModeAction()
                        return@launch
                    }
                    presentFlockSelection(response, fullFlocks)
                }

                is DialogueResponse.ExecuteDirect -> {
                    val allowPriorWarning = _uiState.value.pendingConfirmationCard != null && pendingHasAlreadyWarned
                    pendingIntent = null
                    pendingSlots = null
                    pendingHasAlreadyWarned = false
                    _uiState.update {
                        it.copy(
                            pendingConfirmationCard = null,
                            pendingFlockSelection = null,
                            clarifyOptions = emptyList()
                        )
                    }
                    executeActionInternal(response.intent, response.slots, skipWarnings = allowPriorWarning)
                }

                is DialogueResponse.PromptSlot -> {
                    pendingIntent = null
                    pendingSlots = null
                    pendingHasAlreadyWarned = false
                    _uiState.update {
                        it.copy(
                            pendingConfirmationCard = null,
                            pendingFlockSelection = null,
                            clarifyOptions = emptyList()
                        )
                    }
                    appendMessage("ASSISTANT", response.promptTextFa, isAi = false)
                }

                is DialogueResponse.ClarifyOptions -> {
                    pendingIntent = null
                    pendingSlots = response.slots
                    pendingHasAlreadyWarned = false
                    _uiState.update {
                        it.copy(
                            pendingConfirmationCard = null,
                            pendingFlockSelection = null,
                            clarifyOptions = response.options
                        )
                    }
                    appendMessage("ASSISTANT", "لطفاً یکی از گزینه‌های زیر را انتخاب کنید:", isAi = false)
                }

                is DialogueResponse.AnswerOnline -> {
                    pendingIntent = null
                    pendingSlots = null
                    pendingHasAlreadyWarned = false
                    _uiState.update {
                        it.copy(
                            pendingConfirmationCard = null,
                            pendingFlockSelection = null,
                            clarifyOptions = emptyList()
                        )
                    }
                    val offlineNotice = "این سوال نیاز به اینترنت دارد (حالت آفلاین: فرامین فارم فعال است)."
                    appendMessage("AI", offlineNotice, isAi = true)
                }

                is DialogueResponse.Message -> {
                    pendingIntent = null
                    pendingSlots = null
                    pendingHasAlreadyWarned = false
                    _uiState.update {
                        it.copy(
                            pendingConfirmationCard = null,
                            pendingFlockSelection = null,
                            clarifyOptions = emptyList()
                        )
                    }
                    appendMessage("ASSISTANT", response.textFa, isAi = false)
                }
            }
        }
    }

    private fun presentFlockSelection(
        response: DialogueResponse.SelectFlocks,
        fullFlocks: List<Flock>
    ) {
        if (fullFlocks.isEmpty()) {
            pendingIntent = null
            pendingSlots = null
            pendingHasAlreadyWarned = false
            dialogueManager.reset()
            _uiState.update {
                it.copy(
                    pendingConfirmationCard = null,
                    pendingFlockSelection = null,
                    clarifyOptions = emptyList()
                )
            }
            appendMessage(
                "ASSISTANT",
                "هنوز هیچ گله‌ای ثبت نشده است. لطفاً ابتدا در بخش گله‌ها یک گله ایجاد کنید.",
                isAi = false
            )
            return
        }

        val currentSelection = _uiState.value.pendingFlockSelection
        val selectionId = if (currentSelection != null && currentSelection.intent == response.intent) {
            currentSelection.selectionId
        } else {
            UUID.randomUUID().toString()
        }

        pendingIntent = response.intent
        pendingSlots = response.slots
        pendingHasAlreadyWarned = false

        val parsedDate = (response.slots[SlotId.DATE]?.typedValue as? LocalDate) ?: LocalDate.now()
        val jalaliDate = toPersianDigits(DateParser.formatToStandardJalali(parsedDate))

        val initialCount = when (response.intent) {
            IntentId.ADD_FEED_WATER ->
                (response.slots[SlotId.AMOUNT]?.typedValue as? Number)?.toDouble()
                    ?: (response.slots[SlotId.COUNT]?.typedValue as? Number)?.toDouble()
            else ->
                (response.slots[SlotId.COUNT]?.typedValue as? Number)?.toDouble()
        }?.takeIf { it > 0.0 }

        val titleFa: String
        val unitLabelFa: String
        val allowDecimal: Boolean
        val extraSummaryLine: String

        when (response.intent) {
            IntentId.ADD_FEED_WATER -> {
                val isWater = response.slots[SlotId.FEED_WATER_TYPE]?.typedValue?.toString() == "WATER"
                titleFa = if (isWater) "انتخاب گله و مقدار شارژ آب" else "انتخاب گله و مقدار شارژ دان"
                unitLabelFa = if (isWater) "لیتر" else "کیلوگرم"
                allowDecimal = true
                extraSummaryLine = "نوع: ${if (isWater) "آب" else "دان"}  •  تاریخ: $jalaliDate"
            }

            IntentId.LOG_MORTALITY -> {
                val reasonStr = response.slots[SlotId.MORTALITY_REASON]?.typedValue?.toString()
                val reasonTitle = MortalityReason.fromString(reasonStr).titleFa
                titleFa = "انتخاب گله و تعداد تلفات"
                unitLabelFa = "قطعه"
                allowDecimal = false
                extraSummaryLine = "علت: $reasonTitle  •  تاریخ: $jalaliDate"
            }

            IntentId.SELL_BIRDS -> {
                val liveWeight = (response.slots[SlotId.LIVE_WEIGHT_KG]?.typedValue as? Number)?.toDouble()
                val revenue = (response.slots[SlotId.SALE_AMOUNT_TOMANS]?.typedValue as? Number)?.toDouble() ?: 0.0
                titleFa = "انتخاب گله و تعداد فروش پرنده"
                unitLabelFa = "قطعه"
                allowDecimal = false
                extraSummaryLine = buildList {
                    if (liveWeight != null && liveWeight > 0.0) {
                        add("وزن زنده: ${toPersianDigits(formatCompactDecimal(liveWeight))} کیلو")
                    }
                    if (revenue > 0.0) {
                        add("مبلغ: ${toPersianDigits(String.format(java.util.Locale.US, "%,.0f", revenue))} تومان")
                    }
                    add("تاریخ: $jalaliDate")
                }.joinToString("  •  ")
            }

            IntentId.SLAUGHTER_BIRDS -> {
                val liveWeight = (response.slots[SlotId.LIVE_WEIGHT_KG]?.typedValue as? Number)?.toDouble()
                val carcassWeight = (response.slots[SlotId.CARCASS_WEIGHT_KG]?.typedValue as? Number)?.toDouble()
                titleFa = "انتخاب گله و تعداد کشتار پرنده"
                unitLabelFa = "قطعه"
                allowDecimal = false
                extraSummaryLine = buildList {
                    if (liveWeight != null && liveWeight > 0.0) {
                        add("وزن زنده: ${toPersianDigits(formatCompactDecimal(liveWeight))} کیلو")
                    }
                    if (carcassWeight != null && carcassWeight > 0.0) {
                        add("وزن لاشه: ${toPersianDigits(formatCompactDecimal(carcassWeight))} کیلو")
                    }
                    add("تاریخ: $jalaliDate")
                }.joinToString("  •  ")
            }

            else -> {
                val brokenCount = (response.slots[SlotId.BROKEN_COUNT]?.typedValue as? Number)?.toInt() ?: 0
                titleFa = "انتخاب گله و تعداد تخم‌مرغ"
                unitLabelFa = "عدد"
                allowDecimal = false
                extraSummaryLine = if (brokenCount > 0) {
                    "تاریخ: $jalaliDate  •  شکسته: ${toPersianDigits(brokenCount)} عدد"
                } else {
                    "تاریخ: $jalaliDate"
                }
            }
        }

        val options = fullFlocks.map { flock ->
            FlockPickerOption(
                flockId = flock.id,
                flockName = flock.name,
                birdCount = flock.effectiveBirdCount,
                speciesTitleFa = flock.species.titleFa,
                isWithdrawalActive = flock.isWithdrawalActive,
                withdrawalMedName = flock.activeMedicationName
            )
        }

        val newSelectionState = PendingFlockSelectionState(
            selectionId = selectionId,
            intent = response.intent,
            titleFa = titleFa,
            unitLabelFa = unitLabelFa,
            allowDecimal = allowDecimal,
            initialTotalCount = initialCount,
            preselectedFlockIds = response.preselectedFlockIds,
            flocks = options,
            extraSummaryLine = extraSummaryLine
        )

        _uiState.update {
            it.copy(
                pendingConfirmationCard = null,
                pendingFlockSelection = newSelectionState,
                clarifyOptions = emptyList()
            )
        }

        if (_uiState.value.messages.lastOrNull()?.text != response.promptTextFa) {
            appendMessage("ASSISTANT", response.promptTextFa, isAi = false)
        }
    }

    fun submitFlockSelection(allocations: List<FlockAllocation>) {
        val cleaned = allocations.filter { it.quantity > 0.0 }
        if (cleaned.isEmpty()) return

        val intent = pendingIntent ?: _uiState.value.pendingFlockSelection?.intent ?: return
        val updatedSlots = (pendingSlots ?: emptyMap()).toMutableMap()
        val summaryNames = cleaned.joinToString("، ") { it.flockName }

        updatedSlots[SlotId.FLOCK_ALLOCATIONS] = SlotValue(summaryNames, cleaned)
        if (cleaned.size == 1) {
            updatedSlots[SlotId.FLOCK_ID] = SlotValue(cleaned[0].flockName, cleaned[0].flockId)
            updatedSlots[SlotId.FLOCK_NAME] = SlotValue(cleaned[0].flockName, cleaned[0].flockName)
            updatedSlots.remove(SlotId.SELECTED_FLOCK_IDS)
        } else {
            updatedSlots.remove(SlotId.FLOCK_ID)
            updatedSlots.remove(SlotId.FLOCK_NAME)
            updatedSlots[SlotId.SELECTED_FLOCK_IDS] = SlotValue(summaryNames, cleaned.map { it.flockId })
        }

        if (intent == IntentId.ADD_FEED_WATER) {
            val total = cleaned.sumOf { it.quantity }
            val countTyped: Any = if (total % 1.0 == 0.0) total.toInt() else total
            updatedSlots[SlotId.COUNT] = SlotValue(total.toString(), countTyped)
            updatedSlots[SlotId.AMOUNT] = SlotValue(total.toString(), total)
        } else {
            val totalInt = cleaned.sumOf { it.quantity.toInt() }
            updatedSlots[SlotId.COUNT] = SlotValue(totalInt.toString(), totalInt)
        }

        pendingIntent = null
        pendingSlots = null
        pendingHasAlreadyWarned = false
        dialogueManager.reset()
        _uiState.update {
            it.copy(
                pendingFlockSelection = null,
                pendingConfirmationCard = null,
                clarifyOptions = emptyList()
            )
        }
        executeActionInternal(intent, updatedSlots)
    }

    fun cancelPendingFlockSelection() {
        pendingIntent = null
        pendingSlots = null
        pendingHasAlreadyWarned = false
        dialogueManager.reset()
        _uiState.update {
            it.copy(
                pendingFlockSelection = null,
                pendingConfirmationCard = null,
                clarifyOptions = emptyList()
            )
        }
        appendMessage("ASSISTANT", "عملیات لغو شد.", isAi = false)
    }

    fun confirmPendingAction() {
        val intent = pendingIntent ?: return
        val slots = pendingSlots ?: return
        val alreadyWarned = pendingHasAlreadyWarned
        pendingIntent = null
        pendingSlots = null
        pendingHasAlreadyWarned = false
        dialogueManager.reset()
        _uiState.update { it.copy(pendingConfirmationCard = null) }
        executeActionInternal(intent, slots, skipWarnings = alreadyWarned)
    }

    fun cancelPendingAction() {
        pendingIntent = null
        pendingSlots = null
        pendingHasAlreadyWarned = false
        dialogueManager.reset()
        _uiState.update { it.copy(pendingConfirmationCard = null) }
        appendMessage("ASSISTANT", "عملیات لغو شد.", isAi = false)
    }

    private fun executeActionInternal(
        intent: IntentId,
        slots: Map<SlotId, SlotValue>,
        skipWarnings: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true) }

            // Enforce Worker Mode Restrictions (BIRD SLAUGHTER is non-financial and allowed; BIRD SALE is financial and restricted)
            if (isRestrictedInWorkerMode(intent)) {
                appendMessage("ASSISTANT", "دسترسی به عملیات و اطلاعات مالی در حالت کارگر محدود شده است. لطفاً ابتدا از تنظیمات با رمز مدیریت وارد شوید.", isAi = false)
                _uiState.update { it.copy(isExecuting = false) }
                return@launch
            }

            val action = actionRegistry.get<Any>(intent)
            if (action != null) {
                val params = action.fromSlots(slots)
                val validation = action.validate(params)
                if (validation is ValidationResult.Rejected) {
                    appendMessage("ASSISTANT", validation.reasonFa, isAi = false)
                    _uiState.update { it.copy(isExecuting = false) }
                    return@launch
                }
                if (validation is ValidationResult.Warning && !skipWarnings) {
                    pendingIntent = intent
                    pendingSlots = slots
                    pendingHasAlreadyWarned = true
                    val preview = action.preview(params)
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            pendingConfirmationCard = preview
                        )
                    }
                    appendMessage("ASSISTANT", "⚠️ هشدار: ${validation.messageFa}\nدر صورت اطمینان دکمه «تایید و ثبت» را بزنید.", isAi = false)
                    return@launch
                }

                val result = action.execute(params)
                appendMessage("ASSISTANT", result.messageFa, isAi = false)

                if (result.success && result.undoToken != null) {
                    lastActionRecord = intent to result.undoToken
                    _uiState.update {
                        it.copy(
                            undoSnackbarMessage = result.messageFa,
                            undoToken = result.undoToken
                        )
                    }
                }
            }
            _uiState.update { it.copy(isExecuting = false) }
        }
    }

    fun undoLastAction() {
        val last = lastActionRecord ?: return
        viewModelScope.launch {
            val undoAction = actionRegistry.get<Unit>(IntentId.UNDO_LAST)
            if (undoAction != null) {
                val result = undoAction.execute(Unit)
                appendMessage("ASSISTANT", result.messageFa, isAi = false)
                if (result.success) {
                    lastActionRecord = null
                    _uiState.update { it.copy(undoSnackbarMessage = null, undoToken = null) }
                }
            }
        }
    }

    fun selectClarifyOption(intent: IntentId) {
        _uiState.update { it.copy(clarifyOptions = emptyList()) }
        if (isRestrictedInWorkerMode(intent)) {
            rejectWorkerModeAction()
            return
        }
        val slots = pendingSlots ?: emptyMap()
        viewModelScope.launch {
            val fullFlocks = flockRepo.getAllFlocks()
            val hasSingleFlock = slots.containsKey(SlotId.FLOCK_ID)
            val hasMultiAllocations = (slots[SlotId.FLOCK_ALLOCATIONS]?.typedValue as? List<*>)?.isNotEmpty() == true
            if (intent in AssistantRouter.FLOCK_SELECTION_REQUIRED_INTENTS &&
                fullFlocks.size > 1 &&
                !hasSingleFlock &&
                !hasMultiAllocations
            ) {
                val resp = dialogueManager.beginFlockSelection(
                    intent = intent,
                    slots = slots,
                    availableFlocks = fullFlocks.map { it.id to it.name }
                )
                presentFlockSelection(resp, fullFlocks)
            } else {
                pendingIntent = null
                pendingSlots = null
                pendingHasAlreadyWarned = false
                dialogueManager.reset()
                executeActionInternal(intent, slots)
            }
        }
    }

    fun clearChatHistory() {
        pendingIntent = null
        pendingSlots = null
        pendingHasAlreadyWarned = false
        dialogueManager.reset()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                pendingConfirmationCard = null,
                pendingFlockSelection = null,
                clarifyOptions = emptyList(),
                undoSnackbarMessage = null,
                undoToken = null,
                voiceErrorMessage = null,
                showSystemVoiceFallback = false
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dbHelper.clearMessages()
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    fun dismissUndoSnackbar() {
        _uiState.update { it.copy(undoSnackbarMessage = null, undoToken = null) }
    }

    private fun appendMessage(role: String, text: String, isAi: Boolean) {
        val msg = ChatUiMessage(sender = role, text = text, isAi = isAi)
        _uiState.update { it.copy(messages = it.messages + msg) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dbHelper.insertMessage(
                    AssistantMessageEntity(
                        role = role,
                        text = text,
                        source = if (isAi) "ONLINE" else "LOCAL"
                    )
                )
            } catch (_: Exception) {}
        }
    }
}

