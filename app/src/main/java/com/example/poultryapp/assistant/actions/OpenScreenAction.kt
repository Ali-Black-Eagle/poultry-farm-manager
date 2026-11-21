package com.example.poultryapp.assistant.actions

import com.example.poultryapp.assistant.core.nlu.IntentId
import com.example.poultryapp.assistant.core.nlu.SlotId
import com.example.poultryapp.assistant.core.nlu.SlotValue

data class OpenScreenParams(
    val target: String
)

class OpenScreenAction(
    private val onNavigate: ((tab: String, subTab: String?) -> Unit)? = null
) : AssistantAction<OpenScreenParams> {

    override val intent: IntentId = IntentId.OPEN_SCREEN

    override fun requiredSlots(): List<SlotId> = listOf(SlotId.TARGET_SCREEN)

    override fun fromSlots(slots: Map<SlotId, SlotValue>): OpenScreenParams {
        val target = slots[SlotId.TARGET_SCREEN]?.typedValue?.toString() ?: "HOME"
        return OpenScreenParams(target)
    }

    override suspend fun validate(params: OpenScreenParams): ValidationResult = ValidationResult.Valid

    override fun preview(params: OpenScreenParams): ActionCardPreview {
        return ActionCardPreview(
            titleFa = "رفتن به صفحه",
            titleEn = "Navigate to Screen",
            fields = listOf("صفحه" to params.target)
        )
    }

    override suspend fun execute(params: OpenScreenParams): ActionResult {
        val parts = params.target.split(":")
        val tab = parts.firstOrNull() ?: "HOME"
        val subTab = if (parts.size > 1) parts[1] else null

        onNavigate?.invoke(tab, subTab)

        val titleFa = when (tab) {
            "MANAGEMENT" -> when (subTab) {
                "WAREHOUSE" -> "انبار و موجودی"
                "FINANCIAL" -> "مدیریت مالی"
                "RATION_BUILDER" -> "فرمولاسیون جیره"
                "CONTACTS" -> "مخاطبین فارم"
                "BIOSECURITY" -> "امنیت زیستی"
                "SIMULATORS" -> "شبیه‌ساز اقتصادی"
                else -> "مدیریت فارم"
            }
            "HEALTH" -> when (subTab) {
                "DIAGNOSIS" -> "تشخیص هوشمند بیماری‌ها"
                "MEDICATIONS" -> "داروها و واکسن‌ها"
                "MORTALITY_LOG" -> "ثبت و آمار تلفات"
                "HERBAL_CARE" -> "درمان‌های طبیعی و گیاهی"
                else -> "سلامت و درمان"
            }
            "FLOCKS" -> when (subTab) {
                "MORTALITY" -> "آمار تلفات"
                "SALES_SLAUGHTER" -> "فروش و کشتار پرنده"
                "FEED_WATER" -> "مدیریت دان و آب"
                "CALENDAR" -> "تقویم و یادآورها"
                "EGG_PRODUCTION" -> "تولید تخم‌مرغ"
                else -> "فهرست سالن‌ها و گله‌ها"
            }
            "SETTINGS" -> "تنظیمات برنامه"
            "BREEDING_GUIDE" -> "راهنمای جامع پرورش"
            "GOLDEN_BREEDING" -> "مسابقه و بازی پرورشی"
            else -> "صفحه اصلی داشبورد"
        }

        return ActionResult(
            success = true,
            messageFa = "صفحه $titleFa باز شد.",
            messageEn = "Navigated to $tab.",
            undoToken = null
        )
    }

    override suspend fun undo(undoToken: String): Boolean = false
}

