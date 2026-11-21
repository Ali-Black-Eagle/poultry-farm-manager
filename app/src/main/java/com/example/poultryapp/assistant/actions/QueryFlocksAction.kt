package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.engine.toPersianDigits

data class QueryFlocksParams(
    val flockId: Long?,
    val flockName: String?
)

class QueryFlocksAction(
    private val flockRepo: FlockRepository
) : AssistantAction<QueryFlocksParams> {

    override val intent: IntentId = IntentId.QUERY_FLOCKS

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>): QueryFlocksParams {
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
        val flockName = slots[SlotId.FLOCK_NAME]?.typedValue?.toString()
        return QueryFlocksParams(flockId, flockName)
    }

    override suspend fun validate(params: QueryFlocksParams): ValidationResult = ValidationResult.Valid

    override fun preview(params: QueryFlocksParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "استعلام آمار سالن‌ها و پرندگان",
            titleEn = "Query Flocks",
            fields = listOf("سالن" to (params.flockName ?: "همه سالن‌ها"))
        )
    }

    override suspend fun execute(params: QueryFlocksParams): ActionResult {
        val flocks = flockRepo.getAllFlocks()
        if (flocks.isEmpty()) {
            return ActionResult(
                success = true,
                messageFa = "در حال حاضر هیچ سالن فعالی در فارم ثبت نشده است.",
                messageEn = "No active flocks found in the farm."
            )
        }

        val totalBirds = flocks.sumOf { it.effectiveBirdCount }
        val sbFa = StringBuilder()
        sbFa.append("🏢 وضعیت فارم: ${toPersianDigits(flocks.size)} سالن فعال | مجموعاً ${toPersianDigits(totalBirds)} قطعه پرنده\n\n")

        for (f in flocks) {
            sbFa.append("• ${f.name}: ${toPersianDigits(f.effectiveBirdCount)} قطعه ${f.species.titleFa} (${f.breed})\n")
            sbFa.append("  سن گله: ${f.ageDisplayFa}\n")
            if (f.isWithdrawalActive) {
                sbFa.append("  ⚠️ پرهیز دارویی فعال (${f.activeMedicationName ?: "دارو"}): ${toPersianDigits(f.withdrawalRemainingDays.toInt())} روز باقی‌مانده\n")
            }
        }

        return ActionResult(
            success = true,
            messageFa = sbFa.toString().trim(),
            messageEn = "Found ${flocks.size} active flocks with $totalBirds total birds."
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

