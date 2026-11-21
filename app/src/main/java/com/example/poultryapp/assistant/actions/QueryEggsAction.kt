package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.repository.EggProductionRepository
import com.example.poultryapp.engine.JalaliDateHelper

data class QueryEggsParams(
    val flockId: Long? = null,
    val period: String? = null
)

class QueryEggsAction(
    private val eggRepo: EggProductionRepository
) : AssistantAction<QueryEggsParams> {

    override val intent: IntentId = IntentId.QUERY_EGGS

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>): QueryEggsParams {
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
        val period = slots[SlotId.PERIOD]?.typedValue as? String
        return QueryEggsParams(flockId = flockId, period = period)
    }

    override suspend fun validate(params: QueryEggsParams): ValidationResult = ValidationResult.Valid

    override fun preview(params: QueryEggsParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "استعلام آمار تولید تخم‌مرغ",
            titleEn = "Query Egg Production Stats",
            fields = listOf("بازه" to (params.period ?: "امروز"))
        )
    }

    override suspend fun execute(params: QueryEggsParams): ActionResult {
        val records = if (params.flockId != null) {
            eggRepo.getEggProductionsForFlock(params.flockId)
        } else {
            eggRepo.getAllEggProductions()
        }

        val todayJalali = JalaliDateHelper.getJalaliDate().toStandardString()
        val todayRecords = records.filter { it.dateJalaliStr == todayJalali }

        val totalToday = todayRecords.sumOf { it.totalCount }
        val brokenToday = todayRecords.sumOf { it.brokenCount }

        val messageFa = if (todayRecords.isNotEmpty()) {
            "آمار تولید امروز ($todayJalali): مجموعاً $totalToday عدد تخم‌مرغ ثبت شده است" +
                    (if (brokenToday > 0) " ($brokenToday عدد شکسته)." else ".")
        } else {
            "امروز هنوز آماری برای تولید تخم‌مرغ ثبت نشده است."
        }

        val messageEn = if (todayRecords.isNotEmpty()) {
            "Today's egg production ($todayJalali): total $totalToday eggs recorded" +
                    (if (brokenToday > 0) " ($brokenToday broken)." else ".")
        } else {
            "No egg production has been recorded for today yet."
        }

        return ActionResult(
            success = true,
            messageFa = messageFa,
            messageEn = messageEn
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}
