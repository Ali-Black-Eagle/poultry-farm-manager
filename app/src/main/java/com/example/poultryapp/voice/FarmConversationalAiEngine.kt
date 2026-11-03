package com.example.poultryapp.voice

import com.example.poultryapp.R
import com.example.poultryapp.data.local.EggProduction
import com.example.poultryapp.data.local.FarmContact
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.UserRole
import com.example.poultryapp.engine.FarmIntentSemanticRouter
import com.example.poultryapp.engine.FarmLiveContext
import com.example.poultryapp.engine.toPersianDigits
import java.util.Locale
import java.util.UUID

/**
 * فرستنده پیام در رشته مکالمه هوش مصنوعی
 */
enum class TurnSender {
    USER,
    AI
}

/**
 * ساختار هر نوبت از گفتگوی چندنوبتی
 */
data class ConversationTurn(
    val id: String = UUID.randomUUID().toString(),
    val sender: TurnSender,
    val text: String,
    val actionCard: VoiceActionCardData? = null,
    val proactiveActions: List<AgenticProactiveAction> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * وضعیت حافظه و زمینه مکالمه هوش مصنوعی در طول نشست جاری
 */
data class AiSessionState(
    val activeFlockId: Long? = null,
    val activeFlockName: String? = null,
    val activeTopic: String? = null,
    val pendingActionCard: VoiceActionCardData? = null,
    val turns: List<ConversationTurn> = emptyList(),
    val turnCount: Int = 0
)

/**
 * نتیجه پردازش هر نوبت مکالمه توسط موتور هوش مصنوعی
 */
data class AiTurnResult(
    val aiResponseText: String,
    val actionCard: VoiceActionCardData? = null,
    val updatedSessionState: AiSessionState,
    val shouldDismissSheet: Boolean = false,
    val autoListenNextTurn: Boolean = true
)

/**
 * هسته هماهنگ‌کننده ایجنتیک هوش مصنوعی فارم (Autonomous Agentic Coordinator)
 * این موتور بخش‌های عملیاتی فارم را به صورت ۱۰۰٪ آفلاین و محلی پردازش می‌کند،
 * تعاملات متداول روزمره (سلام، تشکر، خداحافظی) را بدون مصرف توکن پاسخ می‌دهد،
 * و پرسش‌های دانشی و عمومی را جهت پاسخ‌دهی به درگاه API آنلاین (AiQuestionAnswerBridge) ارجاع می‌دهد.
 */
object FarmConversationalAiEngine {

    /**
     * پردازش هوشمند نوبت مکالمه با در نظر گرفتن زمینه زنده فارم
     */
    fun processTurn(
        spokenText: String,
        currentState: AiSessionState,
        farmContext: FarmLiveContext,
        userRole: UserRole = UserRole.MANAGER
    ): AiTurnResult {
        return processTurn(
            spokenText = spokenText,
            currentState = currentState,
            flocks = farmContext.flocks,
            contacts = farmContext.contacts,
            eggProductions = farmContext.eggProductions,
            farmContext = farmContext,
            userRole = userRole
        )
    }

    /**
     * پردازش هوشمند نوبت مکالمه با کانتکست چندمرحله‌ای، اسلات‌فیلینگ، کارت‌های اقدام و تفکیک آفلاین/آنلاین
     */
    fun processTurn(
        spokenText: String,
        currentState: AiSessionState,
        flocks: List<Flock>,
        contacts: List<FarmContact>,
        eggProductions: List<EggProduction> = emptyList(),
        farmContext: FarmLiveContext = FarmLiveContext(flocks = flocks, contacts = contacts, eggProductions = eggProductions),
        userRole: UserRole = UserRole.MANAGER
    ): AiTurnResult {
        val norm = PersianVoiceNormalizer.normalize(spokenText)
        val userTurn = ConversationTurn(
            sender = TurnSender.USER,
            text = spokenText
        )
        val turnsWithUser = currentState.turns + userTurn

        // ۱. تشخیص گله فعال بر اساس کلام جاری + حل ضمایر و تداوم کانتکست (Coreference Resolution)
        val explicitFlock = resolveActiveFlock(norm, flocks)
        val sessionFlock = currentState.activeFlockId?.let { id -> flocks.find { it.id == id } }
        val isContinuityOrPronoun = hasFlockReferenceOrContinuity(norm)

        val resolvedFlock = when {
            explicitFlock != null -> explicitFlock
            sessionFlock != null && isContinuityOrPronoun -> sessionFlock
            else -> null
        }
        val activeFlockId = resolvedFlock?.id ?: currentState.activeFlockId
        val activeFlockName = resolvedFlock?.name ?: currentState.activeFlockName

        // ۲. اگر کارت اقدام منتظر تایید وجود دارد و کاربر تایید یا رد را بیان می‌کند:
        val pendingCard = currentState.pendingActionCard
        if (pendingCard != null) {
            if (VoiceIntentParser.isCancellation(norm)) {
                // لغو شد -> کارت پاک می‌شود و زمینه بازمی‌گردد
                val cancelMsg = "عملیات لغو شد. هیچ داده‌ای تغییر نکرد. کار دیگری هست که در سالن یا فارم انجام دهیم؟"
                val aiTurn = ConversationTurn(
                    sender = TurnSender.AI,
                    text = cancelMsg
                )
                return AiTurnResult(
                    aiResponseText = cancelMsg,
                    actionCard = null,
                    updatedSessionState = currentState.copy(
                        activeFlockId = activeFlockId,
                        activeFlockName = activeFlockName,
                        pendingActionCard = null,
                        turns = turnsWithUser + aiTurn,
                        turnCount = currentState.turnCount + 1
                    ),
                    shouldDismissSheet = false,
                    autoListenNextTurn = true
                )
            } else if (pendingCard.isValid && !pendingCard.needsMoreInfo && VoiceIntentParser.isConfirmation(norm)) {
                // تایید شد -> آماده اجرا
                val aiConfirmText = "بسیار عالی، دستور «${pendingCard.title}» آماده ثبت است. در حال اعمال تغییرات در سیستم..."
                val aiTurn = ConversationTurn(
                    sender = TurnSender.AI,
                    text = aiConfirmText,
                    actionCard = pendingCard
                )
                return AiTurnResult(
                    aiResponseText = aiConfirmText,
                    actionCard = pendingCard,
                    updatedSessionState = currentState.copy(
                        activeFlockId = activeFlockId,
                        activeFlockName = activeFlockName,
                        pendingActionCard = pendingCard,
                        turns = turnsWithUser + aiTurn,
                        turnCount = currentState.turnCount + 1
                    ),
                    shouldDismissSheet = false,
                    autoListenNextTurn = false
                )
            }
        }

        // ۴. انصراف و لغو درون‌نوبتی (Intra-Turn Cancellation) - ۱۰۰٪ آفلاین
        val isIntraTurnCancellation = (norm.contains("ثبت نکن") || norm.contains("لغو کن") || norm.contains("کنسل کن") ||
                norm.contains("ثبتش نکن") || norm.contains("اصلا ثبت نکن") || norm.contains("اصلاً ثبت نکن") ||
                (norm.contains("اشتباه شد") && (norm.contains("لغو") || norm.contains("نکن") || norm.contains("نه")))) &&
                !FarmIntentSemanticRouter.isQuestionPattern(norm)
        if (isIntraTurnCancellation) {
            val cancelMsg = "عملیات با توجه به انصراف شما لغو شد و هیچ اطلاعاتی در سیستم ثبت نگردید."
            val aiTurn = ConversationTurn(
                sender = TurnSender.AI,
                text = cancelMsg
            )
            return AiTurnResult(
                aiResponseText = cancelMsg,
                actionCard = null,
                updatedSessionState = currentState.copy(
                    activeFlockId = activeFlockId,
                    activeFlockName = activeFlockName,
                    pendingActionCard = null,
                    turns = turnsWithUser + aiTurn,
                    turnCount = currentState.turnCount + 1
                ),
                shouldDismissSheet = false,
                autoListenNextTurn = false
            )
        }

        // ۵. دفاع در برابر تزریق پرامپت و نیت‌های تخریبی (Prompt Injection Defense) - ۱۰۰٪ آفلاین
        val isPromptInjection = (norm.contains("دستورات قبلی") && (norm.contains("فراموش") || norm.contains("نادیده"))) ||
                (norm.contains("دیگر دستیار") || norm.contains("نیستی")) ||
                (norm.contains("رمز عبور") || norm.contains("پسورد") || norm.contains("اطلاعات پایگاه داده را پاک"))
        if (isPromptInjection) {
            val secureMsg = "من دستیار تخصصی مدیریت هوشمند مرغداری هستم و مجاز به تغییر دستورالعمل‌های پایه یا افشای اطلاعات امنیتی فارم نمی‌باشم. چطور می‌توانم در مدیریت گله و امور سالن‌ها به شما کمک کنم؟"
            val aiTurn = ConversationTurn(
                sender = TurnSender.AI,
                text = secureMsg
            )
            return AiTurnResult(
                aiResponseText = secureMsg,
                actionCard = null,
                updatedSessionState = currentState.copy(
                    activeFlockId = activeFlockId,
                    activeFlockName = activeFlockName,
                    pendingActionCard = null,
                    turns = turnsWithUser + aiTurn,
                    turnCount = currentState.turnCount + 1
                ),
                shouldDismissSheet = false,
                autoListenNextTurn = false
            )
        }

        // ۶. کلام ناقص یا بریده صوتی - ۱۰۰٪ آفلاین
        val isTrailingIncomplete = (spokenText.contains("...") || spokenText.contains("…") || norm.startsWith("می خواستم برای سالن")) &&
                !norm.contains("ثبت") && !norm.contains("بگو") && !norm.contains("چقدر") && !norm.contains("چطوره") &&
                !norm.contains("تلفات") && !norm.contains("دان") && !norm.contains("تخم")
        if (isTrailingIncomplete) {
            val incompleteMsg = "کلام شما ناقص دریافت شد. لطفاً بفرمایید چه عملیاتی (ثبت دان، تلفات، دما یا استعلام وضعیت) برای سالن مد نظرتان است؟"
            val aiTurn = ConversationTurn(
                sender = TurnSender.AI,
                text = incompleteMsg
            )
            return AiTurnResult(
                aiResponseText = incompleteMsg,
                actionCard = null,
                updatedSessionState = currentState.copy(
                    activeFlockId = activeFlockId,
                    activeFlockName = activeFlockName,
                    pendingActionCard = null,
                    turns = turnsWithUser + aiTurn,
                    turnCount = currentState.turnCount + 1
                ),
                shouldDismissSheet = false,
                autoListenNextTurn = true
            )
        }

        // ۶.۵. ابهام‌زدایی کلمات تک‌کلمه‌ای یا بسیار کوتاه
        val trimmedNorm = norm.trim()
        if (trimmedNorm == "ثبت") {
            val disambiguationCard = VoiceActionCardData(
                intentType = VoiceIntentType.HELP,
                title = "انتخاب موضوع ثبت عملیات",
                iconRes = R.drawable.ic_farm_dashboard,
                flockId = activeFlockId,
                flockName = activeFlockName,
                primaryMetric = "چه داده‌ای می‌خواهید ثبت شود؟",
                details = listOf("راهنما" to "یکی از موارد زیر را لمس کنید یا بگویید."),
                systemEffect = "انتخاب سریع عملیات ثبت",
                rawText = spokenText,
                isValid = false,
                needsMoreInfo = true,
                quickReplies = listOf("ثبت دان", "ثبت تلفات", "ثبت تخم‌مرغ", "ثبت دما", "ثبت وزن")
            )
            val replyText = "مایلید چه آماری برای سالن ثبت شود؟ لطفاً از گزینه‌های زیر انتخاب نمایید یا بفرمایید."
            val aiTurn = ConversationTurn(sender = TurnSender.AI, text = replyText, actionCard = disambiguationCard)
            return AiTurnResult(
                aiResponseText = replyText,
                actionCard = disambiguationCard,
                updatedSessionState = currentState.copy(turns = turnsWithUser + aiTurn, turnCount = currentState.turnCount + 1),
                shouldDismissSheet = false,
                autoListenNextTurn = true
            )
        }
        if (trimmedNorm == "بررسی سالن") {
            val disambiguationCard = VoiceActionCardData(
                intentType = VoiceIntentType.HELP,
                title = "بررسی وضعیت سالن",
                iconRes = R.drawable.ic_farm_dashboard,
                flockId = activeFlockId,
                flockName = activeFlockName,
                primaryMetric = "بررسی کدام بخش مد نظر شماست؟",
                details = listOf("راهنما" to "یکی از موارد زیر را لمس کنید یا بگویید."),
                systemEffect = "بررسی سریع وضعیت سالن",
                rawText = spokenText,
                isValid = false,
                needsMoreInfo = true,
                quickReplies = listOf("بررسی دمای گله‌ها", "آمار مصرف دان", "تلفات امروز")
            )
            val replyText = "چه موردی از سالن را مایلید بررسی کنید؟"
            val aiTurn = ConversationTurn(sender = TurnSender.AI, text = replyText, actionCard = disambiguationCard)
            return AiTurnResult(
                aiResponseText = replyText,
                actionCard = disambiguationCard,
                updatedSessionState = currentState.copy(turns = turnsWithUser + aiTurn, turnCount = currentState.turnCount + 1),
                shouldDismissSheet = false,
                autoListenNextTurn = true
            )
        }
        if (trimmedNorm == "دان") {
            val disambiguationCard = VoiceActionCardData(
                intentType = VoiceIntentType.HELP,
                title = "عملیات مربوط به دان",
                iconRes = R.drawable.ic_farm_feed_water,
                flockId = activeFlockId,
                flockName = activeFlockName,
                primaryMetric = "مدیریت دان و خوراک",
                details = listOf("راهنما" to "یکی از گزینه‌های زیر را انتخاب فرمایید."),
                systemEffect = "عملیات دان فارم",
                rawText = spokenText,
                isValid = false,
                needsMoreInfo = true,
                quickReplies = listOf("ثبت مصرف دان", "موجودی دان در انبار", "پایش هوشمند دان")
            )
            val replyText = "در مورد دان چه عملیاتی مد نظر شماست؟"
            val aiTurn = ConversationTurn(sender = TurnSender.AI, text = replyText, actionCard = disambiguationCard)
            return AiTurnResult(
                aiResponseText = replyText,
                actionCard = disambiguationCard,
                updatedSessionState = currentState.copy(turns = turnsWithUser + aiTurn, turnCount = currentState.turnCount + 1),
                shouldDismissSheet = false,
                autoListenNextTurn = true
            )
        }

        // ۷. ارزیابی فرامین ایجنتیک عملیاتی و ناوبری از طریق VoiceIntentParser - ۱۰۰٪ آفلاین
        val isNavCommand = FarmIntentSemanticRouter.isNavigationCommand(norm)
        val isOperationalIntent = FarmIntentSemanticRouter.isOperationalActionPattern(norm) || isNavCommand
        val sessionCtx = mutableMapOf<String, Any>()
        if (activeFlockId != null) sessionCtx["activeFlockId"] = activeFlockId
        if (pendingCard != null && pendingCard.needsMoreInfo) {
            sessionCtx["pendingIntent"] = pendingCard.intentType.name
            if (pendingCard.flockId != null) sessionCtx["flockId"] = pendingCard.flockId
            sessionCtx.putAll(pendingCard.parameters)
        }

        val parsedCard = VoiceIntentParser.parseVoiceCommand(
            spokenText = spokenText,
            flocks = flocks,
            contacts = contacts,
            sessionContext = sessionCtx,
            eggProductions = eggProductions,
            farmContext = farmContext,
            userRole = userRole
        )

        val isMeaningfulAction = parsedCard.intentType != VoiceIntentType.UNKNOWN &&
                parsedCard.intentType != VoiceIntentType.HELP &&
                parsedCard.intentType != VoiceIntentType.DIAGNOSE_DISEASE &&
                (parsedCard.isValid || parsedCard.needsMoreInfo || isNavCommand || parsedCard.isMutatingAction)

        if ((isMeaningfulAction || isOperationalIntent || isNavCommand) && parsedCard.intentType != VoiceIntentType.DIAGNOSE_DISEASE) {
            // اعتبارسنجی سطح دسترسی کاربر (RBAC Security)
            val rbacRejection = if (!parsedCard.needsMoreInfo) VoiceActionDispatcher.checkRolePermission(parsedCard, userRole) else null
            if (rbacRejection != null) {
                val rejectionCard = parsedCard.copy(isValid = false, errorMessage = rbacRejection.message)
                val aiTurn = ConversationTurn(
                    sender = TurnSender.AI,
                    text = rbacRejection.message,
                    actionCard = rejectionCard
                )
                return AiTurnResult(
                    aiResponseText = rbacRejection.message,
                    actionCard = rejectionCard,
                    updatedSessionState = currentState.copy(
                        activeFlockId = activeFlockId,
                        activeFlockName = activeFlockName,
                        pendingActionCard = null,
                        turns = turnsWithUser + aiTurn,
                        turnCount = currentState.turnCount + 1
                    ),
                    shouldDismissSheet = false,
                    autoListenNextTurn = false
                )
            }

            // تعیین متن پاسخ گفتگویی متناسب با کارت صادرشده
            val aiResponseText = when {
                parsedCard.needsMoreInfo -> {
                    val promptParam = parsedCard.missingPromptFa ?: "اطلاعات تکمیلی"
                    "لطفاً $promptParam را مشخص بفرمایید تا دستور «${parsedCard.title}» تکمیل و ثبت شود."
                }
                parsedCard.intentType == VoiceIntentType.NAVIGATE_SCREEN -> {
                    val screenName = parsedCard.parameters["screenName"] ?: "صفحه مد نظر"
                    "در حال انتقال شما به بخش «$screenName»..."
                }
                parsedCard.intentType == VoiceIntentType.CALL_CONTACT -> {
                    val contactName = parsedCard.parameters["contactName"] ?: "مخاطب"
                    "در حال برقراری تماس با $contactName..."
                }
                parsedCard.intentType == VoiceIntentType.TOGGLE_TRACKING -> {
                    val trackingType = parsedCard.parameters["trackingType"] as? String ?: "پایش"
                    val isEnabled = parsedCard.parameters["isEnabled"] as? Boolean ?: true
                    val flockName = parsedCard.flockName ?: "گله"
                    val typeFa = if (trackingType == "WATER") "آب" else "دان"
                    val actionFa = if (isEnabled) "فعال‌سازی پایش هوشمند" else "غیرفعال‌سازی پایش هوشمند"
                    "دستور $actionFa $typeFa برای $flockName آماده ثبت است."
                }
                parsedCard.intentType == VoiceIntentType.QUERY_STATS -> {
                    val detStr = parsedCard.details.joinToString("\n") { p -> "${p.first}: ${p.second}" }
                    if (detStr.isNotBlank()) detStr else "گزارش وضعیت و آمار زنده سالن خدمت شما آماده گردید."
                }
                parsedCard.intentType == VoiceIntentType.CALCULATE_METRIC -> {
                    val detStr = parsedCard.details.joinToString("، ") { "${it.first}: ${it.second}" }
                    "دستور «${parsedCard.title}» انجام شد. ${parsedCard.primaryMetric} ($detStr)"
                }
                parsedCard.isMutatingAction -> {
                    "دستور «${parsedCard.title}» آماده تایید است. آیا ثبت نهایی انجام شود؟"
                }
                else -> {
                    "دستور «${parsedCard.title}» پردازش گردید."
                }
            }

            val aiTurn = ConversationTurn(
                sender = TurnSender.AI,
                text = aiResponseText,
                actionCard = parsedCard,
                proactiveActions = parsedCard.quickReplies.mapIndexed { idx: Int, reply: String ->
                    AgenticProactiveAction(
                        id = "PROACTIVE_$idx",
                        labelFa = reply,
                        actionCard = parsedCard
                    )
                }
            )

            return AiTurnResult(
                aiResponseText = aiResponseText,
                actionCard = parsedCard,
                updatedSessionState = currentState.copy(
                    activeFlockId = activeFlockId,
                    activeFlockName = activeFlockName,
                    pendingActionCard = if (parsedCard.isMutatingAction || parsedCard.needsMoreInfo) parsedCard else null,
                    turns = turnsWithUser + aiTurn,
                    turnCount = currentState.turnCount + 1,
                    activeTopic = parsedCard.intentType.name
                ),
                shouldDismissSheet = false,
                autoListenNextTurn = parsedCard.needsMoreInfo
            )
        }

        // ۷. اگر فرمان عملیاتی نبود، بررسی اتمام یا خداحافظی (Farewell / Dismiss) - ۱۰۰٪ آفلاین (۰ توکن)
        if (isFarewellIntent(norm)) {
            val farewellText = getFarewellResponse()
            val aiTurn = ConversationTurn(
                sender = TurnSender.AI,
                text = farewellText
            )
            return AiTurnResult(
                aiResponseText = farewellText,
                actionCard = null,
                updatedSessionState = currentState.copy(
                    activeFlockId = activeFlockId,
                    activeFlockName = activeFlockName,
                    turns = turnsWithUser + aiTurn,
                    turnCount = currentState.turnCount + 1,
                    pendingActionCard = null
                ),
                shouldDismissSheet = true,
                autoListenNextTurn = false
            )
        }

        // ۸. سپر ضد توکن‌سوزی: پاسخ‌دهی آفلاین و فوری به سلام، تعارفات، تشکر و هویت دستیار - ۱۰۰٪ آفلاین (۰ توکن)
        val smallTalkResponse = resolveSmallTalk(norm)
        if (smallTalkResponse != null) {
            val aiTurn = ConversationTurn(
                sender = TurnSender.AI,
                text = smallTalkResponse
            )
            return AiTurnResult(
                aiResponseText = smallTalkResponse,
                actionCard = null,
                updatedSessionState = currentState.copy(
                    activeFlockId = activeFlockId,
                    activeFlockName = activeFlockName,
                    turns = turnsWithUser + aiTurn,
                    turnCount = currentState.turnCount + 1
                ),
                shouldDismissSheet = false,
                autoListenNextTurn = true
            )
        }

        // ۹. سوالات علمی، دانشنامه‌ای و پاتولوژی -> ارجاع مستقیم به درگاه API آنلاین (AiQuestionAnswerBridge)
        val farmSummary = if (flocks.isNotEmpty()) {
            "فارم فعال دارای ${flocks.size.toPersianDigits()} گله (گله‌های فعال: ${flocks.joinToString { it.name }})"
        } else null

        val apiAnswer = DefaultAiQuestionAnswerBridge.answerQuestionSync(spokenText, farmSummary)

        val aiTurn = ConversationTurn(
            sender = TurnSender.AI,
            text = apiAnswer,
            actionCard = null
        )

        return AiTurnResult(
            aiResponseText = apiAnswer,
            actionCard = null,
            updatedSessionState = currentState.copy(
                activeFlockId = activeFlockId,
                activeFlockName = activeFlockName,
                pendingActionCard = null,
                turns = turnsWithUser + aiTurn,
                turnCount = currentState.turnCount + 1,
                activeTopic = "ONLINE_API_KNOWLEDGE"
            ),
            shouldDismissSheet = false,
            autoListenNextTurn = false
        )
    }

    /**
     * فیلتر ضد توکن‌سوزی: پاسخ‌دهی آفلاین، سریع و محلی به سلام، احوال‌پرسی، تشکر و معرفی
     */
    fun resolveSmallTalk(norm: String): String? {
        val trimmed = norm.trim()

        // سلام و درود
        if (trimmed in listOf("سلام", "سلام علیکم", "سلام علیک", "درود", "صبح بخیر", "عصر بخیر", "روز بخیر", "وقت بخیر", "یا علی") ||
            norm.startsWith("سلام ") || norm.startsWith("درود ") || norm.startsWith("صبح بخیر") || norm.startsWith("عصر بخیر")) {
            return "سلام و درود! روزتان به‌خیر. در خدمت شما هستم، چه کمکی در مدیریت سالن‌ها یا ثبت آمار فارم از من برمی‌آید؟"
        }

        // خسته نباشید
        if (norm.contains("خسته نباشید") || norm.contains("خسته نباشی") || norm.contains("خسته نباشین")) {
            return "سلامت و پایدار باشید! خسته کار پرزحمت مرغداری نباشید. آماده دریافت گزارش‌ها و فرامین شما هستم."
        }

        // احوال‌پرسی
        if (norm.contains("چطوری") || norm.contains("حالت چطوره") || norm.contains("خوبی") || norm.contains("اوضاع چطوره")) {
            return "ممنون از لطف شما، همه‌چیز عالی و آماده به کار است! سالن‌ها و گله‌ها در چه وضعیتی هستند؟"
        }

        // تشکر و قدردانی
        if (norm.contains("ممنون") || norm.contains("متشکرم") || norm.contains("دستت درد نکنه") ||
            norm.contains("دمت گرم") || norm.contains("عالی بود") || norm.contains("تشکر") || norm.contains("سپاس")) {
            return "خواهش می‌کنم، انجام وظیفه است! در خدمت شما و مرغداری هوشمندتان هستم."
        }

        // معرفی و هویت
        if (norm.contains("تو کی هستی") || norm.contains("شما کی هستی") || norm.contains("خودتو معرفی کن") ||
            norm.contains("چیکار میتونی بکنی") || norm.contains("راهنما") || norm.contains("کمک")) {
            return "من دستیار هوشمند و ایجنت مدیریت مرغداری شما هستم. می‌توانم دستورات ثبت دان، تلفات، تخم‌مرغ، آب، دما و وزن را بلافاصله در دیتابیس ثبت کنم، آمار زنده سالن‌ها را گزارش دهم، صفحات برنامه را باز کنم، و به سوالات علمی و تخصصی شما از طریق اتصال به هوش مصنوعی پاسخ دهم."
        }

        return null
    }

    /**
     * بررسی قصد خداحافظی
     */
    fun isFarewellIntent(norm: String): Boolean {
        if (norm.contains("تمام شده") || norm.contains("تمام‌شده") || norm.contains("بهای تمام") ||
            norm.contains("قیمت تمام") || norm.contains("هزینه تمام")) {
            return false
        }
        val trimmed = norm.trim()
        if (trimmed == "تمام" || trimmed == "خروج" || trimmed == "بای") return true
        return norm.contains("خداحافظ") || norm.contains("فعلا") || norm.contains("شب خوش") ||
                norm.contains("خسته نباشید خدانگهدار") || norm.contains("روز خوش") || norm.contains("خسته نباشی خدانگهدار")
    }

    private fun getFarewellResponse(): String {
        return "خسته نباشید و خداحافظ! در صورت نیاز به هرگونه ثبت عملیات یا استعلام، من همین‌جا آماده‌ام."
    }

    /**
     * تشخیص شماره سالن یا گله صریح در کلام کاربر
     */
    fun resolveActiveFlock(norm: String, flocks: List<Flock>): Flock? {
        return VoiceIntentParser.findTargetFlock(norm, flocks)
    }

    private fun hasFlockReferenceOrContinuity(norm: String): Boolean {
        return norm.contains("دماش") || norm.contains("تلفاتش") || norm.contains("دانش") ||
                norm.contains("تخمش") || norm.contains("تولیدش") || norm.contains("وضعیتش") ||
                norm.contains("اون") || norm.contains("همین سالن") || norm.contains("همین گله") ||
                norm.contains("این سالن") || norm.contains("این گله")
    }
}
