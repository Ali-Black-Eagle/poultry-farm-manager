package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.local.TransactionType
import com.example.poultryapp.data.repository.FinancialRepository
import com.example.poultryapp.engine.toPersianDigits
import java.time.LocalDate
import java.time.ZoneId

data class QueryFinanceParams(
    val metric: String? = null,
    val periodLabel: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

class QueryFinanceAction(
    private val financialRepo: FinancialRepository
) : AssistantAction<QueryFinanceParams> {

    override val intent: IntentId = IntentId.QUERY_FINANCE

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>): QueryFinanceParams {
        val metric = slots[SlotId.METRIC]?.typedValue as? String

        val periodPair = (slots[SlotId.PERIOD]?.typedValue as? Pair<*, *>)?.let { (s, e) ->
            if (s is LocalDate && e is LocalDate) Pair(s, e) else null
        }
        val singleDate = slots[SlotId.DATE]?.typedValue as? LocalDate
        val periodLabel = slots[SlotId.PERIOD]?.rawSpan ?: slots[SlotId.DATE]?.rawSpan

        val start = periodPair?.first ?: singleDate
        val end = periodPair?.second ?: singleDate

        return QueryFinanceParams(
            metric = metric,
            periodLabel = periodLabel,
            startDate = start,
            endDate = end
        )
    }

    override suspend fun validate(params: QueryFinanceParams): ValidationResult = ValidationResult.Valid

    override fun preview(params: QueryFinanceParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "استعلام آمار مالی فارم",
            titleEn = "Query Farm Financials",
            fields = listOf("بازه" to (params.periodLabel ?: "کل دوره"))
        )
    }

    override suspend fun execute(params: QueryFinanceParams): ActionResult {
        val allTx = financialRepo.getAllTransactions()
        val filteredTx = if (params.startDate != null && params.endDate != null) {
            val startMillis = params.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = params.endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            allTx.filter { it.timestampMillis in startMillis..endMillis }
        } else {
            allTx
        }

        val totalExpense = filteredTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountTomans }
        val totalIncome = filteredTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountTomans }
        val balance = totalIncome - totalExpense

        val formatExp = toPersianDigits(String.format("%,.0f", totalExpense))
        val formatInc = toPersianDigits(String.format("%,.0f", totalIncome))
        val formatBal = toPersianDigits(String.format("%,.0f", balance))

        val periodText = if (params.periodLabel != null) " (${params.periodLabel})" else ""
        val messageFa = "گزارش مالی فارم$periodText: مجموع درآمدها $formatInc تومان، مجموع هزینه‌ها $formatExp تومان و تراز خالص $formatBal تومان است."
        val messageEn = "Farm financials$periodText: Total income $formatInc Toman, total expenses $formatExp Toman, net balance $formatBal Toman."

        return ActionResult(
            success = true,
            messageFa = messageFa,
            messageEn = messageEn
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}
