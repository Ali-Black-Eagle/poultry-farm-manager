package com.example.poultryapp.voice

import com.example.poultryapp.R
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.UserRole
import com.example.poultryapp.engine.FarmIntentSemanticRouter
import com.example.poultryapp.engine.FarmLiveContext
import com.example.poultryapp.engine.FarmSemanticIntent
import com.example.poultryapp.engine.toPersianDigits

/**
 * دسته‌بندی حالت اجرای ایجنت هوش مصنوعی
 */
enum class AgentExecutionMode(val titleFa: String, val badgeIconRes: Int) {
    APP_ACTION("اقدام اجرایی در سامانه ⚡", R.drawable.ic_farm_egg),
    FARM_OMNISCIENCE("اشراف آماری گله‌ها و انبار 📊", R.drawable.ic_farm_flocks),
    LOCAL_CONTROL_OR_GREETING("پاسخ آفلاین و تعارفات 💬", R.drawable.ic_farm_dashboard),
    AI_API_KNOWLEDGE("دانش و تحلیل آنلاین هوش مصنوعی 🌐", R.drawable.ic_farm_health)
}

/**
 * نتیجه اجرای نوبت ایجنت هوش مصنوعی
 */
data class AgentTurnResult(
    val aiResponseText: String,
    val actionCard: VoiceActionCardData?,
    val executionMode: AgentExecutionMode,
    val updatedSessionState: AiSessionState,
    val shouldDismissSheet: Boolean = false,
    val autoListenNextTurn: Boolean = true,
    val proactiveSuggestions: List<String> = emptyList()
)

/**
 * هسته مرکزی و ارکستراتور ایجنت هوش مصنوعی فارم (Autonomous Poultry AI Agent)
 */
object LocalPoultryAiAgent {

    fun processAgentTurn(
        spokenText: String,
        currentState: AiSessionState,
        farmContext: FarmLiveContext,
        userRole: UserRole = UserRole.MANAGER
    ): AgentTurnResult {
        val norm = PersianVoiceNormalizer.normalize(spokenText)
        val flocks = farmContext.flocks
        val matchedFlock = findTargetFlock(norm, flocks)

        val semanticIntent = FarmIntentSemanticRouter.routeIntent(
            norm = norm,
            spokenText = spokenText,
            matchedFlock = matchedFlock
        )

        val turnResult = FarmConversationalAiEngine.processTurn(
            spokenText = spokenText,
            currentState = currentState,
            farmContext = farmContext,
            userRole = userRole
        )

        val executionMode = mapToExecutionMode(
            semanticIntent = semanticIntent,
            actionCard = turnResult.actionCard,
            activeTopic = turnResult.updatedSessionState.activeTopic
        )

        return AgentTurnResult(
            aiResponseText = turnResult.aiResponseText,
            actionCard = turnResult.actionCard,
            executionMode = executionMode,
            updatedSessionState = turnResult.updatedSessionState,
            shouldDismissSheet = turnResult.shouldDismissSheet,
            autoListenNextTurn = turnResult.autoListenNextTurn,
            proactiveSuggestions = emptyList()
        )
    }

    private fun mapToExecutionMode(
        semanticIntent: FarmSemanticIntent,
        actionCard: VoiceActionCardData?,
        activeTopic: String?
    ): AgentExecutionMode {
        if (activeTopic == "ONLINE_API_KNOWLEDGE") {
            return AgentExecutionMode.AI_API_KNOWLEDGE
        }
        return when {
            actionCard?.isMutatingAction == true ||
            semanticIntent == FarmSemanticIntent.OPERATIONAL_RECORD ||
            semanticIntent == FarmSemanticIntent.APP_NAVIGATION -> AgentExecutionMode.APP_ACTION

            semanticIntent == FarmSemanticIntent.FLOCK_LIVE_STATS_QUERY ||
            activeTopic == "QUERY_STATS" -> AgentExecutionMode.FARM_OMNISCIENCE

            semanticIntent == FarmSemanticIntent.SMALL_TALK_OR_GREETING ||
            semanticIntent == FarmSemanticIntent.CANCEL_OR_DISMISS -> AgentExecutionMode.LOCAL_CONTROL_OR_GREETING

            else -> AgentExecutionMode.AI_API_KNOWLEDGE
        }
    }

    private fun findTargetFlock(norm: String, flocks: List<Flock>): Flock? {
        for (f in flocks) {
            if (norm.contains(f.name.lowercase())) return f
            if (norm.contains("سالن ${f.id}") || norm.contains("گله ${f.id}") ||
                norm.contains("سالن ${toPersianDigits(f.id)}") || norm.contains("گله ${toPersianDigits(f.id)}")) {
                return f
            }
        }
        return null
    }
}
