package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.entities.DateParser
import com.example.poultryapp.assistant.core.nlu.FlockAllocation
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.local.BirdSaleLog
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.repository.BirdExitRepository
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.engine.toPersianDigits
import java.time.LocalDate
import java.util.Locale
import kotlin.math.round

data class SellBirdsParams(
    val flockId: Long?,
    val flockName: String?,
    val count: Int,
    val allocations: List<FlockAllocation> = emptyList(),
    val liveWeightKg: Double? = null,
    val totalRevenueTomans: Double = 0.0,
    val dateJalaliStr: String,
    val buyerName: String? = null,
    val note: String? = null
)

class SellBirdsAction(
    private val birdExitRepo: BirdExitRepository,
    private val flockRepo: FlockRepository
) : AssistantAction<SellBirdsParams> {

    override val intent: IntentId = IntentId.SELL_BIRDS

    override fun requiredSlots(): List<SlotId> = listOf(SlotId.COUNT)

    override fun fromSlots(slots: Map<SlotId, SlotValue>): SellBirdsParams {
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
        val totalRevenueTomans = (slots[SlotId.SALE_AMOUNT_TOMANS]?.typedValue as? Number)?.toDouble()
            ?.coerceAtLeast(0.0)
            ?: 0.0
        val parsedDate = slots[SlotId.DATE]?.typedValue as? LocalDate
        val dateStr = if (parsedDate != null) {
            DateParser.formatToStandardJalali(parsedDate)
        } else {
            JalaliDateHelper.getJalaliDate().toStandardString()
        }
        val note = slots[SlotId.NOTE]?.typedValue?.toString()

        return SellBirdsParams(
            flockId = flockId,
            flockName = flockName,
            count = count,
            allocations = allocations,
            liveWeightKg = liveWeightKg,
            totalRevenueTomans = totalRevenueTomans,
            dateJalaliStr = dateStr,
            note = note
        )
    }

    private fun resolveTargets(params: SellBirdsParams, flocks: List<Flock>): List<Pair<Flock, Int>>? {
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

    override suspend fun validate(params: SellBirdsParams): ValidationResult {
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
        if (params.totalRevenueTomans < 0.0) {
            return ValidationResult.Rejected(
                "مبلغ فروش نمی‌تواند منفی باشد.",
                "Sale revenue cannot be negative."
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
                    "تعداد فروش برای «${flock.name}» باید بزرگتر از صفر باشد.",
                    "Sold bird count must be greater than zero."
                )
            }
            if (count > flock.effectiveBirdCount) {
                return ValidationResult.Rejected(
                    "تعداد فروش (${toPersianDigits(count)} قطعه) از موجودی زنده «${flock.name}» (${toPersianDigits(flock.effectiveBirdCount)} قطعه) بیشتر است.",
                    "Sold count ($count) exceeds live bird count of ${flock.name} (${flock.effectiveBirdCount})."
                )
            }
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
                "دوره پرهیز دارویی در $detailsFa هنوز فعال است. فروش پرنده در دوره پرهیز توصیه نمی‌شود.",
                "Medication withdrawal period is active for ${withdrawalFlocks.joinToString { it.name }}."
            )
        }

        return ValidationResult.Valid
    }

    override fun preview(params: SellBirdsParams): ActionCardPreview {
        val fields = mutableListOf<Pair<String, String>>()
        if (params.allocations.size > 1) {
            fields.add("مجموع تعداد فروش" to "${toPersianDigits(params.count)} قطعه")
            params.allocations.forEach { alloc ->
                fields.add(alloc.flockName to "${toPersianDigits(alloc.quantity.toInt())} قطعه")
            }
        } else {
            fields.add("تعداد فروش" to "${toPersianDigits(params.count)} قطعه")
            val displayFlock = params.allocations.firstOrNull()?.flockName ?: params.flockName
            if (displayFlock != null) {
                fields.add("گله / سالن" to displayFlock)
            }
        }
        if (params.liveWeightKg != null && params.liveWeightKg > 0.0) {
            fields.add("وزن زنده" to "${toPersianDigits(formatKg(params.liveWeightKg))} کیلوگرم")
        }
        if (params.totalRevenueTomans > 0.0) {
            fields.add(
                "مبلغ کل فروش" to "${toPersianDigits(String.format(Locale.US, "%,.0f", params.totalRevenueTomans))} تومان"
            )
            fields.add("ثبت خودکار درآمد" to "بله (سرفصل فروش پرنده)")
        } else {
            fields.add("وضعیت مالی" to "ثبت فروش بدون مبلغ")
        }
        fields.add("تاریخ" to toPersianDigits(params.dateJalaliStr))

        return ActionCardPreview(
            titleFa = "ثبت فروش پرنده",
            titleEn = "Log Bird Sale",
            fields = fields,
            isExpense = if (params.totalRevenueTomans > 0.0) false else null
        )
    }

    override suspend fun execute(params: SellBirdsParams): ActionResult {
        val flocks = flockRepo.getAllFlocks()
        val targets = resolveTargets(params, flocks)
            ?: return ActionResult(
                success = false,
                messageFa = "لطفاً ابتدا گله یا گله‌های مورد نظر را انتخاب کنید.",
                messageEn = "Target flock(s) must be selected."
            )

        val totalBirds = targets.sumOf { it.second }.coerceAtLeast(1)
        var remainingWeight = params.liveWeightKg
        var remainingRevenue = params.totalRevenueTomans

        val createdIds = mutableListOf<Long>()
        val summaryLinesFa = mutableListOf<String>()
        val summaryLinesEn = mutableListOf<String>()

        targets.forEachIndexed { index, (targetFlock, count) ->
            val isLast = index == targets.lastIndex
            val ratio = count.toDouble() / totalBirds.toDouble()

            val partWeight: Double? = params.liveWeightKg?.let { totalW ->
                if (isLast) {
                    roundToTwoDecimals((remainingWeight ?: 0.0).coerceAtLeast(0.0))
                } else {
                    val split = roundToTwoDecimals(totalW * ratio)
                    remainingWeight = ((remainingWeight ?: totalW) - split).coerceAtLeast(0.0)
                    split
                }
            }

            val partRevenue: Double = if (params.totalRevenueTomans <= 0.0) {
                0.0
            } else if (isLast) {
                remainingRevenue.coerceAtLeast(0.0)
            } else {
                val split = round(params.totalRevenueTomans * ratio).coerceAtMost(remainingRevenue)
                remainingRevenue = (remainingRevenue - split).coerceAtLeast(0.0)
                split
            }

            val unitPrice: Double? = if (partRevenue > 0.0) {
                if (partWeight != null && partWeight > 0.0) {
                    partRevenue / partWeight
                } else {
                    partRevenue / count.toDouble()
                }
            } else null

            val log = BirdSaleLog(
                flockId = targetFlock.id,
                flockName = targetFlock.name,
                birdCount = count,
                birdAgeDays = targetFlock.ageDays.coerceAtLeast(1),
                liveWeightKg = partWeight,
                unitPriceTomans = unitPrice,
                totalRevenueTomans = partRevenue,
                buyerName = params.buyerName,
                dateJalaliStr = params.dateJalaliStr,
                notes = params.note ?: "ثبت فروش با دستیار هوشمند"
            )

            val saleId = birdExitRepo.insertBirdSaleLog(
                log = log,
                createFinanceTransaction = partRevenue > 0.0
            )
            if (saleId > 0L) {
                createdIds.add(saleId)
                summaryLinesFa.add("• ${targetFlock.name}: ${toPersianDigits(count)} قطعه")
                summaryLinesEn.add("${targetFlock.name}: $count")
            }
        }

        if (createdIds.isEmpty()) {
            return ActionResult(
                success = false,
                messageFa = "خطا در ثبت فروش پرنده (ممکن است موجودی زنده گله کافی نباشد).",
                messageEn = "Failed to record bird sale."
            )
        }

        val undoToken = "BIRDSALE:${createdIds.joinToString(",")}"
        val extraPartsFa = buildList {
            if (params.liveWeightKg != null && params.liveWeightKg > 0.0) {
                add("وزن زنده ${toPersianDigits(formatKg(params.liveWeightKg))} کیلوگرم")
            }
            if (params.totalRevenueTomans > 0.0) {
                add("درآمد ${toPersianDigits(String.format(Locale.US, "%,.0f", params.totalRevenueTomans))} تومان در امور مالی ثبت شد")
            }
        }
        val extraSuffixFa = if (extraPartsFa.isNotEmpty()) " (${extraPartsFa.joinToString(" • ")})" else ""

        return if (targets.size == 1) {
            val (onlyFlock, onlyCount) = targets[0]
            ActionResult(
                success = true,
                messageFa = "فروش ${toPersianDigits(onlyCount)} قطعه از «${onlyFlock.name}»$extraSuffixFa با موفقیت ثبت و از موجودی گله کسر شد.",
                messageEn = "Sold $onlyCount birds from ${onlyFlock.name}.",
                createdRecordId = createdIds.firstOrNull(),
                undoToken = undoToken
            )
        } else {
            val totalCount = targets.sumOf { it.second }
            ActionResult(
                success = true,
                messageFa = "فروش پرنده ثبت شد:\n${summaryLinesFa.joinToString("\n")}\nمجموع کل: ${toPersianDigits(totalCount)} قطعه$extraSuffixFa",
                messageEn = "Sold $totalCount birds (${summaryLinesEn.joinToString(", ")}).",
                createdRecordId = createdIds.firstOrNull(),
                undoToken = undoToken
            )
        }
    }

    override suspend fun undo(undoToken: String): Boolean {
        if (!undoToken.startsWith("BIRDSALE:")) return false
        val ids = undoToken.removePrefix("BIRDSALE:")
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        if (ids.isEmpty()) return false
        var anyDeleted = false
        for (id in ids) {
            if (birdExitRepo.deleteBirdSaleLog(id)) {
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

