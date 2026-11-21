package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue

class GreetingAction : AssistantAction<Unit> {
    override val intent: IntentId = IntentId.GREETING

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>) = Unit

    override suspend fun validate(params: Unit): ValidationResult = ValidationResult.Valid

    override fun preview(params: Unit): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "سلام و احوالپرسی",
            titleEn = "Greeting",
            fields = emptyList()
        )
    }

    override suspend fun execute(params: Unit): ActionResult {
        val fa = "سلام! روزتون بخیر. من دستیار هوشمند فارم شما هستم. چطور می‌توانم کمکتان کنم؟ (ثبت تخم‌مرغ، دان و آب، تلفات، هزینه‌ها، استعلام‌ها یا یادآور)"
        val en = "Hello! I am your smart poultry assistant. How can I help you today?"
        return ActionResult(
            success = true,
            messageFa = fa,
            messageEn = en
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

