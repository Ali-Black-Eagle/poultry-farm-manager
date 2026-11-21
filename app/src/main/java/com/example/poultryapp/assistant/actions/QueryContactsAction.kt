package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue
import com.example.poultryapp.data.repository.FarmContactRepository
import com.example.poultryapp.engine.toPersianDigits

data class QueryContactsParams(
    val query: String
)

class QueryContactsAction(
    private val contactRepo: FarmContactRepository
) : AssistantAction<QueryContactsParams> {

    override val intent: IntentId = IntentId.QUERY_CONTACTS

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>): QueryContactsParams {
        val q = slots[SlotId.CONTACT_NAME]?.typedValue?.toString()
            ?: slots[SlotId.NOTE]?.typedValue?.toString()
            ?: ""
        return QueryContactsParams(q)
    }

    override suspend fun validate(params: QueryContactsParams): ValidationResult = ValidationResult.Valid

    override fun preview(params: QueryContactsParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "استعلام دفترچه مخاطبین",
            titleEn = "Query Contacts",
            fields = listOf("جستجو" to params.query)
        )
    }

    override suspend fun execute(params: QueryContactsParams): ActionResult {
        val allContacts = contactRepo.getAllContacts()
        if (allContacts.isEmpty()) {
            return ActionResult(
                success = true,
                messageFa = "در حال حاضر هیچ مخاطبی در دفترچه مخاطبین فارم ثبت نشده است.",
                messageEn = "No farm contacts found."
            )
        }

        val q = params.query.trim().lowercase()
        val matched = if (q.isNotBlank()) {
            allContacts.filter { c ->
                c.fullName.lowercase().contains(q) ||
                        (c.businessName?.lowercase()?.contains(q) == true) ||
                        c.category.titleFa.lowercase().contains(q) ||
                        c.phonePrimary.contains(q)
            }
        } else {
            allContacts
        }

        if (matched.isEmpty()) {
            return ActionResult(
                success = true,
                messageFa = "مخاطبی با مشخصات '$q' در دفترچه تلفن فارم پیدا نشد.",
                messageEn = "Contact not found."
            )
        }

        val sb = StringBuilder()
        sb.append("📞 مخاطبین فارم (${toPersianDigits(matched.size)} نفر):\n\n")

        for (c in matched) {
            sb.append("• ${c.fullName}")
            if (!c.businessName.isNullOrBlank()) sb.append(" (${c.businessName})")
            sb.append(" - ${c.category.titleFa}\n")
            sb.append("  شماره تماس: ${toPersianDigits(c.phonePrimary)}\n")
            if (c.balanceTomans != 0.0) {
                val balanceType = if (c.balanceTomans > 0) "طلبکار" else "بدهکار"
                val absVal = Math.abs(c.balanceTomans).toInt()
                sb.append("  مانده حساب: ${toPersianDigits(absVal)} تومان ($balanceType)\n")
            }
        }

        return ActionResult(
            success = true,
            messageFa = sb.toString().trim(),
            messageEn = "Found ${matched.size} contacts."
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

