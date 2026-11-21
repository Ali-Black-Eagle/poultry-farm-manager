package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.entities.DateParser
import com.example.poultryapp.assistant.core.nlu.FlockAllocation
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.FlockMortalityLog
import com.example.poultryapp.data.local.MortalityReason
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.data.repository.MortalityRepository
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.engine.toPersianDigits
import java.time.LocalDate

data class LogMortalityParams(
    val flockId: Long?,
    val flockName: String?,
    val count: Int,
    val allocations: List<FlockAllocation> = emptyList(),
    val reason: MortalityReason,
    val diagnosisHint: String?,
    val dateJalaliStr: String
)

class LogMortalityAction(
    private val mortalityRepo: MortalityRepository,
    private val flockRepo: FlockRepository
) : AssistantAction<LogMortalityParams> {

    override val intent: IntentId = IntentId.LOG_MORTALITY

    override fun requiredSlots(): List<SlotId> = listOf(SlotId.COUNT)

    override fun fromSlots(slots: Map<SlotId, SlotValue>): LogMortalityParams {
        val allocations = (slots[SlotId.FLOCK_ALLOCATIONS]?.typedValue as? List<*>)
            ?.filterIsInstance<FlockAllocation>()
            ?: emptyList()
        val count = if (allocations.isNotEmpty()) {
            allocations.sumOf { it.quantity.toInt() }
        } else {
            (slots[SlotId.COUNT]?.typedValue as? Number)?.toInt() ?: 0
        }
        val reasonStr = slots[SlotId.MORTALITY_REASON]?.typedValue?.toString()
        val reason = MortalityReason.fromString(reasonStr)
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
            ?: allocations.singleOrNull()?.flockId
        val flockName = slots[SlotId.FLOCK_NAME]?.typedValue?.toString()
            ?: allocations.singleOrNull()?.flockName
        val parsedDate = slots[SlotId.DATE]?.typedValue as? LocalDate
        val dateStr = if (parsedDate != null) {
            DateParser.formatToStandardJalali(parsedDate)
        } else {
            JalaliDateHelper.getJalaliDate().toStandardString()
        }
        val note = slots[SlotId.NOTE]?.typedValue?.toString()

        return LogMortalityParams(
            flockId = flockId,
            flockName = flockName,
            count = count,
            allocations = allocations,
            reason = reason,
            diagnosisHint = note,
            dateJalaliStr = dateStr
        )
    }

    private fun resolveTargets(params: LogMortalityParams, flocks: List<Flock>): List<Pair<Flock, Int>>? {
        if (flocks.isEmpty()) return null
        return when {
            params.allocations.isNotEmpty() -> {
                val resolved = mutableListOf<Pair<Flock, Int>>()
                for (alloc in params.allocations) {
                    val flock = flocks.firstOrNull { it.id == alloc.flockId } ?: return null
                    resolved.add(flock to alloc.quantity.toInt())
                }
                resolved
            }
            params.flockId != null -> {
                val flock = flocks.firstOrNull { it.id == params.flockId } ?: return null
                listOf(flock to params.count)
            }
            flocks.size == 1 -> {
                listOf(flocks[0] to params.count)
            }
            else -> null
        }
    }

    override suspend fun validate(params: LogMortalityParams): ValidationResult {
        val flocks = flockRepo.getAllFlocks()
        if (flocks.isEmpty()) {
            return ValidationResult.Rejected(
                "هنوز هیچ گله‌ای ثبت نشده است.",
                "No flocks registered."
            )
        }
        if (params.allocations.isEmpty() && params.flockId == null && flocks.size > 1) {
            return ValidationResult.Rejected(
                "شما چند گله دارید؛ لطفاً ابتدا گله یا گله‌های مورد نظر را انتخاب کنید.",
                "Multiple flocks exist; please select target flock(s)."
            )
        }
        val targets = resolveTargets(params, flocks)
            ?: return ValidationResult.Rejected(
                "گلهٔ انتخاب‌شده یافت نشد.",
                "Selected flock could not be found."
            )

        for ((flock, count) in targets) {
            if (count <= 0) {
                return ValidationResult.Rejected(
                    "تعداد تلفات برای «${flock.name}» باید بزرگتر از صفر باشد.",
                    "Mortality count must be greater than zero."
                )
            }
            if (count > flock.effectiveBirdCount) {
                return ValidationResult.Rejected(
                    "تعداد تلفات (${toPersianDigits(count)} قطعه) از موجودی زنده «${flock.name}» (${toPersianDigits(flock.effectiveBirdCount)} قطعه) بیشتر است.",
                    "Mortality count ($count) exceeds live bird count of ${flock.name} (${flock.effectiveBirdCount})."
                )
            }
        }
        val total = targets.sumOf { it.second }
        if (total > 1000) {
            return ValidationResult.Warning(
                "تعداد تلفات بسیار بالاست (${toPersianDigits(total)} قطعه). آیا مطمئن هستید؟",
                "Mortality count is very high ($total). Please confirm."
            )
        }
        return ValidationResult.Valid
    }

    override fun preview(params: LogMortalityParams): ActionCardPreview {
        val fields = mutableListOf<Pair<String, String>>()
        if (params.allocations.size > 1) {
            fields.add("مجموع تلفات" to "${toPersianDigits(params.count)} قطعه")
            fields.add("علت" to params.reason.titleFa)
            params.allocations.forEach { alloc ->
                fields.add(alloc.flockName to "${toPersianDigits(alloc.quantity.toInt())} قطعه")
            }
        } else {
            fields.add("تعداد" to "${toPersianDigits(params.count)} قطعه")
            fields.add("علت" to params.reason.titleFa)
            val displayFlock = params.allocations.firstOrNull()?.flockName ?: params.flockName
            if (displayFlock != null) {
                fields.add("سالن" to displayFlock)
            }
        }
        fields.add("تاریخ" to toPersianDigits(params.dateJalaliStr))
        return ActionCardPreview(
            titleFa = "ثبت تلفات پرندگان",
            titleEn = "Log Bird Mortality",
            fields = fields
        )
    }

    override suspend fun execute(params: LogMortalityParams): ActionResult {
        val flocks = flockRepo.getAllFlocks()
        val targets = resolveTargets(params, flocks)
            ?: return ActionResult(
                success = false,
                messageFa = "لطفاً ابتدا گله یا گله‌های مورد نظر را انتخاب کنید.",
                messageEn = "Target flock(s) must be selected."
            )

        val createdIds = mutableListOf<Long>()
        val summaryLinesFa = mutableListOf<String>()
        val summaryLinesEn = mutableListOf<String>()

        for ((targetFlock, count) in targets) {
            val log = FlockMortalityLog(
                flockId = targetFlock.id,
                flockName = targetFlock.name,
                count = count,
                reason = params.reason,
                diagnosisHint = params.diagnosisHint,
                dateJalaliStr = params.dateJalaliStr
            )
            val logId = mortalityRepo.insertMortalityLog(log)
            if (logId > 0) {
                createdIds.add(logId)
                summaryLinesFa.add("• ${targetFlock.name}: ${toPersianDigits(count)} قطعه")
                summaryLinesEn.add("${targetFlock.name}: $count")
            }
        }

        if (createdIds.isEmpty()) {
            return ActionResult(
                success = false,
                messageFa = "خطا در ثبت تلفات.",
                messageEn = "Failed to log mortalities."
            )
        }

        val undoToken = "MORTALITY:${createdIds.joinToString(",")}"
        return if (targets.size == 1) {
            val (onlyFlock, onlyCount) = targets[0]
            ActionResult(
                success = true,
                messageFa = "تلفات ${toPersianDigits(onlyCount)} قطعه برای «${onlyFlock.name}» به علت ${params.reason.titleFa} ثبت شد.",
                messageEn = "Logged $onlyCount mortalities in ${onlyFlock.name} due to ${params.reason.name}.",
                createdRecordId = createdIds.firstOrNull(),
                undoToken = undoToken
            )
        } else {
            val totalCount = targets.sumOf { it.second }
            ActionResult(
                success = true,
                messageFa = "تلفات به علت ${params.reason.titleFa} ثبت شد:\n${summaryLinesFa.joinToString("\n")}\nمجموع کل: ${toPersianDigits(totalCount)} قطعه",
                messageEn = "Logged $totalCount mortalities (${summaryLinesEn.joinToString(", ")}).",
                createdRecordId = createdIds.firstOrNull(),
                undoToken = undoToken
            )
        }
    }

    override suspend fun undo(undoToken: String): Boolean {
        if (!undoToken.startsWith("MORTALITY:")) return false
        val ids = undoToken.removePrefix("MORTALITY:")
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        if (ids.isEmpty()) return false
        var anyDeleted = false
        for (id in ids) {
            if (mortalityRepo.deleteMortalityLog(id)) {
                anyDeleted = true
            }
        }
        return anyDeleted
    }
}
