package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.dialogue.SafetyChecks
import com.example.poultryapp.assistant.core.dialogue.SafetyResult
import com.example.poultryapp.assistant.core.entities.DateParser
import com.example.poultryapp.assistant.core.nlu.FlockAllocation
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.local.EggProduction
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.repository.EggProductionRepository
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.engine.toPersianDigits
import java.time.LocalDate

data class AddEggsParams(
    val count: Int,
    val brokenCount: Int = 0,
    val date: LocalDate = LocalDate.now(),
    val flockId: Long? = null,
    val flockName: String? = null,
    val allocations: List<FlockAllocation> = emptyList(),
    val note: String? = null
)

class AddEggsAction(
    private val eggRepo: EggProductionRepository,
    private val flockRepo: FlockRepository
) : AssistantAction<AddEggsParams> {

    override val intent: IntentId = IntentId.ADD_EGGS

    override fun requiredSlots(): List<SlotId> = listOf(SlotId.COUNT)

    override fun fromSlots(slots: Map<SlotId, SlotValue>): AddEggsParams {
        val allocations = (slots[SlotId.FLOCK_ALLOCATIONS]?.typedValue as? List<*>)
            ?.filterIsInstance<FlockAllocation>()
            ?: emptyList()
        val count = if (allocations.isNotEmpty()) {
            allocations.sumOf { it.quantity.toInt() }
        } else {
            (slots[SlotId.COUNT]?.typedValue as? Number)?.toInt() ?: 0
        }
        val broken = (slots[SlotId.BROKEN_COUNT]?.typedValue as? Number)?.toInt() ?: 0
        val date = (slots[SlotId.DATE]?.typedValue as? LocalDate) ?: LocalDate.now()
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
            ?: allocations.singleOrNull()?.flockId
        val flockName = (slots[SlotId.FLOCK_NAME]?.typedValue as? String)
            ?: allocations.singleOrNull()?.flockName
        val note = slots[SlotId.NOTE]?.typedValue as? String

        return AddEggsParams(
            count = count,
            brokenCount = broken,
            date = date,
            flockId = flockId,
            flockName = flockName,
            allocations = allocations,
            note = note
        )
    }

    private fun resolveTargets(params: AddEggsParams, flocks: List<Flock>): List<Pair<Flock, Int>>? {
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

    override suspend fun validate(params: AddEggsParams): ValidationResult {
        val flocks = flockRepo.getAllFlocks()
        if (flocks.isEmpty()) {
            return ValidationResult.Rejected(
                "هنوز هیچ گله‌ای در سیستم ثبت نشده است. ابتدا یک گله ایجاد کنید.",
                "No flocks are registered yet."
            )
        }
        if (params.allocations.isEmpty() && params.flockId == null && flocks.size > 1) {
            return ValidationResult.Rejected(
                "شما چند گله دارید؛ لطفاً ابتدا گله یا گله‌های مورد نظر را انتخاب کنید.",
                "Multiple flocks exist; please select target flock(s) first."
            )
        }
        val targets = resolveTargets(params, flocks)
            ?: return ValidationResult.Rejected(
                "گلهٔ انتخاب‌شده یافت نشد.",
                "Selected flock could not be found."
            )

        val warnings = mutableListOf<String>()
        for ((flock, eggCount) in targets) {
            when (val safety = SafetyChecks.validateEggCount(eggCount, flock.effectiveBirdCount)) {
                is SafetyResult.Rejected -> {
                    return ValidationResult.Rejected(
                        "${flock.name}: ${safety.reasonMessage}",
                        "${flock.name}: ${safety.reasonMessage}"
                    )
                }
                is SafetyResult.Warning -> {
                    warnings.add("${flock.name}: ${safety.warningMessage}")
                }
                is SafetyResult.Passed -> Unit
            }
        }
        if (warnings.isNotEmpty()) {
            val joined = warnings.joinToString("\n")
            return ValidationResult.Warning(joined, joined)
        }
        return ValidationResult.Valid
    }

    override fun preview(params: AddEggsParams): ActionCardPreview {
        val jalaliStr = DateParser.formatToStandardJalali(params.date)
        val fields = mutableListOf<Pair<String, String>>()
        if (params.allocations.size > 1) {
            fields.add("مجموع تخم‌مرغ" to "${toPersianDigits(params.count)} عدد")
            fields.add("تاریخ" to toPersianDigits(jalaliStr))
            params.allocations.forEach { alloc ->
                fields.add(alloc.flockName to "${toPersianDigits(alloc.quantity.toInt())} عدد")
            }
        } else {
            fields.add("تعداد تخم‌مرغ" to "${toPersianDigits(params.count)} عدد")
            fields.add("تاریخ" to toPersianDigits(jalaliStr))
            if (params.brokenCount > 0) {
                fields.add("تخم‌مرغ شکسته" to "${toPersianDigits(params.brokenCount)} عدد")
            }
            val displayFlock = params.allocations.firstOrNull()?.flockName ?: params.flockName
            if (displayFlock != null) {
                fields.add("سالن / گله" to displayFlock)
            }
        }
        return ActionCardPreview(
            titleFa = "ثبت تولید تخم‌مرغ",
            titleEn = "Record Egg Collection",
            fields = fields
        )
    }

    override suspend fun execute(params: AddEggsParams): ActionResult {
        val flocks = flockRepo.getAllFlocks()
        if (flocks.isEmpty()) {
            return ActionResult(
                success = false,
                messageFa = "هنوز هیچ گله‌ای ثبت نشده است.",
                messageEn = "No flocks found."
            )
        }
        val targets = resolveTargets(params, flocks)
            ?: return ActionResult(
                success = false,
                messageFa = "لطفاً گله یا گله‌های مورد نظر را انتخاب کنید.",
                messageEn = "Target flock(s) must be selected."
            )

        val jalaliStr = DateParser.formatToStandardJalali(params.date)
        val baseEpochMillis = DateParser.toEpochMillis(params.date)
        val createdIds = mutableListOf<Long>()
        val summaryLinesFa = mutableListOf<String>()
        val summaryLinesEn = mutableListOf<String>()

        targets.forEachIndexed { index, (targetFlock, eggCount) ->
            val broken = if (targets.size == 1) params.brokenCount else 0
            val total = eggCount + broken
            val birdCount = targetFlock.effectiveBirdCount
            val rate = if (birdCount > 0) (total.toDouble() / birdCount.toDouble()) * 100.0 else 0.0

            val record = EggProduction(
                flockId = targetFlock.id,
                flockName = targetFlock.name,
                healthyCount = eggCount,
                brokenCount = broken,
                layingRatePercent = rate,
                dateJalaliStr = jalaliStr,
                timestampMillis = baseEpochMillis + index,
                notes = params.note ?: "ثبت شده با دستیار هوشمند"
            )

            val id = eggRepo.insertEggProduction(record)
            if (id > 0) {
                createdIds.add(id)
                summaryLinesFa.add("• ${targetFlock.name}: ${toPersianDigits(eggCount)} عدد")
                summaryLinesEn.add("${targetFlock.name}: $eggCount")
            }
        }

        if (createdIds.isEmpty()) {
            return ActionResult(
                success = false,
                messageFa = "خطا در ثبت اطلاعات تخم‌گذاری.",
                messageEn = "Failed to record egg production."
            )
        }

        val undoToken = "EGG_${createdIds.joinToString(",")}"
        return if (targets.size == 1) {
            val (onlyFlock, onlyCount) = targets[0]
            ActionResult(
                success = true,
                messageFa = "${toPersianDigits(onlyCount)} عدد تخم‌مرغ برای «${onlyFlock.name}» با موفقیت در تاریخ ${toPersianDigits(jalaliStr)} ثبت شد.",
                messageEn = "Recorded $onlyCount eggs for '${onlyFlock.name}'.",
                createdRecordId = createdIds.firstOrNull(),
                undoToken = undoToken
            )
        } else {
            val totalSum = targets.sumOf { it.second }
            ActionResult(
                success = true,
                messageFa = "تولید تخم‌مرغ در تاریخ ${toPersianDigits(jalaliStr)} ثبت شد:\n${summaryLinesFa.joinToString("\n")}\nمجموع کل: ${toPersianDigits(totalSum)} عدد",
                messageEn = "Recorded $totalSum eggs (${summaryLinesEn.joinToString(", ")}).",
                createdRecordId = createdIds.firstOrNull(),
                undoToken = undoToken
            )
        }
    }

    override suspend fun undo(undoToken: String): Boolean {
        if (!undoToken.startsWith("EGG_")) return false
        val ids = undoToken.removePrefix("EGG_")
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        if (ids.isEmpty()) return false
        var anyDeleted = false
        for (id in ids) {
            if (eggRepo.deleteEggProduction(id)) {
                anyDeleted = true
            }
        }
        return anyDeleted
    }
}
