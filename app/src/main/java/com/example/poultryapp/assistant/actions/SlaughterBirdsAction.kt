package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.entities.DateParser
import com.example.poultryapp.assistant.core.nlu.FlockAllocation
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.local.BirdSlaughterLog
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.local.FlockExitAnalytics
import com.example.poultryapp.data.repository.BirdExitRepository
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.engine.toPersianDigits
import java.time.LocalDate
import java.util.Locale
import kotlin.math.round

data class SlaughterBirdsParams(
    val flockId: Long?,
    val flockName: String?,
    val count: Int,
    val allocations: List<FlockAllocation> = emptyList(),
    val liveWeightKg: Double? = null,
    val carcassWeightKg: Double? = null,
    val dateJalaliStr: String,
    val note: String? = null
)

class SlaughterBirdsAction(
    private val birdExitRepo: BirdExitRepository,
    private val flockRepo: FlockRepository
) : AssistantAction<SlaughterBirdsParams> {

    override val intent: IntentId = IntentId.SLAUGHTER_BIRDS

    override fun requiredSlots(): List<SlotId> = listOf(SlotId.COUNT)

    override fun fromSlots(slots: Map<SlotId, SlotValue>): SlaughterBirdsParams {
        val allocations = (slots[SlotId.FLOCK_ALLOCATIONS]?.typedValue as? List<*>)
            ?.filterIsInstance<FlockAllocation>()
            ?: emptyList()
        val count = if (allocations.isNotEmpty()) {
            allocations.sumOf { it.quantity.toInt() }
        } else {
            (slots[SlotId.COUNT]?.typedValue as? Number)?.toInt() ?: 0
        }
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
            ?: allocations.singleOrNull()?.flockId
        val flockName = slots[SlotId.FLOCK_NAME]?.typedValue?.toString()
            ?: allocations.singleOrNull()?.flockName
        val liveWeightKg = (slots[SlotId.LIVE_WEIGHT_KG]?.typedValue as? Number)?.toDouble()
            ?.takeIf { it > 0.0 }
        val carcassWeightKg = (slots[SlotId.CARCASS_WEIGHT_KG]?.typedValue as? Number)?.toDouble()
            ?.takeIf { it > 0.0 }
        val parsedDate = slots[SlotId.DATE]?.typedValue as? LocalDate
        val dateStr = if (parsedDate != null) {
            DateParser.formatToStandardJalali(parsedDate)
        } else {
            JalaliDateHelper.getJalaliDate().toStandardString()
        }
        val note = slots[SlotId.NOTE]?.typedValue?.toString()

        return SlaughterBirdsParams(
            flockId = flockId,
            flockName = flockName,
            count = count,
            allocations = allocations,
            liveWeightKg = liveWeightKg,
            carcassWeightKg = carcassWeightKg,
            dateJalaliStr = dateStr,
            note = note
        )
    }

    private fun resolveTargets(params: SlaughterBirdsParams, flocks: List<Flock>): List<Pair<Flock, Int>>? {
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

    override suspend fun validate(params: SlaughterBirdsParams): ValidationResult {
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
                    "تعداد کشتار برای «${flock.name}» باید بزرگتر از صفر باشد.",
                    "Slaughtered bird count must be greater than zero."
                )
            }
            if (count > flock.effectiveBirdCount) {
                return ValidationResult.Rejected(
                    "تعداد کشتار (${toPersianDigits(count)} قطعه) از موجودی زنده «${flock.name}» (${toPersianDigits(flock.effectiveBirdCount)} قطعه) بیشتر است.",
                    "Slaughtered count ($count) exceeds live bird count of ${flock.name} (${flock.effectiveBirdCount})."
                )
            }
        }

        if (params.liveWeightKg != null && params.carcassWeightKg != null && params.carcassWeightKg > params.liveWeightKg) {
            return ValidationResult.Warning(
                "وزن لاشه (${toPersianDigits(formatKg(params.carcassWeightKg))} کیلوگرم) از وزن زنده (${toPersianDigits(formatKg(params.liveWeightKg))} کیلوگرم) بیشتر است. آیا اطلاعات صحیح است؟",
                "Carcass weight exceeds live weight. Confirm entry?"
            )
        }

        val withdrawalFlocks = targets.map { it.first }.filter { it.isWithdrawalActive }
        if (withdrawalFlocks.isNotEmpty()) {
            val detailsFa = withdrawalFlocks.joinToString("، ") { f ->
                val med = f.activeMedicationName?.takeIf { it.isNotBlank() } ?: "دارو"
                val remText = if (f.withdrawalRemainingDays > 0) {
                    "${toPersianDigits(f.withdrawalRemainingDays)} روز"
                } else {
                    "${toPersianDigits(f.withdrawalRemainingHours.coerceAtLeast(1))} ساعت"
                }
                "«${f.name}» ($med - $remText باقی‌مانده)"
            }
            return ValidationResult.Warning(
                "دوره پرهیز دارویی در $detailsFa هنوز فعال است. کشتار پیش از پایان دوره پرهیز توصیه نمی‌شود.",
                "Medication withdrawal period is active for ${withdrawalFlocks.joinToString { it.name }}."
            )
        }

        return ValidationResult.Valid
    }

    override fun preview(params: SlaughterBirdsParams): ActionCardPreview {
        val fields = mutableListOf<Pair<String, String>>()
        if (params.allocations.size > 1) {
            fields.add("مجموع کشتار" to "${toPersianDigits(params.count)} قطعه")
            params.allocations.forEach { alloc ->
                fields.add(alloc.flockName to "${toPersianDigits(alloc.quantity.toInt())} قطعه")
            }
        } else {
            fields.add("تعداد کشتار" to "${toPersianDigits(params.count)} قطعه")
            val displayFlock = params.allocations.firstOrNull()?.flockName ?: params.flockName
            if (displayFlock != null) {
                fields.add("گله / سالن" to displayFlock)
            }
        }
        if (params.liveWeightKg != null && params.liveWeightKg > 0.0) {
            fields.add("وزن زنده" to "${toPersianDigits(formatKg(params.liveWeightKg))} کیلوگرم")
        }
        if (params.carcassWeightKg != null && params.carcassWeightKg > 0.0) {
            fields.add("وزن لاشه" to "${toPersianDigits(formatKg(params.carcassWeightKg))} کیلوگرم")
        }
        val yieldPct = FlockExitAnalytics.calculateCarcassYieldPercent(params.liveWeightKg, params.carcassWeightKg)
        if (yieldPct != null) {
            fields.add("راندمان لاشه" to "${toPersianDigits(String.format(Locale.US, "%.1f", yieldPct))}٪")
        }
        fields.add("تاریخ کشتار" to toPersianDigits(params.dateJalaliStr))

        return ActionCardPreview(
            titleFa = "ثبت کشتار پرنده",
            titleEn = "Log Bird Slaughter",
            fields = fields,
            isExpense = null
        )
    }

    override suspend fun execute(params: SlaughterBirdsParams): ActionResult {
        val flocks = flockRepo.getAllFlocks()
        val targets = resolveTargets(params, flocks)
            ?: return ActionResult(
                success = false,
                messageFa = "لطفاً ابتدا گله یا گله‌های مورد نظر را انتخاب کنید.",
                messageEn = "Target flock(s) must be selected."
            )

        val totalBirds = targets.sumOf { it.second }.coerceAtLeast(1)
        var remainingLive = params.liveWeightKg
        var remainingCarcass = params.carcassWeightKg

        val createdIds = mutableListOf<Long>()
        val summaryLinesFa = mutableListOf<String>()
        val summaryLinesEn = mutableListOf<String>()

        targets.forEachIndexed { index, (targetFlock, count) ->
            val isLast = index == targets.lastIndex
            val ratio = count.toDouble() / totalBirds.toDouble()

            val partLive: Double? = params.liveWeightKg?.let { totalL ->
                if (isLast) {
                    roundToTwoDecimals((remainingLive ?: 0.0).coerceAtLeast(0.0))
                } else {
                    val split = roundToTwoDecimals(totalL * ratio)
                    remainingLive = ((remainingLive ?: totalL) - split).coerceAtLeast(0.0)
                    split
                }
            }

            val partCarcass: Double? = params.carcassWeightKg?.let { totalC ->
                if (isLast) {
                    roundToTwoDecimals((remainingCarcass ?: 0.0).coerceAtLeast(0.0))
                } else {
                    val split = roundToTwoDecimals(totalC * ratio)
                    remainingCarcass = ((remainingCarcass ?: totalC) - split).coerceAtLeast(0.0)
                    split
                }
            }

            val ageDays = targetFlock.ageDays.coerceAtLeast(1)
            val log = BirdSlaughterLog(
                flockId = targetFlock.id,
                flockName = targetFlock.name,
                birdCount = count,
                birdAgeDays = ageDays,
                liveWeightKg = partLive,
                carcassWeightKg = partCarcass,
                dateJalaliStr = params.dateJalaliStr,
                notes = params.note ?: "ثبت کشتار با دستیار هوشمند"
            )

            val slaughterId = birdExitRepo.insertBirdSlaughterLog(log)
            if (slaughterId > 0L) {
                createdIds.add(slaughterId)
                summaryLinesFa.add("• ${targetFlock.name} (سن ${toPersianDigits(ageDays)} روز): ${toPersianDigits(count)} قطعه")
                summaryLinesEn.add("${targetFlock.name}: $count (age ${ageDays}d)")
            }
        }

        if (createdIds.isEmpty()) {
            return ActionResult(
                success = false,
                messageFa = "خطا در ثبت کشتار پرنده (ممکن است موجودی زنده گله کافی نباشد).",
                messageEn = "Failed to record bird slaughter."
            )
        }

        val undoToken = "SLAUGHTER:${createdIds.joinToString(",")}"
        val yieldPct = FlockExitAnalytics.calculateCarcassYieldPercent(params.liveWeightKg, params.carcassWeightKg)
        val detailsListFa = buildList {
            if (params.liveWeightKg != null && params.liveWeightKg > 0.0) {
                add("وزن زنده ${toPersianDigits(formatKg(params.liveWeightKg))} کیلوگرم")
            }
            if (params.carcassWeightKg != null && params.carcassWeightKg > 0.0) {
                add("وزن لاشه ${toPersianDigits(formatKg(params.carcassWeightKg))} کیلوگرم")
            }
            if (yieldPct != null) {
                add("راندمان ${toPersianDigits(String.format(Locale.US, "%.1f", yieldPct))}٪")
            }
        }

        return if (targets.size == 1) {
            val (onlyFlock, onlyCount) = targets[0]
            val agePart = "سن ${toPersianDigits(onlyFlock.ageDays.coerceAtLeast(1))} روز"
            val allMeta = (listOf(agePart) + detailsListFa).joinToString(" • ")
            ActionResult(
                success = true,
                messageFa = "کشتار ${toPersianDigits(onlyCount)} قطعه از «${onlyFlock.name}» ($allMeta) ثبت و از موجودی زنده کسر شد.",
                messageEn = "Recorded slaughter of $onlyCount birds from ${onlyFlock.name}.",
                createdRecordId = createdIds.firstOrNull(),
                undoToken = undoToken
            )
        } else {
            val totalCount = targets.sumOf { it.second }
            val extraSuffixFa = if (detailsListFa.isNotEmpty()) " (${detailsListFa.joinToString(" • ")})" else ""
            ActionResult(
                success = true,
                messageFa = "کشتار پرنده ثبت شد:\n${summaryLinesFa.joinToString("\n")}\nمجموع کل: ${toPersianDigits(totalCount)} قطعه$extraSuffixFa",
                messageEn = "Recorded slaughter of $totalCount birds (${summaryLinesEn.joinToString(", ")}).",
                createdRecordId = createdIds.firstOrNull(),
                undoToken = undoToken
            )
        }
    }

    override suspend fun undo(undoToken: String): Boolean {
        if (!undoToken.startsWith("SLAUGHTER:")) return false
        val ids = undoToken.removePrefix("SLAUGHTER:")
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        if (ids.isEmpty()) return false
        var anyDeleted = false
        for (id in ids) {
            if (birdExitRepo.deleteBirdSlaughterLog(id)) {
                anyDeleted = true
            }
        }
        return anyDeleted
    }

    private fun roundToTwoDecimals(v: Double): Double = round(v * 100.0) / 100.0

    private fun formatKg(kg: Double): String {
        return if (kg % 1.0 == 0.0) {
            kg.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", kg).trimEnd('0').trimEnd('.')
        }
    }
}

