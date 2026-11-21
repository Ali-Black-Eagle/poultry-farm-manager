package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.repository.FinancialRepository
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.data.repository.WarehouseRepository
import com.example.poultryapp.engine.JalaliDateHelper
import com.example.poultryapp.engine.toPersianDigits

data class ConsumeWarehouseParams(
    val itemId: Long?,
    val itemName: String?,
    val amount: Double,
    val flockId: Long?,
    val flockName: String?,
    val dateJalaliStr: String
)

class ConsumeWarehouseAction(
    private val financialRepo: FinancialRepository,
    private val warehouseRepo: WarehouseRepository,
    private val flockRepo: FlockRepository
) : AssistantAction<ConsumeWarehouseParams> {

    override val intent: IntentId = IntentId.CONSUME_WAREHOUSE

    override fun requiredSlots(): List<SlotId> = listOf(SlotId.COUNT)

    override fun fromSlots(slots: Map<SlotId, SlotValue>): ConsumeWarehouseParams {
        val amount = (slots[SlotId.COUNT]?.typedValue as? Number)?.toDouble() ?: 0.0
        val itemId = (slots[SlotId.WAREHOUSE_ITEM_ID]?.typedValue as? Number)?.toLong()
        val itemName = slots[SlotId.WAREHOUSE_ITEM_NAME]?.typedValue?.toString()
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
        val flockName = slots[SlotId.FLOCK_NAME]?.typedValue?.toString()
        val dateStr = slots[SlotId.DATE]?.rawSpan ?: JalaliDateHelper.getJalaliDate().toStandardString()

        return ConsumeWarehouseParams(itemId, itemName, amount, flockId, flockName, dateStr)
    }

    override suspend fun validate(params: ConsumeWarehouseParams): ValidationResult {
        if (params.amount <= 0) {
            return ValidationResult.Rejected("مقدار مصرف باید بزرگتر از صفر باشد.", "Amount must be greater than zero.")
        }
        return ValidationResult.Valid
    }

    override fun preview(params: ConsumeWarehouseParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "مصرف از انبار",
            titleEn = "Consume from Warehouse",
            fields = listOf(
                "کالا" to (params.itemName ?: "کالای انبار"),
                "مقدار" to "${toPersianDigits(params.amount.toInt())}",
                "سالن مصرف‌کننده" to (params.flockName ?: "گله پیش‌فرض")
            )
        )
    }

    override suspend fun execute(params: ConsumeWarehouseParams): ActionResult {
        val items = warehouseRepo.getAllWarehouseItems()
        val item = if (params.itemId != null) {
            items.find { it.id == params.itemId }
        } else {
            items.firstOrNull()
        }

        if (item == null) {
            return ActionResult(
                success = false,
                messageFa = "کالای مورد نظر در انبار یافت نشد.",
                messageEn = "Warehouse item not found."
            )
        }

        if (item.currentStock < params.amount) {
            return ActionResult(
                success = false,
                messageFa = "موجودی کالای ${item.name} (${toPersianDigits(item.currentStock.toInt())} ${item.unit}) برای مصرف ${toPersianDigits(params.amount.toInt())} کافی نیست.",
                messageEn = "Insufficient stock in warehouse."
            )
        }

        val targetFlockId = params.flockId ?: flockRepo.getAllFlocks().firstOrNull()?.id
        val success = financialRepo.consumeWarehouseStock(
            itemId = item.id,
            amount = params.amount,
            flockId = targetFlockId,
            dateJalaliStr = params.dateJalaliStr,
            notes = "مصرف ثبت شده با دستیار هوشمند"
        )

        return if (success) {
            ActionResult(
                success = true,
                messageFa = "مقدار ${toPersianDigits(params.amount.toInt())} ${item.unit} از ${item.name} با موفقیت مصرف و از موجودی انبار کسر شد.",
                messageEn = "Consumed ${params.amount} ${item.unit} of ${item.name}."
            )
        } else {
            ActionResult(
                success = false,
                messageFa = "خطا در ثبت مصرف کالا از انبار.",
                messageEn = "Failed to consume warehouse item."
            )
        }
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

