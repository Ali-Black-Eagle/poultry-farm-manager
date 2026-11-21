package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.engine.toPersianDigits

data class TransferBirdsParams(
    val fromFlockId: Long?,
    val fromFlockName: String?,
    val toFlockId: Long?,
    val toFlockName: String?,
    val count: Int,
    val dateJalaliStr: String,
    val notes: String?
)

class TransferBirdsAction(
    private val flockRepo: FlockRepository
) : AssistantAction<TransferBirdsParams> {

    override val intent: IntentId = IntentId.TRANSFER_BIRDS

    override fun requiredSlots(): List<SlotId> = listOf(SlotId.COUNT)

    override fun fromSlots(slots: Map<SlotId, SlotValue>): TransferBirdsParams {
        val count = (slots[SlotId.COUNT]?.typedValue as? Number)?.toInt() ?: 1
        val fromId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
        val fromName = slots[SlotId.FLOCK_NAME]?.typedValue?.toString()
        val toId = (slots[SlotId.TARGET_FLOCK_ID]?.typedValue as? Number)?.toLong()
        val toName = slots[SlotId.TARGET_FLOCK_NAME]?.typedValue?.toString()
        val dateStr = slots[SlotId.DATE]?.rawSpan ?: JalaliDateHelper.getJalaliDate().toStandardString()
        val notes = slots[SlotId.NOTE]?.typedValue?.toString()

        return TransferBirdsParams(fromId, fromName, toId, toName, count, dateStr, notes)
    }

    override suspend fun validate(params: TransferBirdsParams): ValidationResult {
        if (params.count <= 0) {
            return ValidationResult.Rejected("تعداد پرندگان انتقالی باید بزرگتر از صفر باشد.", "Count must be greater than zero.")
        }
        val all = flockRepo.getAllFlocks()
        if (all.size < 2) {
            return ValidationResult.Rejected("برای انتقال پرنده، حداقل باید ۲ سالن فعال در فارم وجود داشته باشد.", "At least 2 flocks required for transfer.")
        }
        return ValidationResult.Valid
    }

    override fun preview(params: TransferBirdsParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "انتقال پرندگان بین سالن‌ها",
            titleEn = "Transfer Birds",
            fields = listOf(
                "تعداد" to "${toPersianDigits(params.count)} قطعه",
                "مبدأ" to (params.fromFlockName ?: "سالن مبدأ"),
                "مقصد" to (params.toFlockName ?: "سالن مقصد"),
                "تاریخ" to toPersianDigits(params.dateJalaliStr)
            )
        )
    }

    override suspend fun execute(params: TransferBirdsParams): ActionResult {
        val allFlocks = flockRepo.getAllFlocks()
        val fromFlock = params.fromFlockId?.let { id -> allFlocks.find { it.id == id } } ?: allFlocks.firstOrNull()
        val toFlock = params.toFlockId?.let { id -> allFlocks.find { it.id == id } } ?: allFlocks.getOrNull(1)

        if (fromFlock == null || toFlock == null || fromFlock.id == toFlock.id) {
            return ActionResult(
                success = false,
                messageFa = "سالن مبدأ و مقصد نامعتبر هستند.",
                messageEn = "Invalid source and destination flocks."
            )
        }

        if (fromFlock.effectiveBirdCount < params.count) {
            return ActionResult(
                success = false,
                messageFa = "موجودی پرنده سالن ${fromFlock.name} (${toPersianDigits(fromFlock.effectiveBirdCount)}) کمتر از تعداد درخواستی (${toPersianDigits(params.count)}) است.",
                messageEn = "Insufficient birds in source flock."
            )
        }

        val success = flockRepo.transferBirds(
            fromFlockId = fromFlock.id,
            toFlockId = toFlock.id,
            birdCount = params.count,
            dateJalaliStr = params.dateJalaliStr,
            notes = params.notes ?: "انتقال با دستیار هوشمند"
        )

        return if (success) {
            ActionResult(
                success = true,
                messageFa = "تعداد ${toPersianDigits(params.count)} پرنده با موفقیت از ${fromFlock.name} به ${toFlock.name} منتقل شدند.",
                messageEn = "Transferred ${params.count} birds from ${fromFlock.name} to ${toFlock.name}.",
                undoToken = "TRANSFER:${toFlock.id}:${fromFlock.id}:${params.count}"
            )
        } else {
            ActionResult(
                success = false,
                messageFa = "خطا در انجام عملیات انتقال پرندگان.",
                messageEn = "Failed to transfer birds."
            )
        }
    }

    override suspend fun undo(undoToken: String): Boolean {
        if (!undoToken.startsWith("TRANSFER:")) return false
        val parts = undoToken.removePrefix("TRANSFER:").split(":")
        if (parts.size < 3) return false
        val returnFrom = parts[0].toLongOrNull() ?: return false
        val returnTo = parts[1].toLongOrNull() ?: return false
        val count = parts[2].toIntOrNull() ?: return false
        val todayStr = JalaliDateHelper.getJalaliDate().toStandardString()
        return flockRepo.transferBirds(returnFrom, returnTo, count, todayStr, "بازگردانی انتقال")
    }
}

