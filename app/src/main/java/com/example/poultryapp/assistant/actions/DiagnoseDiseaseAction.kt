package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.model.DiseaseCatalog
import com.example.poultryapp.data.model.DiseaseInfo

data class DiagnoseDiseaseParams(
    val queryText: String
)

class DiagnoseDiseaseAction : AssistantAction<DiagnoseDiseaseParams> {

    override val intent: IntentId = IntentId.DIAGNOSE_DISEASE

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>): DiagnoseDiseaseParams {
        val query = slots[SlotId.NOTE]?.typedValue?.toString()
            ?: slots[SlotId.SYMPTOM_TOKENS]?.rawSpan
            ?: ""
        return DiagnoseDiseaseParams(query)
    }

    override suspend fun validate(params: DiagnoseDiseaseParams): ValidationResult = ValidationResult.Valid

    override fun preview(params: DiagnoseDiseaseParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "تشخیص بالینی بیماری",
            titleEn = "Disease Diagnosis",
            fields = listOf("علائم" to params.queryText)
        )
    }

    override suspend fun execute(params: DiagnoseDiseaseParams): ActionResult {
        val q = params.queryText

        // 1. Direct disease name query (e.g. "علائم نیوکاسل چیه؟")
        val directDisease = DiseaseCatalog.findDiseaseByName(q)
        if (directDisease != null) {
            val report = formatDiseaseDetail(directDisease)
            return ActionResult(
                success = true,
                messageFa = report,
                messageEn = "Found disease info for ${directDisease.nameFa}."
            )
        }

        // 2. Symptom based diagnosis
        val matchedSymptoms = DiseaseCatalog.findSymptomsFromVoiceText(q)
        if (matchedSymptoms.isNotEmpty()) {
            val symptomIds = matchedSymptoms.map { it.id }.toSet()

            val scored = DiseaseCatalog.allDiseases.map { disease ->
                val primaryMatches = disease.primarySymptomIds.count { symptomIds.contains(it) }
                val secondaryMatches = disease.secondarySymptomIds.count { symptomIds.contains(it) }
                val pathognomonicMatches = disease.pathognomonicSymptomIds.count { symptomIds.contains(it) }
                val totalScore = (pathognomonicMatches * 3) + (primaryMatches * 2) + secondaryMatches
                disease to totalScore
            }.filter { it.second > 0 }.sortedByDescending { it.second }

            val topDisease = scored.firstOrNull()?.first
            if (topDisease != null) {
                val report = formatDiseaseDetail(topDisease, matchedSymptoms.map { it.titleFa })
                return ActionResult(
                    success = true,
                    messageFa = report,
                    messageEn = "Diagnosed suspect disease: ${topDisease.nameFa}."
                )
            }
        }

        // Fallback search only if query contains specific terms beyond generic words
        val cleanQ = q.trim()
        val isGenericQuery = cleanQ.length < 3 || cleanQ in listOf("بیماری", "مریضی", "درمان", "مرغ", "پرنده", "disease", "sick", "علائم")
        if (!isGenericQuery) {
            val searchResults = DiseaseCatalog.searchDiseases(cleanQ, null)
            val firstResult = searchResults.firstOrNull()
            if (firstResult != null) {
                return ActionResult(
                    success = true,
                    messageFa = formatDiseaseDetail(firstResult),
                    messageEn = "Matched disease: ${firstResult.nameFa}."
                )
            }
        }

        return ActionResult(
            success = true,
            messageFa = "علائم ذکر شده به تنهایی برای تشخیص دقیق کافی نیست. لطفاً علائم بالینی بیشتری مثل رنگ اسهال (سبز، سفید، خونی)، وضعیت تنفس (خس‌خس، سرفه)، تورم سر، فلجی یا افت تخم‌گذاری را ذکر کنید یا با دامپزشک فارم مشورت نمایید.",
            messageEn = "Could not diagnose disease with provided symptoms."
        )
    }

    private fun formatDiseaseDetail(d: DiseaseInfo, detectedSymptoms: List<String> = emptyList()): String {
        val sb = StringBuilder()
        sb.append("🩺 تشخیص مشکوک: ${d.nameFa} (${d.scientificName})\n\n")

        if (detectedSymptoms.isNotEmpty()) {
            sb.append("🔍 علائم تطبیق‌یافته: ${detectedSymptoms.joinToString("، ")}\n\n")
        }

        sb.append("📋 خلاصه علائم بالینی:\n${d.fullSymptomsDescriptionFa}\n\n")
        sb.append("💊 اقدامات درمانی و دارویی فوری:\n${d.treatmentStepsFa}\n")

        if (d.commonDrugsFa.isNotBlank()) {
            sb.append("داروهای رایج: ${d.commonDrugsFa}\n")
        }
        if (d.dosagesFa.isNotBlank()) {
            sb.append("دوز پیشنهادی: ${d.dosagesFa}\n\n")
        }

        sb.append("⚠️ توصیه‌های قرنطینه و پیشگیری:\n${d.quarantineAdviceFa}")
        return sb.toString()
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

