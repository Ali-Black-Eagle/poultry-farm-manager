package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.local.FlockExitAnalytics
import com.example.poultryapp.data.repository.BirdExitRepository
import com.example.poultryapp.data.repository.MortalityRepository
import com.example.poultryapp.engine.toPersianDigits

data class QueryMortalityParams(
    val flockId: Long?,
    val flockName: String?
)

class QueryMortalityAction(
    private val mortalityRepo: MortalityRepository,
    private val birdExitRepo: BirdExitRepository? = null
) : AssistantAction<QueryMortalityParams> {

    override val intent: IntentId = IntentId.QUERY_MORTALITY

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>): QueryMortalityParams {
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
        val flockName = slots[SlotId.FLOCK_NAME]?.typedValue?.toString()
        return QueryMortalityParams(flockId, flockName)
    }

    override suspend fun validate(params: QueryMortalityParams): ValidationResult = ValidationResult.Valid

    override fun preview(params: QueryMortalityParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "استعلام آمار تلفات زیستی",
            titleEn = "Query Biological Mortality",
            fields = listOf("سالن" to (params.flockName ?: "همه سالن‌ها"))
        )
    }

    override suspend fun execute(params: QueryMortalityParams): ActionResult {
        val logs = mortalityRepo.getAllMortalityLogs()
        val bioLogs = FlockExitAnalytics.filterBiologicalMortality(logs)
        val filtered = if (params.flockId != null) {
            bioLogs.filter { it.flockId == params.flockId }
        } else {
            bioLogs
        }

        val soldCount = birdExitRepo?.getAllBirdSales()
            ?.let { list -> if (params.flockId != null) list.filter { it.flockId == params.flockId } else list }
            ?.sumOf { it.birdCount } ?: 0
        val slaughteredCount = birdExitRepo?.getAllBirdSlaughters()
            ?.let { list -> if (params.flockId != null) list.filter { it.flockId == params.flockId } else list }
            ?.sumOf { it.birdCount } ?: 0

        val separationNoteFa = if (soldCount > 0 || slaughteredCount > 0) {
            "\nℹ️ فروش (${toPersianDigits(soldCount)} قطعه) و کشتار (${toPersianDigits(slaughteredCount)} قطعه) به‌طور مجزا ثبت شده‌اند و در تلفات محاسبه نمی‌شوند."
        } else {
            ""
        }

        if (filtered.isEmpty()) {
            return ActionResult(
                success = true,
                messageFa = "خوشبختانه هیچ رکورد تلفات زیستی برای این دوره ثبت نشده است.$separationNoteFa",
                messageEn = "No biological mortality records found."
            )
        }

        val totalDead = filtered.sumOf { it.count }
        val byReason = filtered.groupBy { it.reason.titleFa }
            .mapValues { entry -> entry.value.sumOf { it.count } }

        val sbFa = StringBuilder()
        sbFa.append("⚠️ آمار تلفات زیستی ثبت‌شده: ${toPersianDigits(totalDead)} قطعه\n")
        sbFa.append("علت‌های ثبت‌شده:\n")
        for ((reason, count) in byReason) {
            sbFa.append("• $reason: ${toPersianDigits(count)} قطعه\n")
        }
        if (separationNoteFa.isNotBlank()) {
            sbFa.append(separationNoteFa.trimStart('\n'))
        }

        return ActionResult(
            success = true,
            messageFa = sbFa.toString().trim(),
            messageEn = "Total biological mortality is $totalDead birds."
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

