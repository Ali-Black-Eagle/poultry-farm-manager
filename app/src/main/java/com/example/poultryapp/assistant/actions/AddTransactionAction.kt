package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.entities.DateParser
import com.example.poultryapp.assistant.core.dialogue.SafetyChecks
import com.example.poultryapp.assistant.core.dialogue.SafetyResult
import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.local.FinancialTransaction
import com.example.poultryapp.data.local.TransactionCategory
import com.example.poultryapp.data.local.TransactionType
import com.example.poultryapp.data.repository.FinancialRepository
import com.example.poultryapp.data.repository.FlockRepository
import com.example.poultryapp.engine.toPersianDigits
import java.time.LocalDate

data class AddTransactionParams(
    val amountTomans: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: TransactionCategory = TransactionCategory.OTHER_EXPENSE,
    val date: LocalDate = LocalDate.now(),
    val flockId: Long? = null,
    val flockName: String? = null,
    val note: String? = null
)

class AddTransactionAction(
    private val financialRepo: FinancialRepository,
    private val flockRepo: FlockRepository
) : AssistantAction<AddTransactionParams> {

    override val intent: IntentId = IntentId.ADD_TRANSACTION

    override fun requiredSlots(): List<SlotId> = listOf(SlotId.AMOUNT)

    override fun fromSlots(slots: Map<SlotId, SlotValue>): AddTransactionParams {
        val amount = (slots[SlotId.AMOUNT]?.typedValue as? Number)?.toDouble() ?: 0.0
        val typeStr = slots[SlotId.TRANSACTION_TYPE]?.typedValue as? String
        val isIncome = typeStr.equals("INCOME", ignoreCase = true)
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        val catKey = slots[SlotId.CATEGORY]?.typedValue as? String
        val category = when (catKey?.uppercase()) {
            "FEED" -> TransactionCategory.FEED_PURCHASE
            "MEDICATION" -> TransactionCategory.MED_PURCHASE
            "EGG_SALES" -> TransactionCategory.EGG_SALE
            "CHICKEN_SALES" -> TransactionCategory.BIRD_SALE
            "EQUIPMENT" -> TransactionCategory.EQUIPMENT
            else -> if (isIncome) TransactionCategory.OTHER_INCOME else TransactionCategory.OTHER_EXPENSE
        }

        val date = (slots[SlotId.DATE]?.typedValue as? LocalDate) ?: LocalDate.now()
        val flockId = (slots[SlotId.FLOCK_ID]?.typedValue as? Number)?.toLong()
        val flockName = slots[SlotId.FLOCK_NAME]?.typedValue as? String
        val note = slots[SlotId.NOTE]?.typedValue as? String

        return AddTransactionParams(
            amountTomans = amount,
            type = type,
            category = category,
            date = date,
            flockId = flockId,
            flockName = flockName,
            note = note
        )
    }

    override suspend fun validate(params: AddTransactionParams): ValidationResult {
        val safety = SafetyChecks.validateMoneyAmount(params.amountTomans)
        return when (safety) {
            is SafetyResult.Passed -> ValidationResult.Valid
            is SafetyResult.Warning -> ValidationResult.Warning(safety.warningMessage, safety.warningMessage)
            is SafetyResult.Rejected -> ValidationResult.Rejected(safety.reasonMessage, safety.reasonMessage)
        }
    }

    override fun preview(params: AddTransactionParams): ActionCardPreview {
        val isExp = params.type == TransactionType.EXPENSE
        val jalaliStr = DateParser.formatToStandardJalali(params.date)
        val fields = mutableListOf(
            "مبلغ" to "${toPersianDigits(String.format("%,.0f", params.amountTomans))} تومان",
            "سرفصل" to params.category.titleFa,
            "نوع" to (if (isExp) "هزینه" else "درآمد"),
            "تاریخ" to toPersianDigits(jalaliStr)
        )
        if (params.flockName != null) {
            fields.add("گله مرتبط" to params.flockName)
        }
        return ActionCardPreview(
            titleFa = if (isExp) "ثبت هزینه فارم" else "ثبت درآمد فارم",
            titleEn = if (isExp) "Record Farm Expense" else "Record Farm Income",
            fields = fields,
            isExpense = isExp
        )
    }

    override suspend fun execute(params: AddTransactionParams): ActionResult {
        val jalaliStr = DateParser.formatToStandardJalali(params.date)
        val epochMillis = DateParser.toEpochMillis(params.date)
        val title = if (params.type == TransactionType.INCOME) "درآمد: ${params.category.titleFa}" else "هزینه: ${params.category.titleFa}"

        val tx = FinancialTransaction(
            title = title,
            amountTomans = params.amountTomans,
            type = params.type,
            category = params.category,
            dateJalaliStr = jalaliStr,
            timestampMillis = epochMillis,
            relatedFlockId = params.flockId,
            relatedFlockName = params.flockName,
            notes = params.note ?: "ثبت شده با دستیار هوشمند"
        )

        val id = financialRepo.insertTransaction(tx)
        val typeLabelFa = if (params.type == TransactionType.INCOME) "درآمد" else "هزینه"
        val formattedAmount = toPersianDigits(String.format("%,.0f", params.amountTomans))
        return ActionResult(
            success = true,
            messageFa = "$typeLabelFa به مبلغ $formattedAmount تومان در سرفصل «${params.category.titleFa}» برای تاریخ ${toPersianDigits(jalaliStr)} ثبت شد.",
            messageEn = "Recorded $typeLabelFa of $formattedAmount Toman for '${params.category.titleFa}'.",
            createdRecordId = id,
            undoToken = "FIN_$id"
        )
    }

    override suspend fun undo(undoToken: String): Boolean {
        val id = undoToken.removePrefix("FIN_").toLongOrNull() ?: return false
        return financialRepo.deleteTransaction(id)
    }
}

