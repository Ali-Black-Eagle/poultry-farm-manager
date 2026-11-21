package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.model.MedicationCatalog
import com.example.poultryapp.data.model.PoultryMedication

data class QueryMedicationParams(
    val medName: String
)

class QueryMedicationAction : AssistantAction<QueryMedicationParams> {

    override val intent: IntentId = IntentId.QUERY_MEDICATION

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>): QueryMedicationParams {
        val med = slots[SlotId.MEDICATION_NAME]?.typedValue?.toString()
            ?: slots[SlotId.NOTE]?.typedValue?.toString()
            ?: ""
        return QueryMedicationParams(med)
    }

    override suspend fun validate(params: QueryMedicationParams): ValidationResult = ValidationResult.Valid

    override fun preview(params: QueryMedicationParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "استعلام مشخصات دارو",
            titleEn = "Query Medication",
            fields = listOf("دارو" to params.medName)
        )
    }

    override suspend fun execute(params: QueryMedicationParams): ActionResult {
        val q = params.medName
        val matchedMed = if (q.isNotBlank()) {
            val fromDrugs = MedicationCatalog.findMatchingMedications(q).firstOrNull()
            fromDrugs ?: MedicationCatalog.allMedications.find {
                it.tradeNameFa.contains(q, ignoreCase = true) ||
                        it.genericNameEn.contains(q, ignoreCase = true) ||
                        it.activeIngredientsFa.contains(q, ignoreCase = true)
            }
        } else {
            MedicationCatalog.allMedications.firstOrNull()
        }

        if (matchedMed == null) {
            return ActionResult(
                success = true,
                messageFa = "داروی '$q' در کاتالوگ دارویی یافت نشد. لطفاً نام ژنریک یا تجاری دارو را بررسی کنید.",
                messageEn = "Medication not found."
            )
        }

        val sb = StringBuilder()
        sb.append("💊 ${matchedMed.tradeNameFa}\n")
        sb.append("نام ژنریک: ${matchedMed.genericNameEn}\n\n")
        sb.append("📋 موارد مصرف:\n${matchedMed.indicationsFa}\n\n")
        sb.append("⚖️ دوز و روش مصرف:\n${matchedMed.dosageFa}\n")
        sb.append("طول دوره درمان: ${matchedMed.durationDaysFa}\n\n")
        sb.append("⏳ دوره پرهیز از مصرف (Withdrawal):\n${matchedMed.withdrawalPeriodFa}\n\n")
        sb.append("⚠️ موارد منع و احتیاط:\n${matchedMed.contraindicationsFa}")

        return ActionResult(
            success = true,
            messageFa = sb.toString().trim(),
            messageEn = "Found medication info for ${matchedMed.tradeNameFa}."
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

