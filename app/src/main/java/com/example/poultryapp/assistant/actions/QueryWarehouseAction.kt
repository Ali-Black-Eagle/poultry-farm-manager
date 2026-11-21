package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.repository.WarehouseRepository
import com.example.poultryapp.engine.toPersianDigits

class QueryWarehouseAction(
    private val warehouseRepo: WarehouseRepository
) : AssistantAction<Unit> {

    override val intent: IntentId = IntentId.QUERY_WAREHOUSE

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>) = Unit

    override suspend fun validate(params: Unit): ValidationResult = ValidationResult.Valid

    override fun preview(params: Unit): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "استعلام موجودی انبار",
            titleEn = "Query Warehouse",
            fields = listOf("انبار" to "انبار مرکزی فارم")
        )
    }

    override suspend fun execute(params: Unit): ActionResult {
        val items = warehouseRepo.getAllWarehouseItems()
        if (items.isEmpty()) {
            return ActionResult(
                success = true,
                messageFa = "در حال حاضر هیچ قلم کالایی در انبار تعریف نشده است.",
                messageEn = "No items found in warehouse."
            )
        }

        val lowStock = items.filter { it.minAlertThreshold > 0.0 && it.currentStock <= it.minAlertThreshold }
        val sbFa = StringBuilder()
        sbFa.append("📦 موجودی انبار فارم (${toPersianDigits(items.size)} قلم کالا):\n\n")

        for (item in items) {
            val isLow = item.minAlertThreshold > 0.0 && item.currentStock <= item.minAlertThreshold
            val prefix = if (isLow) "⚠️ " else "• "
            sbFa.append("$prefix${item.name}: ${toPersianDigits(item.currentStock.toInt())} ${item.unit}")
            if (isLow) sbFa.append(" (رو به اتمام!)")
            sbFa.append("\n")
        }

        if (lowStock.isNotEmpty()) {
            sbFa.append("\n🔔 توجه: ${toPersianDigits(lowStock.size)} قلم کالا زیر حد آستانه هشدار هستند.")
        }

        return ActionResult(
            success = true,
            messageFa = sbFa.toString().trim(),
            messageEn = "Warehouse report generated for ${items.size} items."
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

