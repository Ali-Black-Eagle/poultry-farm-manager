package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue

class UndoAction(
    private val actionRegistry: ActionRegistry,
    private val lastActionTokenProvider: () -> Pair<IntentId, String>?
) : AssistantAction<Unit> {

    override val intent: IntentId = IntentId.UNDO_LAST

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>) = Unit

    override suspend fun validate(params: Unit): ValidationResult {
        val last = lastActionTokenProvider()
        return if (last != null) {
            ValidationResult.Valid
        } else {
            ValidationResult.Rejected(
                "عملیاتی برای بازگردانی وجود ندارد.",
                "There is no recent action to undo."
            )
        }
    }

    override fun preview(params: Unit): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "بازگردانی آخرین عملیات",
            titleEn = "Undo Last Action",
            fields = emptyList()
        )
    }

    override suspend fun execute(params: Unit): ActionResult {
        val last = lastActionTokenProvider()
            ?: return ActionResult(
                success = false,
                messageFa = "عملیاتی برای بازگردانی در این نشست یافت نشد.",
                messageEn = "No recent action found to undo in this session."
            )

        val (intent, token) = last
        val action = actionRegistry.get<Any>(intent)
        val success = action?.undo(token) ?: false

        return if (success) {
            ActionResult(
                success = true,
                messageFa = "آخرین عملیات با موفقیت لغو و از سیستم حذف شد.",
                messageEn = "The last action was successfully undone and removed."
            )
        } else {
            ActionResult(
                success = false,
                messageFa = "خطا در لغو عملیات یا رکورد قبلاً تغییر کرده است.",
                messageEn = "Failed to undo action or record was already modified."
            )
        }
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

