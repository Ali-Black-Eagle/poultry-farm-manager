package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue

class HelpAction : AssistantAction<Unit> {
    override val intent: IntentId = IntentId.HELP

    override fun requiredSlots(): List<SlotId> = emptyList()

    override fun fromSlots(slots: Map<SlotId, SlotValue>) = Unit

    override suspend fun validate(params: Unit): ValidationResult = ValidationResult.Valid

    override fun preview(params: Unit): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "راهنمای فرامین",
            titleEn = "Help & Commands",
            fields = emptyList()
        )
    }

    override suspend fun execute(params: Unit): ActionResult {
        val fa = """
            💡 فرامینی که می‌توانید بگویید یا بنویسید:
            • ثبت تخم‌مرغ: «۱۲۰ تا تخم‌مرغ سالن ۱» یا «۴ شانه تخم جمع شد»
            • ثبت دان و آب: «۵۰ کیلو دان سالن ۲» یا «۱۰۰ لیتر آب اضافه کن»
            • ثبت تلفات: «۲ تا تلفات سالن ۱ به علت گرمازدگی»
            • فروش پرنده: «۵۰ مرغ سالن ۱ فروختم ۱۵ میلیون تومان»
            • کشتار پرنده: «کشتار ۱۰۰ مرغ سالن ۱ با وزن زنده ۲۴۰ کیلو و لاشه ۱۷۵ کیلو»
            • ثبت مالی: «هزینه دان ۲ میلیون تومان» یا «فروش تخم‌مرغ ۵۰۰ هزار»
            • انتقال پرنده: «انتقال ۵۰ پرنده از سالن ۱ به سالن ۲»
            • مصرف انبار: «مصرف ۲ کیسه دان برای سالن ۱»
            • استعلام‌ها: «وضعیت دان و آب»، «آمار امروز»، «تلفات این هفته»، «موجودی انبار»
            • یادآور: «یادآور واکسن فردا ساعت ۹»
            • برگشت: «لغو» یا «برگردان آخرین عملیات»
        """.trimIndent()
        val en = "You can record eggs, feed/water, mortality, bird sales, slaughter, transactions, transfers, query stats, or set reminders."
        return ActionResult(
            success = true,
            messageFa = fa,
            messageEn = en
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

