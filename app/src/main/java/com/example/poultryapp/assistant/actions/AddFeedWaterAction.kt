package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.entities.DateParser
import com.example.poultryapp.assistant.core.nlu.FlockAllocation
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.local.Flock
import com.example.poultryapp.data.repository.FeedWaterRepository
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.engine.ChargeItemType
import com.example.poultryapp.engine.DailyNutritionEngine
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.engine.toPersianDigits
import java.time.LocalDate

data class AddFeedWaterParams(
    val flockId: Long?,
    val flockName: String?,
    val itemType: ChargeItemType,
    val amount: Double, // Stored in grams / ml for database
    val allocations: List<FlockAllocation> = emptyList(), // Quantities in kg / liters (user units)
    val dateJalaliStr: String,
    val timestampMillis: Long,
    val notes: String?
)

class AddFeedWaterAction(
    private val feedWaterRepo: FeedWaterRepository,
    private val flockRepo: FlockRepository
) : AssistantAction<AddFeedWaterParams> {

    override val intent: IntentId = IntentId.ADD_FEED_WATER

    override fun requiredSlots(): List<SlotId> = listOf(SlotId.COUNT)

    private fun toDatabaseUnits(userAmountKgOrLiters: Double): Double {
        // Assistant inputs are in kilograms (feed) or liters (water).
        // Convert to grams / milliliters for the database and live consumption tracker.
        return if (userAmountKgOrLiters in 0.001..10000.0) {
            userAmountKgOrLiters * 1000.0
        } else {
            userAmountKgOrLiters
        }
    }

    override fun fromSlots(slots: Map<SlotId, SlotValue>): AddFeedWaterParams {
        val allocations = (slots[SlotId.FLOCK_ALLOCATIONS]?.typedValue as? List<*>)
            ?.filterIsInstance<FlockAllocation>()
            ?: emptyList()
        val amountVal = if (allocations.isNotEmpty()) {
            allocations.sumOf { it.quantity }
        } else {
            (slots[SlotId.AMOUNT]?.typedValue as? Number)?.toDouble()
                ?: (slots[SlotId.COUNT]?.typedValue as? Number)?.toDouble()
                ?: 0.0
        }
        val effectiveAmount = toDatabaseUnits(amountVal)

        val typeStr = slots[SlotId.FEED_WATER_TYPE]?.typedValue?.toString() ?: "FEED"
        val itemType = if (typeStr == "WATER") ChargeItemType.WATER else ChargeItemType.FEED
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
            ?: allocations.singleOrNull()?.flockId
        val flockName = slots[SlotId.FLOCK_NAME]?.typedValue?.toString()
            ?: allocations.singleOrNull()?.flockName
        val dateVal = slots[SlotId.DATE]?.typedValue as? LocalDate
        val dateStr = if (dateVal != null) {
            DateParser.formatToStandardJalali(dateVal)
        } else {
            JalaliDateHelper.getJalaliDate().toStandardString()
        }
        val timestamp = if (dateVal != null) {
            DateParser.toEpochMillis(dateVal)
        } else {
            System.currentTimeMillis()
        }
        val note = slots[SlotId.NOTE]?.typedValue?.toString()

        return AddFeedWaterParams(
            flockId = flockId,
            flockName = flockName,
            itemType = itemType,
            amount = effectiveAmount,
            allocations = allocations,
            dateJalaliStr = dateStr,
            timestampMillis = timestamp,
            notes = note
        )
    }

    private fun resolveTargets(params: AddFeedWaterParams, flocks: List<Flock>): List<Pair<Flock, Double>>? {
        if (flocks.isEmpty()) return null
        return when {
            params.allocations.isNotEmpty() -> {
                val resolved = mutableListOf<Pair<Flock, Double>>()
                for (alloc in params.allocations) {
                    val flock = flocks.firstOrNull { it.id == alloc.flockId } ?: return null
                    resolved.add(flock to toDatabaseUnits(alloc.quantity))
                }
                resolved
            }
            params.flockId != null -> {
                val flock = flocks.firstOrNull { it.id == params.flockId } ?: return null
                listOf(flock to params.amount)
            }
            flocks.size == 1 -> {
                listOf(flocks[0] to params.amount)
            }
            else -> null
        }
    }

    override suspend fun validate(params: AddFeedWaterParams): ValidationResult {
        val flocks = flockRepo.getAllFlocks()
        if (flocks.isEmpty()) {
            return ValidationResult.Rejected(
                "هنوز هیچ گله‌ای ثبت نشده است. ابتدا یک گله ایجاد کنید.",
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

        for ((flock, dbAmount) in targets) {
            if (dbAmount <= 0.0) {
                return ValidationResult.Rejected(
                    "مقدار شارژ برای «${flock.name}» باید بزرگتر از صفر باشد.",
                    "Charge amount must be greater than zero."
                )
            }
        }
        // Warning threshold: 10,000 kg (10,000,000 grams) per flock
        val hasExtremeAmount = targets.any { (_, dbAmount) -> dbAmount > 10_000_000.0 }
        if (hasExtremeAmount) {
            return ValidationResult.Warning(
                "مقدار شارژ بسیار زیاد است. آیا مطمئن هستید؟",
                "Charge amount is unusually high. Are you sure?"
            )
        }
        return ValidationResult.Valid
    }

    private fun formatAmount(itemType: ChargeItemType, dbAmount: Double): String {
        return if (itemType == ChargeItemType.WATER) {
            DailyNutritionEngine.formatWater(dbAmount)
        } else {
            DailyNutritionEngine.formatFeed(dbAmount)
        }
    }

    override fun preview(params: AddFeedWaterParams): ActionCardPreview {
        val typeTitle = if (params.itemType == ChargeItemType.WATER) "شارژ آب" else "شارژ دان"
        val fields = mutableListOf<Pair<String, String>>()
        fields.add("نوع" to typeTitle)
        if (params.allocations.size > 1) {
            fields.add("مجموع" to formatAmount(params.itemType, params.amount))
            params.allocations.forEach { alloc ->
                fields.add(alloc.flockName to formatAmount(params.itemType, toDatabaseUnits(alloc.quantity)))
            }
        } else {
            fields.add("مقدار" to formatAmount(params.itemType, params.amount))
            val displayFlock = params.allocations.firstOrNull()?.flockName ?: params.flockName
            if (displayFlock != null) {
                fields.add("سالن" to displayFlock)
            }
        }
        fields.add("تاریخ" to toPersianDigits(params.dateJalaliStr))
        return ActionCardPreview(
            titleFa = typeTitle,
            titleEn = "Feed/Water Charge",
            fields = fields
        )
    }

    override suspend fun execute(params: AddFeedWaterParams): ActionResult {
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

        targets.forEachIndexed { index, (targetFlock, dbAmount) ->
            val chargeId = feedWaterRepo.logCharge(
                flockId = targetFlock.id,
                itemType = params.itemType,
                amount = dbAmount,
                notes = params.notes ?: "ثبت شده با دستیار هوشمند",
                dateJalaliStr = params.dateJalaliStr,
                timestampMillis = params.timestampMillis + index
            )
            if (chargeId > 0) {
                createdIds.add(chargeId)
                val formatted = formatAmount(params.itemType, dbAmount)
                summaryLinesFa.add("• ${targetFlock.name}: $formatted")
                summaryLinesEn.add("${targetFlock.name}: $formatted")
            }
        }

        if (createdIds.isEmpty()) {
            return ActionResult(
                success = false,
                messageFa = "خطا در ثبت شارژ دان/آب.",
                messageEn = "Failed to log feed/water charge."
            )
        }

        val undoToken = "CHARGE:${createdIds.joinToString(",")}"
        return if (targets.size == 1) {
            val (onlyFlock, onlyAmount) = targets[0]
            val formatted = formatAmount(params.itemType, onlyAmount)
            ActionResult(
                success = true,
                messageFa = "شارژ $formatted برای «${onlyFlock.name}» ثبت شد.",
                messageEn = "Logged $formatted for ${onlyFlock.name}.",
                createdRecordId = createdIds.firstOrNull(),
                undoToken = undoToken
            )
        } else {
            val totalFormatted = formatAmount(params.itemType, targets.sumOf { it.second })
            val typeTitle = if (params.itemType == ChargeItemType.WATER) "شارژ آب" else "شارژ دان"
            ActionResult(
                success = true,
                messageFa = "$typeTitle با موفقیت ثبت شد:\n${summaryLinesFa.joinToString("\n")}\nمجموع کل: $totalFormatted",
                messageEn = "Logged $typeTitle (${summaryLinesEn.joinToString(", ")}).",
                createdRecordId = createdIds.firstOrNull(),
                undoToken = undoToken
            )
        }
    }

    override suspend fun undo(undoToken: String): Boolean {
        if (!undoToken.startsWith("CHARGE:")) return false
        val ids = undoToken.removePrefix("CHARGE:")
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        if (ids.isEmpty()) return false
        var anyDeleted = false
        for (id in ids) {
            if (feedWaterRepo.deleteCharge(id)) {
                anyDeleted = true
            }
        }
        return anyDeleted
    }
}
